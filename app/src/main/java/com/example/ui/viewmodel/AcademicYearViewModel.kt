package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.AcademicYearRepository
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PromotionMode {
    INDIVIDUAL,
    WHOLE_CLASS,
    WHOLE_GRADE,
    TRANSFER_CLASS,
    REPEAT_GRADE
}

class AcademicYearViewModel(
    private val academicYearRepository: AcademicYearRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    // Academic Years Flow
    val allAcademicYears: StateFlow<List<AcademicYearEntity>> = academicYearRepository
        .getAllAcademicYears()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeAcademicYear: StateFlow<AcademicYearEntity?> = academicYearRepository
        .getActiveAcademicYear()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Promotion History
    val promotionHistoryList: StateFlow<List<PromotionHistoryEntity>> = academicYearRepository
        .getAllPromotionHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All Students from Student Repository
    val allStudents: StateFlow<List<StudentEntity>> = studentRepository
        .allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filters for Student Promotion
    private val _selectedFromYear = MutableStateFlow("")
    val selectedFromYear: StateFlow<String> = _selectedFromYear.asStateFlow()

    private val _selectedToYear = MutableStateFlow("")
    val selectedToYear: StateFlow<String> = _selectedToYear.asStateFlow()

    init {
        viewModelScope.launch {
            activeAcademicYear.collect { active ->
                if (active != null && _selectedFromYear.value.isBlank()) {
                    _selectedFromYear.value = active.yearCode
                }
            }
        }
        viewModelScope.launch {
            allAcademicYears.collect { years ->
                if (_selectedToYear.value.isBlank() && years.isNotEmpty()) {
                    val upcoming = years.firstOrNull { it.status == AcademicYearStatus.UPCOMING }?.yearCode
                    if (upcoming != null) {
                        _selectedToYear.value = upcoming
                    } else {
                        val activeCode = activeAcademicYear.value?.yearCode ?: years.firstOrNull { it.isCurrentActive }?.yearCode
                        val otherYear = years.firstOrNull { it.yearCode != activeCode }?.yearCode
                        _selectedToYear.value = otherYear ?: years.first().yearCode
                    }
                }
            }
        }
    }

    private val _filterGrade = MutableStateFlow("G1")
    val filterGrade: StateFlow<String> = _filterGrade.asStateFlow()

    private val _filterClass = MutableStateFlow("A")
    val filterClass: StateFlow<String> = _filterClass.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Filtered Students list
    val filteredStudents: StateFlow<List<StudentEntity>> = combine(
        allStudents,
        _filterGrade,
        _filterClass,
        _searchQuery
    ) { students, gr, cl, query ->
        students.filter { s ->
            val matchGrade = gr.isBlank() || s.gradeName.equals(gr, ignoreCase = true)
            val matchClass = cl.isBlank() || s.className.equals(cl, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    s.name.contains(query, ignoreCase = true) ||
                    s.studentCode.contains(query, ignoreCase = true)

            matchGrade && matchClass && matchQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFromYear(year: String) { _selectedFromYear.value = year }
    fun setToYear(year: String) { _selectedToYear.value = year }
    fun setFilterGrade(grade: String) { _filterGrade.value = grade }
    fun setFilterClass(clazz: String) { _filterClass.value = clazz }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // Academic Year Actions
    fun createAcademicYear(
        yearCode: String,
        displayName: String,
        startDate: String,
        endDate: String
    ) {
        if (yearCode.isBlank() || displayName.isBlank()) {
            _statusMessage.value = "Year Code and Display Name are required."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.saveAcademicYear(
                yearCode = yearCode.trim(),
                displayName = displayName.trim(),
                startDate = startDate,
                endDate = endDate,
                status = AcademicYearStatus.UPCOMING
            )
            _isProcessing.value = false
            _statusMessage.value = "Created Academic Year '$displayName' successfully."
        }
    }

    init {
        activeAcademicYear.onEach { year ->
            year?.let {
                if (it.yearCode.isNotBlank()) {
                    _selectedFromYear.value = it.yearCode
                }
            }
        }.launchIn(viewModelScope)
    }

    fun activateAcademicYearByCode(yearCode: String) {
        if (yearCode.isBlank()) return
        android.util.Log.d("AcademicYearTrace", "VM_CALL: activateAcademicYearByCode called with '$yearCode'")
        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.activateAcademicYearByCode(yearCode)
            _isProcessing.value = false
            _statusMessage.value = "Activated '$yearCode' as the current Active Academic Year."
        }
    }

    fun activateAcademicYear(yearId: Long, yearName: String) {
        android.util.Log.d("AcademicYearTrace", "VM_CALL: activateAcademicYear called with ID=$yearId, name='$yearName'")
        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.activateAcademicYear(yearId)
            _isProcessing.value = false
            _statusMessage.value = "Activated '$yearName' as the current Active Academic Year."
        }
    }

    fun closeAcademicYear(yearId: Long, yearName: String, closedBy: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.closeAcademicYear(yearId, closedBy)
            _isProcessing.value = false
            _statusMessage.value = "Closed Academic Year '$yearName'. Promotion wizard is now ready."
        }
    }

    fun archiveAcademicYear(yearId: Long, yearName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.archiveAcademicYear(yearId)
            _isProcessing.value = false
            _statusMessage.value = "Archived Academic Year '$yearName'."
        }
    }

    // Student Promotion Actions
    fun promoteIndividual(
        student: StudentEntity,
        targetGrade: String,
        targetClass: String,
        targetRollNumber: Int,
        actionType: PromotionAction,
        promotedBy: String,
        remarks: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.promoteStudent(
                student = student,
                fromAcademicYear = _selectedFromYear.value,
                toAcademicYear = _selectedToYear.value,
                targetGrade = targetGrade,
                targetClass = targetClass,
                targetRollNumber = targetRollNumber,
                actionType = actionType,
                promotedBy = promotedBy,
                remarks = remarks
            )
            _isProcessing.value = false
            _statusMessage.value = "${student.name} was successfully ${actionType.displayName} to $targetGrade-$targetClass."
        }
    }

    fun promoteWholeClass(
        targetGrade: String,
        targetClass: String,
        promotedBy: String
    ) {
        val classStudents = filteredStudents.value
        if (classStudents.isEmpty()) {
            _statusMessage.value = "No students selected in current class filter to promote."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.promoteClass(
                students = classStudents,
                fromAcademicYear = _selectedFromYear.value,
                toAcademicYear = _selectedToYear.value,
                targetGrade = targetGrade,
                targetClass = targetClass,
                promotedBy = promotedBy
            )
            _isProcessing.value = false
            _statusMessage.value = "Successfully promoted all ${classStudents.size} students to $targetGrade-$targetClass!"
        }
    }

    fun promoteWholeGrade(
        promotedBy: String
    ) {
        val currentGr = _filterGrade.value
        val allGradeStudents = allStudents.value.filter { it.gradeName.equals(currentGr, ignoreCase = true) }
        if (allGradeStudents.isEmpty()) {
            _statusMessage.value = "No students found in Grade $currentGr."
            return
        }

        val targetGrade = academicYearRepository.getNextGrade(currentGr)

        viewModelScope.launch {
            _isProcessing.value = true
            academicYearRepository.promoteGrade(
                students = allGradeStudents,
                fromAcademicYear = _selectedFromYear.value,
                toAcademicYear = _selectedToYear.value,
                targetGrade = targetGrade,
                promotedBy = promotedBy
            )
            _isProcessing.value = false
            _statusMessage.value = "Successfully promoted entire Grade $currentGr (${allGradeStudents.size} students) to Grade $targetGrade!"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun canManage(currentUser: UserEntity?): Boolean {
        if (currentUser == null) return false
        return currentUser.role == UserRole.SUPER_ADMIN
    }

    fun getNextGrade(currentGrade: String): String {
        return academicYearRepository.getNextGrade(currentGrade)
    }
}

class AcademicYearViewModelFactory(
    private val academicYearRepository: AcademicYearRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AcademicYearViewModel::class.java)) {
            return AcademicYearViewModel(academicYearRepository, studentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
