package com.example.data.sync

import android.util.Log
import com.example.data.local.db.AppDatabase
import com.example.data.local.entity.*
import com.example.data.remote.SupabaseClientManager
import com.example.data.sync.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase of synchronization lifecycle.
 */
enum class SyncPhase {
    IDLE,
    CONNECTIVITY_CHECK,
    PUSHING_DATA,
    PULLING_DATA,
    DIAGNOSING_TOMBSTONES,
    VERIFYING_DELETIONS,
    COMPLETED,
    ERROR
}

/**
 * Represents the real-time state of the repository synchronization engine.
 */
data class SyncState(
    val phase: SyncPhase = SyncPhase.IDLE,
    val currentEntity: String? = null,
    val isRunning: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val totalEntitiesPushed: Int = 0,
    val totalEntitiesPulled: Int = 0,
    val totalLocalSoftDeleted: Int = 0,
    val totalLocalDirty: Int = 0,
    val resurrectionRisksDetected: Int = 0,
    val resurrectionAttemptsBlocked: Int = 0,
    val lastErrorMessage: String? = null,
    val phaseDetails: String = "Ready"
)

/**
 * Anomaly details for records where local deletion status conflicts with remote state.
 */
data class DeletionAnomaly(
    val entityType: String,
    val localId: Long,
    val uuid: String,
    val identifier: String,
    val localIsDeleted: Boolean,
    val localIsDirty: Boolean,
    val localUpdatedAt: Long,
    val remoteIsDeleted: Boolean?,
    val remoteUpdatedAt: Long?,
    val anomalyCategory: DeletionAnomalyCategory,
    val rootCauseSummary: String,
    val correctiveAction: String
)

enum class DeletionAnomalyCategory {
    RESURRECTION_VULNERABILITY, // Local isDeleted=1 & isDirty=0, Remote is_deleted=false -> Pull will overwrite local and revive record
    UNSYNCED_TOMBSTONE,        // Local isDeleted=1 & isDirty=1 -> Tombstone has not yet been pushed to Supabase
    REMOTE_SCHEMA_MISMATCH,     // Remote table does not have is_deleted column or returned null
    HARD_DELETE_ORPHAN,         // Remote record exists, but local record was hard-deleted from Room -> Pull inserts it as new
    TIMESTAMP_CONFLICT          // Remote timestamp > Local deletion timestamp, potential concurrent remote update
}

/**
 * Diagnostic report for an individual entity type.
 */
data class EntityDiagnosticReport(
    val entityName: String,
    val localTotal: Int,
    val localActive: Int,
    val localSoftDeleted: Int,
    val localDirty: Int,
    val localMissingUuid: Int,
    val remoteTotal: Int,
    val remoteActive: Int,
    val remoteDeleted: Int,
    val resurrectionRisks: Int,
    val anomalies: List<DeletionAnomaly>
)

/**
 * Root cause explanation explaining the exact mechanism of deletion re-fetching.
 */
data class SyncRootCauseExplanation(
    val title: String,
    val code: String,
    val explanation: String,
    val impact: String,
    val repositoryMitigation: String
)

/**
 * Comprehensive diagnostic report of the synchronization and deletion tombstone system.
 */
data class SyncDiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val isSupabaseConfigured: Boolean,
    val isOnline: Boolean,
    val currentSyncState: SyncState,
    val entityReports: List<EntityDiagnosticReport>,
    val totalLocalSoftDeleted: Int,
    val totalResurrectionRisks: Int,
    val totalBlockedResurrections: Int,
    val rootCausesIdentified: List<SyncRootCauseExplanation>,
    val diagnosticSummary: String
)

/**
 * Diagnostic utility in the repository layer to log and inspect SyncState,
 * verify deletion tombstones, and detect why soft-deleted records are re-fetched from Supabase.
 */
object SyncDiagnosticUtility {

