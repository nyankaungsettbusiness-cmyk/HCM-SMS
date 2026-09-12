package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern, clean AI Message Composer for Phase 6.
 * Features:
 * - [ + ] quick action launcher
 * - Multi-line responsive text input
 * - High-contrast Send button
 * - Keyboard & navigation insets handling
 */
@Composable
fun AiComposer(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onActionSelected: (QuickActionType) -> Unit,
    isAdmin: Boolean = false,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showPlusMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Plus Menu Dropdown Overlay / Actions Palette
            DropdownMenu(
                expanded = showPlusMenu,
                onDismissRequest = { showPlusMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Questions / Assessment Paper") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showPlusMenu = false
                        onActionSelected(QuickActionType.QUESTION_PAPER)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Differentiated Worksheet") },
                    leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showPlusMenu = false
                        onActionSelected(QuickActionType.WORKSHEET)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Structured Lesson Plan") },
                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showPlusMenu = false
                        onActionSelected(QuickActionType.LESSON_PLAN)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Interactive Class Activity") },
                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showPlusMenu = false
                        onActionSelected(QuickActionType.CLASS_ACTIVITY)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Bilingual Report Card Comment") },
                    leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showPlusMenu = false
                        onActionSelected(QuickActionType.REPORT_COMMENT)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Teaching Visual / Diagram") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showPlusMenu = false
                        onActionSelected(QuickActionType.TEACHING_VISUAL)
                    }
                )
                if (isAdmin) {
                    Divider()
                    DropdownMenuItem(
                        text = { Text("School Analytics & Trends") },
                        leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        onClick = {
                            showPlusMenu = false
                            onActionSelected(QuickActionType.SCHOOL_ANALYTICS)
                        }
                    )
                }
            }

            // Input Row: [ + ] [ TextField ] [ Send ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Plus Button
                IconButton(
                    onClick = { showPlusMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .testTag("ai_composer_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create or Attach Action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = "Ask AI or instruct adjustments (English / မြန်မာ)...",
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_composer_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                // Send Button
                val canSend = text.isNotBlank() && !isGenerating
                IconButton(
                    onClick = {
                        if (canSend) {
                            onSend(text.trim())
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .testTag("ai_composer_send")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
