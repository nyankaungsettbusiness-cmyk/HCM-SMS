package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AssessmentPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentPeriodDao {
    @Query("SELECT * FROM assessment_periods ORDER BY orderIndex ASC, id ASC")
    fun getAllAssessmentPeriods(): Flow<List<AssessmentPeriodEntity>>

    @Query("SELECT * FROM assessment_periods WHERE isDirty = 1")
    suspend fun getAssessmentPeriodsForSync(): List<AssessmentPeriodEntity>

    @Query("SELECT * FROM assessment_periods WHERE uuid = :uuid LIMIT 1")
    suspend fun getAssessmentPeriodByUuid(uuid: String): AssessmentPeriodEntity?

    @Query("UPDATE assessment_periods SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markAssessmentPeriodSynced(id: Long, uuid: String)

    @Query("SELECT * FROM assessment_periods WHERE educationLevel = :level ORDER BY orderIndex ASC, id ASC")
    fun getAssessmentPeriodsForLevel(level: String): Flow<List<AssessmentPeriodEntity>>

    @Query("SELECT * FROM assessment_periods WHERE educationLevel = :level AND isEnabled = 1 ORDER BY orderIndex ASC, id ASC")
    fun getEnabledAssessmentPeriodsForLevel(level: String): Flow<List<AssessmentPeriodEntity>>

    @Query("SELECT COUNT(*) FROM assessment_periods")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentPeriod(period: AssessmentPeriodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentPeriods(periods: List<AssessmentPeriodEntity>)

    @Update
    suspend fun updateAssessmentPeriod(period: AssessmentPeriodEntity)

    @Query("DELETE FROM assessment_periods WHERE id = :id")
    suspend fun deleteAssessmentPeriod(id: Long)
}
