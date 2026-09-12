package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE entityName = :entityName LIMIT 1")
    suspend fun getMetadata(entityName: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllMetadata(): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata")
    fun observeAllMetadata(): Flow<List<SyncMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: SyncMetadataEntity)

    @Query("UPDATE sync_metadata SET lastSyncedAt = :timestamp, updatedAt = :timestamp WHERE entityName = :entityName")
    suspend fun updateLastSyncedAt(entityName: String, timestamp: Long)

    @Query("UPDATE sync_metadata SET lastSyncedAt = :timestamp, lastSyncedVersion = :version, updatedAt = :timestamp WHERE entityName = :entityName")
    suspend fun updateSyncProgress(entityName: String, timestamp: Long, version: Long)
}
