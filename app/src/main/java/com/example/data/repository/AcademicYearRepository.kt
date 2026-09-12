package com.example.data.repository

import com.example.data.local.dao.AcademicYearDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class AcademicYearRepository(
    private val academicYearDao: AcademicYearDao,
    private val studentDao: StudentDao,
    private val schoolPolicyDao: com.example.data.local.dao.SchoolPolicyDao? = null
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            try {
                academicYearDao.sanitizeActiveAcademicYears()
            } catch (e: Exception) {
                android.util.Log.e("AcademicYearRepository", "Error sanitizing active academic years: ${e.message}")
            }
        }
    }

    fun getAllAcademicYears(): Flow<List<AcademicYearEntity>> {
        return academicYearDao.getAllAcademicYears()
    }

    fun getActiveAcademicYear(): Flow<AcademicYearEntity?> {
        return academicYearDao.getActiveAcademicYear()
    }

    suspend fun getActiveAcademicYearSync(): AcademicYearEntity? {
        return academicYearDao.getSanitizedActiveAcademicYearSync()
    }

    suspend fun saveAcademicYear(
        id: Long = 0,
        yearCode: String,
        displayName: String,
        startDate: String,
        endDate: String,
        status: AcademicYearStatus = AcademicYearStatus.UPCOMING
    ): Long {
        val entity = AcademicYearEntity(
            id = id,
            yearCode = yearCode,
            displayName = displayName,
            startDate = startDate,
            endDate = endDate,
            status = status,
            isCurrentActive = false,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        val res = academicYearDao.insertAcademicYear(entity)
        triggerBackgroundSync()
        return res
    }

    suspend fun activateAcademicYear(id: Long) {
        android.util.Log.d("AcademicYearTrace", "REPO_ACTIVATE_START: Activating yearId=$id")
        academicYearDao.activateAcademicYear(id)
        val activeEntity = academicYearDao.getAcademicYearById(id)
        if (activeEntity != null && schoolPolicyDao != null) {
            val setting = schoolPolicyDao.getSchoolSettingsSync()
            if (setting != null) {
                schoolPolicyDao.updateSchoolSettings(setting.copy(academicYear = activeEntity.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
            } else {
                schoolPolicyDao.updateSchoolSettings(com.example.data.local.entity.SchoolSettingEntity(academicYear = activeEntity.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
            }
        }
        val activeNow = academicYearDao.getSanitizedActiveAcademicYearSync()
        val settingsNow = schoolPolicyDao?.getSchoolSettingsSync()
        android.util.Log.d("AcademicYearTrace", "REPO_ACTIVATE_END: Active year in Room=${activeNow?.yearCode} (isCurrentActive=${activeNow?.isCurrentActive}), school_settings.academicYear=${settingsNow?.academicYear}")
        triggerBackgroundSync()
    }

    suspend fun activateAcademicYearByCode(yearCode: String) {
        if (yearCode.isBlank()) return
        android.util.Log.d("AcademicYearTrace", "REPO_ACTIVATE_BY_CODE_START: Activating yearCode='$yearCode'")
        academicYearDao.activateAcademicYearByCode(yearCode)
        val activeEntity = academicYearDao.getAcademicYearByCode(yearCode)
        if (activeEntity != null && schoolPolicyDao != null) {
            val setting = schoolPolicyDao.getSchoolSettingsSync()
            if (setting != null) {
                schoolPolicyDao.updateSchoolSettings(setting.copy(academicYear = activeEntity.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
            } else {
                schoolPolicyDao.updateSchoolSettings(com.example.data.local.entity.SchoolSettingEntity(academicYear = activeEntity.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
            }
        }
        val activeNow = academicYearDao.getSanitizedActiveAcademicYearSync()
        val settingsNow = schoolPolicyDao?.getSchoolSettingsSync()
        android.util.Log.d("AcademicYearTrace", "REPO_ACTIVATE_BY_CODE_END: Active year in Room=${activeNow?.yearCode} (isCurrentActive=${activeNow?.isCurrentActive}), school_settings.academicYear=${settingsNow?.academicYear}")
        triggerBackgroundSync()
    }

    suspend fun closeAcademicYear(id: Long, closedBy: String) {
        val target = academicYearDao.getAcademicYearById(id)
        android.util.Log.d("AcademicYearTrace", "REPO_CLOSE_START: Closing yearId=$id ('${target?.yearCode}')")
        val wasActive = target?.isCurrentActive == true
        academicYearDao.updateAcademicYearStatus(
            id = id,
            status = AcademicYearStatus.CLOSED,
            closedDate = System.currentTimeMillis(),
            closedBy = closedBy,
            timestamp = System.currentTimeMillis()
        )
        if (wasActive) {
            val remaining = academicYearDao.getSanitizedActiveAcademicYearSync()
            if (remaining != null && schoolPolicyDao != null) {
                val setting = schoolPolicyDao.getSchoolSettingsSync()
                if (setting != null) {
                    schoolPolicyDao.updateSchoolSettings(setting.copy(academicYear = remaining.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
                }
            }
        }
        val activeNow = academicYearDao.getSanitizedActiveAcademicYearSync()
        android.util.Log.d("AcademicYearTrace", "REPO_CLOSE_END: Active year in Room after close = ${activeNow?.yearCode}")
        triggerBackgroundSync()
    }

    suspend fun archiveAcademicYear(id: Long) {
        val target = academicYearDao.getAcademicYearById(id)
        android.util.Log.d("AcademicYearTrace", "REPO_ARCHIVE_START: Archiving yearId=$id ('${target?.yearCode}')")
        val wasActive = target?.isCurrentActive == true
        academicYearDao.updateAcademicYearStatus(
            id = id,
            status = AcademicYearStatus.ARCHIVED,
            closedDate = System.currentTimeMillis(),
            closedBy = "",
            timestamp = System.currentTimeMillis()
        )
        if (wasActive) {
            val remaining = academicYearDao.getSanitizedActiveAcademicYearSync()
            if (remaining != null && schoolPolicyDao != null) {
                val setting = schoolPolicyDao.getSchoolSettingsSync()
                if (setting != null) {
                    schoolPolicyDao.updateSchoolSettings(setting.copy(academicYear = remaining.yearCode, isDirty = true, updatedAt = System.currentTimeMillis()))
                }
            }
        }
        val activeNow = academicYearDao.getSanitizedActiveAcademicYearSync()
        android.util.Log.d("AcademicYearTrace", "REPO_ARCHIVE_END: Active year in Room after archive = ${activeNow?.yearCode}")
        triggerBackgroundSync()
    }

    private fun triggerBackgroundSync() {
        com.example.data.sync.SyncManager.triggerTableSyncAsync("academic_years")
    }

    // Promotion History
    fun getAllPromotionHistory(): Flow<List<PromotionHistoryEntity>> {
        return academicYearDao.getAllPromotionHistory()
    }

    fun getPromotionHistoryForStudent(studentId: Long): Flow<List<PromotionHistoryEntity>> {
        return academicYearDao.getPromotionHistoryForStudent(studentId)
    }

    // Student Academic History
    fun getAllStudentAcademicHistories(): Flow<List<StudentAcademicHistoryEntity>> {
        return academicYearDao.getAllStudentAcademicHistories()
    }

    fun getStudentAcademicHistoriesByYear(academicYear: String): Flow<List<StudentAcademicHistoryEntity>> {
        return academicYearDao.getStudentAcademicHistoriesByYear(academicYear)
    }

    fun getStudentAcademicHistories(studentId: Long): Flow<List<StudentAcademicHistoryEntity>> {
        return academicYearDao.getStudentAcademicHistories(studentId)
    }

    suspend fun saveStudentAcademicHistory(history: StudentAcademicHistoryEntity) {
        academicYearDao.insertStudentAcademicHistory(history)
    }

    // Grade progression mapping helper
    fun getNextGrade(currentGrade: String): String {
        return when (currentGrade.uppercase().trim()) {
            "KG" -> "G1"
            "G1" -> "G2"
            "G2" -> "G3"
            "G3" -> "G4"
            "G4" -> "G5"
            "G5" -> "G6"
            "G6" -> "G7"
            "G7" -> "G8"
            "G8" -> "G9"
            "G9" -> "G10"
            "G10" -> "G11"
            "G11" -> "G12"
            "G12" -> "Alumni"
            else -> currentGrade
        }
    }

    // Individual Promotion / Retention / Transfer / Graduation
    suspend fun promoteStudent(
        student: StudentEntity,
        fromAcademicYear: String,
        toAcademicYear: String,
        targetGrade: String,
        targetClass: String,
        targetRollNumber: Int,
        actionType: PromotionAction,
        promotedBy: String,
        remarks: String = ""
    ) {
        // 1. Save historical academic snapshot
        val historySnapshot = StudentAcademicHistoryEntity(
            studentId = student.id,
            academicYear = fromAcademicYear,
            gradeName = student.gradeName,
            className = student.className,
            rollNumber = student.rollNumber,
            status = actionType.displayName
        )
        academicYearDao.insertStudentAcademicHistory(historySnapshot)

        // 2. Log promotion history record
        val promotionLog = PromotionHistoryEntity(
            studentId = student.id,
            studentCode = student.studentCode,
            studentName = student.name,
            fromAcademicYear = fromAcademicYear,
            toAcademicYear = toAcademicYear,
            fromGrade = student.gradeName,
            toGrade = targetGrade,
            fromClass = student.className,
            toClass = targetClass,
            fromRollNumber = student.rollNumber,
            toRollNumber = targetRollNumber,
            actionType = actionType,
            promotedDate = System.currentTimeMillis(),
            promotedBy = promotedBy,
            remarks = remarks
        )
        academicYearDao.insertPromotionHistory(promotionLog)

        // 3. Update student profile for new academic year
        val newStatus = if (targetGrade == "Alumni" || actionType == PromotionAction.GRADUATED) {
            "Graduated"
        } else {
            "Active"
        }

        val updatedStudent = student.copy(
            gradeName = targetGrade,
            className = targetClass,
            rollNumber = targetRollNumber,
            status = newStatus
        )
        studentDao.insertStudent(updatedStudent)

        // 4. Save toAcademicYear history snapshot
        val toHistorySnapshot = StudentAcademicHistoryEntity(
            studentId = student.id,
            academicYear = toAcademicYear,
            gradeName = targetGrade,
            className = targetClass,
            rollNumber = targetRollNumber,
            status = newStatus
        )
        academicYearDao.insertStudentAcademicHistory(toHistorySnapshot)
    }

    // Bulk Promote Whole Class
    suspend fun promoteClass(
        students: List<StudentEntity>,
        fromAcademicYear: String,
        toAcademicYear: String,
        targetGrade: String,
        targetClass: String,
        actionType: PromotionAction = PromotionAction.PROMOTED,
        promotedBy: String
    ) {
        students.forEachIndexed { index, student ->
            promoteStudent(
                student = student,
                fromAcademicYear = fromAcademicYear,
                toAcademicYear = toAcademicYear,
                targetGrade = targetGrade,
                targetClass = targetClass,
                targetRollNumber = index + 1,
                actionType = actionType,
                promotedBy = promotedBy,
                remarks = "Bulk class promotion from ${student.gradeName}-${student.className} to $targetGrade-$targetClass"
            )
        }
    }

    // Bulk Promote Whole Grade
    suspend fun promoteGrade(
        students: List<StudentEntity>,
        fromAcademicYear: String,
        toAcademicYear: String,
        targetGrade: String,
        promotedBy: String
    ) {
        val groupedByClass = students.groupBy { it.className }
        groupedByClass.forEach { (className, classStudents) ->
            classStudents.forEachIndexed { index, student ->
                promoteStudent(
                    student = student,
                    fromAcademicYear = fromAcademicYear,
                    toAcademicYear = toAcademicYear,
                    targetGrade = targetGrade,
                    targetClass = className,
                    targetRollNumber = index + 1,
                    actionType = PromotionAction.PROMOTED,
                    promotedBy = promotedBy,
                    remarks = "Bulk grade promotion to $targetGrade"
                )
            }
        }
    }
}