    private const val TAG = "SyncDiagnostics"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var blockedResurrectionsCounter = 0

    /**
     * Get snapshot of current SyncState.
     */
    fun getCurrentSyncState(): SyncState = _syncState.value

    /**
     * Update current SyncState in the repository layer and emit log.
     */
    fun updateSyncState(
        phase: SyncPhase,
        currentEntity: String? = null,
        isRunning: Boolean = phase !in listOf(SyncPhase.IDLE, SyncPhase.COMPLETED, SyncPhase.ERROR),
        pushedDelta: Int = 0,
        pulledDelta: Int = 0,
        dirtyDelta: Int = 0,
        softDeletedCount: Int? = null,
        resurrectionRisksDelta: Int = 0,
        errorMessage: String? = null,
        details: String? = null
    ) {
        val current = _syncState.value
        val updated = current.copy(
            phase = phase,
            currentEntity = currentEntity ?: current.currentEntity,
            isRunning = isRunning,
            lastSyncTimestamp = if (phase == SyncPhase.COMPLETED) System.currentTimeMillis() else current.lastSyncTimestamp,
            totalEntitiesPushed = current.totalEntitiesPushed + pushedDelta,
            totalEntitiesPulled = current.totalEntitiesPulled + pulledDelta,
            totalLocalSoftDeleted = softDeletedCount ?: current.totalLocalSoftDeleted,
            totalLocalDirty = if (dirtyDelta != 0) (current.totalLocalDirty + dirtyDelta).coerceAtLeast(0) else current.totalLocalDirty,
            resurrectionRisksDetected = current.resurrectionRisksDetected + resurrectionRisksDelta,
            resurrectionAttemptsBlocked = blockedResurrectionsCounter,
            lastErrorMessage = errorMessage ?: if (phase != SyncPhase.ERROR) current.lastErrorMessage else null,
            phaseDetails = details ?: "Phase: $phase${currentEntity?.let { " ($it)" } ?: ""}"
        )
        _syncState.value = updated

        Log.d(TAG, "[SYNC_STATE_TRANSITION] Phase=${updated.phase} | Entity=${updated.currentEntity ?: "None"} | " +
                "Pushed=${updated.totalEntitiesPushed} | Pulled=${updated.totalEntitiesPulled} | " +
                "SoftDeleted=${updated.totalLocalSoftDeleted} | BlockedResurrections=${updated.resurrectionAttemptsBlocked} | " +
                "Details=${updated.phaseDetails}")
    }

    /**
     * Record and log a blocked attempt where Supabase pull would have resurrected a deleted record.
     */
    fun recordResurrectionBlocked(
        entityType: String,
        uuid: String,
        identifier: String,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long
    ) {
        blockedResurrectionsCounter++
        val localDateStr = dateFormat.format(Date(localUpdatedAt))
        val remoteDateStr = dateFormat.format(Date(remoteUpdatedAt))
        
        Log.w(
            TAG,
            """
            ================================================================================
            [DELETION TOMBSTONE AUDIT: RESURRECTION ATTEMPT BLOCKED]
            Entity Type      : $entityType
            Identifier       : $identifier (UUID: $uuid)
            Local State      : isDeleted=true, updatedAt=$localDateStr ($localUpdatedAt ms)
            Remote State     : is_deleted=false, updatedAt=$remoteDateStr ($remoteUpdatedAt ms)
            Root Cause       : Remote Supabase row was not marked deleted or returned is_deleted=false,
                               which would have overwritten the local deletion tombstone during pull.
            Action Taken     : Blocked overwriting local tombstone. Retained isDeleted=true in SQLite.
            Total Blocked    : $blockedResurrectionsCounter
            ================================================================================
            """.trimIndent()
        )

        updateSyncState(
            phase = _syncState.value.phase,
            resurrectionRisksDelta = 1,
            details = "Blocked resurrection of $entityType: $identifier"
        )
    }

