package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.PdfExportHistoryEntity
import com.example.data.local.entity.PrintHistoryEntity
import com.example.data.local.entity.ReportGenerationHistoryEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.report.GeneratedReportCardData
import com.example.data.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchivedReportRecord(
    val studentId: Long,
    val selectedMonth: String,
    val academicYear: String,
    val reportData: GeneratedReportCardData,
    val generatedAtTimestamp: Long = System.currentTimeMillis()
)

class ReportViewModel(private val repository: ReportRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                repository.ensureG4Student()
            } catch (_: Exception) {}
        }
    }

    val selectedGradeFilter = MutableStateFlow("All")
    val selectedMonth = MutableStateFlow("July")
    val academicYear = MutableStateFlow("2026-2027")

    val students: StateFlow<List<StudentEntity>> = combine(
        repository.allStudents,
        repository.allAcademicHistories,
        academicYear
    ) { all, histories, currentYear ->
        val historiesByStudent = histories.groupBy { it.studentId }

        all.mapNotNull { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(currentYear, ignoreCase = true) }

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
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val grades: StateFlow<List<GradeEntity>> = repository.allGrades.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val generationHistory: StateFlow<List<ReportGenerationHistoryEntity>> = repository.generationHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedStudent = MutableStateFlow<StudentEntity?>(null)

    private val _visibilityTrigger = MutableStateFlow(0)
    val visibilityTrigger: StateFlow<Int> = _visibilityTrigger.asStateFlow()

    fun refreshVisibility() {
        _visibilityTrigger.value += 1
    }

    // Map of archived report records keyed by "${studentId}_${month}_${academicYear}"
    private val _archivedReportsMap = MutableStateFlow<Map<String, ArchivedReportRecord>>(emptyMap())
    val archivedReportsMap: StateFlow<Map<String, ArchivedReportRecord>> = _archivedReportsMap.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveSourceData: StateFlow<GeneratedReportCardData?> = combine(
        selectedStudent,
        selectedMonth,
        academicYear
    ) { student, month, year ->
        Triple(student, month, year)
    }.flatMapLatest { (student, month, year) ->
        if (student == null) {
            flowOf(null)
        } else {
            repository.generateReportCard(
                studentId = student.id,
                selectedMonth = month,
                academicYear = year
            )
        }
    }.catch { e ->
        e.printStackTrace()
        emit(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Archived / Saved Report Data for current selection
    val archivedReportData: StateFlow<GeneratedReportCardData?> = combine(
        selectedStudent,
        selectedMonth,
        academicYear,
        _archivedReportsMap
    ) { student, month, year, map ->
        if (student == null) null
        else {
            val key = "${student.id}_${month}_${year}"
            map[key]?.reportData
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Detect if live source data differs from the archived snapshot
    val hasSourceDataChanged: StateFlow<Boolean> = combine(
        archivedReportData,
        liveSourceData
    ) { archived, live ->
        if (archived == null || live == null) false
        else {
            // Compare archived snapshot with live source data
            archived != live
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun selectStudent(student: StudentEntity?) {
        selectedStudent.value = student
    }

    fun setSelectedGradeFilter(grade: String) {
        selectedGradeFilter.value = grade
    }

    fun setSelectedMonth(month: String) {
        selectedMonth.value = month
    }

    fun setAcademicYear(year: String) {
        academicYear.value = year
    }

    /**
     * Explicitly generates the report card for the selected student & period.
     * Takes the current live source data snapshot and stores it as the archived report.
     */
    fun generateReport(dataToSave: GeneratedReportCardData? = null) {
        val student = selectedStudent.value ?: return
        val month = selectedMonth.value
        val year = academicYear.value
        val data = dataToSave ?: liveSourceData.value ?: return

        val key = "${student.id}_${month}_${year}"
        val record = ArchivedReportRecord(
            studentId = student.id,
            selectedMonth = month,
            academicYear = year,
            reportData = data,
            generatedAtTimestamp = System.currentTimeMillis()
        )

        _archivedReportsMap.value = _archivedReportsMap.value + (key to record)
        logReportGeneration(data, isRegenerate = false)
    }

    /**
     * Regenerates the report card upon teacher confirmation when source data changes.
     * Overwrites the archived report snapshot with the latest live source data.
     */
    fun regenerateReport(dataToSave: GeneratedReportCardData? = null) {
        val student = selectedStudent.value ?: return
        val month = selectedMonth.value
        val year = academicYear.value
        val data = dataToSave ?: liveSourceData.value ?: return

        val key = "${student.id}_${month}_${year}"
        val record = ArchivedReportRecord(
            studentId = student.id,
            selectedMonth = month,
            academicYear = year,
            reportData = data,
            generatedAtTimestamp = System.currentTimeMillis()
        )

        _archivedReportsMap.value = _archivedReportsMap.value + (key to record)
        logReportGeneration(data, isRegenerate = true)
    }

    private fun logReportGeneration(data: GeneratedReportCardData, isRegenerate: Boolean) {
        viewModelScope.launch {
            val log = ReportGenerationHistoryEntity(
                studentId = data.student.id,
                studentName = data.student.name,
                studentCode = data.student.studentCode,
                gradeName = data.gradeName,
                className = data.className,
                templateType = data.ruleResult.selectedTemplate.name,
                reportType = if (isRegenerate) "REGENERATION" else "INITIAL_GENERATION",
                assessmentPeriodName = data.selectedMonth,
                academicYear = data.academicYear,
                generatedBy = "Class Teacher",
                generatedAtTimestamp = System.currentTimeMillis(),
                status = "SUCCESS"
            )
            repository.saveGenerationLog(log)
        }
    }

    fun logPdfExport(data: GeneratedReportCardData) {
        viewModelScope.launch {
            val exportLog = PdfExportHistoryEntity(
                studentId = data.student.id,
                studentName = data.student.name,
                gradeName = data.gradeName,
                className = data.className,
                academicYear = data.academicYear,
                reportType = data.ruleResult.selectedTemplate.displayName,
                assessmentPeriodName = data.selectedMonth,
                fileName = "ReportCard_${data.student.studentCode}_${data.selectedMonth}_${data.academicYear}.pdf",
                fileSizeBytes = 245000L,
                exportedBy = "Class Teacher",
                exportedAtTimestamp = System.currentTimeMillis()
            )
            repository.saveGenerationLog(
                ReportGenerationHistoryEntity(
                    studentId = data.student.id,
                    studentName = data.student.name,
                    studentCode = data.student.studentCode,
                    gradeName = data.gradeName,
                    className = data.className,
                    templateType = data.ruleResult.selectedTemplate.name,
                    reportType = "PDF_EXPORT",
                    assessmentPeriodName = data.selectedMonth,
                    academicYear = data.academicYear,
                    generatedBy = "Class Teacher",
                    generatedAtTimestamp = System.currentTimeMillis(),
                    status = "SUCCESS"
                )
            )
        }
    }

    // AI Comment State for Teacher Review
    private val _isGeneratingAiComment = MutableStateFlow(false)
    val isGeneratingAiComment: StateFlow<Boolean> = _isGeneratingAiComment.asStateFlow()

    private val _aiReportCommentResult = MutableStateFlow<Pair<com.example.data.ai.StudentReportFacts, com.example.data.ai.ReportCardAiResult>?>(null)
    val aiReportCommentResult: StateFlow<Pair<com.example.data.ai.StudentReportFacts, com.example.data.ai.ReportCardAiResult>?> = _aiReportCommentResult.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    private val reportCardAiEngine = com.example.data.ai.ReportCardAiEngine()

    fun dismissAiReviewDialog() {
        _aiReportCommentResult.value = null
    }

    fun clearAiErrorMessage() {
        _aiErrorMessage.value = null
    }

    /**
     * Generates a grounded AI comment draft for the currently selected student, period, and academic year.
     */
    fun generateAiCommentForSelectedStudent(language: String = "MYANMAR") {
        val student = selectedStudent.value ?: return
        val period = selectedMonth.value
        val year = academicYear.value

        viewModelScope.launch {
            _isGeneratingAiComment.value = true
            _aiErrorMessage.value = null
            try {
                val aggregator = repository.getStudentFactsAggregator()
                val facts = aggregator.aggregateFacts(
                    studentId = student.id,
                    periodName = period,
                    academicYear = year
                )

                if (facts == null) {
                    _aiErrorMessage.value = "Unable to retrieve academic or holistic facts for ${student.name}."
                    _isGeneratingAiComment.value = false
                    return@launch
                }

                val aiResult = reportCardAiEngine.generateReportComment(
                    facts = facts,
                    preferredLanguage = language
                )

                _aiReportCommentResult.value = Pair(facts, aiResult)
            } catch (e: Exception) {
                e.printStackTrace()
                _aiErrorMessage.value = "AI comment generation error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isGeneratingAiComment.value = false
            }
        }
    }

    /**
     * Teacher explicit confirmation: Saves the teacher comment & parent suggestion to the database
     * and immediately updates the report card.
     */
    fun applyAiCommentToReportCard(
        teacherComment: String,
        parentSuggestion: String
    ) {
        val student = selectedStudent.value ?: return
        val period = selectedMonth.value
        val year = academicYear.value

        viewModelScope.launch {
            try {
                val existingComment = repository.getExistingTeacherComment(student.id, period, year)
                val commentEntity = existingComment?.copy(
                    generalComment = teacherComment,
                    futureRecommendation = parentSuggestion,
                    updatedBy = "Teacher (AI Assisted)",
                    updatedAt = System.currentTimeMillis(),
                    isDirty = true
                ) ?: com.example.data.local.entity.TeacherCommentEntity(
                    studentId = student.id,
                    assessmentPeriod = period,
                    academicYear = year,
                    grade = student.gradeName,
                    className = student.className,
                    positiveComments = "",
                    areasForImprovement = "",
                    generalComment = teacherComment,
                    futureRecommendation = parentSuggestion,
                    updatedBy = "Teacher (AI Assisted)",
                    updatedAt = System.currentTimeMillis(),
                    uuid = java.util.UUID.randomUUID().toString(),
                    isDirty = true
                )

                repository.saveTeacherComment(commentEntity)
                _aiReportCommentResult.value = null
                refreshVisibility()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logPrint(data: GeneratedReportCardData) {
        viewModelScope.launch {
            repository.saveGenerationLog(
                ReportGenerationHistoryEntity(
                    studentId = data.student.id,
                    studentName = data.student.name,
                    studentCode = data.student.studentCode,
                    gradeName = data.gradeName,
                    className = data.className,
                    templateType = data.ruleResult.selectedTemplate.name,
                    reportType = "PRINT_JOB",
                    assessmentPeriodName = data.selectedMonth,
                    academicYear = data.academicYear,
                    generatedBy = "Class Teacher",
                    generatedAtTimestamp = System.currentTimeMillis(),
                    status = "SUCCESS"
                )
            )
        }
    }
}

class ReportViewModelFactory(private val repository: ReportRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            return ReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
