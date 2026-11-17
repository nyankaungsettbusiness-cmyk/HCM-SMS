package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.MarksDao
import com.example.data.local.entity.*
import com.example.data.repository.HolisticRepository
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class HolisticViewModel(
    private val holisticRepository: HolisticRepository,
    private val studentRepository: StudentRepository,
    private val academicYearRepository: com.example.data.repository.AcademicYearRepository? = null,
    private val marksDao: MarksDao? = null,
    private val attendanceDao: AttendanceDao? = null,
    private val assessmentDao: AssessmentDao? = null
) : ViewModel() {

    // Filter Controls
    val academicYear = MutableStateFlow("2026-2027")

    init {
        viewModelScope.launch {
            val activeYear = academicYearRepository?.getActiveAcademicYear()?.firstOrNull()?.yearCode
            if (!activeYear.isNullOrBlank()) {
                academicYear.value = activeYear
            }
        }
    }

    val selectedGrade = MutableStateFlow("G5")
    val selectedClass = MutableStateFlow("All")
    val selectedPeriod = MutableStateFlow("June")
    val searchQuery = MutableStateFlow("")
    val selectedStudent = MutableStateFlow<StudentEntity?>(null)

    // Admin Level Selector for Config tab
    val adminConfigLevel = MutableStateFlow("PRIMARY")

    private val academicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearRepository?.getAllStudentAcademicHistories()
        ?: flowOf(emptyList())

    // Education Level derived from selectedStudent or selectedGrade
    val activeEducationLevel: StateFlow<String> = combine(selectedGrade, selectedStudent) { grade, student ->
        val targetGrade = student?.gradeName ?: grade
        val norm = targetGrade.uppercase().trim()
        when {
            norm == "KG" || norm.contains("KINDERGARTEN") -> "KINDERGARTEN"
            norm in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5", "PRIMARY 1", "PRIMARY 2", "PRIMARY 3", "PRIMARY 4", "PRIMARY 5") || norm.startsWith("PRIMARY") -> "PRIMARY"
            norm in listOf("G6", "G7", "G8", "G9", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") || norm.startsWith("SECONDARY") -> "SECONDARY"
            norm in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") || norm.startsWith("HIGH") -> "HIGH_SCHOOL"
            else -> "PRIMARY"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PRIMARY")

    // Dynamic Assessment Periods for active grade/level
    val enabledAssessmentPeriodsForLevel: StateFlow<List<AssessmentPeriodEntity>> = activeEducationLevel.flatMapLatest { level ->
        holisticRepository.getEnabledAssessmentPeriodsForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePeriods: StateFlow<List<String>> = enabledAssessmentPeriodsForLevel.map { periods ->
        val standardMonths = listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
        if (periods.isEmpty()) {
            standardMonths
        } else {
            val validMonths = periods.map { it.periodName }.filter { standardMonths.contains(it) }
            if (validMonths.isEmpty()) standardMonths else validMonths
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
    )

    init {
        viewModelScope.launch {
            availablePeriods.collect { periods ->
                if (periods.isNotEmpty() && !periods.contains(selectedPeriod.value)) {
                    selectedPeriod.value = periods.first()
                }
            }
        }
    }

    // Admin Assessment Periods for selected admin level
    val adminAssessmentPeriods: StateFlow<List<AssessmentPeriodEntity>> = adminConfigLevel.flatMapLatest { level ->
        holisticRepository.getAssessmentPeriodsForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Lists for current student's education level
    val holisticCategoriesForLevel: StateFlow<List<HolisticCategoryEntity>> = activeEducationLevel.flatMapLatest { level ->
        holisticRepository.getHolisticCategoriesForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledHolisticCategoriesForLevel: StateFlow<List<HolisticCategoryEntity>> = activeEducationLevel.flatMapLatest { level ->
        holisticRepository.getEnabledHolisticCategoriesForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Config Categories list for selected admin level
    val adminHolisticCategories: StateFlow<List<HolisticCategoryEntity>> = adminConfigLevel.flatMapLatest { level ->
        holisticRepository.getHolisticCategoriesForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHolisticCategories: StateFlow<List<HolisticCategoryEntity>> = holisticRepository.allHolisticCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sgiCategories: StateFlow<List<SgiCategoryEntity>> = holisticRepository.allSgiCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledSgiCategories: StateFlow<List<SgiCategoryEntity>> = holisticRepository.enabledSgiCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class StudentFilterParams(
        val grade: String,
        val className: String,
        val query: String,
        val year: String
    )

    private val filterParamsFlow = combine(
        selectedGrade,
        selectedClass,
        searchQuery,
        academicYear
    ) { grade, className, query, year ->
        StudentFilterParams(grade, className, query, year)
    }

    // Filtered Students scoped to Academic Year
    val filteredStudents: StateFlow<List<StudentEntity>> = combine(
        filterParamsFlow,
        studentRepository.allStudents,
        academicHistories
    ) { params, all, histories ->
        val historiesByStudent = histories.groupBy { it.studentId }

        all.mapNotNull { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(params.year, ignoreCase = true) }

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
                if (params.year.equals(activeCode, ignoreCase = true)) {
                    student
                } else {
                    null
                }
            }
        }.filter { student ->
            val normGrade = student.gradeName.trim()
            val matchGrade = (params.grade == "All" || normGrade.equals(params.grade.trim(), ignoreCase = true) ||
                    (params.grade.trim().equals("KG", ignoreCase = true) && (normGrade.contains("KG", ignoreCase = true) || normGrade.contains("Kindergarten", ignoreCase = true))))

            val matchClass = (params.className == "All" || params.className == "All Classes" || params.className.isBlank() ||
                    student.className.equals(params.className, ignoreCase = true) ||
                    (params.className == "A" && (student.className.isBlank() || student.className.equals("None", ignoreCase = true) || student.className == "-")))

            val matchQuery = params.query.isBlank() ||
                    student.name.contains(params.query, ignoreCase = true) ||
                    student.studentCode.contains(params.query, ignoreCase = true) ||
                    student.rollNumber.toString() == params.query.trim()
            matchGrade && matchClass && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Ratings & Comments State
    val holisticRatings = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val sgiRatings = MutableStateFlow<Map<Long, Int>>(emptyMap())

    val positiveComments = MutableStateFlow("")
    val areasForImprovement = MutableStateFlow("")
    val generalComment = MutableStateFlow("")
    val futureRecommendation = MutableStateFlow("")

    val statusMessage = MutableStateFlow<String?>(null)

    // Auto-Calculated Scores State
    val averageHonestyScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val honestyItems = categories.filter { it.pillar.equals("HONESTY", ignoreCase = true) }
        val rated = honestyItems.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageCuriosityScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val curiosityItems = categories.filter { it.pillar.equals("CURIOSITY", ignoreCase = true) }
        val rated = curiosityItems.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageMindfulnessScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val mindfulnessItems = categories.filter { it.pillar.equals("MINDFULNESS", ignoreCase = true) }
        val rated = mindfulnessItems.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val overallHcmScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val rated = categories.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val sgiAverageScore: StateFlow<Double> = combine(enabledSgiCategories, sgiRatings) { categories, ratings ->
        val rated = categories.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // Keep selectedStudent null initially so student list is shown first
    }

    fun selectStudent(student: StudentEntity?) {
        selectedStudent.value = student
        if (student != null) {
            loadAssessmentForStudent(student.id)
        } else {
            clearRatingsAndComments()
        }
    }

    fun onFilterChanged() {
        // When grade, class, period or academic year changes, go back to student list view
        selectedStudent.value = null
        clearRatingsAndComments()
    }

    private fun clearRatingsAndComments() {
        holisticRatings.value = emptyMap()
        sgiRatings.value = emptyMap()
        positiveComments.value = ""
        areasForImprovement.value = ""
        generalComment.value = ""
        futureRecommendation.value = ""
    }

    fun loadAssessmentForStudent(studentId: Long) {
        val period = selectedPeriod.value
        val year = academicYear.value

        viewModelScope.launch {
            // Observe Holistic Results
            holisticRepository.getHolisticResultsForStudent(studentId, period, year)
                .take(1)
                .collect { results ->
                    val map = results.associate { it.categoryId to it.ratingStars }
                    holisticRatings.value = map
                }

            // Observe SGI Results
            holisticRepository.getSgiResultsForStudent(studentId, period, year)
                .take(1)
                .collect { results ->
                    val map = results.associate { it.sgiCategoryId to it.ratingStars }
                    sgiRatings.value = map
                }

            // Observe Teacher Comment
            holisticRepository.getTeacherCommentForStudent(studentId, period, year)
                .take(1)
                .collect { comment ->
                    if (comment != null) {
                        positiveComments.value = comment.positiveComments
                        areasForImprovement.value = comment.areasForImprovement
                        generalComment.value = comment.generalComment
                        futureRecommendation.value = comment.futureRecommendation
                    } else {
                        positiveComments.value = ""
                        areasForImprovement.value = ""
                        generalComment.value = ""
                        futureRecommendation.value = ""
                    }
                }
        }
    }

    fun updateHolisticRating(categoryId: Long, stars: Int) {
        val current = holisticRatings.value.toMutableMap()
        current[categoryId] = stars
        holisticRatings.value = current
    }

    fun updateSgiRating(sgiCategoryId: Long, stars: Int) {
        val current = sgiRatings.value.toMutableMap()
        current[sgiCategoryId] = stars
        sgiRatings.value = current
    }

    /**
     * Generates factual, individual-specific teacher observations and remarks
     * based on student's actual Academic performance, Holistic HCM ratings, and Attendance records.
     */
    fun generatePersonalizedTeacherComments() {
        val student = selectedStudent.value ?: return
        val period = selectedPeriod.value
        val year = academicYear.value

        viewModelScope.launch {
            try {
                // 1. Fetch Academic Marks for this student in this academic year
                val studentMarks = marksDao?.getMarksForStudent(student.id)?.firstOrNull() ?: emptyList()
                val assessments = assessmentDao?.getAssessmentsByAcademicYear(year)?.firstOrNull() ?: emptyList()

                // Filter assessments matching this period/month or matching student grade
                val periodAssessmentIds = assessments
                    .filter { it.month.equals(period, ignoreCase = true) || it.assessmentName.contains(period, ignoreCase = true) }
                    .map { it.id }
                    .toSet()

                val relevantMarks = if (periodAssessmentIds.isNotEmpty()) {
                    studentMarks.filter { it.assessmentId in periodAssessmentIds && it.obtainedMarks != null }
                } else {
                    studentMarks.filter { it.obtainedMarks != null }
                }

                val distinctionSubjects = relevantMarks.filter { it.isDistinction || ((it.obtainedMarks ?: 0.0) >= it.distinctionMark) }
                val passSubjects = relevantMarks.filter { (it.obtainedMarks ?: 0.0) >= it.passMark && !it.isDistinction && ((it.obtainedMarks ?: 0.0) < it.distinctionMark) }
                val failSubjects = relevantMarks.filter { (it.obtainedMarks ?: 0.0) < it.passMark }

                val avgMarks = if (relevantMarks.isNotEmpty()) {
                    relevantMarks.mapNotNull { it.obtainedMarks }.average().roundToInt()
                } else 0

                // 2. Fetch Holistic HCM Ratings from current in-memory / evaluated state
                val enabledHolistic = enabledHolisticCategoriesForLevel.value
                val ratedCategories = enabledHolistic.map { cat ->
                    val stars = holisticRatings.value[cat.id] ?: 0
                    cat to stars
                }

                val highRatedCategories = ratedCategories.filter { it.second >= 4 }.map { it.first }
                val lowRatedCategories = ratedCategories.filter { it.second in 1..2 }.map { it.first }
                val midRatedCategories = ratedCategories.filter { it.second == 3 }.map { it.first }

                val honestyStars = ratedCategories.filter { it.first.pillar.equals("HONESTY", ignoreCase = true) }.map { it.second }
                val curiosityStars = ratedCategories.filter { it.first.pillar.equals("CURIOSITY", ignoreCase = true) }.map { it.second }
                val mindfulnessStars = ratedCategories.filter { it.first.pillar.equals("MINDFULNESS", ignoreCase = true) }.map { it.second }

                val honestyAvg = if (honestyStars.isNotEmpty()) honestyStars.average() else 0.0
                val curiosityAvg = if (curiosityStars.isNotEmpty()) curiosityStars.average() else 0.0
                val mindfulnessAvg = if (mindfulnessStars.isNotEmpty()) mindfulnessStars.average() else 0.0

                // 3. Fetch Attendance
                val attendanceRecords = attendanceDao?.getAttendanceForStudent(student.id, year)?.firstOrNull() ?: emptyList()
                val monthNum = when (period.lowercase()) {
                    "june" -> "06"; "july" -> "07"; "august" -> "08"; "september" -> "09"
                    "october" -> "10"; "november" -> "11"; "december" -> "12"
                    "january" -> "01"; "february" -> "02"; "march" -> "03"; "april" -> "04"; "may" -> "05"
                    else -> ""
                }
                val monthRecords = if (monthNum.isNotBlank()) {
                    attendanceRecords.filter { it.date.contains("-$monthNum-") }
                } else emptyList()
                val activeRecords = if (monthRecords.isNotEmpty()) monthRecords else attendanceRecords

                val totalAttendanceDays = activeRecords.size
                val presentDays = activeRecords.count { it.status == AttendanceStatus.PRESENT }
                val absentDays = activeRecords.count { it.status == AttendanceStatus.ABSENT }
                val lateDays = activeRecords.count { it.status == AttendanceStatus.LATE }
                val attendanceRate = if (totalAttendanceDays > 0) ((presentDays.toDouble() / totalAttendanceDays) * 100).roundToInt() else 100

                // === BUILD POSITIVE STRENGTHS ===
                val positiveParts = mutableListOf<String>()

                if (distinctionSubjects.isNotEmpty()) {
                    val distNames = distinctionSubjects.take(3).joinToString("၊ ") {
                        val markInt = it.obtainedMarks?.roundToInt() ?: 0
                        "${it.subjectName} (${markInt} မှတ်)"
                    }
                    positiveParts.add("$distNames တို့တွင် Distinction ဂုဏ်ထူးအမှတ်များဖြင့် ထူးချွန်စွာ အောင်မြင်ထားပါသည်။")
                } else if (relevantMarks.isNotEmpty() && failSubjects.isEmpty()) {
                    positiveParts.add("စာမေးပွဲတွင် ဘာသာရပ်အားလုံးကို ပျမ်းမျှ $avgMarks မှတ်ဖြင့် အောင်မြင်စွာ ဖြေဆိုထားပါသည်။")
                }

                if (highRatedCategories.isNotEmpty()) {
                    val highNames = highRatedCategories.take(2).joinToString("၊ ") { it.categoryName }
                    positiveParts.add("$highNames စသည့် အပြုအမူကောင်းများတွင် စံပြဖြစ်ပြီး စိတ်အားထက်သန်မှု ကောင်းမွန်ပါသည်။")
                } else {
                    val highestPillar = when {
                        honestyAvg >= curiosityAvg && honestyAvg >= mindfulnessAvg && honestyAvg > 0 -> "ရိုးသားဖြောင့်မတ်မှု (Honesty)"
                        curiosityAvg >= honestyAvg && curiosityAvg >= mindfulnessAvg && curiosityAvg > 0 -> "စူးစမ်းလေ့လာလိုစိတ် (Curiosity)"
                        mindfulnessAvg > 0 -> "သတိတရားနှင့် ကိုယ်ကျင့်တရား (Mindfulness)"
                        else -> null
                    }
                    if (highestPillar != null) {
                        positiveParts.add("အထူးသဖြင့် $highestPillar ပိုင်းတွင် အလေ့အကျင့်ကောင်းများ ပိုင်ဆိုင်ထားပါသည်။")
                    }
                }

                if (totalAttendanceDays > 0) {
                    if (attendanceRate >= 95 && absentDays == 0) {
                        positiveParts.add("ကျောင်းခေါ်ချိန် ($attendanceRate%) ပြည့်မီပြီး ပျက်ကွက်ရက်မရှိ ကျောင်းမှန်မှန် တက်ရောက်ပါသည်။")
                    } else if (attendanceRate >= 90) {
                        positiveParts.add("ကျောင်းခေါ်ချိန် ($attendanceRate%) ကောင်းမွန်စွာ ရှိပါသည်။")
                    }
                }

                if (positiveParts.isEmpty()) {
                    positiveParts.add("စာသင်ခန်းတွင်း လှုပ်ရှားမှုများတွင် ပုံမှန်ပါဝင်ပြီး တိုးတက်ရန် ကြိုးစားအားထုတ်လိုစိတ် ရှိပါသည်။")
                }

                // === BUILD AREAS FOR IMPROVEMENT ===
                val improvementParts = mutableListOf<String>()

                if (failSubjects.isNotEmpty()) {
                    val failNames = failSubjects.take(3).joinToString("၊ ") {
                        val markInt = it.obtainedMarks?.roundToInt() ?: 0
                        "${it.subjectName} (${markInt} မှတ်)"
                    }
                    improvementParts.add("$failNames ဘာသာရပ်တို့တွင် အမှတ်နည်းနေသဖြင့် (ရမှတ် ၄၀ အောက်) အခြေခံသဘောတရားများနှင့် အိမ်စာလေ့ကျင့်ခန်းများကို အချိန်ပိုပေး၍ အနီးကပ် ပြန်လည်ကုစား သင်ကြားပေးရန် လိုအပ်ပါသည်။")
                } else if (passSubjects.any { (it.obtainedMarks ?: 0.0) < 60 }) {
                    val weakPassNames = passSubjects.filter { (it.obtainedMarks ?: 0.0) < 60 }.take(2).joinToString("၊ ") { "${it.subjectName} (${it.obtainedMarks?.roundToInt()} မှတ်)" }
                    improvementParts.add("$weakPassNames ဘာသာရပ်တို့တွင် အမှတ်အနည်းငယ် လျော့နည်းနေသဖြင့် ပိုမိုအာရုံစိုက် လေ့ကျင့်သင့်ပါသည်။")
                }

                if (lowRatedCategories.isNotEmpty()) {
                    val lowNames = lowRatedCategories.take(2).joinToString("၊ ") { it.categoryName }
                    improvementParts.add("$lowNames ပိုင်းတွင် အာရုံမပျံ့လွင့်စေရန်နှင့် စည်းကမ်းတကျ လိုက်နာတတ်စေရန် သတိပြု ထိန်းကျောင်းပေးရန် လိုအပ်ပါသည်။")
                } else if (midRatedCategories.isNotEmpty()) {
                    val midNames = midRatedCategories.take(1).joinToString("၊ ") { it.categoryName }
                    improvementParts.add("$midNames အလေ့အကျင့်ကို ပိုမိုကောင်းမွန်လာအောင် ဆက်လက် အားပေးသင့်ပါသည်။")
                }

                if (absentDays > 0) {
                    improvementParts.add("ကျောင်း $absentDays ရက် ပျက်ကွက်ခဲ့သဖြင့် (တက်ရောက်မှု $attendanceRate%) သင်ခန်းစာများ မလွတ်စေရန် ကျောင်းခေါ်ချိန်ကို ပိုမိုဂရုပြု တက်ရောက်စေလိုပါသည်။")
                }
                if (lateDays > 0) {
                    improvementParts.add("ကျောင်းသို့ အချိန်မီရောက်ရှိစေရန် မနက်စောစော လာရောက်ဖို့ သတိပေးလိုပါသည်။")
                }

                if (improvementParts.isEmpty()) {
                    improvementParts.add("လက်ရှိ ထူးချွန်သော ရလဒ်ကောင်းများကို ဆက်လက်ထိန်းသိမ်းပြီး ပိုမိုခက်ခဲသော စိန်ခေါ်မှုများနှင့် ဗဟုသုတများကို ရှာဖွေလေ့လာနိုင်ရန် တိုက်တွန်းပါသည်။")
                }

                // === BUILD GENERAL REMARK / FEEDBACK ===
                val isHighAcademic = distinctionSubjects.size >= 2 || (relevantMarks.isNotEmpty() && avgMarks >= 75)
                val isLowAcademic = failSubjects.isNotEmpty() || (relevantMarks.isNotEmpty() && avgMarks < 50)
                val isHighHcm = (honestyAvg >= 4.0 || curiosityAvg >= 4.0 || mindfulnessAvg >= 4.0) && lowRatedCategories.isEmpty()
                val isLowHcm = lowRatedCategories.size >= 2 || (honestyAvg < 2.5 && curiosityAvg < 2.5 && mindfulnessAvg < 2.5 && ratedCategories.any { it.second > 0 })

                val generalText = when {
                    isHighAcademic && isHighHcm ->
                        "${student.name} သည် ပညာရည်ထူးချွန်ပြီး ကိုယ်ကျင့်တရားနှင့် စည်းကမ်းပိုင်းတွင်ပါ စံပြဖြစ်သော ကျောင်းသားကောင်းတစ်ဦး ဖြစ်ပါသည်။ ယခုကြိုးစားမှုအရှိန်ကို ဆက်လက်ထိန်းသိမ်းပါက ပိုမိုထင်ရှားစွာ အောင်မြင်ပါမည်။"
                    isHighAcademic && !isHighHcm ->
                        "ပညာရည်ထူးချွန်သော်လည်း စာသင်ခန်းတွင်း စိတ်ရှည်သည်းခံမှုနှင့် အဖွဲ့လိုက် ပူးပေါင်းဆောင်ရွက်မှုကို ပိုမိုမြှင့်တင်ပေးရန် လိုအပ်ပါသည်။ ဆရာနှင့် မိဘ အတူတကွ ပံ့ပိုးပေးသင့်ပါသည်။"
                    !isHighAcademic && isHighHcm ->
                        "စိတ်သဘောထား ဖြောင့်မတ်ပြီး အလေ့အကျင့်ကောင်းများ ပိုင်ဆိုင်ထားသော ကျောင်းသား ဖြစ်ပါသည်။ အားနည်းနေသော စာများကို မိဘဆရာ ပူးပေါင်း၍ အနီးကပ် တွန်းအားပေးပါက သိသာစွာ တိုးတက်လာမည့် အလားအလာ ကောင်းမွန်ပါသည်။"
                    isLowAcademic && isLowHcm ->
                        "သင်ခန်းစာများနှင့် စည်းကမ်းပိုင်းတွင် ပိုမိုအာရုံစိုက်လာစေရန် မိဘနှင့် ဆရာ နီးကပ်စွာ တိုင်ပင်ဆွေးနွေးပြီး အပြုသဘောဆောင်သော ပံ့ပိုးကူညီမှုများ နေ့စဉ် ပြုလုပ်ပေးရန် လိုအပ်ပါသည်။"
                    else ->
                        "လစဉ် တိုးတက်မှု အလားအလာ ကောင်းမွန်နေပြီး စာကျက်ချိန်နှင့် လေ့ကျင့်ခန်းများကို စနစ်တကျ လုပ်ဆောင်သွားပါက ပိုမိုကောင်းမွန်သော ရလဒ်များ ရရှိလာမည် ဖြစ်ပါသည်။"
                }

                // Populate directly into mutable state flows so teacher can view & edit
                positiveComments.value = positiveParts.joinToString(" ")
                areasForImprovement.value = improvementParts.joinToString(" ")
                generalComment.value = generalText

                statusMessage.value = "Auto-generated personalized remarks for ${student.name} based on Academic, HCM & Attendance!"
            } catch (e: Exception) {
                statusMessage.value = "Error generating comments: ${e.message}"
            }
        }
    }

    fun saveCurrentStudentAssessment(updatedBy: String) {
        val student = selectedStudent.value ?: return
        val period = selectedPeriod.value
        val year = academicYear.value
        val currentLevel = activeEducationLevel.value

        viewModelScope.launch {
            // Save Holistic Results
            val enabledHolistic = enabledHolisticCategoriesForLevel.value
            val holisticEntities = enabledHolistic.map { cat ->
                val stars = holisticRatings.value[cat.id] ?: 0
                HolisticResultEntity(
                    studentId = student.id,
                    assessmentPeriod = period,
                    academicYear = year,
                    grade = student.gradeName,
                    className = student.className,
                    categoryId = cat.id,
                    categoryName = cat.categoryName,
                    pillar = cat.pillar,
                    educationLevel = currentLevel,
                    ratingStars = stars,
                    maxStars = cat.maxStars,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = updatedBy
                )
            }
            holisticRepository.saveHolisticResults(holisticEntities)

            // Save SGI Results
            val enabledSgi = enabledSgiCategories.value
            val sgiEntities = enabledSgi.map { sgi ->
                val stars = sgiRatings.value[sgi.id] ?: 0
                SgiResultEntity(
                    studentId = student.id,
                    assessmentPeriod = period,
                    academicYear = year,
                    grade = student.gradeName,
                    className = student.className,
                    sgiCategoryId = sgi.id,
                    indicatorName = sgi.indicatorName,
                    ratingStars = stars,
                    maxStars = sgi.maxStars,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = updatedBy
                )
            }
            holisticRepository.saveSgiResults(sgiEntities)

            // Save Teacher Comment
            val commentEntity = TeacherCommentEntity(
                studentId = student.id,
                assessmentPeriod = period,
                academicYear = year,
                grade = student.gradeName,
                className = student.className,
                positiveComments = positiveComments.value,
                areasForImprovement = areasForImprovement.value,
                generalComment = generalComment.value,
                futureRecommendation = futureRecommendation.value,
                updatedAt = System.currentTimeMillis(),
                updatedBy = updatedBy
            )
            holisticRepository.saveTeacherComment(commentEntity)

            statusMessage.value = "Assessment saved successfully for ${student.name}!"
        }
    }

    // Category Management
    // Level-scoped HCM Admin Operations
    fun addHolisticCategoryForPillar(name: String, pillar: String, targetLevel: String = adminConfigLevel.value) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = adminHolisticCategories.value.size
            val newCategory = HolisticCategoryEntity(
                categoryName = name.trim(),
                description = "",
                pillar = pillar,
                educationLevel = targetLevel,
                maxStars = 5,
                isEnabled = true,
                isDefault = false,
                orderIndex = count + 1
            )
            holisticRepository.addHolisticCategory(newCategory)
            statusMessage.value = "Added '$name' to $targetLevel HCM items."
        }
    }

    fun updateHolisticCategoryName(category: HolisticCategoryEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val oldName = category.categoryName
            val allCats = allHolisticCategories.value
            val matchingItems = allCats.filter { it.categoryName == oldName && it.pillar.equals(category.pillar, ignoreCase = true) }
            if (matchingItems.isNotEmpty()) {
                matchingItems.forEach { item ->
                    holisticRepository.updateHolisticCategory(item.copy(categoryName = newName.trim()))
                }
            } else {
                holisticRepository.updateHolisticCategory(category.copy(categoryName = newName.trim()))
            }
            statusMessage.value = "Updated HCM Assessment Item."
        }
    }

    fun deleteHolisticCategoryGroup(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            val name = category.categoryName
            val pillar = category.pillar
            val matchingItems = allHolisticCategories.value.filter { it.categoryName == name && it.pillar.equals(pillar, ignoreCase = true) }
            if (matchingItems.isNotEmpty()) {
                matchingItems.forEach { item ->
                    holisticRepository.deleteHolisticCategory(item)
                }
            } else {
                holisticRepository.deleteHolisticCategory(category)
            }
            statusMessage.value = "Deleted HCM Assessment Item."
        }
    }

    fun addHolisticCategory(name: String, description: String, pillar: String, educationLevel: String, maxStars: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = adminHolisticCategories.value.size
            val category = HolisticCategoryEntity(
                categoryName = name.trim(),
                description = description.trim(),
                pillar = pillar,
                educationLevel = educationLevel,
                maxStars = maxStars,
                isEnabled = true,
                orderIndex = count + 1
            )
            holisticRepository.addHolisticCategory(category)
            statusMessage.value = "HCM Item '$name' added for $educationLevel."
        }
    }

    fun updateHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.updateHolisticCategory(category)
            statusMessage.value = "HCM Item updated."
        }
    }

    fun deleteHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.deleteHolisticCategory(category)
            statusMessage.value = "HCM Item deleted."
        }
    }

    fun duplicateHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.duplicateHolisticCategory(category)
            statusMessage.value = "Duplicated '${category.categoryName}'."
        }
    }

    fun toggleHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(isEnabled = !category.isEnabled)
            holisticRepository.updateHolisticCategory(updated)
        }
    }

    fun moveHolisticCategoryOrder(category: HolisticCategoryEntity, moveUp: Boolean) {
        val currentList = adminHolisticCategories.value.sortedBy { it.orderIndex }
        val index = currentList.indexOfFirst { it.id == category.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= currentList.size) return

        val other = currentList[targetIndex]
        viewModelScope.launch {
            val tempIndex = category.orderIndex
            holisticRepository.updateHolisticCategory(category.copy(orderIndex = other.orderIndex))
            holisticRepository.updateHolisticCategory(other.copy(orderIndex = tempIndex))
        }
    }

    fun copyConfigurationBetweenLevels(sourceLevel: String, targetLevel: String) {
        viewModelScope.launch {
            holisticRepository.copyConfigurationBetweenLevels(sourceLevel, targetLevel)
            statusMessage.value = "Successfully copied HCM configuration from $sourceLevel to $targetLevel!"
        }
    }

    // SGI Management
    fun addSgiCategory(name: String, description: String, maxStars: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = sgiCategories.value.size
            val category = SgiCategoryEntity(
                indicatorName = name.trim(),
                description = description.trim(),
                maxStars = maxStars,
                isEnabled = true,
                orderIndex = count + 1
            )
            holisticRepository.addSgiCategory(category)
            statusMessage.value = "SGI Indicator '$name' added."
        }
    }

    fun updateSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.updateSgiCategory(category)
            statusMessage.value = "SGI Indicator updated."
        }
    }

    fun deleteSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.deleteSgiCategory(category)
            statusMessage.value = "Indicator deleted."
        }
    }

    fun duplicateSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.duplicateSgiCategory(category)
            statusMessage.value = "Duplicated '${category.indicatorName}'."
        }
    }

    fun toggleSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(isEnabled = !category.isEnabled)
            holisticRepository.updateSgiCategory(updated)
        }
    }

    fun moveSgiCategoryOrder(category: SgiCategoryEntity, moveUp: Boolean) {
        val currentList = sgiCategories.value.sortedBy { it.orderIndex }
        val index = currentList.indexOfFirst { it.id == category.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= currentList.size) return

        val other = currentList[targetIndex]
        viewModelScope.launch {
            val tempIndex = category.orderIndex
            holisticRepository.updateSgiCategory(category.copy(orderIndex = other.orderIndex))
            holisticRepository.updateSgiCategory(other.copy(orderIndex = tempIndex))
        }
    }

    // Assessment Period Management (Super Admin / Admin)
    fun addAssessmentPeriod(name: String, level: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val currentList = adminAssessmentPeriods.value
            val period = AssessmentPeriodEntity(
                periodName = name.trim(),
                educationLevel = level,
                periodCategory = category.trim(),
                isEnabled = true,
                orderIndex = currentList.size + 1
            )
            holisticRepository.addAssessmentPeriod(period)
            statusMessage.value = "Assessment period '$name' added."
        }
    }

    fun updateAssessmentPeriod(period: AssessmentPeriodEntity) {
        viewModelScope.launch {
            holisticRepository.updateAssessmentPeriod(period)
            statusMessage.value = "Assessment period updated."
        }
    }

    fun deleteAssessmentPeriod(period: AssessmentPeriodEntity) {
        viewModelScope.launch {
            holisticRepository.deleteAssessmentPeriod(period.id)
            statusMessage.value = "Assessment period deleted."
        }
    }

    fun toggleAssessmentPeriod(period: AssessmentPeriodEntity) {
        viewModelScope.launch {
            val updated = period.copy(isEnabled = !period.isEnabled)
            holisticRepository.updateAssessmentPeriod(updated)
        }
    }

    fun moveAssessmentPeriodOrder(period: AssessmentPeriodEntity, moveUp: Boolean) {
        val currentList = adminAssessmentPeriods.value.sortedBy { it.orderIndex }
        val index = currentList.indexOfFirst { it.id == period.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= currentList.size) return

        val other = currentList[targetIndex]
        viewModelScope.launch {
            val tempIndex = period.orderIndex
            holisticRepository.updateAssessmentPeriod(period.copy(orderIndex = other.orderIndex))
            holisticRepository.updateAssessmentPeriod(other.copy(orderIndex = tempIndex))
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}

class HolisticViewModelFactory(
    private val holisticRepository: HolisticRepository,
    private val studentRepository: StudentRepository,
    private val academicYearRepository: com.example.data.repository.AcademicYearRepository? = null,
    private val marksDao: MarksDao? = null,
    private val attendanceDao: AttendanceDao? = null,
    private val assessmentDao: AssessmentDao? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HolisticViewModel::class.java)) {
            return HolisticViewModel(
                holisticRepository,
                studentRepository,
                academicYearRepository,
                marksDao,
                attendanceDao,
                assessmentDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
