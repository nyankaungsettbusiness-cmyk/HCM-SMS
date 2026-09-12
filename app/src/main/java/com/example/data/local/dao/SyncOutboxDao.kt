package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.OutboxStatus
import com.example.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingOutbox(limit: Int = 100): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status IN ('PENDING', 'SYNCING', 'FAILED')")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status IN ('PENDING', 'SYNCING', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: SyncOutboxEntity): Long

    @Update
    suspend fun update(entry: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = :newStatus, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: OutboxStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sync_outbox SET status = 'FAILED', lastError = :error, retryCount = retryCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markFailed(id: Long, error: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_outbox WHERE status = 'COMPLETED' OR (status = 'PENDING' AND entityUuid = :uuid AND entityType = :type)")
    suspend fun pruneOutdated(type: String, uuid: String)

    @Query("DELETE FROM sync_outbox WHERE createdAt < :olderThanTimestamp")
    suspend fun cleanupOldEntries(olderThanTimestamp: Long)
}
