package com.example.data.repository

import com.example.data.local.dao.SchoolPolicyDao
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class SchoolPolicyRepository(
    private val schoolPolicyDao: SchoolPolicyDao,
    private val academicYearDao: com.example.data.local.dao.AcademicYearDao? = null
) {

    val schoolSettings: Flow<SchoolSettingEntity?> = schoolPolicyDao.getSchoolSettings()
    val allGrades: Flow<List<GradeEntity>> = schoolPolicyDao.getAllGrades()
    val allClasses: Flow<List<SchoolClassEntity>> = schoolPolicyDao.getAllClasses()
    val allSubjects: Flow<List<SubjectEntity>> = schoolPolicyDao.getAllSubjects()
    val allAssessmentTypes: Flow<List<AssessmentTypeEntity>> = schoolPolicyDao.getAllAssessmentTypes()
    val allCustomExams: Flow<List<CustomExamEntity>> = schoolPolicyDao.getAllCustomExams()
    val allGradingPolicies: Flow<List<GradingPolicyEntity>> = schoolPolicyDao.getAllGradingPolicies()

    suspend fun updateSchoolSettings(settings: SchoolSettingEntity) {
        android.util.Log.d("AcademicYearDebug", "BEFORE_YEAR_CHANGE: Updating school settings with academicYear='${settings.academicYear}'")
        val current = schoolPolicyDao.getSchoolSettingsSync()
        val dirty = (current?.copy(
            schoolName = settings.schoolName.ifBlank { current.schoolName },
            academicYear = settings.academicYear.ifBlank { current.academicYear },
            contactPhone = settings.contactPhone.ifBlank { current.contactPhone },
            email = settings.email.ifBlank { current.email },
            address = settings.address.ifBlank { current.address },
            website = settings.website.ifBlank { current.website },
            principalName = settings.principalName.ifBlank { current.principalName },
            motto = settings.motto.ifBlank { current.motto },
            logoText = settings.logoText.ifBlank { current.logoText },
            schoolSeal = settings.schoolSeal.ifBlank { current.schoolSeal },
            logoUri = settings.logoUri?.ifBlank { current.logoUri } ?: current.logoUri,
            systemNotificationsEnabled = settings.systemNotificationsEnabled,
            backupRemindersEnabled = settings.backupRemindersEnabled,
            academicYearReminderEnabled = settings.academicYearReminderEnabled,
            assessmentReminderEnabled = settings.assessmentReminderEnabled,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        ) ?: settings.copy(isDirty = true, updatedAt = System.currentTimeMillis()))

        schoolPolicyDao.updateSchoolSettings(dirty)
        if (dirty.academicYear.isNotBlank() && academicYearDao != null) {
            academicYearDao.activateAcademicYearByCode(dirty.academicYear)
        }
        val activeNow = academicYearDao?.getActiveAcademicYearSync()
        val settingsNow = schoolPolicyDao.getSchoolSettingsSync()
        android.util.Log.d("AcademicYearDebug", "AFTER_ROOM_UPDATE: Active year in Room=${activeNow?.yearCode} (isCurrentActive=${activeNow?.isCurrentActive}), school_settings.academicYear=${settingsNow?.academicYear}")
        triggerBackgroundSync()
    }

    suspend fun addGrade(grade: GradeEntity): Long {
        val trimmedName = grade.gradeName.trim()
        val existing = schoolPolicyDao.getGradeByName(trimmedName) ?: schoolPolicyDao.getAllGradesSync().firstOrNull { it.gradeName.equals(trimmedName, ignoreCase = true) }
        if (existing != null) {
            return existing.id
        }
        val dirty = grade.copy(gradeName = trimmedName, isDirty = true, updatedAt = System.currentTimeMillis())
        val gradeId = schoolPolicyDao.insertGrade(dirty)
        // Auto add default class A if no classes exist
        val existingClasses = schoolPolicyDao.getClassesForGradeSync(gradeId)
        if (existingClasses.isEmpty()) {
            val defaultClass = SchoolClassEntity(gradeId = gradeId, className = "A", isDirty = true, updatedAt = System.currentTimeMillis())
            schoolPolicyDao.insertClass(defaultClass)
        }
        triggerBackgroundSync()
        return gradeId
    }

    suspend fun deleteGrade(gradeId: Long) {
        schoolPolicyDao.deleteGrade(gradeId)
        triggerBackgroundSync()
    }

    suspend fun addClass(schoolClass: SchoolClassEntity): Long {
        val trimmedName = schoolClass.className.trim()
        val existing = schoolPolicyDao.getClassesForGradeSync(schoolClass.gradeId).firstOrNull { it.className.equals(trimmedName, ignoreCase = true) }
        if (existing != null) {
            return existing.id
        }
        val dirty = schoolClass.copy(className = trimmedName, isDirty = true, updatedAt = System.currentTimeMillis())
        val id = schoolPolicyDao.insertClass(dirty)
        triggerBackgroundSync()
        return id
    }

    suspend fun updateClass(schoolClass: SchoolClassEntity) {
        val trimmedName = schoolClass.className.trim()
        val dirty = schoolClass.copy(className = trimmedName, isDirty = true, updatedAt = System.currentTimeMillis())
        schoolPolicyDao.insertClass(dirty)
        triggerBackgroundSync()
    }

    suspend fun deleteClass(classId: Long) {
        schoolPolicyDao.deleteClass(classId)
        triggerBackgroundSync()
    }

    suspend fun addSubject(subject: SubjectEntity): Long {
        val existing = schoolPolicyDao.getSubjectByNameAndLevel(subject.name.trim(), subject.educationLevel)
        val id = if (existing != null) {
            val updated = existing.copy(
                name = subject.name.trim(),
                category = subject.category,
                educationLevel = subject.educationLevel,
                subTrack = subject.subTrack,
                isEnabled = true,
                isEditable = true,
                isCustom = subject.isCustom,
                isDeleted = false,
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            )
            schoolPolicyDao.updateSubject(updated)
            existing.id
        } else {
            val dirty = subject.copy(
                name = subject.name.trim(),
                isDirty = true,
                updatedAt = System.currentTimeMillis(),
                uuid = subject.uuid.ifBlank { java.util.UUID.randomUUID().toString() }
            )
            schoolPolicyDao.insertSubject(dirty)
        }
        triggerBackgroundSync()
        return id
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        val dirty = subject.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        schoolPolicyDao.updateSubject(dirty)
        triggerBackgroundSync()
    }

    suspend fun deleteSubject(subjectId: Long) {
        schoolPolicyDao.softDeleteSubject(subjectId, System.currentTimeMillis())
        triggerBackgroundSync()
    }

    suspend fun addAssessmentType(type: AssessmentTypeEntity): Long {
        val dirty = type.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val id = schoolPolicyDao.insertAssessmentType(dirty)
        triggerBackgroundSync()
        return id
    }

    suspend fun deleteAssessmentType(id: Long) {
        schoolPolicyDao.deleteAssessmentType(id)
        triggerBackgroundSync()
    }

    suspend fun addCustomExam(exam: CustomExamEntity): Long {
        val dirty = exam.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val id = schoolPolicyDao.insertCustomExam(dirty)
        triggerBackgroundSync()
        return id
    }

    suspend fun deleteCustomExam(id: Long) {
        schoolPolicyDao.deleteCustomExam(id)
        triggerBackgroundSync()
    }

    suspend fun updateGradingPolicy(policy: GradingPolicyEntity) {
        val dirty = policy.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        schoolPolicyDao.updateGradingPolicy(dirty)
        triggerBackgroundSync()
    }

    suspend fun addGradingPolicy(policy: GradingPolicyEntity): Long {
        val dirty = policy.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val id = schoolPolicyDao.insertGradingPolicy(dirty)
        triggerBackgroundSync()
        return id
    }

    suspend fun deleteGradingPolicy(id: Long) {
        schoolPolicyDao.deleteGradingPolicy(id)
        triggerBackgroundSync()
    }

    private fun triggerBackgroundSync() {
        com.example.data.sync.SyncManager.triggerTableSyncAsync("school_settings")
    }
}
