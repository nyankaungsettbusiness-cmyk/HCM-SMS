package com.example.ui.screens.ai.workspace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

import com.example.data.ai.*

/**
 * UI State Models & Contracts for Phase 3A/3B: AI Assistant Conversational Workspace.
 */

enum class AiAssistantMode(val displayName: String) {
    TEACHER("Teacher Assistant"),
    ADMIN("Admin Assistant")
}

enum class QuickActionType(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val isAdminOnly: Boolean = false
) {
    QUESTION_PAPER("Questions", Icons.Default.Description, "Curriculum assessment papers", false),
    WORKSHEET("Worksheet", Icons.Default.Assignment, "Differentiated practice worksheets", false),
    LESSON_PLAN("Lesson Plan", Icons.Default.MenuBook, "Curriculum-aligned lesson plans", false),
    CLASS_ACTIVITY("Class Activity", Icons.Default.Groups, "Interactive classroom activities", false),
    REPORT_COMMENT("Report Comment", Icons.Default.Comment, "Bilingual report card feedback", false),
    TEACHING_VISUAL("Teaching Visual", Icons.Default.Image, "Curriculum visual aids & diagrams", false),
    QUIZ("Classroom Quiz", Icons.Default.Quiz, "Quick classroom quizzes", false),
    STUDENT_ANALYSIS("Student Analysis", Icons.Default.Analytics, "Holistic student insights", false),
    SCHOOL_ANALYTICS("School Analytics", Icons.Default.Insights, "Academic & attendance overview", true),
    ATTENDANCE_OVERVIEW("Attendance", Icons.Default.FactCheck, "Cohort attendance summary", true),
    EXAM_PERFORMANCE("Exam Trends", Icons.Default.BarChart, "Assessment performance distribution", true),
    AT_RISK_STUDENTS("At-Risk Support", Icons.Default.WarningAmber, "Identify students needing support", true)
}

data class AiContextConfigState(
    val academicYear: String = "2026–2027",
    val grade: String = "G5",
    val className: String = "Room A",
    val subject: String = "English",
    val curriculumSource: String = "Ministry of Education Myanmar Curriculum (2024–2025)",
    val chapterUnit: String = "Unit 3",
    val topic: String = "Food and Nutrition",
    val language: String = "English"
)

enum class SourceTier(val displayName: String, val badgeText: String, val isReferenceOnly: Boolean) {
    PRIMARY_TEXTBOOK("Approved Myanmar Textbook / Curriculum", "PRIMARY SOURCE", false),
    SECONDARY_GUIDE("Teacher Guide / International Course", "SECONDARY APPROVED", false),
    PAST_PAPER_REFERENCE("Past Question Paper Archive", "REFERENCE ONLY", true)
}

enum class QuestionPurposeUi(val displayName: String) {
    OFFICIAL_EXAM("Official Examination"),
    CLASSROOM_QUIZ("Classroom Quick Quiz"),
    PRACTICE_REVISION("Practice / Revision Paper")
}

enum class ExamTypeUi(val displayName: String, val defaultMarks: Int, val defaultDuration: Int) {
    PILOT_TEST("Pilot Test", 50, 45),
    CET("Continuous Evaluation Test (CET)", 50, 45),
    MONTHLY_TEST("Monthly Test", 50, 60),
    MID_TERM("Mid-Term Exam", 50, 60),
    FINAL_EXAM("Final Semester Exam", 100, 120),
    CUSTOM("Custom Exam", 50, 60),
    QUICK_QUIZ("Quick Quiz (10-15m)", 10, 15),
    COMPREHENSIVE_QUIZ("Unit Comprehensive Quiz", 25, 30)
}

data class SourceCitationUi(
    val documentTitle: String,
    val gradeLevel: String,
    val subject: String,
    val chapterUnit: String,
    val sectionTopic: String,
    val pageRange: String,
    val tier: SourceTier = SourceTier.PRIMARY_TEXTBOOK
)

enum class VisualAlignment {
    LEFT, CENTER, RIGHT
}

data class VisualAssetRef(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val caption: String = "",
    val imageBitmap: android.graphics.Bitmap? = null,
    val imageBase64: String? = null,
    val imageUri: String? = null,
    val alignment: VisualAlignment = VisualAlignment.CENTER,
    val targetWidthPt: Float = 260f,
    val targetHeightPt: Float = 160f
)

