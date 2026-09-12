package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AiToolCategory(val displayName: String) {
    LESSON_PLAN("Lesson Planning"),
    WORKSHEET("Worksheet Generator"),
    EXAM("Exam Generator"),
    QUESTION_BANK("Question Bank"),
    TEACHING_MATERIALS("Teaching Materials"),
    REPORT_WRITING("Report Writing"),
    TRANSLATION("Translation"),
    CLASSROOM_ACTIVITIES("Classroom Activities"),
    AI_CHAT("AI Chat")
}

@Entity(tableName = "ai_history")
data class AiHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val title: String,
    val generatedResult: String,
    val category: String, // AiToolCategory name
    val teacherUsername: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val grade: String = "All",
    val subject: String = "General",
    val topic: String = "",
    val difficulty: String = "Medium"
)

@Entity(tableName = "ai_saved_questions")
data class AiSavedQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionText: String,
    val optionsJson: String = "", // JSON array or pipe separated
    val correctAnswer: String = "",
    val questionType: String, // MCQ, TRUE_FALSE, MATCHING, SHORT_ANSWER, etc.
    val grade: String,
    val subject: String,
    val topic: String,
    val difficulty: String,
    val marks: Int = 1,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_settings")
data class AiSettingEntity(
    @PrimaryKey
    val id: Int = 1,
    val preferredProvider: String = "Gemini 2.5 Flash",
    val defaultLanguage: String = "English & Myanmar",
    val defaultGrade: String = "G5",
    val defaultDifficulty: String = "Medium",
    val maxOutputLength: Int = 2000,
    val apiKeyOverride: String = "",
    val isAiModuleEnabled: Boolean = true
)

// --- Curriculum Knowledge Store Entities ---

enum class CurriculumDocumentType(val displayName: String) {
    MYANMAR_TEXTBOOK("Myanmar KG-G12 Textbook"),
    TEACHER_GUIDE("Teacher Guide / Syllabus"),
    CAMBRIDGE_CEFR("Cambridge / CEFR Framework"),
    SCHOOL_MATERIAL("School Approved Teaching Material"),
    EXAM_REFERENCE("Previous Exam Reference (Style Only)")
}

@Entity(tableName = "curriculum_documents")
data class CurriculumDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val gradeLevel: String, // e.g. "KG", "G1", "G2" ... "G12"
    val subject: String, // e.g. "English", "Mathematics", "Science", "Myanmar", "Social Studies", "Physics", "Chemistry", "Biology", "Economics"
    val documentType: String = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
    val authorOrPublisher: String = "Ministry of Education / School",
    val isApproved: Boolean = true,
    val isSystemDefault: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "curriculum_chunks",
    indices = [
        androidx.room.Index(value = ["gradeLevel", "subject"]),
        androidx.room.Index(value = ["chapterUnit"])
    ]
)
data class CurriculumChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long = 0,
    val gradeLevel: String, // e.g. "G5"
    val subject: String, // e.g. "English"
    val chapterUnit: String, // e.g. "Unit 3: Healthy Food"
    val sectionTopic: String, // e.g. "Vocabulary & Reading Comprehension"
    val pageRange: String = "", // e.g. "pp. 32-36"
    val content: String, // The actual curriculum text & teaching points
    val learningObjectives: String = "",
    val vocabularyWords: String = "",
    val keywords: String = "",
    val sourceReference: String = "", // e.g. "Myanmar Basic Education G5 English, Unit 3, pp. 32-36"
    val createdAt: Long = System.currentTimeMillis()
)

// --- Conversational Chat Memory Entities ---

@Entity(tableName = "ai_chat_sessions")
data class AiChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionTitle: String,
    val userRole: String, // "SUPER_ADMIN", "ADMIN", "TEACHER", "OFFICE_STAFF"
    val teacherUsername: String,
    val academicYear: String = "2026-2027",
    val grade: String = "All",
    val subject: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_chat_messages",
    indices = [
        androidx.room.Index(value = ["sessionId"])
    ]
)
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val senderRole: String, // "USER", "ASSISTANT", "SYSTEM"
    val content: String,
    val messageType: String = "TEXT", // "TEXT", "QUESTION_PAPER", "WORKSHEET", "REPORT_COMMENT", "LESSON_PLAN", "CHART"
    val metadataJson: String = "{}", // Contains citation, grade/subject parameters, etc.
    val timestamp: Long = System.currentTimeMillis()
)
