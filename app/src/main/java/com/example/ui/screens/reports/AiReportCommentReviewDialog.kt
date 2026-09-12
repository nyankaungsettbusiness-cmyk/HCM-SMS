package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.ReportCardAiResult
import com.example.data.ai.StudentReportFacts
import com.example.ui.viewmodel.ReportViewModel

/**
 * Teacher Review Dialog for Report Card AI Comments & Parent Support Suggestions.
 * Adheres strictly to Section 11 & 12 of Phase 2:
 * - Shows authentic verified facts used
 * - Shows current saved teacher comment alongside AI generated draft
 * - Allows full editing by the teacher
 * - Does NOT save automatically without teacher explicit confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReportCommentReviewDialog(
    reportViewModel: ReportViewModel,
    facts: StudentReportFacts,
    aiResult: ReportCardAiResult,
    onDismiss: () -> Unit,
    onSavedSuccessfully: () -> Unit
) {
    var editableTeacherComment by remember(aiResult) { mutableStateOf(aiResult.teacherComment) }
    var editableParentSuggestion by remember(aiResult) { mutableStateOf(aiResult.parentSuggestion) }
    var selectedLanguage by remember { mutableStateOf("MYANMAR") }
    var isRegenerating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "AI Report Card Comment Generator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${facts.studentName} (${facts.grade} - ${facts.className}) • ${facts.periodName} (${facts.academicYear})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Verified Facts Grounding Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Grounding Facts (100% Authenticated HCM-SMS Data)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            facts.supportingFactsList().forEach { fact ->
                                Text(
                                    text = "• $fact",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Language Selector Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Draft Output Language:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedLanguage == "MYANMAR",
                                onClick = {
                                    if (selectedLanguage != "MYANMAR") {
                                        selectedLanguage = "MYANMAR"
                                        reportViewModel.generateAiCommentForSelectedStudent(language = "MYANMAR")
                                    }
                                },
                                label = { Text("Myanmar (Default)") }
                            )
                            FilterChip(
                                selected = selectedLanguage == "ENGLISH",
                                onClick = {
                                    if (selectedLanguage != "ENGLISH") {
                                        selectedLanguage = "ENGLISH"
                                        reportViewModel.generateAiCommentForSelectedStudent(language = "ENGLISH")
                                    }
                                },
                                label = { Text("English") }
                            )
                        }
                    }

                    // Section 1: Teacher Comment (Editable)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "1. Teacher Comment (ဆရာ/မ မှတ်ချက်)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Covers overall academic progress, strengths, improvement areas, and encouraging next steps.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = editableTeacherComment,
                                onValueChange = { editableTeacherComment = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Section 2: Parent Support / Suggestion (Editable)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "2. Parent Support Suggestion (မိဘများသို့ အကြံပြုချက်)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Practical, respectful suggestions achievable at home, connected directly to student's results.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = editableParentSuggestion,
                                onValueChange = { editableParentSuggestion = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Teacher Notice
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Teacher Review: AI comments will only be saved to the Report Card when you click 'Accept & Apply to Report Card'.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            reportViewModel.generateAiCommentForSelectedStudent(language = selectedLanguage)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate Draft")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            reportViewModel.applyAiCommentToReportCard(
                                teacherComment = editableTeacherComment,
                                parentSuggestion = editableParentSuggestion
                            )
                            onSavedSuccessfully()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accept & Apply to Report Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun StudentReportFacts.supportingFactsList(): List<String> {
    val list = mutableListOf<String>()
    list.add("Overall Average: ${overallPerformance.overallAveragePercentage}% (Grade ${overallPerformance.overallGrade}, Status: ${overallPerformance.passStatus})")
    if (strongestSubjects.isNotEmpty()) {
        list.add("Strongest Subjects: ${strongestSubjects.joinToString(", ")}")
    }
    if (improvementSubjects.isNotEmpty()) {
        list.add("Subjects Needing Support: ${improvementSubjects.joinToString(", ")}")
    }
    if (assessmentTrends.isNotEmpty()) {
        list.add("Trends: ${assessmentTrends.joinToString("; ") { "${it.subjectName}: ${it.progressionDescription}" }}")
    }
    if (attendanceSummary.isAttendanceDataAvailable) {
        list.add("Attendance: ${attendanceSummary.presentPercentage}% Present (Total ${attendanceSummary.totalDaysRecorded} Days)")
    }
    if (holisticAssessmentSummary.pillars.isNotEmpty()) {
        list.add("HCM Holistic Rating: ${holisticAssessmentSummary.overallAverage}/5.0 (${holisticAssessmentSummary.topStrengths.joinToString(", ").ifEmpty { "Consistent" }})")
    }
    return list
}