data class QuestionItemUi(
    val id: String,
    val questionNumber: Int,
    val sectionName: String,
    val questionType: String,
    val questionText: String,
    val options: List<String> = emptyList(),
    val matchingPairs: List<Pair<String, String>> = emptyList(),
    val correctAnswer: String,
    val markingGuide: String,
    val marks: Int,
    val difficulty: String = "Medium",
    val sourceCitation: SourceCitationUi? = null,
    val isManuallyEdited: Boolean = false,
    val visualAsset: VisualAssetRef? = null
)

data class QuestionSectionUi(
    val sectionName: String,
    val sectionInstruction: String,
    val sectionMarks: Int,
    val questions: List<QuestionItemUi>
)

data class WorkspaceStructuredResult(
    val title: String,
    val academicYear: String,
    val grade: String,
    val subject: String,
    val examType: String,
    val durationMinutes: Int,
    val totalMarks: Int,
    val isCurriculumVerified: Boolean = true,
    val validationSummary: String = "100% Curriculum Grounded • Zero Hallucinations • Mark Allocation Validated",
    val sourceCitations: List<SourceCitationUi> = emptyList(),
    val styleReferenceNote: String = "",
    val generalInstructions: List<String> = emptyList(),
    val sections: List<QuestionSectionUi> = emptyList(),
    val rawContentMarkdown: String = "",
    val originalQuestionPaperRequest: QuestionPaperRequest? = null,
    val originalWorksheetRequest: WorksheetRequest? = null,
    val savedHistoryId: Long? = null,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long = System.currentTimeMillis()
)

enum class ChatSender {
    USER, AI, SYSTEM
}

data class WorkspaceChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedActions: List<QuickActionType> = emptyList(),
    val structuredResult: WorkspaceStructuredResult? = null,
    val isGenerating: Boolean = false
)

data class QuestionPaperConfigState(
    val purpose: QuestionPurposeUi = QuestionPurposeUi.OFFICIAL_EXAM,
    val examType: ExamTypeUi = ExamPaperTypeUiDefault(),
    val academicYear: String = "2026–2027",
    val grade: String = "G5",
    val subject: String = "English",
    val sourceTier: SourceTier = SourceTier.PRIMARY_TEXTBOOK,
    val sourceTitle: String = "Ministry of Education Myanmar Grade 5 English (2024–2025)",
    val chapterUnit: String = "Unit 3: Healthy Food and Nutrition",
    val sectionTopic: String = "Countable/Uncountable Nouns & Food Pyramid",
    val pageRange: String = "p. 32–40",
    val totalMarks: Int = 50,
    val durationMinutes: Int = 60,
    val difficultyDistribution: String = "30% Easy, 50% Medium, 20% Challenge",
    val questionCount: Int = 10,
    val language: String = "English",
    val step: Int = 1
)

fun ExamPaperTypeUiDefault() = ExamTypeUi.MONTHLY_TEST

data class WorksheetConfigState(
    val academicYear: String = "2026–2027",
    val grade: String = "G5",
    val subject: String = "English",
    val sourceTier: SourceTier = SourceTier.PRIMARY_TEXTBOOK,
    val sourceTitle: String = "Ministry of Education Myanmar Grade 5 English (2024–2025)",
    val chapterUnit: String = "Unit 3: Healthy Food and Nutrition",
    val sectionTopic: String = "Countable/Uncountable Nouns & Food Pyramid",
    val learningObjective: String = "Identify food groups and apply quantifiers accurately.",
    val easyCount: Int = 5,
    val mediumCount: Int = 5,
    val challengeCount: Int = 2,
    val language: String = "English"
) {
    val totalQuestions: Int get() = easyCount + mediumCount + challengeCount
}

