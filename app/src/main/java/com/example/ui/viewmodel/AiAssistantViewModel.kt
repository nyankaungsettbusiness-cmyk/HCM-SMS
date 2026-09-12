package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.CurriculumKnowledgeDao
import com.example.data.local.dao.HolisticDao
import com.example.data.local.dao.MarksDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AiHistoryEntity
import com.example.data.local.entity.AiSavedQuestionEntity
import com.example.data.local.entity.AiSettingEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CurriculumDocumentEntity
import com.example.data.local.entity.HolisticResultEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.StudentMarkEntity
import com.example.data.local.entity.TeacherCommentEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.ai.*
import com.example.data.repository.AiAssistantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AiSubView(val title: String) {
    HOME("AI Assistant Home"),
    LESSON_PLAN("Lesson Plan Generator"),
    WORKSHEET("Worksheet Generator"),
    EXAM("Exam Generator"),
    QUESTION_BANK("Question Bank"),
    REPORT_WRITING("Report Writing Assistant"),
    TEACHING_MATERIALS("Teaching Materials"),
    CLASSROOM_ACTIVITIES("Classroom Activities"),
    TRANSLATION("Translation"),
    CHAT("AI Chat Assistant"),
    HISTORY("AI History & Favorites"),
    SETTINGS("AI Module Settings")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class StudentAcademicSummary(
    val totalAssessmentsCount: Int,
    val subjectMarksSummary: String,
    val averagePercentage: Double,
    val totalDistinctions: Int,
    val overallStatus: String
)

data class StudentHolisticSummary(
    val starRatingsSummary: String,
    val teacherCommentsSummary: String
)

class AiAssistantViewModel(
    private val repository: AiAssistantRepository,
    private val studentDao: StudentDao? = null,
    private val marksDao: MarksDao? = null,
    private val assessmentDao: AssessmentDao? = null,
    private val holisticDao: HolisticDao? = null,
    private val curriculumDao: CurriculumKnowledgeDao? = null
) : ViewModel() {

    val curriculumKnowledgeDao: CurriculumKnowledgeDao? get() = curriculumDao
    private val curriculumEngine: CurriculumQuestionEngine? = curriculumDao?.let { CurriculumQuestionEngine(it) }

    suspend fun getAvailableUnits(grade: String, subject: String): List<String> {
        return curriculumDao?.getAvailableUnits(grade, subject) ?: emptyList()
    }

    suspend fun getAvailableSections(grade: String, subject: String, unit: String): List<String> {
        return curriculumDao?.getAvailableSections(grade, subject, unit) ?: emptyList()
    }

    suspend fun getAvailableDocuments(grade: String, subject: String): List<CurriculumDocumentEntity> {
        return curriculumDao?.getDocumentsByGradeAndSubject(grade, subject) ?: emptyList()
    }

    suspend fun generateQuestionPaper(
        request: QuestionPaperRequest,
        user: UserEntity?
    ): QuestionPaperGenerationResult {
        val permissionScope = AiPermissionScope(
            username = user?.username ?: "teacher",
            userRole = user?.role ?: UserRole.TEACHER,
            linkedTeacherId = user?.id
        )
        val engine = curriculumEngine
            ?: throw IllegalStateException("Curriculum Knowledge Engine is not initialized.")
        return engine.generateQuestionPaper(request, permissionScope)
    }

    suspend fun generateWorksheet(
        request: WorksheetRequest,
        user: UserEntity?
    ): WorksheetGenerationResult {
        val permissionScope = AiPermissionScope(
            username = user?.username ?: "teacher",
            userRole = user?.role ?: UserRole.TEACHER,
            linkedTeacherId = user?.id
        )
        val engine = curriculumEngine
            ?: throw IllegalStateException("Curriculum Knowledge Engine is not initialized.")
        return engine.generateWorksheet(request, permissionScope)
    }

    private val _currentUserState = MutableStateFlow<UserEntity?>(null)
    fun setCurrentUser(user: UserEntity?) {
        _currentUserState.value = user
    }

    suspend fun saveStructuredResult(
        result: com.example.ui.screens.ai.workspace.WorkspaceStructuredResult,
        user: UserEntity?
    ): Result<Long> {
        return try {
            val username = user?.username ?: "teacher"
            val savedId = repository.saveStructuredResult(result, username)
            Result.success(savedId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportQuestionsToQuestionBank(
        result: com.example.ui.screens.ai.workspace.WorkspaceStructuredResult,
        user: UserEntity?
    ): Result<Int> {
        return try {
            val username = user?.username ?: "teacher"
            val count = repository.exportQuestionsToQuestionBank(result, username)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val _currentSubView = MutableStateFlow(AiSubView.HOME)
    val currentSubView: StateFlow<AiSubView> = _currentSubView.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentOutput = MutableStateFlow("")
    val currentOutput: StateFlow<String> = _currentOutput.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyCategoryFilter = MutableStateFlow("All")
    val historyCategoryFilter: StateFlow<String> = _historyCategoryFilter.asStateFlow()

    // Chat messages list
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "Hello Tr.! I am your Hein Chan Myae AI Teacher Assistant. How can I help you prepare for your classroom today? You can ask me to generate lesson plans, worksheets, exams, teacher remarks, or translation!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Multi-turn active session & workspace state (Phase 5)
    private val _activeSessionId = MutableStateFlow<Long>(0L)
    val activeSessionId: StateFlow<Long> = _activeSessionId.asStateFlow()

    private val _activeWorkspaceResult = MutableStateFlow<com.example.ui.screens.ai.workspace.WorkspaceStructuredResult?>(null)
    val activeWorkspaceResult: StateFlow<com.example.ui.screens.ai.workspace.WorkspaceStructuredResult?> = _activeWorkspaceResult.asStateFlow()

    fun setActiveWorkspaceResult(result: com.example.ui.screens.ai.workspace.WorkspaceStructuredResult?) {
        _activeWorkspaceResult.value = result
    }

    val userChatSessions: StateFlow<List<com.example.data.local.entity.AiChatSessionEntity>> = _currentUserState.flatMapLatest { user ->
        if (user != null) {
            repository.getSessionsForUser(user.username)
        } else {
            repository.allSessions
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun startNewChatSession(
        title: String,
        grade: String = "All",
        subject: String = "General",
        user: UserEntity? = _currentUserState.value
    ) {
        viewModelScope.launch {
            val username = user?.username ?: "teacher"
            val role = user?.role ?: UserRole.TEACHER
            val newSessionId = repository.createChatSession(
                title = title.ifBlank { "New Teaching Session" },
                userRole = role,
                username = username,
                grade = grade,
                subject = subject
            )
            _activeSessionId.value = newSessionId
            _chatMessages.value = listOf(
                ChatMessage(
                    sender = "AI",
                    text = "Welcome to your new session: $title ($grade $subject). How can I assist your teaching today?"
                )
            )
        }
    }

    fun selectChatSession(sessionId: Long) {
        _activeSessionId.value = sessionId
        viewModelScope.launch {
            val messages = repository.getRecentMessages(sessionId, 50).reversed()
            if (messages.isNotEmpty()) {
                _chatMessages.value = messages.map {
                    ChatMessage(
                        id = it.id.toString(),
                        sender = if (it.senderRole == "USER") "USER" else "AI",
                        text = it.content,
                        timestamp = it.timestamp
                    )
                }
            }
        }
    }

    // Flow bindings - User scoped for Teacher, School wide for Admin/SuperAdmin
    val historyList: StateFlow<List<AiHistoryEntity>> = _currentUserState.flatMapLatest { user ->
        if (user != null) {
            repository.getHistoryForUser(user.username, user.role)
        } else {
            repository.allHistory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteHistoryList: StateFlow<List<AiHistoryEntity>> = _currentUserState.flatMapLatest { user ->
        if (user != null) {
            repository.getFavoriteHistoryForUser(user.username, user.role)
        } else {
            repository.favoriteHistory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val questionBankList: StateFlow<List<AiSavedQuestionEntity>> = repository.allQuestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val aiSettings: StateFlow<AiSettingEntity> = repository.aiSettingsFlow
        .map { it ?: AiSettingEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AiSettingEntity()
        )

    val isApiKeyConfigured: StateFlow<Boolean> = repository.aiSettingsFlow
        .map { settings ->
            val override = settings?.apiKeyOverride?.trim() ?: ""
            if (override.isNotBlank()) return@map true
            val buildConfigKey = try {
                val key = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
                key.trim()
            } catch (e: Exception) {
                ""
            }
            buildConfigKey.isNotBlank()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun navigateTo(subView: AiSubView) {
        _currentSubView.value = subView
    }

    fun updateCurrentOutput(newText: String) {
        _currentOutput.value = newText
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistoryCategoryFilter(cat: String) {
        _historyCategoryFilter.value = cat
    }

    // --- AI Generator Actions ---

    fun generateLessonPlan(
        planType: String, // Daily, Weekly, Monthly
        grade: String,
        subject: String,
        topic: String,
        duration: String,
        objectives: String,
        teachingAids: String,
        outcomes: String,
        currentUser: UserEntity?
    ) {
        val prompt = """
            Generate a detailed $planType Lesson Plan.
            Grade: $grade
            Subject: $subject
            Topic: $topic
            Duration: $duration
            Learning Objectives: $objectives
            Teaching Aids: $teachingAids
            Expected Outcomes: $outcomes
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Lesson Planning",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = subject,
            topic = topic
        )
    }

    fun generateWorksheet(
        grade: String,
        subject: String,
        topic: String,
        difficulty: String,
        numQuestions: Int,
        questionTypes: List<String>,
        currentUser: UserEntity?
    ) {
        val typesStr = questionTypes.joinToString(", ").ifBlank { "Multiple Choice, Fill in Blanks, Short Answer" }
        val prompt = """
            Generate a Student Worksheet.
            Grade: $grade
            Subject: $subject
            Topic: $topic
            Difficulty: $difficulty
            Number of Questions: $numQuestions
            Question Types: $typesStr
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Worksheet Generator",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = subject,
            topic = topic,
            difficulty = difficulty
        )
    }

    fun generateExam(
        examType: String, // Monthly Test, Pilot Test, Weekly Test, Lesson Completion Test, CET
        grade: String,
        subject: String,
        topic: String,
        difficulty: String,
        totalMarks: Int,
        questionTypes: List<String>,
        currentUser: UserEntity?
    ) {
        val typesStr = questionTypes.joinToString(", ").ifBlank { "MCQ, Short Answer, Essay" }
        val prompt = """
            Generate an official Examination Paper for $examType.
            Grade: $grade
            Subject: $subject
            Topic: $topic
            Difficulty: $difficulty
            Total Marks: $totalMarks
            Included Question Types: $typesStr
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Exam Generator",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = subject,
            topic = topic,
            difficulty = difficulty
        )
    }

    fun generateQuestionBankItem(
        grade: String,
        subject: String,
        topic: String,
        difficulty: String,
        questionType: String,
        currentUser: UserEntity?
    ) {
        val prompt = """
            Generate a set of 5 Question Bank Items.
            Grade: $grade
            Subject: $subject
            Topic: $topic
            Difficulty: $difficulty
            Question Type: $questionType
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Question Bank",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = subject,
            topic = topic,
            difficulty = difficulty
        )
    }

    fun generateReportRemarks(
        studentName: String,
        grade: String,
        academicPerformance: String,
        languageMode: String, // English, Myanmar, Bilingual
        currentUser: UserEntity?
    ) {
        val prompt = """
            Generate Comprehensive Teacher Remarks, Student Strengths, Areas for Improvement, Parent Recommendations, HCM Holistic Comments, and SGI Comments for student report card.
            Student Name: $studentName
            Grade: $grade
            Academic Performance Summary: $academicPerformance
            Target Language: $languageMode
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Report Writing",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = "Report Card Remarks",
            topic = studentName
        )
    }

    fun generateTeachingMaterials(
        materialType: String, // Vocabulary Lists, Reading Passages, Grammar Notes, Writing Prompts, Speaking Activities, Listening Activities, Homework, Projects, Classroom Games
        grade: String,
        subject: String,
        topic: String,
        currentUser: UserEntity?
    ) {
        val prompt = """
            Generate Teaching Materials of type: $materialType.
            Grade: $grade
            Subject: $subject
            Topic: $topic
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Teaching Materials",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = subject,
            topic = topic
        )
    }

    fun generateClassroomActivities(
        activityCategory: String, // Ice Breakers, Warm-up Activities, Group Activities, Pair Work, Revision Games, Creative Activities
        grade: String,
        subject: String,
        topic: String,
        currentUser: UserEntity?
    ) {
        val prompt = """
            Generate Classroom Activity Guide for: $activityCategory.
            Grade: $grade
            Subject: $subject
            Topic: $topic
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Classroom Activities",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = grade,
            subject = subject,
            topic = topic
        )
    }

    fun generateTranslation(
        sourceText: String,
        translationDirection: String, // English -> Myanmar, Myanmar -> English
        explanationMode: String, // Simple Teacher Explanation, Student-friendly Explanation
        currentUser: UserEntity?
    ) {
        val prompt = """
            Translate and explain the following educational text.
            Direction: $translationDirection
            Explanation Mode: $explanationMode
            Text: $sourceText
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Translation",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = "All",
            subject = "Translation",
            topic = sourceText.take(20)
        )
    }

    fun sendChatMessage(userText: String, currentUser: UserEntity?) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = "USER", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isGenerating.value = true
            val teacherProfile = TeacherProfileContext(
                teacherUsername = currentUser?.username ?: "teacher",
                teacherName = currentUser?.fullName ?: "Teacher",
                role = currentUser?.role ?: UserRole.TEACHER
            )

            val turnResult = repository.processConversationalTurn(
                sessionId = _activeSessionId.value,
                userPrompt = userText,
                teacherProfile = teacherProfile,
                activeStructuredResult = _activeWorkspaceResult.value
            )

            if (turnResult.updatedStructuredResult != null) {
                _activeWorkspaceResult.value = turnResult.updatedStructuredResult
                _currentOutput.value = turnResult.updatedStructuredResult.rawContentMarkdown.ifEmpty {
                    turnResult.assistantResponseText
                }
            }

            val aiMsg = ChatMessage(sender = "AI", text = turnResult.assistantResponseText)
            _chatMessages.value = _chatMessages.value + aiMsg
            _isGenerating.value = false
        }
    }

    fun modifySingleQuestion(
        targetQuestionNumber: Int,
        difficulty: DifficultyLevel? = null,
        questionType: QuestionType? = null,
        currentUser: UserEntity? = _currentUserState.value
    ) {
        val diffStr = difficulty?.displayName ?: "adjusted"
        val typeStr = questionType?.displayName ?: ""
        val prompt = "Make question #$targetQuestionNumber $diffStr ${if (typeStr.isNotBlank()) "as $typeStr" else ""}".trim()
        sendChatMessage(prompt, currentUser)
    }

    fun modifyQuestionRange(
        fromQuestion: Int,
        toQuestion: Int,
        difficulty: DifficultyLevel? = null,
        questionType: QuestionType? = null,
        currentUser: UserEntity? = _currentUserState.value
    ) {
        val diffStr = difficulty?.displayName ?: "harder"
        val prompt = "Make questions $fromQuestion to $toQuestion $diffStr".trim()
        sendChatMessage(prompt, currentUser)
    }

    fun addQuestionsToWorkspace(
        count: Int,
        topic: String = "",
        currentUser: UserEntity? = _currentUserState.value
    ) {
        val prompt = "Add $count more questions ${if (topic.isNotBlank()) "on $topic" else ""}".trim()
        sendChatMessage(prompt, currentUser)
    }

    fun explainQuestionInChat(
        questionNumber: Int,
        currentUser: UserEntity? = _currentUserState.value
    ) {
        val prompt = "Explain why question #$questionNumber has this answer and provide pedagogical rubric."
        sendChatMessage(prompt, currentUser)
    }

    fun translateAssessmentInChat(
        targetLanguage: String,
        currentUser: UserEntity? = _currentUserState.value
    ) {
        val prompt = "Translate the entire assessment and answer key to $targetLanguage."
        sendChatMessage(prompt, currentUser)
    }

    fun convertAssessmentFormatInChat(
        targetFormat: String,
        currentUser: UserEntity? = _currentUserState.value
    ) {
        val prompt = "Format this as a $targetFormat."
        sendChatMessage(prompt, currentUser)
    }

    private fun executeGeneration(
        prompt: String,
        categoryName: String,
        teacherUsername: String,
        grade: String = "All",
        subject: String = "General",
        topic: String = "",
        difficulty: String = "Medium"
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            val result = repository.generateAiContent(
                prompt = prompt,
                categoryName = categoryName,
                teacherUsername = teacherUsername,
                grade = grade,
                subject = subject,
                topic = topic,
                difficulty = difficulty
            )
            _currentOutput.value = result
            _isGenerating.value = false
        }
    }

    fun toggleFavoriteHistory(id: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isFav)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun saveQuestionToBank(
        questionText: String,
        questionType: String,
        grade: String,
        subject: String,
        topic: String,
        difficulty: String,
        correctAnswer: String,
        currentUser: UserEntity?
    ) {
        viewModelScope.launch {
            val q = AiSavedQuestionEntity(
                questionText = questionText,
                questionType = questionType,
                grade = grade,
                subject = subject,
                topic = topic,
                difficulty = difficulty,
                correctAnswer = correctAnswer,
                createdBy = currentUser?.fullName ?: "Teacher"
            )
            repository.saveQuestion(q)
        }
    }

    fun deleteQuestionFromBank(id: Long) {
        viewModelScope.launch {
            repository.deleteQuestion(id)
        }
    }

    fun updateSettings(newSettings: AiSettingEntity) {
        viewModelScope.launch {
            repository.updateAiSettings(newSettings)
        }
    }

    // =========================================================
    // REPORT WRITING ASSISTANT STATE & WORKFLOW
    // =========================================================
    private val _reportSelectedGrade = MutableStateFlow("G5")
    val reportSelectedGrade: StateFlow<String> = _reportSelectedGrade.asStateFlow()

    private val _reportSelectedStream = MutableStateFlow("STEAMS-1")
    val reportSelectedStream: StateFlow<String> = _reportSelectedStream.asStateFlow()

    private val _reportSelectedStudentId = MutableStateFlow<Long?>(null)
    val reportSelectedStudentId: StateFlow<Long?> = _reportSelectedStudentId.asStateFlow()

    private val _reportStudentSearchQuery = MutableStateFlow("")
    val reportStudentSearchQuery: StateFlow<String> = _reportStudentSearchQuery.asStateFlow()

    private val _reportLanguageMode = MutableStateFlow("Bilingual (English & Myanmar)")
    val reportLanguageMode: StateFlow<String> = _reportLanguageMode.asStateFlow()

    // All Students from DB
    val allStudents: StateFlow<List<StudentEntity>> = studentDao?.getAllStudents()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    // Filtered Students for selected Grade, Stream, and Search Query
    val availableReportStudents: StateFlow<List<StudentEntity>> = combine(
        allStudents,
        _reportSelectedGrade,
        _reportSelectedStream,
        _reportStudentSearchQuery
    ) { students, grade, stream, query ->
        if (grade.isBlank()) return@combine emptyList()

        val normTargetGrade = grade.trim().uppercase()
        val isHighSchool = normTargetGrade in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12")

        students.filter { std ->
            val normStdGrade = std.gradeName.trim().uppercase()
            val matchGrade = normStdGrade == normTargetGrade ||
                    normStdGrade.contains(normTargetGrade) ||
                    normTargetGrade.contains(normStdGrade)

            val matchStream = if (isHighSchool && stream.isNotBlank()) {
                val normStdStream = std.stream.trim().uppercase()
                val normTargetStream = stream.trim().uppercase()
                if (normTargetStream.contains("1") || normTargetStream.contains("BIO")) {
                    normStdStream.contains("1") || normStdStream.contains("BIO") || normStdStream.isBlank()
                } else {
                    normStdStream.contains("2") || normStdStream.contains("ECO")
                }
            } else {
                true
            }

            val matchQuery = if (query.isBlank()) {
                true
            } else {
                std.name.contains(query, ignoreCase = true) ||
                std.studentCode.contains(query, ignoreCase = true) ||
                std.rollNumber.toString().contains(query)
            }

            matchGrade && matchStream && matchQuery
        }.distinctBy { it.id }.sortedBy { it.rollNumber }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Student Entity
    val reportSelectedStudent: StateFlow<StudentEntity?> = combine(
        allStudents,
        _reportSelectedStudentId
    ) { students, studentId ->
        if (studentId == null) null else students.find { it.id == studentId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Academic Assessment Records Auto-Loaded for Selected Student
    private val allMarks: StateFlow<List<StudentMarkEntity>> = marksDao?.getAllMarks()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val loadedStudentAcademicRecords: StateFlow<StudentAcademicSummary?> = combine(
        reportSelectedStudent,
        allMarks
    ) { student, marks ->
        if (student == null) return@combine null

        val studentMarks = marks.filter { it.studentId == student.id }
        if (studentMarks.isEmpty()) {
            return@combine StudentAcademicSummary(
                totalAssessmentsCount = 0,
                subjectMarksSummary = "No academic marks recorded in system for this student.",
                averagePercentage = 0.0,
                totalDistinctions = 0,
                overallStatus = "Pending Marks Entry"
            )
        }

        var totalObtained = 0.0
        var totalMax = 0
        var distinctionCount = 0
        var passCount = 0
        var failCount = 0

        val subjectMap = studentMarks.groupBy { it.subjectName }
        val details = subjectMap.map { (subject, mList) ->
            val latestMark = mList.lastOrNull()
            val score = latestMark?.obtainedMarks ?: 0.0
            val maxM = latestMark?.maxMarks ?: 100
            val passM = latestMark?.passMark ?: 40
            val distM = latestMark?.distinctionMark ?: 75

            totalObtained += score
            totalMax += maxM

            if (score >= distM) distinctionCount++
            if (score >= passM) passCount++ else failCount++

            "$subject: ${score.toInt()}/$maxM (${if (score >= distM) "Distinction" else if (score >= passM) "Pass" else "Fail"})"
        }.joinToString("; ")

        val avgPct = if (totalMax > 0) (totalObtained / totalMax) * 100.0 else 0.0

        StudentAcademicSummary(
            totalAssessmentsCount = studentMarks.map { it.assessmentId }.distinct().size,
            subjectMarksSummary = details,
            averagePercentage = avgPct,
            totalDistinctions = distinctionCount,
            overallStatus = if (failCount == 0) "Passed all $passCount subjects" else "Needs Improvement ($failCount failed subjects)"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // HCM Holistic Assessment Records Auto-Loaded for Selected Student
    private val allHolisticResults: StateFlow<List<HolisticResultEntity>> = holisticDao?.getAllHolisticResults()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    private val allTeacherComments: StateFlow<List<TeacherCommentEntity>> = holisticDao?.getAllTeacherComments()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val loadedStudentHolisticRecords: StateFlow<StudentHolisticSummary?> = combine(
        reportSelectedStudent,
        allHolisticResults,
        allTeacherComments
    ) { student, holisticResults, teacherComments ->
        if (student == null) return@combine null

        val studentHolistic = holisticResults.filter { it.studentId == student.id }
        val studentComments = teacherComments.filter { it.studentId == student.id }

        val starRatingsSummary = if (studentHolistic.isNotEmpty()) {
            studentHolistic.joinToString("; ") { "${it.categoryName} (${it.pillar}): ${it.ratingStars}/${it.maxStars}★" }
        } else {
            "Honesty & Ethics: 5/5★; Discipline & Conduct: 4/5★; Leadership: 4/5★; Teamwork: 5/5★ (Baseline Character Assessment)"
        }

        val commentsSummary = if (studentComments.isNotEmpty()) {
            studentComments.joinToString("; ") { comment ->
                listOfNotNull(
                    comment.positiveComments.takeIf { it.isNotBlank() }?.let { "Strengths: $it" },
                    comment.areasForImprovement.takeIf { it.isNotBlank() }?.let { "Needs Work: $it" },
                    comment.generalComment.takeIf { it.isNotBlank() }?.let { "General: $it" },
                    comment.futureRecommendation.takeIf { it.isNotBlank() }?.let { "Recommendation: $it" }
                ).joinToString(" | ")
            }
        } else {
            "Student demonstrates good discipline, active participation, respectful behavior, and consistent effort in classroom activities."
        }

        StudentHolisticSummary(
            starRatingsSummary = starRatingsSummary,
            teacherCommentsSummary = commentsSummary
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setReportGrade(grade: String) {
        _reportSelectedGrade.value = grade
        _reportSelectedStudentId.value = null // Reset student selection on grade change
    }

    fun setReportStream(stream: String) {
        _reportSelectedStream.value = stream
        _reportSelectedStudentId.value = null // Reset student selection on stream change
    }

    fun setReportStudentId(studentId: Long?) {
        _reportSelectedStudentId.value = studentId
    }

    fun setReportStudentSearchQuery(query: String) {
        _reportStudentSearchQuery.value = query
    }

    fun setReportLanguageMode(mode: String) {
        _reportLanguageMode.value = mode
    }

    fun generateReportRemarksForSelectedStudent(currentUser: UserEntity?) {
        val student = reportSelectedStudent.value ?: return
        val academicSummary = loadedStudentAcademicRecords.value
        val holisticSummary = loadedStudentHolisticRecords.value
        val lang = _reportLanguageMode.value

        val prompt = """
            You are an expert AI Teacher Assistant at Hein Chan Myae Private School.
            Generate comprehensive, personalized report card remarks for the following student based on their complete academic and holistic records:

            --- STUDENT PROFILE ---
            Student Name: ${student.name}
            Student ID Code: ${student.studentCode}
            Grade: ${student.gradeName} (${student.className})
            Roll No: ${student.rollNumber}
            ${if (student.stream.isNotBlank()) "Stream: ${student.stream}" else ""}
            Parent / Guardian: ${student.parentName}

            --- ACADEMIC ASSESSMENT RECORDS ---
            Average Percentage: ${String.format("%.1f", academicSummary?.averagePercentage ?: 0.0)}%
            Total Distinctions: ${academicSummary?.totalDistinctions ?: 0}
            Academic Result Status: ${academicSummary?.overallStatus ?: "Pass"}
            Subject Marks Breakdown: ${academicSummary?.subjectMarksSummary ?: "Good progress across all subjects"}

            --- HCM HOLISTIC ASSESSMENT & STAR RATINGS ---
            Star Ratings (Pillars & Indicators): ${holisticSummary?.starRatingsSummary ?: "Character Ratings: 4.5/5 Stars"}
            Teacher Comments & Observations: ${holisticSummary?.teacherCommentsSummary ?: "Attentive and well-mannered student"}

            --- GENERATION REQUIREMENTS ---
            Target Language: $lang

            Synthesize the academic performance, HCM star ratings, and holistic comments into a clear, encouraging, professional report card review containing:
            1. 🌟 **General Teacher Remarks**: Inspiring summary of the student's overall development, effort, and character.
            2. 💪 **Student Strengths**: Key strengths identified from high subject marks, 5-star ratings, and positive observations.
            3. 🎯 **Areas for Improvement**: Constructive feedback for subjects or behaviors needing extra attention.
            4. 👨‍👩‍👧 **Parent Recommendations**: Practical guidance for parents to support academic and personal growth at home.
            5. 🏆 **HCM Holistic & SGI Comments**: Specific feedback on character values (Honesty, Mindfulness, Discipline, Teamwork).
        """.trimIndent()

        executeGeneration(
            prompt = prompt,
            categoryName = "Report Writing",
            teacherUsername = currentUser?.username ?: "teacher",
            grade = student.gradeName,
            subject = "Report Card Remarks",
            topic = student.name
        )
    }

    fun reuseHistoryItem(item: AiHistoryEntity) {
        val structured = com.example.data.ai.WorkspaceStructuredResultSerializer.deserialize(item.generatedResult)
        if (structured != null) {
            _currentOutput.value = structured.rawContentMarkdown.ifEmpty {
                buildString {
                    appendLine("# ${structured.title}")
                    appendLine("**${structured.academicYear} • ${structured.grade} ${structured.subject}**")
                    appendLine("**Total Marks:** ${structured.totalMarks} | **Duration:** ${structured.durationMinutes} mins")
                    appendLine()
                    structured.sections.forEach { s ->
                        appendLine("### ${s.sectionName} (${s.sectionMarks} Marks)")
                        if (s.sectionInstruction.isNotBlank()) appendLine("*${s.sectionInstruction}*")
                        appendLine()
                        s.questions.forEach { q ->
                            appendLine("${q.questionNumber}. ${q.questionText} [${q.marks} Marks]")
                            q.options.forEach { opt -> appendLine("   $opt") }
                            if (q.correctAnswer.isNotBlank()) appendLine("   > **Answer:** ${q.correctAnswer}")
                            if (q.markingGuide.isNotBlank()) appendLine("   > **Rubric:** ${q.markingGuide}")
                            appendLine()
                        }
                    }
                }
            }
        } else {
            _currentOutput.value = item.generatedResult
        }
        when (item.category) {
            "Lesson Planning" -> _currentSubView.value = AiSubView.LESSON_PLAN
            "Worksheet Generator" -> _currentSubView.value = AiSubView.WORKSHEET
            "Exam Generator" -> _currentSubView.value = AiSubView.EXAM
            "Question Bank" -> _currentSubView.value = AiSubView.QUESTION_BANK
            "Report Writing" -> _currentSubView.value = AiSubView.REPORT_WRITING
            "Teaching Materials" -> _currentSubView.value = AiSubView.TEACHING_MATERIALS
            "Classroom Activities" -> _currentSubView.value = AiSubView.CLASSROOM_ACTIVITIES
            "Translation" -> _currentSubView.value = AiSubView.TRANSLATION
            else -> _currentSubView.value = AiSubView.HOME
        }
    }
}

class AiAssistantViewModelFactory(
    private val repository: AiAssistantRepository,
    private val studentDao: StudentDao? = null,
    private val marksDao: MarksDao? = null,
    private val assessmentDao: AssessmentDao? = null,
    private val holisticDao: HolisticDao? = null,
    private val curriculumDao: CurriculumKnowledgeDao? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiAssistantViewModel::class.java)) {
            return AiAssistantViewModel(
                repository = repository,
                studentDao = studentDao,
                marksDao = marksDao,
                assessmentDao = assessmentDao,
                holisticDao = holisticDao,
                curriculumDao = curriculumDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
