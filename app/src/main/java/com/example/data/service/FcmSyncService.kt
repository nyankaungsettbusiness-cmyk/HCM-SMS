package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.db.AppDatabase
import com.example.data.remote.SupabaseClientManager
import com.example.data.sync.SyncManager
import com.example.data.sync.model.millisToIso
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("school_id") val schoolId: String = "default_school",
    val token: String,
    val platform: String = "android",
    @SerialName("device_model") val deviceModel: String = "",
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)

class FcmSyncService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "FcmSyncService"
        private const val CHANNEL_ID = "school_management_fcm_notifications"
        private const val CHANNEL_NAME = "School Management Notifications"
        private const val PREFS_NAME = "fcm_sync_prefs"
        private const val KEY_FCM_TOKEN = "cached_fcm_token"

        fun getCachedToken(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, null)
        }

        fun saveCachedToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }

        fun registerTokenWithSupabase(
            context: Context,
            token: String,
            userId: String = "default_user",
            schoolId: String = "default_school"
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = SupabaseClientManager.getInstance() ?: return@launch
                    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val dto = DeviceTokenDto(
                        userId = userId,
                        schoolId = schoolId,
                        token = token,
                        platform = "android",
                        deviceModel = deviceModel,
                        lastSeenAt = millisToIso(System.currentTimeMillis())
                    )
                    client.from("device_tokens").upsert(dto, onConflict = "token")
                    Log.i(TAG, "Device FCM token successfully registered with Supabase")
                } catch (e: Exception) {
                    Log.w(TAG, "FCM token registration note: ${e.message}")
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Token received: ${token.take(10)}...")
        saveCachedToken(applicationContext, token)
        registerTokenWithSupabase(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val messageType = data["type"] ?: "sync_hint"

        when (messageType) {
            "sync_hint" -> {
                // Background delta sync hint - no sensitive data in payload
                val entity = data["entity"] ?: "all"
                val schoolId = data["school_id"] ?: "default_school"
                val versionStr = data["version"]
                val version = versionStr?.toLongOrNull() ?: 0L

                Log.i(TAG, "⚡ FCM Sync Hint: entity='$entity', schoolId='$schoolId', version=$version -> Triggering automatic delta sync")
                SyncManager.handleFcmSyncHint(applicationContext, entity, schoolId, version)
            }
            "notification" -> {
                // User-facing important event (e.g. Exam Published, Important Announcement, Fee receipt)
                val title = data["title"] ?: remoteMessage.notification?.title ?: "School Update"
                val body = data["body"] ?: remoteMessage.notification?.body ?: "A new school update is available."
                showNotification(title, body)

                // Also run delta sync for the associated entity if present
                val entity = data["entity"]
                if (!entity.isNullOrBlank()) {
                    SyncManager.handleFcmSyncHint(applicationContext, entity, data["school_id"] ?: "", 0L)
                }
            }
            else -> {
                // If standard notification payload is present
                remoteMessage.notification?.let {
                    showNotification(it.title ?: "School Update", it.body ?: "")
                }
                SyncManager.triggerSyncAsync(applicationContext, forceImmediate = false)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "School Management sync hints and critical announcements"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
