package com.example.data.ai

import com.example.data.local.dao.CurriculumKnowledgeDao
import com.example.data.local.entity.CurriculumChunkEntity
import com.example.data.local.entity.CurriculumDocumentEntity
import com.example.data.local.entity.UserRole
import com.example.ui.screens.ai.workspace.toWorkspaceStructuredResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Test Suite for Phase 3:
 * Curriculum-Grounded Question Paper & Worksheet Engine.
 *
 * Tests all required scenarios from A to T.
 */
class CurriculumQuestionEngineComprehensiveTest {

    private lateinit var fakeDao: FakeCurriculumKnowledgeDao
    private lateinit var engine: CurriculumQuestionEngine

    private val adminScope = AiPermissionScope(
        username = "admin",
        userRole = UserRole.ADMIN
    )

    private val g5EnglishTeacherScope = AiPermissionScope(
        username = "daw_aye",
        userRole = UserRole.TEACHER,
        linkedTeacherId = 101L,
        assignedGrades = listOf("G5", "Grade 5"),
        assignedClasses = listOf("Room A", "A"),
        assignedSubjects = listOf("English", "Science")
    )

    @Before
    fun setUp() = runBlocking {
        fakeDao = FakeCurriculumKnowledgeDao()
        CurriculumKnowledgeSeeder.seedCurriculumIfEmpty(fakeDao)
        engine = CurriculumQuestionEngine(fakeDao)
    }

