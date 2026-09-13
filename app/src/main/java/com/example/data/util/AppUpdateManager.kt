package com.example.data.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val currentVersionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val publishedAt: String
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    // Optional override or fallback repository (owner/repo)
    private const val DEFAULT_REPO = "nyankaungsett-business/hcm-sms"

    /**
     * Checks GitHub Releases for a newer version than current BuildConfig.VERSION_NAME
     */
    suspend fun checkForUpdates(
        repoSlug: String = DEFAULT_REPO,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.github.com/repos/$repoSlug/releases/latest"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "HCM-SMS-Android")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "").removePrefix("v")
                val cleanTagName = tagName.substringBefore("-build").substringBefore("-")
                val releaseNotes = json.optString("body", "Bug fixes and improvements.")
                val publishedAt = json.optString("published_at", "")

                // Look for .apk asset
                var apkUrl = ""
                val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }

                // If no asset found, fallback to html_url
                if (apkUrl.isBlank()) {
                    apkUrl = json.optString("html_url", "")
                }

                val isNewer = isVersionNewer(cleanTagName, currentVersion)

                return@withContext AppUpdateInfo(
                    hasUpdate = isNewer,
                    latestVersionName = cleanTagName.ifBlank { tagName },
                    currentVersionName = currentVersion,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    publishedAt = publishedAt
                )
            } else {
                Log.w(TAG, "GitHub API returned HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
        }

        return@withContext AppUpdateInfo(
            hasUpdate = false,
            latestVersionName = currentVersion,
            currentVersionName = currentVersion,
            releaseNotes = "",
            apkDownloadUrl = "",
            publishedAt = ""
        )
    }

    /**
     * Downloads APK using Android's built-in DownloadManager and prompts installation
     */
    fun startDownloadAndInstall(context: Context, apkUrl: String, versionName: String) {
        try {
            if (apkUrl.isBlank()) return

            val uri = Uri.parse(apkUrl)
            val fileName = "HCM_SMS_Update_$versionName.apk"

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri).apply {
                setTitle("Downloading HCM-SMS Update $versionName")
                setDescription("Please wait while the update is downloading...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)

            // Register receiver to prompt install when finished
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctxt.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        installDownloadedApk(ctxt, fileName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download", e)
            // Fallback: Open browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    private fun installDownloadedApk(context: Context, fileName: String) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!file.exists()) return

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
