package com.example.data.repository

import com.example.data.ai.*
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.data.service.GeminiAiEngine
import com.example.ui.screens.ai.workspace.WorkspaceStructuredResult
import com.example.ui.screens.ai.workspace.toWorkspaceStructuredResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import java.util.Locale

class AiAssistantRepository(
    private val aiDao: AiDao,
    private val aiChatDao: AiChatDao? = null,
    private val curriculumDao: CurriculumKnowledgeDao? = null,
    private val studentDao: StudentDao? = null,
    private val teacherDao: TeacherDao? = null,
    private val marksDao: MarksDao? = null,
    private val assessmentDao: AssessmentDao? = null,
    private val holisticDao: HolisticDao? = null,
    private val attendanceDao: AttendanceDao? = null,
    private val schoolPolicyDao: SchoolPolicyDao? = null
) {
    private val aiEngine = GeminiAiEngine()
    private val curriculumQuestionEngine = curriculumDao?.let { CurriculumQuestionEngine(it) }
    private val reportCardAiEngine = ReportCardAiEngine(aiEngine)

    val aiDataToolsBridge: AiDataToolsBridge? = if (studentDao != null && teacherDao != null && marksDao != null &&
        assessmentDao != null && holisticDao != null && attendanceDao != null &&
        schoolPolicyDao != null && curriculumDao != null) {
        AiDataToolsBridge(
            studentDao = studentDao,
            teacherDao = teacherDao,
            marksDao = marksDao,
            assessmentDao = assessmentDao,
            holisticDao = holisticDao,
            attendanceDao = attendanceDao,
            schoolPolicyDao = schoolPolicyDao,
            curriculumKnowledgeDao = curriculumDao
        )
    } else null

    val studentFactsAggregator: StudentFactsAggregator? = if (studentDao != null && assessmentDao != null &&
        marksDao != null && holisticDao != null && attendanceDao != null && schoolPolicyDao != null) {
        StudentFactsAggregator(
            studentDao = studentDao,
            assessmentDao = assessmentDao,
            marksDao = marksDao,
            holisticDao = holisticDao,
            attendanceDao = attendanceDao,
            schoolPolicyDao = schoolPolicyDao
        )
    } else null

    val allHistory: Flow<List<AiHistoryEntity>> = aiDao.getAllHistory()
    val favoriteHistory: Flow<List<AiHistoryEntity>> = aiDao.getFavoriteHistory()
    val allQuestions: Flow<List<AiSavedQuestionEntity>> = aiDao.getAllQuestions()
    val aiSettingsFlow: Flow<AiSettingEntity?> = aiDao.getAiSettingsFlow()

    // --- Chat Session & Message Persistence (Phase 5) ---

    val allSessions: Flow<List<AiChatSessionEntity>>
        get() = aiChatDao?.getAllSessionsFlow() ?: emptyFlow()

    fun getSessionsForUser(username: String): Flow<List<AiChatSessionEntity>> {
        return aiChatDao?.getSessionsForUserFlow(username) ?: emptyFlow()
    }

    suspend fun getSessionById(sessionId: Long): AiChatSessionEntity? {
        return aiChatDao?.getSessionById(sessionId)
    }

    suspend fun createChatSession(
        title: String,
        userRole: UserRole,
        username: String,
        academicYear: String = "2026-2027",
        grade: String = "All",
        subject: String = "General"
    ): Long {
        val session = AiChatSessionEntity(
            sessionTitle = title,
            userRole = userRole.name,
            teacherUsername = username,
            academicYear = academicYear,
            grade = grade,
            subject = subject,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return aiChatDao?.insertSession(session) ?: 0L
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        val existing = aiChatDao?.getSessionById(sessionId)
        if (existing != null) {
            aiChatDao.updateSession(existing.copy(sessionTitle = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteChatSession(sessionId: Long) {
        aiChatDao?.deleteSessionById(sessionId)
        aiChatDao?.deleteMessagesForSession(sessionId)
    }

    suspend fun clearSessionsForUser(username: String) {
        aiChatDao?.clearSessionsForUser(username)
    }

    fun getMessagesForSessionFlow(sessionId: Long): Flow<List<AiChatMessageEntity>> {
        return aiChatDao?.getMessagesForSessionFlow(sessionId) ?: emptyFlow()
    }

    suspend fun getRecentMessages(sessionId: Long, limit: Int = 10): List<AiChatMessageEntity> {
        return aiChatDao?.getRecentMessages(sessionId, limit) ?: emptyList()
    }

    suspend fun saveChatMessage(
        sessionId: Long,
        senderRole: String,
        content: String,
        messageType: String = "TEXT",
        metadataJson: String = "{}"
    ): Long {
        val entity = AiChatMessageEntity(
            sessionId = sessionId,
            senderRole = senderRole,
            content = content,
            messageType = messageType,
            metadataJson = metadataJson,
            timestamp = System.currentTimeMillis()
        )
        aiChatDao?.updateSessionTimestamp(sessionId)
        return aiChatDao?.insertMessage(entity) ?: 0L
    }

    // --- Phase 5: Multi-Turn Conversational Processing Engine ---

    suspend fun processConversationalTurn(
        sessionId: Long,
        userPrompt: String,
        teacherProfile: TeacherProfileContext = TeacherProfileContext(),
        activeStructuredResult: WorkspaceStructuredResult? = null,
        targetStudentId: Long? = null,
        apiKeyOverride: String = ""
    ): ConversationalTurnResult {
        // 1. Fetch recent message window for this session
        val recentHistory = if (sessionId > 0L) {
            aiChatDao?.getRecentMessages(sessionId, 8)?.reversed() ?: emptyList()
        } else emptyList()

        // 2. Resolve Multi-Turn Context & Intent
        val parsedIntent = ConversationalContextResolver.resolveIntent(
            prompt = userPrompt,
            activeStructuredResult = activeStructuredResult,
            recentMessages = recentHistory,
            teacherProfile = teacherProfile
        )

        // 3. Save User Message to Chat History
        if (sessionId > 0L) {
            saveChatMessage(
                sessionId = sessionId,
                senderRole = "USER",
                content = userPrompt,
                messageType = "TEXT"
            )
        }

        // 4. Resolve Permission Scope with authoritative teacher lookup
        val authoritativeTeacher = if (teacherDao != null && teacherProfile.role == UserRole.TEACHER) {
            val byCode = teacherDao.getTeacherByCode(teacherProfile.teacherUsername)
            byCode ?: teacherDao.getAllTeachers().firstOrNull()?.find { 
                it.fullName.equals(teacherProfile.teacherName, ignoreCase = true) ||
                it.email.equals(teacherProfile.teacherUsername, ignoreCase = true)
            }
        } else null

        val authoritativeGrades = authoritativeTeacher?.assignedGrade?.let { listOf(it).filter { g -> g.isNotBlank() } } ?: emptyList()
        val authoritativeClasses = authoritativeTeacher?.assignedClass?.let { listOf(it).filter { c -> c.isNotBlank() } } ?: emptyList()
        val authoritativeSubjects = authoritativeTeacher?.assignedSubjects?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        val finalAssignedGrades = if (teacherProfile.assignedGrades.isNotEmpty()) {
            teacherProfile.assignedGrades
        } else {
            authoritativeGrades
        }

        val finalAssignedClasses = if (teacherProfile.assignedClasses.isNotEmpty()) {
            teacherProfile.assignedClasses
        } else {
            authoritativeClasses
        }

        val finalAssignedSubjects = if (teacherProfile.assignedSubjects.isNotEmpty()) {
            teacherProfile.assignedSubjects
        } else {
            authoritativeSubjects
        }

        val scope = AiPermissionScope(
            username = teacherProfile.teacherUsername,
            userRole = teacherProfile.role,
            teacherName = authoritativeTeacher?.fullName ?: teacherProfile.teacherName,
            teacherQualifications = teacherProfile.qualifications,
            assignedGrades = finalAssignedGrades,
            assignedClasses = finalAssignedClasses,
            assignedSubjects = finalAssignedSubjects
        )

        val targetGrade = parsedIntent.targetGrade ?: activeStructuredResult?.grade ?: teacherProfile.assignedGrades.firstOrNull() ?: "G5"
        val targetSubject = parsedIntent.targetSubject ?: activeStructuredResult?.subject ?: teacherProfile.assignedSubjects.firstOrNull() ?: "English"
        val targetUnit = parsedIntent.targetChapterUnit ?: activeStructuredResult?.sourceCitations?.firstOrNull()?.chapterUnit ?: "Unit 3"

        // Retrieve curriculum chunks for grounding if available
        val curriculumChunks = curriculumDao?.getChunksForScope(
            gradeLevel = targetGrade,
            subject = targetSubject,
            chapterUnit = targetUnit
        ) ?: emptyList()

        var turnResult: ConversationalTurnResult

        when (parsedIntent.intentType) {
            ConversationIntentType.PARTIAL_EDIT_QUESTION -> {
                val qNum = parsedIntent.targetQuestionNumber ?: 1
                if (activeStructuredResult != null) {
                    val updated = PartialRegenerationEngine.regenerateSingleQuestion(
                        currentResult = activeStructuredResult,
                        targetQuestionNumber = qNum,
                        newDifficulty = parsedIntent.targetDifficulty,
                        newQuestionType = parsedIntent.targetQuestionType,
                        curriculumChunks = curriculumChunks
                    )
                    val diffMsg = parsedIntent.targetDifficulty?.displayName ?: "Modified"
                    val typeMsg = parsedIntent.targetQuestionType?.displayName ?: "Updated Format"
                    val msg = "I have updated Question #$qNum for **$targetGrade $targetSubject** ($diffMsg, $typeMsg) while preserving all other questions in the assessment."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = updated
                    )
                } else {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "No active question paper is currently loaded to edit question #$qNum. Please generate or open an assessment first.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                }
            }

            ConversationIntentType.REGENERATE_RANGE -> {
                val range = parsedIntent.targetQuestionRange ?: Pair(1, 3)
                if (activeStructuredResult != null) {
                    val updated = PartialRegenerationEngine.regenerateQuestionRange(
                        currentResult = activeStructuredResult,
                        fromQuestionNumber = range.first,
                        toQuestionNumber = range.second,
                        newDifficulty = parsedIntent.targetDifficulty ?: DifficultyLevel.CHALLENGE,
                        newQuestionType = parsedIntent.targetQuestionType,
                        curriculumChunks = curriculumChunks
                    )
                    val diffName = parsedIntent.targetDifficulty?.displayName ?: "Higher Difficulty"
                    val msg = "I have regenerated Questions #${range.first}–#${range.second} with **$diffName** for **$targetGrade $targetSubject** while keeping all preceding and subsequent questions intact."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = updated
                    )
                } else {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "No active question paper found to regenerate questions #${range.first}–#${range.second}. Please generate an assessment first.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                }
            }

            ConversationIntentType.ADD_QUESTIONS -> {
                val addCount = parsedIntent.additionalCount ?: 2
                if (activeStructuredResult != null) {
                    val updated = PartialRegenerationEngine.addQuestions(
                        currentResult = activeStructuredResult,
                        count = addCount,
                        topic = targetUnit,
                        questionType = parsedIntent.targetQuestionType,
                        difficulty = parsedIntent.targetDifficulty,
                        curriculumChunks = curriculumChunks
                    )
                    val msg = "I have appended $addCount additional question(s) on **$targetUnit** to your assessment. The new total is ${updated.sections.sumOf { it.questions.size }} questions (${updated.totalMarks} marks)."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = updated
                    )
                } else {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Please load or generate an initial question paper before adding further questions.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                }
            }

            ConversationIntentType.EXPLAIN_QUESTION -> {
                val qNum = parsedIntent.targetQuestionNumber ?: 1
                if (activeStructuredResult != null) {
                    val explanation = PartialRegenerationEngine.explainQuestion(activeStructuredResult, qNum)
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = explanation,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = activeStructuredResult
                    )
                } else {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Could not find Question #$qNum because no assessment is currently active in this workspace.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                }
            }

            ConversationIntentType.TRANSLATE_CONTENT -> {
                if (activeStructuredResult != null) {
                    val translated = PartialRegenerationEngine.translateContent(activeStructuredResult, parsedIntent.targetLanguage)
                    val langDisplay = if (parsedIntent.targetLanguage.equals("MYANMAR", true)) "Myanmar" else "English"
                    val msg = "I have translated the entire assessment paper and scoring guide into **$langDisplay** while preserving question order and mark allocations."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = translated
                    )
                } else {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Please generate or select an assessment first to translate its content.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                }
            }

            ConversationIntentType.CONVERT_FORMAT -> {
                if (activeStructuredResult != null) {
                    val converted = if (activeStructuredResult.examType.contains("Worksheet", true)) {
                        PartialRegenerationEngine.convertWorksheetToQuestionPaper(activeStructuredResult, "Monthly Test")
                    } else {
                        PartialRegenerationEngine.convertQuestionPaperToWorksheet(activeStructuredResult)
                    }
                    val msg = "I have reformatted your assessment into **${converted.examType}** format with updated headers and pedagogical instructions."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = converted
                    )
                } else {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "No active document found to reformat. Please generate an exam paper or worksheet first.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                }
            }

            ConversationIntentType.CREATE_QUESTION_PAPER -> {
                val qCount = parsedIntent.additionalCount ?: 10
                val paperRequest = QuestionPaperRequest(
                    purpose = QuestionPurpose.OFFICIAL_EXAM,
                    examType = ExamPaperType.MONTHLY_TEST,
                    academicYear = teacherProfile.academicYear,
                    grade = targetGrade,
                    subject = targetSubject,
                    chapterUnit = targetUnit,
                    totalMarks = 50,
                    questionCount = qCount,
                    language = parsedIntent.targetLanguage
                )

                if (curriculumQuestionEngine != null) {
                    val paperResult = curriculumQuestionEngine.generateQuestionPaper(paperRequest, scope)
                    val structured = paperResult.toWorkspaceStructuredResult(paperRequest)
                    val msg = "I have generated a curriculum-grounded **${paperResult.title}** (${paperResult.allQuestions.size} questions, ${paperResult.totalMarks} marks) strictly sourced from approved textbooks."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = structured,
                        sourceCitations = paperResult.sourceCitations
                    )
                } else {
                    // Fallback using Gemini Engine
                    val prompt = "Generate an official Question Paper for $targetGrade $targetSubject ($targetUnit) with $qCount questions."
                    val text = aiEngine.generateContent(prompt, apiKeyOverride = apiKeyOverride)
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = text,
                        intentType = parsedIntent.intentType
                    )
                }
            }

            ConversationIntentType.CREATE_WORKSHEET -> {
                val qCount = parsedIntent.additionalCount ?: 10
                val worksheetRequest = WorksheetRequest(
                    academicYear = teacherProfile.academicYear,
                    grade = targetGrade,
                    subject = targetSubject,
                    chapterUnit = targetUnit,
                    questionCount = qCount,
                    language = parsedIntent.targetLanguage
                )

                if (curriculumQuestionEngine != null) {
                    val wsResult = curriculumQuestionEngine.generateWorksheet(worksheetRequest, scope)
                    val structured = wsResult.toWorkspaceStructuredResult(worksheetRequest)
                    val msg = "I have generated a differentiated practice worksheet for **$targetGrade $targetSubject** ($targetUnit, ${wsResult.totalQuestions} items) with verified answer keys."
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        updatedStructuredResult = structured,
                        sourceCitations = wsResult.sourceCitations
                    )
                } else {
                    val prompt = "Generate a Student Practice Worksheet for $targetGrade $targetSubject ($targetUnit)."
                    val text = aiEngine.generateContent(prompt, apiKeyOverride = apiKeyOverride)
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = text,
                        intentType = parsedIntent.intentType
                    )
                }
            }

            ConversationIntentType.CREATE_REPORT_COMMENT -> {
                val studentNameQuery = parsedIntent.targetStudentName
                val allStudents = studentDao?.getAllStudents()?.firstOrNull() ?: emptyList()
                val targetStudent = when {
                    targetStudentId != null -> studentDao?.getStudentById(targetStudentId)
                    !studentNameQuery.isNullOrBlank() -> allStudents.find { 
                        it.name.contains(studentNameQuery, ignoreCase = true) || studentNameQuery.contains(it.name, ignoreCase = true)
                    }
                    else -> allStudents.find { 
                        it.gradeName.equals(targetGrade, ignoreCase = true) && scope.canAccessStudent(it.gradeName, it.className) 
                    }
                }

                if (targetStudent == null) {
                    val msg = if (!studentNameQuery.isNullOrBlank()) {
                        "No student record found matching name '$studentNameQuery' in the database. Please verify the student name in the Student Management module."
                    } else {
                        "No student found for Grade $targetGrade in your accessible classes. Please specify the student's name."
                    }
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = msg,
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else if (!scope.canAccessStudent(targetStudent.gradeName, targetStudent.className)) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: You do not have permission to access records for student '${targetStudent.name}' in Grade ${targetStudent.gradeName} (${targetStudent.className}).",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else {
                    val facts = studentFactsAggregator?.aggregateFacts(
                        studentId = targetStudent.id,
                        periodName = "Term 1 Examination",
                        academicYear = teacherProfile.academicYear
                    )

                    if (facts != null) {
                        val commentResult = reportCardAiEngine.generateReportComment(
                            facts = facts,
                            preferredLanguage = parsedIntent.targetLanguage,
                            apiKeyOverride = apiKeyOverride
                        )

                        val responseText = buildString {
                            appendLine("### 📝 Report Card Feedback for ${targetStudent.name} (Grade ${targetStudent.gradeName}-${targetStudent.className}, Roll #${targetStudent.rollNumber})")
                            appendLine("**Teacher Comment:**")
                            appendLine(commentResult.teacherComment)
                            appendLine()
                            appendLine("**Parent Support Suggestion:**")
                            appendLine(commentResult.parentSuggestion)
                            appendLine()
                            appendLine("*Authoritatively grounded in real marks, attendance, and holistic records. Zero fabricated metrics.*")
                        }

                        turnResult = ConversationalTurnResult(
                            assistantResponseText = responseText,
                            intentType = parsedIntent.intentType,
                            reportCardResult = commentResult
                        )
                    } else {
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = "Could not aggregate academic facts for ${targetStudent.name}. Please ensure assessment marks or attendance records have been entered.",
                            intentType = parsedIntent.intentType,
                            isSuccess = false
                        )
                    }
                }
            }

            ConversationIntentType.ADMIN_SCHOOL_ANALYTICS -> {
                if (scope.userRole == UserRole.OFFICE_STAFF) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Office Staff role does not have permission to view academic analytics.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else if (scope.userRole == UserRole.TEACHER && (scope.assignedGrades.isEmpty() || scope.assignedClasses.isEmpty())) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Your teacher account has no configured grade or class assignments.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else {
                    val analytics = aiDataToolsBridge?.getSchoolAnalyticsOverview(scope, teacherProfile.academicYear)
                    if (analytics != null) {
                        val responseText = buildString {
                            appendLine("### 📊 School Academic & Attendance Analytics (${analytics.academicYear})")
                            appendLine()
                            appendLine("| Metric | Authoritative Value |")
                            appendLine("| :--- | :--- |")
                            appendLine("| **Total Evaluated Students** | ${analytics.totalStudents} |")
                            appendLine("| **Active Staff / Teachers** | ${analytics.totalTeachers} |")
                            appendLine("| **Active Grades / Classes** | ${analytics.activeGradesCount} Grades / ${analytics.activeClassesCount} Classes |")
                            appendLine("| **Overall Academic Pass Rate** | **${String.format(Locale.US, "%.1f", analytics.overallPassRate)}%** |")
                            appendLine("| **Overall Attendance Rate** | **${String.format(Locale.US, "%.1f", analytics.overallAttendanceRate)}%** |")
                            appendLine("| **Curriculum Units Available** | ${analytics.totalCurriculumUnitsAvailable} Units |")
                            appendLine()
                            appendLine("#### 🏫 Grade-Level Breakdown:")
                            appendLine("| Grade | Students | Attendance Rate | Present | Late | Absent |")
                            appendLine("| :--- | :---: | :---: | :---: | :---: | :---: |")
                            analytics.gradeLevelSummaries.forEach { g ->
                                appendLine("| **Grade ${g.gradeName}** | ${g.studentCount} | ${String.format(Locale.US, "%.1f", g.attendancePercentage)}% | ${g.presentDaysCount} | ${g.lateDaysCount} | ${g.absentDaysCount} |")
                            }
                            appendLine()
                            appendLine("*Authoritatively computed via HCM-SMS database engines. Zero AI math estimation.*")
                        }
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = responseText,
                            intentType = parsedIntent.intentType
                        )
                    } else {
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = "No school analytics data available for ${teacherProfile.academicYear}.",
                            intentType = parsedIntent.intentType,
                            isSuccess = false
                        )
                    }
                }
            }

            ConversationIntentType.ADMIN_ATTENDANCE_OVERVIEW -> {
                if (scope.userRole == UserRole.OFFICE_STAFF) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Office Staff role does not have permission to view attendance analytics.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else if (scope.userRole == UserRole.TEACHER && (scope.assignedGrades.isEmpty() || scope.assignedClasses.isEmpty())) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Your teacher account has no configured grade or class assignments.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else {
                    val attSummary = aiDataToolsBridge?.getSchoolAttendanceOverview(scope, teacherProfile.academicYear, parsedIntent.targetGrade)
                    if (attSummary != null) {
                        val responseText = buildString {
                            appendLine("### 📋 Attendance Overview — ${attSummary.scopeDescription}")
                            appendLine("**Academic Year:** ${attSummary.academicYear} | **Students Evaluated:** ${attSummary.totalStudentsEvaluated}")
                            appendLine()
                            appendLine("| Status | Total Days | Percentage |")
                            appendLine("| :--- | :---: | :---: |")
                            val presPct = if (attSummary.totalRecords > 0) (attSummary.presentCount.toDouble() / attSummary.totalRecords) * 100.0 else 0.0
                            val latePct = if (attSummary.totalRecords > 0) (attSummary.lateCount.toDouble() / attSummary.totalRecords) * 100.0 else 0.0
                            val absPct = if (attSummary.totalRecords > 0) (attSummary.absentCount.toDouble() / attSummary.totalRecords) * 100.0 else 0.0
                            appendLine("| ✅ **Present** | ${attSummary.presentCount} | ${String.format(Locale.US, "%.1f", presPct)}% |")
                            appendLine("| ⚠️ **Late** | ${attSummary.lateCount} | ${String.format(Locale.US, "%.1f", latePct)}% |")
                            appendLine("| ❌ **Absent** | ${attSummary.absentCount} | ${String.format(Locale.US, "%.1f", absPct)}% |")
                            appendLine("| 🎯 **Net Attendance Rate** | **${attSummary.presentCount + attSummary.lateCount}** | **${String.format(Locale.US, "%.1f", attSummary.overallAttendancePercentage)}%** |")
                            appendLine()
                            if (attSummary.gradeBreakdown.isNotEmpty()) {
                                appendLine("#### Grade Breakdown:")
                                appendLine("| Grade | Enrolled | Present | Absent | Rate |")
                                appendLine("| :--- | :---: | :---: | :---: | :---: |")
                                attSummary.gradeBreakdown.forEach { g ->
                                    appendLine("| Grade ${g.gradeName} | ${g.studentCount} | ${g.presentDaysCount} | ${g.absentDaysCount} | ${String.format(Locale.US, "%.1f", g.attendancePercentage)}% |")
                                }
                                appendLine()
                            }
                            appendLine("*Authoritatively computed via HCM Attendance Records.*")
                        }
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = responseText,
                            intentType = parsedIntent.intentType
                        )
                    } else {
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = "No attendance records found for academic year ${teacherProfile.academicYear}.",
                            intentType = parsedIntent.intentType,
                            isSuccess = false
                        )
                    }
                }
            }

            ConversationIntentType.ADMIN_EXAM_PERFORMANCE -> {
                if (scope.userRole == UserRole.OFFICE_STAFF) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Office Staff role does not have permission to view examination performance.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else if (scope.userRole == UserRole.TEACHER && (scope.assignedGrades.isEmpty() || scope.assignedClasses.isEmpty())) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Your teacher account has no configured grade or class assignments.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else {
                    val examSummary = aiDataToolsBridge?.getExamPerformanceOverview(scope, teacherProfile.academicYear, parsedIntent.targetGrade)
                    if (examSummary != null) {
                        val responseText = buildString {
                            appendLine("### 📈 Exam Performance Summary — ${examSummary.scopeDescription}")
                            appendLine("**Academic Year:** ${examSummary.academicYear} | **Students Evaluated:** ${examSummary.totalStudentsEvaluated}")
                            appendLine()
                            appendLine("| Metric | Result |")
                            appendLine("| :--- | :--- |")
                            appendLine("| **Overall Average Score** | **${String.format(Locale.US, "%.1f", examSummary.overallAverageScore)}%** |")
                            appendLine("| **Overall Pass Rate** | **${String.format(Locale.US, "%.1f", examSummary.overallPassRate)}%** |")
                            appendLine("| **Total Distinctions** | ${examSummary.distinctionCount} |")
                            appendLine("| **Top Performing Subject** | 🌟 ${examSummary.highestPerformingSubject} |")
                            appendLine("| **Needs Support Subject** | ⚠️ ${examSummary.lowestPerformingSubject} |")
                            appendLine()
                            appendLine("#### Subject-by-Subject Breakdown:")
                            appendLine("| Subject | Tested | Average | Pass Rate | Distinctions |")
                            appendLine("| :--- | :---: | :---: | :---: | :---: |")
                            examSummary.subjectPerformance.forEach { subj ->
                                appendLine("| **${subj.subjectName}** | ${subj.totalStudentsTested} | ${String.format(Locale.US, "%.1f", subj.averageScore)}% | ${String.format(Locale.US, "%.1f", subj.passRatePercentage)}% | ${subj.distinctionCount} |")
                            }
                            appendLine()
                            appendLine("*Authoritatively computed via HCM Marks & Assessment DAOs.*")
                        }
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = responseText,
                            intentType = parsedIntent.intentType
                        )
                    } else {
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = "No exam performance records found for academic year ${teacherProfile.academicYear}.",
                            intentType = parsedIntent.intentType,
                            isSuccess = false
                        )
                    }
                }
            }

            ConversationIntentType.ADMIN_AT_RISK_STUDENTS -> {
                if (scope.userRole == UserRole.OFFICE_STAFF) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Office Staff role does not have permission to view at-risk student data.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else if (scope.userRole == UserRole.TEACHER && (scope.assignedGrades.isEmpty() || scope.assignedClasses.isEmpty())) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: Your teacher account has no configured grade or class assignments.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else {
                    val atRiskList = aiDataToolsBridge?.getAtRiskStudents(scope, teacherProfile.academicYear, parsedIntent.targetGrade) ?: emptyList()
                    if (atRiskList.isEmpty()) {
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = "✅ **No At-Risk Students Detected**: All students in the current scope meet academic pass thresholds (>=50%) and maintain satisfactory attendance (>=75%).",
                            intentType = parsedIntent.intentType
                        )
                    } else {
                        val responseText = buildString {
                            appendLine("### ⚠️ At-Risk Students & Targeted Interventions")
                            appendLine("**Identified Students Requiring Academic / Attendance Support:** ${atRiskList.size}")
                            appendLine()
                            appendLine("| Student | Grade & Roll | Average | Attendance | Primary Risk Factor | Recommended Action |")
                            appendLine("| :--- | :---: | :---: | :---: | :--- | :--- |")
                            atRiskList.forEach { st ->
                                val attStr = st.attendancePercentage?.let { "${String.format(Locale.US, "%.1f", it)}%" } ?: "N/A"
                                appendLine("| **${st.studentName}** | Grade ${st.gradeName} (${st.className}) #${st.rollNumber} | ${String.format(Locale.US, "%.1f", st.averageScorePercentage)}% | $attStr | ${st.primaryRiskFactor} | ${st.recommendedIntervention} |")
                            }
                            appendLine()
                            appendLine("*Grounded in verified marks and attendance records. Excludes confidential PII.*")
                        }
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = responseText,
                            intentType = parsedIntent.intentType
                        )
                    }
                }
            }

            ConversationIntentType.ADMIN_POLICY_OVERVIEW -> {
                val policyText = aiDataToolsBridge?.getSchoolGradingPolicy() ?: "No grading policy found."
                val responseText = buildString {
                    appendLine("### 📜 HeinChanMyae School Grading & Assessment Policy")
                    appendLine()
                    appendLine(policyText)
                    appendLine()
                    appendLine("*Authoritative institutional grading standards.*")
                }
                turnResult = ConversationalTurnResult(
                    assistantResponseText = responseText,
                    intentType = parsedIntent.intentType
                )
            }

            ConversationIntentType.STUDENT_FACTS_ANALYSIS -> {
                val studentNameQuery = parsedIntent.targetStudentName
                val allStudents = studentDao?.getAllStudents()?.firstOrNull() ?: emptyList()
                val targetStudent = if (!studentNameQuery.isNullOrBlank()) {
                    allStudents.find { it.name.contains(studentNameQuery, ignoreCase = true) || studentNameQuery.contains(it.name, ignoreCase = true) }
                } else null

                if (targetStudent == null) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Could not find student '$studentNameQuery' to analyze. Please provide a valid student name.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else if (!scope.canAccessStudent(targetStudent.gradeName, targetStudent.className)) {
                    turnResult = ConversationalTurnResult(
                        assistantResponseText = "Access Denied: You do not have permission to view records for student '${targetStudent.name}'.",
                        intentType = parsedIntent.intentType,
                        isSuccess = false
                    )
                } else {
                    val facts = aiDataToolsBridge?.getStudentFacts(
                        studentId = targetStudent.id,
                        academicYear = teacherProfile.academicYear,
                        assessmentPeriod = "Term 1 Examination",
                        scope = scope
                    )
                    if (facts != null) {
                        val responseText = buildString {
                            appendLine("### 📊 Student Verified Fact Profile: ${targetStudent.name}")
                            appendLine("**Grade:** ${targetStudent.gradeName} (${targetStudent.className}) | **Roll Number:** #${targetStudent.rollNumber}")
                            appendLine()
                            appendLine(facts.verifiedSummaryText)
                        }
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = responseText,
                            intentType = parsedIntent.intentType
                        )
                    } else {
                        turnResult = ConversationalTurnResult(
                            assistantResponseText = "No verified academic or holistic facts available for ${targetStudent.name}.",
                            intentType = parsedIntent.intentType,
                            isSuccess = false
                        )
                    }
                }
            }

            else -> {
                // General Pedagogy / Q&A with Sliding Window
                val slidingPrompt = ConversationalContextResolver.buildSlidingWindowPrompt(
                    userPrompt = userPrompt,
                    teacherProfile = teacherProfile,
                    recentTurns = recentHistory,
                    activeStructuredResult = activeStructuredResult
                )

                val settings = aiDao.getAiSettings()
                val effectiveApiKey = apiKeyOverride.ifBlank { settings?.apiKeyOverride ?: "" }
                val systemInst = """
                    You are the Official AI Teacher Assistant for HeinChanMyae Private School (HCM-SMS).
                    Assist teachers in Myanmar with lesson planning, question revision, classroom strategies, and curriculum advice.
                    Respect teacher assignments: Grade(s) ${teacherProfile.assignedGrades.joinToString(", ")}, Subject(s) ${teacherProfile.assignedSubjects.joinToString(", ")}.
                    Current Academic Year: ${teacherProfile.academicYear}.
                    Output concise, high-quality, professional educational guidance.
                """.trimIndent()

                val outputText = aiEngine.generateContent(
                    prompt = slidingPrompt,
                    systemInstruction = systemInst,
                    apiKeyOverride = effectiveApiKey,
                    modelPreference = settings?.preferredProvider ?: "gemini-3.5-flash"
                )

                turnResult = ConversationalTurnResult(
                    assistantResponseText = outputText,
                    intentType = parsedIntent.intentType,
                    updatedStructuredResult = activeStructuredResult
                )
            }
        }

        // 5. Save Assistant Message to Chat History
        if (sessionId > 0L) {
            val messageType = when {
                turnResult.updatedStructuredResult != null -> if (turnResult.updatedStructuredResult?.examType?.contains("Worksheet", true) == true) "WORKSHEET" else "QUESTION_PAPER"
                turnResult.reportCardResult != null -> "REPORT_COMMENT"
                else -> "TEXT"
            }
            val metadata = if (turnResult.updatedStructuredResult != null) {
                WorkspaceStructuredResultSerializer.serialize(turnResult.updatedStructuredResult!!)
            } else "{}"

            saveChatMessage(
                sessionId = sessionId,
                senderRole = "ASSISTANT",
                content = turnResult.assistantResponseText,
                messageType = messageType,
                metadataJson = metadata
            )
        }

        return turnResult
    }

    fun getHistoryByCategory(category: String): Flow<List<AiHistoryEntity>> {
        return aiDao.getHistoryByCategory(category)
    }

    fun getHistoryForUser(username: String, userRole: UserRole): Flow<List<AiHistoryEntity>> {
        return if (userRole == UserRole.ADMIN || userRole == UserRole.SUPER_ADMIN) {
            aiDao.getAllHistory()
        } else {
            aiDao.getHistoryForTeacher(username)
        }
    }

    fun getFavoriteHistoryForUser(username: String, userRole: UserRole): Flow<List<AiHistoryEntity>> {
        return if (userRole == UserRole.ADMIN || userRole == UserRole.SUPER_ADMIN) {
            aiDao.getFavoriteHistory()
        } else {
            aiDao.getFavoriteHistoryForTeacher(username)
        }
    }

    suspend fun saveStructuredResult(
        result: WorkspaceStructuredResult,
        teacherUsername: String,
        isWorksheet: Boolean = result.originalWorksheetRequest != null || result.examType.contains("Worksheet", ignoreCase = true)
    ): Long {
        val category = if (isWorksheet) {
            AiToolCategory.WORKSHEET.displayName
        } else {
            AiToolCategory.EXAM.displayName
        }

        val jsonResult = WorkspaceStructuredResultSerializer.serialize(
            result = result,
            teacherUsername = teacherUsername
        )

        val topic = result.sections.firstOrNull()?.questions?.firstOrNull()?.sourceCitation?.chapterUnit
            ?: result.sourceCitations.firstOrNull()?.chapterUnit
            ?: result.title

        val difficulty = result.sections.firstOrNull()?.questions?.firstOrNull()?.difficulty ?: "Medium"

        // Duplicate protection: Check if existing ID is known or if a matching record exists
        val existingId = result.savedHistoryId ?: aiDao.findExistingHistory(teacherUsername, result.title)?.id

        val historyEntity = AiHistoryEntity(
            id = existingId ?: 0L,
            prompt = "Curriculum assessment generation for ${result.grade} ${result.subject} (${result.title})",
            title = result.title,
            generatedResult = jsonResult,
            category = category,
            teacherUsername = teacherUsername,
            timestamp = System.currentTimeMillis(),
            isFavorite = false,
            grade = result.grade,
            subject = result.subject,
            topic = topic,
            difficulty = difficulty
        )

        return aiDao.insertHistory(historyEntity)
    }

    suspend fun exportQuestionsToQuestionBank(
        result: WorkspaceStructuredResult,
        teacherUsername: String
    ): Int {
        val questionsToSave = mutableListOf<AiSavedQuestionEntity>()
        val defaultTopic = result.sections.firstOrNull()?.questions?.firstOrNull()?.sourceCitation?.chapterUnit
            ?: result.sourceCitations.firstOrNull()?.chapterUnit
            ?: result.title

        for (section in result.sections) {
            for (q in section.questions) {
                val optJson = if (q.options.isNotEmpty()) {
                    JSONArray(q.options).toString()
                } else ""

                val qTopic = q.sourceCitation?.chapterUnit?.ifBlank { defaultTopic } ?: defaultTopic

                questionsToSave.add(
                    AiSavedQuestionEntity(
                        questionText = q.questionText,
                        optionsJson = optJson,
                        correctAnswer = q.correctAnswer,
                        questionType = q.questionType,
                        grade = result.grade,
                        subject = result.subject,
                        topic = qTopic,
                        difficulty = q.difficulty,
                        marks = q.marks,
                        createdBy = teacherUsername,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }

        if (questionsToSave.isNotEmpty()) {
            aiDao.insertQuestions(questionsToSave)
        }
        return questionsToSave.size
    }

    suspend fun generateAiContent(
        prompt: String,
        categoryName: String,
        teacherUsername: String,
        grade: String = "All",
        subject: String = "General",
        topic: String = "",
        difficulty: String = "Medium"
    ): String {
        val settings = aiDao.getAiSettings()
        val apiKeyOverride = settings?.apiKeyOverride ?: ""

        val systemInstruction = """
            You are the Official AI Teacher Assistant for HeinChanMyae Private School (HCM-SMS).
            Assist teachers in Myanmar with lesson planning, worksheet generation, exam creation, question bank generation, teaching materials, report writing remarks (English & Myanmar), translation, and classroom activities.
            Do NOT attempt to directly modify student database records or marks.
            Output clean, markdown-formatted text that teachers can edit and review.
            Current Default Language: ${settings?.defaultLanguage ?: "English & Myanmar"}.
        """.trimIndent()

        val generatedText = aiEngine.generateContent(
            prompt = prompt,
            systemInstruction = systemInstruction,
            apiKeyOverride = apiKeyOverride,
            modelPreference = settings?.preferredProvider ?: "gemini-3.5-flash"
        )

        // Save into AI History automatically only if generation was successful
        if (!generatedText.startsWith("⚠️")) {
            val historyItem = AiHistoryEntity(
                prompt = prompt,
                title = "$categoryName - ${topic.ifBlank { "Generated Document" }}",
                generatedResult = generatedText,
                category = categoryName,
                teacherUsername = teacherUsername,
                grade = grade,
                subject = subject,
                topic = topic,
                difficulty = difficulty
            )
            aiDao.insertHistory(historyItem)
        }

        return generatedText
    }

    suspend fun saveHistoryItem(item: AiHistoryEntity): Long {
        return aiDao.insertHistory(item)
    }

    suspend fun toggleFavorite(id: Long, isFav: Boolean) {
        aiDao.toggleFavorite(id, isFav)
    }

    suspend fun deleteHistory(id: Long) {
        aiDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        aiDao.clearAllHistory()
    }

    suspend fun saveQuestion(question: AiSavedQuestionEntity): Long {
        return aiDao.insertQuestion(question)
    }

    suspend fun saveQuestions(questions: List<AiSavedQuestionEntity>) {
        aiDao.insertQuestions(questions)
    }

    suspend fun deleteQuestion(id: Long) {
        aiDao.deleteQuestionById(id)
    }

    suspend fun getAiSettings(): AiSettingEntity {
        return aiDao.getAiSettings() ?: AiSettingEntity()
    }

    suspend fun updateAiSettings(settings: AiSettingEntity) {
        aiDao.saveAiSettings(settings)
    }
}
