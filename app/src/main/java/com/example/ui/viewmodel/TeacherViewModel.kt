package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TeacherEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.TeacherRepository
import com.example.data.sync.DeletionVerificationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeacherViewModel(
    private val teacherRepo: TeacherRepository
) : ViewModel() {

    private val _deletionVerificationEvent = MutableSharedFlow<DeletionVerificationResult>(extraBufferCapacity = 1)
    val deletionVerificationEvent: SharedFlow<DeletionVerificationResult> = _deletionVerificationEvent.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("All")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _gradeFilter = MutableStateFlow("All Grades")
    val gradeFilter: StateFlow<String> = _gradeFilter.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val teachersList: StateFlow<List<TeacherEntity>> = combine(
        teacherRepo.allTeachers,
        _searchQuery,
        _statusFilter,
        _gradeFilter
    ) { teachers, query, status, grade ->
        teachers.filter { teacher ->
            val matchesQuery = query.isBlank() ||
                    teacher.fullName.contains(query, ignoreCase = true) ||
                    teacher.teacherCode.contains(query, ignoreCase = true) ||
                    teacher.assignedSubjects.contains(query, ignoreCase = true)

            val matchesStatus = status == "All" || teacher.employmentStatus.equals(status, ignoreCase = true)

            val matchesGrade = grade == "All Grades" ||
                    teacher.assignedGrade.contains(grade, ignoreCase = true) ||
                    teacher.assignedGrade.equals("All", ignoreCase = true) ||
                    teacher.assignedGrade.equals("All Grades", ignoreCase = true)

            matchesQuery && matchesStatus && matchesGrade
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun setGradeFilter(grade: String) {
        _gradeFilter.value = grade
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun saveTeacher(
        teacher: TeacherEntity,
        performer: UserEntity? = null,
        onSuccess: () -> Unit
    ) {
        if (teacher.teacherCode.isBlank()) {
            _toastMessage.value = "Teacher ID / Code cannot be empty"
            return
        }
        if (teacher.fullName.isBlank()) {
            _toastMessage.value = "Teacher Full Name cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                teacherRepo.saveTeacher(teacher, performer)
                _toastMessage.value = if (teacher.id == 0L) "Teacher created successfully!" else "Teacher updated successfully!"
                onSuccess()
            } catch (e: SecurityException) {
                _toastMessage.value = e.message ?: "Permission Denied: Action not allowed for your role"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to save teacher: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTeacher(
        teacher: TeacherEntity,
        performer: UserEntity? = null,
        onCompleted: ((DeletionVerificationResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val verification = teacherRepo.deleteTeacher(teacher, performer)
                _deletionVerificationEvent.emit(verification)
                onCompleted?.invoke(verification)

                _toastMessage.value = when (verification) {
                    is DeletionVerificationResult.VerifiedPurged -> "✅ Teacher '${teacher.fullName}' permanently removed from Supabase Cloud."
                    is DeletionVerificationResult.VerifiedTombstoned -> "✅ Teacher '${teacher.fullName}' soft-delete confirmed on Supabase."
                    is DeletionVerificationResult.ServerRejectedOrIgnored -> "⚠️ Supabase Warning: Server rejected or ignored deletion (${verification.reason})"
                    is DeletionVerificationResult.OfflineQueued -> "📶 Teacher deleted locally. Deletion queued for cloud sync."
                }
            } catch (e: SecurityException) {
                _toastMessage.value = e.message ?: "Permission Denied: Teachers and Staff cannot delete teacher records"
            } catch (e: Exception) {
                _toastMessage.value = "Error deleting teacher: ${e.localizedMessage}"
            }
        }
    }
}

class TeacherViewModelFactory(
    private val teacherRepo: TeacherRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            return TeacherViewModel(teacherRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
