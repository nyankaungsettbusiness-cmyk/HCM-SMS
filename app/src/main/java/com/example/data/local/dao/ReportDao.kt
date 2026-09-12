package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PdfExportHistoryEntity
import com.example.data.local.entity.PrintHistoryEntity
import com.example.data.local.entity.ReportGenerationHistoryEntity
import com.example.data.local.entity.ReportSettingEntity
import com.example.data.local.entity.ReportTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    // Templates
    @Query("SELECT * FROM report_templates ORDER BY id ASC")
    fun getAllTemplates(): Flow<List<ReportTemplateEntity>>

    @Query("SELECT * FROM report_templates WHERE targetLevel = :targetLevel AND isActive = 1 LIMIT 1")
    suspend fun getActiveTemplateForLevel(targetLevel: String): ReportTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ReportTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ReportTemplateEntity>)

    // Settings
    @Query("SELECT * FROM report_settings WHERE id = 1 LIMIT 1")
    fun getReportSettingsFlow(): Flow<ReportSettingEntity?>

    @Query("SELECT * FROM report_settings WHERE id = 1 LIMIT 1")
    suspend fun getReportSettings(): ReportSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReportSettings(settings: ReportSettingEntity)

    // Generation History
    @Query("SELECT * FROM report_generation_history ORDER BY generatedAtTimestamp DESC")
    fun getAllGenerationHistory(): Flow<List<ReportGenerationHistoryEntity>>

    @Query("SELECT * FROM report_generation_history WHERE studentId = :studentId ORDER BY generatedAtTimestamp DESC")
    fun getGenerationHistoryForStudent(studentId: Long): Flow<List<ReportGenerationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenerationHistory(history: ReportGenerationHistoryEntity): Long

    @Query("UPDATE report_generation_history SET status = :status WHERE studentId = :studentId AND assessmentPeriodName = :periodName AND academicYear = :academicYear")
    suspend fun updateHistoryStatus(studentId: Long, periodName: String, academicYear: String, status: String)

    // PDF Export History
    @Query("SELECT * FROM pdf_export_history ORDER BY exportedAtTimestamp DESC")
    fun getAllExportHistory(): Flow<List<PdfExportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportHistory(export: PdfExportHistoryEntity): Long

    // Print History
    @Query("SELECT * FROM print_history ORDER BY printedAtTimestamp DESC")
    fun getAllPrintHistory(): Flow<List<PrintHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrintHistory(printLog: PrintHistoryEntity): Long
}
