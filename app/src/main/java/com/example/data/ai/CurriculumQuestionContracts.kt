package com.example.data.ai

/**
 * Factual contracts and request/response specifications for the
 * HCM-SMS Curriculum-Grounded Question Paper & Worksheet Engine (Phase 3).
 *
 * Core Principle: Approved Myanmar KG-G12 Textbooks & School Materials are the
 * primary source of truth. Past question papers are strictly REFERENCE ONLY.
 */

enum class QuestionPurpose(val displayName: String) {
    OFFICIAL_EXAM("Official Examination"),
    CLASSROOM_QUIZ("Classroom Quiz"),
    PRACTICE_REVISION("Practice & Revision"),
    WORKSHEET("Differentiated Worksheet")
}

enum class ExamPaperType(val displayName: String, val defaultDuration: Int, val defaultMarks: Int) {
    PILOT_TEST("Pilot Test", 45, 50),
    CET("Continuous Evaluation Test (CET)", 45, 50),
    MONTHLY_TEST("Monthly Examination", 60, 50),
    MID_TERM("Mid-Term Examination", 90, 100),
    FINAL_EXAM("Final Semester Examination", 120, 100),
    CUSTOM_EXAM("Custom Examination", 60, 50),
    QUICK_QUIZ("Quick Classroom Quiz", 15, 10),
    COMPREHENSIVE_QUIZ("Comprehensive Unit Quiz", 30, 25),
    REVISION_QUIZ("Revision Quiz", 30, 20),
    PRACTICE_QUESTIONS("Practice Questions", 30, 20),
    HOMEWORK_PRACTICE("Homework Practice", 45, 25)
}

enum class DifficultyLevel(val displayName: String) {
    EASY("Easy (Recall & Recognition)"),
    MEDIUM("Medium (Understanding & Application)"),
    CHALLENGE("Challenge (Higher-Order Thinking & Synthesis)"),
    MIXED("Mixed Difficulty")
}

enum class QuestionType(val displayName: String) {
    MCQ("Multiple Choice"),
    MATCHING("Matching"),
    FILL_IN_BLANKS("Fill in the Blanks"),
    TRUE_FALSE("True / False"),
    SHORT_ANSWER("Short Answer"),
    STRUCTURED("Structured Question"),
    DESCRIPTIVE("Descriptive Question"),
    ESSAY("Essay / Long Answer"),
    LABEL_DIAGRAM("Label the Diagram"),
    MAP_BASED("Map-based Question"),
    PICTURE_BASED("Picture-based Question")
}

enum class EducationalAssetType(val displayName: String) {
    PICTURE("Picture / Illustration"),
    DIAGRAM("Scientific / Technical Diagram"),
    LABEL_DIAGRAM("Diagram with Missing Labels"),
    MAP("Geographical / Regional Map"),
    CHART("Statistical / Conceptual Chart")
}

/**
 * Educational visual asset specification for picture/diagram/map/label questions.
 */
data class QuestionAssetRequirement(
    val type: EducationalAssetType,
    val topic: String,
    val description: String,
    val labelsRequired: List<String> = emptyList(),
    val layoutPosition: String = "TOP"
)

/**
 * Traceability metadata linking a generated question directly to its approved curriculum source chunk.
 */
data class QuestionSourceReference(
    val documentTitle: String,
    val gradeLevel: String,
    val subject: String,
    val chapterUnit: String,
    val sectionTopic: String,
    val pageRange: String,
    val sourcePriority: Int = 1 // 1 = Myanmar Textbook, 2 = Teacher Guide, 3 = School International Material, 4 = Reference
)

/**
 * Active school context to avoid asking teachers to re-enter known parameters.
 */
data class ActiveSchoolContext(
    val academicYear: String = "2026-2027",
    val grade: String = "G5",
    val className: String = "Room A",
    val subject: String = "English",
    val teacherId: Long? = null,
    val teacherUsername: String = ""
)

/**
 * Configuration for a section in an official examination paper.
 */
data class SectionConfig(
    val sectionName: String, // e.g. "Section A", "Section B"
    val sectionInstruction: String, // e.g. "Choose the correct answer for each question."
    val questionType: QuestionType,
    val questionCount: Int,
    val marksPerQuestion: Int,
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM
) {
    val totalSectionMarks: Int get() = questionCount * marksPerQuestion
}

/**
 * Differentiated difficulty distribution for worksheets.
 */
