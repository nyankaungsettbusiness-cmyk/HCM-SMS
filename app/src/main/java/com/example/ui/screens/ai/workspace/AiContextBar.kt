package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact, non-intrusive Context Bar for Phase 3A AI Workspace.
 * Shows: 2026–2027 • Teacher • G5 • English
 */
@Composable
fun AiContextBar(
    academicYear: String,
    roleTitle: String,
    selectedGrade: String,
    selectedSubject: String,
    onGradeChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { showContextDialog = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$academicYear • $roleTitle • $selectedGrade • $selectedSubject",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "Change Context Scope",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showContextDialog) {
        AlertDialog(
            onDismissRequest = { showContextDialog = false },
            title = {
                Text(
                    "Select Teaching Context",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Teaching Grade:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("KG", "G1", "G5", "G7", "G10").forEach { grade ->
                            FilterChip(
                                selected = selectedGrade == grade,
                                onClick = { onGradeChange(grade) },
                                label = { Text(grade, fontSize = 12.sp) }
                            )
                        }
                    }

                    Text(
                        "Teaching Subject:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("English", "Mathematics", "Science", "Myanmar").forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { onSubjectChange(subj) },
                                label = { Text(subj, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