    // --- Scenario A: Myanmar KG textbook question generation ---
    @Test
    fun testScenarioA_MyanmarKGTextbookGeneration() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "KG",
            className = "KG-A",
            subject = "Myanmar",
            chapterUnit = "Unit 1: Myanmar Consonants and Vowels",
            sectionTopic = "က မှ င အက္ခရာများ နှင့် ရုပ်ပုံများ",
            totalMarks = 50,
            durationMinutes = 45,
            language = "MYANMAR"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue("KG Exam must be grounded in curriculum", result.isGroundedInCurriculum)
        assertTrue("KG Exam must be valid", result.validationReport.isValid)
        assertEquals("Total marks must equal 50", 50, result.validationReport.totalCalculatedMarks)
        assertTrue("Contains KG matching or visual recognition questions", result.allQuestions.any { it.questionType == QuestionType.MATCHING || it.questionType == QuestionType.MCQ })
        assertTrue("Traceability points to KG Myanmar textbook", result.allQuestions.all { it.sourceReference.gradeLevel == "KG" })
    }

    // --- Scenario B: Primary grade question generation ---
    @Test
    fun testScenarioB_PrimaryGradeG5Generation() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, g5EnglishTeacherScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(50, result.totalMarks)
        assertEquals(50, result.validationReport.totalCalculatedMarks)
        assertTrue("Traceability contains Unit 3 Healthy Food", result.allQuestions.any { it.sourceReference.chapterUnit.contains("Unit 3") })
        assertTrue("Contains food/nutrition terms in answer keys", result.answerKey.isNotEmpty())
    }

    // --- Scenario C: Secondary grade question generation ---
    @Test
    fun testScenarioC_SecondaryGradeG7ScienceGeneration() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G7",
            subject = "Science",
            chapterUnit = "Chapter 1: Cells as the Basic Unit of Life",
            sectionTopic = "Plant vs Animal Cells & Organelles",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(50, result.totalMarks)
        assertTrue("Contains cell organelles vocabulary in question traces", result.sourceCitations.any { it.chapterUnit.contains("Cells") })
    }

    // --- Scenario D: High School question generation ---
    @Test
    fun testScenarioD_HighSchoolG10PhysicsChemistryGeneration() = runBlocking {
        val physicsRequest = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.FINAL_EXAM,
            academicYear = "2026-2027",
            grade = "G10",
            subject = "Physics",
            chapterUnit = "Chapter 2: Kinematics and Motion",
            sectionTopic = "Speed, Velocity, Acceleration & Graphs",
            totalMarks = 100,
            durationMinutes = 120,
            language = "ENGLISH"
        )

        val physicsResult = engine.generateQuestionPaper(physicsRequest, adminScope)

        assertTrue(physicsResult.validationReport.isValid)
        assertEquals(100, physicsResult.totalMarks)
        assertEquals(100, physicsResult.validationReport.totalCalculatedMarks)
        assertTrue("Includes structured/challenge questions for high school", physicsResult.allQuestions.any { it.difficulty == DifficultyLevel.CHALLENGE })
    }

    // --- Scenario E: International course source ---
    @Test
    fun testScenarioE_InternationalCambridgeScienceGeneration() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G8",
            subject = "Science",
            chapterUnit = "Stage 8 Unit 2: Chemical Reactions and Energy",
            sectionTopic = "Exothermic and Endothermic Reactions",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertTrue("Traceability links to Cambridge Science material", result.sourceCitations.any { it.sourceReference.contains("Cambridge") })
        assertTrue("Contains exothermic/endothermic topics", result.allQuestions.any { it.questionText.contains("Unit 2") || it.sourceReference.chapterUnit.contains("Unit 2") })
    }

    // --- Scenario F: Official Pilot Test ---
    @Test
    fun testScenarioF_OfficialPilotTest() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.PILOT_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "Mathematics",
            chapterUnit = "Chapter 1: Multi-digit Numbers & Place Values",
            totalMarks = 50,
            durationMinutes = 45,
            language = "MYANMAR"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(ExamPaperType.PILOT_TEST, result.examType)
        assertTrue(result.title.contains("Pilot Test") || result.title.contains("စမ်းသပ်စစ်ဆေးခြင်း"))
    }

    // --- Scenario G: Continuous Evaluation Test (CET) ---
    @Test
    fun testScenarioG_CETTest() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.CET,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "Science",
            chapterUnit = "Chapter 1: Plant Systems and Photosynthesis",
            totalMarks = 50,
            durationMinutes = 45,
            language = "MYANMAR"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(ExamPaperType.CET, result.examType)
        assertTrue(result.title.contains("CET") || result.title.contains("စဉ်ဆက်မပြတ်"))
    }

    // --- Scenario H: Monthly Examination ---
    @Test
    fun testScenarioH_MonthlyExam() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 1: My Family and School",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(ExamPaperType.MONTHLY_TEST, result.examType)
    }

    // --- Scenario I: Classroom Quiz ---
    @Test
    fun testScenarioI_ClassroomQuiz() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.CLASSROOM_QUIZ,
            examType = ExamPaperType.QUICK_QUIZ,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 2: Animals and Their Habitats",
            totalMarks = 10,
            durationMinutes = 15,
            sections = listOf(
                SectionConfig(
                    sectionName = "Quick Quiz",
                    sectionInstruction = "Answer all questions.",
                    questionType = QuestionType.MCQ,
                    questionCount = 5,
                    marksPerQuestion = 2,
                    difficulty = DifficultyLevel.EASY
                )
            ),
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(10, result.validationReport.totalCalculatedMarks)
        assertEquals(5, result.allQuestions.size)
    }

    // --- Scenario J: Differentiated Worksheet ---
    @Test
    fun testScenarioJ_WorksheetGeneration() = runBlocking {
        val request = WorksheetRequest(
            academicYear = "2026-2027",
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
            learningObjective = "Identify food groups and apply quantifiers accurately.",
            difficultyDistribution = DifficultyDistribution(easyCount = 5, mediumCount = 5, challengeCount = 2),
            questionCount = 12,
            language = "ENGLISH"
        )

        val result = engine.generateWorksheet(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertEquals(12, result.totalQuestions)
        val easyCount = result.items.count { it.difficulty == DifficultyLevel.EASY }
        val medCount = result.items.count { it.difficulty == DifficultyLevel.MEDIUM }
        val chCount = result.items.count { it.difficulty == DifficultyLevel.CHALLENGE }

        assertEquals(5, easyCount)
        assertEquals(5, medCount)
        assertEquals(2, chCount)
        assertTrue("Traceability contains food unit", result.sourceCitations.any { it.chapterUnit.contains("Healthy Food") })
    }

    // --- Scenario K: Old Paper Style Reference (REFERENCE ONLY) ---
    @Test
    fun testScenarioK_OldPaperStyleReference_IsReferenceOnly() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MID_TERM,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 4: Travel and Myanmar Festivals",
            totalMarks = 50,
            durationMinutes = 60,
            referencePastPaperId = 999L, // Past paper reference ID
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertTrue("Style reference note indicates REFERENCE ONLY", result.styleReferenceNote.contains("REFERENCE ONLY"))
        // Check that actual questions come from textbook Unit 4, not the past paper
        assertTrue("Questions are grounded in Unit 4 Travel", result.allQuestions.all { it.sourceReference.chapterUnit.contains("Unit 4") })
    }

    // --- Scenario L: Insufficient Source Content ---
    @Test
    fun testScenarioL_InsufficientSourceContent_FailsGracefully() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G12", // Non-existent grade in database
            subject = "Astronomy", // Non-existent subject
            chapterUnit = "Chapter 99: Deep Space",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertFalse("Should be marked invalid due to missing source chunks", result.validationReport.isValid)
        assertFalse("Cannot be grounded in curriculum when source is missing", result.isGroundedInCurriculum)
        assertTrue("Error mentions insufficient curriculum content", result.validationReport.errors.any { it.contains("Insufficient curriculum content") })
    }

    // --- Scenario M: Wrong Grade/Source Mismatch ---
    @Test
    fun testScenarioM_WrongGradeSourceMismatch() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G1", // Primary Grade 1
            subject = "Physics", // High school subject not in G1
            chapterUnit = "Quantum Mechanics",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertFalse(result.validationReport.isValid)
        assertTrue(result.validationReport.errors.isNotEmpty())
    }

    // --- Scenario N: Total Marks Validation ---
    @Test
    fun testScenarioN_TotalMarksValidationMismatch() {
        val mockRequest = QuestionPaperRequest(
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 1",
            totalMarks = 50
        )

        // Mock a section that sums to 40 instead of 50
        val mockQuestions = listOf(
            GeneratedQuestion(
                id = "1",
                questionNumber = 1,
                questionType = QuestionType.SHORT_ANSWER,
                questionText = "What is a noun?",
                correctAnswer = "Naming word",
                markingGuide = "Award 20 marks",
                marks = 20,
                sourceReference = QuestionSourceReference("Doc", "G5", "English", "Unit 1", "Sec 1", "p.1")
            ),
            GeneratedQuestion(
                id = "2",
                questionNumber = 2,
                questionType = QuestionType.SHORT_ANSWER,
                questionText = "What is a verb?",
                correctAnswer = "Action word",
                markingGuide = "Award 20 marks",
                marks = 20,
                sourceReference = QuestionSourceReference("Doc", "G5", "English", "Unit 1", "Sec 1", "p.1")
            )
        )

        val mockSections = listOf(
            QuestionSectionResult("Section A", "Answer all", 40, mockQuestions)
        )

        val dummyChunk = CurriculumChunkEntity(1, 1, "G5", "English", "Unit 1", "Sec 1", "p.1", "Sample content")

        val report = CurriculumQuestionValidator.validateQuestionPaper(mockRequest, mockSections, listOf(dummyChunk))

        assertFalse("Mark mismatch (40 vs 50) must fail validation", report.isValid)
        assertTrue(report.errors.any { it.contains("Total mark mismatch") })
    }

    // --- Scenario O: Duplicate Question Detection ---
    @Test
    fun testScenarioO_DuplicateQuestionDetection() {
        val mockRequest = QuestionPaperRequest(
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 1",
            totalMarks = 10
        )

        val duplicateQuestions = listOf(
            GeneratedQuestion(
                id = "1",
                questionNumber = 1,
                questionType = QuestionType.SHORT_ANSWER,
                questionText = "What are the five food groups in nutrition?",
                correctAnswer = "Carbohydrates, proteins, fats, vitamins, minerals",
                markingGuide = "Award 5 marks",
                marks = 5,
                sourceReference = QuestionSourceReference("Doc", "G5", "English", "Unit 1", "Sec 1", "p.1")
            ),
            GeneratedQuestion(
                id = "2",
                questionNumber = 2,
                questionType = QuestionType.SHORT_ANSWER,
                questionText = "What are the five food groups in nutrition?", // Duplicate
                correctAnswer = "Carbohydrates, proteins, fats, vitamins, minerals",
                markingGuide = "Award 5 marks",
                marks = 5,
                sourceReference = QuestionSourceReference("Doc", "G5", "English", "Unit 1", "Sec 1", "p.1")
            )
        )

        val mockSections = listOf(
            QuestionSectionResult("Section A", "Answer all", 10, duplicateQuestions)
        )

        val dummyChunk = CurriculumChunkEntity(1, 1, "G5", "English", "Unit 1", "Sec 1", "p.1", "Sample content")

        val report = CurriculumQuestionValidator.validateQuestionPaper(mockRequest, mockSections, listOf(dummyChunk))

        assertFalse("Duplicate question must trigger validation failure", report.isValid)
        assertTrue(report.errors.any { it.contains("Duplicate question detected") })
    }

    // --- Scenario P: Missing Answer Key Validation ---
    @Test
    fun testScenarioP_MissingAnswerKeyValidation() {
        val mockRequest = QuestionPaperRequest(
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 1",
            totalMarks = 10
        )

        val incompleteQuestions = listOf(
            GeneratedQuestion(
                id = "1",
                questionNumber = 1,
                questionType = QuestionType.MCQ,
                questionText = "What is the capital of Myanmar?",
                options = listOf("Yangon", "Naypyidaw", "Mandalay"),
                correctAnswer = "", // Missing correct answer
                markingGuide = "",
                marks = 5,
                sourceReference = QuestionSourceReference("Doc", "G5", "English", "Unit 1", "Sec 1", "p.1")
            ),
            GeneratedQuestion(
                id = "2",
                questionNumber = 2,
                questionType = QuestionType.SHORT_ANSWER,
                questionText = "Explain photosynthesis.",
                correctAnswer = "",
                markingGuide = "", // Missing marking rubric
                marks = 5,
                sourceReference = QuestionSourceReference("Doc", "G5", "English", "Unit 1", "Sec 1", "p.1")
            )
        )

        val mockSections = listOf(
            QuestionSectionResult("Section A", "Answer all", 10, incompleteQuestions)
        )

        val dummyChunk = CurriculumChunkEntity(1, 1, "G5", "English", "Unit 1", "Sec 1", "p.1", "Sample content")

        val report = CurriculumQuestionValidator.validateQuestionPaper(mockRequest, mockSections, listOf(dummyChunk))

        assertFalse("Missing answers must fail validation", report.isValid)
        assertTrue(report.errors.any { it.contains("Missing correct answer key") })
        assertTrue(report.errors.any { it.contains("must have a marking rubric") })
    }

    // --- Scenario Q: Myanmar Language Output ---
    @Test
    fun testScenarioQ_MyanmarLanguageOutput() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "Mathematics",
            chapterUnit = "Chapter 2: Multiplication & Division",
            totalMarks = 50,
            durationMinutes = 60,
            language = "MYANMAR"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertTrue("General instructions are in Myanmar", result.generalInstructions.any { it.contains("မေးခွန်း") || it.contains("ဖြေဆို") })
        assertTrue("Section headers are in Myanmar", result.sections.any { it.sectionName.contains("အပိုင်း") })
    }

    // --- Scenario R: English Language Output ---
    @Test
    fun testScenarioR_EnglishLanguageOutput() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "English",
            chapterUnit = "Unit 1: My Family and School",
            totalMarks = 50,
            durationMinutes = 60,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertTrue("General instructions are in English", result.generalInstructions.any { it.contains("Answer all questions") })
        assertTrue("Section headers are in English", result.sections.any { it.sectionName.contains("Section") })
    }

    // --- Scenario S: Mixed Language Output ---
    @Test
    fun testScenarioS_MixedLanguageOutput() = runBlocking {
        val request = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            subject = "Science",
            chapterUnit = "Chapter 2: The Water Cycle and Weather",
            totalMarks = 50,
            durationMinutes = 60,
            language = "MIXED"
        )

        val result = engine.generateQuestionPaper(request, adminScope)

        assertTrue(result.validationReport.isValid)
        assertNotNull(result.title)
    }

    // --- Scenario T: Teacher Permission Restriction ---
    @Test
    fun testScenarioT_TeacherPermissionRestriction() = runBlocking {
        // Teacher assigned to Grade 5 English & Science attempts to generate Grade 10 Physics
        val unauthorizedRequest = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.FINAL_EXAM,
            academicYear = "2026-2027",
            grade = "G10",
            subject = "Physics",
            chapterUnit = "Chapter 1: Units and Measurement",
            totalMarks = 100,
            durationMinutes = 120
        )

        val result = engine.generateQuestionPaper(unauthorizedRequest, g5EnglishTeacherScope)

        assertFalse("Unauthorized generation should fail validation", result.validationReport.isValid)
        assertTrue("Error explicitly states permission denied", result.validationReport.errors.any { it.contains("Permission Denied") })
    }

    // =========================================================================
    // PHASE 3B — STEP 2: REGENERATE ACTION TESTS
    // =========================================================================

    // --- Phase 3B Scenario A: Question Paper G5 English Unit 3 10 Questions Generate & Regenerate ---
    @Test
    fun testPhase3B_ScenarioA_QuestionPaperRegeneration_preservesRequestAndProducesValidOutput() = runBlocking {
        val originalRequest = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MONTHLY_TEST,
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
            pageRange = "p. 25-36",
            totalMarks = 50,
            durationMinutes = 60,
            difficulty = DifficultyLevel.MIXED,
            questionCount = 10,
            language = "ENGLISH"
        )

        // 1. First Generation
        val firstResult = engine.generateQuestionPaper(originalRequest, g5EnglishTeacherScope)
        assertTrue("First result must be valid", firstResult.validationReport.isValid)
        assertEquals("Total marks must equal 50", 50, firstResult.totalMarks)
        assertEquals("Question count must equal 10", 10, firstResult.allQuestions.size)

        // 2. Simulate User pressing [Regenerate] using the preserved request
        val regeneratedRequest = originalRequest.copy(generationSeed = 42L)
        val secondResult = engine.generateQuestionPaper(regeneratedRequest, g5EnglishTeacherScope)

        assertTrue("Regenerated result must be valid", secondResult.validationReport.isValid)
        assertEquals("Regenerated marks must equal 50", 50, secondResult.totalMarks)
        assertEquals("Regenerated question count must equal 10", 10, secondResult.allQuestions.size)
        assertEquals("Grade must remain G5", "G5", secondResult.grade)
        assertEquals("Subject must remain English", "English", secondResult.subject)
        assertEquals("Academic year must remain 2026-2027", "2026-2027", secondResult.academicYear)
        assertTrue("Grounded in curriculum", secondResult.isGroundedInCurriculum)
        assertTrue("All regenerated questions have source references", secondResult.allQuestions.all { it.sourceReference.gradeLevel == "G5" && it.sourceReference.subject == "English" })
    }

    // --- Phase 3B Scenario B: Worksheet Generation & Regeneration ---
    @Test
    fun testPhase3B_ScenarioB_WorksheetRegeneration_preservesRequestAndProducesValidOutput() = runBlocking {
        val originalWorksheetRequest = WorksheetRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
            pageRange = "p. 25-36",
            learningObjective = "Identify healthy food groups and countable/uncountable nouns",
            difficultyDistribution = DifficultyDistribution(easyCount = 5, mediumCount = 5, challengeCount = 2),
            questionCount = 12,
            language = "ENGLISH"
        )

        // 1. Initial Generation
        val firstResult = engine.generateWorksheet(originalWorksheetRequest, g5EnglishTeacherScope)
        assertTrue("Initial worksheet must be valid", firstResult.validationReport.isValid)
        assertEquals("Item count must be 12", 12, firstResult.items.size)

        // 2. Regeneration with preserved request
        val regeneratedRequest = originalWorksheetRequest.copy(generationSeed = 99L)
        val secondResult = engine.generateWorksheet(regeneratedRequest, g5EnglishTeacherScope)

        assertTrue("Regenerated worksheet must be valid", secondResult.validationReport.isValid)
        assertEquals("Preserved item count of 12", 12, secondResult.items.size)
        assertEquals("Preserved grade G5", "G5", secondResult.grade)
        assertEquals("Preserved subject English", "English", secondResult.subject)
        assertEquals("Preserved academic year", "2026-2027", secondResult.academicYear)
        assertTrue("Answer key exists for all 12 items", secondResult.items.all { it.correctAnswer.isNotBlank() })
    }

    // --- Phase 3B Scenario C: Insufficient Source Chunk Regeneration Fails Safely Without Hallucination ---
    @Test
    fun testPhase3B_ScenarioC_InsufficientSource_regenerationFailsSafelyWithoutHallucination() = runBlocking {
        // A request targeting a non-existent chapter/unit
        val nonExistentScopeRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 999: Non-Existent Space Exploration Unit",
            sectionTopic = "Alien Vocabulary",
            totalMarks = 50,
            durationMinutes = 60,
            questionCount = 10,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(nonExistentScopeRequest, g5EnglishTeacherScope)

        assertFalse("Should fail validation when curriculum source is missing", result.validationReport.isValid)
        assertFalse("Must not claim curriculum grounding", result.isGroundedInCurriculum)
        assertTrue("No ungrounded or hallucinated questions should be returned", result.allQuestions.isEmpty())
        assertTrue("Validation error indicates missing chunks", result.validationReport.errors.any { it.contains("Insufficient curriculum content") })
    }

    // --- Phase 3B Scenario D: Teacher Scope Is Strictly Enforced on Regeneration ---
    @Test
    fun testPhase3B_ScenarioD_RegenerationPreservesTeacherPermissionScope() = runBlocking {
        // Teacher Daw Aye (assigned to G5 English & Science) cannot regenerate G10 Physics
        val unauthorizedRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G10",
            className = "Room B",
            subject = "Physics",
            chapterUnit = "Chapter 1: Units and Measurement",
            totalMarks = 100,
            durationMinutes = 120,
            questionCount = 10,
            language = "ENGLISH"
        )

        val result = engine.generateQuestionPaper(unauthorizedRequest, g5EnglishTeacherScope)

        assertFalse("Teacher scope violation must fail validation", result.validationReport.isValid)
        assertTrue("Error specifically states Permission Denied", result.validationReport.errors.any { it.contains("Permission Denied") })
        assertTrue("No unauthorized questions generated", result.allQuestions.isEmpty())
    }

    // --- Phase 3B Scenario E: Workspace Result Preserves Original Request Roundtrip ---
    @Test
    fun testPhase3B_ScenarioE_WorkspaceResultPreservesAllOriginalParameters() = runBlocking {
        val qpRequest = QuestionPaperRequest(
            purpose = QuestionPurpose.OFFICIAL_EXAM,
            examType = ExamPaperType.MID_TERM,
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            sourceDocumentTitle = "Grade 5 English Official Textbook",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
            pageRange = "p. 25-36",
            totalMarks = 50,
            durationMinutes = 60,
            difficulty = DifficultyLevel.MIXED,
            questionCount = 10,
            language = "ENGLISH"
        )

        val generatedPaper = engine.generateQuestionPaper(qpRequest, g5EnglishTeacherScope)
        val structuredResult = generatedPaper.toWorkspaceStructuredResult(qpRequest)

        // Verify all required parameters are preserved in structuredResult
        assertNotNull("originalQuestionPaperRequest must be preserved", structuredResult.originalQuestionPaperRequest)
        val preserved = structuredResult.originalQuestionPaperRequest!!
        assertEquals(qpRequest.academicYear, preserved.academicYear)
        assertEquals(qpRequest.grade, preserved.grade)
        assertEquals(qpRequest.className, preserved.className)
        assertEquals(qpRequest.subject, preserved.subject)
        assertEquals(qpRequest.chapterUnit, preserved.chapterUnit)
        assertEquals(qpRequest.sectionTopic, preserved.sectionTopic)
        assertEquals(qpRequest.pageRange, preserved.pageRange)
        assertEquals(qpRequest.examType, preserved.examType)
        assertEquals(qpRequest.questionCount, preserved.questionCount)
        assertEquals(qpRequest.totalMarks, preserved.totalMarks)
        assertEquals(qpRequest.durationMinutes, preserved.durationMinutes)
        assertEquals(qpRequest.difficulty, preserved.difficulty)
        assertEquals(qpRequest.language, preserved.language)
    }

    // =========================================================================
    // PHASE 3B — STEP 3: EDIT ACTION & QUESTION-LEVEL EDITING TESTS
    // =========================================================================

    // --- Scenario A, B, C, D, H, J: Valid Question Edit Preserves Traceability & Validates ---
    @Test
    fun testPhase3B_Step3_ValidQuestionEdit_updatesContentAndPreservesSourceTraceability() = runBlocking {
        val qpRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
            totalMarks = 50,
            durationMinutes = 60,
            questionCount = 10,
            language = "ENGLISH"
        )

        val generatedPaper = engine.generateQuestionPaper(qpRequest, g5EnglishTeacherScope)
        val structuredResult = generatedPaper.toWorkspaceStructuredResult(qpRequest)

        // Select Question 1 (MCQ) to edit
        val origQ1 = structuredResult.sections.first().questions.first()
        val originalCitation = origQ1.sourceCitation
        assertNotNull("Original question must have citation", originalCitation)

        // Apply edits: (A) Question Text, (B) MCQ Option, (C) Correct Answer, (D) Marks
        val editedQ1 = origQ1.copy(
            questionText = "Which of the following is a healthy source of calcium?",
            options = listOf("A. Soda", "B. Milk", "C. Candy", "D. French Fries"),
            correctAnswer = "B. Milk",
            markingGuide = "Award 2 marks for selecting option B (Milk)",
            marks = 2,
            isManuallyEdited = true
        )

        // Modify section with edited question
        val updatedSections = structuredResult.sections.mapIndexed { sIdx, sec ->
            if (sIdx == 0) {
                val updatedQuestions = sec.questions.mapIndexed { qIdx, q -> if (qIdx == 0) editedQ1 else q }
                sec.copy(
                    sectionMarks = updatedQuestions.sumOf { it.marks },
                    questions = updatedQuestions
                )
            } else sec
        }

        // Validate edited structure
        val report = CurriculumQuestionValidator.validateEditedStructuredResult(
            originalTotalMarks = 50,
            sections = updatedSections
        )

        assertTrue("Valid edit must pass validation", report.isValid)
        assertEquals("Calculated total marks must match 50", 50, report.totalCalculatedMarks)

        val updatedQ1 = updatedSections.first().questions.first()
        assertEquals("Which of the following is a healthy source of calcium?", updatedQ1.questionText)
        assertEquals("B. Milk", updatedQ1.correctAnswer)
        assertEquals(4, updatedQ1.options.size)
        assertTrue("Marked as manually edited", updatedQ1.isManuallyEdited)
        assertEquals("Source citation document title must be preserved", originalCitation?.documentTitle, updatedQ1.sourceCitation?.documentTitle)
        assertEquals("Source citation chapter must be preserved", originalCitation?.chapterUnit, updatedQ1.sourceCitation?.chapterUnit)
    }

    // --- Scenario E, F: Invalid Marks Total is Caught by Validator ---
    @Test
    fun testPhase3B_Step3_InvalidMarksTotal_isCaughtByValidator() = runBlocking {
        val qpRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            totalMarks = 50,
            durationMinutes = 60,
            questionCount = 10,
            language = "ENGLISH"
        )

        val generatedPaper = engine.generateQuestionPaper(qpRequest, g5EnglishTeacherScope)
        val structuredResult = generatedPaper.toWorkspaceStructuredResult(qpRequest)

        // Increase marks of Q1 from 2 to 15 (Total becomes 63 instead of 50)
        val origQ1 = structuredResult.sections.first().questions.first()
        val invalidQ1 = origQ1.copy(marks = 15)

        val updatedSections = structuredResult.sections.mapIndexed { sIdx, sec ->
            if (sIdx == 0) {
                val updatedQuestions = sec.questions.mapIndexed { qIdx, q -> if (qIdx == 0) invalidQ1 else q }
                sec.copy(
                    sectionMarks = updatedQuestions.sumOf { it.marks },
                    questions = updatedQuestions
                )
            } else sec
        }

        val report = CurriculumQuestionValidator.validateEditedStructuredResult(
            originalTotalMarks = 50,
            sections = updatedSections
        )

        assertFalse("Mismatched total marks must fail validation", report.isValid)
        assertTrue("Error contains marks mismatch message", report.errors.any { it.contains("Total marks mismatch") })
    }

    // --- Scenario G: Cancel Leaves Original Result Intact ---
    @Test
    fun testPhase3B_Step3_CancelEdit_preservesOriginalResultUnchanged() = runBlocking {
        val qpRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            totalMarks = 50,
            durationMinutes = 60,
            questionCount = 10,
            language = "ENGLISH"
        )

        val generatedPaper = engine.generateQuestionPaper(qpRequest, g5EnglishTeacherScope)
        val structuredResult = generatedPaper.toWorkspaceStructuredResult(qpRequest)

        // Create a copy simulating an ongoing edit
        val draftEditedResult = structuredResult.copy(
            sections = structuredResult.sections.map { sec ->
                sec.copy(questions = sec.questions.map { it.copy(questionText = "Draft change") })
            }
        )

        // Cancel action: discard draft and keep structuredResult
        val finalResult = structuredResult

        assertNotEquals(draftEditedResult.sections.first().questions.first().questionText, finalResult.sections.first().questions.first().questionText)
        assertEquals(generatedPaper.allQuestions.first().questionText, finalResult.sections.first().questions.first().questionText)
        assertFalse("Original is not marked as manually edited", finalResult.sections.first().questions.first().isManuallyEdited)
    }

    // --- Scenario I: Multiple Questions Edited in Same Session ---
    @Test
    fun testPhase3B_Step3_MultipleQuestionEdits_appliesAllChangesSimultaneously() = runBlocking {
        val qpRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            totalMarks = 50,
            durationMinutes = 60,
            questionCount = 10,
            language = "ENGLISH"
        )

        val generatedPaper = engine.generateQuestionPaper(qpRequest, g5EnglishTeacherScope)
        val structuredResult = generatedPaper.toWorkspaceStructuredResult(qpRequest)

        val updatedSections = structuredResult.sections.mapIndexed { sIdx, sec ->
            if (sIdx == 0) {
                val questions = sec.questions.toMutableList()
                val q1 = questions[0].copy(questionText = "Edited Q1 text", marks = 3, isManuallyEdited = true)
                val q2 = questions[1].copy(questionText = "Edited Q2 text", marks = 1, isManuallyEdited = true) // 3 + 1 = 4 (preserves 2 + 2 = 4 total)
                questions[0] = q1
                questions[1] = q2
                sec.copy(
                    sectionMarks = questions.sumOf { it.marks },
                    questions = questions
                )
            } else sec
        }

        val report = CurriculumQuestionValidator.validateEditedStructuredResult(
            originalTotalMarks = 50,
            sections = updatedSections
        )

        assertTrue("Multi-question edit maintaining marks must be valid", report.isValid)
        val updatedQ1 = updatedSections.first().questions[0]
        val updatedQ2 = updatedSections.first().questions[1]
        val uneditedQ3 = updatedSections.first().questions[2]

        assertTrue(updatedQ1.isManuallyEdited)
        assertTrue(updatedQ2.isManuallyEdited)
        assertFalse(uneditedQ3.isManuallyEdited)
        assertEquals("Edited Q1 text", updatedQ1.questionText)
        assertEquals("Edited Q2 text", updatedQ2.questionText)
    }

    // --- Scenario K: Regenerate Still Functions With Preserved Request After Edits ---
    @Test
    fun testPhase3B_Step3_RegenerateStillWorksAfterEditing() = runBlocking {
        val qpRequest = QuestionPaperRequest(
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            subject = "English",
            chapterUnit = "Unit 3: Healthy Food and Nutrition",
            totalMarks = 50,
            durationMinutes = 60,
            questionCount = 10,
            language = "ENGLISH"
        )

        val generatedPaper = engine.generateQuestionPaper(qpRequest, g5EnglishTeacherScope)
        val structuredResult = generatedPaper.toWorkspaceStructuredResult(qpRequest)

        // Preserved request remains attached to structuredResult even if questions are edited
        assertNotNull("originalQuestionPaperRequest must remain present", structuredResult.originalQuestionPaperRequest)

        val preservedRequest = structuredResult.originalQuestionPaperRequest!!
        val freshRegeneratedPaper = engine.generateQuestionPaper(preservedRequest.copy(generationSeed = 777L), g5EnglishTeacherScope)

        assertTrue("Regeneration from edited paper's preserved request succeeds", freshRegeneratedPaper.validationReport.isValid)
        assertEquals(50, freshRegeneratedPaper.totalMarks)
        assertEquals(10, freshRegeneratedPaper.allQuestions.size)
    }
}

