package com.example.ai

import com.example.data.ai.*
import com.example.data.local.entity.CurriculumChunkEntity
import com.example.data.local.entity.UserRole
import com.example.ui.screens.ai.workspace.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive Unit Test Suite for Phase 5:
 * Multi-Turn Conversational AI Engine & Teacher Context (Scenarios A through U).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationalAiEngineComprehensiveTest {

    private lateinit var sampleChunks: List<CurriculumChunkEntity>
    private lateinit var sampleStructuredResult: WorkspaceStructuredResult
    private lateinit var teacherProfile: TeacherProfileContext

    @Before
    fun setUp() {
        teacherProfile = TeacherProfileContext(
            teacherUsername = "daw_khin",
            teacherName = "Daw Khin Myo",
            role = UserRole.TEACHER,
            teacherCode = "T-001",
            qualifications = "B.Ed (English), Dip. in Educational Leadership",
            assignedGrades = listOf("G5", "G6"),
            assignedClasses = listOf("Room A", "Room B"),
            assignedSubjects = listOf("English", "Science"),
            academicYear = "2026-2027"
        )

        sampleChunks = listOf(
            CurriculumChunkEntity(
                id = 1L,
                documentId = 101L,
                gradeLevel = "G5",
                subject = "English",
                chapterUnit = "Unit 3: Healthy Food and Nutrition",
                sectionTopic = "Countable and Uncountable Nouns & Food Pyramid",
                pageRange = "pp. 32-36",
                content = "Healthy food provides essential nutrients. Fruits, vegetables, and dairy products maintain body vitality. We use 'much' for uncountable nouns and 'many' for countable nouns.",
                learningObjectives = "Identify food groups and apply quantifiers accurately.",
                vocabularyWords = "nutrition, vitamin, mineral, balanced diet, carbohydrate",
                keywords = "food, nutrition, healthy, quantifiers",
                sourceReference = "Myanmar MoE G5 English, Unit 3, pp. 32-36"
            )
        )

        val questions = (1..10).map { i ->
            QuestionItemUi(
                id = "Q-$i",
                questionNumber = i,
                sectionName = if (i <= 5) "Section A" else "Section B",
                questionType = if (i <= 5) "Multiple Choice" else "Short Answer",
                questionText = "Question $i regarding healthy food and nutrition in Unit 3.",
                options = if (i <= 5) listOf("A) Option 1", "B) Option 2", "C) Option 3", "D) Option 4") else emptyList(),
                correctAnswer = if (i <= 5) "A" else "Standard answer for question $i",
                markingGuide = "Award full mark for correct answer.",
                marks = if (i <= 5) 1 else 2,
                difficulty = "Medium",
                sourceCitation = SourceCitationUi(
                    documentTitle = "Myanmar Basic Education G5 English",
                    gradeLevel = "G5",
                    subject = "English",
                    chapterUnit = "Unit 3: Healthy Food and Nutrition",
                    sectionTopic = "Food Pyramid",
                    pageRange = "pp. 32-36",
                    tier = SourceTier.PRIMARY_TEXTBOOK
                )
            )
        }

        val secA = QuestionSectionUi(
            sectionName = "Section A",
            sectionInstruction = "Answer all multiple choice questions.",
            sectionMarks = 5,
            questions = questions.take(5)
        )
        val secB = QuestionSectionUi(
            sectionName = "Section B",
            sectionInstruction = "Answer all short answer questions.",
            sectionMarks = 10,
            questions = questions.drop(5)
        )

        sampleStructuredResult = WorkspaceStructuredResult(
            title = "Grade 5 English — Monthly Examination",
            academicYear = "2026-2027",
            grade = "G5",
            subject = "English",
            examType = "Monthly Test",
            durationMinutes = 60,
            totalMarks = 15,
            isCurriculumVerified = true,
            validationSummary = "100% Grounded in Approved MoE Textbooks",
            sourceCitations = listOf(
                SourceCitationUi(
                    documentTitle = "Myanmar Basic Education G5 English",
                    gradeLevel = "G5",
                    subject = "English",
                    chapterUnit = "Unit 3: Healthy Food and Nutrition",
                    sectionTopic = "Food Pyramid",
                    pageRange = "pp. 32-36",
                    tier = SourceTier.PRIMARY_TEXTBOOK
                )
            ),
            sections = listOf(secA, secB)
        )
    }

    // --- Scenario A: Multi-turn Context & Parameter Retention ---
    @Test
    fun testScenarioA_ContextCarryoverAcrossTurns() {
        val userPrompt = "Make questions 8-10 harder."
        val parsed = ConversationalContextResolver.resolveIntent(
            prompt = userPrompt,
            activeStructuredResult = sampleStructuredResult,
            teacherProfile = teacherProfile
        )

        assertEquals(ConversationIntentType.REGENERATE_RANGE, parsed.intentType)
        assertEquals(Pair(8, 10), parsed.targetQuestionRange)
        assertEquals(DifficultyLevel.CHALLENGE, parsed.targetDifficulty)
        assertEquals("G5", parsed.targetGrade)
        assertEquals("English", parsed.targetSubject)
        assertEquals("Unit 3: Healthy Food and Nutrition", parsed.targetChapterUnit)
    }

    // --- Scenario B: Myanmar Language Numerals and Prompt Resolution ---
    @Test
    fun testScenarioB_MyanmarLanguageNumeralsAndIntent() {
        val prompt = "မေးခွန်းနံပါတ် ၄ ကို ပိုမိုလွယ်ကူအောင် ပြင်ဆင်ပေးပါ"
        val parsed = ConversationalContextResolver.resolveIntent(
            prompt = prompt,
            activeStructuredResult = sampleStructuredResult,
            teacherProfile = teacherProfile
        )

        assertEquals(ConversationIntentType.PARTIAL_EDIT_QUESTION, parsed.intentType)
        assertEquals(4, parsed.targetQuestionNumber)
        assertEquals(DifficultyLevel.EASY, parsed.targetDifficulty)
    }

    // --- Scenario C: Surgical Single Question Modification ---
    @Test
    fun testScenarioC_SurgicalSingleQuestionModification() {
        val originalQ1 = sampleStructuredResult.sections[0].questions[0]
        val originalQ4 = sampleStructuredResult.sections[0].questions[3]

        val updated = PartialRegenerationEngine.regenerateSingleQuestion(
            currentResult = sampleStructuredResult,
            targetQuestionNumber = 4,
            newDifficulty = DifficultyLevel.EASY,
            newQuestionType = QuestionType.MCQ,
            curriculumChunks = sampleChunks
        )

        val updatedQ1 = updated.sections[0].questions[0]
        val updatedQ4 = updated.sections[0].questions[3]

        // Q1 must remain completely identical
        assertEquals(originalQ1.questionText, updatedQ1.questionText)
        assertEquals(originalQ1.correctAnswer, updatedQ1.correctAnswer)
        assertFalse(updatedQ1.isManuallyEdited)

        // Q4 must be updated
        assertNotEquals(originalQ4.questionText, updatedQ4.questionText)
        assertEquals("EASY", updatedQ4.difficulty)
        assertTrue(updatedQ4.isManuallyEdited)
    }

    // --- Scenario D: Question Range Regeneration ---
    @Test
    fun testScenarioD_QuestionRangeRegeneration() {
        val originalQ7 = sampleStructuredResult.sections[1].questions[1] // Q7
        val originalQ8 = sampleStructuredResult.sections[1].questions[2] // Q8
        val originalQ10 = sampleStructuredResult.sections[1].questions[4] // Q10

        val updated = PartialRegenerationEngine.regenerateQuestionRange(
            currentResult = sampleStructuredResult,
            fromQuestionNumber = 8,
            toQuestionNumber = 10,
            newDifficulty = DifficultyLevel.CHALLENGE,
            newQuestionType = QuestionType.SHORT_ANSWER,
            curriculumChunks = sampleChunks
        )

        val updatedQ7 = updated.sections[1].questions[1]
        val updatedQ8 = updated.sections[1].questions[2]
        val updatedQ10 = updated.sections[1].questions[4]

        // Q7 untouched
        assertEquals(originalQ7.questionText, updatedQ7.questionText)
        assertFalse(updatedQ7.isManuallyEdited)

        // Q8 and Q10 regenerated with CHALLENGE difficulty
        assertEquals("CHALLENGE", updatedQ8.difficulty)
        assertEquals("CHALLENGE", updatedQ10.difficulty)
        assertTrue(updatedQ8.isManuallyEdited)
        assertTrue(updatedQ10.isManuallyEdited)
    }

    // --- Scenario E: Question Type Alteration ---
    @Test
    fun testScenarioE_QuestionTypeAlteration() {
        val updated = PartialRegenerationEngine.regenerateSingleQuestion(
            currentResult = sampleStructuredResult,
            targetQuestionNumber = 2,
            newDifficulty = DifficultyLevel.MEDIUM,
            newQuestionType = QuestionType.FILL_IN_BLANKS,
            curriculumChunks = sampleChunks
        )

        val q2 = updated.sections[0].questions[1]
        assertEquals("Fill in the Blanks", q2.questionType)
        assertTrue(q2.questionText.contains("____"))
    }

    // --- Scenario F: Question Addition ---
    @Test
    fun testScenarioF_AddingQuestionsToWorkspace() {
        val initialCount = sampleStructuredResult.sections.sumOf { it.questions.size }
        val updated = PartialRegenerationEngine.addQuestions(
            currentResult = sampleStructuredResult,
            count = 2,
            topic = "Vocabulary and Grammar",
            questionType = QuestionType.SHORT_ANSWER,
            difficulty = DifficultyLevel.MEDIUM,
            curriculumChunks = sampleChunks
        )

        val newTotalCount = updated.sections.sumOf { it.questions.size }
        assertEquals(initialCount + 2, newTotalCount)

        val q11 = updated.sections[1].questions[5]
        val q12 = updated.sections[1].questions[6]
        assertEquals(11, q11.questionNumber)
        assertEquals(12, q12.questionNumber)
        assertTrue(updated.totalMarks > sampleStructuredResult.totalMarks)
    }

    // --- Scenario G: Pedagogical Question Explanation ---
    @Test
    fun testScenarioG_PedagogicalExplanationGeneration() {
        val explanation = PartialRegenerationEngine.explainQuestion(
            currentResult = sampleStructuredResult,
            questionNumber = 5
        )

        assertTrue(explanation.contains("Explanation for Question #5"))
        assertTrue(explanation.contains("Curriculum Alignment"))
        assertTrue(explanation.contains("G5 English"))
        assertTrue(explanation.contains("Marking Rubric"))
    }

    // --- Scenario H: Bilingual Translation of Assessment ---
    @Test
    fun testScenarioH_BilingualTranslationPreservesSchema() {
        val translated = PartialRegenerationEngine.translateContent(
            currentResult = sampleStructuredResult,
            targetLanguage = "MYANMAR"
        )

        assertEquals(sampleStructuredResult.sections.size, translated.sections.size)
        assertEquals(sampleStructuredResult.totalMarks, translated.totalMarks)
        assertTrue(translated.title.contains("မေးခွန်းလွှာ"))
    }

    // --- Scenario I: Format Transformation Exam <-> Worksheet ---
    @Test
    fun testScenarioI_FormatTransformation() {
        val worksheet = PartialRegenerationEngine.convertQuestionPaperToWorksheet(sampleStructuredResult)
        assertEquals("Differentiated Worksheet", worksheet.examType)
        assertTrue(worksheet.title.contains("Worksheet"))

        val convertedBackExam = PartialRegenerationEngine.convertWorksheetToQuestionPaper(worksheet, "Final Exam")
        assertEquals("Final Exam", convertedBackExam.examType)
        assertTrue(convertedBackExam.title.contains("Final Exam"))
    }

    // --- Scenario J: Student Report Card Intent from Conversation ---
    @Test
    fun testScenarioJ_ReportCardCommentIntentResolution() {
        val prompt = "Generate report card comment for student Aung Aung"
        val parsed = ConversationalContextResolver.resolveIntent(
            prompt = prompt,
            activeStructuredResult = sampleStructuredResult,
            teacherProfile = teacherProfile
        )

        assertEquals(ConversationIntentType.CREATE_REPORT_COMMENT, parsed.intentType)
        assertEquals("Aung Aung", parsed.targetStudentName)
        assertEquals("G5", parsed.targetGrade)
    }

    // --- Scenario K: Teacher Profile Context Injection ---
    @Test
    fun testScenarioK_TeacherProfileContextInjection() {
        val slidingPrompt = ConversationalContextResolver.buildSlidingWindowPrompt(
            userPrompt = "Suggest 3 group activities for Unit 3",
            teacherProfile = teacherProfile,
            recentTurns = emptyList(),
            activeStructuredResult = sampleStructuredResult
        )

        assertTrue(slidingPrompt.contains("Daw Khin Myo"))
        assertTrue(slidingPrompt.contains("B.Ed (English)"))
        assertTrue(slidingPrompt.contains("G5, G6"))
        assertTrue(slidingPrompt.contains("2026-2027"))
    }

    // --- Scenario L: Academic Year & Scope Resolution ---
    @Test
    fun testScenarioL_AcademicYearAndScope() {
        assertEquals("2026-2027", teacherProfile.academicYear)
        assertTrue(teacherProfile.assignedGrades.contains("G5"))
        assertTrue(teacherProfile.assignedSubjects.contains("English"))
    }

    // --- Scenario M: Multilingual Prompt Understanding ---
    @Test
    fun testScenarioM_MultilingualPrompts() {
        val promptMyanmar = "မေးခွန်း ၈ မှ ၁၀ ကို ပိုခက်အောင် ပြုလုပ်ပါ"
        val parsed = ConversationalContextResolver.resolveIntent(promptMyanmar, sampleStructuredResult, teacherProfile = teacherProfile)
        assertEquals(ConversationIntentType.REGENERATE_RANGE, parsed.intentType)
        assertEquals(Pair(8, 10), parsed.targetQuestionRange)
        assertEquals(DifficultyLevel.CHALLENGE, parsed.targetDifficulty)
    }

    // --- Scenario N: Sliding Window Truncation ---
    @Test
    fun testScenarioN_SlidingWindowPromptTruncation() {
        val turns = (1..20).map { i ->
            com.example.data.local.entity.AiChatMessageEntity(
                id = i.toLong(),
                sessionId = 1L,
                senderRole = if (i % 2 == 1) "USER" else "ASSISTANT",
                content = "Turn $i message content."
            )
        }

        val prompt = ConversationalContextResolver.buildSlidingWindowPrompt(
            userPrompt = "Next question",
            teacherProfile = teacherProfile,
            recentTurns = turns,
            activeStructuredResult = sampleStructuredResult,
            maxTurns = 6
        )

        assertFalse(prompt.contains("Turn 1 message content."))
        assertTrue(prompt.contains("Turn 20 message content."))
        assertTrue(prompt.contains("Turn 15 message content."))
    }

    // --- Scenario O: Mark Recalculation Integrity ---
    @Test
    fun testScenarioO_MarkRecalculationIntegrity() {
        val updated = PartialRegenerationEngine.regenerateSingleQuestion(
            currentResult = sampleStructuredResult,
            targetQuestionNumber = 1,
            newDifficulty = DifficultyLevel.CHALLENGE,
            newQuestionType = QuestionType.ESSAY,
            curriculumChunks = sampleChunks
        )

        // Q1 changed from 1 mark to 5 marks
        val expectedNewTotal = sampleStructuredResult.totalMarks - 1 + 5
        assertEquals(expectedNewTotal, updated.totalMarks)
    }

    // --- Scenario P: Citation Consistency ---
    @Test
    fun testScenarioP_CitationConsistency() {
        val updated = PartialRegenerationEngine.regenerateSingleQuestion(
            currentResult = sampleStructuredResult,
            targetQuestionNumber = 4,
            newDifficulty = DifficultyLevel.MEDIUM,
            newQuestionType = QuestionType.MCQ,
            curriculumChunks = sampleChunks
        )

        val q4 = updated.sections[0].questions[3]
        assertNotNull(q4.sourceCitation)
        assertEquals("G5", q4.sourceCitation?.gradeLevel)
        assertEquals("English", q4.sourceCitation?.subject)
    }

    // --- Scenario Q: Fallback Mode ---
    @Test
    fun testScenarioQ_DeterministicOfflineFallback() {
        val facts = StudentReportFacts(
            studentId = 101L,
            studentName = "Mg Mg",
            academicYear = "2026-2027",
            grade = "G5",
            className = "Room A",
            periodName = "Monthly Test",
            subjectResults = listOf(SubjectFact("English", 85.0, 100.0, 85.0, "A", true, "Distinction")),
            strongestSubjects = listOf("English"),
            improvementSubjects = emptyList(),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(85.0, 100.0, 85.0, "A", 1, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("Monthly", 98.0, 2.0, 0.0, 25, true, "Regular"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 4.5, 4.5, 4.5, 4.5, "Good", emptyList(), emptyList())
        )

        val engine = ReportCardAiEngine()
        val result = engine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertNotNull(result)
        assertTrue(result.teacherComment.isNotBlank())
        assertTrue(result.parentSuggestion.isNotBlank())
    }

    // --- Scenario R: Serialization & Deserialization Continuity ---
    @Test
    fun testScenarioR_SerializationAndDeserializationContinuity() {
        val serialized = WorkspaceStructuredResultSerializer.serialize(sampleStructuredResult, "daw_khin")
        val deserialized = WorkspaceStructuredResultSerializer.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(sampleStructuredResult.title, deserialized?.title)
        assertEquals(sampleStructuredResult.sections.size, deserialized?.sections?.size)
        assertEquals(sampleStructuredResult.totalMarks, deserialized?.totalMarks)
    }

    // --- Scenario S: Question Number Range Boundary Tests ---
    @Test
    fun testScenarioS_QuestionNumberRangeBoundaries() {
        val range1 = ConversationalContextResolver.extractQuestionRange("questions 8 to 10")
        assertEquals(Pair(8, 10), range1)

        val range2 = ConversationalContextResolver.extractQuestionRange("8-10")
        assertEquals(Pair(8, 10), range2)

        val range3 = ConversationalContextResolver.extractQuestionRange("မေးခွန်း ၅ မှ ၈")
        val normalized = ConversationalContextResolver.normalizeMyanmarNumerals("မေးခွန်း ၅ မှ ၈")
        val range3Parsed = ConversationalContextResolver.extractQuestionRange(normalized)
        assertEquals(Pair(5, 8), range3Parsed)
    }

    // --- Scenario T: Single Question Extraction Tests ---
    @Test
    fun testScenarioT_SingleQuestionExtraction() {
        val q1 = ConversationalContextResolver.extractSingleQuestionNumber("make number 4 easier")
        assertEquals(4, q1)

        val q2 = ConversationalContextResolver.extractSingleQuestionNumber("question #9")
        assertEquals(9, q2)

        val q3 = ConversationalContextResolver.extractSingleQuestionNumber("မေးခွန်းနံပါတ် ၃")
        val normalized = ConversationalContextResolver.normalizeMyanmarNumerals("မေးခွန်းနံပါတ် ၃")
        val q3Parsed = ConversationalContextResolver.extractSingleQuestionNumber(normalized)
        assertEquals(3, q3Parsed)
    }

    // --- Scenario U: End-to-End Multi-Turn Simulation Flow ---
    @Test
    fun testScenarioU_EndToEndMultiTurnConversationSimulation() {
        // Turn 1: Initial creation result loaded into workspace
        var activeWorkspace: WorkspaceStructuredResult = sampleStructuredResult
        assertEquals(10, activeWorkspace.sections.sumOf { it.questions.size })

        // Turn 2: Teacher asks: "Make questions 8-10 harder"
        val intentTurn2 = ConversationalContextResolver.resolveIntent(
            prompt = "Make questions 8 to 10 harder",
            activeStructuredResult = activeWorkspace,
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.REGENERATE_RANGE, intentTurn2.intentType)
        activeWorkspace = PartialRegenerationEngine.regenerateQuestionRange(
            currentResult = activeWorkspace,
            fromQuestionNumber = intentTurn2.targetQuestionRange!!.first,
            toQuestionNumber = intentTurn2.targetQuestionRange!!.second,
            newDifficulty = intentTurn2.targetDifficulty,
            curriculumChunks = sampleChunks
        )
        assertEquals("CHALLENGE", activeWorkspace.sections[1].questions[4].difficulty)

        // Turn 3: Teacher asks: "Change question 2 to multiple choice"
        val intentTurn3 = ConversationalContextResolver.resolveIntent(
            prompt = "Change question 2 to multiple choice",
            activeStructuredResult = activeWorkspace,
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.PARTIAL_EDIT_QUESTION, intentTurn3.intentType)
        activeWorkspace = PartialRegenerationEngine.regenerateSingleQuestion(
            currentResult = activeWorkspace,
            targetQuestionNumber = intentTurn3.targetQuestionNumber!!,
            newQuestionType = intentTurn3.targetQuestionType,
            curriculumChunks = sampleChunks
        )
        assertEquals("Multiple Choice", activeWorkspace.sections[0].questions[1].questionType)

        // Turn 4: Teacher asks: "Add 2 more questions on vocabulary"
        val intentTurn4 = ConversationalContextResolver.resolveIntent(
            prompt = "Add 2 more questions on vocabulary",
            activeStructuredResult = activeWorkspace,
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.ADD_QUESTIONS, intentTurn4.intentType)
        activeWorkspace = PartialRegenerationEngine.addQuestions(
            currentResult = activeWorkspace,
            count = intentTurn4.additionalCount!!,
            topic = "Vocabulary",
            curriculumChunks = sampleChunks
        )
        assertEquals(12, activeWorkspace.sections.sumOf { it.questions.size })

        // Turn 5: Teacher asks: "Explain why question 5 has this answer"
        val intentTurn5 = ConversationalContextResolver.resolveIntent(
            prompt = "Explain why question 5 has this answer",
            activeStructuredResult = activeWorkspace,
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.EXPLAIN_QUESTION, intentTurn5.intentType)
        val explanation = PartialRegenerationEngine.explainQuestion(activeWorkspace, 5)
        assertTrue(explanation.contains("Question #5"))

        // Final verification: Workspace maintains complete consistency and validation
        assertTrue(activeWorkspace.totalMarks > 0)
        assertEquals("G5", activeWorkspace.grade)
        assertEquals("English", activeWorkspace.subject)
    }
}
