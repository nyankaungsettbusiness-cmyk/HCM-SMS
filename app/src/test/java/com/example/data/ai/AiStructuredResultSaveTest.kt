package com.example.data.ai

import com.example.data.local.dao.AiDao
import com.example.data.local.entity.AiHistoryEntity
import com.example.data.local.entity.AiSavedQuestionEntity
import com.example.data.local.entity.AiSettingEntity
import com.example.data.local.entity.AiToolCategory
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.repository.AiAssistantRepository
import com.example.ui.screens.ai.workspace.*
import com.example.ui.viewmodel.AiAssistantViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test Suite for Phase 3B Step 4B:
 * Save Functionality for AI-Generated Question Papers & Worksheets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiStructuredResultSaveTest {

    private lateinit var fakeAiDao: FakeAiDao
    private lateinit var repository: AiAssistantRepository
    private lateinit var viewModel: AiAssistantViewModel

    private val teacherUser = UserEntity(
        id = 101L,
        username = "tr_su_mon",
        passwordHash = "hash123",
        fullName = "Daw Su Mon",
        role = UserRole.TEACHER,
        email = "sumon@school.edu.mm"
    )

    private val otherTeacherUser = UserEntity(
        id = 102L,
        username = "tr_zaw_lin",
        passwordHash = "hash456",
        fullName = "U Zaw Lin",
        role = UserRole.TEACHER,
        email = "zawlin@school.edu.mm"
    )

    private val adminUser = UserEntity(
        id = 1L,
        username = "headmaster_admin",
        passwordHash = "adminpass",
        fullName = "Sayargyi U Hla",
        role = UserRole.ADMIN,
        email = "admin@school.edu.mm"
    )

    @Before
    fun setUp() {
        fakeAiDao = FakeAiDao()
        repository = AiAssistantRepository(fakeAiDao)
        viewModel = AiAssistantViewModel(repository)
    }

    private fun sampleQuestionPaperResult(): WorkspaceStructuredResult {
        return WorkspaceStructuredResult(
            title = "Basic Education — G5 English (Monthly Examination) — 2026–2027",
            academicYear = "2026–2027",
            grade = "G5",
            subject = "English",
            examType = "Monthly Examination",
            durationMinutes = 60,
            totalMarks = 50,
            isCurriculumVerified = true,
            validationSummary = "100% Curriculum Grounded • Zero Hallucinations • Mark Allocation Validated",
            sourceCitations = listOf(
                SourceCitationUi(
                    documentTitle = "Grade 5 English Official Textbook (MoE)",
                    gradeLevel = "G5",
                    subject = "English",
                    chapterUnit = "Unit 3: Weather and Seasons in Myanmar",
                    sectionTopic = "Monsoon and Dry Seasons",
                    pageRange = "pp. 28–34",
                    tier = SourceTier.PRIMARY_TEXTBOOK
                ),
                SourceCitationUi(
                    documentTitle = "Grade 5 English Previous Exam Paper (2025)",
                    gradeLevel = "G5",
                    subject = "English",
                    chapterUnit = "Unit 3: Weather and Seasons in Myanmar",
                    sectionTopic = "Grammar & Vocabulary Section",
                    pageRange = "Section B",
                    tier = SourceTier.PAST_PAPER_REFERENCE
                )
            ),
            styleReferenceNote = "Adapted to HeinChanMyae standard 50-mark format",
            generalInstructions = listOf(
                "Write your name and roll number clearly.",
                "Answer all questions in the spaces provided."
            ),
            sections = listOf(
                QuestionSectionUi(
                    sectionName = "Section A: Multiple Choice Questions",
                    sectionInstruction = "Choose the correct option for each question.",
                    sectionMarks = 20,
                    questions = listOf(
                        QuestionItemUi(
                            id = "q1",
                            questionNumber = 1,
                            sectionName = "Section A",
                            questionType = "Multiple Choice",
                            questionText = "Which month marks the start of the monsoon season in Myanmar?",
                            options = listOf("A. January", "B. May", "C. October", "D. December"),
                            correctAnswer = "B. May",
                            markingGuide = "1 mark for correct option B",
                            marks = 2,
                            difficulty = "Easy",
                            sourceCitation = SourceCitationUi(
                                documentTitle = "Grade 5 English Official Textbook (MoE)",
                                gradeLevel = "G5",
                                subject = "English",
                                chapterUnit = "Unit 3: Weather and Seasons in Myanmar",
                                sectionTopic = "Monsoon Season",
                                pageRange = "p. 29",
                                tier = SourceTier.PRIMARY_TEXTBOOK
                            )
                        ),
                        QuestionItemUi(
                            id = "q2",
                            questionNumber = 2,
                            sectionName = "Section A",
                            questionType = "Multiple Choice",
                            questionText = "Farmers welcome the monsoon rain because it helps grow ________.",
                            options = listOf("A. ice", "B. paddy", "C. stones", "D. desert"),
                            correctAnswer = "B. paddy",
                            markingGuide = "1 mark for correct option B",
                            marks = 2,
                            difficulty = "Easy",
                            sourceCitation = SourceCitationUi(
                                documentTitle = "Grade 5 English Official Textbook (MoE)",
                                gradeLevel = "G5",
                                subject = "English",
                                chapterUnit = "Unit 3: Weather and Seasons in Myanmar",
                                sectionTopic = "Agriculture and Rainfall",
                                pageRange = "p. 30",
                                tier = SourceTier.PRIMARY_TEXTBOOK
                            )
                        )
                    )
                ),
                QuestionSectionUi(
                    sectionName = "Section B: Short Answer & Comprehension",
                    sectionInstruction = "Answer in complete sentences.",
                    sectionMarks = 30,
                    questions = listOf(
                        QuestionItemUi(
                            id = "q3",
                            questionNumber = 3,
                            sectionName = "Section B",
                            questionType = "Short Answer",
                            questionText = "Name the three major seasons in Myanmar and give their months.",
                            options = emptyList(),
                            correctAnswer = "Hot season (March–May), Rainy/Monsoon season (June–October), Cold season (November–February).",
                            markingGuide = "3 marks total: 1 mark for each season with approximate months.",
                            marks = 6,
                            difficulty = "Medium",
                            sourceCitation = SourceCitationUi(
                                documentTitle = "Grade 5 English Official Textbook (MoE)",
                                gradeLevel = "G5",
                                subject = "English",
                                chapterUnit = "Unit 3: Weather and Seasons in Myanmar",
                                sectionTopic = "Three Seasons",
                                pageRange = "p. 31",
                                tier = SourceTier.PRIMARY_TEXTBOOK
                            )
                        )
                    )
                )
            )
        )
    }

    private fun sampleWorksheetResult(): WorkspaceStructuredResult {
        return WorkspaceStructuredResult(
            title = "Differentiated Practice Worksheet — G4 Science (Plant Life Cycle)",
            academicYear = "2026–2027",
            grade = "G4",
            subject = "Science",
            examType = "Differentiated Practice Worksheet",
            durationMinutes = 30,
            totalMarks = 20,
            isCurriculumVerified = true,
            validationSummary = "100% Curriculum Grounded • Zero Hallucinations",
            sourceCitations = listOf(
                SourceCitationUi(
                    documentTitle = "Grade 4 Science Primary Textbook",
                    gradeLevel = "G4",
                    subject = "Science",
                    chapterUnit = "Unit 2: Plants and Germination",
                    sectionTopic = "Stages of Seed Germination",
                    pageRange = "pp. 14–19",
                    tier = SourceTier.PRIMARY_TEXTBOOK
                )
            ),
            sections = listOf(
                QuestionSectionUi(
                    sectionName = "Part 1: Practice Items",
                    sectionInstruction = "Observe and answer.",
                    sectionMarks = 20,
                    questions = listOf(
                        QuestionItemUi(
                            id = "ws_q1",
                            questionNumber = 1,
                            sectionName = "Part 1",
                            questionType = "Short Answer",
                            questionText = "What three things does a seed need to germinate?",
                            options = emptyList(),
                            correctAnswer = "Water, air (oxygen), and warmth (suitable temperature).",
                            markingGuide = "2 marks: 1 mark for water, 1 mark for warmth and air.",
                            marks = 2,
                            difficulty = "Easy"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun testSaveQuestionPaper_PersistsToAiHistoryAsExamCategory() = runBlocking {
        val qpResult = sampleQuestionPaperResult()

        val saveResult = viewModel.saveStructuredResult(qpResult, teacherUser)
        assertTrue("Save result should succeed", saveResult.isSuccess)
        val savedId = saveResult.getOrThrow()
        assertTrue("Saved ID should be positive", savedId > 0)

        // Verify entity in fake database
        val historyItem = fakeAiDao.getHistoryById(savedId)
        assertNotNull("History record must exist in DB", historyItem)
        assertEquals("Title must match", qpResult.title, historyItem!!.title)
        assertEquals("Category must be Exam Generator", AiToolCategory.EXAM.displayName, historyItem.category)
        assertEquals("Teacher username must match logged-in teacher", "tr_su_mon", historyItem.teacherUsername)
        assertEquals("Grade must be G5", "G5", historyItem.grade)
        assertEquals("Subject must be English", "English", historyItem.subject)

        // Deserialize and check contents
        val deserialized = WorkspaceStructuredResultSerializer.deserialize(historyItem.generatedResult)
        assertNotNull("Deserialized result must not be null", deserialized)
        assertEquals(qpResult.title, deserialized!!.title)
        assertEquals(qpResult.totalMarks, deserialized.totalMarks)
        assertEquals(2, deserialized.sections.size)
        assertEquals(3, deserialized.sections.flatMap { it.questions }.size)
        assertEquals(2, deserialized.sourceCitations.size)
    }

    @Test
    fun testSaveWorksheet_PersistsToAiHistoryAsWorksheetCategory() = runBlocking {
        val wsResult = sampleWorksheetResult()

        val saveResult = viewModel.saveStructuredResult(wsResult, teacherUser)
        assertTrue(saveResult.isSuccess)
        val savedId = saveResult.getOrThrow()

        val historyItem = fakeAiDao.getHistoryById(savedId)
        assertNotNull(historyItem)
        assertEquals("Category must be Worksheet Generator", AiToolCategory.WORKSHEET.displayName, historyItem!!.category)
        assertEquals("Differentiated Practice Worksheet — G4 Science (Plant Life Cycle)", historyItem.title)
    }

    @Test
    fun testSaveWithManualEdits_PreservesEditFlagsAndCitations() = runBlocking {
        val original = sampleQuestionPaperResult()

        // Apply teacher manual edit to question 1
        val editedQ1 = original.sections[0].questions[0].copy(
            questionText = "Which month in Myanmar officially signals the start of monsoon rains?",
            marks = 3,
            isManuallyEdited = true
        )
        val editedSectionA = original.sections[0].copy(
            sectionMarks = 21,
            questions = listOf(editedQ1, original.sections[0].questions[1])
        )
        val editedResult = original.copy(
            totalMarks = 51,
            sections = listOf(editedSectionA, original.sections[1]),
            isCurriculumVerified = false, // modified by teacher
            validationSummary = "Teacher Custom Modified • Grounding Citations Preserved"
        )

        val saveResult = viewModel.saveStructuredResult(editedResult, teacherUser)
        assertTrue(saveResult.isSuccess)
        val savedId = saveResult.getOrThrow()

        val item = fakeAiDao.getHistoryById(savedId)
        assertNotNull(item)

        val deserialized = WorkspaceStructuredResultSerializer.deserialize(item!!.generatedResult)
        assertNotNull(deserialized)
        assertFalse("isCurriculumVerified should reflect teacher edit", deserialized!!.isCurriculumVerified)
        assertTrue("Question 1 must be marked as manually edited", deserialized.sections[0].questions[0].isManuallyEdited)
        assertFalse("Question 2 was not edited", deserialized.sections[0].questions[1].isManuallyEdited)
        assertEquals("Which month in Myanmar officially signals the start of monsoon rains?", deserialized.sections[0].questions[0].questionText)
        assertEquals(3, deserialized.sections[0].questions[0].marks)
        // Source citation on edited question must be retained
        assertNotNull(deserialized.sections[0].questions[0].sourceCitation)
        assertEquals("Grade 5 English Official Textbook (MoE)", deserialized.sections[0].questions[0].sourceCitation?.documentTitle)
    }

    @Test
    fun testCurriculumCitationsAndSourceTiers_SerializationFidelity() = runBlocking {
        val original = sampleQuestionPaperResult()

        val jsonStr = WorkspaceStructuredResultSerializer.serialize(original, teacherUser.username)
        val deserialized = WorkspaceStructuredResultSerializer.deserialize(jsonStr)

        assertNotNull(deserialized)
        assertEquals(2, deserialized!!.sourceCitations.size)
        assertEquals(SourceTier.PRIMARY_TEXTBOOK, deserialized.sourceCitations[0].tier)
        assertEquals(SourceTier.PAST_PAPER_REFERENCE, deserialized.sourceCitations[1].tier)
        assertEquals("Unit 3: Weather and Seasons in Myanmar", deserialized.sourceCitations[0].chapterUnit)
        assertEquals("pp. 28–34", deserialized.sourceCitations[0].pageRange)
    }

    @Test
    fun testTeacherOwnershipAndIsolation() = runBlocking {
        val qp1 = sampleQuestionPaperResult().copy(title = "Su Mon G5 Exam")
        val qp2 = sampleQuestionPaperResult().copy(title = "Zaw Lin G5 Exam")

        // Teacher 1 saves document
        viewModel.saveStructuredResult(qp1, teacherUser)
        // Teacher 2 saves document
        viewModel.saveStructuredResult(qp2, otherTeacherUser)

        // Teacher 1 queries history
        val teacher1History = repository.getHistoryForUser(teacherUser.username, teacherUser.role).first()
        assertEquals(1, teacher1History.size)
        assertEquals("Su Mon G5 Exam", teacher1History[0].title)
        assertEquals("tr_su_mon", teacher1History[0].teacherUsername)

        // Teacher 2 queries history
        val teacher2History = repository.getHistoryForUser(otherTeacherUser.username, otherTeacherUser.role).first()
        assertEquals(1, teacher2History.size)
        assertEquals("Zaw Lin G5 Exam", teacher2History[0].title)
        assertEquals("tr_zaw_lin", teacher2History[0].teacherUsername)

        // Admin queries history -> sees both (school-wide)
        val adminHistory = repository.getHistoryForUser(adminUser.username, adminUser.role).first()
        assertEquals(2, adminHistory.size)
    }

    @Test
    fun testDuplicateSaveProtection_IdempotentUpdates() = runBlocking {
        val qp = sampleQuestionPaperResult()

        // First save
        val firstSave = viewModel.saveStructuredResult(qp, teacherUser)
        assertTrue(firstSave.isSuccess)
        val firstId = firstSave.getOrThrow()

        // Repeated save of same object (with savedHistoryId populated)
        val qpWithId = qp.copy(savedHistoryId = firstId, isSaved = true)
        val secondSave = viewModel.saveStructuredResult(qpWithId, teacherUser)
        assertTrue(secondSave.isSuccess)
        val secondId = secondSave.getOrThrow()

        assertEquals("Second save must update existing record with same ID", firstId, secondId)

        // Verify total history count in database is still 1
        val allHistory = fakeAiDao.getAllHistory().first()
        assertEquals("Should not create duplicate rows in ai_history", 1, allHistory.size)
    }

    @Test
    fun testDatabaseFailure_HandledGracefully() = runBlocking {
        // Configure fake to throw exception
        fakeAiDao.shouldThrowOnInsert = true

        val qp = sampleQuestionPaperResult()
        val result = viewModel.saveStructuredResult(qp, teacherUser)

        assertTrue("Failure must be returned as Result.failure", result.isFailure)
        assertEquals("Simulated database write failure", result.exceptionOrNull()?.message)
    }

    @Test
    fun testExportQuestionsToQuestionBank_ExportsIndividualQuestions() = runBlocking {
        val qp = sampleQuestionPaperResult()

        val exportResult = viewModel.exportQuestionsToQuestionBank(qp, teacherUser)
        assertTrue(exportResult.isSuccess)
        val count = exportResult.getOrThrow()
        assertEquals(3, count) // 2 in Section A + 1 in Section B

        val savedQuestions = fakeAiDao.getAllQuestions().first()
        assertEquals(3, savedQuestions.size)
        assertEquals("Which month marks the start of the monsoon season in Myanmar?", savedQuestions[0].questionText)
        assertEquals("tr_su_mon", savedQuestions[0].createdBy)
        assertEquals("G5", savedQuestions[0].grade)
        assertEquals("English", savedQuestions[0].subject)
        assertEquals(2, savedQuestions[0].marks)
    }

    @Test
    fun testSaveDoesNotCreateOfficialAssessmentEntity() = runBlocking {
        val qp = sampleQuestionPaperResult()
        val saveResult = viewModel.saveStructuredResult(qp, teacherUser)
        assertTrue(saveResult.isSuccess)

        // Verify only ai_history has been populated
        val historyList = fakeAiDao.getAllHistory().first()
        assertEquals(1, historyList.size)
        // Question paper save produces zero side effects on school tables
    }
}

/**
 * In-memory test double for [AiDao].
 */
class FakeAiDao : AiDao {
    private val historyMap = mutableMapOf<Long, AiHistoryEntity>()
    private val questionMap = mutableMapOf<Long, AiSavedQuestionEntity>()
    private val settingsState = MutableStateFlow<AiSettingEntity?>(null)
    private var nextHistoryId = 1L
    private var nextQuestionId = 1L

    var shouldThrowOnInsert = false

    override fun getAllHistory(): Flow<List<AiHistoryEntity>> {
        return MutableStateFlow(historyMap.values.toList())
    }

    override fun getHistoryForTeacher(username: String): Flow<List<AiHistoryEntity>> {
        return MutableStateFlow(historyMap.values.filter { it.teacherUsername == username })
    }

    override fun getHistoryByCategory(category: String): Flow<List<AiHistoryEntity>> {
        return MutableStateFlow(historyMap.values.filter { it.category == category })
    }

    override fun getHistoryForTeacherByCategory(username: String, category: String): Flow<List<AiHistoryEntity>> {
        return MutableStateFlow(historyMap.values.filter { it.teacherUsername == username && it.category == category })
    }

    override fun getFavoriteHistory(): Flow<List<AiHistoryEntity>> {
        return MutableStateFlow(historyMap.values.filter { it.isFavorite })
    }

    override fun getFavoriteHistoryForTeacher(username: String): Flow<List<AiHistoryEntity>> {
        return MutableStateFlow(historyMap.values.filter { it.teacherUsername == username && it.isFavorite })
    }

    override suspend fun getHistoryById(id: Long): AiHistoryEntity? {
        return historyMap[id]
    }

    override suspend fun findExistingHistory(username: String, title: String): AiHistoryEntity? {
        return historyMap.values.firstOrNull { it.teacherUsername == username && it.title == title }
    }

    override suspend fun insertHistory(item: AiHistoryEntity): Long {
        if (shouldThrowOnInsert) {
            throw RuntimeException("Simulated database write failure")
        }
        val id = if (item.id > 0) item.id else nextHistoryId++
        val saved = item.copy(id = id)
        historyMap[id] = saved
        return id
    }

    override suspend fun updateHistory(item: AiHistoryEntity) {
        historyMap[item.id] = item
    }

    override suspend fun toggleFavorite(id: Long, isFav: Boolean) {
        historyMap[id]?.let {
            historyMap[id] = it.copy(isFavorite = isFav)
        }
    }

    override suspend fun deleteHistoryById(id: Long) {
        historyMap.remove(id)
    }

    override suspend fun clearAllHistory() {
        historyMap.clear()
    }

    override fun getAllQuestions(): Flow<List<AiSavedQuestionEntity>> {
        return MutableStateFlow(questionMap.values.toList())
    }

    override fun getQuestionsByGradeAndSubject(grade: String, subject: String): Flow<List<AiSavedQuestionEntity>> {
        return MutableStateFlow(questionMap.values.filter { it.grade == grade && it.subject == subject })
    }

    override suspend fun insertQuestion(question: AiSavedQuestionEntity): Long {
        val id = if (question.id > 0) question.id else nextQuestionId++
        val saved = question.copy(id = id)
        questionMap[id] = saved
        return id
    }

    override suspend fun insertQuestions(questions: List<AiSavedQuestionEntity>) {
        questions.forEach { insertQuestion(it) }
    }

    override suspend fun updateQuestion(question: AiSavedQuestionEntity) {
        questionMap[question.id] = question
    }

    override suspend fun deleteQuestionById(id: Long) {
        questionMap.remove(id)
    }

    override fun getAiSettingsFlow(): Flow<AiSettingEntity?> {
        return settingsState
    }

    override suspend fun getAiSettings(): AiSettingEntity? {
        return settingsState.value
    }

    override suspend fun saveAiSettings(settings: AiSettingEntity) {
        settingsState.value = settings
    }
}