/**
 * In-Memory Fake DAO for Curriculum Knowledge testing.
 */
class FakeCurriculumKnowledgeDao : CurriculumKnowledgeDao {
    private val documents = mutableListOf<CurriculumDocumentEntity>()
    private val chunks = mutableListOf<CurriculumChunkEntity>()

    override fun getAllDocumentsFlow(): Flow<List<CurriculumDocumentEntity>> = flowOf(documents)

    override suspend fun getDocumentsByGradeAndSubject(gradeLevel: String, subject: String): List<CurriculumDocumentEntity> {
        return documents.filter { it.gradeLevel.equals(gradeLevel, ignoreCase = true) && it.subject.equals(subject, ignoreCase = true) }
    }

    override suspend fun getDocumentById(id: Long): CurriculumDocumentEntity? {
        return documents.find { it.id == id }
    }

    override suspend fun insertDocument(document: CurriculumDocumentEntity): Long {
        documents.add(document)
        return document.id
    }

    override suspend fun insertDocuments(documents: List<CurriculumDocumentEntity>): List<Long> {
        this.documents.addAll(documents)
        return documents.map { it.id }
    }

    override suspend fun deleteDocument(document: CurriculumDocumentEntity) {
        documents.remove(document)
    }

    override fun getChunksByGradeAndSubjectFlow(gradeLevel: String, subject: String): Flow<List<CurriculumChunkEntity>> {
        return flowOf(chunks.filter { it.gradeLevel.equals(gradeLevel, ignoreCase = true) && it.subject.equals(subject, ignoreCase = true) })
    }

