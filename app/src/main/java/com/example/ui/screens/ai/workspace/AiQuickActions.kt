package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact Quick Action Chips for Phase 10B AI Workspace.
 * Low vertical profile (~32dp) with clean iconography and distinct states.
 */
@Composable
fun AiQuickActions(
    onActionSelected: (QuickActionType) -> Unit,
    modifier: Modifier = Modifier,
    activeAction: QuickActionType? = null,
    currentMode: AiAssistantMode = AiAssistantMode.TEACHER,
    isAdmin: Boolean = false
) {
    val visibleActions = when (currentMode) {
        AiAssistantMode.TEACHER -> listOf(
            QuickActionType.QUESTION_PAPER,
            QuickActionType.WORKSHEET,
            QuickActionType.LESSON_PLAN,
            QuickActionType.REPORT_COMMENT,
            QuickActionType.CLASS_ACTIVITY,
            QuickActionType.TEACHING_VISUAL
        )
        AiAssistantMode.ADMIN -> if (isAdmin) {
            listOf(
                QuickActionType.SCHOOL_ANALYTICS,
                QuickActionType.ATTENDANCE_OVERVIEW,
                AiAssistantModeAction.EXAM_PERFORMANCE,
                QuickActionType.AT_RISK_STUDENTS,
                QuickActionType.REPORT_COMMENT
            ).filterIsInstance<QuickActionType>()
        } else {
            listOf(
                QuickActionType.QUESTION_PAPER,
                QuickActionType.WORKSHEET,
                QuickActionType.LESSON_PLAN
            )
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .testTag("ai_quick_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(vertical = 1.dp)
    ) {
        items(visibleActions) { action ->
            val isSelected = activeAction == action
            SuggestionChip(
                onClick = { onActionSelected(action) },
                label = {
                    Text(
                        text = action.title,
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                },
                icon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("quick_action_${action.name.lowercase()}")
            )
        }
    }
}

private object AiAssistantModeAction {
    val EXAM_PERFORMANCE = QuickActionType.EXAM_PERFORMANCE
}
