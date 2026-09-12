package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.HolisticRepository
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HolisticViewModel(
    private val holisticRepository: HolisticRepository,
    private val studentRepository: StudentRepository,
    private val academicYearRepository: com.example.data.repository.AcademicYearRepository? = null
) : ViewModel() {

    // Filter Controls
    val academicYear = MutableStateFlow("2026-2027")
    val selectedGrade = MutableStateFlow("G5")
    val selectedClass = MutableStateFlow("A")
    val selectedPeriod = MutableStateFlow("June")
    val searchQuery = MutableStateFlow("")
    val selectedStudent = MutableStateFlow<StudentEntity?>(null)

    // Admin Level Selector for Config tab
    val adminConfigLevel = MutableStateFlow("PRIMARY")

    private val academicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearRepository?.getAllStudentAcademicHistories()
        ?: flowOf(emptyList())

    // Education Level derived from selectedStudent or selectedGrade
    val activeEducationLevel: StateFlow<String> = combine(selectedGrade, selectedStudent) { grade, student ->
        val targetGrade = student?.gradeName ?: grade
        val norm = targetGrade.uppercase().trim()
        when {
            norm == "KG" || norm.contains("KINDERGARTEN") -> "KINDERGARTEN"
            norm in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5", "PRIMARY 1", "PRIMARY 2", "PRIMARY 3", "PRIMARY 4", "PRIMARY 5") || norm.startsWith("PRIMARY") -> "PRIMARY"
            norm in listOf("G6", "G7", "G8", "G9", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") || norm.startsWith("SECONDARY") -> "SECONDARY"
            norm in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") || norm.startsWith("HIGH") -> "HIGH_SCHOOL"
            else -> "PRIMARY"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PRIMARY")

    // Dynamic Assessment Periods for active grade/level
    val enabledAssessmentPeriodsForLevel: StateFlow<List<AssessmentPeriodEntity>> = activeEducationLevel.flatMapLatest { level ->
        holisticRepository.getEnabledAssessmentPeriodsForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePeriods: StateFlow<List<String>> = enabledAssessmentPeriodsForLevel.map { periods ->
        val standardMonths = listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
        if (periods.isEmpty()) {
            standardMonths
        } else {
            val validMonths = periods.map { it.periodName }.filter { standardMonths.contains(it) }
            if (validMonths.isEmpty()) standardMonths else validMonths
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
    )

    init {
        viewModelScope.launch {
            availablePeriods.collect { periods ->
                if (periods.isNotEmpty() && !periods.contains(selectedPeriod.value)) {
                    selectedPeriod.value = periods.first()
                }
            }
        }
        viewModelScope.launch {
            activeEducationLevel.collect { level ->
                ensureDefaultHcmItemsForLevel(level)
            }
        }
        viewModelScope.launch {
            adminConfigLevel.collect { level ->
                ensureDefaultHcmItemsForLevel(level)
            }
        }
    }

    private fun ensureDefaultHcmItemsForLevel(level: String) {
        viewModelScope.launch {
            val existing = holisticRepository.getHolisticCategoriesForLevelList(level)
            if (existing.isEmpty()) {
                val defaultItems = getDefaultHcmItemsForLevel(level)
                if (defaultItems.isNotEmpty()) {
                    holisticRepository.addHolisticCategories(defaultItems)
                }
            }
        }
    }

    private fun getDefaultHcmItemsForLevel(level: String): List<HolisticCategoryEntity> {
        return when (level.uppercase()) {
            "KINDERGARTEN" -> listOf(
                HolisticCategoryEntity(categoryName = "မိမိကိုယ်ကို ယုံကြည်မှုရှိခြင်း", description = "Self-confidence and self-reliance in daily activities", pillar = "HONESTY", educationLevel = "KINDERGARTEN", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 1),
                HolisticCategoryEntity(categoryName = "စည်းကမ်းလိုက်နာခြင်း", description = "Adherence to classroom rules and teacher guidance", pillar = "HONESTY", educationLevel = "KINDERGARTEN", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 2),
                HolisticCategoryEntity(categoryName = "သူငယ်ချင်းများနှင့် ပူးပေါင်းဆောင်ရွက်ခြင်း", description = "Peer cooperation and friendly interaction", pillar = "MINDFULNESS", educationLevel = "KINDERGARTEN", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 3),
                HolisticCategoryEntity(categoryName = "ဆရာ/ဆရာမ၏ ညွှန်ကြားချက်ကို လိုက်နာခြင်း", description = "Following instructions attentiveness", pillar = "HONESTY", educationLevel = "KINDERGARTEN", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 4),
                HolisticCategoryEntity(categoryName = "အခြေခံတစ်ကိုယ်ရေသန့်ရှင်းရေးကို ထိန်းသိမ်းခြင်း", description = "Basic personal hygiene and cleanliness habits", pillar = "MINDFULNESS", educationLevel = "KINDERGARTEN", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 5)
            )
            "PRIMARY" -> listOf(
                HolisticCategoryEntity(categoryName = "တာဝန်ယူမှုရှိခြင်း", description = "Personal responsibility and duty fulfillment", pillar = "HONESTY", educationLevel = "PRIMARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 1),
                HolisticCategoryEntity(categoryName = "သင်ယူမှုတွင် စိတ်ပါဝင်စားခြင်း", description = "Enthusiasm and active engagement in learning", pillar = "CURIOSITY", educationLevel = "PRIMARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 2),
                HolisticCategoryEntity(categoryName = "အဖွဲ့လိုက်လုပ်ဆောင်နိုင်ခြင်း", description = "Teamwork and active group participation", pillar = "MINDFULNESS", educationLevel = "PRIMARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 3),
                HolisticCategoryEntity(categoryName = "အချိန်ကို တန်ဖိုးထားအသုံးပြုခြင်း", description = "Punctuality and efficient time management", pillar = "HONESTY", educationLevel = "PRIMARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 4),
                HolisticCategoryEntity(categoryName = "ရိုသေလေးစားမှုနှင့် ယဉ်ကျေးပျူငှာမှုရှိခြင်း", description = "Respect towards elders and polite social etiquette", pillar = "MINDFULNESS", educationLevel = "PRIMARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 5)
            )
            "SECONDARY" -> listOf(
                HolisticCategoryEntity(categoryName = "မိမိကိုယ်ကို စီမံခန့်ခွဲနိုင်ခြင်း", description = "Self-management and emotional regulation", pillar = "MINDFULNESS", educationLevel = "SECONDARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 1),
                HolisticCategoryEntity(categoryName = "ဝေဖန်စဉ်းစားနိုင်ခြင်း", description = "Critical thinking and logical analysis", pillar = "CURIOSITY", educationLevel = "SECONDARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 2),
                HolisticCategoryEntity(categoryName = "ခေါင်းဆောင်မှုစွမ်းရည်", description = "Leadership skills and positive initiative", pillar = "HONESTY", educationLevel = "SECONDARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 3),
                HolisticCategoryEntity(categoryName = "လူမှုဆက်ဆံရေးကောင်းမွန်ခြင်း", description = "Effective communication and interpersonal relationship", pillar = "MINDFULNESS", educationLevel = "SECONDARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 4),
                HolisticCategoryEntity(categoryName = "ပြဿနာဖြေရှင်းနိုင်စွမ်းရှိခြင်း", description = "Problem solving capability and resilience", pillar = "CURIOSITY", educationLevel = "SECONDARY", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 5)
            )
            "HIGH_SCHOOL" -> listOf(
                HolisticCategoryEntity(categoryName = "စည်းကမ်းနှင့် တာဝန်ယူမှု", description = "High integrity, discipline, and ethical responsibility", pillar = "HONESTY", educationLevel = "HIGH_SCHOOL", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 1),
                HolisticCategoryEntity(categoryName = "မိမိရည်မှန်းချက်အတွက် ကြိုးစားအားထုတ်မှု", description = "Goal-oriented effort and academic dedication", pillar = "CURIOSITY", educationLevel = "HIGH_SCHOOL", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 2),
                HolisticCategoryEntity(categoryName = "ခေါင်းဆောင်မှုနှင့် ပူးပေါင်းဆောင်ရွက်မှု", description = "Advanced leadership and strategic collaboration", pillar = "MINDFULNESS", educationLevel = "HIGH_SCHOOL", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 3),
                HolisticCategoryEntity(categoryName = "ကိုယ်ပိုင်ဆုံးဖြတ်ချက်ချနိုင်မှု", description = "Independent decision-making and maturity", pillar = "CURIOSITY", educationLevel = "HIGH_SCHOOL", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 4),
                HolisticCategoryEntity(categoryName = "လူ့ကျင့်ဝတ်နှင့် ပတ်ဝန်းကျင်ဆိုင်ရာ သတိပြုမှု", description = "Ethical conduct and civic/environmental awareness", pillar = "MINDFULNESS", educationLevel = "HIGH_SCHOOL", maxStars = 5, isEnabled = true, isDefault = true, orderIndex = 5)
            )
            else -> emptyList()
        }
    }

    // Admin Assessment Periods for selected admin level
    val adminAssessmentPeriods: StateFlow<List<AssessmentPeriodEntity>> = adminConfigLevel.flatMapLatest { level ->
        holisticRepository.getAssessmentPeriodsForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Lists for current student's education level
    val holisticCategoriesForLevel: StateFlow<List<HolisticCategoryEntity>> = activeEducationLevel.flatMapLatest { level ->
        holisticRepository.getHolisticCategoriesForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledHolisticCategoriesForLevel: StateFlow<List<HolisticCategoryEntity>> = activeEducationLevel.flatMapLatest { level ->
        holisticRepository.getEnabledHolisticCategoriesForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Config Categories list for selected admin level
    val adminHolisticCategories: StateFlow<List<HolisticCategoryEntity>> = adminConfigLevel.flatMapLatest { level ->
        holisticRepository.getHolisticCategoriesForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHolisticCategories: StateFlow<List<HolisticCategoryEntity>> = holisticRepository.allHolisticCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sgiCategories: StateFlow<List<SgiCategoryEntity>> = holisticRepository.allSgiCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledSgiCategories: StateFlow<List<SgiCategoryEntity>> = holisticRepository.enabledSgiCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class StudentFilterParams(
        val grade: String,
        val className: String,
        val query: String,
        val year: String
    )

    private val filterParamsFlow = combine(
        selectedGrade,
        selectedClass,
        searchQuery,
        academicYear
    ) { grade, className, query, year ->
        StudentFilterParams(grade, className, query, year)
    }

    // Filtered Students scoped to Academic Year
    val filteredStudents: StateFlow<List<StudentEntity>> = combine(
        filterParamsFlow,
        studentRepository.allStudents,
        academicHistories
    ) { params, all, histories ->
        val historiesByStudent = histories.groupBy { it.studentId }

        all.mapNotNull { student ->
            val studentHistories = historiesByStudent[student.id] ?: emptyList()
            val yearHistory = studentHistories.firstOrNull { it.academicYear.equals(params.year, ignoreCase = true) }

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
                if (params.year.equals(activeCode, ignoreCase = true)) {
                    student
                } else {
                    null
                }
            }
        }.filter { student ->
            val matchGrade = (params.grade == "All" || student.gradeName.equals(params.grade, ignoreCase = true))
            val matchClass = (params.className == "All" || student.className.equals(params.className, ignoreCase = true))
            val matchQuery = params.query.isBlank() ||
                    student.name.contains(params.query, ignoreCase = true) ||
                    student.studentCode.contains(params.query, ignoreCase = true) ||
                    student.rollNumber.toString() == params.query.trim()
            matchGrade && matchClass && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Ratings & Comments State
    val holisticRatings = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val sgiRatings = MutableStateFlow<Map<Long, Int>>(emptyMap())

    val positiveComments = MutableStateFlow("")
    val areasForImprovement = MutableStateFlow("")
    val generalComment = MutableStateFlow("")
    val futureRecommendation = MutableStateFlow("")

    val statusMessage = MutableStateFlow<String?>(null)

    // Auto-Calculated Scores State
    val averageHonestyScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val honestyItems = categories.filter { it.pillar.equals("HONESTY", ignoreCase = true) }
        val rated = honestyItems.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageCuriosityScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val curiosityItems = categories.filter { it.pillar.equals("CURIOSITY", ignoreCase = true) }
        val rated = curiosityItems.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageMindfulnessScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val mindfulnessItems = categories.filter { it.pillar.equals("MINDFULNESS", ignoreCase = true) }
        val rated = mindfulnessItems.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val overallHcmScore: StateFlow<Double> = combine(enabledHolisticCategoriesForLevel, holisticRatings) { categories, ratings ->
        val rated = categories.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val sgiAverageScore: StateFlow<Double> = combine(enabledSgiCategories, sgiRatings) { categories, ratings ->
        val rated = categories.mapNotNull { ratings[it.id] }.filter { it > 0 }
        if (rated.isEmpty()) 0.0 else rated.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // Keep selectedStudent null initially so student list is shown first
    }

    fun selectStudent(student: StudentEntity?) {
        selectedStudent.value = student
        if (student != null) {
            loadAssessmentForStudent(student.id)
        } else {
            clearRatingsAndComments()
        }
    }

    fun onFilterChanged() {
        // When grade, class, period or academic year changes, go back to student list view
        selectedStudent.value = null
        clearRatingsAndComments()
    }

    private fun clearRatingsAndComments() {
        holisticRatings.value = emptyMap()
        sgiRatings.value = emptyMap()
        positiveComments.value = ""
        areasForImprovement.value = ""
        generalComment.value = ""
        futureRecommendation.value = ""
    }

    fun loadAssessmentForStudent(studentId: Long) {
        val period = selectedPeriod.value
        val year = academicYear.value

        viewModelScope.launch {
            // Observe Holistic Results
            holisticRepository.getHolisticResultsForStudent(studentId, period, year)
                .take(1)
                .collect { results ->
                    val map = results.associate { it.categoryId to it.ratingStars }
                    holisticRatings.value = map
                }

            // Observe SGI Results
            holisticRepository.getSgiResultsForStudent(studentId, period, year)
                .take(1)
                .collect { results ->
                    val map = results.associate { it.sgiCategoryId to it.ratingStars }
                    sgiRatings.value = map
                }

            // Observe Teacher Comment
            holisticRepository.getTeacherCommentForStudent(studentId, period, year)
                .take(1)
                .collect { comment ->
                    if (comment != null) {
                        positiveComments.value = comment.positiveComments
                        areasForImprovement.value = comment.areasForImprovement
                        generalComment.value = comment.generalComment
                        futureRecommendation.value = comment.futureRecommendation
                    } else {
                        positiveComments.value = ""
                        areasForImprovement.value = ""
                        generalComment.value = ""
                        futureRecommendation.value = ""
                    }
                }
        }
    }

    fun updateHolisticRating(categoryId: Long, stars: Int) {
        val current = holisticRatings.value.toMutableMap()
        current[categoryId] = stars
        holisticRatings.value = current
    }

    fun updateSgiRating(sgiCategoryId: Long, stars: Int) {
        val current = sgiRatings.value.toMutableMap()
        current[sgiCategoryId] = stars
        sgiRatings.value = current
    }

    fun saveCurrentStudentAssessment(updatedBy: String) {
        val student = selectedStudent.value ?: return
        val period = selectedPeriod.value
        val year = academicYear.value
        val currentLevel = activeEducationLevel.value

        viewModelScope.launch {
            // Save Holistic Results
            val enabledHolistic = enabledHolisticCategoriesForLevel.value
            val holisticEntities = enabledHolistic.map { cat ->
                val stars = holisticRatings.value[cat.id] ?: 0
                HolisticResultEntity(
                    studentId = student.id,
                    assessmentPeriod = period,
                    academicYear = year,
                    grade = student.gradeName,
                    className = student.className,
                    categoryId = cat.id,
                    categoryName = cat.categoryName,
                    pillar = cat.pillar,
                    educationLevel = currentLevel,
                    ratingStars = stars,
                    maxStars = cat.maxStars,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = updatedBy
                )
            }
            holisticRepository.saveHolisticResults(holisticEntities)

            // Save SGI Results
            val enabledSgi = enabledSgiCategories.value
            val sgiEntities = enabledSgi.map { sgi ->
                val stars = sgiRatings.value[sgi.id] ?: 0
                SgiResultEntity(
                    studentId = student.id,
                    assessmentPeriod = period,
                    academicYear = year,
                    grade = student.gradeName,
                    className = student.className,
                    sgiCategoryId = sgi.id,
                    indicatorName = sgi.indicatorName,
                    ratingStars = stars,
                    maxStars = sgi.maxStars,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = updatedBy
                )
            }
            holisticRepository.saveSgiResults(sgiEntities)

            // Save Teacher Comment
            val commentEntity = TeacherCommentEntity(
                studentId = student.id,
                assessmentPeriod = period,
                academicYear = year,
                grade = student.gradeName,
                className = student.className,
                positiveComments = positiveComments.value,
                areasForImprovement = areasForImprovement.value,
                generalComment = generalComment.value,
                futureRecommendation = futureRecommendation.value,
                updatedAt = System.currentTimeMillis(),
                updatedBy = updatedBy
            )
            holisticRepository.saveTeacherComment(commentEntity)

            statusMessage.value = "Assessment saved successfully for ${student.name}!"
        }
    }

    // Category Management
    // Level-scoped HCM Admin Operations
    fun addHolisticCategoryForPillar(name: String, pillar: String, targetLevel: String = adminConfigLevel.value) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = adminHolisticCategories.value.size
            val newCategory = HolisticCategoryEntity(
                categoryName = name.trim(),
                description = "",
                pillar = pillar,
                educationLevel = targetLevel,
                maxStars = 5,
                isEnabled = true,
                isDefault = false,
                orderIndex = count + 1
            )
            holisticRepository.addHolisticCategory(newCategory)
            statusMessage.value = "Added '$name' to $targetLevel HCM items."
        }
    }

    fun updateHolisticCategoryName(category: HolisticCategoryEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val oldName = category.categoryName
            val allCats = allHolisticCategories.value
            val matchingItems = allCats.filter { it.categoryName == oldName && it.pillar.equals(category.pillar, ignoreCase = true) }
            if (matchingItems.isNotEmpty()) {
                matchingItems.forEach { item ->
                    holisticRepository.updateHolisticCategory(item.copy(categoryName = newName.trim()))
                }
            } else {
                holisticRepository.updateHolisticCategory(category.copy(categoryName = newName.trim()))
            }
            statusMessage.value = "Updated HCM Assessment Item."
        }
    }

    fun deleteHolisticCategoryGroup(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            val name = category.categoryName
            val pillar = category.pillar
            val matchingItems = allHolisticCategories.value.filter { it.categoryName == name && it.pillar.equals(pillar, ignoreCase = true) }
            if (matchingItems.isNotEmpty()) {
                matchingItems.forEach { item ->
                    holisticRepository.deleteHolisticCategory(item)
                }
            } else {
                holisticRepository.deleteHolisticCategory(category)
            }
            statusMessage.value = "Deleted HCM Assessment Item."
        }
    }

    fun addHolisticCategory(name: String, description: String, pillar: String, educationLevel: String, maxStars: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = adminHolisticCategories.value.size
            val category = HolisticCategoryEntity(
                categoryName = name.trim(),
                description = description.trim(),
                pillar = pillar,
                educationLevel = educationLevel,
                maxStars = maxStars,
                isEnabled = true,
                orderIndex = count + 1
            )
            holisticRepository.addHolisticCategory(category)
            statusMessage.value = "HCM Item '$name' added for $educationLevel."
        }
    }

    fun updateHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.updateHolisticCategory(category)
            statusMessage.value = "HCM Item updated."
        }
    }

    fun deleteHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.deleteHolisticCategory(category)
            statusMessage.value = "HCM Item deleted."
        }
    }

    fun duplicateHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.duplicateHolisticCategory(category)
            statusMessage.value = "Duplicated '${category.categoryName}'."
        }
    }

    fun toggleHolisticCategory(category: HolisticCategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(isEnabled = !category.isEnabled)
            holisticRepository.updateHolisticCategory(updated)
        }
    }

    fun moveHolisticCategoryOrder(category: HolisticCategoryEntity, moveUp: Boolean) {
        val currentList = adminHolisticCategories.value.sortedBy { it.orderIndex }
        val index = currentList.indexOfFirst { it.id == category.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= currentList.size) return

        val other = currentList[targetIndex]
        viewModelScope.launch {
            val tempIndex = category.orderIndex
            holisticRepository.updateHolisticCategory(category.copy(orderIndex = other.orderIndex))
            holisticRepository.updateHolisticCategory(other.copy(orderIndex = tempIndex))
        }
    }

    fun copyConfigurationBetweenLevels(sourceLevel: String, targetLevel: String) {
        viewModelScope.launch {
            holisticRepository.copyConfigurationBetweenLevels(sourceLevel, targetLevel)
            statusMessage.value = "Successfully copied HCM configuration from $sourceLevel to $targetLevel!"
        }
    }

    // SGI Management
    fun addSgiCategory(name: String, description: String, maxStars: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = sgiCategories.value.size
            val category = SgiCategoryEntity(
                indicatorName = name.trim(),
                description = description.trim(),
                maxStars = maxStars,
                isEnabled = true,
                orderIndex = count + 1
            )
            holisticRepository.addSgiCategory(category)
            statusMessage.value = "SGI Indicator '$name' added."
        }
    }

    fun updateSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.updateSgiCategory(category)
            statusMessage.value = "SGI Indicator updated."
        }
    }

    fun deleteSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.deleteSgiCategory(category)
            statusMessage.value = "Indicator deleted."
        }
    }

    fun duplicateSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            holisticRepository.duplicateSgiCategory(category)
            statusMessage.value = "Duplicated '${category.indicatorName}'."
        }
    }

    fun toggleSgiCategory(category: SgiCategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(isEnabled = !category.isEnabled)
            holisticRepository.updateSgiCategory(updated)
        }
    }

    fun moveSgiCategoryOrder(category: SgiCategoryEntity, moveUp: Boolean) {
        val currentList = sgiCategories.value.sortedBy { it.orderIndex }
        val index = currentList.indexOfFirst { it.id == category.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= currentList.size) return

        val other = currentList[targetIndex]
        viewModelScope.launch {
            val tempIndex = category.orderIndex
            holisticRepository.updateSgiCategory(category.copy(orderIndex = other.orderIndex))
            holisticRepository.updateSgiCategory(other.copy(orderIndex = tempIndex))
        }
    }

    // Assessment Period Management (Super Admin / Admin)
    fun addAssessmentPeriod(name: String, level: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val currentList = adminAssessmentPeriods.value
            val period = AssessmentPeriodEntity(
                periodName = name.trim(),
                educationLevel = level,
                periodCategory = category.trim(),
                isEnabled = true,
                orderIndex = currentList.size + 1
            )
            holisticRepository.addAssessmentPeriod(period)
            statusMessage.value = "Assessment period '$name' added."
        }
    }

    fun updateAssessmentPeriod(period: AssessmentPeriodEntity) {
        viewModelScope.launch {
            holisticRepository.updateAssessmentPeriod(period)
            statusMessage.value = "Assessment period updated."
        }
    }

    fun deleteAssessmentPeriod(period: AssessmentPeriodEntity) {
        viewModelScope.launch {
            holisticRepository.deleteAssessmentPeriod(period.id)
            statusMessage.value = "Assessment period deleted."
        }
    }

    fun toggleAssessmentPeriod(period: AssessmentPeriodEntity) {
        viewModelScope.launch {
            val updated = period.copy(isEnabled = !period.isEnabled)
            holisticRepository.updateAssessmentPeriod(updated)
        }
    }

    fun moveAssessmentPeriodOrder(period: AssessmentPeriodEntity, moveUp: Boolean) {
        val currentList = adminAssessmentPeriods.value.sortedBy { it.orderIndex }
        val index = currentList.indexOfFirst { it.id == period.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= currentList.size) return

        val other = currentList[targetIndex]
        viewModelScope.launch {
            val tempIndex = period.orderIndex
            holisticRepository.updateAssessmentPeriod(period.copy(orderIndex = other.orderIndex))
            holisticRepository.updateAssessmentPeriod(other.copy(orderIndex = tempIndex))
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}

class HolisticViewModelFactory(
    private val holisticRepository: HolisticRepository,
    private val studentRepository: StudentRepository,
    private val academicYearRepository: com.example.data.repository.AcademicYearRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HolisticViewModel::class.java)) {
            return HolisticViewModel(holisticRepository, studentRepository, academicYearRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
