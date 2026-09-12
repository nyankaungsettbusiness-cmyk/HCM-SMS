package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val entityName: String, // e.g. "students", "attendance_records", "student_marks"
    val schoolId: String = "default_school",
    val lastSyncedAt: Long = 0L,
    val lastSyncedVersion: Long = 0L,
    val syncStatus: String = "IDLE",
    val updatedAt: Long = System.currentTimeMillis()
)