fun QuestionPaperConfigState.toQuestionPaperRequest(className: String = "Room A"): QuestionPaperRequest {
    val mappedPurpose = when (this.purpose) {
        QuestionPurposeUi.OFFICIAL_EXAM -> QuestionPurpose.OFFICIAL_EXAM
        QuestionPurposeUi.CLASSROOM_QUIZ -> QuestionPurpose.CLASSROOM_QUIZ
        QuestionPurposeUi.PRACTICE_REVISION -> QuestionPurpose.PRACTICE_REVISION
    }
    val mappedExamType = when (this.examType) {
        ExamTypeUi.PILOT_TEST -> ExamPaperType.PILOT_TEST
        ExamTypeUi.CET -> ExamPaperType.CET
        ExamTypeUi.MONTHLY_TEST -> ExamPaperType.MONTHLY_TEST
        ExamTypeUi.MID_TERM -> ExamPaperType.MID_TERM
        ExamTypeUi.FINAL_EXAM -> ExamPaperType.FINAL_EXAM
        ExamTypeUi.CUSTOM -> ExamPaperType.CUSTOM_EXAM
        ExamTypeUi.QUICK_QUIZ -> ExamPaperType.QUICK_QUIZ
        ExamTypeUi.COMPREHENSIVE_QUIZ -> ExamPaperType.COMPREHENSIVE_QUIZ
    }
    val mappedLanguage = when {
        this.language.contains("Myanmar", ignoreCase = true) -> "MYANMAR"
        this.language.contains("English", ignoreCase = true) -> "ENGLISH"
        else -> "MIXED"
    }

    return QuestionPaperRequest(
        purpose = mappedPurpose,
        examType = mappedExamType,
        academicYear = this.academicYear,
        grade = this.grade,
        className = className,
        subject = this.subject,
        sourceDocumentTitle = this.sourceTitle,
        chapterUnit = this.chapterUnit,
        sectionTopic = this.sectionTopic,
        pageRange = this.pageRange,
        totalMarks = this.totalMarks,
        durationMinutes = this.durationMinutes,
        questionCount = this.questionCount,
        language = mappedLanguage,
        referencePastPaperNotes = if (this.sourceTier == SourceTier.PAST_PAPER_REFERENCE) "Archive Reference" else ""
    )
}

fun WorksheetConfigState.toWorksheetRequest(className: String = "Room A"): WorksheetRequest {
    val mappedLanguage = when {
        this.language.contains("Myanmar", ignoreCase = true) -> "MYANMAR"
        this.language.contains("English", ignoreCase = true) -> "ENGLISH"
        else -> "MIXED"
    }

    return WorksheetRequest(
        academicYear = this.academicYear,
        grade = this.grade,
        className = className,
        subject = this.subject,
        chapterUnit = this.chapterUnit,
        sectionTopic = this.sectionTopic,
        pageRange = "p. 1-10",
        learningObjective = this.learningObjective,
        difficultyDistribution = DifficultyDistribution(
            easyCount = this.easyCount,
            mediumCount = this.mediumCount,
            challengeCount = this.challengeCount
        ),
        questionCount = this.totalQuestions,
        language = mappedLanguage
    )
}

fun QuestionPaperGenerationResult.toWorkspaceStructuredResult(request: QuestionPaperRequest): WorkspaceStructuredResult {
    return WorkspaceStructuredResult(
        title = this.title,
        academicYear = this.academicYear,
        grade = this.grade,
        subject = this.subject,
        examType = this.examType.displayName,
        durationMinutes = this.durationMinutes,
        totalMarks = this.totalMarks,
        isCurriculumVerified = this.isGroundedInCurriculum && this.validationReport.isValid,
        validationSummary = if (this.validationReport.isValid) {
            "100% Curriculum Grounded • ${this.allQuestions.size} Questions • Total ${this.totalMarks} Marks Validated"
        } else {
            "Validation Notice: ${this.validationReport.errors.joinToString("; ")}"
        },
        sourceCitations = this.sourceCitations.map {
            val tier = if (it.documentType.contains("PAST", ignoreCase = true) || it.documentType.contains("REFERENCE", ignoreCase = true)) {
                SourceTier.PAST_PAPER_REFERENCE
            } else if (it.documentType.contains("GUIDE", ignoreCase = true)) {
                SourceTier.SECONDARY_GUIDE
            } else {
                SourceTier.PRIMARY_TEXTBOOK
            }
            SourceCitationUi(
                documentTitle = it.documentTitle,
                gradeLevel = it.gradeLevel,
                subject = it.subject,
                chapterUnit = it.chapterUnit,
                sectionTopic = it.sectionTopic,
                pageRange = it.pageRange,
                tier = tier
            )
        },
        styleReferenceNote = this.styleReferenceNote,
        generalInstructions = this.generalInstructions,
        sections = this.sections.map { sec ->
            QuestionSectionUi(
                sectionName = sec.sectionName,
                sectionInstruction = sec.sectionInstruction,
                sectionMarks = sec.sectionMarks,
                questions = sec.questions.map { q ->
                    val qTier = if (q.sourceReference.sourcePriority == 4) SourceTier.PAST_PAPER_REFERENCE else if (q.sourceReference.sourcePriority == 2) SourceTier.SECONDARY_GUIDE else SourceTier.PRIMARY_TEXTBOOK
                    QuestionItemUi(
                        id = q.id,
                        questionNumber = q.questionNumber,
                        sectionName = q.sectionName,
                        questionType = q.questionType.displayName,
                        questionText = q.questionText,
                        options = q.options,
                        matchingPairs = q.matchingPairs,
                        correctAnswer = q.correctAnswer,
                        markingGuide = q.markingGuide,
                        marks = q.marks,
                        difficulty = q.difficulty.name,
                        sourceCitation = SourceCitationUi(
                            documentTitle = q.sourceReference.documentTitle,
                            gradeLevel = q.sourceReference.gradeLevel,
                            subject = q.sourceReference.subject,
                            chapterUnit = q.sourceReference.chapterUnit,
                            sectionTopic = q.sourceReference.sectionTopic,
                            pageRange = q.sourceReference.pageRange,
                            tier = qTier
                        )
                    )
                }
            )
        },
        rawContentMarkdown = "",
        originalQuestionPaperRequest = request
    )
}

