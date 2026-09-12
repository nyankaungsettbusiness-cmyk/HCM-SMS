package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AssessmentLockStatusEntity
import com.example.data.local.entity.AssessmentResultSummaryEntity
import com.example.data.local.entity.StudentMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarksDao {
    @Query("SELECT * FROM student_marks WHERE assessmentId = :assessmentId ORDER BY rollNo ASC")
    fun getMarksForAssessment(assessmentId: Long): Flow<List<StudentMarkEntity>>

    @Query("""
        SELECT * FROM student_marks 
        WHERE assessmentId = :assessmentId 
          AND (
            subjectName = :subjectName 
            OR (:subjectName = 'Biology' AND (subjectName LIKE 'Biology%' OR subjectName = 'Biology / Computer Science'))
            OR (:subjectName = 'Economics' AND (subjectName LIKE '%Economics%' OR subjectName LIKE '%History & Economics%' OR subjectName LIKE '%Economics / History%' OR subjectName LIKE '%Economics/History%'))
          ) 
        ORDER BY rollNo ASC
    """)
    fun getMarksForAssessmentAndSubject(assessmentId: Long, subjectName: String): Flow<List<StudentMarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMark(mark: StudentMarkEntity): Long

    @Query("SELECT * FROM student_marks WHERE isDirty = 1 LIMIT :limit")
    suspend fun getStudentMarksForSync(limit: Int = 300): List<StudentMarkEntity>

    @Query("SELECT * FROM student_marks WHERE uuid = :uuid LIMIT 1")
    suspend fun getStudentMarkByUuid(uuid: String): StudentMarkEntity?

    @Query("UPDATE student_marks SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markStudentMarkSynced(id: Long, uuid: String)

    @Query("SELECT * FROM student_marks")
    fun getAllMarks(): Flow<List<StudentMarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMarks(marks: List<StudentMarkEntity>)

    @Query("DELETE FROM student_marks WHERE assessmentId = :assessmentId")
    suspend fun deleteMarksForAssessment(assessmentId: Long)

    // Lock Status
    @Query("SELECT * FROM assessment_lock_statuses WHERE assessmentId = :assessmentId LIMIT 1")
    fun getLockStatus(assessmentId: Long): Flow<AssessmentLockStatusEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLockStatus(lockStatus: AssessmentLockStatusEntity)

    @Query("UPDATE assessment_lock_statuses SET isLocked = :isLocked, lockedBy = :lockedBy, lockedAt = :lockedAt WHERE assessmentId = :assessmentId")
    suspend fun updateLockStatus(assessmentId: Long, isLocked: Boolean, lockedBy: String, lockedAt: Long)

    // Result Summaries
    @Query("SELECT * FROM assessment_results WHERE assessmentId = :assessmentId ORDER BY rollNo ASC")
    fun getResultSummaries(assessmentId: Long): Flow<List<AssessmentResultSummaryEntity>>

    @Query("SELECT * FROM assessment_results WHERE isDirty = 1 LIMIT :limit")
    suspend fun getAssessmentResultsForSync(limit: Int = 300): List<AssessmentResultSummaryEntity>

    @Query("SELECT * FROM assessment_results WHERE uuid = :uuid LIMIT 1")
    suspend fun getAssessmentResultByUuid(uuid: String): AssessmentResultSummaryEntity?

    @Query("UPDATE assessment_results SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markAssessmentResultSynced(id: Long, uuid: String)

    @Query("SELECT * FROM assessment_results ORDER BY id ASC")
    fun getAllAssessmentResults(): Flow<List<AssessmentResultSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateResultSummary(summary: AssessmentResultSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateResultSummaries(summaries: List<AssessmentResultSummaryEntity>)
}