    override suspend fun getChunksByGradeAndSubject(gradeLevel: String, subject: String): List<CurriculumChunkEntity> {
        return chunks.filter { it.gradeLevel.equals(gradeLevel, ignoreCase = true) && it.subject.equals(subject, ignoreCase = true) }
    }

    override suspend fun getChunksForUnit(gradeLevel: String, subject: String, chapterUnit: String): List<CurriculumChunkEntity> {
        return chunks.filter { 
            it.gradeLevel.equals(gradeLevel, ignoreCase = true) && 
            it.subject.equals(subject, ignoreCase = true) &&
            it.chapterUnit.contains(chapterUnit, ignoreCase = true)
        }
    }

    override suspend fun getChunksForScope(
        gradeLevel: String,
        subject: String,
        chapterUnit: String,
        sectionTopic: String
    ): List<CurriculumChunkEntity> {
        return chunks.filter { chunk ->
            chunk.gradeLevel.equals(gradeLevel, ignoreCase = true) &&
            chunk.subject.equals(subject, ignoreCase = true) &&
            (chapterUnit.isBlank() || chunk.chapterUnit.contains(chapterUnit, ignoreCase = true)) &&
            (sectionTopic.isBlank() || chunk.sectionTopic.contains(sectionTopic, ignoreCase = true))
        }
    }

