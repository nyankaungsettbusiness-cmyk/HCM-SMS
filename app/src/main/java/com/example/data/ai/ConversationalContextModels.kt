package com.example.data.ai

import com.example.data.local.entity.UserRole
import com.example.ui.screens.ai.workspace.WorkspaceStructuredResult

/**
 * Models and data structures for Phase 5: Multi-Turn Conversational AI Engine & Teacher Context.
 */

data class TeacherProfileContext(
    val teacherUsername: String = "teacher",
    val teacherName: String = "Daw Khin Myo",
    val role: UserRole = UserRole.TEACHER,
    val teacherCode: String = "T-001",
    val qualifications: String = "B.Ed (English), Dip. in Educational Leadership",
    val assignedGrades: List<String> = listOf("G5", "G6"),
    val assignedClasses: List<String> = listOf("Room A", "Room B"),
    val assignedSubjects: List<String> = listOf("English", "Science"),
    val academicYear: String = "2026-2027"
)

data class ConversationalMessageItem(
    val id: Long = 0,
    val senderRole: String, // "USER", "ASSISTANT", "SYSTEM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT", // "TEXT", "QUESTION_PAPER", "WORKSHEET", "REPORT_COMMENT", "LESSON_PLAN"
    val metadataJson: String = "{}"
)

enum class ConversationIntentType {
    CREATE_QUESTION_PAPER,
    CREATE_WORKSHEET,
    CREATE_LESSON_PLAN,
    CREATE_QUIZ,
    CREATE_REPORT_COMMENT,
    CREATE_ACTIVITY,
    CREATE_TRANSLATION,
    PARTIAL_EDIT_QUESTION,      // e.g. "make number 4 easier", "change question 2 to multiple choice"
    REGENERATE_RANGE,           // e.g. "make questions 8-10 harder"
    ADD_QUESTIONS,              // e.g. "add 2 more questions on vocabulary"
    EXPLAIN_QUESTION,           // e.g. "explain why question 5 has this answer"
    TRANSLATE_CONTENT,          // e.g. "translate the answer key to Myanmar"
    CONVERT_FORMAT,             // e.g. "format this as a printable worksheet"
    STUDENT_FACTS_ANALYSIS,     // e.g. "analyze student Aung Aung's progress"
    ADMIN_SCHOOL_ANALYTICS,     // e.g. "school analytics", "overall school performance"
    ADMIN_ATTENDANCE_OVERVIEW,  // e.g. "attendance overview", "school attendance trends"
    ADMIN_EXAM_PERFORMANCE,     // e.g. "exam performance", "class academic overview"
    ADMIN_AT_RISK_STUDENTS,     // e.g. "show at-risk students", "struggling students"
    ADMIN_POLICY_OVERVIEW,      // e.g. "school grading policy", "grading rules"
    GENERAL_PEDAGOGY            // General educational advice, greetings, Q&A
}

data class ParsedIntentResult(
    val intentType: ConversationIntentType,
    val targetGrade: String? = null,
    val targetSubject: String? = null,
    val targetChapterUnit: String? = null,
    val targetSectionTopic: String? = null,
    val targetQuestionNumber: Int? = null,
    val targetQuestionRange: Pair<Int, Int>? = null,
    val targetDifficulty: DifficultyLevel? = null,
    val targetQuestionType: QuestionType? = null,
    val additionalCount: Int? = null,
    val targetStudentName: String? = null,
    val targetLanguage: String = "MYANMAR",
    val userPromptCleaned: String = ""
)

data class ConversationalTurnResult(
    val assistantResponseText: String,
    val intentType: ConversationIntentType,
    val updatedStructuredResult: WorkspaceStructuredResult? = null,
    val reportCardResult: ReportCardAiResult? = null,
    val sourceCitations: List<CurriculumSourceCitation> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
