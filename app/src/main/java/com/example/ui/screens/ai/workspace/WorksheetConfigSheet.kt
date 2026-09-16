package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AiAssistantViewModel

/**
 * Compact, Progressive Differentiated Worksheet Configuration Sheet.
 * Mobile-first layout with scrollable content and pinned footer actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksheetConfigSheet(
    initialState: WorksheetConfigState,
    viewModel: AiAssistantViewModel? = null,
    onDismiss: () -> Unit,
    onGenerate: (WorksheetConfigState) -> Unit
) {
    var state by remember { mutableStateOf(initialState) }
    var dynamicUnits by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(state.grade, state.subject) {
        if (viewModel != null) {
            val units = viewModel.getAvailableUnits(state.grade, state.subject)
            dynamicUnits = units
            if (units.isNotEmpty() && !units.contains(state.chapterUnit)) {
                state = state.copy(chapterUnit = units.first())
            }
        }
    }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Create Differentiated Worksheet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Curriculum-grounded progressive tiered practice",
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

            // Scrollable Content
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Grade & Subject Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grade", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("KG", "G1", "G5", "G7", "G8", "G10").take(4).forEach { grade ->
                                FilterChip(
                                    selected = state.grade == grade,
                                    onClick = { state = state.copy(grade = grade) },
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
                                    onClick = { state = state.copy(subject = subj) },
                                    label = { Text(subj, fontSize = 10.sp) }
                                )
                            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    unitsToShow.take(3).forEach { unit ->
                        val isSelected = state.chapterUnit == unit
                        FilterChip(
                            selected = isSelected,
                            onClick = { state = state.copy(chapterUnit = unit) },
                            label = { Text(unit.take(16) + "...", fontSize = 10.sp) }
                        )
                    }
                }

                // Learning Objective
                OutlinedTextField(
                    value = state.learningObjective,
                    onValueChange = { state = state.copy(learningObjective = it) },
                    label = { Text("Target Learning Objective", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Differentiated Difficulty Split (Easy / Medium / Challenge)
                Text(
                    "Difficulty Split (Total: ${state.totalQuestions} Questions):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Easy Stepper
                    DifficultyStepperCard(
                        title = "Easy",
                        count = state.easyCount,
                        onIncrement = { state = state.copy(easyCount = state.easyCount + 1) },
                        onDecrement = { if (state.easyCount > 1) state = state.copy(easyCount = state.easyCount - 1) },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    // Medium Stepper
                    DifficultyStepperCard(
                        title = "Medium",
                        count = state.mediumCount,
                        onIncrement = { state = state.copy(mediumCount = state.mediumCount + 1) },
                        onDecrement = { if (state.mediumCount > 1) state = state.copy(mediumCount = state.mediumCount - 1) },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    // Challenge Stepper
                    DifficultyStepperCard(
                        title = "Challenge",
                        count = state.challengeCount,
                        onIncrement = { state = state.copy(challengeCount = state.challengeCount + 1) },
                        onDecrement = { if (state.challengeCount > 0) state = state.copy(challengeCount = state.challengeCount - 1) },
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pinned Bottom Action Button Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Button(
                        onClick = {
                            onGenerate(state)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Worksheet (${state.totalQuestions} items)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyStepperCard(
    title: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(12.dp))
                }
                Text("$count", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
