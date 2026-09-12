package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.DashboardRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class StudentAttendanceSummary(
    val studentId: Long,
    val name: String,
    val rollNo: String,
    val grade: String,
    val className: String,
    val totalDays: Int,
    val presentDays: Int,
    val percentage: Float
)

data class StudentHcmSummary(
    val studentId: Long,
    val name: String,
    val rollNo: String,
    val grade: String,
    val honesty: Float,
    val curiosity: Float,
    val mindfulness: Float,
    val overallScore: Float
)

data class RecentActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val category: ActivityCategory
)

enum class ActivityCategory {
    ATTENDANCE,
    MARKS,
    REPORT,
    STUDENT,
    PROMOTION,
    AUDIT
}

data class DashboardAnalyticsState(
    // Filters
    val searchQuery: String = "",
    val selectedAcademicYear: String = "All",
    val selectedGrade: String = "All",
    val selectedClass: String = "All",
    val selectedAssessmentType: String = "All",

    // Overview Cards
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalClasses: Int = 0,
    val totalGrades: Int = 0,
    val activeAcademicYearName: String = "2026-2027",
    val totalSubjects: Int = 0,
    val todayAttendancePercentage: Float = 0f,
    val monthlyAttendancePercentage: Float = 0f,
    val pendingAssessmentsCount: Int = 0,
    val generatedReportCardsCount: Int = 0,

    // Student Analytics
    val studentsByGradeMap: Map<String, Int> = emptyMap(),
    val studentsByClassMap: Map<String, Int> = emptyMap(),
    val genderDistributionMap: Map<String, Int> = emptyMap(),
    val activeStudentsCount: Int = 0,
    val transferredStudentsCount: Int = 0,
    val graduatedStudentsCount: Int = 0,

    // Attendance Analytics
    val todayAttendancePresentCount: Int = 0,
    val todayAttendanceAbsentCount: Int = 0,
    val todayAttendanceLateCount: Int = 0,
    val todayAttendanceLeaveCount: Int = 0,
    val yearlyAttendancePercentage: Float = 0f,
    val attendanceTrendsMap: Map<String, Float> = emptyMap(),
    val lowAttendanceStudents: List<StudentAttendanceSummary> = emptyList(),
    val topRegularStudents: List<StudentAttendanceSummary> = emptyList(),

    // Academic Analytics
    val assessmentTypePerformanceMap: Map<String, Float> = emptyMap(),
    val overallAcademicAverage: Float = 0f,
    val averageScoreByGradeMap: Map<String, Float> = emptyMap(),
    val averageScoreBySubjectMap: Map<String, Float> = emptyMap(),
    val highestScoreStudent: Pair<String, Float>? = null,
    val lowestScoreStudent: Pair<String, Float>? = null,

    // HCM Analytics
    val averageHonesty: Float = 0f,
    val averageCuriosity: Float = 0f,
    val averageMindfulness: Float = 0f,
    val overallHcmScore: Float = 0f,
    val topHcmStudents: List<StudentHcmSummary> = emptyList(),
    val supportRequiredHcmStudents: List<StudentHcmSummary> = emptyList(),

    // SGI Analytics
    val averageSgi: Float = 0f,
    val highestSgi: Float = 0f,
    val lowestSgi: Float = 0f,
    val sgiGrowthTrendsMap: Map<String, Float> = emptyMap(),

    // Report Analytics
    val totalReportsGenerated: Int = 0,
    val monthlyReportsCount: Int = 0,
    val academicYearReportsCount: Int = 0,
    val pdfExportCount: Int = 0,
    val printCount: Int = 0,

    // Recent Activities
    val recentActivities: List<RecentActivityItem> = emptyList(),

    // Available Filter Options
    val availableAcademicYears: List<String> = emptyList(),
    val availableGrades: List<String> = emptyList(),
    val availableClasses: List<String> = emptyList(),
    val availableAssessmentTypes: List<String> = listOf("All", "Monthly Test", "Pilot Test", "Weekly Test", "Lesson Completion Test", "CET")
)

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAcademicYear = MutableStateFlow("All")
    val selectedAcademicYear: StateFlow<String> = _selectedAcademicYear.asStateFlow()

    private val _selectedGrade = MutableStateFlow("All")
    val selectedGrade: StateFlow<String> = _selectedGrade.asStateFlow()

    private val _selectedClass = MutableStateFlow("All")
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    private val _selectedAssessmentType = MutableStateFlow("All")
    val selectedAssessmentType: StateFlow<String> = _selectedAssessmentType.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedAcademicYear(year: String) {
        _selectedAcademicYear.value = year
    }

    fun setSelectedGrade(grade: String) {
        _selectedGrade.value = grade
    }

    fun setSelectedClass(className: String) {
        _selectedClass.value = className
    }

    fun setSelectedAssessmentType(type: String) {
        _selectedAssessmentType.value = type
    }

    private val filterContainerFlow = combine(
        _searchQuery,
        _selectedAcademicYear,
        _selectedGrade,
        _selectedClass,
        _selectedAssessmentType
    ) { search, year, grade, clazz, assessType ->
        FilterContainer(search, year, grade, clazz, assessType)
    }

    private val groupAFlow = combine(
        filterContainerFlow,
        repository.allStudents,
        repository.allTeachers,
        repository.allAttendance,
        repository.allMarks
    ) { filter, students, teachers, attendance, marks ->
        DataGroupA(filter, students, teachers, attendance, marks)
    }

    private val groupBFlow = combine(
        repository.allAssessments,
        repository.allHolisticResults,
        repository.allSgiResults,
        repository.allReportGenerations,
        repository.allPdfExports
    ) { assessments, holistic, sgi, reports, pdfs ->
        DataGroupB(assessments, holistic, sgi, reports, pdfs)
    }

    private val groupC1Flow = combine(
        repository.allPrintLogs,
        repository.allAcademicYears,
        repository.activeAcademicYear
    ) { prints, years, activeYear ->
        Triple(prints, years, activeYear)
    }

    private val groupC2Flow = combine(
        repository.allStudentAcademicHistories,
        repository.allPromotions,
        repository.allUsers
    ) { histories, promotions, users ->
        Triple(histories, promotions, users)
    }

    private val groupCFlow = combine(
        groupC1Flow,
        groupC2Flow
    ) { c1, c2 ->
        DataGroupC(
            prints = c1.first,
            years = c1.second,
            activeYear = c1.third,
            histories = c2.first,
            promotions = c2.second,
            users = c2.third
        )
    }

    private val groupDFlow = combine(
        repository.recentAuditLogs,
        repository.allGrades,
        repository.allSubjects
    ) { auditLogs, grades, subjects ->
        DataGroupD(auditLogs, grades, subjects)
    }

    val analyticsState: StateFlow<DashboardAnalyticsState> = combine(
        groupAFlow,
        groupBFlow,
        groupCFlow,
        groupDFlow
    ) { gA, gB, gC, gD ->
        buildAnalyticsState(
            filter = gA.filter,
            students = gA.students,
            teachers = gA.teachers,
            attendance = gA.attendance,
            marks = gA.marks,
            assessments = gB.assessments,
            holistic = gB.holistic,
            sgi = gB.sgi,
            reports = gB.reports,
            pdfs = gB.pdfs,
            prints = gC.prints,
            years = gC.years,
            activeYear = gC.activeYear,
            histories = gC.histories,
            promotions = gC.promotions,
            users = gC.users,
            auditLogs = gD.auditLogs,
            grades = gD.grades,
            subjects = gD.subjects
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardAnalyticsState()
    )


    private fun buildAnalyticsState(
        filter: FilterContainer,
        students: List<StudentEntity>,
        teachers: List<TeacherEntity>,
        attendance: List<AttendanceRecordEntity>,
        marks: List<StudentMarkEntity>,
        assessments: List<AssessmentEntity>,
        holistic: List<HolisticResultEntity>,
        sgi: List<SgiResultEntity>,
        reports: List<ReportGenerationHistoryEntity>,
        pdfs: List<PdfExportHistoryEntity>,
        prints: List<PrintHistoryEntity>,
        years: List<AcademicYearEntity>,
        activeYear: AcademicYearEntity?,
        histories: List<StudentAcademicHistoryEntity>,
        promotions: List<PromotionHistoryEntity>,
        users: List<UserEntity>,
        auditLogs: List<AuditLogEntity>,
        grades: List<GradeEntity>,
        subjects: List<SubjectEntity>
    ): DashboardAnalyticsState {

        val activeYearName = activeYear?.yearCode ?: years.firstOrNull { it.isCurrentActive }?.yearCode ?: years.firstOrNull()?.yearCode ?: ""
        android.util.Log.d("AcademicYearTrace", "DASHBOARD_ANALYTICS: Computing activeYearName='$activeYearName', activeYearParam='${activeYear?.yearCode}'")

        // Filter lists for Dropdowns
        val availableYears = (listOf("All") + years.map { it.yearCode } + if (activeYearName.isNotBlank()) listOf(activeYearName) else emptyList()).distinct()
        val availableGradesList = listOf("All") + (grades.map { it.gradeName } + students.map { it.gradeName }).distinct().sorted()
        val availableClassesList = listOf("All") + students.map { it.className }.distinct().sorted()

        val historiesByStudent = histories.groupBy { it.studentId }
        val targetYear = filter.selectedAcademicYear

        val yearStudents = if (targetYear == "All" || targetYear.isBlank()) {
            students
        } else {
            students.mapNotNull { student ->
                val studentHistories = historiesByStudent[student.id] ?: emptyList()
                val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(targetYear, ignoreCase = true) }

                if (yearHistory != null) {
                    student.copy(
                        gradeName = yearHistory.gradeName,
                        className = yearHistory.className,
                        rollNumber = yearHistory.rollNumber,
                        status = yearHistory.status
                    )
                } else if (studentHistories.isEmpty()) {
                    if (targetYear.equals(activeYearName, ignoreCase = true)) student else null
                } else {
                    null
                }
            }
        }

        // Apply filters to students
        val filteredStudents = yearStudents.filter { s ->
            (filter.selectedGrade == "All" || s.gradeName == filter.selectedGrade) &&
            (filter.selectedClass == "All" || s.className == filter.selectedClass) &&
            (filter.searchQuery.isBlank() ||
                s.name.contains(filter.searchQuery, ignoreCase = true) ||
                s.studentCode.contains(filter.searchQuery, ignoreCase = true) ||
                s.rollNumber.toString().contains(filter.searchQuery, ignoreCase = true) ||
                s.gradeName.contains(filter.searchQuery, ignoreCase = true) ||
                s.className.contains(filter.searchQuery, ignoreCase = true))
        }

        val filteredStudentIds = filteredStudents.map { it.id }.toSet()

        // Overview Calculations
        val totalStudentsCount = filteredStudents.size
        val totalTeachersCount = teachers.size

        val totalClassesCount = if (filteredStudents.isEmpty()) 0 else filteredStudents.map { "${it.gradeName}-${it.className}" }.distinct().size
        val totalGradesCount = if (filteredStudents.isEmpty()) 0 else filteredStudents.map { it.gradeName }.distinct().size
        val totalSubjectsCount = subjects.size

        // Attendance Calculations
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val filteredAttendance = if (filteredStudents.isEmpty()) {
            emptyList()
        } else {
            attendance.filter { a ->
                filteredStudentIds.contains(a.studentId) &&
                (filter.selectedAcademicYear == "All" || a.academicYear == filter.selectedAcademicYear) &&
                (filter.selectedGrade == "All" || a.grade == filter.selectedGrade) &&
                (filter.selectedClass == "All" || a.className == filter.selectedClass)
            }
        }

        val todayAttendanceRecords = filteredAttendance.filter { it.date == todayStr }
        val todayPresent = todayAttendanceRecords.count { it.status == AttendanceStatus.PRESENT }
        val todayAbsent = todayAttendanceRecords.count { it.status == AttendanceStatus.ABSENT }
        val todayLate = todayAttendanceRecords.count { it.status == AttendanceStatus.LATE }
        val todayLeave = todayAttendanceRecords.count { it.status == AttendanceStatus.LEAVE }
        val todayTotal = todayAttendanceRecords.size
        val todayPercentage = if (todayTotal > 0) (todayPresent.toFloat() / todayTotal) * 100f else 0f

        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthlyAttendanceRecords = filteredAttendance.filter { it.date.startsWith(currentMonthStr) }
        val monthlyPresent = monthlyAttendanceRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
        val monthlyTotal = monthlyAttendanceRecords.size
        val monthlyPercentage = if (monthlyTotal > 0) (monthlyPresent.toFloat() / monthlyTotal) * 100f else 0f

        val yearlyPresent = filteredAttendance.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
        val yearlyTotal = filteredAttendance.size
        val yearlyPercentage = if (yearlyTotal > 0) (yearlyPresent.toFloat() / yearlyTotal) * 100f else 0f

        // Attendance Trends
        val attendanceTrends = filteredAttendance
            .groupBy { if (it.date.length >= 7) it.date.substring(0, 7) else it.date }
            .mapValues { entry ->
                val p = entry.value.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
                val t = entry.value.size
                if (t > 0) (p.toFloat() / t) * 100f else 0f
            }

        // Student Attendance Summaries
        val studentAttendanceSummaries = filteredStudents.map { student ->
            val records = filteredAttendance.filter { it.studentId == student.id }
            val tDays = records.size
            val pDays = records.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
            val pct = if (tDays > 0) (pDays.toFloat() / tDays) * 100f else 0f
            StudentAttendanceSummary(
                studentId = student.id,
                name = student.name,
                rollNo = student.rollNumber.toString(),
                grade = student.gradeName,
                className = student.className,
                totalDays = tDays,
                presentDays = pDays,
                percentage = pct
            )
        }

        val lowAttendanceList = studentAttendanceSummaries.filter { it.totalDays > 0 && it.percentage < 75f }.sortedBy { it.percentage }
        val topRegularList = studentAttendanceSummaries.filter { it.totalDays > 0 && it.percentage >= 95f }.sortedByDescending { it.percentage }

        // Student Analytics
        val studentsByGradeMap = filteredStudents.groupBy { it.gradeName }.mapValues { it.value.size }
        val studentsByClassMap = filteredStudents.groupBy { "${it.gradeName} - ${it.className}" }.mapValues { it.value.size }
        val genderMap = filteredStudents.groupBy { it.gender.ifBlank { "Unspecified" } }.mapValues { it.value.size }
        val activeStudents = filteredStudents.count { it.status.equals("Active", ignoreCase = true) }
        val transferredStudents = filteredStudents.count { it.status.equals("Transferred", ignoreCase = true) }
        val graduatedStudents = filteredStudents.count { it.status.equals("Graduated", ignoreCase = true) }

        // Assessments & Pending
        val pendingAssessmentsCount = assessments.count {
            it.status == AssessmentStatus.DRAFT || it.status == AssessmentStatus.PUBLISHED
        }

        // Academic Analytics
        val filteredMarks = if (filteredStudents.isEmpty()) {
            emptyList()
        } else {
            marks.filter { m ->
                filteredStudentIds.contains(m.studentId) && m.obtainedMarks != null
            }
        }

        // Assessment Map
        val assessmentMap = assessments.associateBy { it.id }

        val avgScoreByGradeMap = filteredMarks
            .groupBy { m -> assessmentMap[m.assessmentId]?.grade ?: "G-1" }
            .mapValues { entry ->
                val validMarks = entry.value.mapNotNull { it.obtainedMarks }
                if (validMarks.isNotEmpty()) validMarks.average().toFloat() else 0f
            }

        val avgScoreBySubjectMap = filteredMarks
            .groupBy { it.subjectName }
            .mapValues { entry ->
                val validMarks = entry.value.mapNotNull { it.obtainedMarks }
                if (validMarks.isNotEmpty()) validMarks.average().toFloat() else 0f
            }

        val assessmentTypePerfMap = filteredMarks
            .groupBy { m -> assessmentMap[m.assessmentId]?.assessmentType ?: "Monthly Test" }
            .mapValues { entry ->
                val validMarks = entry.value.mapNotNull { it.obtainedMarks }
                if (validMarks.isNotEmpty()) validMarks.average().toFloat() else 0f
            }

        val overallAcadAvg = if (filteredMarks.isNotEmpty()) {
            filteredMarks.mapNotNull { it.obtainedMarks }.average().toFloat()
        } else 0f

        // Highest & Lowest Score Students
        val studentMarksGrouped = filteredMarks.groupBy { it.studentId }
        var highestPair: Pair<String, Float>? = null
        var lowestPair: Pair<String, Float>? = null

        if (studentMarksGrouped.isNotEmpty()) {
            val studentAverages = studentMarksGrouped.mapNotNull { (sId, markList) ->
                val studentName = filteredStudents.find { it.id == sId }?.name ?: markList.firstOrNull()?.studentName ?: "Student #$sId"
                val validMarks = markList.mapNotNull { it.obtainedMarks }
                if (validMarks.isNotEmpty()) Pair(studentName, validMarks.average().toFloat()) else null
            }
            highestPair = studentAverages.maxByOrNull { it.second }
            lowestPair = studentAverages.minByOrNull { it.second }
        }

        // HCM Analytics
        val filteredHolistic = if (filteredStudents.isEmpty()) {
            emptyList()
        } else {
            holistic.filter { h ->
                filteredStudentIds.contains(h.studentId)
            }
        }

        val honestyList = filteredHolistic.filter { it.pillar == "HONESTY" }.map { it.ratingStars.toFloat() }
        val honestyAvg = if (honestyList.isNotEmpty()) honestyList.average().toFloat() else 0f

        val curiosityList = filteredHolistic.filter { it.pillar == "CURIOSITY" }.map { it.ratingStars.toFloat() }
        val curiosityAvg = if (curiosityList.isNotEmpty()) curiosityList.average().toFloat() else 0f

        val mindfulnessList = filteredHolistic.filter { it.pillar == "MINDFULNESS" }.map { it.ratingStars.toFloat() }
        val mindfulnessAvg = if (mindfulnessList.isNotEmpty()) mindfulnessList.average().toFloat() else 0f

        val overallHcm = if (filteredHolistic.isNotEmpty()) (honestyAvg + curiosityAvg + mindfulnessAvg) / 3f else 0f

        val studentHcmList = filteredStudents.mapNotNull { student ->
            val hRecords = filteredHolistic.filter { it.studentId == student.id }
            if (hRecords.isNotEmpty()) {
                val hList = hRecords.filter { it.pillar == "HONESTY" }.map { it.ratingStars.toFloat() }
                val cList = hRecords.filter { it.pillar == "CURIOSITY" }.map { it.ratingStars.toFloat() }
                val mList = hRecords.filter { it.pillar == "MINDFULNESS" }.map { it.ratingStars.toFloat() }

                val h = if (hList.isNotEmpty()) hList.average().toFloat() else 0f
                val c = if (cList.isNotEmpty()) cList.average().toFloat() else 0f
                val m = if (mList.isNotEmpty()) mList.average().toFloat() else 0f
                val ov = (h + c + m) / 3f

                StudentHcmSummary(
                    studentId = student.id,
                    name = student.name,
                    rollNo = student.rollNumber.toString(),
                    grade = student.gradeName,
                    honesty = h,
                    curiosity = c,
                    mindfulness = m,
                    overallScore = ov
                )
            } else null
        }

        val topHcmList = studentHcmList.sortedByDescending { it.overallScore }.take(5)
        val supportHcmList = studentHcmList.sortedBy { it.overallScore }.take(5)

        // SGI Analytics
        val filteredSgi = if (filteredStudents.isEmpty()) {
            emptyList()
        } else {
            sgi.filter { s ->
                filteredStudentIds.contains(s.studentId)
            }
        }

        val sgiRatings = filteredSgi.map { it.ratingStars.toFloat() }
        val avgSgiVal = if (sgiRatings.isNotEmpty()) sgiRatings.average().toFloat() else 0f
        val highestSgiVal = if (sgiRatings.isNotEmpty()) (sgiRatings.maxOrNull() ?: 0f) else 0f
        val lowestSgiVal = if (sgiRatings.isNotEmpty()) (sgiRatings.minOrNull() ?: 0f) else 0f

        val sgiTrendsMap = filteredSgi.groupBy { it.assessmentPeriod }.mapValues { entry ->
            val list = entry.value.map { it.ratingStars.toFloat() }
            if (list.isNotEmpty()) list.average().toFloat() else 0f
        }

        // Report Analytics
        val filteredReports = if (filteredStudents.isEmpty()) {
            emptyList()
        } else {
            reports.filter { r -> filteredStudentIds.contains(r.studentId) }
        }
        val totalReports = filteredReports.size
        val monthlyReports = filteredReports.count { r ->
            r.generatedAtTimestamp >= (System.currentTimeMillis() - 30L * 24 * 3600 * 1000)
        }
        val academicYearReports = filteredReports.size
        val pdfCount = pdfs.size
        val printCountVal = prints.size

        // Recent Activities List (Combined Timeline)
        val activityList = mutableListOf<RecentActivityItem>()

        // 1. Audit Logs
        auditLogs.take(15).forEach { log ->
            activityList.add(
                RecentActivityItem(
                    id = "audit_${log.id}",
                    title = "${log.userName} (${log.roleName})",
                    subtitle = "${log.action}: ${log.details}",
                    timestamp = log.timestamp,
                    category = ActivityCategory.AUDIT
                )
            )
        }

        // 2. Attendance Records
        attendance.take(10).forEach { att ->
            activityList.add(
                RecentActivityItem(
                    id = "att_${att.id}",
                    title = "Attendance Marked (${att.grade} - ${att.className})",
                    subtitle = "Status: ${att.status.displayName} for Date ${att.date}",
                    timestamp = att.recordedDateTime,
                    category = ActivityCategory.ATTENDANCE
                )
            )
        }

        // 3. Reports
        reports.take(10).forEach { rep ->
            val sName = students.find { it.id == rep.studentId }?.name ?: "Student #${rep.studentId}"
            activityList.add(
                RecentActivityItem(
                    id = "rep_${rep.id}",
                    title = "Report Card Generated",
                    subtitle = "Student: $sName • Period: ${rep.assessmentPeriodName}",
                    timestamp = rep.generatedAtTimestamp,
                    category = ActivityCategory.REPORT
                )
            )
        }

        // 4. Promotions
        promotions.take(10).forEach { promo ->
            val sName = students.find { it.id == promo.studentId }?.name ?: promo.studentName
            activityList.add(
                RecentActivityItem(
                    id = "promo_${promo.id}",
                    title = "Academic Promotion",
                    subtitle = "$sName promoted from ${promo.fromGrade} to ${promo.toGrade} (${promo.actionType.displayName})",
                    timestamp = promo.promotedDate,
                    category = ActivityCategory.PROMOTION
                )
            )
        }

        val sortedActivities = activityList.sortedByDescending { it.timestamp }.take(25)

        return DashboardAnalyticsState(
            searchQuery = filter.searchQuery,
            selectedAcademicYear = filter.selectedAcademicYear,
            selectedGrade = filter.selectedGrade,
            selectedClass = filter.selectedClass,
            selectedAssessmentType = filter.selectedAssessmentType,
            totalStudents = totalStudentsCount,
            totalTeachers = totalTeachersCount,
            totalClasses = totalClassesCount,
            totalGrades = totalGradesCount,
            activeAcademicYearName = activeYearName,
            totalSubjects = totalSubjectsCount,
            todayAttendancePercentage = todayPercentage,
            monthlyAttendancePercentage = monthlyPercentage,
            pendingAssessmentsCount = pendingAssessmentsCount,
            generatedReportCardsCount = totalReports,
            studentsByGradeMap = studentsByGradeMap,
            studentsByClassMap = studentsByClassMap,
            genderDistributionMap = genderMap,
            activeStudentsCount = activeStudents,
            transferredStudentsCount = transferredStudents,
            graduatedStudentsCount = graduatedStudents,
            todayAttendancePresentCount = todayPresent,
            todayAttendanceAbsentCount = todayAbsent,
            todayAttendanceLateCount = todayLate,
            todayAttendanceLeaveCount = todayLeave,
            yearlyAttendancePercentage = yearlyPercentage,
            attendanceTrendsMap = attendanceTrends,
            lowAttendanceStudents = lowAttendanceList,
            topRegularStudents = topRegularList,
            assessmentTypePerformanceMap = assessmentTypePerfMap,
            overallAcademicAverage = overallAcadAvg,
            averageScoreByGradeMap = avgScoreByGradeMap,
            averageScoreBySubjectMap = avgScoreBySubjectMap,
            highestScoreStudent = highestPair,
            lowestScoreStudent = lowestPair,
            averageHonesty = honestyAvg,
            averageCuriosity = curiosityAvg,
            averageMindfulness = mindfulnessAvg,
            overallHcmScore = overallHcm,
            topHcmStudents = topHcmList,
            supportRequiredHcmStudents = supportHcmList,
            averageSgi = avgSgiVal,
            highestSgi = highestSgiVal,
            lowestSgi = lowestSgiVal,
            sgiGrowthTrendsMap = sgiTrendsMap,
            totalReportsGenerated = totalReports,
            monthlyReportsCount = monthlyReports,
            academicYearReportsCount = academicYearReports,
            pdfExportCount = pdfCount,
            printCount = printCountVal,
            recentActivities = sortedActivities,
            availableAcademicYears = availableYears,
            availableGrades = availableGradesList,
            availableClasses = availableClassesList
        )
    }
}

private data class FilterContainer(
    val searchQuery: String,
    val selectedAcademicYear: String,
    val selectedGrade: String,
    val selectedClass: String,
    val selectedAssessmentType: String
)

private data class DataGroupA(
    val filter: FilterContainer,
    val students: List<StudentEntity>,
    val teachers: List<TeacherEntity>,
    val attendance: List<AttendanceRecordEntity>,
    val marks: List<StudentMarkEntity>
)

private data class DataGroupB(
    val assessments: List<AssessmentEntity>,
    val holistic: List<HolisticResultEntity>,
    val sgi: List<SgiResultEntity>,
    val reports: List<ReportGenerationHistoryEntity>,
    val pdfs: List<PdfExportHistoryEntity>
)

private data class DataGroupC(
    val prints: List<PrintHistoryEntity>,
    val years: List<AcademicYearEntity>,
    val activeYear: AcademicYearEntity?,
    val histories: List<StudentAcademicHistoryEntity>,
    val promotions: List<PromotionHistoryEntity>,
    val users: List<UserEntity>
)

private data class DataGroupD(
    val auditLogs: List<AuditLogEntity>,
    val grades: List<GradeEntity>,
    val subjects: List<SubjectEntity>
)


class DashboardViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
