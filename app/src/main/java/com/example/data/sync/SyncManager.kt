package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.example.data.local.db.AppDatabase
import com.example.data.local.entity.OutboxOperation
import com.example.data.local.entity.OutboxStatus
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.remote.SupabaseClientManager
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class SyncStatus {
    object Idle : SyncStatus()
    data class OfflinePending(val count: Int) : SyncStatus()
    object Syncing : SyncStatus()
    data class Synced(val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
    data class SyncFailed(val reason: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()

    // Backward-compatibility aliases
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
    data class Error(val errorMessage: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
}

enum class SupabaseConnectionMode {
    ONLINE,
    SYNCING,
    OFFLINE
}

/**
 * Intelligent, Bandwidth-Efficient Multi-Device Synchronization Engine.
 *
 * Key Pillars:
 * 1. Offline-First Outbox Queue: Instant local writes; mutations queued in Room outbox and synced idempotently.
 * 2. Automatic Delta Synchronization: Uses `updated_at > last_synced_at`, pagination, and entity targeting (no full table reloads).
 * 3. Screen-Scoped Realtime: Active ONLY while collaborative screens are visible; unsubscribed on leave/background.
 * 4. Background FCM Sync Hints: Thin sync signals trigger targeted background delta fetch without transmitting payload data.
 * 5. Conflict Resolution: Version-based optimistic concurrency, protecting attendance and marks.
 */
object SyncManager {
    private const val TAG = "SyncManager"
    private val globalSyncMutex = Mutex()
    private val tableMutexMap = ConcurrentHashMap<String, Mutex>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Active screen realtime channels
    private val activeScreenChannels = ConcurrentHashMap<String, RealtimeChannel>()
    private val activeScreenJobs = ConcurrentHashMap<String, Job>()

    // Debounce & Throttling
    private var globalDebounceJob: Job? = null
    private var lastGlobalSyncTime: Long = 0L
    private val tableDebounceJobs = ConcurrentHashMap<String, Job>()
    private val tableLastSyncTimes = ConcurrentHashMap<String, Long>()

    private const val TABLE_DEBOUNCE_DELAY_MS = 1500L
    private const val TABLE_MIN_THROTTLE_MS = 3000L
    private const val GLOBAL_DEBOUNCE_DELAY_MS = 2000L
    private const val GLOBAL_MIN_AUTO_THROTTLE_MS = 10_000L
    private const val DELTA_SYNC_INTERVAL_MS = 120_000L // 2 min minimum interval for idle auto-delta

    var appContext: Context? = null
        private set
    private var isNetworkCallbackRegistered = false
    private var isLifecycleRegistered = false

    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    private val _connectionMode = MutableStateFlow<SupabaseConnectionMode>(SupabaseConnectionMode.ONLINE)
    val connectionMode: StateFlow<SupabaseConnectionMode> = _connectionMode.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow<Boolean>(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _pendingChangesCount = MutableStateFlow<Int>(0)
    val pendingChangesCount: StateFlow<Int> = _pendingChangesCount.asStateFlow()

    val diagnosticSyncState: StateFlow<SyncState> = SyncDiagnosticUtility.syncState

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        registerNetworkCallback(app)
        registerAppLifecycle(app)
        refreshPendingChangesCount(app)
        schedulePeriodicSync(app)

        // Run initial delta sync on startup if online
        coroutineScope.launch {
            delay(1500)
            if (_isNetworkAvailable.value && SupabaseClientManager.isConfigured()) {
                performDeltaSync(app, forceImmediate = false)
            }
        }
    }

    private fun getTableMutex(tableName: String): Mutex {
        val key = tableName.trim().lowercase()
        return tableMutexMap.computeIfAbsent(key) { Mutex() }
    }

    private fun registerNetworkCallback(context: Context) {
        if (isNetworkCallbackRegistered) return
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                _isNetworkAvailable.value = isConnected
                _connectionMode.value = if (isConnected) SupabaseConnectionMode.ONLINE else SupabaseConnectionMode.OFFLINE

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "🌐 Network restored -> Automatic delta sync & outbox flush triggered")
                        _isNetworkAvailable.value = true
                        _connectionMode.value = SupabaseConnectionMode.ONLINE
                        coroutineScope.launch {
                            processOutboxQueue(context)
                            performDeltaSync(context, forceImmediate = false)
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.i(TAG, "🌐 Network lost -> Operating in offline-first mode")
                        _isNetworkAvailable.value = false
                        _connectionMode.value = SupabaseConnectionMode.OFFLINE
                        refreshPendingChangesCount(context)
                    }
                })
                isNetworkCallbackRegistered = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }
    }

    private fun registerAppLifecycle(context: Context) {
        if (isLifecycleRegistered) return
        try {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                try {
                    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onStart(owner: LifecycleOwner) {
                            Log.i(TAG, "📱 App entered foreground -> Running automatic delta sync")
                            coroutineScope.launch {
                                if (_isNetworkAvailable.value && SupabaseClientManager.isConfigured()) {
                                    processOutboxQueue(context)
                                    performDeltaSync(context, forceImmediate = false)
                                }
                            }
                        }

                        override fun onStop(owner: LifecycleOwner) {
                            Log.i(TAG, "📱 App moved to background -> Disconnecting active screen realtime sockets")
                            unsubscribeAllRealtime()
                        }
                    })
                    isLifecycleRegistered = true
                } catch (e: Exception) {
                    Log.w(TAG, "Lifecycle observer registration note: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not post lifecycle registration: ${e.message}")
        }
    }

    // ========================================================================
    // SCREEN-SCOPED REALTIME COLLABORATION (Scoped by school_id & Screen)
    // ========================================================================

    /**
     * Subscribes to Supabase Realtime Postgres Changes strictly for the currently active visible screen.
     * When the user navigates away or backgrounds the app, [unsubscribeScreenRealtime] MUST be called.
     */
    fun subscribeScreenRealtime(
        screenKey: String,
        tableName: String,
        schoolId: String = "default_school",
        onRecordChanged: ((String) -> Unit)? = null
    ) {
        if (!_isNetworkAvailable.value || !SupabaseClientManager.isConfigured()) return

        unsubscribeScreenRealtime(screenKey)

        val job = coroutineScope.launch {
            try {
                val client = SupabaseClientManager.getInstance() ?: return@launch
                val channelId = "screen-$screenKey-$tableName"
                val channel = client.channel(channelId)

                Log.i(TAG, "⚡ Screen '$screenKey' subscribing to Realtime table '$tableName' (schoolId=$schoolId)")

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = tableName
                }

                val flowJob = launch {
                    changeFlow.collect { action ->
                        Log.i(TAG, "⚡ Active Screen Realtime Event ($tableName) -> Triggering targeted single-table delta sync")
                        onRecordChanged?.invoke(tableName)
                        triggerTableSyncAsync(tableName = tableName, context = appContext, forceImmediate = true)
                    }
                }

                channel.subscribe()
                activeScreenChannels[screenKey] = channel
                activeScreenJobs[screenKey] = flowJob
            } catch (e: Exception) {
                Log.w(TAG, "Screen Realtime subscription note ($screenKey): ${e.message}")
            }
        }
        activeScreenJobs["job_$screenKey"] = job
    }

    /**
     * Unsubscribes immediately from Realtime channel when screen is disposed or backgrounded.
     */
    fun unsubscribeScreenRealtime(screenKey: String) {
        try {
            activeScreenJobs[screenKey]?.cancel()
            activeScreenJobs.remove(screenKey)
            activeScreenJobs["job_$screenKey"]?.cancel()
            activeScreenJobs.remove("job_$screenKey")

            val channel = activeScreenChannels.remove(screenKey)
            if (channel != null) {
                coroutineScope.launch {
                    try {
                        val client = SupabaseClientManager.getInstance()
                        client?.realtime?.removeChannel(channel)
                        Log.d(TAG, "⚡ Realtime channel for '$screenKey' successfully removed/unsubscribed.")
                    } catch (e: Exception) {
                        Log.d(TAG, "Channel remove note: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unsubscribing screen Realtime: ${e.message}")
        }
    }

    private fun unsubscribeAllRealtime() {
        activeScreenChannels.keys.forEach { screenKey ->
            unsubscribeScreenRealtime(screenKey)
        }
    }

    // ========================================================================
    // FCM BACKGROUND SYNC HINT HANDLER
    // ========================================================================

    /**
     * Receives FCM background sync hints and automatically triggers a targeted delta sync.
     */
    fun handleFcmSyncHint(context: Context, entity: String, schoolId: String, version: Long) {
        appContext = context.applicationContext
        coroutineScope.launch {
            try {
                Log.i(TAG, "⚡ Ingesting FCM Sync Hint for entity '$entity' (schoolId=$schoolId, version=$version)")
                if (entity.equals("all", ignoreCase = true)) {
                    performDeltaSync(context, forceImmediate = true)
                } else {
                    performSingleTableSync(tableName = entity, context = context, forceImmediate = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error processing FCM sync hint: ${e.message}")
            }
        }
    }

    // ========================================================================
    // OUTBOX QUEUE PROCESSING (Offline-First Mutations)
    // ========================================================================

    /**
     * Queues an offline mutation into the local Room outbox for safe, idempotent retry.
     */
    suspend fun enqueueOutboxMutation(
        context: Context,
        entityType: String,
        entityUuid: String,
        operation: OutboxOperation,
        payloadJson: String = "{}",
        schoolId: String = "default_school",
        version: Long = 1
    ): Long {
        val db = AppDatabase.getInstance(context)
        val entry = SyncOutboxEntity(
            entityType = entityType,
            entityUuid = entityUuid,
            operation = operation,
            payloadJson = payloadJson,
            schoolId = schoolId,
            version = version,
            status = OutboxStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = db.syncOutboxDao().enqueue(entry)
        refreshPendingChangesCount(context)

        // If online, immediately trigger background outbox flush
        if (_isNetworkAvailable.value) {
            coroutineScope.launch {
                processOutboxQueue(context)
            }
        }
        return id
    }

    /**
     * Flushes pending items in the outbox queue to Supabase.
     */
    suspend fun processOutboxQueue(context: Context): Int {
        if (!_isNetworkAvailable.value || !SupabaseClientManager.isConfigured()) {
            return 0
        }
        val db = AppDatabase.getInstance(context)
        val outboxDao = db.syncOutboxDao()
        val pendingEntries = outboxDao.getPendingOutbox(limit = 50)
        if (pendingEntries.isEmpty()) return 0

        var processedCount = 0
        val repository = SyncRepository(db)

        for (entry in pendingEntries) {
            try {
                outboxDao.updateStatus(entry.id, OutboxStatus.SYNCING)
                val success = repository.syncOutboxEntry(entry)
                if (success) {
                    outboxDao.deleteById(entry.id)
                    processedCount++
                } else {
                    outboxDao.markFailed(entry.id, "Sync attempt returned false")
                }
            } catch (e: Exception) {
                outboxDao.markFailed(entry.id, e.message ?: "Unknown error")
                Log.w(TAG, "Outbox processing error for ${entry.entityType}/${entry.entityUuid}: ${e.message}")
            }
        }
        refreshPendingChangesCount(context)
        return processedCount
    }

    // ========================================================================
    // AUTOMATIC DELTA SYNCHRONIZATION
    // ========================================================================

    /**
     * Performs automatic delta synchronization without pulling full tables.
     */
    suspend fun performDeltaSync(context: Context? = null, forceImmediate: Boolean = false): SyncResult {
        val ctx = context?.applicationContext ?: appContext
            ?: return SyncResult.Error("Context not available for sync")
        appContext = ctx

        if (!_isNetworkAvailable.value) {
            _connectionMode.value = SupabaseConnectionMode.OFFLINE
            val dirty = AppDatabase.getInstance(ctx).syncOutboxDao().getPendingCount()
            _syncState.value = SyncStatus.OfflinePending(dirty)
            return SyncResult.Error("Offline: Device is operating with local cache.")
        }

        return globalSyncMutex.withLock {
            val db = AppDatabase.getInstance(ctx)
            val repository = SyncRepository(db)

            val dirtyCount = repository.countDirtyRecords() + db.syncOutboxDao().getPendingCount()
            _pendingChangesCount.value = dirtyCount

            val now = System.currentTimeMillis()
            val timeSinceLast = now - _lastSyncTime.value

            if (!forceImmediate && dirtyCount == 0 && timeSinceLast < DELTA_SYNC_INTERVAL_MS && _lastSyncTime.value > 0L) {
                return@withLock SyncResult.Success()
            }

            _syncState.value = SyncStatus.Syncing
            _connectionMode.value = SupabaseConnectionMode.SYNCING
            Log.i(TAG, "⚡ Executing Automatic Delta Sync (dirty=$dirtyCount)...")

            try {
                // 30-second guard to guarantee sync never hangs
                val result = kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                    // 1. Flush Outbox first
                    processOutboxQueue(ctx)

                    // 2. Delta fetch only modified entities
                    repository.syncAllEntities()
                } ?: SyncResult.Error("Sync timed out. Operating in offline cache.")

                when (result) {
                    is SyncResult.Success -> {
                        _lastSyncTime.value = System.currentTimeMillis()
                        _syncState.value = SyncStatus.Synced(_lastSyncTime.value)
                        _connectionMode.value = SupabaseConnectionMode.ONLINE
                        Log.i(TAG, "⚡ Delta Sync completed: Pushed=${result.totalPushed}, Pulled=${result.totalPulled}")
                    }
                    is SyncResult.Error -> {
                        _syncState.value = SyncStatus.SyncFailed(result.message)
                        _connectionMode.value = if (_isNetworkAvailable.value) SupabaseConnectionMode.ONLINE else SupabaseConnectionMode.OFFLINE
                        Log.w(TAG, "⚡ Delta Sync note: ${result.message}")
                    }
                }
                result
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "⚡ Delta Sync error: ${e.message}", e)
                _syncState.value = SyncStatus.SyncFailed(e.message ?: "Sync error")
                SyncResult.Error(e.message ?: "Sync error")
            } finally {
                // Guarantee the sync indicator never stays in SYNCING state
                if (_connectionMode.value == SupabaseConnectionMode.SYNCING) {
                    _connectionMode.value = if (_isNetworkAvailable.value) SupabaseConnectionMode.ONLINE else SupabaseConnectionMode.OFFLINE
                }
                if (_syncState.value is SyncStatus.Syncing) {
                    _syncState.value = SyncStatus.Synced(_lastSyncTime.value.takeIf { it > 0L } ?: System.currentTimeMillis())
                }
                refreshPendingChangesCount(ctx)
            }
        }
    }

    /**
     * Backward compatible wrapper for performSync.
     */
    suspend fun performSync(context: Context? = null, forceImmediate: Boolean = false): SyncResult {
        return performDeltaSync(context, forceImmediate)
    }

    /**
     * Targeted Single-Table Synchronization.
     */
    suspend fun performSingleTableSync(
        tableName: String,
        context: Context? = null,
        forceImmediate: Boolean = false
    ): Pair<Int, Int> {
        val ctx = context?.applicationContext ?: appContext ?: return Pair(0, 0)
        appContext = ctx

        if (!_isNetworkAvailable.value || !SupabaseClientManager.isConfigured()) {
            return Pair(0, 0)
        }

        val cleanTable = tableName.trim().lowercase()
        val tableMutex = getTableMutex(cleanTable)

        return tableMutex.withLock {
            val now = System.currentTimeMillis()
            val lastTime = tableLastSyncTimes[cleanTable] ?: 0L

            if (!forceImmediate && (now - lastTime < TABLE_MIN_THROTTLE_MS)) {
                return@withLock Pair(0, 0)
            }

            try {
                Log.i(TAG, "⚡ Executing Targeted Single-Table Delta Sync for '$cleanTable'...")
                val db = AppDatabase.getInstance(ctx)
                val repository = SyncRepository(db)

                val (pushed, pulled) = repository.syncSingleTable(cleanTable)
                tableLastSyncTimes[cleanTable] = System.currentTimeMillis()
                _lastSyncTime.value = System.currentTimeMillis()
                _syncState.value = SyncStatus.Synced(_lastSyncTime.value)
                _connectionMode.value = SupabaseConnectionMode.ONLINE
                Pair(pushed, pulled)
            } catch (e: Exception) {
                Log.e(TAG, "Error in targeted delta sync for '$cleanTable': ${e.message}", e)
                Pair(0, 0)
            } finally {
                if (_connectionMode.value == SupabaseConnectionMode.SYNCING) {
                    _connectionMode.value = if (_isNetworkAvailable.value) SupabaseConnectionMode.ONLINE else SupabaseConnectionMode.OFFLINE
                }
                refreshPendingChangesCount(ctx)
            }
        }
    }

    fun triggerTableSyncAsync(
        tableName: String,
        context: Context? = null,
        forceImmediate: Boolean = false
    ) {
        val ctx = context?.applicationContext ?: appContext ?: return
        val cleanTable = tableName.trim().lowercase()
        refreshPendingChangesCount(ctx)

        tableDebounceJobs[cleanTable]?.cancel()
        val job = coroutineScope.launch {
            if (!forceImmediate) {
                delay(TABLE_DEBOUNCE_DELAY_MS)
            }
            performSingleTableSync(cleanTable, ctx, forceImmediate = forceImmediate)
        }
        tableDebounceJobs[cleanTable] = job
    }

    fun triggerSyncAsync(context: Context? = null, forceImmediate: Boolean = false) {
        val ctx = context?.applicationContext ?: appContext ?: return
        appContext = ctx
        refreshPendingChangesCount(ctx)

        globalDebounceJob?.cancel()
        globalDebounceJob = coroutineScope.launch {
            if (!forceImmediate) {
                delay(GLOBAL_DEBOUNCE_DELAY_MS)
                val now = System.currentTimeMillis()
                if (now - lastGlobalSyncTime < GLOBAL_MIN_AUTO_THROTTLE_MS) return@launch
                lastGlobalSyncTime = now
            }
            performDeltaSync(ctx, forceImmediate = forceImmediate)
        }
    }

    fun refreshPendingChangesCount(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return
        coroutineScope.launch {
            try {
                val db = AppDatabase.getInstance(ctx)
                val repository = SyncRepository(db)
                val dirty = repository.countDirtyRecords() + db.syncOutboxDao().getPendingCount()
                _pendingChangesCount.value = dirty
                if (!_isNetworkAvailable.value && dirty > 0) {
                    _syncState.value = SyncStatus.OfflinePending(dirty)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not count pending changes: ${e.message}")
            }
        }
    }

    /**
     * Instantly clears all local pending dirty marks across all database tables.
     * Resets pending count to 0 and clears the outbox.
     */
    fun clearAllPendingChanges(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return
        coroutineScope.launch {
            try {
                val db = AppDatabase.getInstance(ctx)
                val repository = SyncRepository(db)
                repository.clearAllDirtyFlags(db)
                _pendingChangesCount.value = 0
                _syncState.value = SyncStatus.Idle
                refreshPendingChangesCount(ctx)
            } catch (e: Exception) {
                Log.e(TAG, "Error in clearAllPendingChanges: ${e.message}", e)
            }
        }
    }

    fun schedulePeriodicSync(context: Context? = null, intervalMinutes: Long = 240) {
        val ctx = context?.applicationContext ?: appContext ?: return
        appContext = ctx
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                SyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic sync with WorkManager: ${e.message}", e)
        }
    }

    suspend fun runDiagnosticAudit(context: Context? = null): SyncDiagnosticReport? {
        val ctx = context?.applicationContext ?: appContext ?: return null
        appContext = ctx
        val db = AppDatabase.getInstance(ctx)
        val repository = SyncRepository(db)
        return repository.runDiagnosticAudit(db)
    }

    suspend fun repairDeletionTombstones(context: Context? = null): Int {
        val ctx = context?.applicationContext ?: appContext ?: return 0
        appContext = ctx
        val db = AppDatabase.getInstance(ctx)
        val repository = SyncRepository(db)
        val count = repository.repairDeletionTombstones(db)
        refreshPendingChangesCount(ctx)
        return count
    }

    suspend fun verifyAndPropagateDeletion(
        tableName: String,
        uuid: String = "",
        codeOrKey: String = "",
        fallbackRemoteId: Long? = null,
        preferHardDelete: Boolean = true,
        context: Context? = null
    ): DeletionVerificationResult {
        val ctx = context?.applicationContext ?: appContext
            ?: return DeletionVerificationResult.OfflineQueued(
                tableName = tableName,
                identifier = uuid.ifBlank { codeOrKey },
                message = "Context not available for delete verification."
            )
        val db = AppDatabase.getInstance(ctx)
        val repository = SyncRepository(db)
        val result = repository.verifyAndPropagateDeletion(
            tableName = tableName,
            uuid = uuid,
            codeOrKey = codeOrKey,
            fallbackRemoteId = fallbackRemoteId,
            preferHardDelete = preferHardDelete
        )
        refreshPendingChangesCount(ctx)
        return result
    }

    suspend fun testConnection(context: Context? = null): Boolean {
        val ctx = context?.applicationContext ?: appContext ?: return false
        appContext = ctx
        if (!SupabaseClientManager.isConfigured()) return false
        val db = AppDatabase.getInstance(ctx)
        val repository = SyncRepository(db)
        return repository.testConnectivity()
    }
}
