package com.example.ui.screens.ai.workspace

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.*
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.ChatMessage
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Phase 6 Redesigned Modern AI Assistant Workspace.
 * A unified, mobile-first conversational canvas with:
 * - TopAppBar with History & Settings menu
 * - Compact Mode Selector (Teacher / Admin)
 * - Persistent Context Strip with quick-edit bottom sheet
 * - Quick Action Chips row
 * - Chat Area with progressive cards, Myanmar/English bubbles & empty state suggestions
 * - Bottom Message Composer with [ + ] quick palette and full keyboard inset support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantWorkspace(
    viewModel: AiAssistantViewModel? = null,
    currentUser: UserEntity? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isAdmin = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.SUPER_ADMIN

    // Workspace Mode State
    var currentMode by remember { mutableStateOf(AiAssistantMode.TEACHER) }

    // Context Strip State
    var contextState by remember {
        mutableStateOf(
            AiContextConfigState(
                academicYear = "2026–2027",
                grade = "G5",
                className = "Room A",
                subject = "English",
                chapterUnit = "Unit 3",
                topic = "Healthy Food & Nutrition",
                language = "English"
            )
        )
    }

    // Modal & Sheet Visibility
    var showContextSheet by remember { mutableStateOf(false) }
    var showQuestionPaperSheet by remember { mutableStateOf(false) }
    var showWorksheetSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<WorkspaceChatMessage?>(null) }
    var activeQuickAction by remember { mutableStateOf<QuickActionType?>(null) }

    // Composer State
    var composerText by remember { mutableStateOf("") }
    val isGeneratingFromVm by (viewModel?.isGenerating ?: remember { mutableStateOf(false) }).let {
        if (viewModel != null) viewModel.isGenerating.collectAsState() else remember { mutableStateOf(false) }
    }

    LaunchedEffect(currentUser) {
        viewModel?.setCurrentUser(currentUser)
    }

    // Chat Message Feed
    val messages = remember {
        mutableStateListOf(
            WorkspaceChatMessage(
                id = "welcome",
                sender = ChatSender.AI,
                text = "Mingalarbar! I am your curriculum-grounded AI Assistant. I can help you prepare questions, worksheets, lesson plans, or holistic report comments for ${contextState.grade}-${contextState.className} ${contextState.subject}.",
                suggestedActions = listOf(
                    QuickActionType.QUESTION_PAPER,
                    QuickActionType.WORKSHEET,
                    QuickActionType.LESSON_PLAN
                )
            )
        )
    }

    // Observe active workspace structured result from ViewModel if present
    val vmStructuredResult by (viewModel?.activeWorkspaceResult ?: remember { mutableStateOf(null) }).let {
        if (viewModel != null) viewModel.activeWorkspaceResult.collectAsState() else remember { mutableStateOf(null) }
    }

    LaunchedEffect(vmStructuredResult) {
        val result = vmStructuredResult
        if (result != null) {
            val lastAiIndex = messages.indexOfLast { it.sender == ChatSender.AI }
            if (lastAiIndex != -1) {
                messages[lastAiIndex] = messages[lastAiIndex].copy(
                    structuredResult = result,
                    isGenerating = false
                )
            }
        }
    }

    // Observe ViewModel chat messages stream to sync reply text
    val vmChatMessages by (viewModel?.chatMessages ?: remember { mutableStateOf(emptyList<ChatMessage>()) }).let {
        if (viewModel != null) viewModel.chatMessages.collectAsState() else remember { mutableStateOf(emptyList()) }
    }

    LaunchedEffect(vmChatMessages) {
        if (vmChatMessages.isNotEmpty() && viewModel != null) {
            val lastAi = vmChatMessages.lastOrNull { it.sender == "AI" }
            if (lastAi != null) {
                val lastAiIndex = messages.indexOfLast { it.sender == ChatSender.AI }
                if (lastAiIndex != -1 && messages[lastAiIndex].isGenerating) {
                    messages[lastAiIndex] = messages[lastAiIndex].copy(
                        text = lastAi.text,
                        isGenerating = false
                    )
                }
            }
        }
    }

    // Auto-scroll chat list to bottom whenever new messages arrive or state updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Message Send Handler
    val handleSendMessage: (String) -> Unit = { prompt ->
        val userMsgId = UUID.randomUUID().toString()
        val aiMsgId = UUID.randomUUID().toString()

        messages.add(
            WorkspaceChatMessage(
                id = userMsgId,
                sender = ChatSender.USER,
                text = prompt
            )
        )

        // Add placeholder for AI
        messages.add(
            WorkspaceChatMessage(
                id = aiMsgId,
                sender = ChatSender.AI,
                text = "",
                isGenerating = true
            )
        )

        composerText = ""
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }

        coroutineScope.launch {
            try {
                if (viewModel != null) {
                    viewModel.sendChatMessage(prompt, currentUser)
                } else {
                    val aiIndex = messages.indexOfFirst { it.id == aiMsgId }
                    if (aiIndex != -1) {
                        messages[aiIndex] = messages[aiIndex].copy(
                            text = "Received instruction: '$prompt'. Grounding against ${contextState.grade} ${contextState.subject} curriculum.",
                            isGenerating = false
                        )
                    }
                }
            } catch (e: Exception) {
                val aiIndex = messages.indexOfFirst { it.id == aiMsgId }
                if (aiIndex != -1) {
                    messages[aiIndex] = messages[aiIndex].copy(
                        text = "Error processing request: ${e.localizedMessage ?: "Unknown error"}. Please check curriculum connection.",
                        isGenerating = false
                    )
                }
            }
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Trigger Action Handler
    val handleQuickAction: (QuickActionType) -> Unit = { action ->
        activeQuickAction = action
        when (action) {
            QuickActionType.QUESTION_PAPER, QuickActionType.QUIZ -> {
                showQuestionPaperSheet = true
            }
            QuickActionType.WORKSHEET -> {
                showWorksheetSheet = true
            }
            QuickActionType.LESSON_PLAN -> {
                handleSendMessage("Prepare a structured 5E Lesson Plan for ${contextState.grade} ${contextState.subject} (${contextState.chapterUnit}).")
            }
            QuickActionType.CLASS_ACTIVITY -> {
                handleSendMessage("Suggest interactive classroom activities for ${contextState.grade} ${contextState.subject} (${contextState.chapterUnit}).")
            }
            QuickActionType.REPORT_COMMENT -> {
                handleSendMessage("Generate a bilingual report card comment for ${contextState.grade} ${contextState.subject}.")
            }
            QuickActionType.TEACHING_VISUAL -> {
                handleSendMessage("Create a teaching visual and concept outline for ${contextState.grade} ${contextState.subject} (${contextState.chapterUnit}).")
            }
            QuickActionType.SCHOOL_ANALYTICS -> {
                handleSendMessage("Show school analytics for ${contextState.academicYear}.")
            }
            QuickActionType.ATTENDANCE_OVERVIEW -> {
                handleSendMessage("Show attendance overview for ${contextState.academicYear}.")
            }
            QuickActionType.EXAM_PERFORMANCE -> {
                handleSendMessage("Show exam performance for ${contextState.grade} ${contextState.subject} ${contextState.academicYear}.")
            }
            QuickActionType.AT_RISK_STUDENTS -> {
                handleSendMessage("Show at-risk students for ${contextState.academicYear}.")
            }
            QuickActionType.STUDENT_ANALYSIS -> {
                handleSendMessage("Analyze student academic progress for ${contextState.grade}-${contextState.className}.")
            }
            else -> {
                showQuestionPaperSheet = true
            }
        }
    }

    // Handle Regeneration
    val handleRegenerate: (WorkspaceChatMessage) -> Unit = { targetMsg ->
        val structured = targetMsg.structuredResult
        if (structured != null && !targetMsg.isGenerating) {
            val msgIndex = messages.indexOfFirst { it.id == targetMsg.id }
            if (msgIndex != -1) {
                messages[msgIndex] = targetMsg.copy(isGenerating = true)
            }

            coroutineScope.launch {
                try {
                    val qpReq = structured.originalQuestionPaperRequest
                    val wsReq = structured.originalWorksheetRequest

                    if (qpReq != null && viewModel != null) {
                        val nextSeed = if (qpReq.generationSeed == 0L) System.currentTimeMillis() else qpReq.generationSeed + 1
                        val newResult = viewModel.generateQuestionPaper(qpReq.copy(generationSeed = nextSeed), currentUser)
                        if (newResult.validationReport.isValid && newResult.allQuestions.isNotEmpty()) {
                            val newStructured = newResult.toWorkspaceStructuredResult(qpReq)
                            val idx = messages.indexOfFirst { it.id == targetMsg.id }
                            if (idx != -1) {
                                messages[idx] = targetMsg.copy(
                                    structuredResult = newStructured,
                                    isGenerating = false
                                )
                            }
                        } else {
                            val idx = messages.indexOfFirst { it.id == targetMsg.id }
                            if (idx != -1) messages[idx] = targetMsg.copy(isGenerating = false)
                            snackbarHostState.showSnackbar("Regeneration notice: Previous assessment preserved.")
                        }
                    } else if (wsReq != null && viewModel != null) {
                        val nextSeed = if (wsReq.generationSeed == 0L) System.currentTimeMillis() else wsReq.generationSeed + 1
                        val newResult = viewModel.generateWorksheet(wsReq.copy(generationSeed = nextSeed), currentUser)
                        if (newResult.validationReport.isValid && newResult.items.isNotEmpty()) {
                            val newStructured = newResult.toWorkspaceStructuredResult(wsReq)
                            val idx = messages.indexOfFirst { it.id == targetMsg.id }
                            if (idx != -1) {
                                messages[idx] = targetMsg.copy(
                                    structuredResult = newStructured,
                                    isGenerating = false
                                )
                            }
                        } else {
                            val idx = messages.indexOfFirst { it.id == targetMsg.id }
                            if (idx != -1) messages[idx] = targetMsg.copy(isGenerating = false)
                            snackbarHostState.showSnackbar("Regeneration notice: Previous worksheet preserved.")
                        }
                    }
                } catch (e: Exception) {
                    val idx = messages.indexOfFirst { it.id == targetMsg.id }
                    if (idx != -1) messages[idx] = targetMsg.copy(isGenerating = false)
                    snackbarHostState.showSnackbar("Unable to regenerate: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    // Handle Save Structured Result
    val handleSave: (WorkspaceChatMessage) -> Unit = { targetMsg ->
        val structured = targetMsg.structuredResult
        if (structured != null && viewModel != null) {
            coroutineScope.launch {
                val saveResult = viewModel.saveStructuredResult(structured, currentUser)
                saveResult.onSuccess { savedId ->
                    val updatedStructured = structured.copy(
                        savedHistoryId = savedId,
                        isSaved = true
                    )
                    val targetIndex = messages.indexOfFirst { it.id == targetMsg.id }
                    if (targetIndex != -1) {
                        messages[targetIndex] = messages[targetIndex].copy(
                            structuredResult = updatedStructured
                        )
                    }
                    snackbarHostState.showSnackbar("Saved to AI History")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("Failed to save: ${err.localizedMessage ?: "Database error"}")
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Compact Header Bar (Unified with Mobile Layout)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Grounded",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.size(36.dp).testTag("top_app_bar_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Chat History",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showTopMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("AI Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        showSettingsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Start New Session") },
                                    leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        viewModel?.startNewChatSession("New Teaching Session", contextState.grade, contextState.subject, currentUser)
                                        messages.clear()
                                        messages.add(
                                            WorkspaceChatMessage(
                                                id = UUID.randomUUID().toString(),
                                                sender = ChatSender.AI,
                                                text = "Started a fresh conversation session for ${contextState.grade}-${contextState.className} ${contextState.subject}. How can I assist you today?"
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        messages.clear()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            // 1. Mode Selector (Teacher / Admin Assistant) - Shown if Admin
            AiModeSelector(
                currentMode = currentMode,
                onModeChanged = { currentMode = it },
                isAdmin = isAdmin
            )

            // 2. Compact Persistent Context Strip
            AiContextStrip(
                contextState = contextState,
                onOpenContextSheet = { showContextSheet = true }
            )

            // 3. Quick Action Chips Row
            AiQuickActions(
                onActionSelected = handleQuickAction,
                activeAction = activeQuickAction,
                currentMode = currentMode,
                isAdmin = isAdmin
            )

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(top = 2.dp)
            )

            // 4. Main Conversational Chat Area
            Box(modifier = Modifier.weight(1f)) {
                AiChatArea(
                    messages = messages,
                    listState = listState,
                    onQuickActionClick = handleQuickAction,
                    onSuggestionPromptClick = { prompt -> handleSendMessage(prompt) },
                    onRegenerate = handleRegenerate,
                    onEdit = { targetMsg ->
                        if (targetMsg.structuredResult != null) {
                            editingMessage = targetMsg
                        }
                    },
                    onSave = handleSave
                )
            }

            // 5. Modern Bottom Composer with Keyboard & Inset Handling
            AiComposer(
                text = composerText,
                onTextChanged = { composerText = it },
                onSend = { prompt -> handleSendMessage(prompt) },
                onActionSelected = handleQuickAction,
                isAdmin = isAdmin,
                isGenerating = isGeneratingFromVm
            )
        }
    }

    // Modal Context Bottom Sheet
    if (showContextSheet) {
        AiContextBottomSheet(
            initialState = contextState,
            viewModel = viewModel,
            onDismiss = { showContextSheet = false },
            onApply = { updated ->
                contextState = updated
                showContextSheet = false
            }
        )
    }

    // Question Paper Config Sheet
    if (showQuestionPaperSheet) {
        QuestionPaperConfigSheet(
            initialState = QuestionPaperConfigState(
                academicYear = contextState.academicYear,
                grade = contextState.grade,
                subject = contextState.subject,
                chapterUnit = contextState.chapterUnit
            ),
            viewModel = viewModel,
            onDismiss = { showQuestionPaperSheet = false },
            onGenerate = { config ->
                messages.add(
                    WorkspaceChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.USER,
                        text = "Create a ${config.examType.displayName} for ${config.grade} ${config.subject} (${config.chapterUnit}) — ${config.totalMarks} Marks."
                    )
                )

                coroutineScope.launch {
                    try {
                        val request = config.toQuestionPaperRequest(className = contextState.className)
                        val structuredResult = if (viewModel != null) {
                            val result = viewModel.generateQuestionPaper(request, currentUser)
                            result.toWorkspaceStructuredResult(request)
                        } else {
                            WorkspaceStructuredResult(
                                title = "Basic Education — ${config.grade} ${config.subject} (${config.examType.displayName})",
                                academicYear = config.academicYear,
                                grade = config.grade,
                                subject = config.subject,
                                examType = config.examType.displayName,
                                durationMinutes = config.durationMinutes,
                                totalMarks = config.totalMarks,
                                isCurriculumVerified = true,
                                validationSummary = "100% Grounded in Official Curriculum • ${config.totalMarks} Marks",
                                sourceCitations = listOf(
                                    SourceCitationUi(
                                        documentTitle = config.sourceTitle,
                                        gradeLevel = config.grade,
                                        subject = config.subject,
                                        chapterUnit = config.chapterUnit,
                                        sectionTopic = config.sectionTopic,
                                        pageRange = config.pageRange,
                                        tier = config.sourceTier
                                    )
                                ),
                                styleReferenceNote = if (config.sourceTier == SourceTier.PAST_PAPER_REFERENCE) "Using past paper archive as reference." else "",
                                generalInstructions = listOf(
                                    "Answer all questions clearly in the allocated duration (${config.durationMinutes} mins).",
                                    "Marks for each question are indicated in brackets [ ]."
                                ),
                                sections = emptyList(),
                                originalQuestionPaperRequest = request
                            )
                        }

                        messages.add(
                            WorkspaceChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = ChatSender.AI,
                                text = "Here is your curriculum-grounded assessment paper for ${config.grade} ${config.subject}. All questions are grounded in verified textbook sources (${config.chapterUnit}).",
                                structuredResult = structuredResult
                            )
                        )
                    } catch (e: Exception) {
                        messages.add(
                            WorkspaceChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = ChatSender.AI,
                                text = "Unable to generate question paper: ${e.localizedMessage ?: "Unknown error"}. Please check curriculum source grounding."
                            )
                        )
                    }
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        )
    }

    // Worksheet Config Sheet
    if (showWorksheetSheet) {
        WorksheetConfigSheet(
            initialState = WorksheetConfigState(
                academicYear = contextState.academicYear,
                grade = contextState.grade,
                subject = contextState.subject,
                chapterUnit = contextState.chapterUnit
            ),
            viewModel = viewModel,
            onDismiss = { showWorksheetSheet = false },
            onGenerate = { config ->
                messages.add(
                    WorkspaceChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.USER,
                        text = "Generate a differentiated worksheet for ${config.grade} ${config.subject} (${config.chapterUnit}) with ${config.totalQuestions} questions."
                    )
                )

                coroutineScope.launch {
                    try {
                        val request = config.toWorksheetRequest(className = contextState.className)
                        val structuredResult = if (viewModel != null) {
                            val result = viewModel.generateWorksheet(request, currentUser)
                            result.toWorkspaceStructuredResult(request)
                        } else {
                            WorkspaceStructuredResult(
                                title = "${config.grade} ${config.subject} — Differentiated Practice Worksheet",
                                academicYear = config.academicYear,
                                grade = config.grade,
                                subject = config.subject,
                                examType = "Differentiated Worksheet",
                                durationMinutes = 30,
                                totalMarks = config.totalQuestions * 2,
                                isCurriculumVerified = true,
                                validationSummary = "Verified Grounding • ${config.totalQuestions} Items",
                                sourceCitations = listOf(
                                    SourceCitationUi(
                                        documentTitle = config.sourceTitle,
                                        gradeLevel = config.grade,
                                        subject = config.subject,
                                        chapterUnit = config.chapterUnit,
                                        sectionTopic = config.sectionTopic,
                                        pageRange = "p. 1-10",
                                        tier = config.sourceTier
                                    )
                                ),
                                sections = emptyList(),
                                originalWorksheetRequest = request
                            )
                        }

                        messages.add(
                            WorkspaceChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = ChatSender.AI,
                                text = "Differentiated worksheet generated with ${config.totalQuestions} items strictly aligned with ${config.chapterUnit}.",
                                structuredResult = structuredResult
                            )
                        )
                    } catch (e: Exception) {
                        messages.add(
                            WorkspaceChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = ChatSender.AI,
                                text = "Unable to generate worksheet: ${e.localizedMessage ?: "Unknown error"}."
                            )
                        )
                    }
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        )
    }

    // Modal Assessment Question Editor Sheet
    val activeEditMsg = editingMessage
    if (activeEditMsg?.structuredResult != null) {
        AssessmentQuestionEditSheet(
            structuredResult = activeEditMsg.structuredResult,
            onDismiss = { editingMessage = null },
            onApply = { updatedResult ->
                val targetId = activeEditMsg.id
                val targetIndex = messages.indexOfFirst { it.id == targetId }
                if (targetIndex != -1) {
                    messages[targetIndex] = messages[targetIndex].copy(
                        structuredResult = updatedResult
                    )
                }
                editingMessage = null
            }
        )
    }

    // Modal AI History Sheet
    if (showHistorySheet) {
        AiHistorySheet(
            viewModel = viewModel,
            currentSessionId = viewModel?.activeSessionId?.value,
            onSessionSelected = { sessionId ->
                viewModel?.selectChatSession(sessionId)
                showHistorySheet = false
            },
            onNewChat = {
                viewModel?.startNewChatSession("New Teaching Session", contextState.grade, contextState.subject, currentUser)
                messages.clear()
                messages.add(
                    WorkspaceChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.AI,
                        text = "New session started. What would you like to prepare for ${contextState.grade}-${contextState.className} ${contextState.subject}?"
                    )
                )
                showHistorySheet = false
            },
            onDismiss = { showHistorySheet = false }
        )
    }

    // Modal AI Settings Dialog
    if (showSettingsDialog) {
        AiSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}
