package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AssessmentStatus
import com.example.data.local.entity.EducationLevel
import com.example.data.local.entity.SubjectCategory
import com.example.data.policy.SchoolPolicy
import com.example.data.repository.AssessmentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AssessmentSortOption(val displayName: String) {
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    NAME_ASC("Name (A-Z)"),
    GRADE_ASC("Grade Level")
}

class AssessmentViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val schoolPolicyRepository: com.example.data.repository.SchoolPolicyRepository? = null
) : ViewModel() {

    val allAssessments: StateFlow<List<AssessmentEntity>> = assessmentRepository.allAssessments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedAcademicYearFilter = MutableStateFlow("ALL")
    val selectedGradeFilter = MutableStateFlow("ALL")
    val selectedSubjectFilter = MutableStateFlow("ALL")
    val selectedStatusFilter = MutableStateFlow("ALL")
    val selectedTypeFilter = MutableStateFlow("ALL")
    val sortBy = MutableStateFlow(AssessmentSortOption.DATE_DESC)

    private val extraFilterState = combine(
        selectedAcademicYearFilter,
        selectedSubjectFilter,
        selectedTypeFilter,
        sortBy
    ) { year, subject, type, sort ->
        Triple(Pair(year, subject), type, sort)
    }

    val filteredAssessments: StateFlow<List<AssessmentEntity>> = combine(
        allAssessments,
        searchQuery,
        selectedGradeFilter,
        selectedStatusFilter,
        extraFilterState
    ) { list, query, gradeFilter, statusFilter, (yearSubjectPair, typeFilter, sort) ->
        val (yearFilter, subjectFilter) = yearSubjectPair
        list.filter { assessment ->
            val matchesSearch = query.isBlank() ||
                    assessment.assessmentName.contains(query, ignoreCase = true) ||
                    assessment.subjectName.contains(query, ignoreCase = true) ||
                    assessment.description.contains(query, ignoreCase = true)

            val matchesYear = yearFilter == "ALL" || assessment.academicYear.equals(yearFilter, ignoreCase = true)

            val matchesGrade = gradeFilter == "ALL" || assessment.grade.equals(gradeFilter, ignoreCase = true)

            val matchesSubject = subjectFilter == "ALL" ||
                    assessment.subjectName.equals(subjectFilter, ignoreCase = true) ||
                    (assessment.subjectName == "All Subjects" && subjectFilter != "ALL")

            val matchesStatus = statusFilter == "ALL" || assessment.status.name.equals(statusFilter, ignoreCase = true)

            val matchesType = typeFilter == "ALL" || assessment.assessmentType.equals(typeFilter, ignoreCase = true)

            matchesSearch && matchesYear && matchesGrade && matchesSubject && matchesStatus && matchesType
        }.sortedWith { a1, a2 ->
            when (sort) {
                AssessmentSortOption.DATE_DESC -> a2.assessmentDate.compareTo(a1.assessmentDate)
                AssessmentSortOption.DATE_ASC -> a1.assessmentDate.compareTo(a2.assessmentDate)
                AssessmentSortOption.NAME_ASC -> a1.assessmentName.compareTo(a2.assessmentName, ignoreCase = true)
                AssessmentSortOption.GRADE_ASC -> a1.grade.compareTo(a2.grade, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered list strictly by Academic Year for Dashboard Statistics
    val currentYearAssessments: StateFlow<List<AssessmentEntity>> = combine(
        allAssessments,
        selectedAcademicYearFilter
    ) { list, yearFilter ->
        if (yearFilter == "ALL") {
            list
        } else {
            list.filter { it.academicYear.equals(yearFilter, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Statistics scoped to current academic year
    val totalAssessmentsCount: StateFlow<Int> = currentYearAssessments.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upcomingAssessmentsCount: StateFlow<Int> = currentYearAssessments.map { list ->
        list.count { it.status == AssessmentStatus.PUBLISHED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedAssessmentsCount: StateFlow<Int> = currentYearAssessments.map { list ->
        list.count { it.status == AssessmentStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val draftAssessmentsCount: StateFlow<Int> = currentYearAssessments.map { list ->
        list.count { it.status == AssessmentStatus.DRAFT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentAssessments: StateFlow<List<AssessmentEntity>> = currentYearAssessments.map { list ->
        list.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun setAcademicYearFilter(year: String) {
        selectedAcademicYearFilter.value = year
    }

    fun setGradeFilter(grade: String) {
        selectedGradeFilter.value = grade
    }

    fun setSubjectFilter(subject: String) {
        selectedSubjectFilter.value = subject
    }

    fun setStatusFilter(status: String) {
        selectedStatusFilter.value = status
    }

    fun setTypeFilter(type: String) {
        selectedTypeFilter.value = type
    }

    fun setSortBy(sort: AssessmentSortOption) {
        sortBy.value = sort
    }

    fun saveAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            assessmentRepository.saveAssessment(assessment)
        }
    }

    fun deleteAssessment(id: Long) {
        viewModelScope.launch {
            assessmentRepository.deleteAssessment(id)
        }
    }

    fun archiveAssessment(id: Long) {
        viewModelScope.launch {
            assessmentRepository.updateAssessmentStatus(id, AssessmentStatus.ARCHIVED)
        }
    }

    fun updateStatus(id: Long, status: AssessmentStatus) {
        viewModelScope.launch {
            assessmentRepository.updateAssessmentStatus(id, status)
        }
    }

    fun duplicateAssessment(assessment: AssessmentEntity, currentUserName: String) {
        viewModelScope.launch {
            assessmentRepository.duplicateAssessment(assessment, currentUserName)
        }
    }

    fun saveSchedule(schedule: com.example.data.local.entity.AssessmentScheduleEntity) {
        viewModelScope.launch {
            assessmentRepository.saveSchedule(schedule)
        }
    }

    fun getSchedule(assessmentId: Long): Flow<com.example.data.local.entity.AssessmentScheduleEntity?> {
        return assessmentRepository.getSchedule(assessmentId)
    }

    // Helper functions for Assessment Types according to Grade / Level
    fun getAssessmentTypesForGrade(grade: String): List<String> {
        val level = getEducationLevelForGrade(grade)
        return when (level) {
            EducationLevel.KINDERGARTEN -> listOf(
                "Monthly Test",
                "Custom Exam"
            )
            else -> listOf(
                "Monthly Test",
                "Weekly Test",
                "Pilot Test",
                "CET",
                "Lesson Completion Test",
                "Custom Exam"
            )
        }
    }

    fun getEducationLevelForGrade(grade: String): EducationLevel {
        return SchoolPolicy.getEducationLevel(grade)
    }

    private val dbSubjectsState = schoolPolicyRepository?.allSubjects?.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    // Helper function for auto subjects based on grade and stream
    fun getSubjectsForGrade(grade: String, stream: String = ""): List<Pair<String, String>> {
        val subjects = mutableListOf<Pair<String, String>>()
        val dbSubjects = dbSubjectsState?.value ?: emptyList()
        val subjectsForGrade = SchoolPolicy.filterSubjectsForGrade(dbSubjects, grade, stream)

        subjectsForGrade.forEach { subj ->
            val type = if (subj.category == SubjectCategory.ACADEMIC) "Academic Subject" else "Additional Subject"
            subjects.add(Pair(subj.name, type))
        }

        return subjects.distinct()
    }
}

class AssessmentViewModelFactory(
    private val repository: AssessmentRepository,
    private val schoolPolicyRepository: com.example.data.repository.SchoolPolicyRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssessmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssessmentViewModel(repository, schoolPolicyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