    /**
     * Run a comprehensive diagnostic audit across Room database and Supabase.
     */
    suspend fun runFullDiagnosticAudit(
        db: AppDatabase,
        client: SupabaseClient? = SupabaseClientManager.getInstance()
    ): SyncDiagnosticReport {
        Log.i(TAG, "[SYNC_DIAGNOSTICS] Starting full diagnostic audit of SyncState and Deletion Tombstones...")
        
        val isConfigured = SupabaseClientManager.isConfigured()
        val isOnline = client != null && try {
            client.from("students").select {
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                limit(1)
            }
            true
        } catch (e: Exception) {
            false
        }

        val entityReports = mutableListOf<EntityDiagnosticReport>()

        // 1. Audit Students
        entityReports.add(auditStudents(db, client))

        // 2. Audit Assessments
        entityReports.add(auditAssessments(db, client))

        // 3. Audit Teachers
        entityReports.add(auditTeachers(db, client))

        // 4. Audit Student Marks
        entityReports.add(auditMarks(db, client))

        val totalSoftDeleted = entityReports.sumOf { it.localSoftDeleted }
        val totalRisks = entityReports.sumOf { it.resurrectionRisks }

        val rootCauses = buildRootCausesList(entityReports)

        val report = SyncDiagnosticReport(
            timestamp = System.currentTimeMillis(),
            isSupabaseConfigured = isConfigured,
            isOnline = isOnline,
            currentSyncState = _syncState.value.copy(
                totalLocalSoftDeleted = totalSoftDeleted,
                resurrectionRisksDetected = totalRisks,
                resurrectionAttemptsBlocked = blockedResurrectionsCounter
            ),
            entityReports = entityReports,
            totalLocalSoftDeleted = totalSoftDeleted,
            totalResurrectionRisks = totalRisks,
            totalBlockedResurrections = blockedResurrectionsCounter,
            rootCausesIdentified = rootCauses,
            diagnosticSummary = "Diagnostic completed: $totalSoftDeleted soft-deleted local records audited, $totalRisks resurrection risks analyzed, $blockedResurrectionsCounter resurrection attempts blocked."
        )

        logDiagnosticReport(report)
        return report
    }

    private suspend fun auditStudents(db: AppDatabase, client: SupabaseClient?): EntityDiagnosticReport {
        val studentDao = db.studentDao()
        val allLocal = studentDao.getAllStudentsIncludingDeleted()
        val activeLocal = allLocal.filter { !it.isDeleted }
        val deletedLocal = allLocal.filter { it.isDeleted }
        val dirtyLocal = allLocal.filter { it.isDirty }
        val missingUuidLocal = allLocal.filter { it.uuid.isBlank() }

        val remoteList = if (client != null) {
            try {
                client.from("students").select().decodeList<StudentSupabaseDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not query remote students for diagnostics: ${e.message}")
                emptyList()
            }
        } else emptyList()

        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByCode = remoteList.filter { it.studentId.isNotBlank() }.associateBy { it.studentId }

        val anomalies = mutableListOf<DeletionAnomaly>()
        for (localDeleted in deletedLocal) {
            val remote = remoteByUuid[localDeleted.uuid] ?: remoteByCode[localDeleted.studentCode]
            if (remote != null) {
                val remoteIsDeleted = remote.isDeleted
                val remoteUpdatedAtMillis = parseIsoToMillis(remote.updatedAt)
                
                if (!remoteIsDeleted) {
                    // This is a prime resurrection vulnerability!
                    anomalies.add(
                        DeletionAnomaly(
                            entityType = "Student",
                            localId = localDeleted.id,
                            uuid = localDeleted.uuid,
                            identifier = "${localDeleted.name} (${localDeleted.studentCode})",
                            localIsDeleted = true,
                            localIsDirty = localDeleted.isDirty,
                            localUpdatedAt = localDeleted.updatedAt,
                            remoteIsDeleted = false,
                            remoteUpdatedAt = remoteUpdatedAtMillis,
                            anomalyCategory = if (!localDeleted.isDirty) 
                                DeletionAnomalyCategory.RESURRECTION_VULNERABILITY 
                            else 
                                DeletionAnomalyCategory.UNSYNCED_TOMBSTONE,
                            rootCauseSummary = "Local student is soft-deleted (isDeleted=true), but Supabase remote record has is_deleted=false. " +
                                    if (!localDeleted.isDirty) "Local isDirty=0 (marked clean), so standard pull would overwrite local tombstone with active remote state!"
                                    else "Local isDirty=1, awaiting push sync to upload tombstone to Supabase.",
                            correctiveAction = "Tombstone protection must block pull override; push sync must send is_deleted=true to Supabase."
                        )
                    )
                }
            }
        }

        return EntityDiagnosticReport(
            entityName = "Students",
            localTotal = allLocal.size,
            localActive = activeLocal.size,
            localSoftDeleted = deletedLocal.size,
            localDirty = dirtyLocal.size,
            localMissingUuid = missingUuidLocal.size,
            remoteTotal = remoteList.size,
            remoteActive = remoteList.count { !it.isDeleted },
            remoteDeleted = remoteList.count { it.isDeleted },
            resurrectionRisks = anomalies.count { it.anomalyCategory == DeletionAnomalyCategory.RESURRECTION_VULNERABILITY },
            anomalies = anomalies
        )
    }

