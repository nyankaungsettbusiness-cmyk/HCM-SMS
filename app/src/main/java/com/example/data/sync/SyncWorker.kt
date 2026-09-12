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
                    Log.e(TAG, "Background sync failed: ${result.message}")
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "Background sync worker was cancelled.")
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during background sync worker: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
