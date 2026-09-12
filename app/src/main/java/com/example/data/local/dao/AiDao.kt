package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AiHistoryEntity
import com.example.data.local.entity.AiSavedQuestionEntity
import com.example.data.local.entity.AiSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDao {

    // AI History
    @Query("SELECT * FROM ai_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE teacherUsername = :username ORDER BY timestamp DESC")
    fun getHistoryForTeacher(username: String): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE category = :category ORDER BY timestamp DESC")
    fun getHistoryByCategory(category: String): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE teacherUsername = :username AND category = :category ORDER BY timestamp DESC")
    fun getHistoryForTeacherByCategory(username: String, category: String): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistory(): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE teacherUsername = :username AND isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistoryForTeacher(username: String): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Long): AiHistoryEntity?

    @Query("SELECT * FROM ai_history WHERE teacherUsername = :username AND title = :title ORDER BY timestamp DESC LIMIT 1")
    suspend fun findExistingHistory(username: String, title: String): AiHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: AiHistoryEntity): Long

    @Update
    suspend fun updateHistory(item: AiHistoryEntity)

    @Query("UPDATE ai_history SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFav: Boolean)

    @Query("DELETE FROM ai_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM ai_history")
    suspend fun clearAllHistory()

    // Question Bank
    @Query("SELECT * FROM ai_saved_questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<AiSavedQuestionEntity>>

    @Query("SELECT * FROM ai_saved_questions WHERE grade = :grade AND subject = :subject ORDER BY createdAt DESC")
    fun getQuestionsByGradeAndSubject(grade: String, subject: String): Flow<List<AiSavedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: AiSavedQuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<AiSavedQuestionEntity>)

    @Update
    suspend fun updateQuestion(question: AiSavedQuestionEntity)

    @Query("DELETE FROM ai_saved_questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)

    // AI Settings
    @Query("SELECT * FROM ai_settings WHERE id = 1 LIMIT 1")
    fun getAiSettingsFlow(): Flow<AiSettingEntity?>

    @Query("SELECT * FROM ai_settings WHERE id = 1 LIMIT 1")
    suspend fun getAiSettings(): AiSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAiSettings(settings: AiSettingEntity)
}
