package com.example.ui.screens.ai.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Structured Result Canvas Component for Phase 10B AI Assistant.
 * Mobile-first compact result card with collapsible section accordions,
 * clean typography, clear answer keys/rubrics, and a responsive action toolbar.
 */
@Composable
fun AiStructuredResultCard(
    result: WorkspaceStructuredResult,
    isRegenerating: Boolean = false,
    onEdit: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onSave: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showAnswerKey by remember { mutableStateOf(false) }
    var showRubric by remember { mutableStateOf(false) }
    var isSaved by remember(result.isSaved, result.savedHistoryId) { mutableStateOf(result.isSaved || result.savedHistoryId != null) }
    var isCopied by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Track expanded state for each section (Default: expand first section, or all if 1-2 sections)
    val expandedSections = remember(result.sections) {
        mutableStateMapOf<Int, Boolean>().apply {
            result.sections.forEachIndexed { index, _ ->
                put(index, index == 0 || result.sections.size <= 2)
            }
        }
    }

    val allExpanded = result.sections.indices.all { expandedSections[it] == true }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Regeneration Indicator
            AnimatedVisibility(visible = isRegenerating) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Generating a new question set...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Header Bar: Title + Total Marks & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${result.academicYear} • ${result.grade} ${result.subject} • ${result.examType}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${result.totalMarks} Marks • ${result.durationMinutes}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Grounding & Verification Badge Box (Compact)
            Surface(
                color = if (result.isCurriculumVerified) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (result.isCurriculumVerified) Color(0xFF81C784) else MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Curriculum Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }

                    if (result.sourceCitations.isNotEmpty()) {
                        val citation = result.sourceCitations.first()
                        Text(
                            text = "Source: ${citation.gradeLevel} ${citation.subject} • ${citation.chapterUnit} • ${citation.sectionTopic} (${citation.pageRange})",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    if (result.styleReferenceNote.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "REF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = result.styleReferenceNote,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // General Instructions (if any - compact)
            if (result.generalInstructions.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Instructions:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        result.generalInstructions.forEach { ins ->
                            Text("• $ins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Section Accordion Header Action (Expand/Collapse All)
            if (result.sections.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sections (${result.sections.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = if (allExpanded) "Collapse All" else "Expand All",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val target = !allExpanded
                                result.sections.indices.forEach { expandedSections[it] = target }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Sections & Questions Preview (Accordion Items)
            result.sections.forEachIndexed { sIndex, section ->
                val isExpanded = expandedSections[sIndex] ?: true
                val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "accordion_arrow")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                ) {
                    // Clickable Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSections[sIndex] = !isExpanded
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(rotationAngle),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = section.sectionName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${section.questions.size} Qs",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "[${section.sectionMarks} M]",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Collapsible Content
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (section.sectionInstruction.isNotBlank()) {
                                Text(
                                    text = section.sectionInstruction,
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                )
                            }

                            // Questions in Section
                            section.questions.forEach { q ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "${q.questionNumber}. ${q.questionText}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (q.isManuallyEdited) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Edited",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "[${q.marks}]",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        )
                                    }

                                    // Options if MCQ
                                    if (q.options.isNotEmpty()) {
                                        Column(modifier = Modifier.padding(start = 12.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            q.options.forEach { opt ->
                                                Text(opt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    // Answer Key & Rubric inline toggle content
                                    AnimatedVisibility(visible = showAnswerKey) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 3.dp)
                                        ) {
                                            Text(
                                                text = "Key: ${q.correctAnswer}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(5.dp)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = showRubric) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "Rubric: ${q.markingGuide}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Toggles for Answer Key & Rubric (Compact Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = showAnswerKey,
                    onClick = { showAnswerKey = !showAnswerKey },
                    label = { Text(if (showAnswerKey) "Hide Answers" else "Answer Key", fontSize = 10.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showAnswerKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = showRubric,
                    onClick = { showRubric = !showRubric },
                    label = { Text(if (showRubric) "Hide Rubric" else "Rubric", fontSize = 10.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Grading,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action Toolbar: Mobile-Optimized ([Edit] [Regenerate] [Export] [Save] [Copy])
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Edit", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (!isRegenerating) {
                            onRegenerate()
                        }
                    },
                    enabled = !isRegenerating,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Regenerate", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Export", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = {
                        onSave()
                        isSaved = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSaved) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(if (isSaved) Icons.Default.Check else Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isSaved) "Saved" else "Save", fontSize = 11.sp)
                }

                IconButton(
                    onClick = {
                        val allText = buildString {
                            appendLine(result.title)
                            appendLine("${result.academicYear} • ${result.grade} ${result.subject}")
                            appendLine("Total Marks: ${result.totalMarks} • Duration: ${result.durationMinutes} mins")
                            appendLine()
                            result.sections.forEach { s ->
                                appendLine("${s.sectionName} (${s.sectionMarks} Marks)")
                                appendLine(s.sectionInstruction)
                                s.questions.forEach { q ->
                                    appendLine("${q.questionNumber}. ${q.questionText} [${q.marks}]")
                                    q.options.forEach { opt -> appendLine("   $opt") }
                                }
                                appendLine()
                            }
                        }
                        clipboardManager.setText(AnnotatedString(allText))
                        isCopied = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = if (isCopied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        CurriculumExportDialog(
            result = result,
            onDismiss = { showExportDialog = false }
        )
    }
}
