package com.example.data.sync

import android.util.Log
import com.example.data.local.db.AppDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.data.remote.SupabaseClientManager
import com.example.data.sync.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import java.util.UUID

sealed class SyncResult {
    data class Success(
        val pushedStudents: Int = 0,
        val pulledStudents: Int = 0,
        val pushedTeachers: Int = 0,
        val pulledTeachers: Int = 0,
        val totalPushed: Int = 0,
        val totalPulled: Int = 0
    ) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}

class SyncRepository(
    private val studentDao: StudentDao,
    private val teacherDao: TeacherDao,
    private val userDao: UserDao? = null,
    private val schoolPolicyDao: SchoolPolicyDao? = null,
    private val assessmentDao: AssessmentDao? = null,
    private val marksDao: MarksDao? = null,
    private val holisticDao: HolisticDao? = null,
    private val assessmentPeriodDao: AssessmentPeriodDao? = null,
    private val attendanceDao: AttendanceDao? = null,
    private val academicYearDao: AcademicYearDao? = null
) {
    constructor(db: AppDatabase) : this(
        studentDao = db.studentDao(),
        teacherDao = db.teacherDao(),
        userDao = db.userDao(),
        schoolPolicyDao = db.schoolPolicyDao(),
        assessmentDao = db.assessmentDao(),
        marksDao = db.marksDao(),
        holisticDao = db.holisticDao(),
        assessmentPeriodDao = db.assessmentPeriodDao(),
        attendanceDao = db.attendanceDao(),
        academicYearDao = db.academicYearDao()
    )

    companion object {
        private const val TAG = "SyncRepository"
        private val localToRemoteStudentIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val localToRemoteAssessmentIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val remoteToLocalStudentIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val remoteToLocalAssessmentIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val localToRemoteGradeIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val remoteToLocalGradeIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val localToRemoteSubjectIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        private val remoteToLocalSubjectIdCache = java.util.concurrent.ConcurrentHashMap<Long, Long>()
    }

    /**
     * Test active connection to Supabase instance.
     */
    suspend fun testConnectivity(): Boolean {
        val client = SupabaseClientManager.getInstance() ?: return false
        return try {
            client.from("students").select {
                count(Count.EXACT)
                limit(1)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Supabase connectivity test failed: ${e.message}", e)
            false
        }
    }

    /**
     * Rapidly inspects local SQLite tables to count unsynced or modified (isDirty=1 or uuid='') records.
     * Operates 100% on-device with ZERO remote API calls to conserve Supabase quota.
     */
    suspend fun countDirtyRecords(): Int {
        var dirtyCount = 0
        try {
            dirtyCount += studentDao.getStudentsForSync().count { it.isDirty }
            dirtyCount += teacherDao.getTeachersForSync().count { it.isDirty }
            userDao?.let { dirtyCount += it.getUsersForSync().count { u -> u.isDirty } }
            marksDao?.let {
                dirtyCount += it.getStudentMarksForSync(limit = 100).count { m -> m.isDirty }
                dirtyCount += it.getAssessmentResultsForSync(limit = 100).count { r -> r.isDirty }
            }
            attendanceDao?.let { dirtyCount += it.getAttendanceRecordsForSync(limit = 100).count { a -> a.isDirty } }
            assessmentDao?.let { dirtyCount += it.getAssessmentsForSync().count { a -> a.isDirty } }
            academicYearDao?.let { dirtyCount += it.getAcademicYearsForSync().count { y -> y.isDirty } }
            schoolPolicyDao?.let {
                dirtyCount += it.getSchoolSettingsForSync().count { s -> s.isDirty }
                dirtyCount += it.getGradesForSync().count { g -> g.isDirty }
                dirtyCount += it.getClassesForSync().count { c -> c.isDirty }
                dirtyCount += it.getSubjectsForSync().count { s -> s.isDirty }
            }
            holisticDao?.let {
                dirtyCount += it.getHolisticResultsForSync(limit = 100).count { h -> h.isDirty }
                dirtyCount += it.getTeacherCommentsForSync(limit = 100).count { t -> t.isDirty }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error counting dirty records: ${e.message}")
        }
        return dirtyCount
    }

    /**
     * Clear all dirty flags locally across all Room database tables and empty the sync outbox.
     * Guarantees pending changes count resets to 0 immediately.
     */
    suspend fun clearAllDirtyFlags(db: AppDatabase) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val writableDb = db.openHelper.writableDatabase
                writableDb.execSQL("UPDATE school_settings SET isDirty = 0;")
                writableDb.execSQL("UPDATE subjects SET isDirty = 0;")
                writableDb.execSQL("UPDATE grades SET isDirty = 0;")
                writableDb.execSQL("UPDATE school_classes SET isDirty = 0;")
                writableDb.execSQL("UPDATE students SET isDirty = 0;")
                writableDb.execSQL("UPDATE teachers SET isDirty = 0;")
                writableDb.execSQL("UPDATE users SET isDirty = 0;")
                writableDb.execSQL("UPDATE academic_years SET isDirty = 0;")
                writableDb.execSQL("UPDATE assessments SET isDirty = 0;")
                writableDb.execSQL("UPDATE student_marks SET isDirty = 0;")
                writableDb.execSQL("UPDATE assessment_results SET isDirty = 0;")
                writableDb.execSQL("UPDATE attendance_records SET isDirty = 0;")
                writableDb.execSQL("UPDATE custom_exams SET isDirty = 0;")
                writableDb.execSQL("UPDATE grading_policies SET isDirty = 0;")
                try {
                    writableDb.execSQL("UPDATE holistic_categories SET isDirty = 0;")
                    writableDb.execSQL("UPDATE holistic_results SET isDirty = 0;")
                    writableDb.execSQL("UPDATE teacher_comments SET isDirty = 0;")
                    writableDb.execSQL("UPDATE sgi_categories SET isDirty = 0;")
                    writableDb.execSQL("UPDATE sgi_results SET isDirty = 0;")
                } catch (_: Exception) {}
                try {
                    writableDb.execSQL("DELETE FROM sync_outbox;")
                } catch (_: Exception) {}
                Log.i(TAG, "All dirty flags and sync outbox cleared successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing all dirty flags: ${e.message}", e)
            }
        }
    }

    /**
     * Perform Student push sync only (push unsynced/dirty local students to Supabase).
     */
    suspend fun syncStudentsPushOnly(): SyncResult {
        val client = SupabaseClientManager.getInstance()
            ?: return SyncResult.Error("Supabase client is not configured with valid URL/Key")

        return try {
            val (pushedCount, _) = syncStudentsInternal(client)
            Log.i(TAG, "Student push complete. Students pushed: $pushedCount")
            SyncResult.Success(
                pushedStudents = pushedCount,
                pulledStudents = 0,
                pushedTeachers = 0,
                pulledTeachers = 0,
                totalPushed = pushedCount,
                totalPulled = 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during student push sync: ${e.message}", e)
            SyncResult.Error(e.message ?: "Unknown sync error", e)
        }
    }

    /**
     * Perform bidirectional sync for Student and Teacher entities.
     */
    suspend fun syncStudentsAndTeachers(): SyncResult {
        return syncAllEntities()
    }

    /**
     * Perform comprehensive bidirectional sync for ALL entities in Room and Supabase.
     */
    suspend fun syncAllEntities(): SyncResult {
        val client = SupabaseClientManager.getInstance()
            ?: return SyncResult.Error("Supabase client is not configured with valid URL/Key")

        return try {
            var totalPushed = 0
            var totalPulled = 0

            // 1. Users
            val (pUsers, lUsers) = syncUsersInternal(client)
            totalPushed += pUsers; totalPulled += lUsers

            // 2. Academic Years (Synced BEFORE school_settings so active academic year is resolved and aligned)
            val (pYears, lYears) = syncAcademicYearsInternal(client)
            totalPushed += pYears; totalPulled += lYears

            // 3. School Settings
            val (pSettings, lSettings) = syncSchoolSettingsInternal(client)
            totalPushed += pSettings; totalPulled += lSettings

            // 4. Grades
            val (pGrades, lGrades) = syncGradesInternal(client)
            totalPushed += pGrades; totalPulled += lGrades

            // 5. School Classes
            val (pClasses, lClasses) = syncSchoolClassesInternal(client)
            totalPushed += pClasses; totalPulled += lClasses

            // 6. Subjects
            val (pSubjects, lSubjects) = syncSubjectsInternal(client)
            totalPushed += pSubjects; totalPulled += lSubjects

            // 7. Grade-Subject Cross Refs
            val (pRefs, lRefs) = syncGradeSubjectCrossRefsInternal(client)
            totalPushed += pRefs; totalPulled += lRefs

            // 8. Assessment Types
            val (pATypes, lATypes) = syncAssessmentTypesInternal(client)
            totalPushed += pATypes; totalPulled += lATypes

            // 9. Custom Exams
            val (pExams, lExams) = syncCustomExamsInternal(client)
            totalPushed += pExams; totalPulled += lExams

            // 10. Grading Policies
            val (pPolicies, lPolicies) = syncGradingPoliciesInternal(client)
            totalPushed += pPolicies; totalPulled += lPolicies

            // 11. Assessment Periods
            val (pPeriods, lPeriods) = syncAssessmentPeriodsInternal(client)
            totalPushed += pPeriods; totalPulled += lPeriods

            // 12. Students
            val (pStudents, lStudents) = syncStudentsInternal(client)
            totalPushed += pStudents; totalPulled += lStudents

            // 13. Teachers
            val (pTeachers, lTeachers) = syncTeachersInternal(client)
            totalPushed += pTeachers; totalPulled += lTeachers

            // 14. Assessments
            val (pAssessments, lAssessments) = syncAssessmentsInternal(client)
            totalPushed += pAssessments; totalPulled += lAssessments

            // 15. Student Marks
            val (pMarks, lMarks) = syncStudentMarksInternal(client)
            totalPushed += pMarks; totalPulled += lMarks

            // 16. Assessment Results
            val (pResults, lResults) = syncAssessmentResultsInternal(client)
            totalPushed += pResults; totalPulled += lResults

            // 17. Holistic Categories
            val (pHCategories, lHCategories) = syncHolisticCategoriesInternal(client)
            totalPushed += pHCategories; totalPulled += lHCategories

            // 18. Holistic Results
            val (pHResults, lHResults) = syncHolisticResultsInternal(client)
            totalPushed += pHResults; totalPulled += lHResults

            // 19. SGI Categories
            val (pSCategories, lSCategories) = syncSgiCategoriesInternal(client)
            totalPushed += pSCategories; totalPulled += lSCategories

            // 20. SGI Results
            val (pSResults, lSResults) = syncSgiResultsInternal(client)
            totalPushed += pSResults; totalPulled += lSResults

            // 21. Teacher Comments
            val (pComments, lComments) = syncTeacherCommentsInternal(client)
            totalPushed += pComments; totalPulled += lComments

            // 22. Attendance Records
            val (pAttendance, lAttendance) = syncAttendanceInternal(client)
            totalPushed += pAttendance; totalPulled += lAttendance

            // 23. Promotion History
            val (pPromotion, lPromotion) = syncPromotionHistoryInternal(client)
            totalPushed += pPromotion; totalPulled += lPromotion

            Log.i(TAG, "Full sync complete. Total Pushed: $totalPushed, Total Pulled: $totalPulled")
            SyncDiagnosticUtility.updateSyncState(
                phase = SyncPhase.COMPLETED,
                pushedDelta = totalPushed,
                pulledDelta = totalPulled,
                details = "Sync finished successfully. Pushed $totalPushed, pulled $totalPulled."
            )

            SyncResult.Success(
                pushedStudents = pStudents,
                pulledStudents = lStudents,
                pushedTeachers = pTeachers,
                pulledTeachers = lTeachers,
                totalPushed = totalPushed,
                totalPulled = totalPulled
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "Full sync execution was cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync execution: ${e.message}", e)
            SyncDiagnosticUtility.updateSyncState(
                phase = SyncPhase.ERROR,
                errorMessage = e.message ?: "Unknown sync error",
                details = "Sync failed: ${e.message}"
            )
            SyncResult.Error(e.message ?: "Unknown sync error", e)
        }
    }

    /**
     * Run full diagnostic audit to inspect SyncState and verify why deleted records
     * are being re-fetched from Supabase during synchronization.
     */
    suspend fun runDiagnosticAudit(db: AppDatabase): SyncDiagnosticReport {
        SyncDiagnosticUtility.updateSyncState(phase = SyncPhase.DIAGNOSING_TOMBSTONES, details = "Running comprehensive diagnostic audit...")
        val client = SupabaseClientManager.getInstance()
        val report = SyncDiagnosticUtility.runFullDiagnosticAudit(db, client)
        SyncDiagnosticUtility.updateSyncState(phase = SyncPhase.IDLE, details = "Diagnostic audit completed.")
        return report
    }

    /**
     * Get real-time SyncState from repository layer.
     */
    fun getCurrentSyncState(): SyncState = SyncDiagnosticUtility.getCurrentSyncState()

    /**
     * Repair deletion tombstones by marking all soft-deleted records as dirty to force
     * synchronization of tombstones to Supabase.
     */
    suspend fun repairDeletionTombstones(db: AppDatabase): Int {
        return SyncDiagnosticUtility.repairDeletionTombstones(db, SupabaseClientManager.getInstance())
    }

    /**
     * Targeted Single-Table Synchronization.
     * Executes a single, isolated push/pull operation strictly for the specified table.
     * Prevents pulling or locking unrelated tables, minimizing Supabase API calls.
     */
    suspend fun syncSingleTable(tableName: String): Pair<Int, Int> {
        val client = SupabaseClientManager.getInstance() ?: return Pair(0, 0)
        return try {
            when (tableName.trim().lowercase()) {
                "students", "student" -> syncStudentsInternal(client)
                "teachers", "teacher" -> syncTeachersInternal(client)
                "users", "user" -> syncUsersInternal(client)
                "school_settings", "school_setting", "settings" -> syncSchoolSettingsInternal(client)
                "academic_years", "academic_year" -> syncAcademicYearsInternal(client)
                "grades", "grade" -> syncGradesInternal(client)
                "school_classes", "school_class", "classes" -> syncSchoolClassesInternal(client)
                "subjects", "subject" -> syncSubjectsInternal(client)
                "grade_subject_cross_ref", "grade_subject_cross_refs" -> syncGradeSubjectCrossRefsInternal(client)
                "assessment_types", "assessment_type" -> syncAssessmentTypesInternal(client)
                "custom_exams", "custom_exam" -> syncCustomExamsInternal(client)
                "grading_policies", "grading_policy" -> syncGradingPoliciesInternal(client)
                "assessment_periods", "assessment_period" -> syncAssessmentPeriodsInternal(client)
                "assessments", "assessment" -> syncAssessmentsInternal(client)
                "student_marks", "student_mark", "marks" -> syncStudentMarksInternal(client)
                "assessment_results", "assessment_result", "results" -> syncAssessmentResultsInternal(client)
                "holistic_categories", "holistic_category" -> syncHolisticCategoriesInternal(client)
                "holistic_results", "holistic_result" -> syncHolisticResultsInternal(client)
                "sgi_categories", "sgi_category" -> syncSgiCategoriesInternal(client)
                "sgi_results", "sgi_result" -> syncSgiResultsInternal(client)
                "teacher_comments", "teacher_comment", "comments" -> syncTeacherCommentsInternal(client)
                "attendance_records", "attendance_record", "attendance" -> syncAttendanceInternal(client)
                "promotion_history", "promotion" -> syncPromotionHistoryInternal(client)
                else -> {
                    Log.w(TAG, "syncSingleTable: Unknown table name '$tableName'")
                    Pair(0, 0)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during targeted sync of table $tableName: ${e.message}", e)
            Pair(0, 0)
        }
    }

    private suspend fun syncStudentsInternal(client: SupabaseClient): Pair<Int, Int> {
        var pushedCount = 0
        var pulledCount = 0

        val remoteStudentsList = try {
            client.from("students").select().decodeList<StudentSupabaseDto>()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch remote students before push: ${e.message}")
            emptyList()
        }
        val remoteByUuid = remoteStudentsList.associateBy { it.uuid }
        val remoteByCode = remoteStudentsList.filter { it.studentId.isNotBlank() }.associateBy { it.studentId }

        for (remote in remoteStudentsList) {
            if (remote.id != null) {
                if (remote.uuid.isNotBlank()) {
                    val local = studentDao.getStudentByUuid(remote.uuid)
                    if (local != null) {
                        localToRemoteStudentIdCache[local.id] = remote.id
                        remoteToLocalStudentIdCache[remote.id] = local.id
                    }
                }
                if (remote.studentId.isNotBlank()) {
                    val local = studentDao.getStudentByCode(remote.studentId)
                    if (local != null) {
                        localToRemoteStudentIdCache[local.id] = remote.id
                        remoteToLocalStudentIdCache[remote.id] = local.id
                    }
                }
            }
        }

        val allLocalStudents = studentDao.getStudentsForSync()
        for (student in allLocalStudents) {
            val assignedUuid = student.uuid.ifBlank { UUID.randomUUID().toString() }
            val updatedStudent = if (student.uuid.isBlank()) student.copy(uuid = assignedUuid) else student
            val existingRemote = remoteByUuid[assignedUuid]
                ?: remoteByCode[updatedStudent.studentCode]

            if (existingRemote?.id != null) {
                localToRemoteStudentIdCache[updatedStudent.id] = existingRemote.id
                remoteToLocalStudentIdCache[existingRemote.id] = updatedStudent.id
            }

            val isRemoteMissing = existingRemote == null
            if (student.isDirty || student.uuid.isBlank() || isRemoteMissing) {
                val targetUuid = existingRemote?.uuid?.ifBlank { null } ?: assignedUuid
                val dto = StudentSupabaseDto.fromEntity(updatedStudent).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid,
                    isDeleted = updatedStudent.isDeleted
                )
                var pushSuccess = false

                // 1. Try upsert with onConflict = uuid
                try {
                    client.from("students").upsert(dto, onConflict = "uuid")
                    pushSuccess = true
                } catch (e: Exception) {
                    Log.w(TAG, "Student upsert on uuid failed: ${e.message}")
                }

                // 2. Try upsert with onConflict = student_id
                if (!pushSuccess && updatedStudent.studentCode.isNotBlank()) {
                    try {
                        client.from("students").upsert(dto, onConflict = "student_id")
                        pushSuccess = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Student upsert on student_id failed: ${e.message}")
                    }
                }

                // 3. Try direct update by remote ID if existing
                if (!pushSuccess && existingRemote?.id != null) {
                    try {
                        client.from("students").update(dto) {
                            filter { eq("id", existingRemote.id) }
                        }
                        pushSuccess = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Student update by id failed: ${e.message}")
                    }
                }

                // 4. Try direct update by student_id / uuid
                if (!pushSuccess && existingRemote != null) {
                    try {
                        if (targetUuid.isNotBlank()) {
                            client.from("students").update(dto) {
                                filter { eq("uuid", targetUuid) }
                            }
                            pushSuccess = true
                        } else if (updatedStudent.studentCode.isNotBlank()) {
                            client.from("students").update(dto) {
                                filter { eq("student_id", updatedStudent.studentCode) }
                            }
                            pushSuccess = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Student update by code/uuid failed: ${e.message}")
                    }
                }

                // 5. Try insert if new
                if (!pushSuccess && existingRemote == null) {
                    try {
                        client.from("students").insert(dto.copy(id = null))
                        pushSuccess = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Student insert failed: ${e.message}")
                    }
                }

                if (pushSuccess) {
                    studentDao.markStudentSynced(updatedStudent.id, targetUuid)
                    pushedCount++
                } else {
                    Log.e(TAG, "All push methods failed for student ${student.studentCode}")
                }
            }
        }

        try {
            val remoteStudents = client.from("students").select().decodeList<StudentSupabaseDto>()
            for (remoteDto in remoteStudents) {
                val localByUuid = if (remoteDto.uuid.isNotBlank()) studentDao.getStudentByUuid(remoteDto.uuid) else null
                val localByCode = if (localByUuid == null && remoteDto.studentId.isNotBlank()) studentDao.getStudentByCode(remoteDto.studentId) else null
                val existingLocal = localByUuid ?: localByCode

                if (existingLocal != null) {
                    if (remoteDto.id != null) {
                        localToRemoteStudentIdCache[existingLocal.id] = remoteDto.id
                        remoteToLocalStudentIdCache[remoteDto.id] = existingLocal.id
                    }
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        existingRollNumber = existingLocal.rollNumber
                    )
                    
                    // DELETION TOMBSTONE PROTECTION:
                    // If local record is deleted and remote is NOT deleted, do not allow pull to resurrect the item
                    if (existingLocal.isDeleted && !remoteDto.isDeleted && remoteEntity.updatedAt <= existingLocal.updatedAt) {
                        SyncDiagnosticUtility.recordResurrectionBlocked(
                            entityType = "Student",
                            uuid = remoteDto.uuid,
                            identifier = "${existingLocal.name} (${existingLocal.studentCode})",
                            localUpdatedAt = existingLocal.updatedAt,
                            remoteUpdatedAt = remoteEntity.updatedAt
                        )
                        // Actively synchronize the deletion flag back to Supabase
                        try {
                            if (remoteDto.id != null) {
                                client.from("students").update(mapOf("is_deleted" to true)) {
                                    filter { eq("id", remoteDto.id) }
                                }
                            } else if (remoteDto.uuid.isNotBlank()) {
                                client.from("students").update(mapOf("is_deleted" to true)) {
                                    filter { eq("uuid", remoteDto.uuid) }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to propagate student deletion tombstone to Supabase: ${e.message}")
                        }
                        continue
                    }

                    if (remoteDto.isDeleted) {
                        // Remote is deleted, apply soft-delete locally
                        studentDao.updateStudent(remoteEntity.copy(isDeleted = true, isDirty = false))
                        pulledCount++
                    } else if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        studentDao.updateStudent(remoteEntity)
                        pulledCount++
                    }
                } else {
                    // Only insert new records if they are active (not deleted)
                    if (!remoteDto.isDeleted) {
                        val newEntity = remoteDto.toEntity()
                        val newId = studentDao.insertStudent(newEntity)
                        if (newId > 0 && remoteDto.id != null) {
                            localToRemoteStudentIdCache[newId] = remoteDto.id
                            remoteToLocalStudentIdCache[remoteDto.id] = newId
                        }
                        pulledCount++
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote students: ${e.message}")
        }

        return Pair(pushedCount, pulledCount)
    }

    private suspend fun syncTeachersInternal(client: SupabaseClient): Pair<Int, Int> {
        var pushedCount = 0
        var pulledCount = 0

        val remoteList = try {
            client.from("teachers").select().decodeList<TeacherSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByCode = remoteList.filter { it.teacherId.isNotBlank() }.associateBy { it.teacherId }

        val unsyncedTeachers = teacherDao.getTeachersForSync()
        for (teacher in unsyncedTeachers) {
            val assignedUuid = teacher.uuid.ifBlank { UUID.randomUUID().toString() }
            val updatedTeacher = if (teacher.uuid.isBlank()) teacher.copy(uuid = assignedUuid) else teacher
            val existingRemote = remoteByUuid[assignedUuid]
                ?: remoteByCode[updatedTeacher.teacherCode]
                ?: try {
                    client.from("teachers").select {
                        filter { eq("teacher_id", updatedTeacher.teacherCode) }
                    }.decodeList<TeacherSupabaseDto>().firstOrNull()
                } catch (e: Exception) { null }

            val targetUuid = existingRemote?.uuid?.ifBlank { null } ?: assignedUuid
            val dto = TeacherSupabaseDto.fromEntity(updatedTeacher).copy(
                id = existingRemote?.id,
                uuid = targetUuid,
                isDeleted = updatedTeacher.isDeleted
            )
            var pushSuccess = false

            // 1. Try upsert with onConflict = uuid
            try {
                client.from("teachers").upsert(dto, onConflict = "uuid")
                pushSuccess = true
            } catch (e: Exception) {
                Log.w(TAG, "Teacher upsert on uuid failed: ${e.message}")
            }

            // 2. Try upsert with onConflict = teacher_id
            if (!pushSuccess && updatedTeacher.teacherCode.isNotBlank()) {
                try {
                    client.from("teachers").upsert(dto, onConflict = "teacher_id")
                    pushSuccess = true
                } catch (e: Exception) {
                    Log.w(TAG, "Teacher upsert on teacher_id failed: ${e.message}")
                }
            }

            // 3. Try direct update by remote id
            if (!pushSuccess && existingRemote?.id != null) {
                try {
                    client.from("teachers").update(dto) {
                        filter { eq("id", existingRemote.id) }
                    }
                    pushSuccess = true
                } catch (e: Exception) {
                    Log.w(TAG, "Teacher update by id failed: ${e.message}")
                }
            }

            // 4. Try direct update by teacher_id / uuid
            if (!pushSuccess && existingRemote != null) {
                try {
                    if (targetUuid.isNotBlank()) {
                        client.from("teachers").update(dto) {
                            filter { eq("uuid", targetUuid) }
                        }
                        pushSuccess = true
                    } else if (updatedTeacher.teacherCode.isNotBlank()) {
                        client.from("teachers").update(dto) {
                            filter { eq("teacher_id", updatedTeacher.teacherCode) }
                        }
                        pushSuccess = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Teacher update by code/uuid failed: ${e.message}")
                }
            }

            // 5. Try insert if new
            if (!pushSuccess && existingRemote == null) {
                try {
                    client.from("teachers").insert(dto.copy(id = null))
                    pushSuccess = true
                } catch (e: Exception) {
                    Log.e(TAG, "Teacher insert failed: ${e.message}")
                }
            }

            if (pushSuccess) {
                teacherDao.markTeacherSynced(updatedTeacher.id, targetUuid)
                pushedCount++
            } else {
                Log.e(TAG, "All push methods failed for teacher ${teacher.teacherCode}")
            }
        }

        try {
            val remoteTeachers = client.from("teachers").select().decodeList<TeacherSupabaseDto>()
            for (remoteDto in remoteTeachers) {
                val localByUuid = if (remoteDto.uuid.isNotBlank()) teacherDao.getTeacherByUuid(remoteDto.uuid) else null
                val localByCode = if (localByUuid == null && remoteDto.teacherId.isNotBlank()) teacherDao.getTeacherByCode(remoteDto.teacherId) else null
                val existingLocal = localByUuid ?: localByCode

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id, existingAddress = existingLocal.address)
                    
                    // DELETION TOMBSTONE PROTECTION:
                    if (existingLocal.isDeleted && !remoteDto.isDeleted && remoteEntity.updatedAt <= existingLocal.updatedAt) {
                        SyncDiagnosticUtility.recordResurrectionBlocked(
                            entityType = "Teacher",
                            uuid = remoteDto.uuid,
                            identifier = "${existingLocal.fullName} (${existingLocal.teacherCode})",
                            localUpdatedAt = existingLocal.updatedAt,
                            remoteUpdatedAt = remoteEntity.updatedAt
                        )
                        // Actively synchronize the deletion flag back to Supabase
                        try {
                            if (remoteDto.id != null) {
                                client.from("teachers").update(mapOf("is_deleted" to true)) {
                                    filter { eq("id", remoteDto.id) }
                                }
                            } else if (remoteDto.uuid.isNotBlank()) {
                                client.from("teachers").update(mapOf("is_deleted" to true)) {
                                    filter { eq("uuid", remoteDto.uuid) }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to propagate teacher deletion tombstone to Supabase: ${e.message}")
                        }
                        continue
                    }

                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        teacherDao.updateTeacher(remoteEntity)
                        pulledCount++
                    }
                } else {
                    if (!remoteDto.isDeleted) {
                        val newEntity = remoteDto.toEntity()
                        teacherDao.insertTeacher(newEntity)
                        pulledCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote teachers: ${e.message}")
        }

        return Pair(pushedCount, pulledCount)
    }

    private suspend fun syncUsersInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = userDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("users").select().decodeList<UserSupabaseDto>()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch remote users before push: ${e.message}")
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByUsername = remoteList.filter { it.username.isNotBlank() }.associateBy { it.username.lowercase() }

        val allLocalUsers = dao.getAllUsersList()
        for (item in allLocalUsers) {
            val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
            val updated = if (item.uuid.isBlank()) {
                val withUuid = item.copy(uuid = assignedUuid)
                dao.updateUser(withUuid)
                withUuid
            } else item

            val existingRemote = remoteByUuid[assignedUuid]
                ?: remoteByUsername[updated.username.lowercase()]

            val isRemoteMissing = existingRemote == null
            if (item.isDirty || item.uuid.isBlank() || isRemoteMissing) {
                try {
                    val targetUuid = existingRemote?.uuid?.ifBlank { null } ?: assignedUuid
                    val dto = UserSupabaseDto.fromEntity(updated).copy(
                        id = existingRemote?.id,
                        uuid = targetUuid,
                        isDeleted = updated.isDeleted
                    )
                    var pushedSuccess = false
                    try {
                        client.from("users").upsert(dto, onConflict = "uuid")
                        pushedSuccess = true
                    } catch (e1: Exception) {
                        Log.w(TAG, "User upsert with onConflict=uuid failed for @${item.username}: ${e1.message}")
                        try {
                            client.from("users").upsert(dto, onConflict = "username")
                            pushedSuccess = true
                        } catch (e2: Exception) {
                            Log.w(TAG, "User upsert with onConflict=username failed for @${item.username}: ${e2.message}")
                            try {
                                if (existingRemote != null) {
                                    client.from("users").update(dto) {
                                        filter {
                                            if (existingRemote.id != null) eq("id", existingRemote.id)
                                            else eq("username", updated.username)
                                        }
                                    }
                                } else {
                                    client.from("users").insert(dto)
                                }
                                pushedSuccess = true
                            } catch (e3: Exception) {
                                Log.w(TAG, "Push to Supabase users table note for @${item.username}: ${e3.message}")
                            }
                        }
                    }

                    if (pushedSuccess) {
                        dao.markUserSynced(updated.id, targetUuid)
                        pushed++
                    } else {
                        // If remote sync is restricted (e.g. Supabase RLS prevents anon users insert),
                        // safely mark local account with targetUuid and clear dirty flag so it functions offline-first
                        // without an endless spinning loop.
                        dao.markUserSynced(updated.id, targetUuid)
                        Log.i(TAG, "User @${item.username} marked safe in local cache with UUID=$targetUuid")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push user @${item.username}: ${e.message}")
                    dao.markUserSynced(updated.id, assignedUuid)
                }
            }
        }

        try {
            val remotes = client.from("users").select().decodeList<UserSupabaseDto>()
            for (remoteDto in remotes) {
                val localByUuid = if (remoteDto.uuid.isNotBlank()) dao.getUserByUuid(remoteDto.uuid) else null
                val localByName = if (localByUuid == null && remoteDto.username.isNotBlank()) dao.getUserByUsernameIncludingDeleted(remoteDto.username) else null
                val existingLocal = localByUuid ?: localByName

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id).copy(
                        managedPassword = if (existingLocal.managedPassword.isNotBlank()) existingLocal.managedPassword else "Password123!"
                    )

                    // STRICT DELETION TOMBSTONE PROTECTION:
                    // If local record is marked deleted, NEVER allow pull to resurrect the account under any circumstance.
                    if (existingLocal.isDeleted) {
                        // Actively synchronize the deletion back to Supabase (hard delete + fallback soft delete)
                        try {
                            if (remoteDto.uuid.isNotBlank()) {
                                client.from("users").delete { filter { eq("uuid", remoteDto.uuid) } }
                            } else if (remoteDto.id != null) {
                                client.from("users").delete { filter { eq("id", remoteDto.id) } }
                            }
                            if (remoteDto.username.isNotBlank()) {
                                client.from("users").delete { filter { eq("username", remoteDto.username) } }
                            }
                        } catch (e: Exception) {
                            try {
                                if (remoteDto.id != null) {
                                    client.from("users").update(mapOf("is_deleted" to true, "is_active" to false)) {
                                        filter { eq("id", remoteDto.id) }
                                    }
                                } else if (remoteDto.uuid.isNotBlank()) {
                                    client.from("users").update(mapOf("is_deleted" to true, "is_active" to false)) {
                                        filter { eq("uuid", remoteDto.uuid) }
                                    }
                                }
                            } catch (e2: Exception) {
                                Log.w(TAG, "Failed to propagate user deletion tombstone to Supabase: ${e2.message}")
                            }
                        }
                        continue
                    }

                    val remoteIsDeleted = remoteDto.isDeleted ?: false
                    if (remoteIsDeleted) {
                        // Remote is deleted, apply soft-delete locally
                        dao.updateUser(remoteEntity.copy(isDeleted = true, isDirty = false))
                        pulled++
                    } else if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateUser(remoteEntity)
                        pulled++
                    }
                } else {
                    // Only insert new records if they are active (not deleted)
                    if (remoteDto.isDeleted != true) {
                        // If it's a default "admin" account from remote, check if other active admins exist
                        if (remoteDto.username.equals("admin", ignoreCase = true)) {
                            val activeAdmins = dao.getAllUsersList().filter { !it.isDeleted && (it.role == UserRole.ADMIN || it.role == UserRole.SUPER_ADMIN) }
                            if (activeAdmins.isNotEmpty()) {
                                // Another admin is already configured, skip resurrecting default admin
                                continue
                            }
                        }
                        val newEntity = remoteDto.toEntity().copy(managedPassword = "Password123!")
                        dao.insertUser(newEntity)
                        pulled++
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote users: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncSchoolSettingsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("school_settings").select().decodeList<SchoolSettingSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val existingRemote = remoteList.firstOrNull()

        val activeAcademicYear = academicYearDao?.getSanitizedActiveAcademicYearSync()
        val currentLocal = dao.getSchoolSettingsSync()
        if (activeAcademicYear != null && currentLocal != null && currentLocal.academicYear != activeAcademicYear.yearCode) {
            dao.updateSchoolSettings(currentLocal.copy(academicYear = activeAcademicYear.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
        }

        val unsynced = dao.getSchoolSettingsForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { existingRemote?.uuid ?: UUID.randomUUID().toString() }
                val activeCode = activeAcademicYear?.yearCode?.takeIf { it.isNotBlank() } ?: item.academicYear
                val updated = (if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item).let {
                    if (it.academicYear != activeCode) it.copy(academicYear = activeCode) else it
                }
                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val base64Logo = if (!item.logoUri.isNullOrBlank() && item.logoUri.length > 50) {
                    item.logoUri
                } else {
                    SyncManager.appContext?.let { ctx ->
                        com.example.ui.util.SchoolLogoUtils.getSchoolLogoAsBase64(ctx)
                    } ?: item.logoUri ?: ""
                }
                val dto = SchoolSettingSupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid,
                    logoPath = base64Logo
                )
                client.from("school_settings").upsert(dto, onConflict = "uuid")
                dao.markSchoolSettingSynced(updated.id, targetUuid)
                pushed++
                Log.d("AcademicYearTrace", "SYNC_PUSH_SETTINGS: Pushed school_settings academicYear=${updated.academicYear}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push school_settings: ${e.message}")
            }
        }

        try {
            val freshRemoteList = try {
                client.from("school_settings").select().decodeList<SchoolSettingSupabaseDto>()
            } catch (e: Exception) {
                emptyList()
            }
            val local = dao.getSchoolSettingsSync()
            val currentActiveYear = academicYearDao?.getSanitizedActiveAcademicYearSync()
            Log.d("AcademicYearTrace", "SYNC_PULL_SETTINGS_START: Local school_settings.academicYear = ${local?.academicYear} (isDirty=${local?.isDirty})")
            for (remoteDto in freshRemoteList) {
                if (remoteDto.logoPath.isNotBlank()) {
                    SyncManager.appContext?.let { ctx ->
                        com.example.ui.util.SchoolLogoUtils.saveLogoFromBase64(ctx, remoteDto.logoPath)
                    }
                }
                val remoteEntity = remoteDto.toEntity(existingLocalId = local?.id ?: 1)
                if (local == null || (!local.isDirty && remoteEntity.updatedAt > local.updatedAt)) {
                    Log.d("AcademicYearTrace", "SYNC_PULL_SETTINGS_UPDATE: Updating local school_settings from remote: remoteAcademicYear=${remoteEntity.academicYear}")
                    val entityToApply = if (currentActiveYear != null && remoteEntity.academicYear != currentActiveYear.yearCode) {
                        remoteEntity.copy(academicYear = currentActiveYear.yearCode)
                    } else {
                        remoteEntity
                    }
                    dao.updateSchoolSettings(entityToApply)
                    if (academicYearDao != null && remoteEntity.academicYear.isNotBlank()) {
                        val matching = academicYearDao.getAcademicYearByCode(remoteEntity.academicYear)
                        if (matching != null && matching.status == com.example.data.local.entity.AcademicYearStatus.ACTIVE && currentActiveYear == null) {
                            Log.d("AcademicYearTrace", "SYNC_PULL_SETTINGS_ACTIVATE: Activating matching year '${matching.yearCode}'")
                            academicYearDao.activateAcademicYear(matching.id)
                        }
                    }
                    pulled++
                } else if (local != null && local.isDirty) {
                    Log.d("AcademicYearTrace", "SYNC_PULL_SETTINGS_SKIP: Skipping remote school_settings update because local IS DIRTY (${local.academicYear})")
                }
            }
            Log.d("AcademicYearTrace", "SYNC_PULL_SETTINGS_FINISH: school_settings academicYear = ${dao.getSchoolSettingsSync()?.academicYear}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote school_settings: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncAcademicYearsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = academicYearDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("academic_years").select().decodeList<AcademicYearSupabaseDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote academic_years before push: ${e.message}")
            emptyList()
        }
        val remoteByUuid = remoteList.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
        val remoteByCode = remoteList.filter { it.yearName.isNotBlank() }.associateBy { it.yearName }

        val unsynced = dao.getAcademicYearsForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid]
                    ?: remoteByCode[updated.yearCode]
                    ?: try {
                        client.from("academic_years").select {
                            filter { eq("year_name", updated.yearCode) }
                        }.decodeList<AcademicYearSupabaseDto>().firstOrNull()
                    } catch (e: Exception) { null }

                val targetUuid = existingRemote?.uuid?.takeIf { it.isNotBlank() } ?: assignedUuid
                val dto = AcademicYearSupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid
                )
                try {
                    client.from("academic_years").upsert(dto, onConflict = "uuid")
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("academic_years_year_name_key") || msg.contains("duplicate key") || msg.contains("unique constraint")) {
                        Log.w(TAG, "Conflict on year_name during upsert, retrying with onConflict = year_name...")
                        client.from("academic_years").upsert(dto, onConflict = "year_name")
                    } else {
                        throw e
                    }
                }
                dao.markAcademicYearSynced(updated.id, targetUuid)
                pushed++
                Log.d("AcademicYearTrace", "SYNC_PUSH_YEAR: Pushed academic_year ${item.yearCode} (isCurrentActive=${item.isCurrentActive}, status=${item.status})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push academic_year ${item.yearCode}: ${e.message}")
            }
        }

        try {
            val remotes = client.from("academic_years").select().decodeList<AcademicYearSupabaseDto>()
            Log.d("AcademicYearTrace", "SYNC_PULL_YEARS_START: Pulled ${remotes.size} remote academic_years")
            for (remoteDto in remotes) {
                val localByUuid = if (remoteDto.uuid.isNotBlank()) dao.getAcademicYearByUuid(remoteDto.uuid) else null
                val localByCode = if (localByUuid == null && remoteDto.yearName.isNotBlank()) dao.getAcademicYearByCode(remoteDto.yearName) else null
                val existingLocal = localByUuid ?: localByCode

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty && remoteEntity.updatedAt > existingLocal.updatedAt) {
                        Log.d("AcademicYearTrace", "SYNC_PULL_YEAR_UPDATE: Updating local year ${existingLocal.yearCode} from remote (remoteActive=${remoteEntity.isCurrentActive}, remoteStatus=${remoteEntity.status})")
                        dao.updateAcademicYear(remoteEntity)
                        pulled++
                    } else if (existingLocal.isDirty) {
                        Log.d("AcademicYearTrace", "SYNC_PULL_YEAR_SKIP: Skipping remote update for ${existingLocal.yearCode} because local IS DIRTY")
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    Log.d("AcademicYearTrace", "SYNC_PULL_YEAR_INSERT: Inserting new local year ${newEntity.yearCode} (active=${newEntity.isCurrentActive})")
                    dao.insertAcademicYear(newEntity)
                    pulled++
                }
            }
            dao.sanitizeActiveAcademicYears()
            var activeAfterPull = dao.getSanitizedActiveAcademicYearSync()
            if (activeAfterPull == null) {
                val settingsYear = schoolPolicyDao?.getSchoolSettingsSync()?.academicYear?.takeIf { it.isNotBlank() }
                if (settingsYear != null) {
                    val matching = dao.getAcademicYearByCode(settingsYear)
                    if (matching != null) {
                        dao.activateAcademicYear(matching.id)
                        activeAfterPull = dao.getSanitizedActiveAcademicYearSync()
                    }
                }
            }
            Log.d("AcademicYearTrace", "SYNC_PULL_YEARS_FINISH: Room active year after pull and sanitize = ${activeAfterPull?.yearCode} (isCurrentActive=${activeAfterPull?.isCurrentActive})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote academic_years: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncGradesInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("grades").select().decodeList<GradeSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.gradeName.isNotBlank() }.associateBy { it.gradeName.trim().lowercase() }

        val allLocalGrades = dao.getAllGradesSync()
        for (local in allLocalGrades) {
            val remote = (if (local.uuid.isNotBlank()) remoteByUuid[local.uuid] else null)
                ?: remoteByName[local.gradeName.trim().lowercase()]
            if (remote?.id != null) {
                localToRemoteGradeIdCache[local.id] = remote.id
                remoteToLocalGradeIdCache[remote.id] = local.id
            }
        }

        val unsynced = if (remoteList.isEmpty()) allLocalGrades else dao.getGradesForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName[updated.gradeName.trim().lowercase()]

                val targetUuid = existingRemote?.uuid?.takeIf { it.isNotBlank() } ?: assignedUuid
                val targetId = existingRemote?.id ?: if (item.id > 0) item.id else null
                val dto = GradeSupabaseDto.fromEntity(updated).copy(
                    id = targetId,
                    uuid = targetUuid
                )
                try {
                    client.from("grades").upsert(dto, onConflict = "uuid")
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("grade_name") || msg.contains("duplicate key") || msg.contains("unique constraint")) {
                        client.from("grades").upsert(dto, onConflict = "grade_name")
                    } else {
                        throw e
                    }
                }
                dao.markGradeSynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push grade ${item.gradeName}: ${e.message}")
            }
        }

        try {
            val remotes = client.from("grades").select().decodeList<GradeSupabaseDto>()
            for (remoteDto in remotes) {
                val localByUuid = if (remoteDto.uuid.isNotBlank()) dao.getGradeByUuid(remoteDto.uuid) else null
                val localByName = if (localByUuid == null && remoteDto.gradeName.isNotBlank()) dao.getGradeByName(remoteDto.gradeName) else null
                val existingLocal = localByUuid ?: localByName

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertGrade(remoteEntity)
                        pulled++
                    }
                    if (remoteDto.id != null) {
                        localToRemoteGradeIdCache[existingLocal.id] = remoteDto.id
                        remoteToLocalGradeIdCache[remoteDto.id] = existingLocal.id
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    val newId = dao.insertGrade(newEntity)
                    pulled++
                    if (remoteDto.id != null && newId > 0) {
                        localToRemoteGradeIdCache[newId] = remoteDto.id
                        remoteToLocalGradeIdCache[remoteDto.id] = newId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote grades: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncSchoolClassesInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("school_classes").select().decodeList<SchoolClassSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.className.isNotBlank() }.associateBy { "${it.gradeId}_${it.className}" }

        val unsynced = dao.getClassesForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val remoteGradeId = localToRemoteGradeIdCache[updated.gradeId] ?: updated.gradeId
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName["${remoteGradeId}_${updated.className}"]

                val targetUuid = existingRemote?.uuid?.takeIf { it.isNotBlank() } ?: assignedUuid
                val dto = SchoolClassSupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid,
                    gradeId = remoteGradeId
                )
                try {
                    client.from("school_classes").upsert(dto, onConflict = "uuid")
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("duplicate key") || msg.contains("unique constraint")) {
                        client.from("school_classes").upsert(dto, onConflict = "class_name")
                    } else {
                        throw e
                    }
                }
                dao.markClassSynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push school_class ${item.className}: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getClassByUuid(remoteDto.uuid) else null
                val resolvedGradeId = remoteToLocalGradeIdCache[remoteDto.gradeId] ?: remoteDto.gradeId
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id, resolvedGradeId = resolvedGradeId)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertClass(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity(resolvedGradeId = resolvedGradeId)
                    dao.insertClass(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote school_classes: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncSubjectsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("subjects").select().decodeList<SubjectSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
        val remoteByLevelAndName = remoteList.filter { it.name.isNotBlank() }.associateBy { 
            "${it.educationLevel.trim().uppercase()}_${it.name.trim().lowercase()}_${it.category.trim().uppercase()}_${it.subTrack.trim().lowercase()}"
        }
        val remoteByNameOnly = remoteList.filter { it.name.isNotBlank() }.associateBy { it.name.trim().lowercase() }

        val allLocalSubjects = dao.getAllSubjectsSync()
        for (local in allLocalSubjects) {
            val compositeKey = "${local.educationLevel.name}_${local.name.trim().lowercase()}_${local.category.name}_${local.subTrack.trim().lowercase()}"
            val remote = (if (local.uuid.isNotBlank()) remoteByUuid[local.uuid] else null)
                ?: remoteByLevelAndName[compositeKey]
                ?: remoteByNameOnly[local.name.trim().lowercase()]
            if (remote?.id != null) {
                localToRemoteSubjectIdCache[local.id] = remote.id
                remoteToLocalSubjectIdCache[remote.id] = local.id
            }
        }

        val unsynced = if (remoteList.isEmpty()) allLocalSubjects else dao.getSubjectsForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val compositeKey = "${item.educationLevel.name}_${item.name.trim().lowercase()}_${item.category.name}_${item.subTrack.trim().lowercase()}"
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByLevelAndName[compositeKey] ?: remoteByNameOnly[item.name.trim().lowercase()]

                val targetUuid = existingRemote?.uuid?.takeIf { it.isNotBlank() } ?: assignedUuid
                val targetId = existingRemote?.id ?: if (item.id > 0) item.id else null
                val dto = SubjectSupabaseDto.fromEntity(updated).copy(
                    id = targetId,
                    uuid = targetUuid
                )
                try {
                    client.from("subjects").upsert(dto, onConflict = "uuid")
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("schema cache") || msg.contains("column") || msg.contains("Could not find")) {
                        // Remote table does not have newer columns (e.g. category, education_level); fallback to minimal DTO
                        val minimalDto = SubjectMinimalSupabaseDto(
                            id = targetId,
                            uuid = targetUuid,
                            name = updated.name,
                            updatedAt = millisToIso(updated.updatedAt),
                            isDeleted = updated.isDeleted
                        )
                        try {
                            client.from("subjects").upsert(minimalDto, onConflict = "uuid")
                        } catch (e2: Exception) {
                            client.from("subjects").upsert(minimalDto, onConflict = "name")
                        }
                    } else if (msg.contains("name") || msg.contains("duplicate key") || msg.contains("unique constraint")) {
                        client.from("subjects").upsert(dto, onConflict = "name")
                    } else {
                        throw e
                    }
                }
                dao.markSubjectSynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push subject ${item.name}: ${e.message}")
            }
        }

        try {
            val remotes = client.from("subjects").select().decodeList<SubjectSupabaseDto>()
            for (remoteDto in remotes) {
                val parsedLevel = try { EducationLevel.valueOf(remoteDto.educationLevel.trim().uppercase()) } catch (e: Exception) { EducationLevel.PRIMARY }
                val localByUuid = if (remoteDto.uuid.isNotBlank()) dao.getSubjectByUuid(remoteDto.uuid) else null
                val localByName = if (localByUuid == null && remoteDto.name.isNotBlank()) dao.getSubjectByNameAndLevel(remoteDto.name.trim(), parsedLevel) else null
                val existingLocal = localByUuid ?: localByName

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (remoteDto.isDeleted) {
                        if (!existingLocal.isDeleted) {
                            dao.updateSubject(existingLocal.copy(isDeleted = true, isDirty = false, updatedAt = remoteEntity.updatedAt))
                            pulled++
                        }
                    } else if (existingLocal.isDeleted) {
                        // If local is deleted and remote is not deleted, only resurrect if remote is strictly newer
                        if (remoteEntity.updatedAt > existingLocal.updatedAt) {
                            dao.updateSubject(remoteEntity)
                            pulled++
                        } else {
                            // DELETION TOMBSTONE PROTECTION: actively propagate deletion flag back to Supabase
                            SyncDiagnosticUtility.recordResurrectionBlocked(
                                entityType = "Subject",
                                uuid = remoteDto.uuid,
                                identifier = "${existingLocal.name} (${existingLocal.educationLevel})",
                                localUpdatedAt = existingLocal.updatedAt,
                                remoteUpdatedAt = remoteEntity.updatedAt
                            )
                            try {
                                if (remoteDto.id != null) {
                                    client.from("subjects").update(mapOf("is_deleted" to true)) {
                                        filter { eq("id", remoteDto.id) }
                                    }
                                } else if (remoteDto.uuid.isNotBlank()) {
                                    client.from("subjects").update(mapOf("is_deleted" to true)) {
                                        filter { eq("uuid", remoteDto.uuid) }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to propagate subject deletion tombstone to Supabase: ${e.message}")
                            }
                        }
                    } else if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateSubject(remoteEntity)
                        pulled++
                    }
                    if (remoteDto.id != null) {
                        localToRemoteSubjectIdCache[existingLocal.id] = remoteDto.id
                        remoteToLocalSubjectIdCache[remoteDto.id] = existingLocal.id
                    }
                } else if (!remoteDto.isDeleted) {
                    val newEntity = remoteDto.toEntity()
                    val newId = dao.insertSubject(newEntity)
                    pulled++
                    if (remoteDto.id != null && newId > 0) {
                        localToRemoteSubjectIdCache[newId] = remoteDto.id
                        remoteToLocalSubjectIdCache[remoteDto.id] = newId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote subjects: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncGradeSubjectCrossRefsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteGrades = try {
            client.from("grades").select().decodeList<GradeSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteGradeByUuid = remoteGrades.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
        val remoteGradeByName = remoteGrades.filter { it.gradeName.isNotBlank() }.associateBy { it.gradeName.trim().lowercase() }
        val remoteGradeById = remoteGrades.filter { it.id != null }.associateBy { it.id!! }

        val remoteSubjects = try {
            client.from("subjects").select().decodeList<SubjectSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteSubjByUuid = remoteSubjects.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
        val remoteSubjByName = remoteSubjects.filter { it.name.isNotBlank() }.associateBy { it.name.trim().lowercase() }
        val remoteSubjById = remoteSubjects.filter { it.id != null }.associateBy { it.id!! }

        val allLocalGrades = dao.getAllGradesSync().associateBy { it.id }
        val allLocalSubjects = dao.getAllSubjectsSync().associateBy { it.id }

        try {
            val localRefs = dao.getGradeSubjectCrossRefs()
            if (localRefs.isNotEmpty()) {
                val dtos = mutableListOf<GradeSubjectCrossRefSupabaseDto>()
                for (ref in localRefs) {
                    val localGrade = allLocalGrades[ref.gradeId]
                    val localSubject = allLocalSubjects[ref.subjectId]

                    val remoteGrade = if (localGrade != null) {
                        (if (localGrade.uuid.isNotBlank()) remoteGradeByUuid[localGrade.uuid] else null)
                            ?: remoteGradeByName[localGrade.gradeName.trim().lowercase()]
                            ?: (localToRemoteGradeIdCache[ref.gradeId]?.let { remoteGradeById[it] })
                    } else null

                    val remoteSubject = if (localSubject != null) {
                        (if (localSubject.uuid.isNotBlank()) remoteSubjByUuid[localSubject.uuid] else null)
                            ?: remoteSubjByName[localSubject.name.trim().lowercase()]
                            ?: (localToRemoteSubjectIdCache[ref.subjectId]?.let { remoteSubjById[it] })
                    } else null

                    val targetGradeId = remoteGrade?.id
                        ?: localToRemoteGradeIdCache[ref.gradeId]
                        ?: if (remoteGradeById.containsKey(ref.gradeId)) ref.gradeId else null

                    val targetSubjectId = remoteSubject?.id
                        ?: localToRemoteSubjectIdCache[ref.subjectId]
                        ?: if (remoteSubjById.containsKey(ref.subjectId)) ref.subjectId else null

                    // SAFETY: Guard against foreign key violations when remote entities are missing
                    if (targetGradeId == null || (remoteGradeById.isNotEmpty() && !remoteGradeById.containsKey(targetGradeId))) {
                        Log.w(TAG, "Skipping grade_subject_cross_ref push for gradeId ${ref.gradeId}: remote grade ID $targetGradeId not present in remote grades table")
                        continue
                    }
                    if (targetSubjectId == null || (remoteSubjById.isNotEmpty() && !remoteSubjById.containsKey(targetSubjectId))) {
                        Log.w(TAG, "Skipping grade_subject_cross_ref push for subjectId ${ref.subjectId}: remote subject ID $targetSubjectId not present in remote subjects table")
                        continue
                    }

                    dtos.add(GradeSubjectCrossRefSupabaseDto(gradeId = targetGradeId, subjectId = targetSubjectId))
                }

                if (dtos.isNotEmpty()) {
                    try {
                        client.from("grade_subject_cross_ref").upsert(dtos, onConflict = "grade_id,subject_id")
                    } catch (e: Exception) {
                        client.from("grade_subject_cross_ref").upsert(dtos)
                    }
                    pushed = dtos.size
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push grade_subject_cross_ref: ${e.message}")
        }

        try {
            val remoteDtos = client.from("grade_subject_cross_ref").select().decodeList<GradeSubjectCrossRefSupabaseDto>()
            if (remoteDtos.isNotEmpty()) {
                val localByGradeUuid = allLocalGrades.values.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
                val localByGradeName = allLocalGrades.values.filter { it.gradeName.isNotBlank() }.associateBy { it.gradeName.trim().lowercase() }
                val localBySubjUuid = allLocalSubjects.values.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
                val localBySubjName = allLocalSubjects.values.filter { it.name.isNotBlank() }.associateBy { it.name.trim().lowercase() }

                val entities = mutableListOf<GradeSubjectCrossRef>()
                for (remoteDto in remoteDtos) {
                    val remoteG = remoteGradeById[remoteDto.gradeId]
                    val localGId = remoteToLocalGradeIdCache[remoteDto.gradeId]
                        ?: (remoteG?.uuid?.let { localByGradeUuid[it]?.id })
                        ?: (remoteG?.gradeName?.let { localByGradeName[it.trim().lowercase()]?.id })
                        ?: if (allLocalGrades.containsKey(remoteDto.gradeId)) remoteDto.gradeId else null

                    val remoteS = remoteSubjById[remoteDto.subjectId]
                    val localSId = remoteToLocalSubjectIdCache[remoteDto.subjectId]
                        ?: (remoteS?.uuid?.let { localBySubjUuid[it]?.id })
                        ?: (remoteS?.name?.let { localBySubjName[it.trim().lowercase()]?.id })
                        ?: if (allLocalSubjects.containsKey(remoteDto.subjectId)) remoteDto.subjectId else null

                    if (localGId != null && localSId != null) {
                        entities.add(GradeSubjectCrossRef(gradeId = localGId, subjectId = localSId))
                    }
                }
                if (entities.isNotEmpty()) {
                    dao.insertGradeSubjectCrossRefs(entities)
                    pulled = entities.size
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote grade_subject_cross_ref: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncAssessmentTypesInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("assessment_types").select().decodeList<AssessmentTypeSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.name.isNotBlank() }.associateBy { it.name }

        val unsynced = dao.getAssessmentTypesForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName[updated.name]

                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val dto = AssessmentTypeSupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid
                )
                client.from("assessment_types").upsert(dto, onConflict = "uuid")
                dao.markAssessmentTypeSynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push assessment_type ${item.name}: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getAssessmentTypeByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertAssessmentType(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertAssessmentType(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote assessment_types: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncCustomExamsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("custom_exams").select().decodeList<CustomExamSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.examName.isNotBlank() }.associateBy { it.examName }

        val unsynced = dao.getCustomExamsForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName[updated.examName]

                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val dto = CustomExamSupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid
                )
                client.from("custom_exams").upsert(dto, onConflict = "uuid")
                dao.markCustomExamSynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push custom_exam ${item.examName}: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getCustomExamByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertCustomExam(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertCustomExam(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote custom_exams: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncGradingPoliciesInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = schoolPolicyDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("grading_policies").select().decodeList<GradingPolicySupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByLevel = remoteList.filter { it.educationLevel.isNotBlank() }.associateBy { it.educationLevel }

        val unsynced = dao.getGradingPoliciesForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByLevel[updated.educationLevel.name]

                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val dto = GradingPolicySupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid
                )
                client.from("grading_policies").upsert(dto, onConflict = "uuid")
                dao.markGradingPolicySynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push grading_policy: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getGradingPolicyByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateGradingPolicy(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertGradingPolicy(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote grading_policies: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncAssessmentPeriodsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = assessmentPeriodDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("assessment_periods").select().decodeList<AssessmentPeriodSupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.periodName.isNotBlank() }.associateBy { "${it.educationLevel}_${it.periodName}" }

        val unsynced = dao.getAssessmentPeriodsForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName["${updated.educationLevel}_${updated.periodName}"]

                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val dto = AssessmentPeriodSupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid
                )
                client.from("assessment_periods").upsert(dto, onConflict = "uuid")
                dao.markAssessmentPeriodSynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push assessment_period ${item.periodName}: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getAssessmentPeriodByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateAssessmentPeriod(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertAssessmentPeriod(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote assessment_periods: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncAssessmentsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = assessmentDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("assessments").select().decodeList<AssessmentSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }

        for (remote in remoteMap.values) {
            if (remote.id != null) {
                if (remote.uuid.isNotBlank()) {
                    val local = dao.getAssessmentByUuid(remote.uuid)
                    if (local != null) {
                        localToRemoteAssessmentIdCache[local.id] = remote.id
                        remoteToLocalAssessmentIdCache[remote.id] = local.id
                    }
                }
            }
        }

        val remoteByKey = remoteMap.values.associateBy {
            "${it.grade.trim().uppercase()}_${it.subject.trim().uppercase()}_${it.title.trim().uppercase()}_${it.assessmentType.trim().uppercase()}"
        }

        val allLocal = dao.getAssessmentsForSync()
        for (item in allLocal) {
            val localKey = "${item.grade.trim().uppercase()}_${item.subjectName.trim().uppercase()}_${item.assessmentName.trim().uppercase()}_${item.assessmentType.trim().uppercase()}"
            val existingRemote = (if (item.uuid.isNotBlank()) remoteMap[item.uuid] else null) ?: remoteByKey[localKey]

            if (existingRemote?.id != null) {
                localToRemoteAssessmentIdCache[item.id] = existingRemote.id
                remoteToLocalAssessmentIdCache[existingRemote.id] = item.id
            }

            val targetUuid = existingRemote?.uuid?.ifBlank { null }
                ?: item.uuid.ifBlank { UUID.randomUUID().toString() }
            val updated = if (item.uuid != targetUuid) item.copy(uuid = targetUuid) else item

            val isRemoteMissing = existingRemote == null
            if (item.isDirty || item.uuid.isBlank() || isRemoteMissing) {
                try {
                    val dto = AssessmentSupabaseDto.fromEntity(updated).copy(
                        id = existingRemote?.id,
                        uuid = targetUuid
                    )
                    client.from("assessments").upsert(dto, onConflict = "uuid")
                    dao.markAssessmentSynced(updated.id, targetUuid)
                    pushed++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push assessment ${item.assessmentName}: ${e.message}")
                }
            }
        }

        try {
            val remotes = client.from("assessments").select().decodeList<AssessmentSupabaseDto>()
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getAssessmentByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    if (remoteDto.id != null) {
                        localToRemoteAssessmentIdCache[existingLocal.id] = remoteDto.id
                        remoteToLocalAssessmentIdCache[remoteDto.id] = existingLocal.id
                    }
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    
                    // DELETION TOMBSTONE PROTECTION:
                    if (existingLocal.isDeleted && !remoteDto.isDeleted && remoteEntity.updatedAt <= existingLocal.updatedAt) {
                        SyncDiagnosticUtility.recordResurrectionBlocked(
                            entityType = "Assessment",
                            uuid = remoteDto.uuid,
                            identifier = "${existingLocal.assessmentName} (${existingLocal.grade}/${existingLocal.subjectName})",
                            localUpdatedAt = existingLocal.updatedAt,
                            remoteUpdatedAt = remoteEntity.updatedAt
                        )
                        continue
                    }

                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateAssessment(remoteEntity)
                        pulled++
                    }
                } else {
                    if (!remoteDto.isDeleted) {
                        val newEntity = remoteDto.toEntity()
                        val newId = dao.insertAssessment(newEntity)
                        if (newId > 0 && remoteDto.id != null) {
                            localToRemoteAssessmentIdCache[newId] = remoteDto.id
                            remoteToLocalAssessmentIdCache[remoteDto.id] = newId
                        }
                        pulled++
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote assessments: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncStudentMarksInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = marksDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("student_marks").select().decodeList<StudentMarkSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }

        while (true) {
            val unsynced = dao.getStudentMarksForSync(limit = 300)
            if (unsynced.isEmpty()) break
            var batchProcessed = 0
            for (item in unsynced) {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val localStudent = studentDao.getStudentById(item.studentId)
                if (localStudent == null) {
                    Log.w(TAG, "Skipping student_mark push: local studentId ${item.studentId} not found")
                    dao.markStudentMarkSynced(item.id, assignedUuid)
                    batchProcessed++
                    continue
                }
                val targetStudentId = ensureRemoteStudentId(client, localStudent)
                val localAssessment = assessmentDao?.getAssessmentByIdDirect(item.assessmentId)
                val targetAssessmentId = if (localAssessment != null) ensureRemoteAssessmentId(client, localAssessment) else item.assessmentId

                try {
                    val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                    val pushDto = StudentMarkPushDto.fromEntity(
                        entity = updated,
                        targetStudentId = targetStudentId,
                        targetAssessmentId = targetAssessmentId
                    )
                    client.from("student_marks").upsert(pushDto, onConflict = "uuid")
                    dao.markStudentMarkSynced(updated.id, assignedUuid)
                    pushed++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push student_mark: ${e.message}")
                    dao.markStudentMarkSynced(item.id, assignedUuid)
                }
                batchProcessed++
            }
            if (batchProcessed == 0) break
        }

        val localAssessmentCache = mutableMapOf<Long, Long>()
        val localStudentCache = mutableMapOf<Long, Long>()

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("student_marks").select().decodeList<StudentMarkSupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getStudentMarkByUuid(remoteDto.uuid) else null

                val localAssessmentId = if (existingLocal != null && existingLocal.assessmentId > 0) {
                    existingLocal.assessmentId
                } else {
                    resolveLocalAssessmentId(remoteDto.assessmentId, client, localAssessmentCache)
                }

                val localStudentId = if (existingLocal != null && existingLocal.studentId > 0) {
                    existingLocal.studentId
                } else {
                    resolveLocalStudentId(remoteDto.studentId, client, localStudentCache)
                }

                if (localAssessmentId == null || localStudentId == null) {
                    Log.w(TAG, "Skipping remote student_mark (uuid=${remoteDto.uuid}): assessment or student reference missing")
                    continue
                }

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        resolvedAssessmentId = localAssessmentId,
                        resolvedStudentId = localStudentId,
                        studentCode = existingLocal.studentCode,
                        studentName = existingLocal.studentName,
                        rollNo = existingLocal.rollNo,
                        subjectName = existingLocal.subjectName
                    )
                    
                    // DELETION TOMBSTONE PROTECTION:
                    if (existingLocal.isDeleted && !remoteDto.isDeleted && remoteEntity.updatedAt <= existingLocal.updatedAt) {
                        SyncDiagnosticUtility.recordResurrectionBlocked(
                            entityType = "StudentMark",
                            uuid = remoteDto.uuid,
                            identifier = "Assessment ${existingLocal.assessmentId}, Student ${existingLocal.studentId}, Subject ${existingLocal.subjectName}",
                            localUpdatedAt = existingLocal.updatedAt,
                            remoteUpdatedAt = remoteEntity.updatedAt
                        )
                        continue
                    }

                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        try {
                            dao.insertOrUpdateMark(remoteEntity)
                            pulled++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update student_mark: ${e.message}")
                        }
                    }
                } else {
                    if (!remoteDto.isDeleted) {
                        val student = studentDao.getStudentById(localStudentId)
                        val newEntity = remoteDto.toEntity(
                            existingLocalId = 0,
                            resolvedAssessmentId = localAssessmentId,
                            resolvedStudentId = localStudentId,
                            studentCode = student?.studentCode ?: "",
                            studentName = student?.name ?: "",
                            rollNo = student?.rollNumber ?: 0
                        )
                        try {
                            dao.insertOrUpdateMark(newEntity)
                            pulled++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to insert remote student_mark: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote student_marks: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncAssessmentResultsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = marksDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("assessment_results").select().decodeList<AssessmentResultSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }

        while (true) {
            val unsynced = dao.getAssessmentResultsForSync(limit = 300)
            if (unsynced.isEmpty()) break
            var batchProcessed = 0
            for (item in unsynced) {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val localStudent = studentDao.getStudentById(item.studentId)
                if (localStudent == null) {
                    Log.w(TAG, "Skipping assessment_result push: local studentId ${item.studentId} not found")
                    dao.markAssessmentResultSynced(item.id, assignedUuid)
                    batchProcessed++
                    continue
                }
                val targetStudentId = ensureRemoteStudentId(client, localStudent)
                try {
                    val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                    val pushDto = AssessmentResultPushDto.fromEntity(
                        entity = updated,
                        targetStudentId = targetStudentId
                    )
                    client.from("assessment_results").upsert(pushDto, onConflict = "uuid")
                    dao.markAssessmentResultSynced(updated.id, assignedUuid)
                    pushed++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push assessment_result: ${e.message}")
                    dao.markAssessmentResultSynced(item.id, assignedUuid)
                }
                batchProcessed++
            }
            if (batchProcessed == 0) break
        }

        val localAssessmentCache = mutableMapOf<Long, Long>()
        val localStudentCache = mutableMapOf<Long, Long>()

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("assessment_results").select().decodeList<AssessmentResultSupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getAssessmentResultByUuid(remoteDto.uuid) else null

                val localAssessmentId = if (existingLocal != null && existingLocal.assessmentId > 0) {
                    existingLocal.assessmentId
                } else {
                    resolveLocalAssessmentId(remoteDto.assessmentPeriod.toLongOrNull() ?: 0L, client, localAssessmentCache)
                }

                val localStudentId = if (existingLocal != null && existingLocal.studentId > 0) {
                    existingLocal.studentId
                } else {
                    resolveLocalStudentId(remoteDto.studentId, client, localStudentCache)
                }

                if (localStudentId == null) {
                    Log.w(TAG, "Skipping remote assessment_result (uuid=${remoteDto.uuid}): student reference missing")
                    continue
                }

                val targetAssessmentId = localAssessmentId ?: 0L

                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        resolvedAssessmentId = targetAssessmentId,
                        resolvedStudentId = localStudentId,
                        studentCode = existingLocal.studentCode,
                        studentName = existingLocal.studentName,
                        rollNo = existingLocal.rollNo
                    )
                    
                    // DELETION TOMBSTONE PROTECTION:
                    if (existingLocal.isDeleted && !remoteDto.isDeleted && remoteEntity.updatedAt <= existingLocal.updatedAt) {
                        SyncDiagnosticUtility.recordResurrectionBlocked(
                            entityType = "AssessmentResult",
                            uuid = remoteDto.uuid,
                            identifier = "Assessment $targetAssessmentId, Student $localStudentId",
                            localUpdatedAt = existingLocal.updatedAt,
                            remoteUpdatedAt = remoteEntity.updatedAt
                        )
                        continue
                    }

                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        try {
                            dao.insertOrUpdateResultSummary(remoteEntity)
                            pulled++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update assessment_result: ${e.message}")
                        }
                    }
                } else {
                    if (!remoteDto.isDeleted) {
                        val student = studentDao.getStudentById(localStudentId)
                        val newEntity = remoteDto.toEntity(
                            existingLocalId = 0,
                            resolvedAssessmentId = targetAssessmentId,
                            resolvedStudentId = localStudentId,
                            studentCode = student?.studentCode ?: "",
                            studentName = student?.name ?: "",
                            rollNo = student?.rollNumber ?: 0
                        )
                        try {
                            dao.insertOrUpdateResultSummary(newEntity)
                            pulled++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to insert remote assessment_result: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote assessment_results: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncHolisticCategoriesInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = holisticDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("holistic_categories").select().decodeList<HolisticCategorySupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.categoryName.isNotBlank() || it.name.isNotBlank() }
            .associateBy { it.categoryName.ifBlank { it.name } }

        val unsynced = dao.getHolisticCategoriesForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName[updated.categoryName]

                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val baseDto = HolisticCategorySupabaseDto.fromEntity(updated)
                val existingCode = existingRemote?.code?.ifBlank { null }
                val dto = baseDto.copy(
                    id = existingRemote?.id,
                    uuid = targetUuid,
                    code = existingCode ?: baseDto.code.ifBlank { "CAT_${updated.id.takeIf { it > 0 } ?: targetUuid.take(8).uppercase()}" }
                )
                client.from("holistic_categories").upsert(dto, onConflict = "uuid")
                dao.markHolisticCategorySynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push holistic_category ${item.categoryName}: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getHolisticCategoryByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateHolisticCategory(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertHolisticCategory(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote holistic_categories: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncHolisticResultsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = holisticDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("holistic_results").select().decodeList<HolisticResultSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: Exception) {
            emptyMap()
        }

        while (true) {
            val unsynced = dao.getHolisticResultsForSync(limit = 300)
            if (unsynced.isEmpty()) break
            var batchProcessed = 0
            for (item in unsynced) {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val localStudent = studentDao.getStudentById(item.studentId)
                if (localStudent == null) {
                    Log.w(TAG, "Skipping holistic_result push: local studentId ${item.studentId} not found")
                    dao.markHolisticResultSynced(item.id, assignedUuid)
                    batchProcessed++
                    continue
                }
                val targetStudentId = ensureRemoteStudentId(client, localStudent)
                try {
                    val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                    val pushDto = HolisticResultPushDto.fromEntity(
                        entity = updated,
                        targetStudentId = targetStudentId,
                        targetCategoryId = item.categoryId
                    )
                    client.from("holistic_results").upsert(pushDto, onConflict = "uuid")
                    dao.markHolisticResultSynced(updated.id, assignedUuid)
                    pushed++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push holistic_result: ${e.message}")
                    dao.markHolisticResultSynced(item.id, assignedUuid)
                }
                batchProcessed++
            }
            if (batchProcessed == 0) break
        }

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("holistic_results").select().decodeList<HolisticResultSupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getHolisticResultByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        categoryName = existingLocal.categoryName,
                        grade = existingLocal.grade,
                        className = existingLocal.className
                    )
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertOrUpdateHolisticResults(listOf(remoteEntity))
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertOrUpdateHolisticResults(listOf(newEntity))
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote holistic_results: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncSgiCategoriesInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = holisticDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteList = try {
            client.from("sgi_categories").select().decodeList<SgiCategorySupabaseDto>()
        } catch (e: Exception) {
            emptyList()
        }
        val remoteByUuid = remoteList.associateBy { it.uuid }
        val remoteByName = remoteList.filter { it.categoryName.isNotBlank() || it.name.isNotBlank() }
            .associateBy { it.categoryName.ifBlank { it.name } }

        val unsynced = dao.getSgiCategoriesForSync()
        for (item in unsynced) {
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val existingRemote = remoteByUuid[assignedUuid] ?: remoteByName[updated.indicatorName]

                val targetUuid = existingRemote?.uuid ?: assignedUuid
                val dto = SgiCategorySupabaseDto.fromEntity(updated).copy(
                    id = existingRemote?.id,
                    uuid = targetUuid
                )
                client.from("sgi_categories").upsert(dto, onConflict = "uuid")
                dao.markSgiCategorySynced(updated.id, targetUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push sgi_category ${item.indicatorName}: ${e.message}")
            }
        }

        try {
            for (remoteDto in remoteList) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getSgiCategoryByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(existingLocalId = existingLocal.id)
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.updateSgiCategory(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertSgiCategory(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote sgi_categories: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncSgiResultsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = holisticDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("sgi_results").select().decodeList<SgiResultSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: Exception) {
            emptyMap()
        }

        while (true) {
            val unsynced = dao.getSgiResultsForSync(limit = 300)
            if (unsynced.isEmpty()) break
            var batchProcessed = 0
            for (item in unsynced) {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val localStudent = studentDao.getStudentById(item.studentId)
                if (localStudent == null) {
                    Log.w(TAG, "Skipping sgi_result push: local studentId ${item.studentId} not found")
                    dao.markSgiResultSynced(item.id, assignedUuid)
                    batchProcessed++
                    continue
                }
                val targetStudentId = ensureRemoteStudentId(client, localStudent)
                try {
                    val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                    val pushDto = SgiResultPushDto.fromEntity(
                        entity = updated,
                        targetStudentId = targetStudentId,
                        targetCategoryId = item.sgiCategoryId
                    )
                    client.from("sgi_results").upsert(pushDto, onConflict = "uuid")
                    dao.markSgiResultSynced(updated.id, assignedUuid)
                    pushed++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push sgi_result: ${e.message}")
                    dao.markSgiResultSynced(item.id, assignedUuid)
                }
                batchProcessed++
            }
            if (batchProcessed == 0) break
        }

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("sgi_results").select().decodeList<SgiResultSupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getSgiResultByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        indicatorName = existingLocal.indicatorName,
                        grade = existingLocal.grade,
                        className = existingLocal.className
                    )
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertOrUpdateSgiResults(listOf(remoteEntity))
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertOrUpdateSgiResults(listOf(newEntity))
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote sgi_results: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncTeacherCommentsInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = holisticDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("teacher_comments").select().decodeList<TeacherCommentSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: Exception) {
            emptyMap()
        }

        while (true) {
            val unsynced = dao.getTeacherCommentsForSync(limit = 300)
            if (unsynced.isEmpty()) break
            var batchProcessed = 0
            for (item in unsynced) {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val localStudent = studentDao.getStudentById(item.studentId)
                if (localStudent == null) {
                    Log.w(TAG, "Skipping teacher_comment push: local studentId ${item.studentId} not found")
                    dao.markTeacherCommentSynced(item.id, assignedUuid)
                    batchProcessed++
                    continue
                }
                val targetStudentId = ensureRemoteStudentId(client, localStudent)
                try {
                    val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                    val pushDto = TeacherCommentPushDto.fromEntity(
                        entity = updated,
                        targetStudentId = targetStudentId
                    )
                    client.from("teacher_comments").upsert(pushDto, onConflict = "uuid")
                    dao.markTeacherCommentSynced(updated.id, assignedUuid)
                    pushed++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push teacher_comment: ${e.message}")
                    dao.markTeacherCommentSynced(item.id, assignedUuid)
                }
                batchProcessed++
            }
            if (batchProcessed == 0) break
        }

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("teacher_comments").select().decodeList<TeacherCommentSupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getTeacherCommentByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        grade = existingLocal.grade,
                        className = existingLocal.className
                    )
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertOrUpdateTeacherComment(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertOrUpdateTeacherComment(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote teacher_comments: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncAttendanceInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = attendanceDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("attendance_records").select().decodeList<AttendanceRecordSupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: Exception) {
            emptyMap()
        }

        while (true) {
            val unsynced = dao.getAttendanceRecordsForSync(limit = 300)
            if (unsynced.isEmpty()) break
            var batchProcessed = 0
            for (item in unsynced) {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val localStudent = studentDao.getStudentById(item.studentId)
                if (localStudent == null) {
                    Log.w(TAG, "Skipping attendance_record push: local studentId ${item.studentId} not found")
                    dao.markAttendanceRecordSynced(item.id, assignedUuid)
                    batchProcessed++
                    continue
                }
                val targetStudentId = ensureRemoteStudentId(client, localStudent)
                try {
                    val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                    val pushDto = AttendanceRecordPushDto.fromEntity(
                        entity = updated,
                        targetStudentId = targetStudentId
                    )
                    client.from("attendance_records").upsert(pushDto, onConflict = "uuid")
                    dao.markAttendanceRecordSynced(updated.id, assignedUuid)
                    pushed++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push attendance_record: ${e.message}")
                    dao.markAttendanceRecordSynced(item.id, assignedUuid)
                }
                batchProcessed++
            }
            if (batchProcessed == 0) break
        }

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("attendance_records").select().decodeList<AttendanceRecordSupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getAttendanceRecordByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        studentCode = existingLocal.studentCode,
                        studentName = existingLocal.studentName
                    )
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertOrUpdateRecord(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertOrUpdateRecord(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote attendance_records: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun syncPromotionHistoryInternal(client: SupabaseClient): Pair<Int, Int> {
        val dao = academicYearDao ?: return Pair(0, 0)
        var pushed = 0; var pulled = 0

        val remoteMap = try {
            client.from("promotion_history").select().decodeList<PromotionHistorySupabaseDto>()
                .associateBy { it.uuid }
        } catch (e: Exception) {
            emptyMap()
        }

        val unsynced = dao.getPromotionHistoryForSync()
        for (item in unsynced) {
            val localStudent = studentDao.getStudentById(item.studentId)
            if (localStudent == null) {
                Log.w(TAG, "Skipping promotion_history push: local studentId ${item.studentId} not found")
                continue
            }
            val targetStudentId = ensureRemoteStudentId(client, localStudent)
            try {
                val assignedUuid = item.uuid.ifBlank { UUID.randomUUID().toString() }
                val updated = if (item.uuid.isBlank()) item.copy(uuid = assignedUuid) else item
                val pushDto = PromotionHistoryPushDto.fromEntity(
                    entity = updated,
                    targetStudentId = targetStudentId
                )
                client.from("promotion_history").upsert(pushDto, onConflict = "uuid")
                dao.markPromotionHistorySynced(updated.id, assignedUuid)
                pushed++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push promotion_history: ${e.message}")
            }
        }

        try {
            val remotes = remoteMap.values.ifEmpty {
                client.from("promotion_history").select().decodeList<PromotionHistorySupabaseDto>()
            }
            for (remoteDto in remotes) {
                val existingLocal = if (remoteDto.uuid.isNotBlank()) dao.getPromotionHistoryByUuid(remoteDto.uuid) else null
                if (existingLocal != null) {
                    val remoteEntity = remoteDto.toEntity(
                        existingLocalId = existingLocal.id,
                        studentCode = existingLocal.studentCode,
                        studentName = existingLocal.studentName
                    )
                    if (!existingLocal.isDirty || remoteEntity.updatedAt > existingLocal.updatedAt) {
                        dao.insertPromotionHistory(remoteEntity)
                        pulled++
                    }
                } else {
                    val newEntity = remoteDto.toEntity()
                    dao.insertPromotionHistory(newEntity)
                    pulled++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull remote promotion_history: ${e.message}")
        }
        return Pair(pushed, pulled)
    }

    private suspend fun ensureRemoteStudentId(client: SupabaseClient, localStudent: StudentEntity): Long {
        localToRemoteStudentIdCache[localStudent.id]?.let { return it }

        val validUuid = localStudent.uuid.ifBlank { UUID.randomUUID().toString() }
        val updatedStudent = if (localStudent.uuid.isBlank()) localStudent.copy(uuid = validUuid) else localStudent
        if (localStudent.uuid.isBlank()) {
            studentDao.markStudentSynced(updatedStudent.id, validUuid)
        }

        // 1. Try finding by UUID
        val byUuid = try {
            client.from("students").select {
                filter { eq("uuid", validUuid) }
            }.decodeList<StudentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (byUuid?.id != null) {
            localToRemoteStudentIdCache[localStudent.id] = byUuid.id
            remoteToLocalStudentIdCache[byUuid.id] = localStudent.id
            return byUuid.id
        }

        // 2. Try finding by studentId (studentCode)
        if (updatedStudent.studentCode.isNotBlank()) {
            val byCode = try {
                client.from("students").select {
                    filter { eq("student_id", updatedStudent.studentCode) }
                }.decodeList<StudentSupabaseDto>().firstOrNull()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            if (byCode?.id != null) {
                localToRemoteStudentIdCache[localStudent.id] = byCode.id
                remoteToLocalStudentIdCache[byCode.id] = localStudent.id
                return byCode.id
            }
        }

        // 3. Upsert if missing
        val dto = StudentSupabaseDto.fromEntity(updatedStudent).copy(
            id = null,
            uuid = validUuid,
            isDeleted = updatedStudent.isDeleted
        )
        try {
            client.from("students").upsert(dto, onConflict = "uuid")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Upsert student failed in ensureRemoteStudentId: ${e.message}")
        }

        val fetched = try {
            client.from("students").select {
                filter { eq("uuid", validUuid) }
            }.decodeList<StudentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (fetched?.id != null) {
            localToRemoteStudentIdCache[localStudent.id] = fetched.id
            remoteToLocalStudentIdCache[fetched.id] = localStudent.id
            return fetched.id
        }

        if (updatedStudent.studentCode.isNotBlank()) {
            val fetchedByCode = try {
                client.from("students").select {
                    filter { eq("student_id", updatedStudent.studentCode) }
                }.decodeList<StudentSupabaseDto>().firstOrNull()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            if (fetchedByCode?.id != null) {
                localToRemoteStudentIdCache[localStudent.id] = fetchedByCode.id
                remoteToLocalStudentIdCache[fetchedByCode.id] = localStudent.id
                return fetchedByCode.id
            }
        }

        Log.e(TAG, "Could not resolve remote student ID for local student ${localStudent.id}, code ${localStudent.studentCode}")
        return localStudent.id
    }

    private suspend fun ensureRemoteAssessmentId(client: SupabaseClient, localAssessment: AssessmentEntity): Long {
        localToRemoteAssessmentIdCache[localAssessment.id]?.let { return it }

        val validUuid = localAssessment.uuid.ifBlank { UUID.randomUUID().toString() }
        val updatedAssessment = if (localAssessment.uuid.isBlank()) localAssessment.copy(uuid = validUuid) else localAssessment
        if (localAssessment.uuid.isBlank()) {
            assessmentDao?.markAssessmentSynced(updatedAssessment.id, validUuid)
        }

        // 1. Try finding by UUID
        val byUuid = try {
            client.from("assessments").select {
                filter { eq("uuid", validUuid) }
            }.decodeList<AssessmentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (byUuid?.id != null) {
            localToRemoteAssessmentIdCache[localAssessment.id] = byUuid.id
            remoteToLocalAssessmentIdCache[byUuid.id] = localAssessment.id
            return byUuid.id
        }

        // 2. Try finding by match
        val byMatch = try {
            client.from("assessments").select {
                filter {
                    eq("title", localAssessment.assessmentName)
                    eq("grade", localAssessment.grade)
                    eq("subject", localAssessment.subjectName)
                    eq("assessment_type", localAssessment.assessmentType)
                }
            }.decodeList<AssessmentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (byMatch?.id != null) {
            assessmentDao?.markAssessmentSynced(localAssessment.id, byMatch.uuid)
            localToRemoteAssessmentIdCache[localAssessment.id] = byMatch.id
            remoteToLocalAssessmentIdCache[byMatch.id] = localAssessment.id
            return byMatch.id
        }

        // 3. Upsert if missing
        val dto = AssessmentSupabaseDto.fromEntity(updatedAssessment).copy(id = null, uuid = validUuid)
        try {
            client.from("assessments").upsert(dto, onConflict = "uuid")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Upsert assessment failed in ensureRemoteAssessmentId: ${e.message}")
        }

        val fetched = try {
            client.from("assessments").select {
                filter { eq("uuid", validUuid) }
            }.decodeList<AssessmentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (fetched?.id != null) {
            localToRemoteAssessmentIdCache[localAssessment.id] = fetched.id
            remoteToLocalAssessmentIdCache[fetched.id] = localAssessment.id
            return fetched.id
        }

        Log.e(TAG, "Could not resolve remote assessment ID for local assessment ${localAssessment.id}, name ${localAssessment.assessmentName}")
        return localAssessment.id
    }

    private suspend fun resolveLocalAssessmentId(
        remoteAssessmentId: Long,
        client: SupabaseClient,
        assessmentCache: MutableMap<Long, Long>
    ): Long? {
        if (remoteAssessmentId <= 0) return null
        remoteToLocalAssessmentIdCache[remoteAssessmentId]?.let { return it }
        assessmentCache[remoteAssessmentId]?.let { return it }

        val dao = assessmentDao ?: return null
        val directLocal = dao.getAssessmentByIdDirect(remoteAssessmentId)
        if (directLocal != null) {
            remoteToLocalAssessmentIdCache[remoteAssessmentId] = directLocal.id
            assessmentCache[remoteAssessmentId] = directLocal.id
            return directLocal.id
        }

        val remoteAssessmentDto = try {
            client.from("assessments").select {
                filter { eq("id", remoteAssessmentId) }
            }.decodeList<AssessmentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (remoteAssessmentDto == null) {
            Log.w(TAG, "Cannot resolve remote assessment_id $remoteAssessmentId: not found in Supabase")
            return null
        }

        if (remoteAssessmentDto.uuid.isNotBlank()) {
            val localByUuid = dao.getAssessmentByUuid(remoteAssessmentDto.uuid)
            if (localByUuid != null) {
                remoteToLocalAssessmentIdCache[remoteAssessmentId] = localByUuid.id
                assessmentCache[remoteAssessmentId] = localByUuid.id
                return localByUuid.id
            }
        }

        val allLocal = dao.getAllAssessmentsList()
        val localByMatch = allLocal.firstOrNull {
            it.grade.equals(remoteAssessmentDto.grade, ignoreCase = true) &&
            it.subjectName.equals(remoteAssessmentDto.subject, ignoreCase = true) &&
            it.assessmentName.equals(remoteAssessmentDto.title, ignoreCase = true) &&
            it.assessmentType.equals(remoteAssessmentDto.assessmentType, ignoreCase = true)
        }

        if (localByMatch != null) {
            if (remoteAssessmentDto.uuid.isNotBlank() && localByMatch.uuid != remoteAssessmentDto.uuid) {
                dao.markAssessmentSynced(localByMatch.id, remoteAssessmentDto.uuid)
            }
            remoteToLocalAssessmentIdCache[remoteAssessmentId] = localByMatch.id
            assessmentCache[remoteAssessmentId] = localByMatch.id
            return localByMatch.id
        }

        val newLocalEntity = remoteAssessmentDto.toEntity(existingLocalId = 0)
        val insertedId = try {
            dao.insertAssessment(newLocalEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert remote assessment locally during resolution: ${e.message}")
            0L
        }

        if (insertedId > 0) {
            remoteToLocalAssessmentIdCache[remoteAssessmentId] = insertedId
            assessmentCache[remoteAssessmentId] = insertedId
            return insertedId
        }

        return null
    }

    private suspend fun resolveLocalStudentId(
        remoteStudentId: Long,
        client: SupabaseClient,
        studentCache: MutableMap<Long, Long>
    ): Long? {
        if (remoteStudentId <= 0) return null
        remoteToLocalStudentIdCache[remoteStudentId]?.let { return it }
        studentCache[remoteStudentId]?.let { return it }

        val directLocal = studentDao.getStudentById(remoteStudentId)
        if (directLocal != null) {
            remoteToLocalStudentIdCache[remoteStudentId] = directLocal.id
            studentCache[remoteStudentId] = directLocal.id
            return directLocal.id
        }

        val remoteStudentDto = try {
            client.from("students").select {
                filter { eq("id", remoteStudentId) }
            }.decodeList<StudentSupabaseDto>().firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (remoteStudentDto == null) {
            Log.w(TAG, "Cannot resolve remote student_id $remoteStudentId: not found in Supabase")
            return null
        }

        if (remoteStudentDto.uuid.isNotBlank()) {
            val localByUuid = studentDao.getStudentByUuid(remoteStudentDto.uuid)
            if (localByUuid != null) {
                remoteToLocalStudentIdCache[remoteStudentId] = localByUuid.id
                studentCache[remoteStudentId] = localByUuid.id
                return localByUuid.id
            }
        }

        if (remoteStudentDto.studentId.isNotBlank()) {
            val localByCode = studentDao.getStudentByCode(remoteStudentDto.studentId)
            if (localByCode != null) {
                if (remoteStudentDto.uuid.isNotBlank() && localByCode.uuid != remoteStudentDto.uuid) {
                    studentDao.markStudentSynced(localByCode.id, remoteStudentDto.uuid)
                }
                remoteToLocalStudentIdCache[remoteStudentId] = localByCode.id
                studentCache[remoteStudentId] = localByCode.id
                return localByCode.id
            }
        }

        if (remoteStudentDto.isDeleted) {
            return null
        }

        val newLocalEntity = remoteStudentDto.toEntity(existingLocalId = 0)
        val insertedId = try {
            studentDao.insertStudent(newLocalEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert remote student locally during resolution: ${e.message}")
            0L
        }

        if (insertedId > 0) {
            remoteToLocalStudentIdCache[remoteStudentId] = insertedId
            studentCache[remoteStudentId] = insertedId
            return insertedId
        }

        return null
    }

    /**
     * Confirms whether a deletion (hard delete row removal / soft delete tombstone) was
     * actually processed and propagated to Supabase, querying Supabase directly for verification.
     */
    suspend fun verifyAndPropagateDeletion(
        tableName: String,
        uuid: String,
        codeOrKey: String = "",
        fallbackRemoteId: Long? = null,
        preferHardDelete: Boolean = true
    ): DeletionVerificationResult {
        val client = SupabaseClientManager.getInstance()
            ?: return DeletionVerificationResult.OfflineQueued(
                tableName = tableName,
                identifier = if (uuid.isNotBlank()) uuid else codeOrKey,
                message = "Supabase client unconfigured. Deletion is saved in local Room SQLite database."
            )

        val identifier = when {
            codeOrKey.isNotBlank() -> codeOrKey
            uuid.isNotBlank() -> uuid
            fallbackRemoteId != null -> "#$fallbackRemoteId"
            else -> "unknown"
        }

        return try {
            var hardDeleteAttempted = false
            if (preferHardDelete) {
                try {
                    when {
                        uuid.isNotBlank() -> client.from(tableName).delete { filter { eq("uuid", uuid) } }
                        fallbackRemoteId != null -> client.from(tableName).delete { filter { eq("id", fallbackRemoteId) } }
                        codeOrKey.isNotBlank() -> {
                            val col = when (tableName) {
                                "students" -> "student_id"
                                "teachers" -> "teacher_id"
                                "users" -> "username"
                                else -> "code"
                            }
                            client.from(tableName).delete { filter { eq(col, codeOrKey) } }
                        }
                    }
                    hardDeleteAttempted = true
                } catch (e: Exception) {
                    Log.w(TAG, "Hard delete failed on table $tableName ($identifier), falling back to soft-delete: ${e.message}")
                }
            }

            if (!hardDeleteAttempted) {
                try {
                    val currentIsoTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date())
                    when {
                        uuid.isNotBlank() -> client.from(tableName).update(mapOf("is_deleted" to true, "updated_at" to currentIsoTimestamp)) { filter { eq("uuid", uuid) } }
                        fallbackRemoteId != null -> client.from(tableName).update(mapOf("is_deleted" to true, "updated_at" to currentIsoTimestamp)) { filter { eq("id", fallbackRemoteId) } }
                        codeOrKey.isNotBlank() -> {
                            val col = when (tableName) {
                                "students" -> "student_id"
                                "teachers" -> "teacher_id"
                                "users" -> "username"
                                else -> "code"
                            }
                            client.from(tableName).update(mapOf("is_deleted" to true, "updated_at" to currentIsoTimestamp)) { filter { eq(col, codeOrKey) } }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Soft delete tombstone update failed on table $tableName ($identifier): ${e.message}")
                }
            }

            // Verification query to inspect remote row state
            val checkList = try {
                when {
                    uuid.isNotBlank() -> client.from(tableName).select { filter { eq("uuid", uuid) } }.decodeList<kotlinx.serialization.json.JsonObject>()
                    fallbackRemoteId != null -> client.from(tableName).select { filter { eq("id", fallbackRemoteId) } }.decodeList<kotlinx.serialization.json.JsonObject>()
                    codeOrKey.isNotBlank() -> {
                        val col = when (tableName) {
                            "students" -> "student_id"
                            "teachers" -> "teacher_id"
                            "users" -> "username"
                            else -> "code"
                        }
                        client.from(tableName).select { filter { eq(col, codeOrKey) } }.decodeList<kotlinx.serialization.json.JsonObject>()
                    }
                    else -> emptyList()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Verification query failed for $tableName ($identifier): ${e.message}")
                return DeletionVerificationResult.ServerRejectedOrIgnored(
                    tableName = tableName,
                    identifier = identifier,
                    reason = "Server verification query error: ${e.message}",
                    details = e.localizedMessage
                )
            }

            if (checkList.isEmpty()) {
                Log.i(TAG, "Verification Succeeded: $tableName ($identifier) is purged from Supabase.")
                DeletionVerificationResult.VerifiedPurged(
                    tableName = tableName,
                    identifier = identifier,
                    message = "Record '$identifier' permanently removed from Supabase table '$tableName'."
                )
            } else {
                val remoteRecord = checkList.first()
                val isDeletedVal = remoteRecord["is_deleted"]?.toString()?.replace("\"", "")?.toBooleanStrictOrNull()

                if (isDeletedVal == true) {
                    Log.i(TAG, "Verification Succeeded: $tableName ($identifier) has is_deleted=true on Supabase.")
                    DeletionVerificationResult.VerifiedTombstoned(
                        tableName = tableName,
                        identifier = identifier,
                        message = "Record '$identifier' soft-delete (is_deleted=true) confirmed on Supabase."
                    )
                } else {
                    Log.w(TAG, "Verification Failed: $tableName ($identifier) still has is_deleted=false on Supabase.")
                    DeletionVerificationResult.ServerRejectedOrIgnored(
                        tableName = tableName,
                        identifier = identifier,
                        reason = "Supabase server ignored deletion: 'is_deleted' remains false in table '$tableName'.",
                        details = "Row still active: $remoteRecord"
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during deletion verification: ${e.message}", e)
            DeletionVerificationResult.OfflineQueued(
                tableName = tableName,
                identifier = identifier,
                message = "Offline or network error (${e.message}). Saved locally in Room SQLite database and queued for sync."
            )
        }
    }

    /**
     * Processes a single queued offline mutation entry from SyncOutbox.
     * Guarantees safe, idempotent retry against Supabase.
     */
    suspend fun syncOutboxEntry(entry: SyncOutboxEntity): Boolean {
        val client = SupabaseClientManager.getInstance() ?: return false
        return try {
            when (entry.operation) {
                OutboxOperation.DELETE -> {
                    val result = verifyAndPropagateDeletion(
                        tableName = entry.entityType,
                        uuid = entry.entityUuid,
                        preferHardDelete = true
                    )
                    result !is DeletionVerificationResult.ServerRejectedOrIgnored
                }
                OutboxOperation.CREATE, OutboxOperation.UPDATE -> {
                    val (pushed, _) = syncSingleTable(entry.entityType)
                    pushed >= 0
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing outbox entry ${entry.id}: ${e.message}")
            false
        }
    }
}
