package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class OutboxOperation {
    CREATE,
    UPDATE,
    DELETE
}

enum class OutboxStatus {
    PENDING,
    SYNCING,
    FAILED,
    COMPLETED
}

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entityType", "entityUuid"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String, // e.g. "attendance", "student", "student_mark", "payment", "teacher"
    val entityUuid: String,
    val operation: OutboxOperation,
    val payloadJson: String = "{}",
    val schoolId: String = "default_school",
    val version: Long = 1,
    val retryCount: Int = 0,
    val status: OutboxStatus = OutboxStatus.PENDING,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
