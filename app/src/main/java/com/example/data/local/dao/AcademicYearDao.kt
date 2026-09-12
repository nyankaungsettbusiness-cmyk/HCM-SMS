package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AcademicYearEntity
import com.example.data.local.entity.AcademicYearStatus
import com.example.data.local.entity.PromotionHistoryEntity
import com.example.data.local.entity.StudentAcademicHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicYearDao {

    @Query("SELECT * FROM academic_years ORDER BY startDate DESC, id DESC")
    fun getAllAcademicYears(): Flow<List<AcademicYearEntity>>

    @Query("SELECT * FROM academic_years WHERE isDirty = 1")
    suspend fun getAcademicYearsForSync(): List<AcademicYearEntity>

    @Query("SELECT * FROM academic_years WHERE uuid = :uuid LIMIT 1")
    suspend fun getAcademicYearByUuid(uuid: String): AcademicYearEntity?

    @Query("UPDATE academic_years SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markAcademicYearSynced(id: Long, uuid: String)

    @Query("SELECT * FROM academic_years WHERE isCurrentActive = 1")
    suspend fun getAllActiveAcademicYearsSync(): List<AcademicYearEntity>

    @Query("SELECT * FROM academic_years ORDER BY startDate DESC, id DESC")
    suspend fun getAllAcademicYearsList(): List<AcademicYearEntity>

    @Query("DELETE FROM academic_years WHERE yearCode IN ('2024-2025', '2025-2026')")
    suspend fun removeObsoleteAcademicYears()

    @Transaction
    suspend fun sanitizeActiveAcademicYears(timestamp: Long = System.currentTimeMillis()) {
        removeObsoleteAcademicYears()
        val allYears = getAllAcademicYearsList()
        if (allYears.none { it.yearCode == "2026-2027" }) {
            insertAcademicYear(
                AcademicYearEntity(
                    yearCode = "2026-2027",
                    displayName = "2026–2027 Academic Year",
                    startDate = "2026-06-01",
                    endDate = "2027-03-31",
                    status = AcademicYearStatus.ACTIVE,
                    isCurrentActive = true,
                    isDirty = false,
                    updatedAt = timestamp
                )
            )
            if (allYears.none { it.yearCode == "2027-2028" }) {
                insertAcademicYear(
                    AcademicYearEntity(
                        yearCode = "2027-2028",
                        displayName = "2027–2028 Academic Year",
                        startDate = "2027-06-01",
                        endDate = "2028-03-31",
                        status = AcademicYearStatus.UPCOMING,
                        isCurrentActive = false,
                        isDirty = false,
                        updatedAt = timestamp
                    )
                )
            }
        }
        val activeList = getAllActiveAcademicYearsSync()
        if (activeList.size > 1) {
            android.util.Log.d("AcademicYearTrace", "DAO_SANITIZE: Found ${activeList.size} active years: ${activeList.map { "${it.yearCode}(status=${it.status}, updated=${it.updatedAt})" }}")
            val activeOnly = activeList.filter { it.status == AcademicYearStatus.ACTIVE }
            val chosen = activeOnly.maxByOrNull { it.updatedAt }
                ?: activeList.filter { it.status != AcademicYearStatus.ARCHIVED && it.status != AcademicYearStatus.CLOSED }.maxByOrNull { it.updatedAt }
                ?: activeList.maxByOrNull { it.updatedAt }
                ?: activeList.first()
            android.util.Log.d("AcademicYearTrace", "DAO_SANITIZE: Selected '${chosen.yearCode}' (id=${chosen.id}) as sole active year")
            activateAcademicYear(chosen.id, timestamp, markDirty = false)
        } else if (activeList.isEmpty()) {
            val all = getAllAcademicYearsList()
            val preferred = all.filter { it.yearCode == "2026-2027" }.firstOrNull()
                ?: all.filter { it.status == AcademicYearStatus.ACTIVE }.maxByOrNull { it.updatedAt }
                ?: all.filter { it.status != AcademicYearStatus.ARCHIVED && it.status != AcademicYearStatus.CLOSED }.maxByOrNull { it.updatedAt }
                ?: all.maxByOrNull { it.updatedAt }
                ?: all.firstOrNull()
            if (preferred != null) {
                android.util.Log.d("AcademicYearTrace", "DAO_SANITIZE: No active year found. Activating '${preferred.yearCode}'")
                activateAcademicYear(preferred.id, timestamp, markDirty = false)
            }
        }
    }

    @Transaction
    suspend fun getSanitizedActiveAcademicYearSync(): AcademicYearEntity? {
        sanitizeActiveAcademicYears()
        return getActiveAcademicYearSync()
    }

    @Query("SELECT * FROM academic_years WHERE isCurrentActive = 1 LIMIT 1")
    fun getActiveAcademicYear(): Flow<AcademicYearEntity?>

    @Query("SELECT * FROM academic_years WHERE isCurrentActive = 1 LIMIT 1")
    suspend fun getActiveAcademicYearSync(): AcademicYearEntity?

    @Query("SELECT * FROM academic_years WHERE yearCode = :yearCode LIMIT 1")
    suspend fun getAcademicYearByCode(yearCode: String): AcademicYearEntity?

    @Query("SELECT * FROM academic_years WHERE id = :id LIMIT 1")
    suspend fun getAcademicYearById(id: Long): AcademicYearEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicYear(year: AcademicYearEntity): Long

    @Update
    suspend fun updateAcademicYear(year: AcademicYearEntity)

    @Query("UPDATE academic_years SET isCurrentActive = 0, status = CASE WHEN status = 'ACTIVE' THEN 'UPCOMING' ELSE status END, isDirty = CASE WHEN :markDirty = 1 THEN 1 ELSE isDirty END, updatedAt = :timestamp WHERE isCurrentActive = 1")
    suspend fun deactivateAllAcademicYears(timestamp: Long = System.currentTimeMillis(), markDirty: Int = 1)

    @Transaction
    suspend fun activateAcademicYear(id: Long, timestamp: Long = System.currentTimeMillis(), markDirty: Boolean = true) {
        android.util.Log.d("AcademicYearTrace", "DAO_ACTIVATE: Activating yearId=$id at timestamp=$timestamp, markDirty=$markDirty")
        val current = getAcademicYearById(id)
        if (current != null && current.isCurrentActive && current.status == AcademicYearStatus.ACTIVE && !markDirty) {
            return
        }
        deactivateAllAcademicYears(timestamp, if (markDirty) 1 else 0)
        setActiveYearFlag(id, timestamp, if (markDirty) 1 else 0)
    }

    @Transaction
    suspend fun activateAcademicYearByCode(yearCode: String, timestamp: Long = System.currentTimeMillis()): Long {
        android.util.Log.d("AcademicYearTrace", "DAO_ACTIVATE_BY_CODE: Activating yearCode='$yearCode' at timestamp=$timestamp")
        val existing = getAcademicYearByCode(yearCode)
        return if (existing != null) {
            activateAcademicYear(existing.id, timestamp, markDirty = true)
            existing.id
        } else {
            deactivateAllAcademicYears(timestamp, markDirty = 1)
            val startY = if (yearCode.length >= 4) yearCode.take(4) else "2026"
            val endY = if (yearCode.length >= 4) yearCode.takeLast(4) else "2027"
            val newYear = AcademicYearEntity(
                yearCode = yearCode,
                displayName = "$yearCode Academic Year",
                startDate = "$startY-06-01",
                endDate = "$endY-03-31",
                status = AcademicYearStatus.ACTIVE,
                isCurrentActive = true,
                isDirty = true,
                updatedAt = timestamp
            )
            insertAcademicYear(newYear)
        }
    }

    @Query("UPDATE academic_years SET isCurrentActive = 1, status = 'ACTIVE', isDirty = CASE WHEN :markDirty = 1 THEN 1 ELSE isDirty END, updatedAt = :timestamp WHERE id = :id")
    suspend fun setActiveYearFlag(id: Long, timestamp: Long = System.currentTimeMillis(), markDirty: Int = 1)

    @Query("UPDATE academic_years SET status = :status, isCurrentActive = 0, closedDate = :closedDate, closedBy = :closedBy, isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateAcademicYearStatus(id: Long, status: AcademicYearStatus, closedDate: Long = System.currentTimeMillis(), closedBy: String = "", timestamp: Long = System.currentTimeMillis())

    // Promotion History
    @Query("SELECT * FROM promotion_history ORDER BY promotedDate DESC")
    fun getAllPromotionHistory(): Flow<List<PromotionHistoryEntity>>

    @Query("SELECT * FROM promotion_history WHERE isDirty = 1")
    suspend fun getPromotionHistoryForSync(): List<PromotionHistoryEntity>

    @Query("SELECT * FROM promotion_history WHERE uuid = :uuid LIMIT 1")
    suspend fun getPromotionHistoryByUuid(uuid: String): PromotionHistoryEntity?

    @Query("UPDATE promotion_history SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markPromotionHistorySynced(id: Long, uuid: String)

    @Query("SELECT * FROM promotion_history WHERE studentId = :studentId ORDER BY promotedDate DESC")
    fun getPromotionHistoryForStudent(studentId: Long): Flow<List<PromotionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotionHistory(history: PromotionHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotionHistories(histories: List<PromotionHistoryEntity>)

    // Student Academic History Snapshot
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentAcademicHistory(history: StudentAcademicHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentAcademicHistories(histories: List<StudentAcademicHistoryEntity>)

    @Query("SELECT * FROM student_academic_history")
    fun getAllStudentAcademicHistories(): Flow<List<StudentAcademicHistoryEntity>>

    @Query("SELECT * FROM student_academic_history")
    suspend fun getAllStudentAcademicHistoriesList(): List<StudentAcademicHistoryEntity>

    @Query("SELECT * FROM student_academic_history WHERE academicYear = :academicYear")
    fun getStudentAcademicHistoriesByYear(academicYear: String): Flow<List<StudentAcademicHistoryEntity>>

    @Query("SELECT * FROM student_academic_history WHERE studentId = :studentId")
    fun getStudentAcademicHistories(studentId: Long): Flow<List<StudentAcademicHistoryEntity>>
}
