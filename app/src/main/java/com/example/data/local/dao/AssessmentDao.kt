package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AssessmentScheduleEntity
import com.example.data.local.entity.AssessmentStatus
import com.example.data.local.entity.AssessmentSubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessments WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllAssessments(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getAllAssessmentsList(): List<AssessmentEntity>

    @Query("SELECT * FROM assessments ORDER BY id DESC")
    suspend fun getAllAssessmentsIncludingDeleted(): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE isDeleted = 1 ORDER BY id DESC")
    suspend fun getSoftDeletedAssessments(): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 AND (:academicYear = '' OR :academicYear = 'ALL' OR academicYear = :academicYear) ORDER BY id DESC")
    fun getAssessmentsByAcademicYear(academicYear: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 AND (:academicYear = '' OR :academicYear = 'ALL' OR academicYear = :academicYear) ORDER BY id DESC")
    suspend fun getAssessmentsByAcademicYearList(academicYear: String): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE id = :id LIMIT 1")
    fun getAssessmentById(id: Long): Flow<AssessmentEntity?>

    @Query("SELECT * FROM assessments WHERE id = :id LIMIT 1")
    suspend fun getAssessmentByIdDirect(id: Long): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 AND grade = :grade ORDER BY id DESC")
    fun getAssessmentsByGrade(grade: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 AND grade = :grade AND (:academicYear = '' OR :academicYear = 'ALL' OR academicYear = :academicYear) ORDER BY id DESC")
    fun getAssessmentsByGradeAndYear(grade: String, academicYear: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 AND status = :status ORDER BY id DESC")
    fun getAssessmentsByStatus(status: AssessmentStatus): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE isDeleted = 0 AND status = :status AND (:academicYear = '' OR :academicYear = 'ALL' OR academicYear = :academicYear) ORDER BY id DESC")
    fun getAssessmentsByStatusAndYear(status: AssessmentStatus, academicYear: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE isDirty = 1")
    suspend fun getAssessmentsForSync(): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE uuid = :uuid LIMIT 1")
    suspend fun getAssessmentByUuid(uuid: String): AssessmentEntity?

    @Query("UPDATE assessments SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markAssessmentSynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Update
    suspend fun updateAssessment(assessment: AssessmentEntity)

    @Query("UPDATE assessments SET isDeleted = 1, isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteAssessment(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM assessments WHERE id = :id")
    suspend fun deleteAssessment(id: Long)

    @Query("UPDATE assessments SET status = :status WHERE id = :id")
    suspend fun updateAssessmentStatus(id: Long, status: AssessmentStatus)

    // Schedule operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentSchedule(schedule: AssessmentScheduleEntity): Long

    @Query("SELECT * FROM assessment_schedules WHERE assessmentId = :assessmentId LIMIT 1")
    fun getScheduleForAssessment(assessmentId: Long): Flow<AssessmentScheduleEntity?>

    // Subject operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentSubject(subject: AssessmentSubjectEntity): Long

    @Query("SELECT * FROM assessment_subjects WHERE assessmentId = :assessmentId")
    fun getSubjectsForAssessment(assessmentId: Long): Flow<List<AssessmentSubjectEntity>>
}
