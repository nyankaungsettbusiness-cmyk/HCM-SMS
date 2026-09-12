package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact Mode Selector for Phase 10B AI Assistant Workspace.
 * [ Teacher Assistant ] [ Admin Assistant ]
 * Only rendered for administrators with a low vertical profile (~30dp).
 */
@Composable
fun AiModeSelector(
    currentMode: AiAssistantMode,
    onModeChanged: (AiAssistantMode) -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isAdmin) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .testTag("ai_mode_selector")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Teacher Assistant Tab
            val isTeacherSelected = currentMode == AiAssistantMode.TEACHER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isTeacherSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f))
                    .clickable { onModeChanged(AiAssistantMode.TEACHER) }
                    .padding(vertical = 4.dp)
                    .testTag("mode_teacher_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (isTeacherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Teacher",
                        fontSize = 11.sp,
                        fontWeight = if (isTeacherSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTeacherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Admin Assistant Tab
            val isAdminSelected = currentMode == AiAssistantMode.ADMIN
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isAdminSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f))
                    .clickable { onModeChanged(AiAssistantMode.ADMIN) }
                    .padding(vertical = 4.dp)
                    .testTag("mode_admin_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (isAdminSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Admin",
                        fontSize = 11.sp,
                        fontWeight = if (isAdminSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAdminSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
