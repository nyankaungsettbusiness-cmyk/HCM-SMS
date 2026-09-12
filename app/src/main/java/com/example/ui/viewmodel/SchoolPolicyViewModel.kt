package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.SchoolPolicyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SchoolPolicyViewModel(private val repository: SchoolPolicyRepository) : ViewModel() {

    val schoolSettings: StateFlow<SchoolSettingEntity?> = repository.schoolSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val grades: StateFlow<List<GradeEntity>> = repository.allGrades.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val classes: StateFlow<List<SchoolClassEntity>> = repository.allClasses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val assessmentTypes: StateFlow<List<AssessmentTypeEntity>> = repository.allAssessmentTypes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customExams: StateFlow<List<CustomExamEntity>> = repository.allCustomExams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val gradingPolicies: StateFlow<List<GradingPolicyEntity>> = repository.allGradingPolicies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSchoolSettings(schoolName: String, academicYear: String, phone: String, email: String, address: String) {
        android.util.Log.d("AcademicYearDebug", "VM_CALL: SchoolPolicyViewModel.updateSchoolSettings called for year='$academicYear'")
        viewModelScope.launch {
            repository.updateSchoolSettings(
                SchoolSettingEntity(
                    id = 1,
                    schoolName = schoolName,
                    academicYear = academicYear,
                    contactPhone = phone,
                    email = email,
                    address = address
                )
            )
        }
    }

    fun addGrade(gradeName: String, level: EducationLevel, reportTemplate: String) {
        viewModelScope.launch {
            repository.addGrade(GradeEntity(gradeName = gradeName, educationLevel = level, reportCardTemplate = reportTemplate))
        }
    }

    fun deleteGrade(gradeId: Long) {
        viewModelScope.launch {
            repository.deleteGrade(gradeId)
        }
    }

    fun addClass(gradeId: Long, className: String, capacity: Int) {
        viewModelScope.launch {
            repository.addClass(SchoolClassEntity(gradeId = gradeId, className = className, capacity = capacity))
        }
    }

    fun updateClass(schoolClass: SchoolClassEntity) {
        viewModelScope.launch {
            repository.updateClass(schoolClass)
        }
    }

    fun deleteClass(classId: Long) {
        viewModelScope.launch {
            repository.deleteClass(classId)
        }
    }

    fun addSubject(name: String, category: SubjectCategory, level: EducationLevel, subTrack: String = "", isCustom: Boolean = true) {
        viewModelScope.launch {
            repository.addSubject(
                SubjectEntity(
                    name = name,
                    category = category,
                    educationLevel = level,
                    subTrack = subTrack,
                    isEnabled = true,
                    isEditable = true,
                    isCustom = isCustom
                )
            )
        }
    }

    fun toggleSubjectEnabled(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject.copy(isEnabled = !subject.isEnabled))
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    fun addAssessmentType(name: String, level: EducationLevel, isCustom: Boolean = true) {
        viewModelScope.launch {
            repository.addAssessmentType(AssessmentTypeEntity(name = name, educationLevel = level, isCustom = isCustom))
        }
    }

    fun deleteAssessmentType(id: Long) {
        viewModelScope.launch {
            repository.deleteAssessmentType(id)
        }
    }

    fun addCustomExam(gradeId: Long, examName: String, description: String) {
        viewModelScope.launch {
            repository.addCustomExam(CustomExamEntity(gradeId = gradeId, examName = examName, description = description))
        }
    }

    fun toggleCustomExamEnabled(exam: CustomExamEntity) {
        viewModelScope.launch {
            repository.addCustomExam(exam.copy(isEnabled = !exam.isEnabled))
        }
    }

    fun deleteCustomExam(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomExam(id)
        }
    }

    fun updateGradingPolicy(policy: GradingPolicyEntity) {
        viewModelScope.launch {
            repository.updateGradingPolicy(policy)
        }
    }

    fun addGradingPolicy(level: EducationLevel, subjectName: String, maxMark: Int, passMark: Int, distinctionMark: Int) {
        viewModelScope.launch {
            repository.addGradingPolicy(
                GradingPolicyEntity(
                    educationLevel = level,
                    subjectName = subjectName,
                    maxMark = maxMark,
                    passMark = passMark,
                    distinctionMark = distinctionMark
                )
            )
        }
    }

    fun deleteGradingPolicy(id: Long) {
        viewModelScope.launch {
            repository.deleteGradingPolicy(id)
        }
    }
}

class SchoolPolicyViewModelFactory(private val repository: SchoolPolicyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchoolPolicyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SchoolPolicyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
