package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.AssessmentRepository
import com.example.data.repository.MarksRepository
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.data.policy.SchoolPolicy

enum class MarkFilterStatus(val displayName: String) {
    ALL("All Students"),
    PASSED("Passed"),
    FAILED("Failed"),
    DISTINCTION("Distinction"),
    PENDING("Pending / Unmarked")
}

enum class MarkSortOption(val displayName: String) {
    ROLL_ASC("Roll No (Ascending)"),
    ROLL_DESC("Roll No (Descending)"),
    NAME_ASC("Student Name (A-Z)"),
    MARKS_DESC("Marks (Highest First)"),
    MARKS_ASC("Marks (Lowest First)")
}

data class EditableMarkRow(
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val rollNo: Int,
    val maxMarks: Int,
    val obtainedText: String, // String for live input editing
    val obtainedMarks: Double?,
    val passMark: Int,
    val distinctionMark: Int,
    val isPassed: Boolean,
    val isDistinction: Boolean,
    val remarks: String,
    val errorMessage: String? = null
)

class MarksViewModel(
    private val marksRepository: MarksRepository,
    private val studentRepository: StudentRepository,
    private val assessmentRepository: AssessmentRepository,
    private val academicYearRepository: com.example.data.repository.AcademicYearRepository? = null
) : ViewModel() {

    val academicYear = MutableStateFlow("2026-2027")
    val selectedGrade = MutableStateFlow("G5")
    val selectedStream = MutableStateFlow("STEAMS-1")
    val selectedClass = MutableStateFlow("All Classes")
    val selectedAssessment = MutableStateFlow<AssessmentEntity?>(null)
    val selectedSubject = MutableStateFlow("Mathematics")

    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow(MarkFilterStatus.ALL)
    val sortOption = MutableStateFlow(MarkSortOption.ROLL_ASC)

    private val academicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearRepository?.getAllStudentAcademicHistories()
        ?: flowOf(emptyList())

    // Dynamic subjects based on School Policy for selected grade and stream
    val availableSubjectsForGrade: StateFlow<List<String>> = combine(
        marksRepository.getAllSubjects(),
        selectedGrade,
        selectedStream
    ) { subjects, grade, stream ->
        SchoolPolicy.getSubjectNamesForGrade(subjects, grade, stream)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available assessments for selected grade and academic year
    val availableAssessments: StateFlow<List<AssessmentEntity>> = combine(
        selectedGrade,
        academicYear
    ) { grade, year ->
        Pair(grade, year)
    }.flatMapLatest { (grade, year) ->
        assessmentRepository.getAssessmentsByGrade(grade, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class MarkStudentFilter(
        val grade: String,
        val className: String,
        val stream: String,
        val year: String
    )

    private val markStudentFilterFlow = combine(
        selectedGrade,
        selectedClass,
        selectedStream,
        academicYear
    ) { grade, className, stream, year ->
        MarkStudentFilter(grade, className, stream, year)
    }

    // All students for selected grade/class/stream and academic year
    val classStudents: StateFlow<List<StudentEntity>> = combine(
        studentRepository.allStudents,
        academicHistories,
        markStudentFilterFlow
    ) { students, histories, filter ->
        val historiesByStudent = histories.groupBy { it.studentId }

        students.mapNotNull { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(filter.year, ignoreCase = true) }

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
                if (filter.year.equals(activeCode, ignoreCase = true)) {
                    student
                } else {
                    null
                }
            }
        }.filter { student ->
            val matchesGrade = student.gradeName.equals(filter.grade, ignoreCase = true)
            val matchesClass = filter.className == "All Classes" || filter.className == "All" || student.className.equals(filter.className, ignoreCase = true)
            val matchesStream = if (SchoolPolicy.isHighSchool(filter.grade)) {
                val stStream = student.stream.trim().uppercase()
                val selStream = filter.stream.trim().uppercase()
                if (stStream.isBlank()) {
                    true
                } else if (selStream.contains("1")) {
                    stStream.contains("1") || stStream.contains("BIO")
                } else if (selStream.contains("2")) {
                    stStream.contains("2") || stStream.contains("ECON")
                } else {
                    stStream == selStream
                }
            } else true

            matchesGrade && matchesClass && matchesStream
        }.sortedBy { it.rollNumber }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently loaded marks from DB
    private val dbMarks: StateFlow<List<StudentMarkEntity>> = combine(
        selectedAssessment,
        selectedSubject
    ) { assessment, subject ->
        if (assessment != null && subject.isNotBlank()) {
            marksRepository.getMarksForAssessmentAndSubject(assessment.id, subject)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Assessment Lock Status
    val lockStatus: StateFlow<AssessmentLockStatusEntity?> = selectedAssessment
        .flatMapLatest { assessment ->
            if (assessment != null) {
                marksRepository.getLockStatus(assessment.id)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLocked: StateFlow<Boolean> = lockStatus.map { it?.isLocked == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // In-memory editable rows state
    val editableMarkRows = MutableStateFlow<List<EditableMarkRow>>(emptyList())

    init {
        viewModelScope.launch {
            availableSubjectsForGrade.collect { subjects ->
                if (subjects.isNotEmpty() && !subjects.contains(selectedSubject.value)) {
                    selectedSubject.value = subjects.first()
                }
            }
        }

        // Sync students and saved DB marks into editable rows
        viewModelScope.launch {
            combine(classStudents, dbMarks, selectedAssessment, selectedSubject) { students, marks, assessment, subject ->
                val level = getEducationLevel(selectedGrade.value)
                val passMark = marksRepository.getPassMark(level, subject)
                val distinctionMark = marksRepository.getDistinctionMark(level, subject)
                val maxMark = assessment?.maxMarks ?: 100

                val markMap = marks.associateBy { it.studentId }

                students.map { student ->
                    val existing = markMap[student.id]
                    val obtainedVal = existing?.obtainedMarks
                    val textVal = obtainedVal?.let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                    } ?: ""

                    EditableMarkRow(
                        studentId = student.id,
                        studentCode = student.studentCode,
                        studentName = student.name,
                        rollNo = student.rollNumber,
                        maxMarks = maxMark,
                        obtainedText = textVal,
                        obtainedMarks = obtainedVal,
                        passMark = passMark,
                        distinctionMark = distinctionMark,
                        isPassed = obtainedVal != null && obtainedVal >= passMark,
                        isDistinction = obtainedVal != null && obtainedVal >= distinctionMark,
                        remarks = existing?.remarks ?: "",
                        errorMessage = null
                    )
                }
            }.collect { rows ->
                editableMarkRows.value = rows
            }
        }
    }

    // Filtered & Sorted Display Rows
    val displayedRows: StateFlow<List<EditableMarkRow>> = combine(
        editableMarkRows,
        searchQuery,
        filterStatus,
        sortOption
    ) { rows, query, filter, sort ->
        rows.filter { row ->
            val matchesQuery = query.isBlank() ||
                    row.studentName.contains(query, ignoreCase = true) ||
                    row.studentCode.contains(query, ignoreCase = true) ||
                    row.rollNo.toString() == query

            val matchesFilter = when (filter) {
                MarkFilterStatus.ALL -> true
                MarkFilterStatus.PASSED -> row.obtainedMarks != null && row.isPassed
                MarkFilterStatus.FAILED -> row.obtainedMarks != null && !row.isPassed
                MarkFilterStatus.DISTINCTION -> row.obtainedMarks != null && row.isDistinction
                MarkFilterStatus.PENDING -> row.obtainedMarks == null
            }

            matchesQuery && matchesFilter
        }.sortedWith { r1, r2 ->
            when (sort) {
                MarkSortOption.ROLL_ASC -> r1.rollNo.compareTo(r2.rollNo)
                MarkSortOption.ROLL_DESC -> r2.rollNo.compareTo(r1.rollNo)
                MarkSortOption.NAME_ASC -> r1.studentName.compareTo(r2.studentName, ignoreCase = true)
                MarkSortOption.MARKS_DESC -> (r2.obtainedMarks ?: -1.0).compareTo(r1.obtainedMarks ?: -1.0)
                MarkSortOption.MARKS_ASC -> (r1.obtainedMarks ?: -1.0).compareTo(r2.obtainedMarks ?: -1.0)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update cell value with real-time validation
    fun updateObtainedMark(studentId: Long, input: String) {
        if (isLocked.value) return

        val currentList = editableMarkRows.value.toMutableList()
        val index = currentList.indexOfFirst { it.studentId == studentId }
        if (index != -1) {
            val target = currentList[index]
            val max = target.maxMarks

            var errorMsg: String? = null
            val numVal = input.toDoubleOrNull()

            if (input.isNotBlank() && numVal == null) {
                errorMsg = "Invalid Number"
            } else if (numVal != null) {
                if (numVal < 0) {
                    errorMsg = "Marks cannot be negative"
                } else if (numVal > max) {
                    errorMsg = "Marks cannot exceed $max"
                }
            }

            val validObtained = if (errorMsg == null) numVal else null
            val passed = validObtained != null && validObtained >= target.passMark
            val distinction = validObtained != null && validObtained >= target.distinctionMark

            currentList[index] = target.copy(
                obtainedText = input,
                obtainedMarks = validObtained,
                isPassed = passed,
                isDistinction = distinction,
                errorMessage = errorMsg
            )
            editableMarkRows.value = currentList
        }
    }

    fun updateRemarks(studentId: Long, remarks: String) {
        if (isLocked.value) return
        val currentList = editableMarkRows.value.toMutableList()
        val index = currentList.indexOfFirst { it.studentId == studentId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(remarks = remarks)
            editableMarkRows.value = currentList
        }
    }

    // Save Draft / Quick Save to database
    fun saveAllMarks(currentUserName: String, onComplete: (Boolean, String) -> Unit) {
        val assessment = selectedAssessment.value
        if (assessment == null) {
            onComplete(false, "Please select an assessment first.")
            return
        }

        val rows = editableMarkRows.value
        val hasErrors = rows.any { it.errorMessage != null }
        if (hasErrors) {
            onComplete(false, "Please fix validation errors before saving.")
            return
        }

        viewModelScope.launch {
            val markEntities = rows.map { row ->
                StudentMarkEntity(
                    assessmentId = assessment.id,
                    studentId = row.studentId,
                    studentCode = row.studentCode,
                    studentName = row.studentName,
                    rollNo = row.rollNo,
                    subjectName = selectedSubject.value,
                    maxMarks = row.maxMarks,
                    obtainedMarks = row.obtainedMarks,
                    passMark = row.passMark,
                    distinctionMark = row.distinctionMark,
                    isPassed = row.isPassed,
                    isDistinction = row.isDistinction,
                    remarks = row.remarks,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = currentUserName
                )
            }
            marksRepository.saveMarks(markEntities)
            onComplete(true, "Successfully saved marks for ${rows.size} students.")
        }
    }

    // Lock Assessment
    fun lockAssessment(userName: String, note: String, onComplete: () -> Unit) {
        val assessment = selectedAssessment.value ?: return
        viewModelScope.launch {
            marksRepository.lockAssessment(assessment.id, userName, note)
            // Also update assessment status to LOCKED/PUBLISHED
            assessmentRepository.updateAssessmentStatus(assessment.id, AssessmentStatus.COMPLETED)
            onComplete()
        }
    }

    // Unlock Assessment (Admin/Super Admin only)
    fun unlockAssessment(userName: String, onComplete: () -> Unit) {
        val assessment = selectedAssessment.value ?: return
        viewModelScope.launch {
            marksRepository.unlockAssessment(assessment.id, userName)
            assessmentRepository.updateAssessmentStatus(assessment.id, AssessmentStatus.PUBLISHED)
            onComplete()
        }
    }

    private fun getEducationLevel(grade: String): EducationLevel {
        return SchoolPolicy.getEducationLevel(grade)
    }
}

class MarksViewModelFactory(
    private val marksRepository: MarksRepository,
    private val studentRepository: StudentRepository,
    private val assessmentRepository: AssessmentRepository,
    private val academicYearRepository: com.example.data.repository.AcademicYearRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarksViewModel(marksRepository, studentRepository, assessmentRepository, academicYearRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
