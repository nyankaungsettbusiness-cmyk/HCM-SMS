package com.example.data.sync

/**
 * Result model representing the server verification status of a deletion
 * operation on Supabase Cloud.
 */
sealed class DeletionVerificationResult {
    abstract val tableName: String
    abstract val identifier: String

    /**
     * Confirmed: The row was physically removed/purged from Supabase Cloud table.
     */
    data class VerifiedPurged(
        override val tableName: String,
        override val identifier: String,
        val message: String = "Row permanently removed from Supabase."
    ) : DeletionVerificationResult()

    /**
     * Confirmed: The row exists on Supabase with is_deleted = true (tombstone verified).
     */
    data class VerifiedTombstoned(
        override val tableName: String,
        override val identifier: String,
        val message: String = "Record soft-delete (is_deleted = true) confirmed on Supabase."
    ) : DeletionVerificationResult()

    /**
     * Warning / Error: Supabase server rejected or ignored the delete/tombstone request.
     */
    data class ServerRejectedOrIgnored(
        override val tableName: String,
        override val identifier: String,
        val reason: String,
        val details: String? = null
    ) : DeletionVerificationResult()

    /**
     * Queued: Device is currently offline or unreachable. Saved locally in Room database
     * and queued for verification once connection is restored.
     */
    data class OfflineQueued(
        override val tableName: String,
        override val identifier: String,
        val message: String = "Offline mode: Deletion saved locally and queued for Supabase sync."
    ) : DeletionVerificationResult()
}
