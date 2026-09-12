package com.example.data.ai

import com.example.data.local.entity.AiChatMessageEntity
import com.example.ui.screens.ai.workspace.WorkspaceStructuredResult
import java.util.Locale

/**
 * Robust Multi-turn Context & Intent Resolver for HCM-SMS AI Teacher Assistant (Phase 5).
 *
 * Capabilities:
 * 1. Resolves natural language user prompts in Myanmar, English, and Mixed language.
 * 2. Accurately identifies conversational intent (initial generation, surgical single-question edit,
 *    range regeneration, question addition, explanation, translation, worksheet/exam format conversion,
 *    and student report remarks).
 * 3. Preserves context continuity (Grade, Subject, Chapter/Unit, Sections, Active Structured Result)
 *    so teachers never have to repeat known parameters across turns.
 * 4. Extracts question numbers and ranges across standard ASCII and Myanmar numerals (၁, ၂, ၃...).
 * 5. Builds high-density sliding-window system & user prompts for Gemini AI.
 */
object ConversationalContextResolver {

    /**
     * Resolves intent and parameters from the user prompt, incorporating multi-turn context.
     */
    fun resolveIntent(
        prompt: String,
        activeStructuredResult: WorkspaceStructuredResult? = null,
        recentMessages: List<AiChatMessageEntity> = emptyList(),
        teacherProfile: TeacherProfileContext = TeacherProfileContext()
    ): ParsedIntentResult {
        val normalizedPrompt = normalizeMyanmarNumerals(prompt.trim())
        val lower = normalizedPrompt.lowercase(Locale.ROOT)

        // 1. Check for Explanation Intent (e.g. "explain why question 5 has this answer", "explain #3")
        if (isExplanationRequest(lower)) {
            val qNum = extractSingleQuestionNumber(lower)
            return ParsedIntentResult(
                intentType = ConversationIntentType.EXPLAIN_QUESTION,
                targetQuestionNumber = qNum ?: 1,
                targetGrade = activeStructuredResult?.grade ?: extractGrade(lower) ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5",
                targetSubject = activeStructuredResult?.subject ?: extractSubject(lower) ?: teacherProfile.assignedSubjects.firstOrNull() ?: "English",
                userPromptCleaned = normalizedPrompt
            )
        }

        // 2. Check for Translation Intent (e.g. "translate answer key to Myanmar", "translate to English")
        if (isTranslationRequest(lower)) {
            val targetLang = if (lower.contains("myanmar") || lower.contains("မြန်မာ") || lower.contains("burmese")) "MYANMAR" else "ENGLISH"
            return ParsedIntentResult(
                intentType = ConversationIntentType.TRANSLATE_CONTENT,
                targetLanguage = targetLang,
                targetGrade = activeStructuredResult?.grade ?: extractGrade(lower),
                targetSubject = activeStructuredResult?.subject ?: extractSubject(lower),
                userPromptCleaned = normalizedPrompt
            )
        }

        // 3. Check for Format Conversion Intent (e.g. "format this as a printable worksheet", "make this a quiz")
        if (isFormatConversionRequest(lower)) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.CONVERT_FORMAT,
                targetGrade = activeStructuredResult?.grade ?: extractGrade(lower) ?: "G5",
                targetSubject = activeStructuredResult?.subject ?: extractSubject(lower) ?: "English",
                userPromptCleaned = normalizedPrompt
            )
        }

        // 4. Check for Student Report Card Intent (e.g. "generate report comment for Aung Aung", "report remarks for Su Su")
        if (isReportCommentRequest(lower)) {
            val studentName = extractStudentName(prompt)
            return ParsedIntentResult(
                intentType = ConversationIntentType.CREATE_REPORT_COMMENT,
                targetStudentName = studentName,
                targetGrade = extractGrade(lower) ?: activeStructuredResult?.grade ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5",
                targetLanguage = if (lower.contains("english")) "ENGLISH" else "MYANMAR",
                userPromptCleaned = normalizedPrompt
            )
        }

        // 4b. Check for Admin Intelligence & Analytical Intents
        if (isSchoolAnalyticsRequest(lower)) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.ADMIN_SCHOOL_ANALYTICS,
                targetGrade = extractGrade(lower),
                userPromptCleaned = normalizedPrompt
            )
        }

        if (isAttendanceOverviewRequest(lower)) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.ADMIN_ATTENDANCE_OVERVIEW,
                targetGrade = extractGrade(lower),
                userPromptCleaned = normalizedPrompt
            )
        }

        if (isExamPerformanceRequest(lower)) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.ADMIN_EXAM_PERFORMANCE,
                targetGrade = extractGrade(lower) ?: activeStructuredResult?.grade ?: teacherProfile.assignedGrades.firstOrNull(),
                targetSubject = extractSubject(lower) ?: activeStructuredResult?.subject ?: teacherProfile.assignedSubjects.firstOrNull(),
                userPromptCleaned = normalizedPrompt
            )
        }

        if (isAtRiskStudentsRequest(lower)) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.ADMIN_AT_RISK_STUDENTS,
                targetGrade = extractGrade(lower) ?: activeStructuredResult?.grade ?: teacherProfile.assignedGrades.firstOrNull(),
                userPromptCleaned = normalizedPrompt
            )
        }

        if (isPolicyOverviewRequest(lower)) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.ADMIN_POLICY_OVERVIEW,
                userPromptCleaned = normalizedPrompt
            )
        }

        // 5. Check for Adding Questions (e.g. "add 2 more questions on vocabulary", "နောက်ထပ် ၂ ပုဒ် ထပ်ထည့်ပါ")
        if (isAddQuestionsRequest(lower)) {
            val count = extractQuestionCount(lower) ?: 2
            val qType = extractQuestionType(lower)
            val diff = extractDifficulty(lower)
            return ParsedIntentResult(
                intentType = ConversationIntentType.ADD_QUESTIONS,
                additionalCount = count,
                targetQuestionType = qType,
                targetDifficulty = diff,
                targetGrade = activeStructuredResult?.grade ?: extractGrade(lower) ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5",
                targetSubject = activeStructuredResult?.subject ?: extractSubject(lower) ?: teacherProfile.assignedSubjects.firstOrNull() ?: "English",
                targetChapterUnit = activeStructuredResult?.sourceCitations?.firstOrNull()?.chapterUnit ?: extractChapterUnit(normalizedPrompt),
                userPromptCleaned = normalizedPrompt
            )
        }

        // 6. Check for Range Regeneration (e.g. "make questions 8 to 10 harder", "regenerate 8-10 with higher difficulty")
        val range = extractQuestionRange(lower)
        if (range != null && (activeStructuredResult != null || containsFollowUpKeywords(lower))) {
            val diff = extractDifficulty(lower) ?: DifficultyLevel.CHALLENGE
            val qType = extractQuestionType(lower)
            return ParsedIntentResult(
                intentType = ConversationIntentType.REGENERATE_RANGE,
                targetQuestionRange = range,
                targetDifficulty = diff,
                targetQuestionType = qType,
                targetGrade = activeStructuredResult?.grade ?: extractGrade(lower) ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5",
                targetSubject = activeStructuredResult?.subject ?: extractSubject(lower) ?: teacherProfile.assignedSubjects.firstOrNull() ?: "English",
                targetChapterUnit = activeStructuredResult?.sourceCitations?.firstOrNull()?.chapterUnit ?: extractChapterUnit(normalizedPrompt),
                userPromptCleaned = normalizedPrompt
            )
        }

        // 7. Check for Single Question Partial Edit (e.g. "make number 4 easier", "change question 2 to multiple choice")
        val singleQNum = extractSingleQuestionNumber(lower)
        if (singleQNum != null && (activeStructuredResult != null || containsFollowUpKeywords(lower))) {
            val diff = extractDifficulty(lower)
            val qType = extractQuestionType(lower)
            return ParsedIntentResult(
                intentType = ConversationIntentType.PARTIAL_EDIT_QUESTION,
                targetQuestionNumber = singleQNum,
                targetDifficulty = diff,
                targetQuestionType = qType,
                targetGrade = activeStructuredResult?.grade ?: extractGrade(lower) ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5",
                targetSubject = activeStructuredResult?.subject ?: extractSubject(lower) ?: teacherProfile.assignedSubjects.firstOrNull() ?: "English",
                targetChapterUnit = activeStructuredResult?.sourceCitations?.firstOrNull()?.chapterUnit ?: extractChapterUnit(normalizedPrompt),
                userPromptCleaned = normalizedPrompt
            )
        }

        // 8. Check for Initial Creation Requests
        val grade = extractGrade(lower) ?: activeStructuredResult?.grade ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5"
        val subject = extractSubject(lower) ?: activeStructuredResult?.subject ?: teacherProfile.assignedSubjects.firstOrNull() ?: "English"
        val unit = extractChapterUnit(normalizedPrompt).ifEmpty {
            activeStructuredResult?.sourceCitations?.firstOrNull()?.chapterUnit ?: "Unit 3: Healthy Food and Nutrition"
        }
        val diff = extractDifficulty(lower) ?: DifficultyLevel.MIXED

        if (lower.contains("lesson plan") || lower.contains("သင်ခန်းစာစီမံချက်") || lower.contains("5e model")) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.CREATE_LESSON_PLAN,
                targetGrade = grade,
                targetSubject = subject,
                targetChapterUnit = unit,
                targetDifficulty = diff,
                userPromptCleaned = normalizedPrompt
            )
        }

        if (lower.contains("worksheet") || lower.contains("လေ့ကျင့်ခန်း") || lower.contains("practice sheet")) {
            val qCount = extractQuestionCount(lower) ?: 10
            return ParsedIntentResult(
                intentType = ConversationIntentType.CREATE_WORKSHEET,
                targetGrade = grade,
                targetSubject = subject,
                targetChapterUnit = unit,
                targetDifficulty = diff,
                additionalCount = qCount,
                userPromptCleaned = normalizedPrompt
            )
        }

        if (lower.contains("quiz") || lower.contains("စစ်ဆေးမေးခွန်း") || lower.contains("quick quiz")) {
            val qCount = extractQuestionCount(lower) ?: 5
            return ParsedIntentResult(
                intentType = ConversationIntentType.CREATE_QUIZ,
                targetGrade = grade,
                targetSubject = subject,
                targetChapterUnit = unit,
                targetDifficulty = diff,
                additionalCount = qCount,
                userPromptCleaned = normalizedPrompt
            )
        }

        if (lower.contains("exam") || lower.contains("question paper") || lower.contains("test paper") || lower.contains("မေးခွန်းလွှာ") || lower.contains("questions for")) {
            val qCount = extractQuestionCount(lower) ?: 10
            return ParsedIntentResult(
                intentType = ConversationIntentType.CREATE_QUESTION_PAPER,
                targetGrade = grade,
                targetSubject = subject,
                targetChapterUnit = unit,
                targetDifficulty = diff,
                additionalCount = qCount,
                userPromptCleaned = normalizedPrompt
            )
        }

        if (lower.contains("activity") || lower.contains("game") || lower.contains("လှုပ်ရှားမှု")) {
            return ParsedIntentResult(
                intentType = ConversationIntentType.CREATE_ACTIVITY,
                targetGrade = grade,
                targetSubject = subject,
                targetChapterUnit = unit,
                userPromptCleaned = normalizedPrompt
            )
        }

        // 9. Default to General Pedagogical / Multi-Turn Conversation
        return ParsedIntentResult(
            intentType = ConversationIntentType.GENERAL_PEDAGOGY,
            targetGrade = grade,
            targetSubject = subject,
            targetChapterUnit = unit,
            userPromptCleaned = normalizedPrompt
        )
    }

    /**
     * Converts Myanmar numerals (၀-၉) to standard ASCII digits (0-9).
     */
    fun normalizeMyanmarNumerals(input: String): String {
        val myanmarDigits = charArrayOf('၀', '၁', '၂', '၃', '၄', '၅', '၆', '၇', '၈', '၉')
        var result = input
        for (i in 0..9) {
            result = result.replace(myanmarDigits[i], ('0' + i))
        }
        return result
    }

    private fun isExplanationRequest(lower: String): Boolean {
        return lower.contains("explain") || lower.contains("explanation") ||
                lower.contains("why is") || lower.contains("why does") ||
                lower.contains("ဘာကြောင့်") || lower.contains("ရှင်းပြပါ") ||
                lower.contains("အဖြေရှင်းလင်းချက်")
    }

    private fun isTranslationRequest(lower: String): Boolean {
        return lower.contains("translate") || lower.contains("translation") ||
                lower.contains("ဘာသာပြန်") || lower.contains("myanmar language") ||
                lower.contains("english language") || lower.contains("မြန်မာလို") || lower.contains("အင်္ဂလိပ်လို")
    }

    private fun isFormatConversionRequest(lower: String): Boolean {
        return (lower.contains("format this as") || lower.contains("make this a worksheet") ||
                lower.contains("convert to worksheet") || lower.contains("convert to exam") ||
                lower.contains("printable worksheet") || lower.contains("လေ့ကျင့်ခန်းစာရွက်အဖြစ် ပြောင်း"))
    }

    private fun isReportCommentRequest(lower: String): Boolean {
        return lower.contains("report comment") || lower.contains("report card") ||
                lower.contains("teacher remark") || lower.contains("student remark") ||
                lower.contains("အစီရင်ခံစာ") || lower.contains("ကျောင်းသားမှတ်ချက်") ||
                (lower.contains("comment for") && !lower.contains("question"))
    }

    private fun isSchoolAnalyticsRequest(lower: String): Boolean {
        return lower.contains("school analytics") || lower.contains("school performance") ||
                lower.contains("overall analytics") || lower.contains("ကျောင်း စာရင်းအင်း") ||
                lower.contains("ကျောင်း အချက်အလက်") || lower.contains("cohort analytics")
    }

    private fun isAttendanceOverviewRequest(lower: String): Boolean {
        return lower.contains("attendance overview") || lower.contains("attendance summary") ||
                lower.contains("attendance rate") || lower.contains("ကျောင်းတက်ရောက်မှု") ||
                lower.contains("ပျက်ကွက်မှု") || lower.contains("absenteeism")
    }

    private fun isExamPerformanceRequest(lower: String): Boolean {
        return lower.contains("exam performance") || lower.contains("academic performance") ||
                lower.contains("exam overview") || lower.contains("စာမေးပွဲ ရလဒ်") ||
                lower.contains("subject performance") || lower.contains("class performance")
    }

    private fun isAtRiskStudentsRequest(lower: String): Boolean {
        return lower.contains("at risk") || lower.contains("at-risk") ||
                lower.contains("struggling students") || lower.contains("failing students") ||
                lower.contains("ကျရှုံးနိုင်ခြေ") || lower.contains("အကူအညီလိုအပ်သော ကျောင်းသား") ||
                lower.contains("support needed")
    }

    private fun isPolicyOverviewRequest(lower: String): Boolean {
        return lower.contains("grading policy") || lower.contains("pass mark") ||
                lower.contains("distinction mark") || lower.contains("အဆင့်သတ်မှတ်ချက် မူဝါဒ")
    }

    private fun isAddQuestionsRequest(lower: String): Boolean {
        return lower.contains("add ") || lower.contains("include more") ||
                lower.contains("more questions") || lower.contains("ထပ်ထည့်") ||
                lower.contains("နောက်ထပ်")
    }

    private fun containsFollowUpKeywords(lower: String): Boolean {
        return lower.contains("make ") || lower.contains("change ") || lower.contains("regenerate") ||
                lower.contains("replace ") || lower.contains("harder") || lower.contains("easier") ||
                lower.contains("ပြင်ဆင်") || lower.contains("ပြောင်းလဲ") || lower.contains("ခက်") || lower.contains("လွယ်")
    }

    /**
     * Extracts a question range like "8-10", "8 to 10", "8 မှ 10", "questions 8 to 10".
     */
    fun extractQuestionRange(lower: String): Pair<Int, Int>? {
        val patterns = listOf(
            Regex("""(?:questions?|no\.?|items?|မေးခွန်း)?\s*(\d+)\s*(?:to|-|through|မှ|ထိ)\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*[-–]\s*(\d+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val start = match.groupValues[1].toIntOrNull() ?: continue
                val end = match.groupValues[2].toIntOrNull() ?: continue
                if (start <= end && start > 0 && end <= 100) {
                    return Pair(start, end)
                }
            }
        }
        return null
    }

    /**
     * Extracts a single target question number, e.g. "number 4", "question 2", "#4", "မေးခွန်း ၄", "နံပါတ် ၄".
     */
    fun extractSingleQuestionNumber(lower: String): Int? {
        val patterns = listOf(
            Regex("""(?:number|no\.?|question|q|item|မေးခွန်းနံပါတ်|နံပါတ်|မေးခွန်း)\s*#?\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""#(\d+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null && num in 1..100) {
                    return num
                }
            }
        }
        return null
    }

    fun extractDifficulty(lower: String): DifficultyLevel? {
        return when {
            lower.contains("easier") || lower.contains("easy") || lower.contains("simple") ||
                    lower.contains("လွယ်") || lower.contains("အခြေခံ") -> DifficultyLevel.EASY
            lower.contains("harder") || lower.contains("hard") || lower.contains("challenge") ||
                    lower.contains("challenging") || lower.contains("difficult") ||
                    lower.contains("ခက်") || lower.contains("စိန်ခေါ်မှု") -> DifficultyLevel.CHALLENGE
            lower.contains("medium") || lower.contains("moderate") || lower.contains("အလယ်အလတ်") -> DifficultyLevel.MEDIUM
            else -> null
        }
    }

    fun extractQuestionType(lower: String): QuestionType? {
        return when {
            lower.contains("multiple choice") || lower.contains("mcq") || lower.contains("ရွေးချယ်") -> QuestionType.MCQ
            lower.contains("matching") || lower.contains("match") || lower.contains("တွဲဖက်") -> QuestionType.MATCHING
            lower.contains("fill in") || lower.contains("blank") || lower.contains("ကွက်လပ်") -> QuestionType.FILL_IN_BLANKS
            lower.contains("true") || lower.contains("false") || lower.contains("မှန်/မှား") -> QuestionType.TRUE_FALSE
            lower.contains("short answer") || lower.contains("brief") || lower.contains("တိုတိုနှင့်ရှင်းရှင်း") -> QuestionType.SHORT_ANSWER
            lower.contains("essay") || lower.contains("long answer") || lower.contains("စာစီစာကုံး") || lower.contains("အကျယ်") -> QuestionType.ESSAY
            lower.contains("diagram") || lower.contains("label") || lower.contains("ပုံကြမ်း") -> QuestionType.LABEL_DIAGRAM
            lower.contains("map") || lower.contains("မြေပုံ") -> QuestionType.MAP_BASED
            else -> null
        }
    }

    fun extractQuestionCount(lower: String): Int? {
        val patterns = listOf(
            Regex("""(\d+)\s*(?:questions?|items?|tasks?|problems?|ပုဒ်|ခု|မေးခွန်း)""", RegexOption.IGNORE_CASE),
            Regex("""add\s*(\d+)""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val count = match.groupValues[1].toIntOrNull()
                if (count != null && count in 1..50) return count
            }
        }
        return null
    }

    fun extractGrade(lower: String): String? {
        val match = Regex("""\b(KG|G[1-9]|G1[0-2]|Grade\s*\d+)\b""", RegexOption.IGNORE_CASE).find(lower)
        if (match != null) {
            val raw = match.value.uppercase()
            return if (raw.startsWith("GRADE")) "G" + raw.removePrefix("GRADE").trim() else raw
        }
        return null
    }

    fun extractSubject(lower: String): String? {
        val subjects = listOf("English", "Mathematics", "Science", "Myanmar", "Social Studies", "Physics", "Chemistry", "Biology", "Economics", "History", "Geography")
        for (sub in subjects) {
            if (lower.contains(sub.lowercase())) return sub
        }
        if (lower.contains("သင်္ချာ")) return "Mathematics"
        if (lower.contains("အင်္ဂလိပ်") || lower.contains("english")) return "English"
        if (lower.contains("သိပ္ပံ") || lower.contains("science")) return "Science"
        if (lower.contains("မြန်မာစာ") || lower.contains("myanmar")) return "Myanmar"
        if (lower.contains("လူမှုရေး") || lower.contains("social")) return "Social Studies"
        return null
    }

    fun extractChapterUnit(prompt: String): String {
        val unitMatch = Regex("""(?:Unit|Chapter|အခန်း)\s*(\d+)(?::\s*([^\n,]+))?""", RegexOption.IGNORE_CASE).find(prompt)
        if (unitMatch != null) {
            val unitNum = unitMatch.groupValues[1]
            val subtitle = unitMatch.groupValues.getOrNull(2)?.trim()
            return if (!subtitle.isNullOrBlank()) "Unit $unitNum: $subtitle" else "Unit $unitNum"
        }
        return ""
    }

    fun extractStudentName(prompt: String): String {
        val patterns = listOf(
            Regex("""(?:for\s+student|for|student|ကျောင်းသား|ကျောင်းသူ)\s+([A-Za-z\s]+|[\u1000-\u109F\s]+)""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z][a-z]+\s+[A-Z][a-z]+)""")
        )
        for (pattern in patterns) {
            val match = pattern.find(prompt)
            if (match != null) {
                var candidate = match.groupValues[1].trim()
                if (candidate.startsWith("student ", ignoreCase = true)) {
                    candidate = candidate.removePrefix("student ").removePrefix("Student ").trim()
                }
                if (candidate.length in 2..30 && !candidate.equals("english", true) && !candidate.equals("unit", true)) {
                    return candidate
                }
            }
        }
        return "Student"
    }

    /**
     * Builds structured prompt for Gemini with sliding window and teacher context injection.
     */
    fun buildSlidingWindowPrompt(
        userPrompt: String,
        teacherProfile: TeacherProfileContext,
        recentTurns: List<AiChatMessageEntity>,
        activeStructuredResult: WorkspaceStructuredResult?,
        maxTurns: Int = 8
    ): String {
        return buildString {
            appendLine("### ACTIVE TEACHER CONTEXT:")
            appendLine("- Teacher Name: ${teacherProfile.teacherName} (${teacherProfile.teacherCode})")
            appendLine("- Qualifications: ${teacherProfile.qualifications}")
            appendLine("- Assigned Grades: ${teacherProfile.assignedGrades.joinToString(", ")}")
            appendLine("- Assigned Classes: ${teacherProfile.assignedClasses.joinToString(", ")}")
            appendLine("- Assigned Subjects: ${teacherProfile.assignedSubjects.joinToString(", ")}")
            appendLine("- Academic Year: ${teacherProfile.academicYear}")
            appendLine()

            if (activeStructuredResult != null) {
                appendLine("### CURRENT WORKSPACE STRUCTURED STATE:")
                appendLine("- Title: ${activeStructuredResult.title}")
                appendLine("- Grade & Subject: ${activeStructuredResult.grade} ${activeStructuredResult.subject}")
                appendLine("- Total Marks: ${activeStructuredResult.totalMarks} | Duration: ${activeStructuredResult.durationMinutes} mins")
                appendLine("- Total Questions: ${activeStructuredResult.sections.sumOf { it.questions.size }}")
                val unitTopic = activeStructuredResult.sourceCitations.firstOrNull()?.chapterUnit ?: ""
                if (unitTopic.isNotBlank()) appendLine("- Active Curriculum Unit: $unitTopic")
                appendLine()
            }

            if (recentTurns.isNotEmpty()) {
                appendLine("### RECENT CONVERSATION TURNS (Sliding Window):")
                val window = recentTurns.takeLast(maxTurns)
                for (turn in window) {
                    appendLine("${turn.senderRole}: ${turn.content}")
                }
                appendLine()
            }

            appendLine("### CURRENT TEACHER INSTRUCTION:")
            appendLine(userPrompt)
        }
    }
}
