package com.example.data.repository

import com.example.data.local.dao.AssessmentDao
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AssessmentScheduleEntity
import com.example.data.local.entity.AssessmentStatus
import com.example.data.local.entity.AssessmentSubjectEntity
import com.example.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class AssessmentRepository(private val assessmentDao: AssessmentDao) {

    val allAssessments: Flow<List<AssessmentEntity>> = assessmentDao.getAllAssessments()
 
    fun getAssessmentsForAcademicYear(academicYear: String): Flow<List<AssessmentEntity>> {
        return if (academicYear.isBlank() || academicYear.equals("ALL", ignoreCase = true)) {
            assessmentDao.getAllAssessments()
        } else {
            assessmentDao.getAssessmentsByAcademicYear(academicYear)
        }
    }

    suspend fun getAssessmentsByAcademicYearList(academicYear: String): List<AssessmentEntity> {
        return if (academicYear.isBlank() || academicYear.equals("ALL", ignoreCase = true)) {
            assessmentDao.getAllAssessmentsList()
        } else {
            assessmentDao.getAssessmentsByAcademicYearList(academicYear)
        }
    }

    fun getAssessmentById(id: Long): Flow<AssessmentEntity?> = assessmentDao.getAssessmentById(id)

    fun getAssessmentsByGrade(grade: String, academicYear: String? = null): Flow<List<AssessmentEntity>> {
        return if (academicYear.isNullOrBlank() || academicYear.equals("ALL", ignoreCase = true)) {
            assessmentDao.getAssessmentsByGrade(grade)
        } else {
            assessmentDao.getAssessmentsByGradeAndYear(grade, academicYear)
        }
    }

    fun getAssessmentsByStatus(status: AssessmentStatus, academicYear: String? = null): Flow<List<AssessmentEntity>> {
        return if (academicYear.isNullOrBlank() || academicYear.equals("ALL", ignoreCase = true)) {
            assessmentDao.getAssessmentsByStatus(status)
        } else {
            assessmentDao.getAssessmentsByStatusAndYear(status, academicYear)
        }
    }

    suspend fun saveAssessment(assessment: AssessmentEntity): Long {
        val dirtyEntity = assessment.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val id = if (assessment.id == 0L) {
            assessmentDao.insertAssessment(dirtyEntity)
        } else {
            assessmentDao.updateAssessment(dirtyEntity)
            assessment.id
        }
        triggerBackgroundSync()
        return id
    }

    suspend fun deleteAssessment(id: Long) {
        val assessment = assessmentDao.getAssessmentByIdDirect(id)
        if (assessment != null) {
            val updated = assessment.copy(
                isDeleted = true,
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            )
            assessmentDao.updateAssessment(updated)
        } else {
            assessmentDao.softDeleteAssessment(id)
        }
        triggerBackgroundSync()
    }

    suspend fun updateAssessmentStatus(id: Long, status: AssessmentStatus) {
        assessmentDao.updateAssessmentStatus(id, status)
        triggerBackgroundSync()
    }

    suspend fun duplicateAssessment(assessment: AssessmentEntity, currentUserName: String): Long {
        val copy = assessment.copy(
            id = 0L,
            assessmentName = "${assessment.assessmentName} (Copy)",
            status = AssessmentStatus.DRAFT,
            createdBy = currentUserName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDirty = true
        )
        val newId = assessmentDao.insertAssessment(copy)
        triggerBackgroundSync()
        return newId
    }

    // Schedule & Subjects
    suspend fun saveSchedule(schedule: AssessmentScheduleEntity): Long = assessmentDao.insertAssessmentSchedule(schedule)

    fun getSchedule(assessmentId: Long): Flow<AssessmentScheduleEntity?> = assessmentDao.getScheduleForAssessment(assessmentId)

    suspend fun saveSubject(subject: AssessmentSubjectEntity): Long = assessmentDao.insertAssessmentSubject(subject)

    fun getSubjects(assessmentId: Long): Flow<List<AssessmentSubjectEntity>> = assessmentDao.getSubjectsForAssessment(assessmentId)

    private fun triggerBackgroundSync() {
        SyncManager.triggerTableSyncAsync("assessments")
    }
}
