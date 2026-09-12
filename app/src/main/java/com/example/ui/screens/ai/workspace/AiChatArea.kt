package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Conversational Chat Message Feed for Phase 6 AI Workspace.
 * Clean, compact, with welcoming empty state & suggestion prompts.
 */
@Composable
fun AiChatArea(
    messages: List<WorkspaceChatMessage>,
    listState: LazyListState,
    onQuickActionClick: (QuickActionType) -> Unit,
    onSuggestionPromptClick: (String) -> Unit = {},
    onRegenerate: (WorkspaceChatMessage) -> Unit = {},
    onEdit: (WorkspaceChatMessage) -> Unit = {},
    onSave: (WorkspaceChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        AiEmptyWelcomeState(
            onSuggestionClick = onSuggestionPromptClick,
            onQuickActionClick = onQuickActionClick,
            modifier = modifier
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .testTag("ai_chat_message_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                when (message.sender) {
                    ChatSender.USER -> UserMessageBubble(message = message)
                    ChatSender.AI -> AiMessageBubble(
                        message = message,
                        onQuickActionClick = onQuickActionClick,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onSave = onSave
                    )
                    ChatSender.SYSTEM -> SystemMessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
fun AiEmptyWelcomeState(
    onSuggestionClick: (String) -> Unit,
    onQuickActionClick: (QuickActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_empty_welcome_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Curriculum-grounded lesson planning, exam generator, and bilingual teacher assistance.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Quick Suggestions",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Compact Suggestion Cards
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionCard(
                icon = Icons.Default.Description,
                title = "Make questions",
                subtitle = "Generate 10 curriculum questions for Unit 3",
                onClick = { onSuggestionClick("Grade 5 English Unit 3 ကနေ 10 questions လုပ်ပေးပါ") }
            )

            SuggestionCard(
                icon = Icons.Default.Assignment,
                title = "Create a worksheet",
                subtitle = "Tiered practice (Easy, Core, Challenge)",
                onClick = { onQuickActionClick(QuickActionType.WORKSHEET) }
            )

            SuggestionCard(
                icon = Icons.Default.MenuBook,
                title = "Create a lesson plan",
                subtitle = "5E instructional framework with MoE alignment",
                onClick = { onSuggestionClick("Prepare a structured lesson plan for Grade 5 English Unit 3.") }
            )

            SuggestionCard(
                icon = Icons.Default.Comment,
                title = "Generate report comment",
                subtitle = "Bilingual holistic comment for student",
                onClick = { onSuggestionClick("Generate a bilingual report card comment for Grade 5 English.") }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
