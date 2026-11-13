package com.example.data.repository

import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSession
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

data class MonthlyStudentAttendanceInput(
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val totalDays: Double,
    val presentDays: Double,
    val absentDays: Double,
    val leaveDays: Double
)

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

    fun getAttendanceForClassMonth(
        academicYear: String,
        grade: String,
        className: String,
        datePrefix: String
    ): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAttendanceForClassMonth(academicYear, grade, className, datePrefix)
    }

    suspend fun saveMonthlyAttendanceSummary(
        academicYear: String,
        grade: String,
        className: String,
        monthName: String,
        schoolDays: Int,
        inputs: List<MonthlyStudentAttendanceInput>,
        recordedBy: String
    ) {
        val prefix = getYearMonthPrefix(academicYear, monthName)
        val parts = prefix.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 6

        val weekdays = getWeekdaysForMonth(year, month, schoolDays)
        attendanceDao.deleteAttendanceForClassMonth(academicYear, grade, className, prefix)

        val newRecords = mutableListOf<AttendanceRecordEntity>()
        val now = System.currentTimeMillis()

        for (input in inputs) {
            val totalSessions = (schoolDays * 2).coerceAtLeast(0)
            val presentSessions = (input.presentDays * 2.0).roundToInt().coerceIn(0, totalSessions)
            val leaveSessions = (input.leaveDays * 2.0).roundToInt().coerceIn(0, totalSessions - presentSessions)

            var pRemaining = presentSessions
            var lRemaining = leaveSessions

            for (dateStr in weekdays) {
                for (session in listOf(AttendanceSession.MORNING, AttendanceSession.EVENING)) {
                    val status = when {
                        pRemaining > 0 -> {
                            pRemaining--
                            AttendanceStatus.PRESENT
                        }
                        lRemaining > 0 -> {
                            lRemaining--
                            AttendanceStatus.LEAVE
                        }
                        else -> AttendanceStatus.ABSENT
                    }

                    newRecords.add(
                        AttendanceRecordEntity(
                            id = 0L,
                            academicYear = academicYear,
                            date = dateStr,
                            session = session,
                            studentId = input.studentId,
                            studentCode = input.studentCode,
                            studentName = input.studentName,
                            grade = grade,
                            className = className,
                            status = status,
                            recordedBy = recordedBy,
                            recordedDateTime = now,
                            uuid = java.util.UUID.randomUUID().toString(),
                            updatedAt = now,
                            isDirty = true,
                            isDeleted = false
                        )
                    )
                }
            }
        }

        if (newRecords.isNotEmpty()) {
            attendanceDao.insertOrUpdateAttendance(newRecords)
            triggerBackgroundSync()
        }
    }

    companion object {
        fun getYearMonthPrefix(academicYear: String, monthName: String): String {
            val parts = academicYear.split("-", "/").mapNotNull { it.trim().toIntOrNull() }
            val startYear = parts.getOrNull(0) ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val endYear = parts.getOrNull(1) ?: (startYear + 1)

            val (monthNum, isNextYear) = when (monthName.trim().lowercase()) {
                "june", "jun" -> Pair("06", false)
                "july", "jul" -> Pair("07", false)
                "august", "aug" -> Pair("08", false)
                "september", "sep" -> Pair("09", false)
                "october", "oct" -> Pair("10", false)
                "november", "nov" -> Pair("11", false)
                "december", "dec" -> Pair("12", false)
                "january", "jan" -> Pair("01", true)
                "february", "feb" -> Pair("02", true)
                "march", "mar" -> Pair("03", true)
                "april", "apr" -> Pair("04", true)
                "may" -> Pair("05", true)
                else -> Pair("06", false)
            }

            val year = if (isNextYear && parts.size >= 2) endYear else startYear
            return String.format(java.util.Locale.US, "%04d-%s", year, monthNum)
        }

        fun getWeekdaysForMonth(year: Int, monthIndex: Int, countNeeded: Int): List<String> {
            val dates = mutableListOf<String>()
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.YEAR, year)
            cal.set(java.util.Calendar.MONTH, monthIndex - 1)
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)

            val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            for (day in 1..maxDay) {
                cal.set(java.util.Calendar.DAY_OF_MONTH, day)
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                if (dow != java.util.Calendar.SATURDAY && dow != java.util.Calendar.SUNDAY) {
                    dates.add(String.format(java.util.Locale.US, "%04d-%02d-%02d", year, monthIndex, day))
                }
            }

            if (dates.size >= countNeeded) {
                return dates.take(countNeeded)
            }

            for (day in 1..maxDay) {
                val candidate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, monthIndex, day)
                if (!dates.contains(candidate)) {
                    dates.add(candidate)
                    if (dates.size >= countNeeded) break
                }
            }
            return dates.take(countNeeded)
        }
    }
}
