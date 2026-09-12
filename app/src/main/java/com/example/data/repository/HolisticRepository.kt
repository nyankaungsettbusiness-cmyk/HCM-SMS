package com.example.data.repository

import com.example.data.local.dao.AssessmentPeriodDao
import com.example.data.local.dao.HolisticDao
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class HolisticRepository(
    private val holisticDao: HolisticDao,
    private val assessmentPeriodDao: AssessmentPeriodDao? = null
) {
    val allHolisticCategories: Flow<List<HolisticCategoryEntity>> = holisticDao.getAllHolisticCategories()
    val enabledHolisticCategories: Flow<List<HolisticCategoryEntity>> = holisticDao.getEnabledHolisticCategories()

    // Assessment Period operations
    fun getAssessmentPeriodsForLevel(level: String): Flow<List<AssessmentPeriodEntity>> =
        assessmentPeriodDao?.getAssessmentPeriodsForLevel(level)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getEnabledAssessmentPeriodsForLevel(level: String): Flow<List<AssessmentPeriodEntity>> =
        assessmentPeriodDao?.getEnabledAssessmentPeriodsForLevel(level)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun addAssessmentPeriod(period: AssessmentPeriodEntity): Long {
        val dirty = period.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val res = assessmentPeriodDao?.insertAssessmentPeriod(dirty) ?: 0L
        triggerBackgroundSync()
        return res
    }

    suspend fun updateAssessmentPeriod(period: AssessmentPeriodEntity) {
        val dirty = period.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        assessmentPeriodDao?.updateAssessmentPeriod(dirty)
        triggerBackgroundSync()
    }

    suspend fun deleteAssessmentPeriod(id: Long) {
        assessmentPeriodDao?.deleteAssessmentPeriod(id)
        triggerBackgroundSync()
    }

    fun getHolisticCategoriesForLevel(level: String): Flow<List<HolisticCategoryEntity>> =
        holisticDao.getHolisticCategoriesForLevel(level)

    fun getEnabledHolisticCategoriesForLevel(level: String): Flow<List<HolisticCategoryEntity>> =
        holisticDao.getEnabledHolisticCategoriesForLevel(level)

    suspend fun getHolisticCategoriesForLevelList(level: String): List<HolisticCategoryEntity> =
        holisticDao.getHolisticCategoriesForLevelList(level)

    suspend fun addHolisticCategory(category: HolisticCategoryEntity): Long {
        val dirty = category.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val res = holisticDao.insertHolisticCategory(dirty)
        triggerBackgroundSync()
        return res
    }

    suspend fun addHolisticCategories(categories: List<HolisticCategoryEntity>) {
        val dirtyList = categories.map { it.copy(isDirty = true, updatedAt = System.currentTimeMillis()) }
        holisticDao.insertHolisticCategories(dirtyList)
        triggerBackgroundSync()
    }

    suspend fun updateHolisticCategory(category: HolisticCategoryEntity) {
        val dirty = category.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        holisticDao.updateHolisticCategory(dirty)
        triggerBackgroundSync()
    }

    suspend fun deleteHolisticCategory(category: HolisticCategoryEntity) {
        holisticDao.deleteHolisticCategory(category)
        triggerBackgroundSync()
    }

    suspend fun duplicateHolisticCategory(category: HolisticCategoryEntity): Long {
        val copy = category.copy(
            id = 0,
            categoryName = "${category.categoryName} (Copy)",
            orderIndex = category.orderIndex + 1,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        val res = holisticDao.insertHolisticCategory(copy)
        triggerBackgroundSync()
        return res
    }

    suspend fun copyConfigurationBetweenLevels(sourceLevel: String, targetLevel: String) {
        if (sourceLevel == targetLevel) return
        val sourceItems = holisticDao.getHolisticCategoriesForLevelList(sourceLevel)
        if (sourceItems.isEmpty()) return

        holisticDao.deleteHolisticCategoriesForLevel(targetLevel)

        val targetItems = sourceItems.map { item ->
            item.copy(
                id = 0,
                educationLevel = targetLevel,
                isDefault = false,
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            )
        }
        holisticDao.insertHolisticCategories(targetItems)
        triggerBackgroundSync()
    }

    val allSgiCategories: Flow<List<SgiCategoryEntity>> = holisticDao.getAllSgiCategories()
    val enabledSgiCategories: Flow<List<SgiCategoryEntity>> = holisticDao.getEnabledSgiCategories()

    suspend fun addSgiCategory(category: SgiCategoryEntity): Long {
        val dirty = category.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val res = holisticDao.insertSgiCategory(dirty)
        triggerBackgroundSync()
        return res
    }

    suspend fun updateSgiCategory(category: SgiCategoryEntity) {
        val dirty = category.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        holisticDao.updateSgiCategory(dirty)
        triggerBackgroundSync()
    }

    suspend fun deleteSgiCategory(category: SgiCategoryEntity) {
        holisticDao.deleteSgiCategory(category)
        triggerBackgroundSync()
    }

    suspend fun duplicateSgiCategory(category: SgiCategoryEntity): Long {
        val copy = category.copy(
            id = 0,
            indicatorName = "${category.indicatorName} (Copy)",
            orderIndex = category.orderIndex + 1,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        val res = holisticDao.insertSgiCategory(copy)
        triggerBackgroundSync()
        return res
    }

    fun getHolisticResultsForStudent(studentId: Long, period: String, academicYear: String): Flow<List<HolisticResultEntity>> {
        return holisticDao.getHolisticResultsForStudent(studentId, period, academicYear)
    }

    suspend fun saveHolisticResults(results: List<HolisticResultEntity>) {
        val dirty = results.map { it.copy(isDirty = true, updatedAt = System.currentTimeMillis()) }
        holisticDao.insertOrUpdateHolisticResults(dirty)
        triggerBackgroundSync()
    }

    fun getSgiResultsForStudent(studentId: Long, period: String, academicYear: String): Flow<List<SgiResultEntity>> {
        return holisticDao.getSgiResultsForStudent(studentId, period, academicYear)
    }

    suspend fun saveSgiResults(results: List<SgiResultEntity>) {
        val dirty = results.map { it.copy(isDirty = true, updatedAt = System.currentTimeMillis()) }
        holisticDao.insertOrUpdateSgiResults(dirty)
        triggerBackgroundSync()
    }

    fun getTeacherCommentForStudent(studentId: Long, period: String, academicYear: String): Flow<TeacherCommentEntity?> {
        return holisticDao.getTeacherCommentForStudent(studentId, period, academicYear)
    }

    suspend fun saveTeacherComment(comment: TeacherCommentEntity) {
        val dirty = comment.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        holisticDao.insertOrUpdateTeacherComment(dirty)
        triggerBackgroundSync()
    }

    private fun triggerBackgroundSync() {
        com.example.data.sync.SyncManager.triggerTableSyncAsync("holistic_results")
    }
}
