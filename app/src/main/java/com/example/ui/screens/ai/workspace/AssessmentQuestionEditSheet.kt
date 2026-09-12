package com.example.ui.screens.ai.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.CurriculumQuestionValidator
import com.example.data.ai.ValidationReport

/**
 * Phase 3B Step 3: Compact Bottom Sheet Editor for Generated Assessment Questions.
 * Supports question-level editing (text, MCQ options, correct answer, marks, rubric)
 * while preserving curriculum source traceability and enforcing authoritative validation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentQuestionEditSheet(
    structuredResult: WorkspaceStructuredResult,
    onDismiss: () -> Unit,
    onApply: (WorkspaceStructuredResult) -> Unit
) {
    // Immutable original copy for Reset/Undo functionality
    val originalResult = remember { structuredResult }

    // Mutable working state of sections & questions
    var currentSections by remember { mutableStateOf(structuredResult.sections) }
    var selectedQuestionIndex by remember { mutableIntStateOf(0) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var validationWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

    // Flattened question list for indexing
    val allQuestions = remember(currentSections) {
        currentSections.flatMap { it.questions }
    }

    val totalCalculatedMarks = remember(allQuestions) {
        allQuestions.sumOf { it.marks }
    }

    val hasAnyEdits = remember(currentSections, originalResult) {
        val origQuestions = originalResult.sections.flatMap { it.questions }
        val currQuestions = currentSections.flatMap { it.questions }
        if (origQuestions.size != currQuestions.size) true
        else {
            currQuestions.zip(origQuestions).any { (curr, orig) ->
                curr.questionText != orig.questionText ||
                        curr.marks != orig.marks ||
                        curr.correctAnswer != orig.correctAnswer ||
                        curr.markingGuide != orig.markingGuide ||
                        curr.options != orig.options ||
                        curr.questionType != orig.questionType
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
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .imePadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Assessment Questions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${structuredResult.grade} ${structuredResult.subject} • ${allQuestions.size} Questions • Target: ${structuredResult.totalMarks} Marks",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question Navigation Selector Tabs / Chips
            if (allQuestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(allQuestions) { index, q ->
                        val isSelected = index == selectedQuestionIndex
                        val origQ = originalResult.sections.flatMap { it.questions }.getOrNull(index)
                        val isEdited = origQ != null && (
                                q.questionText != origQ.questionText ||
                                        q.marks != origQ.marks ||
                                        q.correctAnswer != origQ.correctAnswer ||
                                        q.markingGuide != origQ.markingGuide ||
                                        q.options != origQ.options
                                )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isEdited -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.clickable {
                                selectedQuestionIndex = index
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Q${q.questionNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isEdited -> MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (isEdited) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Editor Body for Selected Question
            val currentQuestion = allQuestions.getOrNull(selectedQuestionIndex)

            if (currentQuestion != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Section & Question Meta Banner
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentQuestion.sectionName.ifEmpty { "Question ${currentQuestion.questionNumber}" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Type: ${currentQuestion.questionType}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Question Text Editor
                    item {
                        OutlinedTextField(
                            value = currentQuestion.questionText,
                            onValueChange = { newText ->
                                updateCurrentQuestion(
                                    currentSections = currentSections,
                                    selectedQuestionIndex = selectedQuestionIndex,
                                    update = { it.copy(questionText = newText) },
                                    onUpdated = { currentSections = it }
                                )
                            },
                            label = { Text("Question Text (Required)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Marks & Difficulty Allocation
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentQuestion.marks.toString(),
                                onValueChange = { newMarksStr ->
                                    val parsed = newMarksStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                                    updateCurrentQuestion(
                                        currentSections = currentSections,
                                        selectedQuestionIndex = selectedQuestionIndex,
                                        update = { it.copy(marks = parsed.coerceIn(1, 100)) },
                                        onUpdated = { currentSections = it }
                                    )
                                },
                                label = { Text("Marks") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (currentQuestion.marks > 1) {
                                                    updateCurrentQuestion(
                                                        currentSections = currentSections,
                                                        selectedQuestionIndex = selectedQuestionIndex,
                                                        update = { it.copy(marks = it.marks - 1) },
                                                        onUpdated = { currentSections = it }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease marks", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                updateCurrentQuestion(
                                                    currentSections = currentSections,
                                                    selectedQuestionIndex = selectedQuestionIndex,
                                                    update = { it.copy(marks = it.marks + 1) },
                                                    onUpdated = { currentSections = it }
                                                )
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase marks", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Calculated Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "$totalCalculatedMarks / ${structuredResult.totalMarks} Marks",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalCalculatedMarks == structuredResult.totalMarks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // MCQ Options Editing (if options are present or question type is MCQ)
                    if (currentQuestion.options.isNotEmpty() || currentQuestion.questionType.contains("Choice", ignoreCase = true)) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Multiple Choice Options",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            val nextChar = ('A'.code + currentQuestion.options.size).toChar()
                                            val newOptions = currentQuestion.options + "$nextChar. Option $nextChar"
                                            updateCurrentQuestion(
                                                currentSections = currentSections,
                                                selectedQuestionIndex = selectedQuestionIndex,
                                                update = { it.copy(options = newOptions) },
                                                onUpdated = { currentSections = it }
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Add Option", fontSize = 11.sp)
                                    }
                                }

                                currentQuestion.options.forEachIndexed { optIndex, optText ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = optText,
                                            onValueChange = { updatedOpt ->
                                                val mutableOpts = currentQuestion.options.toMutableList()
                                                mutableOpts[optIndex] = updatedOpt
                                                updateCurrentQuestion(
                                                    currentSections = currentSections,
                                                    selectedQuestionIndex = selectedQuestionIndex,
                                                    update = { it.copy(options = mutableOpts) },
                                                    onUpdated = { currentSections = it }
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            singleLine = true
                                        )

                                        if (currentQuestion.options.size > 2) {
                                            IconButton(
                                                onClick = {
                                                    val mutableOpts = currentQuestion.options.toMutableList()
                                                    mutableOpts.removeAt(optIndex)
                                                    updateCurrentQuestion(
                                                        currentSections = currentSections,
                                                        selectedQuestionIndex = selectedQuestionIndex,
                                                        update = { it.copy(options = mutableOpts) },
                                                        onUpdated = { currentSections = it }
                                                    )
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete option", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Correct Answer Key
                    item {
                        OutlinedTextField(
                            value = currentQuestion.correctAnswer,
                            onValueChange = { newAnswer ->
                                updateCurrentQuestion(
                                    currentSections = currentSections,
                                    selectedQuestionIndex = selectedQuestionIndex,
                                    update = { it.copy(correctAnswer = newAnswer) },
                                    onUpdated = { currentSections = it }
                                )
                            },
                            label = { Text("Correct Answer Key (Required)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Marking Guide / Rubric
                    item {
                        OutlinedTextField(
                            value = currentQuestion.markingGuide,
                            onValueChange = { newGuide ->
                                updateCurrentQuestion(
                                    currentSections = currentSections,
                                    selectedQuestionIndex = selectedQuestionIndex,
                                    update = { it.copy(markingGuide = newGuide) },
                                    onUpdated = { currentSections = it }
                                )
                            },
                            label = { Text("Teacher Rubric / Marking Guide") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Preserved Source Citation Banner (Read-only Traceability)
                    if (currentQuestion.sourceCitation != null) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(
                                            text = "Source Citation (Preserved)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${currentQuestion.sourceCitation.documentTitle} • ${currentQuestion.sourceCitation.chapterUnit} (${currentQuestion.sourceCitation.pageRange})",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reset this Question button
                    item {
                        val origQ = originalResult.sections.flatMap { it.questions }.getOrNull(selectedQuestionIndex)
                        if (origQ != null) {
                            TextButton(
                                onClick = {
                                    updateCurrentQuestion(
                                        currentSections = currentSections,
                                        selectedQuestionIndex = selectedQuestionIndex,
                                        update = { origQ },
                                        onUpdated = { currentSections = it }
                                    )
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Question to Original", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Live Validation Errors Display
            AnimatedVisibility(visible = validationErrors.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Text("Validation Issues Found", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        validationErrors.forEach { err ->
                            Text("• $err", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: [Cancel] [Undo All] [Apply]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                if (hasAnyEdits) {
                    OutlinedButton(
                        onClick = {
                            currentSections = originalResult.sections
                            validationErrors = emptyList()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo All", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        // 1. Run authoritative validation
                        val report = CurriculumQuestionValidator.validateEditedStructuredResult(
                            originalTotalMarks = structuredResult.totalMarks,
                            sections = currentSections
                        )

                        if (!report.isValid) {
                            validationErrors = report.errors
                            validationWarnings = report.warnings
                        } else {
                            validationErrors = emptyList()
                            // 2. Mark modified questions as isManuallyEdited = true
                            val origQuestions = originalResult.sections.flatMap { it.questions }
                            var qCounter = 0

                            val finalizedSections = currentSections.map { sec ->
                                val finalizedQuestions = sec.questions.map { q ->
                                    val origQ = origQuestions.getOrNull(qCounter++)
                                    val isModified = origQ == null || (
                                            q.questionText != origQ.questionText ||
                                                    q.marks != origQ.marks ||
                                                    q.correctAnswer != origQ.correctAnswer ||
                                                    q.markingGuide != origQ.markingGuide ||
                                                    q.options != origQ.options
                                            )
                                    if (isModified) {
                                        q.copy(isManuallyEdited = true)
                                    } else {
                                        q
                                    }
                                }
                                sec.copy(
                                    sectionMarks = finalizedQuestions.sumOf { it.marks },
                                    questions = finalizedQuestions
                                )
                            }

                            val newTotalMarks = finalizedSections.flatMap { it.questions }.sumOf { it.marks }
                            val updatedResult = structuredResult.copy(
                                totalMarks = newTotalMarks,
                                sections = finalizedSections,
                                validationSummary = "Curriculum Grounded (Teacher Edited) • ${finalizedSections.flatMap { it.questions }.size} Questions • Total $newTotalMarks Marks Validated"
                            )

                            onApply(updatedResult)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply Edits")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Helper to immutably update the selected question in a nested list of sections.
 */
private fun updateCurrentQuestion(
    currentSections: List<QuestionSectionUi>,
    selectedQuestionIndex: Int,
    update: (QuestionItemUi) -> QuestionItemUi,
    onUpdated: (List<QuestionSectionUi>) -> Unit
) {
    var globalIdx = 0
    val newSections = currentSections.map { section ->
        val newQuestions = section.questions.map { q ->
            if (globalIdx == selectedQuestionIndex) {
                globalIdx++
                update(q)
            } else {
                globalIdx++
                q
            }
        }
        section.copy(
            sectionMarks = newQuestions.sumOf { it.marks },
            questions = newQuestions
        )
    }
    onUpdated(newSections)
}
