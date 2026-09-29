package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ExamQuestionBlueprintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamQuestionBlueprintDao {
    @Query("SELECT * FROM exam_question_blueprints WHERE isDeleted = 0 ORDER BY gradeName ASC, subjectName ASC")
    fun getAllBlueprints(): Flow<List<ExamQuestionBlueprintEntity>>

    @Query("SELECT * FROM exam_question_blueprints WHERE gradeName = :gradeName AND LOWER(subjectName) = LOWER(:subjectName) AND isDeleted = 0 LIMIT 1")
    fun getBlueprintFlow(gradeName: String, subjectName: String): Flow<ExamQuestionBlueprintEntity?>

    @Query("SELECT * FROM exam_question_blueprints WHERE gradeName = :gradeName AND LOWER(subjectName) = LOWER(:subjectName) AND isDeleted = 0 LIMIT 1")
    suspend fun getBlueprint(gradeName: String, subjectName: String): ExamQuestionBlueprintEntity?

    @Query("SELECT * FROM exam_question_blueprints WHERE gradeName = :gradeName AND isDeleted = 0")
    fun getBlueprintsForGrade(gradeName: String): Flow<List<ExamQuestionBlueprintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBlueprint(blueprint: ExamQuestionBlueprintEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlueprints(blueprints: List<ExamQuestionBlueprintEntity>)

    @Query("UPDATE exam_question_blueprints SET isDeleted = 1, isDirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteBlueprint(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM exam_question_blueprints WHERE gradeName = :gradeName")
    suspend fun deleteBlueprintsForGrade(gradeName: String)

    @Query("SELECT * FROM exam_question_blueprints WHERE isDirty = 1")
    suspend fun getBlueprintsForSync(): List<ExamQuestionBlueprintEntity>

    @Query("SELECT * FROM exam_question_blueprints WHERE isDeleted = 0")
    suspend fun getAllBlueprintsSync(): List<ExamQuestionBlueprintEntity>
}