    private suspend fun auditAssessments(db: AppDatabase, client: SupabaseClient?): EntityDiagnosticReport {
        val assessmentDao = db.assessmentDao()
        val allLocal = assessmentDao.getAllAssessmentsIncludingDeleted()
        val activeLocal = allLocal.filter { !it.isDeleted }
        val deletedLocal = allLocal.filter { it.isDeleted }
        val dirtyLocal = allLocal.filter { it.isDirty }
        val missingUuidLocal = allLocal.filter { it.uuid.isBlank() }

        val remoteList = if (client != null) {
            try {
                client.from("assessments").select().decodeList<AssessmentSupabaseDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not query remote assessments for diagnostics: ${e.message}")
                emptyList()
            }
        } else emptyList()

        val remoteByUuid = remoteList.associateBy { it.uuid }

        val anomalies = mutableListOf<DeletionAnomaly>()
        for (localDeleted in deletedLocal) {
            val remote = remoteByUuid[localDeleted.uuid]
            if (remote != null) {
                val remoteIsDeleted = remote.isDeleted
                val remoteUpdatedAtMillis = parseIsoToMillis(remote.updatedAt)
                
                if (!remoteIsDeleted) {
                    anomalies.add(
                        DeletionAnomaly(
                            entityType = "Assessment",
                            localId = localDeleted.id,
                            uuid = localDeleted.uuid,
                            identifier = "${localDeleted.assessmentName} (${localDeleted.grade}/${localDeleted.subjectName})",
                            localIsDeleted = true,
                            localIsDirty = localDeleted.isDirty,
                            localUpdatedAt = localDeleted.updatedAt,
                            remoteIsDeleted = false,
                            remoteUpdatedAt = remoteUpdatedAtMillis,
                            anomalyCategory = if (!localDeleted.isDirty) 
                                DeletionAnomalyCategory.RESURRECTION_VULNERABILITY 
                            else 
                                DeletionAnomalyCategory.UNSYNCED_TOMBSTONE,
                            rootCauseSummary = "Local assessment is soft-deleted (isDeleted=true), but Supabase remote record has is_deleted=false. " +
                                    if (!localDeleted.isDirty) "Local isDirty=0, so normal pull would overwrite local tombstone and revive assessment!"
                                    else "Local isDirty=1, needs push to update remote.",
                            correctiveAction = "Tombstone protection in pull loop must block resurrection and enforce local deletion precedence."
                        )
                    )
                }
            }
        }

        return EntityDiagnosticReport(
            entityName = "Assessments",
            localTotal = allLocal.size,
            localActive = activeLocal.size,
            localSoftDeleted = deletedLocal.size,
            localDirty = dirtyLocal.size,
            localMissingUuid = missingUuidLocal.size,
            remoteTotal = remoteList.size,
            remoteActive = remoteList.count { !it.isDeleted },
            remoteDeleted = remoteList.count { it.isDeleted },
            resurrectionRisks = anomalies.count { it.anomalyCategory == DeletionAnomalyCategory.RESURRECTION_VULNERABILITY },
            anomalies = anomalies
        )
    }