fun WorksheetGenerationResult.toWorkspaceStructuredResult(request: WorksheetRequest): WorkspaceStructuredResult {
    return WorkspaceStructuredResult(
        title = this.title,
        academicYear = this.academicYear,
        grade = this.grade,
        subject = this.subject,
        examType = "Differentiated Worksheet",
        durationMinutes = 30,
        totalMarks = this.items.sumOf { it.marks },
        isCurriculumVerified = this.validationReport.isValid,
        validationSummary = if (this.validationReport.isValid) {
            "Curriculum Grounded • ${this.totalQuestions} Questions (${this.difficultySummary})"
        } else {
            "Validation Notice: ${this.validationReport.errors.joinToString("; ")}"
        },
        sourceCitations = this.sourceCitations.map {
            val tier = if (it.documentType.contains("PAST", ignoreCase = true) || it.documentType.contains("REFERENCE", ignoreCase = true)) {
                SourceTier.PAST_PAPER_REFERENCE
            } else if (it.documentType.contains("GUIDE", ignoreCase = true)) {
                SourceTier.SECONDARY_GUIDE
            } else {
                SourceTier.PRIMARY_TEXTBOOK
            }
            SourceCitationUi(
                documentTitle = it.documentTitle,
                gradeLevel = it.gradeLevel,
                subject = it.subject,
                chapterUnit = it.chapterUnit,
                sectionTopic = it.sectionTopic,
                pageRange = it.pageRange,
                tier = tier
            )
        },
        styleReferenceNote = "",
        generalInstructions = listOf(
            "Answer all questions on this practice worksheet.",
            "Review key concepts from ${this.chapterUnit}."
        ),
        sections = listOf(
            QuestionSectionUi(
                sectionName = "Part 1: Differentiated Practice Items",
                sectionInstruction = "Complete each task carefully based on the assigned curriculum scope.",
                sectionMarks = this.items.sumOf { it.marks },
                questions = this.items.map { q ->
                    val qTier = if (q.sourceReference.sourcePriority == 4) SourceTier.PAST_PAPER_REFERENCE else if (q.sourceReference.sourcePriority == 2) SourceTier.SECONDARY_GUIDE else SourceTier.PRIMARY_TEXTBOOK
                    QuestionItemUi(
                        id = q.id,
                        questionNumber = q.questionNumber,
                        sectionName = q.sectionName,
                        questionType = q.questionType.displayName,
                        questionText = q.questionText,
                        options = q.options,
                        matchingPairs = q.matchingPairs,
                        correctAnswer = q.correctAnswer,
                        markingGuide = q.markingGuide,
                        marks = q.marks,
                        difficulty = q.difficulty.name,
                        sourceCitation = SourceCitationUi(
                            documentTitle = q.sourceReference.documentTitle,
                            gradeLevel = q.sourceReference.gradeLevel,
                            subject = q.sourceReference.subject,
                            chapterUnit = q.sourceReference.chapterUnit,
                            sectionTopic = q.sourceReference.sectionTopic,
                            pageRange = q.sourceReference.pageRange,
                            tier = qTier
                        )
                    )
                }
            )
        ),
        rawContentMarkdown = "",
        originalWorksheetRequest = request
    )
}
