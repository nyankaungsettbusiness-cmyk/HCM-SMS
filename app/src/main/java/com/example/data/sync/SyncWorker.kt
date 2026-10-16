package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.db.AppDatabase

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "hcm_sms_sync_worker"
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting background sync worker execution...")

        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val syncRepository = SyncRepository(database)

            when (val result = syncRepository.syncAllEntities()) {
                is SyncResult.Success -> {
                    Log.i(TAG, "Background sync completed successfully: $result")
                    Result.success()
                }
                is SyncResult.Error -> {
                    if (result.message.contains("offline", ignoreCase = true) ||
                        result.message.contains("unreachable", ignoreCase = true) ||
                        result.message.contains("not configured", ignoreCase = true) ||
                        result.message.contains("UnknownHost", ignoreCase = true) ||
                        result.cause is java.net.UnknownHostException ||
                        result.cause is java.io.IOException
                    ) {
                        Log.i(TAG, "Background sync deferred: device is offline or host unreachable.")
                    } else {
                        Log.w(TAG, "Background sync did not finish: ${result.message}")
                    }
                    Result.success()
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "Background sync worker was cancelled.")
            Result.success()
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException ||
                e is java.io.IOException ||
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true
            ) {
                Log.i(TAG, "Background sync deferred due to network condition: ${e.message}")
            } else {
                Log.w(TAG, "Exception during background sync worker: ${e.message}")
            }
            Result.success()
        }
    }
}