    private suspend fun auditTeachers(db: AppDatabase, client: SupabaseClient?): EntityDiagnosticReport {
        val teacherDao = db.teacherDao()
        val allLocal = teacherDao.getAllTeachersIncludingDeleted()
        val activeLocal = allLocal.filter { !it.isDeleted }
        val deletedLocal = allLocal.filter { it.isDeleted }
        val dirtyLocal = allLocal.filter { it.isDirty }
        val missingUuidLocal = allLocal.filter { it.uuid.isBlank() }

        val remoteList = if (client != null) {
            try {
                client.from("teachers").select().decodeList<TeacherSupabaseDto>()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val remoteByUuid = remoteList.associateBy { it.uuid }
        val anomalies = mutableListOf<DeletionAnomaly>()

        for (localDeleted in deletedLocal) {
            val remote = remoteByUuid[localDeleted.uuid]
            if (remote != null && !remote.isDeleted) {
                anomalies.add(
                    DeletionAnomaly(
                        entityType = "Teacher",
                        localId = localDeleted.id,
                        uuid = localDeleted.uuid,
                        identifier = "${localDeleted.fullName} (${localDeleted.teacherCode})",
                        localIsDeleted = true,
                        localIsDirty = localDeleted.isDirty,
                        localUpdatedAt = localDeleted.updatedAt,
                        remoteIsDeleted = false,
                        remoteUpdatedAt = parseIsoToMillis(remote.updatedAt),
                        anomalyCategory = if (!localDeleted.isDirty)
                            DeletionAnomalyCategory.RESURRECTION_VULNERABILITY
                        else
                            DeletionAnomalyCategory.UNSYNCED_TOMBSTONE,
                        rootCauseSummary = "Local teacher is soft-deleted, but remote Supabase row has is_deleted=false.",
                        correctiveAction = "Enforce tombstone precedence during sync pull."
                    )
                )
            }
        }

        return EntityDiagnosticReport(
            entityName = "Teachers",
            localTotal = allLocal.size,
            localActive = activeLocal.size,
            localSoftDeleted = deletedLocal.size,
            localDirty = dirtyLocal.size,
            localMissingUuid = missingUuidLocal.size,
            remoteTotal = remoteList.size,
            remoteActive = remoteList.count { !it.isDeleted },
            remoteDeleted = remoteList.count { it.isDeleted },
            resurrectionRisks = anomalies.count { it.anomalyCategory == DeletionAnomalyCategory.RESURRECTION_VULNERABILITY },
            anomalies = anomalies
        )
    }

    private suspend fun auditMarks(db: AppDatabase, client: SupabaseClient?): EntityDiagnosticReport {
        val marksDao = db.marksDao()
        val allLocal = marksDao.getStudentMarksForSync(limit = 1000)
        val deletedLocal = allLocal.filter { it.isDeleted }
        val dirtyLocal = allLocal.filter { it.isDirty }

        return EntityDiagnosticReport(
            entityName = "Student Marks",
            localTotal = allLocal.size,
            localActive = allLocal.count { !it.isDeleted },
            localSoftDeleted = deletedLocal.size,
            localDirty = dirtyLocal.size,
            localMissingUuid = allLocal.count { it.uuid.isBlank() },
            remoteTotal = 0,
            remoteActive = 0,
            remoteDeleted = 0,
            resurrectionRisks = 0,
            anomalies = emptyList()
        )
    }

    private fun buildRootCausesList(reports: List<EntityDiagnosticReport>): List<SyncRootCauseExplanation> {
        val list = mutableListOf<SyncRootCauseExplanation>()

        list.add(
            SyncRootCauseExplanation(
                title = "1. Premature isDirty Reset & Pull Overwrite",
                code = "PREMATURE_DIRTY_RESET",
                explanation = "When a local record is soft-deleted (isDeleted=true, isDirty=true), push sends the update to Supabase and immediately clears the local dirty flag (isDirty=0). If Supabase did not persist the is_deleted flag (or remote table schema lacks the column), the subsequent pull fetches the remote row with is_deleted=false. Because !local.isDirty is true, the pull handler blindly updates the local database, overwriting isDeleted=true with isDeleted=false and resurrecting the record.",
                impact = "Soft-deleted records reappear in the UI after synchronization.",
                repositoryMitigation = "Added strict Deletion Tombstone Validation in SyncRepository pull methods: If existingLocal.isDeleted == true and remoteDto.isDeleted == false, the local tombstone is preserved unless remoteUpdatedAt is strictly greater than local deletion timestamp."
            )
        )

        list.add(
            SyncRootCauseExplanation(
                title = "2. Remote Soft-Delete Flag Omission",
                code = "REMOTE_FLAG_OMISSION",
                explanation = "Remote Supabase tables may have rows where is_deleted is NULL or FALSE because either the table schema does not enforce default false, or previous manual edits in Supabase dashboard bypassed soft delete flags. In JSON deserialization, null or absent is_deleted fields default to false.",
                impact = "Remote pull queries treat all returned rows as active entities.",
                repositoryMitigation = "SyncRepository now maps and preserves local deletion tombstones during bidirectional synchronization."
            )
        )

        list.add(
            SyncRootCauseExplanation(
                title = "3. Hard Delete Orphaned Records",
                code = "HARD_DELETE_ORPHAN",
                explanation = "If a record is hard-deleted locally (DELETE FROM table) instead of soft-deleted (UPDATE SET isDeleted=1), Room loses all tombstone history for that UUID. On the next sync pull, Supabase still has the record, and Room treats it as a brand-new insert.",
                impact = "Hard-deleted items are immediately restored by sync pull.",
                repositoryMitigation = "All repository delete operations (StudentRepository, AssessmentRepository, TeacherRepository) utilize soft-deletion tombstones (isDeleted=1, isDirty=1, updatedAt=now) and propagate deletions to Supabase."
            )
        )

        return list
    }

    /**
     * Print the complete diagnostic audit to Android Logcat.
     */
    fun logDiagnosticReport(report: SyncDiagnosticReport, tag: String = TAG) {
        val sb = StringBuilder()
        sb.appendLine("=========================================================================================")
        sb.appendLine("                       SYNC REPOSITORY DIAGNOSTIC AUDIT REPORT                            ")
        sb.appendLine("=========================================================================================")
        sb.appendLine("Audit Timestamp        : ${dateFormat.format(Date(report.timestamp))}")
        sb.appendLine("Supabase Configured    : ${report.isSupabaseConfigured}")
        sb.appendLine("Online Connectivity    : ${report.isOnline}")
        sb.appendLine("Current Sync State     : Phase=${report.currentSyncState.phase}, Entity=${report.currentSyncState.currentEntity ?: "None"}")
        sb.appendLine("Total Local Soft-Deleted: ${report.totalLocalSoftDeleted}")
        sb.appendLine("Resurrection Risks     : ${report.totalResurrectionRisks}")
        sb.appendLine("Resurrections Blocked  : ${report.totalBlockedResurrections}")
        sb.appendLine("-----------------------------------------------------------------------------------------")
        sb.appendLine("ENTITY BREAKDOWN:")
        for (entity in report.entityReports) {
            sb.appendLine(
                "  - %-16s | Local Total: %-4d | Active: %-4d | Soft-Deleted: %-4d | Dirty: %-4d | Risks: %-3d".format(
                    entity.entityName,
                    entity.localTotal,
                    entity.localActive,
                    entity.localSoftDeleted,
                    entity.localDirty,
                    entity.resurrectionRisks
                )
            )
        }
        sb.appendLine("-----------------------------------------------------------------------------------------")
        sb.appendLine("IDENTIFIED ROOT CAUSES FOR DELETED RECORD RE-FETCHING:")
        for (cause in report.rootCausesIdentified) {
            sb.appendLine("  [${cause.code}] ${cause.title}")
            sb.appendLine("    Mechanism : ${cause.explanation}")
            sb.appendLine("    Mitigation: ${cause.repositoryMitigation}")
        }
        
        if (report.entityReports.any { it.anomalies.isNotEmpty() }) {
            sb.appendLine("-----------------------------------------------------------------------------------------")
            sb.appendLine("SPECIFIC DELETION TOMBSTONE ANOMALIES DETECTED:")
            for (entity in report.entityReports) {
                for (anomaly in entity.anomalies) {
                    sb.appendLine("  * [${anomaly.entityType}] ID=${anomaly.localId} | UUID=${anomaly.uuid} | Name=${anomaly.identifier}")
                    sb.appendLine("    Category   : ${anomaly.anomalyCategory}")
                    sb.appendLine("    Root Cause : ${anomaly.rootCauseSummary}")
                    sb.appendLine("    Action     : ${anomaly.correctiveAction}")
                }
            }
        }
        sb.appendLine("=========================================================================================")

        Log.i(tag, sb.toString())
    }

    /**
     * Proactively fixes deletion tombstones by marking all soft-deleted records as dirty
     * and pushing them to Supabase to synchronize tombstone state.
     */
    suspend fun repairDeletionTombstones(
        db: AppDatabase,
        client: SupabaseClient? = SupabaseClientManager.getInstance()
    ): Int {
        var repairedCount = 0
        Log.i(TAG, "[REPAIR_TOMBSTONES] Starting deletion tombstone repair...")

        val studentDao = db.studentDao()
        val deletedStudents = studentDao.getSoftDeletedStudents()
        for (student in deletedStudents) {
            if (!student.isDirty) {
                studentDao.updateStudent(student.copy(isDirty = true, updatedAt = System.currentTimeMillis()))
                repairedCount++
            }
        }

        val assessmentDao = db.assessmentDao()
        val deletedAssessments = assessmentDao.getSoftDeletedAssessments()
        for (assessment in deletedAssessments) {
            if (!assessment.isDirty) {
                assessmentDao.updateAssessment(assessment.copy(isDirty = true, updatedAt = System.currentTimeMillis()))
                repairedCount++
            }
        }

        val teacherDao = db.teacherDao()
        val deletedTeachers = teacherDao.getSoftDeletedTeachers()
        for (teacher in deletedTeachers) {
            if (!teacher.isDirty) {
                teacherDao.updateTeacher(teacher.copy(isDirty = true, updatedAt = System.currentTimeMillis()))
                repairedCount++
            }
        }

        val userDao = db.userDao()
        val allUsers = userDao.getAllUsersList()
        for (user in allUsers.filter { it.isDeleted }) {
            if (!user.isDirty) {
                userDao.updateUser(user.copy(isDirty = true, updatedAt = System.currentTimeMillis()))
                repairedCount++
            }
        }

        Log.i(TAG, "[REPAIR_TOMBSTONES] Marked $repairedCount soft-deleted records as dirty for re-sync push.")
        return repairedCount
    }
}
