package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AiAssistantViewModel

/**
 * Progressive Disclosure Configuration Bottom Sheet for Question Papers / Quizzes.
 * Mobile-first, fixed footer buttons, scrollable content area preventing off-screen buttons on small displays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaperConfigSheet(
    initialState: QuestionPaperConfigState,
    viewModel: AiAssistantViewModel? = null,
    onDismiss: () -> Unit,
    onGenerate: (QuestionPaperConfigState) -> Unit
) {
    var state by remember { mutableStateOf(initialState) }
    var currentStep by remember { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header with Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Create Assessment Paper",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Step $currentStep of 3: " + when (currentStep) {
                            1 -> "Assessment Purpose & Type"
                            2 -> "Curriculum & Source Grounding"
                            else -> "Marks & Duration Specifications"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Scrollable Form Body (Ensures content never pushes buttons off-screen)
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                when (currentStep) {
                    1 -> Step1PurposeAndType(state = state, onUpdate = { state = it })
                    2 -> Step2CurriculumSourceScope(state = state, viewModel = viewModel, onUpdate = { state = it })
                    3 -> Step3PaperStructureAndMarks(state = state, onUpdate = { state = it })
                }
            }

            // Pinned Bottom Navigation Buttons Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = { currentStep += 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Next Step")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                onGenerate(state)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Paper", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1PurposeAndType(
    state: QuestionPaperConfigState,
    onUpdate: (QuestionPaperConfigState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "1. What kind of assessment are you creating?",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuestionPurposeUi.values().forEach { purpose ->
                val isSelected = state.purpose == purpose
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(state.copy(purpose = purpose)) }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (purpose) {
                                QuestionPurposeUi.OFFICIAL_EXAM -> Icons.Default.Description
                                QuestionPurposeUi.CLASSROOM_QUIZ -> Icons.Default.Quiz
                                QuestionPurposeUi.PRACTICE_REVISION -> Icons.Default.EditNote
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = purpose.displayName.replace(" ", "\n"),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text(
            "2. Select Examination Format:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )

        val relevantExamTypes = when (state.purpose) {
            QuestionPurposeUi.OFFICIAL_EXAM -> listOf(
                ExamTypeUi.PILOT_TEST,
                ExamTypeUi.CET,
                ExamTypeUi.MONTHLY_TEST,
                ExamTypeUi.MID_TERM,
                ExamTypeUi.FINAL_EXAM,
                ExamTypeUi.CUSTOM
            )
            QuestionPurposeUi.CLASSROOM_QUIZ -> listOf(
                ExamTypeUi.QUICK_QUIZ,
                ExamTypeUi.COMPREHENSIVE_QUIZ
            )
            QuestionPurposeUi.PRACTICE_REVISION -> listOf(
                ExamTypeUi.MONTHLY_TEST,
                ExamTypeUi.CUSTOM
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            relevantExamTypes.forEach { examType ->
                val isSelected = state.examType == examType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable {
                            onUpdate(
                                state.copy(
                                    examType = examType,
                                    totalMarks = examType.defaultMarks,
                                    durationMinutes = examType.defaultDuration
                                )
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onUpdate(
                                    state.copy(
                                        examType = examType,
                                        totalMarks = examType.defaultMarks,
                                        durationMinutes = examType.defaultDuration
                                    )
                                )
                            }
                        )
                        Text(
                            text = examType.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        )
                    }
                    Text(
                        text = "${examType.defaultMarks} pts • ${examType.defaultDuration}m",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Step2CurriculumSourceScope(
    state: QuestionPaperConfigState,
    viewModel: AiAssistantViewModel?,
    onUpdate: (QuestionPaperConfigState) -> Unit
) {
    var dynamicUnits by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(state.grade, state.subject) {
        if (viewModel != null) {
            val units = viewModel.getAvailableUnits(state.grade, state.subject)
            dynamicUnits = units
            if (units.isNotEmpty() && !units.contains(state.chapterUnit)) {
                onUpdate(state.copy(chapterUnit = units.first()))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Grade & Subject Selection
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Grade Level", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("KG", "G1", "G5", "G7", "G8", "G10").take(4).forEach { grade ->
                        FilterChip(
                            selected = state.grade == grade,
                            onClick = { onUpdate(state.copy(grade = grade)) },
                            label = { Text(grade, fontSize = 10.sp) }
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1.2f)) {
                Text("Subject", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("English", "Math", "Science", "Myanmar").take(3).forEach { subj ->
                        FilterChip(
                            selected = state.subject == subj,
                            onClick = { onUpdate(state.copy(subject = subj)) },
                            label = { Text(subj, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // Source Tier Selection
        Text(
            "Curriculum Source of Truth:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        SourceTier.values().forEach { tier ->
            val isSelected = state.sourceTier == tier
            Surface(
                color = if (isSelected) {
                    if (tier.isReferenceOnly) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        if (tier.isReferenceOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdate(state.copy(sourceTier = tier)) }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = if (tier.isReferenceOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tier.badgeText,
                                    color = if (tier.isReferenceOnly) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(tier.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (tier.isReferenceOnly) {
                            Text(
                                text = "Structure reference. Questions ground only in verified textbooks.",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(selected = isSelected, onClick = { onUpdate(state.copy(sourceTier = tier)) })
                }
            }
        }

        // Chapter / Unit Scope
        Text("Chapter / Unit Scope:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        val fallbackUnits = listOf(
            "Unit 1: Food and Nutrition",
            "Unit 2: Animals and Their Habitats",
            "Unit 3: Living Things and Surroundings",
            "Unit 4: Matter and Energy"
        )
        val unitsToShow = if (dynamicUnits.isNotEmpty()) dynamicUnits else fallbackUnits

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            unitsToShow.forEach { unit ->
                val isSelected = state.chapterUnit == unit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .clickable { onUpdate(state.copy(chapterUnit = unit)) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(unit, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Step3PaperStructureAndMarks(
    state: QuestionPaperConfigState,
    onUpdate: (QuestionPaperConfigState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Marks & Duration
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total Marks:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(10, 25, 50, 100).forEach { marks ->
                        FilterChip(
                            selected = state.totalMarks == marks,
                            onClick = { onUpdate(state.copy(totalMarks = marks)) },
                            label = { Text("$marks pts", fontSize = 10.sp) }
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Duration:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = state.durationMinutes == mins,
                            onClick = { onUpdate(state.copy(durationMinutes = mins)) },
                            label = { Text("${mins}m", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // Language
        Column {
            Text("Assessment Language:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("English", "Myanmar", "Bilingual").forEach { lang ->
                    FilterChip(
                        selected = state.language == lang || (lang == "Bilingual" && state.language.startsWith("Bilingual")),
                        onClick = { onUpdate(state.copy(language = if (lang == "Bilingual") "Bilingual (Mixed)" else lang)) },
                        label = { Text(lang, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Summary Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Text("Grounding Verification Summary", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text("• Target: ${state.grade} ${state.subject} (${state.examType.displayName})", fontSize = 10.sp)
                Text("• Scope: ${state.chapterUnit}", fontSize = 10.sp)
                Text("• Total Marks: ${state.totalMarks} pts | Duration: ${state.durationMinutes} mins", fontSize = 10.sp)
                Text("• Tier: ${state.sourceTier.displayName}", fontSize = 10.sp)
            }
        }
    }
}
