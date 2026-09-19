package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.StudentAcademicHistoryEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.repository.AcademicYearRepository
import com.example.data.repository.StudentRepository
import com.example.data.sync.DeletionVerificationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentViewModel(
    private val repository: StudentRepository,
    private val academicYearRepository: AcademicYearRepository? = null
) : ViewModel() {

    private val _deletionVerificationEvent = MutableSharedFlow<DeletionVerificationResult>(extraBufferCapacity = 1)
    val deletionVerificationEvent: SharedFlow<DeletionVerificationResult> = _deletionVerificationEvent.asSharedFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGradeFilter = MutableStateFlow("All")
    val selectedGradeFilter: StateFlow<String> = _selectedGradeFilter.asStateFlow()

    private val _selectedClassFilter = MutableStateFlow("All")
    val selectedClassFilter: StateFlow<String> = _selectedClassFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("All")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _academicYear = MutableStateFlow("2026-2027")
    val academicYear: StateFlow<String> = _academicYear.asStateFlow()

    private val academicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearRepository?.getAllStudentAcademicHistories()
        ?: flowOf(emptyList())

    private data class StudentFilterCriteria(
        val query: String,
        val grade: String,
        val clazz: String,
        val status: String,
        val year: String
    )

    private val filterCriteriaFlow = combine(
        _searchQuery,
        _selectedGradeFilter,
        _selectedClassFilter,
        _selectedStatusFilter,
        _academicYear
    ) { query, grade, clazz, status, year ->
        StudentFilterCriteria(query, grade, clazz, status, year)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentsList: StateFlow<List<StudentEntity>> = combine(
        repository.allStudents,
        academicHistories,
        filterCriteriaFlow
    ) { all, histories, criteria ->
        // Group academic history by studentId
        val historiesByStudent = histories.groupBy { it.studentId }

        all.map { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            
            // Check if there is an explicit record for the selected year
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(criteria.year, ignoreCase = true) }

            if (yearHistory != null) {
                // Return student with projected grade, class, rollNumber and status for this specific academic year
                student.copy(
                    gradeName = yearHistory.gradeName,
                    className = yearHistory.className,
                    rollNumber = yearHistory.rollNumber,
                    status = yearHistory.status
                )
            } else {
                student
            }
        }.filter { student ->
            val matchesQuery = criteria.query.isEmpty() ||
                    student.name.contains(criteria.query, ignoreCase = true) ||
                    student.studentCode.contains(criteria.query, ignoreCase = true) ||
                    student.studentNrc.contains(criteria.query, ignoreCase = true) ||
                    student.parentName.contains(criteria.query, ignoreCase = true) ||
                    student.fatherName.contains(criteria.query, ignoreCase = true) ||
                    student.motherName.contains(criteria.query, ignoreCase = true)

            val matchesGrade = criteria.grade == "All" || student.gradeName.equals(criteria.grade, ignoreCase = true)
            val matchesClass = criteria.clazz == "All" || student.className.equals(criteria.clazz, ignoreCase = true)
            val matchesStatus = criteria.status == "All" || student.status.equals(criteria.status, ignoreCase = true)

            matchesQuery && matchesGrade && matchesClass && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setAcademicYear(year: String) {
        if (year.isNotBlank()) {
            _academicYear.value = year
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setGradeFilter(grade: String) {
        _selectedGradeFilter.value = grade
    }

    fun setClassFilter(clazz: String) {
        _selectedClassFilter.value = clazz
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun saveStudent(student: StudentEntity) {
        viewModelScope.launch {
            val studentId = repository.saveStudent(student)
            val effectiveId = if (student.id != 0L) student.id else studentId
            
            // Also record student academic enrollment history snapshot for the active year
            academicYearRepository?.let { repo ->
                val year = _academicYear.value
                repo.saveStudentAcademicHistory(
                    StudentAcademicHistoryEntity(
                        studentId = effectiveId,
                        academicYear = year,
                        gradeName = student.gradeName,
                        className = student.className,
                        rollNumber = student.rollNumber,
                        status = student.status
                    )
                )
            }
        }
    }

    fun deleteStudent(studentId: Long, onCompleted: ((DeletionVerificationResult) -> Unit)? = null) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                val result = repository.deleteStudent(studentId)
                _deletionVerificationEvent.emit(result)
                onCompleted?.invoke(result)
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun clearAllStudents(onCompleted: ((DeletionVerificationResult) -> Unit)? = null) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                val result = repository.clearAll()
                _deletionVerificationEvent.emit(result)
                onCompleted?.invoke(result)
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun importMockData() {
        viewModelScope.launch {
            val mockList = listOf(
                StudentEntity(studentCode = "HCM-2025-101", name = "Min Khant Zaw", gender = "Male", dateOfBirth = "2014-03-25", gradeName = "G6", className = "A", rollNumber = 10, parentName = "U Zaw Zaw / Daw Khin Myo", fatherName = "U Zaw Zaw", fatherNrc = "12/KAMAYA(N)112233", motherName = "Daw Khin Myo", motherNrc = "12/KAMAYA(N)445566", studentNrc = "", phone = "09780001111", address = "Kamayut, Yangon", status = "Active", photoAvatarIndex = 1),
                StudentEntity(studentCode = "HCM-2025-102", name = "Pyae Sone Aung", gender = "Male", dateOfBirth = "2013-07-14", gradeName = "G7", className = "B", rollNumber = 4, parentName = "U Aung Than / Daw Nilar", fatherName = "U Aung Than", fatherNrc = "12/HLEDA(N)223344", motherName = "Daw Nilar", motherNrc = "12/HLEDA(N)556677", studentNrc = "", phone = "09780002222", address = "Hledan, Yangon", status = "Active", photoAvatarIndex = 2),
                StudentEntity(studentCode = "HCM-2025-103", name = "Hnin Nandar Shwe", gender = "Female", dateOfBirth = "2011-02-18", gradeName = "G9", className = "A", rollNumber = 2, parentName = "U Kyaw Shwe / Daw Shwe Shwe", fatherName = "U Kyaw Shwe", fatherNrc = "12/SAKANA(N)334455", motherName = "Daw Shwe Shwe", motherNrc = "12/SAKANA(N)667788", studentNrc = "", phone = "09780003333", address = "Sanchaung, Yangon", status = "Active", photoAvatarIndex = 3),
                StudentEntity(studentCode = "HCM-2025-104", name = "Zin Lin Htet", gender = "Male", dateOfBirth = "2009-12-01", gradeName = "G11", className = "A", rollNumber = 1, parentName = "U Htet Lin / Daw Aye Aye", fatherName = "U Htet Lin", fatherNrc = "12/BAHANA(N)445566", motherName = "Daw Aye Aye", motherNrc = "12/BAHANA(N)778899", studentNrc = "12/BAHANA(N)998877", phone = "09780004444", address = "Bahan, Yangon", status = "Active", photoAvatarIndex = 4)
            )
            repository.importMockStudents(mockList)
        }
    }
}

class StudentViewModelFactory(
    private val repository: StudentRepository,
    private val academicYearRepository: AcademicYearRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(repository, academicYearRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
