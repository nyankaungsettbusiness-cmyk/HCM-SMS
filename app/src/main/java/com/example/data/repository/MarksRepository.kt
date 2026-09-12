package com.example.data.repository

import com.example.data.local.dao.MarksDao
import com.example.data.local.dao.SchoolPolicyDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AssessmentLockStatusEntity
import com.example.data.local.entity.AssessmentResultSummaryEntity
import com.example.data.local.entity.EducationLevel
import com.example.data.local.entity.StudentMarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MarksRepository(
    private val marksDao: MarksDao,
    private val studentDao: StudentDao,
    private val schoolPolicyDao: SchoolPolicyDao
) {

    fun getAllSubjects(): Flow<List<com.example.data.local.entity.SubjectEntity>> {
        return schoolPolicyDao.getAllSubjects()
    }

    fun getMarksForAssessmentAndSubject(assessmentId: Long, subjectName: String): Flow<List<StudentMarkEntity>> {
        return marksDao.getMarksForAssessmentAndSubject(assessmentId, subjectName)
    }

    fun getMarksForAssessment(assessmentId: Long): Flow<List<StudentMarkEntity>> {
        return marksDao.getMarksForAssessment(assessmentId)
    }

    fun getLockStatus(assessmentId: Long): Flow<AssessmentLockStatusEntity?> {
        return marksDao.getLockStatus(assessmentId)
    }

    suspend fun saveMarks(marks: List<StudentMarkEntity>) {
        val dirtyMarks = marks.map {
            it.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        }
        marksDao.insertOrUpdateMarks(dirtyMarks)
        triggerBackgroundSync()
    }

    suspend fun saveMark(mark: StudentMarkEntity) {
        val dirtyMark = mark.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        marksDao.insertOrUpdateMark(dirtyMark)
        triggerBackgroundSync()
    }

    suspend fun lockAssessment(assessmentId: Long, lockedBy: String, note: String = "") {
        val status = AssessmentLockStatusEntity(
            assessmentId = assessmentId,
            isLocked = true,
            lockedBy = lockedBy,
            lockedAt = System.currentTimeMillis(),
            lockNote = note
        )
        marksDao.insertOrUpdateLockStatus(status)
        triggerBackgroundSync()
    }

    suspend fun unlockAssessment(assessmentId: Long, unlockedBy: String) {
        val status = AssessmentLockStatusEntity(
            assessmentId = assessmentId,
            isLocked = false,
            lockedBy = unlockedBy,
            lockedAt = System.currentTimeMillis(),
            lockNote = "Unlocked by $unlockedBy"
        )
        marksDao.insertOrUpdateLockStatus(status)
        triggerBackgroundSync()
    }

    fun getResultSummaries(assessmentId: Long): Flow<List<AssessmentResultSummaryEntity>> {
        return marksDao.getResultSummaries(assessmentId)
    }

    suspend fun saveResultSummary(summary: AssessmentResultSummaryEntity) {
        val dirtySummary = summary.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        marksDao.insertOrUpdateResultSummary(dirtySummary)
        triggerBackgroundSync()
    }

    private fun triggerBackgroundSync() {
        com.example.data.sync.SyncManager.triggerTableSyncAsync("student_marks")
    }

    // Dynamic Policy Calculation Rules
    fun getPassMark(educationLevel: EducationLevel, subjectName: String): Int {
        return 40 // Default standard pass mark from policy
    }

    fun getDistinctionMark(educationLevel: EducationLevel, subjectName: String): Int {
        val subjectLower = subjectName.lowercase()
        return when (educationLevel) {
            EducationLevel.KINDERGARTEN, EducationLevel.PRIMARY, EducationLevel.SECONDARY -> {
                if (subjectLower.contains("math")) 80 else 75
            }
            EducationLevel.HIGH_SCHOOL -> {
                when {
                    subjectLower.contains("myanmar") -> 75
                    subjectLower.contains("english") -> 75
                    else -> 80
                }
            }
        }
    }
}
