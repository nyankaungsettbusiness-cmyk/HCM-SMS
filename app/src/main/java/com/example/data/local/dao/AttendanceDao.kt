package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(records: List<AttendanceRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: AttendanceRecordEntity): Long

    @Query("SELECT * FROM attendance_records WHERE isDirty = 1 LIMIT :limit")
    suspend fun getAttendanceRecordsForSync(limit: Int = 300): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE uuid = :uuid LIMIT 1")
    suspend fun getAttendanceRecordByUuid(uuid: String): AttendanceRecordEntity?

    @Query("UPDATE attendance_records SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markAttendanceRecordSynced(id: Long, uuid: String)

    @Query("SELECT * FROM attendance_records WHERE academicYear = :academicYear AND date = :date AND session = :session AND grade = :grade AND className = :className")
    fun getAttendanceForSession(
        academicYear: String,
        date: String,
        session: AttendanceSession,
        grade: String,
        className: String
    ): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND academicYear = :academicYear")
    fun getAttendanceForStudent(studentId: Long, academicYear: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND academicYear = :academicYear")
    suspend fun getAttendanceListForStudentSync(studentId: Long, academicYear: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY recordedDateTime DESC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE academicYear = :academicYear AND grade = :grade AND className = :className")
    fun getAttendanceForClass(academicYear: String, grade: String, className: String): Flow<List<AttendanceRecordEntity>>

    @Query("DELETE FROM attendance_records WHERE academicYear = :academicYear AND date = :date AND session = :session AND grade = :grade AND className = :className")
    suspend fun resetSessionAttendance(
        academicYear: String,
        date: String,
        session: AttendanceSession,
        grade: String,
        className: String
    )

    @Query("SELECT * FROM attendance_records ORDER BY recordedDateTime DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}
