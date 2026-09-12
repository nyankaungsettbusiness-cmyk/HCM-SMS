package com.example.data.repository

import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSession
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class StudentAttendanceSummary(
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val grade: String,
    val className: String,
    val totalDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val lateDays: Int,
    val leaveDays: Int,
    val attendancePercentage: Double
)

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao
) {

    fun getAttendanceForSession(
        academicYear: String,
        date: String,
        session: AttendanceSession,
        grade: String,
        className: String
    ): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAttendanceForSession(academicYear, date, session, grade, className)
    }

    fun getAttendanceForStudent(studentId: Long, academicYear: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAttendanceForStudent(studentId, academicYear)
    }

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAttendanceForDate(date)
    }

    fun getAttendanceForClass(academicYear: String, grade: String, className: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAttendanceForClass(academicYear, grade, className)
    }

    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAllAttendanceRecords()
    }

    suspend fun saveAttendanceRecords(records: List<AttendanceRecordEntity>) {
        val dirtyRecords = records.map {
            it.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        }
        attendanceDao.insertOrUpdateAttendance(dirtyRecords)
        triggerBackgroundSync()
    }

    suspend fun resetSessionAttendance(
        academicYear: String,
        date: String,
        session: AttendanceSession,
        grade: String,
        className: String
    ) {
        attendanceDao.resetSessionAttendance(academicYear, date, session, grade, className)
        triggerBackgroundSync()
    }

    private fun triggerBackgroundSync() {
        com.example.data.sync.SyncManager.triggerTableSyncAsync("attendance_records")
    }

    suspend fun calculateStudentSummarySync(studentId: Long, academicYear: String): StudentAttendanceSummary? {
        val student = studentDao.getStudentById(studentId) ?: return null
        val records = attendanceDao.getAttendanceListForStudentSync(studentId, academicYear)

        val total = records.size
        val present = records.count { it.status == AttendanceStatus.PRESENT }
        val absent = records.count { it.status == AttendanceStatus.ABSENT }
        val late = records.count { it.status == AttendanceStatus.LATE }
        val leave = records.count { it.status == AttendanceStatus.LEAVE }

        val pct = if (total > 0) {
            ((present.toDouble() + (late.toDouble() * 0.5)) / total.toDouble()) * 100.0
        } else {
            100.0
        }

        return StudentAttendanceSummary(
            studentId = student.id,
            studentCode = student.studentCode,
            studentName = student.name,
            grade = student.gradeName,
            className = student.className,
            totalDays = total,
            presentDays = present,
            absentDays = absent,
            lateDays = late,
            leaveDays = leave,
            attendancePercentage = pct
        )
    }

    fun calculateSummary(
        student: StudentEntity,
        records: List<AttendanceRecordEntity>
    ): StudentAttendanceSummary {
        val studentRecords = records.filter { it.studentId == student.id }
        val total = studentRecords.size
        val present = studentRecords.count { it.status == AttendanceStatus.PRESENT }
        val absent = studentRecords.count { it.status == AttendanceStatus.ABSENT }
        val late = studentRecords.count { it.status == AttendanceStatus.LATE }
        val leave = studentRecords.count { it.status == AttendanceStatus.LEAVE }

        val pct = if (total > 0) {
            ((present.toDouble() + (late.toDouble() * 0.5)) / total.toDouble()) * 100.0
        } else {
            100.0
        }

        return StudentAttendanceSummary(
            studentId = student.id,
            studentCode = student.studentCode,
            studentName = student.name,
            grade = student.gradeName,
            className = student.className,
            totalDays = total,
            presentDays = present,
            absentDays = absent,
            lateDays = late,
            leaveDays = leave,
            attendancePercentage = pct
        )
    }
}
