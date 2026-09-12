package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HolisticDao {
    // Holistic Categories
    @Query("SELECT * FROM holistic_categories ORDER BY orderIndex ASC, id ASC")
    fun getAllHolisticCategories(): Flow<List<HolisticCategoryEntity>>

    @Query("SELECT * FROM holistic_categories WHERE isDirty = 1")
    suspend fun getHolisticCategoriesForSync(): List<HolisticCategoryEntity>

    @Query("SELECT * FROM holistic_categories WHERE uuid = :uuid LIMIT 1")
    suspend fun getHolisticCategoryByUuid(uuid: String): HolisticCategoryEntity?

    @Query("UPDATE holistic_categories SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markHolisticCategorySynced(id: Long, uuid: String)

    @Query("SELECT * FROM holistic_categories WHERE isEnabled = 1 ORDER BY orderIndex ASC, id ASC")
    fun getEnabledHolisticCategories(): Flow<List<HolisticCategoryEntity>>

    @Query("SELECT * FROM holistic_categories WHERE educationLevel = :educationLevel ORDER BY orderIndex ASC, id ASC")
    fun getHolisticCategoriesForLevel(educationLevel: String): Flow<List<HolisticCategoryEntity>>

    @Query("SELECT * FROM holistic_categories WHERE educationLevel = :educationLevel AND isEnabled = 1 ORDER BY orderIndex ASC, id ASC")
    fun getEnabledHolisticCategoriesForLevel(educationLevel: String): Flow<List<HolisticCategoryEntity>>

    @Query("SELECT * FROM holistic_categories WHERE educationLevel = :educationLevel")
    suspend fun getHolisticCategoriesForLevelList(educationLevel: String): List<HolisticCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolisticCategory(category: HolisticCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolisticCategories(categories: List<HolisticCategoryEntity>)

    @Update
    suspend fun updateHolisticCategory(category: HolisticCategoryEntity)

    @Delete
    suspend fun deleteHolisticCategory(category: HolisticCategoryEntity)

    @Query("DELETE FROM holistic_categories WHERE educationLevel = :targetLevel")
    suspend fun deleteHolisticCategoriesForLevel(targetLevel: String)

    // Holistic Results
    @Query("SELECT * FROM holistic_results WHERE studentId = :studentId AND assessmentPeriod = :period AND academicYear = :academicYear")
    fun getHolisticResultsForStudent(studentId: Long, period: String, academicYear: String): Flow<List<HolisticResultEntity>>

    @Query("SELECT * FROM holistic_results WHERE isDirty = 1 LIMIT :limit")
    suspend fun getHolisticResultsForSync(limit: Int = 300): List<HolisticResultEntity>

    @Query("SELECT * FROM holistic_results WHERE uuid = :uuid LIMIT 1")
    suspend fun getHolisticResultByUuid(uuid: String): HolisticResultEntity?

    @Query("UPDATE holistic_results SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markHolisticResultSynced(id: Long, uuid: String)

    @Query("SELECT * FROM holistic_results")
    fun getAllHolisticResults(): Flow<List<HolisticResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHolisticResults(results: List<HolisticResultEntity>)

    // SGI Categories
    @Query("SELECT * FROM sgi_categories ORDER BY orderIndex ASC, id ASC")
    fun getAllSgiCategories(): Flow<List<SgiCategoryEntity>>

    @Query("SELECT * FROM sgi_categories WHERE isDirty = 1")
    suspend fun getSgiCategoriesForSync(): List<SgiCategoryEntity>

    @Query("SELECT * FROM sgi_categories WHERE uuid = :uuid LIMIT 1")
    suspend fun getSgiCategoryByUuid(uuid: String): SgiCategoryEntity?

    @Query("UPDATE sgi_categories SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markSgiCategorySynced(id: Long, uuid: String)

    @Query("SELECT * FROM sgi_categories WHERE isEnabled = 1 ORDER BY orderIndex ASC, id ASC")
    fun getEnabledSgiCategories(): Flow<List<SgiCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSgiCategory(category: SgiCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSgiCategories(categories: List<SgiCategoryEntity>)

    @Update
    suspend fun updateSgiCategory(category: SgiCategoryEntity)

    @Delete
    suspend fun deleteSgiCategory(category: SgiCategoryEntity)

    // SGI Results
    @Query("SELECT * FROM sgi_results WHERE studentId = :studentId AND assessmentPeriod = :period AND academicYear = :academicYear")
    fun getSgiResultsForStudent(studentId: Long, period: String, academicYear: String): Flow<List<SgiResultEntity>>

    @Query("SELECT * FROM sgi_results WHERE isDirty = 1 LIMIT :limit")
    suspend fun getSgiResultsForSync(limit: Int = 300): List<SgiResultEntity>

    @Query("SELECT * FROM sgi_results WHERE uuid = :uuid LIMIT 1")
    suspend fun getSgiResultByUuid(uuid: String): SgiResultEntity?

    @Query("UPDATE sgi_results SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markSgiResultSynced(id: Long, uuid: String)

    @Query("SELECT * FROM sgi_results")
    fun getAllSgiResults(): Flow<List<SgiResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSgiResults(results: List<SgiResultEntity>)

    // Teacher Comments
    @Query("SELECT * FROM teacher_comments")
    fun getAllTeacherComments(): Flow<List<TeacherCommentEntity>>

    @Query("SELECT * FROM teacher_comments WHERE isDirty = 1 LIMIT :limit")
    suspend fun getTeacherCommentsForSync(limit: Int = 300): List<TeacherCommentEntity>

    @Query("SELECT * FROM teacher_comments WHERE uuid = :uuid LIMIT 1")
    suspend fun getTeacherCommentByUuid(uuid: String): TeacherCommentEntity?

    @Query("UPDATE teacher_comments SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markTeacherCommentSynced(id: Long, uuid: String)

    @Query("SELECT * FROM teacher_comments WHERE studentId = :studentId AND assessmentPeriod = :period AND academicYear = :academicYear LIMIT 1")
    fun getTeacherCommentForStudent(studentId: Long, period: String, academicYear: String): Flow<TeacherCommentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTeacherComment(comment: TeacherCommentEntity)
}
