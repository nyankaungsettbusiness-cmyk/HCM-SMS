package com.example.ui.screens.holistic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssessmentPeriodEntity
import com.example.data.local.entity.HolisticCategoryEntity
import com.example.data.local.entity.SgiCategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHolisticCategoryDialog(
    categoryToEdit: HolisticCategoryEntity? = null,
    initialEducationLevel: String = "PRIMARY",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, pillar: String, level: String, maxStars: Int) -> Unit
) {
    var name by remember { mutableStateOf(categoryToEdit?.categoryName ?: "") }
    var description by remember { mutableStateOf(categoryToEdit?.description ?: "") }
    var selectedPillar by remember { mutableStateOf(categoryToEdit?.pillar ?: "HONESTY") }
    var selectedLevel by remember { mutableStateOf(categoryToEdit?.educationLevel ?: initialEducationLevel) }
    var maxStarsText by remember { mutableStateOf((categoryToEdit?.maxStars ?: 5).toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (categoryToEdit == null) "Add HCM Assessment Item" else "Edit HCM Assessment Item",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Item Name *") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Evaluation Criteria") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_desc_input")
                )

                Column {
                    Text("HCM Core Value Pillar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val pillars = listOf("HONESTY" to "Honesty (H)", "CURIOSITY" to "Curiosity (C)", "MINDFULNESS" to "Mindfulness (M)")
                        for (pair in pillars) {
                            val pCode = pair.first
                            val pLabel = pair.second
                            FilterChip(
                                selected = selectedPillar == pCode,
                                onClick = { selectedPillar = pCode },
                                label = { Text(pLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Column {
                    Text("Grade Level", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val levels = listOf(
                            "KINDERGARTEN" to "KG",
                            "PRIMARY" to "Primary",
                            "SECONDARY" to "Secondary",
                            "HIGH_SCHOOL" to "High School"
                        )
                        for (pair in levels) {
                            val lCode = pair.first
                            val lLabel = pair.second
                            FilterChip(
                                selected = selectedLevel == lCode,
                                onClick = { selectedLevel = lCode },
                                label = { Text(lLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = maxStarsText,
                    onValueChange = { maxStarsText = it },
                    label = { Text("Max Scale Stars (e.g. 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_max_stars_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val stars = maxStarsText.toIntOrNull()?.coerceIn(3, 10) ?: 5
                    onConfirm(name.trim(), description.trim(), selectedPillar, selectedLevel, stars)
                },
                modifier = Modifier.testTag("save_category_btn")
            ) {
                Text(if (categoryToEdit == null) "Add Item" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSgiCategoryDialog(
    indicatorToEdit: SgiCategoryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, maxStars: Int) -> Unit
) {
    var name by remember { mutableStateOf(indicatorToEdit?.indicatorName ?: "") }
    var description by remember { mutableStateOf(indicatorToEdit?.description ?: "") }
    var maxStarsText by remember { mutableStateOf((indicatorToEdit?.maxStars ?: 5).toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (indicatorToEdit == null) "Add SGI Growth Indicator" else "Edit SGI Growth Indicator",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Indicator Name *") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sgi_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Growth Target Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sgi_desc_input")
                )

                OutlinedTextField(
                    value = maxStarsText,
                    onValueChange = { maxStarsText = it },
                    label = { Text("Max Rating Stars (e.g. 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sgi_max_stars_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val stars = maxStarsText.toIntOrNull()?.coerceIn(3, 10) ?: 5
                    onConfirm(name.trim(), description.trim(), stars)
                },
                modifier = Modifier.testTag("save_sgi_btn")
            ) {
                Text(if (indicatorToEdit == null) "Add Indicator" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CopyConfigConfirmDialog(
    currentSourceLevel: String,
    onDismiss: () -> Unit,
    onConfirmCopy: (sourceLevel: String, targetLevel: String) -> Unit
) {
    var sourceLevel by remember { mutableStateOf(currentSourceLevel) }
    var targetLevel by remember { mutableStateOf(if (currentSourceLevel == "PRIMARY") "SECONDARY" else "HIGH_SCHOOL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Grade Level HCM Configuration", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Duplicate all configured HCM items from source level to target level.", fontSize = 13.sp)

                Text("Source Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val levels = listOf("KINDERGARTEN", "PRIMARY", "SECONDARY", "HIGH_SCHOOL")
                    for (lvl in levels) {
                        FilterChip(
                            selected = sourceLevel == lvl,
                            onClick = { sourceLevel = lvl },
                            label = { Text(lvl, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Target Level (Will overwrite target HCM items):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val levels = listOf("KINDERGARTEN", "PRIMARY", "SECONDARY", "HIGH_SCHOOL")
                    for (lvl in levels) {
                        FilterChip(
                            selected = targetLevel == lvl,
                            onClick = { targetLevel = lvl },
                            label = { Text(lvl, fontSize = 10.sp) },
                            enabled = lvl != sourceLevel
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmCopy(sourceLevel, targetLevel) },
                enabled = sourceLevel != targetLevel
            ) {
                Text("Copy & Replace Target Config")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssessmentPeriodDialog(
    periodToEdit: AssessmentPeriodEntity? = null,
    initialEducationLevel: String = "PRIMARY",
    onDismiss: () -> Unit,
    onConfirm: (name: String, level: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(periodToEdit?.periodName ?: "") }
    var selectedLevel by remember { mutableStateOf(periodToEdit?.educationLevel ?: initialEducationLevel) }
    var category by remember { mutableStateOf(periodToEdit?.periodCategory ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (periodToEdit == null) "Add Assessment Period" else "Edit Assessment Period",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Period Name *") },
                    placeholder = { Text("E.g. June, July, August") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_name_input")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category / Group Name (Optional)") },
                    placeholder = { Text("E.g. Monthly") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_category_input")
                )

                Column {
                    Text("Target Grade Level", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val levels = listOf(
                            "KINDERGARTEN" to "Kindergarten (KG)",
                            "PRIMARY" to "Primary (G1-G5)",
                            "SECONDARY" to "Secondary (G6-G9)",
                            "HIGH_SCHOOL" to "High School (G10-G12)"
                        )
                        for (pair in levels) {
                            val lCode = pair.first
                            val lLabel = pair.second
                            FilterChip(
                                selected = selectedLevel == lCode,
                                onClick = { selectedLevel = lCode },
                                label = { Text(lLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        return@Button
                    }
                    onConfirm(name.trim(), selectedLevel, category.trim())
                },
                modifier = Modifier.testTag("save_period_btn")
            ) {
                Text(if (periodToEdit == null) "Add Period" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