data class DifficultyDistribution(
    val easyCount: Int = 5,
    val mediumCount: Int = 5,
    val challengeCount: Int = 2
) {
    val totalCount: Int get() = easyCount + mediumCount + challengeCount
}

/**
 * Input request to generate an Official Question Paper or Classroom Quiz.
 */
data class QuestionPaperRequest(
    val purpose: QuestionPurpose = QuestionPurpose.OFFICIAL_EXAM,
    val examType: ExamPaperType = ExamPaperType.MONTHLY_TEST,
    val academicYear: String = "2026-2027",
    val grade: String,
    val className: String = "",
    val subject: String,
    val sourceDocumentId: Long? = null,
    val sourceDocumentTitle: String = "",
    val chapterUnit: String,
    val sectionTopic: String = "",
    val pageRange: String = "",
    val totalMarks: Int = 50,
    val durationMinutes: Int = 60,
    val difficulty: DifficultyLevel = DifficultyLevel.MIXED,
    val questionCount: Int = 10,
    val sections: List<SectionConfig> = emptyList(),
    val language: String = "MYANMAR", // "MYANMAR", "ENGLISH", "MIXED"
    val referencePastPaperId: Long? = null,
    val referencePastPaperNotes: String = "",
    val generationSeed: Long = 0L
)

/**
 * Input request to generate a Differentiated Worksheet.
 */
data class WorksheetRequest(
    val academicYear: String = "2026-2027",
    val grade: String,
    val className: String = "",
    val subject: String,
    val sourceDocumentId: Long? = null,
    val chapterUnit: String,
    val sectionTopic: String = "",
    val pageRange: String = "",
    val learningObjective: String = "",
    val difficultyDistribution: DifficultyDistribution = DifficultyDistribution(5, 5, 2),
    val questionTypes: List<QuestionType> = listOf(QuestionType.FILL_IN_BLANKS, QuestionType.SHORT_ANSWER, QuestionType.MCQ),
    val questionCount: Int = 12,
    val language: String = "MYANMAR", // "MYANMAR", "ENGLISH", "MIXED"
    val generationSeed: Long = 0L
)

/**
 * A single generated question with strict grounding, answer key, and traceability.
 */
data class GeneratedQuestion(
    val id: String,
    val questionNumber: Int,
    val sectionName: String = "Section A",
    val questionType: QuestionType,
    val questionText: String,
    val options: List<String> = emptyList(), // For MCQ
    val matchingPairs: List<Pair<String, String>> = emptyList(), // For MATCHING
    val correctAnswer: String = "", // For auto-marking / answer key
    val markingGuide: String = "", // Detailed grading rubrics for teacher
    val marks: Int = 1,
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val sourceReference: QuestionSourceReference,
    val assetRequirement: QuestionAssetRequirement? = null
)

/**
 * A grouped section inside the generated examination paper.
 */
data class QuestionSectionResult(
    val sectionName: String,
    val sectionInstruction: String,
    val sectionMarks: Int,
    val questions: List<GeneratedQuestion>
)

/**
 * Comprehensive validation report checking marks, question counts, answers, and curriculum boundaries.
 */
data class ValidationReport(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val totalCalculatedMarks: Int = 0,
    val targetMarks: Int = 0,
    val validatedQuestionCount: Int = 0
)

/**
 * Generated Output of an Official Question Paper or Classroom Quiz.
 */
data class QuestionPaperGenerationResult(
    val title: String,
    val academicYear: String,
    val grade: String,
    val subject: String,
    val examType: ExamPaperType,
    val durationMinutes: Int,
    val totalMarks: Int,
    val generalInstructions: List<String>,
    val sections: List<QuestionSectionResult>,
    val allQuestions: List<GeneratedQuestion>,
    val sourceCitations: List<CurriculumSourceCitation>,
    val styleReferenceNote: String = "",
    val isGroundedInCurriculum: Boolean = true,
    val answerKey: List<GeneratedQuestion> = allQuestions,
    val validationReport: ValidationReport
)

/**
 * Generated Output of a Differentiated Worksheet.
 */
data class WorksheetGenerationResult(
    val title: String,
    val academicYear: String,
    val grade: String,
    val subject: String,
    val chapterUnit: String,
    val sectionTopic: String,
    val learningObjectives: String,
    val difficultySummary: String,
    val totalQuestions: Int,
    val items: List<GeneratedQuestion>,
    val sourceCitations: List<CurriculumSourceCitation>,
    val answerKey: List<GeneratedQuestion> = items,
    val validationReport: ValidationReport
)