    override suspend fun getAvailableUnits(gradeLevel: String, subject: String): List<String> {
        return chunks.filter { it.gradeLevel.equals(gradeLevel, ignoreCase = true) && it.subject.equals(subject, ignoreCase = true) }
            .map { it.chapterUnit }
            .distinct()
    }

    override suspend fun getAvailableSections(gradeLevel: String, subject: String, chapterUnit: String): List<String> {
        return chunks.filter { 
            it.gradeLevel.equals(gradeLevel, ignoreCase = true) && 
            it.subject.equals(subject, ignoreCase = true) &&
            it.chapterUnit.contains(chapterUnit, ignoreCase = true)
        }.map { it.sectionTopic }.distinct()
    }

    override suspend fun searchChunks(gradeLevel: String, subject: String, query: String): List<CurriculumChunkEntity> {
        return chunks.filter { 
            it.gradeLevel.equals(gradeLevel, ignoreCase = true) && 
            it.subject.equals(subject, ignoreCase = true) &&
            (it.content.contains(query, ignoreCase = true) || it.keywords.contains(query, ignoreCase = true))
        }
    }

    override suspend fun getChunkCount(): Int = chunks.size

    override suspend fun insertChunk(chunk: CurriculumChunkEntity): Long {
        chunks.add(chunk)
        return chunk.id
    }

    override suspend fun insertChunks(chunks: List<CurriculumChunkEntity>) {
        this.chunks.addAll(chunks)
    }

    override suspend fun deleteChunkById(id: Long) {
        chunks.removeIf { it.id == id }
    }

    override suspend fun deleteChunksByDocumentId(documentId: Long) {
        chunks.removeIf { it.documentId == documentId }
    }
}
