package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSession
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.StudentAcademicHistoryEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.repository.AcademicYearRepository
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.MonthlyStudentAttendanceInput
import com.example.data.repository.StudentAttendanceSummary
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudentAttendanceUiItem(
    val student: StudentEntity,
    val currentStatus: AttendanceStatus,
    val existingRecordId: Long = 0L,
    val recordedBy: String = "",
    val lastRecordedTime: String = ""
)

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val academicYearRepository: AcademicYearRepository? = null
) : ViewModel() {

    private val _academicYear = MutableStateFlow("2026-2027")
    val academicYear: StateFlow<String> = _academicYear.asStateFlow()

    init {
        viewModelScope.launch {
            val activeYear = academicYearRepository?.getActiveAcademicYear()?.firstOrNull()?.yearCode
            if (!activeYear.isNullOrBlank()) {
                _academicYear.value = activeYear
            }
        }
    }

    private val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private val _selectedDate = MutableStateFlow(todayStr)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedGrade = MutableStateFlow("G1")
    val selectedGrade: StateFlow<String> = _selectedGrade.asStateFlow()

    private val _selectedClass = MutableStateFlow("A")
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    private val _selectedSession = MutableStateFlow(AttendanceSession.MORNING)
    val selectedSession: StateFlow<AttendanceSession> = _selectedSession.asStateFlow()

    // Mode: Daily Roll Call vs Monthly Summary
    private val _selectedMode = MutableStateFlow(AttendanceMode.DAILY)
    val selectedMode: StateFlow<AttendanceMode> = _selectedMode.asStateFlow()

    // Monthly Summary State
    val availableMonths = listOf("June", "July", "August", "September", "October", "November", "December", "January", "February", "March", "April", "May")
    private val _selectedMonth = MutableStateFlow("June")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _monthlySchoolDays = MutableStateFlow(20)
    val monthlySchoolDays: StateFlow<Int> = _monthlySchoolDays.asStateFlow()

    private val _monthlyEdits = MutableStateFlow<Map<Long, MonthlyStudentAttendanceInput>>(emptyMap())
    val monthlyEdits: StateFlow<Map<Long, MonthlyStudentAttendanceInput>> = _monthlyEdits.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Local in-memory edits before saving
    private val _editedStatusMap = MutableStateFlow<Map<Long, AttendanceStatus>>(emptyMap())
    val editedStatusMap: StateFlow<Map<Long, AttendanceStatus>> = _editedStatusMap.asStateFlow()

    // Selected student for detailed history breakdown
    private val _selectedHistoryStudent = MutableStateFlow<StudentEntity?>(null)
    val selectedHistoryStudent: StateFlow<StudentEntity?> = _selectedHistoryStudent.asStateFlow()

    private val academicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearRepository?.getAllStudentAcademicHistories()
        ?: flowOf(emptyList())

    // Students list from student repository for selected Grade, Class & Academic Year
    @OptIn(ExperimentalCoroutinesApi::class)
    val studentsInClass: StateFlow<List<StudentEntity>> = combine(
        studentRepository.allStudents,
        academicHistories,
        _academicYear,
        _selectedGrade,
        _selectedClass
    ) { all, histories, selectedYr, grade, clazz ->
        val historiesByStudent = histories.groupBy { it.studentId }

        all.mapNotNull { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(selectedYr, ignoreCase = true) }

            if (yearHistory != null) {
                student.copy(
                    gradeName = yearHistory.gradeName,
                    className = yearHistory.className,
                    rollNumber = yearHistory.rollNumber,
                    status = yearHistory.status
                )
            } else if (studentHistories.isEmpty()) {
                student
            } else {
                val activeCode = academicYearRepository?.getActiveAcademicYear()?.firstOrNull()?.yearCode ?: "2026-2027"
                if (selectedYr.equals(activeCode, ignoreCase = true)) {
                    student
                } else {
                    null
                }
            }
        }.filter { student ->
            student.gradeName.equals(grade, ignoreCase = true) &&
                    (clazz == "All" || clazz == "All Classes" || student.className.equals(clazz, ignoreCase = true))
        }.sortedBy { it.rollNumber }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Saved DB records for current academicYear, date, session, grade, class
    val savedSessionRecords: StateFlow<List<AttendanceRecordEntity>> = combine(
        _academicYear,
        _selectedDate,
        _selectedSession,
        _selectedGrade,
        _selectedClass
    ) { yr, date, session, grade, clazz ->
        Tuple5(yr, date, session, grade, clazz)
    }.flatMapLatest { (yr, date, session, grade, clazz) ->
        attendanceRepository.getAttendanceForSession(yr, date, session, grade, clazz)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All attendance records for history
    val allAttendanceRecords: StateFlow<List<AttendanceRecordEntity>> = attendanceRepository
        .getAllAttendanceRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined UI list of students with attendance status
    val attendanceUiList: StateFlow<List<StudentAttendanceUiItem>> = combine(
        studentsInClass,
        savedSessionRecords,
        _editedStatusMap,
        _searchQuery
    ) { students, savedRecords, edits, query ->
        val recordMap = savedRecords.associateBy { it.studentId }

        students
            .filter { student ->
                if (query.isBlank()) true
                else student.name.contains(query, ignoreCase = true) ||
                        student.studentCode.contains(query, ignoreCase = true) ||
                        student.rollNumber.toString().contains(query)
            }
            .map { student ->
                val savedRec = recordMap[student.id]
                val status = edits[student.id] ?: savedRec?.status ?: AttendanceStatus.PRESENT
                val lastTime = savedRec?.let {
                    SimpleDateFormat("HH:mm", Locale.US).format(Date(it.recordedDateTime))
                } ?: ""

                StudentAttendanceUiItem(
                    student = student,
                    currentStatus = status,
                    existingRecordId = savedRec?.id ?: 0L,
                    recordedBy = savedRec?.recordedBy ?: "",
                    lastRecordedTime = lastTime
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary Statistics for current class session
    val sessionStats: StateFlow<AttendanceSessionStats> = attendanceUiList.map { list ->
        val total = list.size
        val present = list.count { it.currentStatus == AttendanceStatus.PRESENT }
        val absent = list.count { it.currentStatus == AttendanceStatus.ABSENT }
        val late = list.count { it.currentStatus == AttendanceStatus.LATE }
        val leave = list.count { it.currentStatus == AttendanceStatus.LEAVE }
        val pct = if (total > 0) {
            ((present.toDouble() + (late.toDouble() * 0.5)) / total.toDouble()) * 100.0
        } else 100.0

        AttendanceSessionStats(
            totalStudents = total,
            presentCount = present,
            absentCount = absent,
            lateCount = late,
            leaveCount = leave,
            attendancePercentage = pct
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AttendanceSessionStats()
    )

    // Monthly Records for selected Class & Month
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyRecordsForClass: StateFlow<List<AttendanceRecordEntity>> = combine(
        _academicYear,
        _selectedGrade,
        _selectedClass,
        _selectedMonth
    ) { yr, gr, cl, mo ->
        val prefix = AttendanceRepository.getYearMonthPrefix(yr, mo)
        attendanceRepository.getAttendanceForClassMonth(yr, gr, cl, prefix)
    }.flatMapLatest { it }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined UI list for Monthly Summary
    val monthlyUiList: StateFlow<List<MonthlyStudentAttendanceInput>> = combine(
        studentsInClass,
        monthlyRecordsForClass,
        _monthlyEdits,
        _monthlySchoolDays,
        _searchQuery
    ) { students, records, edits, schoolDays, query ->
        val recordsByStudent = records.groupBy { it.studentId }
        val dSchoolDays = schoolDays.toDouble()

        students.filter { student ->
            if (query.isBlank()) true
            else student.name.contains(query, ignoreCase = true) ||
                    student.studentCode.contains(query, ignoreCase = true) ||
                    student.rollNumber.toString().contains(query)
        }.map { student ->
            val edited = edits[student.id]
            if (edited != null) {
                edited
            } else {
                val studentRecs = recordsByStudent[student.id]
                if (!studentRecs.isNullOrEmpty()) {
                    val pCount = studentRecs.count { it.status == AttendanceStatus.PRESENT }
                    val lCount = studentRecs.count { it.status == AttendanceStatus.LEAVE }
                    val pDays = (pCount / 2.0).coerceAtMost(dSchoolDays)
                    val lDays = (lCount / 2.0).coerceAtMost(dSchoolDays)
                    val aDays = (dSchoolDays - pDays - lDays).coerceAtLeast(0.0)

                    MonthlyStudentAttendanceInput(
                        studentId = student.id,
                        studentCode = student.studentCode,
                        studentName = student.name,
                        totalDays = dSchoolDays,
                        presentDays = pDays,
                        absentDays = aDays,
                        leaveDays = lDays
                    )
                } else {
                    MonthlyStudentAttendanceInput(
                        studentId = student.id,
                        studentCode = student.studentCode,
                        studentName = student.name,
                        totalDays = dSchoolDays,
                        presentDays = dSchoolDays,
                        absentDays = 0.0,
                        leaveDays = 0.0
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Student summary history
    val selectedStudentHistorySummary: StateFlow<StudentAttendanceSummary?> = combine(
        _selectedHistoryStudent,
        _academicYear,
        allAttendanceRecords
    ) { student, yr, records ->
        if (student == null) null
        else attendanceRepository.calculateSummary(student, records)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setAcademicYear(year: String) {
        _academicYear.value = year
        _editedStatusMap.value = emptyMap()
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        _editedStatusMap.value = emptyMap()
    }

    fun setSelectedGrade(grade: String) {
        _selectedGrade.value = grade
        _editedStatusMap.value = emptyMap()
    }

    fun setSelectedClass(clazz: String) {
        _selectedClass.value = clazz
        _editedStatusMap.value = emptyMap()
    }

    fun setSelectedSession(session: AttendanceSession) {
        _selectedSession.value = session
        _editedStatusMap.value = emptyMap()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStudentStatus(studentId: Long, status: AttendanceStatus) {
        val current = _editedStatusMap.value.toMutableMap()
        current[studentId] = status
        _editedStatusMap.value = current
    }

    fun markAllPresent() {
        val students = studentsInClass.value
        val map = students.associate { it.id to AttendanceStatus.PRESENT }
        _editedStatusMap.value = map
        _statusMessage.value = "Marked all ${students.size} students as Present"
    }

    fun markAllAbsent() {
        val students = studentsInClass.value
        val map = students.associate { it.id to AttendanceStatus.ABSENT }
        _editedStatusMap.value = map
        _statusMessage.value = "Marked all ${students.size} students as Absent"
    }

    fun resetAttendance() {
        val yr = _academicYear.value
        val dt = _selectedDate.value
        val sess = _selectedSession.value
        val gr = _selectedGrade.value
        val cl = _selectedClass.value

        viewModelScope.launch {
            attendanceRepository.resetSessionAttendance(yr, dt, sess, gr, cl)
            _editedStatusMap.value = emptyMap()
            _statusMessage.value = "Reset attendance for $gr-$cl ($sess session on $dt)"
        }
    }

    fun saveAttendance(recordedBy: String) {
        val students = studentsInClass.value
        if (students.isEmpty()) {
            _statusMessage.value = "No students in selected class to save attendance."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val yr = _academicYear.value
            val dt = _selectedDate.value
            val sess = _selectedSession.value
            val gr = _selectedGrade.value
            val cl = _selectedClass.value
            val edits = _editedStatusMap.value
            val savedRecordsMap = savedSessionRecords.value.associateBy { it.studentId }

            val recordsToSave = students.map { student ->
                val status = edits[student.id] ?: savedRecordsMap[student.id]?.status ?: AttendanceStatus.PRESENT
                val existingId = savedRecordsMap[student.id]?.id ?: 0L

                AttendanceRecordEntity(
                    id = existingId,
                    academicYear = yr,
                    date = dt,
                    session = sess,
                    studentId = student.id,
                    studentCode = student.studentCode,
                    studentName = student.name,
                    grade = gr,
                    className = cl,
                    status = status,
                    recordedBy = recordedBy.ifEmpty { "Teacher" },
                    recordedDateTime = System.currentTimeMillis()
                )
            }

            attendanceRepository.saveAttendanceRecords(recordsToSave)
            _editedStatusMap.value = emptyMap()
            _isSaving.value = false
            _statusMessage.value = "Successfully saved attendance for ${recordsToSave.size} students!"
        }
    }

    fun selectHistoryStudent(student: StudentEntity?) {
        _selectedHistoryStudent.value = student
    }

    fun setSelectedMode(mode: AttendanceMode) {
        _selectedMode.value = mode
        _statusMessage.value = null
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
        _monthlyEdits.value = emptyMap()
    }

    fun setMonthlySchoolDays(days: Int) {
        val safeDays = days.coerceAtLeast(1)
        _monthlySchoolDays.value = safeDays
        val currentEdits = _monthlyEdits.value.toMutableMap()
        for ((sId, input) in currentEdits) {
            val newPresent = input.presentDays.coerceAtMost(safeDays.toDouble())
            val newLeave = input.leaveDays.coerceAtMost((safeDays.toDouble() - newPresent).coerceAtLeast(0.0))
            val newAbsent = (safeDays.toDouble() - newPresent - newLeave).coerceAtLeast(0.0)
            currentEdits[sId] = input.copy(
                totalDays = safeDays.toDouble(),
                presentDays = newPresent,
                leaveDays = newLeave,
                absentDays = newAbsent
            )
        }
        _monthlyEdits.value = currentEdits
    }

    fun updateMonthlyStudent(studentId: Long, present: Double, absent: Double, leave: Double) {
        val student = studentsInClass.value.find { it.id == studentId } ?: return
        val currentEdits = _monthlyEdits.value.toMutableMap()
        currentEdits[studentId] = MonthlyStudentAttendanceInput(
            studentId = studentId,
            studentCode = student.studentCode,
            studentName = student.name,
            totalDays = _monthlySchoolDays.value.toDouble(),
            presentDays = present,
            absentDays = absent,
            leaveDays = leave
        )
        _monthlyEdits.value = currentEdits
    }

    fun markAllFullMonthly() {
        val students = studentsInClass.value
        val schoolDays = _monthlySchoolDays.value.toDouble()
        val edits = students.associate { student ->
            student.id to MonthlyStudentAttendanceInput(
                studentId = student.id,
                studentCode = student.studentCode,
                studentName = student.name,
                totalDays = schoolDays,
                presentDays = schoolDays,
                absentDays = 0.0,
                leaveDays = 0.0
            )
        }
        _monthlyEdits.value = edits
        _statusMessage.value = "Set all ${students.size} students to full attendance ($schoolDays days)"
    }

    fun saveMonthlyAttendance(recordedBy: String) {
        val inputs = monthlyUiList.value
        if (inputs.isEmpty()) {
            _statusMessage.value = "No students in selected class to save monthly attendance."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val yr = _academicYear.value
            val gr = _selectedGrade.value
            val cl = _selectedClass.value
            val mo = _selectedMonth.value
            val days = _monthlySchoolDays.value

            attendanceRepository.saveMonthlyAttendanceSummary(
                academicYear = yr,
                grade = gr,
                className = cl,
                monthName = mo,
                schoolDays = days,
                inputs = inputs,
                recordedBy = recordedBy.ifEmpty { "Teacher" }
            )

            _monthlyEdits.value = emptyMap()
            _isSaving.value = false
            _statusMessage.value = "Successfully saved monthly attendance for $gr-$cl ($mo) for ${inputs.size} students!"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Permission Check Helper
    fun canEditAttendance(currentUser: UserEntity?): Boolean {
        if (currentUser == null) return false
        return currentUser.role == UserRole.SUPER_ADMIN ||
                currentUser.role == UserRole.ADMIN ||
                currentUser.role == UserRole.TEACHER
    }
}

enum class AttendanceMode(val displayName: String) {
    DAILY("Daily Roll Call"),
    MONTHLY("Monthly Summary")
}

data class AttendanceSessionStats(
    val totalStudents: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val leaveCount: Int = 0,
    val attendancePercentage: Double = 100.0
)

data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

class AttendanceViewModelFactory(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val academicYearRepository: AcademicYearRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            return AttendanceViewModel(attendanceRepository, studentRepository, academicYearRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
