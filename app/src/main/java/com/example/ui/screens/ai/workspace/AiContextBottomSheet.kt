package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AiAssistantViewModel

/**
 * Compact Context Bottom Sheet for Phase 6.
 * Allows quick adjustments to Academic Year, Grade, Class, Subject, Unit, and Language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiContextBottomSheet(
    initialState: AiContextConfigState,
    viewModel: AiAssistantViewModel? = null,
    onDismiss: () -> Unit,
    onApply: (AiContextConfigState) -> Unit
) {
    var state by remember { mutableStateOf(initialState) }
    var availableUnits by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(state.grade, state.subject) {
        if (viewModel != null) {
            val units = viewModel.getAvailableUnits(state.grade, state.subject)
            availableUnits = units
            if (units.isNotEmpty() && !units.contains(state.chapterUnit)) {
                state = state.copy(chapterUnit = units.first())
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_context_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Teaching Context & Scope",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Academic Year & Language
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Academic Year", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("2026–2027", "2025–2026")) { yr ->
                            FilterChip(
                                selected = state.academicYear == yr,
                                onClick = { state = state.copy(academicYear = yr) },
                                label = { Text(yr, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Language", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("English", "Myanmar", "Bilingual")) { lang ->
                            FilterChip(
                                selected = state.language.equals(lang, ignoreCase = true),
                                onClick = { state = state.copy(language = lang) },
                                label = { Text(lang, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Grade Level
            Column {
                Text("Grade Level", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("KG", "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8", "G9", "G10", "G11", "G12")) { g ->
                        FilterChip(
                            selected = state.grade == g,
                            onClick = { state = state.copy(grade = g) },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Class / Section
            Column {
                Text("Class Section", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Room A", "Room B", "Room C", "Room D")) { cls ->
                        FilterChip(
                            selected = state.className == cls,
                            onClick = { state = state.copy(className = cls) },
                            label = { Text(cls, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Subject
            Column {
                Text("Teaching Subject", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("English", "Mathematics", "Science", "Myanmar", "Social Studies", "History")) { subj ->
                        FilterChip(
                            selected = state.subject.equals(subj, ignoreCase = true),
                            onClick = { state = state.copy(subject = subj) },
                            label = { Text(subj, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Target Unit / Chapter
            Column {
                Text("Target Chapter / Unit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                val units = if (availableUnits.isNotEmpty()) availableUnits else listOf(
                    "Unit 1: Basic Foundations",
                    "Unit 2: Living Things & Habitats",
                    "Unit 3: Healthy Food and Nutrition",
                    "Unit 4: Matter and Energy",
                    "Unit 5: Revision & Synthesis"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(units) { u ->
                        FilterChip(
                            selected = state.chapterUnit == u,
                            onClick = { state = state.copy(chapterUnit = u) },
                            label = { Text(u, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Curriculum Source
            Column {
                Text("Curriculum Source", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "MoE Myanmar Curriculum (2024–2025 Standard) • Primary Source Grounding",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onApply(state)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Context")
                }
            }
        }
    }
}
