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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed class DownloadProgressState {
    object Idle : DownloadProgressState()
    data class Downloading(
        val progress: Float, // 0.0f to 1.0f (-1f if indeterminate)
        val percentage: Int, // 0 to 100 (-1 if indeterminate)
        val downloadedBytes: Long,
        val totalBytes: Long,
        val downloadedMb: String,
        val totalMb: String,
        val statusText: String
    ) : DownloadProgressState()
    data class Installing(val fileName: String) : DownloadProgressState()
    data class Error(val message: String) : DownloadProgressState()
}

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

    // Primary GitHub Repository for releases
    const val DEFAULT_REPO = "nyankaungsettbusiness-cmyk/HCM-SMS"

    // Fallback candidates if the repo was renamed or migrated
    private val REPO_CANDIDATES = listOf(
        DEFAULT_REPO,
        "nyankaungsett-business/HCM-SMS",
        "nyankaungsett-business/hcm-sms"
    )

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var progressPollingJob: Job? = null
    private val _downloadProgress = MutableStateFlow<DownloadProgressState>(DownloadProgressState.Idle)
    val downloadProgress: StateFlow<DownloadProgressState> = _downloadProgress.asStateFlow()

    var activeDownloadId: Long? = null
        private set
    var activeDownloadedFileName: String? = null
        private set

    fun resetDownloadState() {
        progressPollingJob?.cancel()
        progressPollingJob = null
        _downloadProgress.value = DownloadProgressState.Idle
    }

    fun hasUnknownSourcesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.packageManager.canRequestPackageInstalls()
            } catch (e: Exception) {
                true
            }
        } else {
            true
        }
    }

    fun requestUnknownSourcesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Checks GitHub Releases for a newer version than current BuildConfig.VERSION_NAME
     */
    suspend fun checkForUpdates(
        repoSlug: String? = null,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        val reposToCheck = if (!repoSlug.isNullOrBlank()) listOf(repoSlug) else REPO_CANDIDATES

        for (repo in reposToCheck) {
            try {
                val endpoint = "https://api.github.com/repos/$repo/releases/latest"
                val url = URL(endpoint)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "HCM-SMS-Android")
                    connectTimeout = 3500 // Quick timeout to prevent UI freezes / log spam
                    readTimeout = 3500
                    instanceFollowRedirects = true
                }

                val responseCode = try {
                    connection.responseCode
                } catch (timeoutEx: java.net.SocketTimeoutException) {
                    Log.d(TAG, "GitHub API timeout for $repo (Network restricted or offline)")
                    continue
                } catch (ioEx: Exception) {
                    Log.d(TAG, "GitHub API network unavailable for $repo: ${ioEx.message}")
                    continue
                }

                if (responseCode == 200) {
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

                    if (isNewer && apkUrl.isNotBlank()) {
                        return@withContext AppUpdateInfo(
                            hasUpdate = true,
                            latestVersionName = cleanTagName.ifBlank { tagName },
                            currentVersionName = currentVersion,
                            releaseNotes = releaseNotes,
                            apkDownloadUrl = apkUrl,
                            publishedAt = publishedAt
                        )
                    } else {
                        // Found valid latest release for this repo and already up to date, no need to check further candidates
                        break
                    }
                } else if (responseCode == 404) {
                    Log.d(TAG, "No public release found for repo: $repo (HTTP 404)")
                } else {
                    Log.w(TAG, "GitHub API for $repo returned HTTP $responseCode")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to check for updates from $repo: ${e.message}")
            }
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
     * Downloads APK using Android's built-in DownloadManager and prompts installation.
     * If already downloaded, prompts installation directly.
     */
    fun startDownloadAndInstall(context: Context, apkUrl: String, versionName: String) {
        try {
            if (apkUrl.isBlank()) return

            // Proactively verify / prompt Unknown App Sources permission (Android 8.0+)
            if (!hasUnknownSourcesPermission(context)) {
                requestUnknownSourcesPermission(context)
                try {
                    android.widget.Toast.makeText(
                        context,
                        "ကျေးဇူးပြု၍ HCM-SMS အတွက် 'Allow from this source' (ပြင်ပ App သွင်းခွင့်) ခွင့်ပြုပေးပါ",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
            }

            val fileName = "HCM_SMS_v${versionName.replace(' ', '_')}.apk"
            activeDownloadedFileName = fileName
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val targetFile = if (downloadsDir != null) File(downloadsDir, fileName) else null
            if (targetFile != null && targetFile.exists() && targetFile.length() > 1024 * 1024) {
                // If the APK was already downloaded, report complete and install directly
                val fileSizeMb = String.format(Locale.US, "%.1f MB", targetFile.length() / (1024.0 * 1024.0))
                _downloadProgress.value = DownloadProgressState.Downloading(
                    progress = 1f,
                    percentage = 100,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    downloadedMb = fileSizeMb,
                    totalMb = fileSizeMb,
                    statusText = "APK ready in cache. Launching installer..."
                )
                _downloadProgress.value = DownloadProgressState.Installing(fileName)
                installDownloadedApk(context, fileName)
                return
            }

            _downloadProgress.value = DownloadProgressState.Downloading(
                progress = -1f,
                percentage = -1,
                downloadedBytes = 0,
                totalBytes = 0,
                downloadedMb = "0.0 MB",
                totalMb = "-- MB",
                statusText = "Connecting to update server..."
            )

            val uri = Uri.parse(apkUrl)
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri).apply {
                setTitle("HCM-SMS Update $versionName")
                setDescription("Downloading latest application APK...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            activeDownloadId = downloadId

            // Start polling progress every 250ms
            progressPollingJob?.cancel()
            progressPollingJob = coroutineScope.launch {
                var isDone = false
                while (!isDone && isActive) {
                    delay(250)
                    try {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                val bytesSoFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                                when (status) {
                                    DownloadManager.STATUS_RUNNING -> {
                                        val pct = if (totalBytes > 0) {
                                            ((bytesSoFar.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
                                        } else {
                                            -1
                                        }
                                        val fraction = if (totalBytes > 0) {
                                            (bytesSoFar.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                        } else {
                                            -1f
                                        }
                                        val dlMb = String.format(Locale.US, "%.1f MB", bytesSoFar / (1024.0 * 1024.0))
                                        val totMb = if (totalBytes > 0) {
                                            String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0))
                                        } else {
                                            "-- MB"
                                        }
                                        _downloadProgress.value = DownloadProgressState.Downloading(
                                            progress = fraction,
                                            percentage = pct,
                                            downloadedBytes = bytesSoFar,
                                            totalBytes = totalBytes,
                                            downloadedMb = dlMb,
                                            totalMb = totMb,
                                            statusText = if (pct >= 0) "Downloading... $pct%" else "Downloading... ($dlMb)"
                                        )
                                    }
                                    DownloadManager.STATUS_PENDING -> {
                                        _downloadProgress.value = DownloadProgressState.Downloading(
                                            progress = -1f,
                                            percentage = -1,
                                            downloadedBytes = 0,
                                            totalBytes = totalBytes,
                                            downloadedMb = "0.0 MB",
                                            totalMb = if (totalBytes > 0) String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0)) else "--",
                                            statusText = "Waiting for network connection..."
                                        )
                                    }
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        isDone = true
                                        val finalSize = if (totalBytes > 0) totalBytes else bytesSoFar
                                        val finalMb = String.format(Locale.US, "%.1f MB", finalSize / (1024.0 * 1024.0))
                                        _downloadProgress.value = DownloadProgressState.Downloading(
                                            progress = 1f,
                                            percentage = 100,
                                            downloadedBytes = finalSize,
                                            totalBytes = finalSize,
                                            downloadedMb = finalMb,
                                            totalMb = finalMb,
                                            statusText = "Download complete (100%). Launching installer..."
                                        )
                                        _downloadProgress.value = DownloadProgressState.Installing(fileName)
                                        delay(500)
                                        installDownloadedApk(context, fileName, downloadId)
                                    }
                                    DownloadManager.STATUS_PAUSED -> {
                                        _downloadProgress.value = DownloadProgressState.Downloading(
                                            progress = if (totalBytes > 0) (bytesSoFar.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else -1f,
                                            percentage = if (totalBytes > 0) ((bytesSoFar.toDouble() / totalBytes.toDouble()) * 100).toInt() else -1,
                                            downloadedBytes = bytesSoFar,
                                            totalBytes = totalBytes,
                                            downloadedMb = String.format(Locale.US, "%.1f MB", bytesSoFar / (1024.0 * 1024.0)),
                                            totalMb = if (totalBytes > 0) String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0)) else "--",
                                            statusText = "Download paused (waiting for WiFi/network)..."
                                        )
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        isDone = true
                                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                        _downloadProgress.value = DownloadProgressState.Error(
                                            "Download interrupted (code $reason). You can retry or open in browser."
                                        )
                                    }
                                }
                            }
                            cursor.close()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error querying download progress", e)
                    }
                }
            }

            // Register receiver to prompt install when finished
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctxt.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        _downloadProgress.value = DownloadProgressState.Installing(fileName)
                        installDownloadedApk(ctxt, fileName, downloadId)
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
            _downloadProgress.value = DownloadProgressState.Error(
                "Failed to start download: ${e.message ?: "Unknown error"}. Opening browser fallback..."
            )
            // Fallback: Open browser directly
            openApkInBrowser(context, apkUrl)
        }
    }

    fun openApkInBrowser(context: Context, apkUrl: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
        }
    }

    fun installDownloadedApk(context: Context, fileName: String? = null, downloadId: Long? = null) {
        try {
            // 1. Verify and request Unknown App Sources permission if needed
            if (!hasUnknownSourcesPermission(context)) {
                try {
                    android.widget.Toast.makeText(
                        context,
                        "Installer ဖွင့်ရန် 'Allow from this source' (ပြင်ပ App သွင်းခွင့်) ခွင့်ပြုပေးပါ",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
                requestUnknownSourcesPermission(context)
                return
            }

            val effectiveDownloadId = downloadId ?: activeDownloadId
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            var targetFile: File? = null

            // 2a. Query DownloadManager cursor for the exact downloaded local file
            if (effectiveDownloadId != null && effectiveDownloadId > 0 && downloadManager != null) {
                try {
                    val query = DownloadManager.Query().setFilterById(effectiveDownloadId)
                    downloadManager.query(query)?.use { c ->
                        if (c.moveToFirst()) {
                            val localUriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (localUriIdx >= 0) {
                                val localUriStr = c.getString(localUriIdx)
                                if (!localUriStr.isNullOrBlank()) {
                                    val parsed = Uri.parse(localUriStr)
                                    val f = if (parsed.scheme == "file") File(parsed.path ?: "") else null
                                    if (f != null && f.exists() && f.length() > 1024 * 50) {
                                        targetFile = f
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error resolving local file from DownloadManager cursor: ${e.message}")
                }
            }

            // 2b. If not found via cursor, check downloadsDir with specified name
            val nameToLookFor = fileName ?: activeDownloadedFileName
            if (targetFile == null && !nameToLookFor.isNullOrBlank() && downloadsDir != null) {
                val candidate = File(downloadsDir, nameToLookFor)
                if (candidate.exists() && candidate.length() > 1024 * 50) {
                    targetFile = candidate
                }
            }

            // 2c. Fallback: Check newest .apk in downloads folder
            if (targetFile == null && downloadsDir != null) {
                val apkFiles = downloadsDir.listFiles { f ->
                    f.isFile && f.name.endsWith(".apk", ignoreCase = true) && f.length() > 1024 * 50
                }
                targetFile = apkFiles?.maxByOrNull { it.lastModified() }
            }

            // 2d. Fallback: Stream directly from DownloadManager PFD into cache
            val cacheApk = File(context.cacheDir, "update_installer.apk")
            if (targetFile == null && effectiveDownloadId != null && effectiveDownloadId > 0 && downloadManager != null) {
                try {
                    downloadManager.openDownloadedFile(effectiveDownloadId)?.use { pfd ->
                        java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                            java.io.FileOutputStream(cacheApk).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    if (cacheApk.exists() && cacheApk.length() > 1024 * 50) {
                        targetFile = cacheApk
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stream APK from DownloadManager PFD: ${e.message}")
                }
            }

            // 3. Prepare pristine install file in internal cache to ensure no storage isolation or permission barriers
            val fileToInstall: File? = if (targetFile != null && targetFile.exists()) {
                try {
                    if (targetFile != cacheApk) {
                        targetFile.copyTo(cacheApk, overwrite = true)
                        if (cacheApk.exists() && cacheApk.length() == targetFile.length()) cacheApk else targetFile
                    } else {
                        cacheApk
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cache copy note: ${e.message}, using original file")
                    targetFile
                }
            } else {
                null
            }

            // 4. Construct FileProvider URI (DO NOT use raw dmUri which causes permission denial in PackageInstaller)
            var installUri: Uri? = null
            if (fileToInstall != null && fileToInstall.exists()) {
                try {
                    installUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        fileToInstall
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "FileProvider error: ${e.message}")
                }
            }

            // Extreme fallback if FileProvider fails
            if (installUri == null && effectiveDownloadId != null && effectiveDownloadId > 0 && downloadManager != null) {
                try {
                    installUri = downloadManager.getUriForDownloadedFile(effectiveDownloadId)
                } catch (_: Exception) {}
            }

            if (installUri == null) {
                Log.w(TAG, "No valid APK URI or file found to install")
                try {
                    android.widget.Toast.makeText(
                        context,
                        "APK ဖိုင် မတွေ့ရှိသေးပါ (သို့မဟုတ် ဒေါင်းလုဒ်မပြီးသေးပါ)။ ကျေးဇူးပြု၍ ဒေါင်းလုဒ်ပြီးဆုံးအောင် စောင့်ပါ",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
                return
            }

            // 5. Construct Package Installer Intent
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(installUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // 6. Explicitly grant URI read permissions to all installer packages
            val resolvedActivities = try {
                context.packageManager.queryIntentActivities(installIntent, 0)
            } catch (e: Exception) {
                emptyList()
            }

            for (resolveInfo in resolvedActivities) {
                val packageName = resolveInfo.activityInfo.packageName
                try {
                    context.grantUriPermission(packageName, installUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    Log.w(TAG, "grantUriPermission for $packageName: ${e.message}")
                }
            }

            // Also explicitly grant to standard Android package installers
            listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.android.shell"
            ).forEach { pkg ->
                try {
                    context.grantUriPermission(pkg, installUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            try {
                android.widget.Toast.makeText(
                    context,
                    "Installer တိုက်ရိုက်မပွင့်ပါက Notification Bar သို့မဟုတ် Files app မှ ထည့်သွင်းနိုင်ပါသည်",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {}
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
