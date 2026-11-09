package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.AcademicYearDao
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.MarksDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.StudentMarkEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentRankingItem(
    val rank: Int,
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val displayName: String,
    val rollNo: Int,
    val stream: String,
    val subjectMarks: Map<String, Double?>,
    val totalObtained: Double,
    val totalMax: Int,
    val distinctionCount: Int,
    val distinctionText: String,
    val isPassed: Boolean,
    val resultStatus: String
)

data class RankingResultData(
    val academicYear: String,
    val grade: String,
    val assessment: AssessmentEntity?,
    val subjects: List<String>,
    val rankingItems: List<StudentRankingItem>
)

data class FilterParams(
    val year: String,
    val grade: String,
    val asmId: Long?,
    val subject: String
)

class ExamRankingViewModel(
    private val assessmentDao: AssessmentDao,
    private val marksDao: MarksDao,
    private val studentDao: StudentDao,
    private val academicYearDao: AcademicYearDao
) : ViewModel() {

    val selectedAcademicYear = MutableStateFlow("2026-2027")

    init {
        viewModelScope.launch {
            val activeYear = academicYearDao.getActiveAcademicYear().firstOrNull()?.yearCode
            if (!activeYear.isNullOrBlank()) {
                selectedAcademicYear.value = activeYear
            }
        }
    }

    val selectedGrade = MutableStateFlow("G1")
    val selectedAssessmentId = MutableStateFlow<Long?>(null)
    val selectedSubject = MutableStateFlow("ALL")

    val availableAcademicYears: StateFlow<List<String>> = academicYearDao.getAllAcademicYears()
        .map { list ->
            val yearCodes = list.map { it.yearCode }
            val defaultYears = listOf("2026-2027", "2027-2028", "2028-2029")
            (yearCodes + defaultYears)
                .filterNot { it == "2024-2025" }
                .distinct()
                .sortedDescending()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("2026-2027", "2027-2028"))

    val availableGrades = listOf(
        "KG", "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8", "G9", "G10", "G11", "G12"
    )

    // Flow of assessments matching selected academic year and grade
    val availableAssessments: StateFlow<List<AssessmentEntity>> = combine(
        assessmentDao.getAllAssessments(),
        selectedAcademicYear,
        selectedGrade
    ) { allAsm, year, grade ->
        allAsm.filter { asm ->
            val matchYear = asm.academicYear.isBlank() ||
                    year.isBlank() ||
                    asm.academicYear.equals(year, ignoreCase = true) ||
                    (year == "2026-2027" && asm.academicYear == "2025-2026")
            val matchGrade = isGradeMatch(asm.grade, grade)
            matchYear && matchGrade
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterParams: Flow<FilterParams> = combine(
        selectedAcademicYear,
        selectedGrade,
        selectedAssessmentId,
        selectedSubject
    ) { year, grade, asmId, subject ->
        FilterParams(year, grade, asmId, subject)
    }

    val rankingData: StateFlow<RankingResultData?> = combine(
        filterParams,
        availableAssessments,
        studentDao.getAllStudents(),
        academicYearDao.getAllStudentAcademicHistories(),
        marksDao.getAllMarks()
    ) { params, assessments, students, histories, allMarks ->
        val year = params.year
        val grade = params.grade
        val asmId = params.asmId
        val filterSub = params.subject

        val currentAsm = if (asmId != null) {
            assessments.find { it.id == asmId } ?: assessments.firstOrNull()
        } else {
            // Pick first assessment that has marks for this grade, or fallback to first
            assessments.firstOrNull { asm -> allMarks.any { it.assessmentId == asm.id && it.obtainedMarks != null } }
                ?: assessments.firstOrNull()
        }

        if (currentAsm == null) {
            return@combine RankingResultData(
                academicYear = year,
                grade = grade,
                assessment = null,
                subjects = emptyList(),
                rankingItems = emptyList()
            )
        }

        val historiesByStudent = histories.groupBy { it.studentId }
        val targetGradeKey = normalizeGradeKey(grade)

        val yearStudents = students.map { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(year, ignoreCase = true) }
                ?: studentHistories.firstOrNull { it.academicYear.equals("2025-2026", ignoreCase = true) }
                ?: studentHistories.maxByOrNull { it.academicYear }

            if (yearHistory != null) {
                student.copy(
                    gradeName = yearHistory.gradeName.ifBlank { student.gradeName },
                    className = yearHistory.className.ifBlank { student.className },
                    rollNumber = if (yearHistory.rollNumber > 0) yearHistory.rollNumber else student.rollNumber,
                    status = yearHistory.status.ifBlank { student.status }
                )
            } else {
                student
            }
        }

        val gradeStudents = yearStudents.filter { std ->
            normalizeGradeKey(std.gradeName) == targetGradeKey
        }.distinctBy { it.id }.sortedBy { it.rollNumber }

        val asmMarks = allMarks.filter { it.assessmentId == currentAsm.id }

        // Collect all distinct subject names for this assessment
        val subjectsForAsm = asmMarks.map { it.subjectName }.distinct()
        val sortedSubjects = sortSubjectsLogically(subjectsForAsm)

        val isHighSchool = targetGradeKey in listOf("G10", "G11", "G12")

        val displaySubjects = if (filterSub != "ALL" && filterSub.isNotBlank()) {
            sortedSubjects.filter { it.equals(filterSub, ignoreCase = true) }
        } else {
            sortedSubjects
        }

        val items = gradeStudents.map { student ->
            val studentMarks = asmMarks.filter { it.studentId == student.id }
            val marksMap = studentMarks.associate { it.subjectName to it.obtainedMarks }

            val isSteams2 = isHighSchool && (
                student.stream.trim().uppercase().contains("2") ||
                student.stream.trim().uppercase().contains("ECON")
            )

            val displayName = if (isHighSchool && isSteams2) {
                "${student.name} (Eco)"
            } else {
                student.name
            }

            var totalObtained = 0.0
            var totalMax = 0
            var distinctionCount = 0
            var isPassed = true
            var hasAnyMark = false

            // Subjects relevant to student
            val relevantSubjects = (if (filterSub != "ALL" && filterSub.isNotBlank()) displaySubjects else sortedSubjects).filter { sub ->
                if (isHighSchool) {
                    if (isSteams2 && sub.equals("Biology", ignoreCase = true)) false
                    else if (!isSteams2 && sub.equals("Economics", ignoreCase = true)) false
                    else true
                } else true
            }

            for (sub in relevantSubjects) {
                val markEntity = studentMarks.find { it.subjectName.equals(sub, ignoreCase = true) }
                if (markEntity != null && markEntity.obtainedMarks != null) {
                    hasAnyMark = true
                    val score = markEntity.obtainedMarks
                    totalObtained += score
                    totalMax += markEntity.maxMarks

                    val passM = markEntity.passMark
                    val distM = markEntity.distinctionMark

                    if (score < passM) {
                        isPassed = false
                    }
                    if (score >= distM) {
                        distinctionCount++
                    }
                } else {
                    isPassed = false
                }
            }

            if (!hasAnyMark) {
                isPassed = false
            }

            StudentRankingItem(
                rank = 0,
                studentId = student.id,
                studentCode = student.studentCode,
                studentName = student.name,
                displayName = displayName,
                rollNo = student.rollNumber,
                stream = student.stream,
                subjectMarks = marksMap,
                totalObtained = totalObtained,
                totalMax = totalMax,
                distinctionCount = distinctionCount,
                distinctionText = "${distinctionCount}D",
                isPassed = isPassed,
                resultStatus = if (!hasAnyMark) "Pending" else if (isPassed) "Pass" else "Fail"
            )
        }

        // Sort by Total Obtained Marks descending
        val sortedItems = items.sortedWith(
            compareByDescending<StudentRankingItem> { it.totalObtained }
                .thenByDescending { it.distinctionCount }
                .thenBy { it.rollNo }
        )

        // Assign standard competition ranking (1, 2, 2, 4...)
        val rankedItems = mutableListOf<StudentRankingItem>()
        var currentRank = 1
        for (i in sortedItems.indices) {
            if (i > 0 && sortedItems[i].totalObtained < sortedItems[i - 1].totalObtained) {
                currentRank = i + 1
            }
            rankedItems.add(sortedItems[i].copy(rank = currentRank))
        }

        RankingResultData(
            academicYear = year,
            grade = grade,
            assessment = currentAsm,
            subjects = displaySubjects.ifEmpty { sortedSubjects },
            rankingItems = rankedItems
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableSubjects: StateFlow<List<String>> = combine(
        rankingData,
        availableAssessments,
        marksDao.getAllMarks()
    ) { currentRanking, assessments, allMarks ->
        val asm = currentRanking?.assessment
        if (asm != null) {
            val asmMarks = allMarks.filter { it.assessmentId == asm.id }
            val subs = sortSubjectsLogically(asmMarks.map { it.subjectName }.distinct())
            (listOf("ALL") + subs).distinct()
        } else {
            val asmIds = assessments.map { it.id }
            val subs = sortSubjectsLogically(allMarks.filter { it.assessmentId in asmIds }.map { it.subjectName }.distinct())
            val defaults = listOf("Myanmar", "English", "Mathematics", "Science", "Physics", "Chemistry", "Biology", "Economics")
            (listOf("ALL") + subs + defaults).distinct()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("ALL", "Myanmar", "English", "Mathematics", "Science", "Physics", "Chemistry", "Biology", "Economics"))

    fun setAcademicYear(year: String) {
        selectedAcademicYear.value = year
        selectedAssessmentId.value = null
    }

    fun setGrade(grade: String) {
        selectedGrade.value = grade
        selectedAssessmentId.value = null
    }

    fun setAssessmentId(id: Long) {
        selectedAssessmentId.value = id
    }

    fun setSubject(subject: String) {
        selectedSubject.value = subject
    }

    private fun sortSubjectsLogically(subjects: List<String>): List<String> {
        val priorityMap = mapOf(
            "Myanmar" to 1,
            "English" to 2,
            "English (Language)" to 3,
            "English (Phonics)" to 4,
            "Mathematics" to 5,
            "Science" to 6,
            "Physics" to 7,
            "Chemistry" to 8,
            "Biology" to 9,
            "Economics" to 10,
            "Geography" to 11,
            "History" to 12,
            "Social Studies" to 13,
            "English (International)" to 14
        )
        return subjects.sortedWith(compareBy({ priorityMap[it] ?: 99 }, { it }))
    }

    companion object {
        fun normalizeGradeKey(grade: String): String {
            val clean = grade.trim().uppercase()
                .replace("GRADE", "")
                .replace("CLASS", "")
                .replace("YEAR", "")
                .replace("PRIMARY", "P")
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
            return when {
                clean == "KG" || clean == "KINDERGARTEN" || clean == "PRESCHOOL" -> "KG"
                clean.startsWith("G") -> {
                    val n = clean.removePrefix("G").toIntOrNull()
                    if (n != null) "G$n" else clean
                }
                clean.startsWith("P") -> {
                    val n = clean.removePrefix("P").toIntOrNull()
                    if (n != null) "G$n" else clean
                }
                else -> {
                    val n = clean.toIntOrNull()
                    if (n != null) "G$n" else clean
                }
            }
        }

        fun isGradeMatch(assessmentGrade: String, targetGrade: String): Boolean {
            val asmNorm = normalizeGradeKey(assessmentGrade)
            val targetNorm = normalizeGradeKey(targetGrade)
            if (asmNorm == targetNorm) return true
            val rawAsm = assessmentGrade.trim().uppercase()
            if (rawAsm == "ALL" || rawAsm == "ALL GRADES" || rawAsm == "PRIMARY") {
                val isPrimary = targetNorm in listOf("KG", "G1", "G2", "G3", "G4", "G5")
                if (isPrimary) return true
            }
            return false
        }
    }
}

class ExamRankingViewModelFactory(
    private val assessmentDao: AssessmentDao,
    private val marksDao: MarksDao,
    private val studentDao: StudentDao,
    private val academicYearDao: AcademicYearDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamRankingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamRankingViewModel(
                assessmentDao, marksDao, studentDao, academicYearDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
