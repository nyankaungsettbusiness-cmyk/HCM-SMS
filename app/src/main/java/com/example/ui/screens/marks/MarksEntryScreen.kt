package com.example.ui.screens.marks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.data.policy.SchoolPolicy
import com.example.ui.viewmodel.EditableMarkRow
import com.example.ui.viewmodel.MarksViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksEntryScreen(
    marksViewModel: MarksViewModel,
    currentUser: UserEntity?,
    availableAcademicYears: List<String> = emptyList(),
    policyViewModel: SchoolPolicyViewModel
) {
    val academicYear by marksViewModel.academicYear.collectAsState()
    val selectedGrade by marksViewModel.selectedGrade.collectAsState()
    val selectedClass by marksViewModel.selectedClass.collectAsState()
    val selectedStream by marksViewModel.selectedStream.collectAsState()
    val selectedSubject by marksViewModel.selectedSubject.collectAsState()
    val selectedAssessment by marksViewModel.selectedAssessment.collectAsState()

    val availableAssessments by marksViewModel.availableAssessments.collectAsState()
    val displayedRows by marksViewModel.displayedRows.collectAsState()
    val searchQuery by marksViewModel.searchQuery.collectAsState()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(academicYear, selectedGrade, selectedClass, selectedStream, selectedSubject, selectedAssessment) {
        isLocked = false
    }

    val availableSubjects by marksViewModel.availableSubjectsForGrade.collectAsState()

    val dbGrades by policyViewModel.grades.collectAsState()
    val dbClasses by policyViewModel.classes.collectAsState()

    // Options for dropdowns
    val academicYears = if (availableAcademicYears.isNotEmpty()) (availableAcademicYears + academicYear).filter { it.isNotBlank() }.distinct() else listOf(academicYear).filter { it.isNotBlank() }
    val grades = if (dbGrades.isNotEmpty()) dbGrades.map { it.gradeName } else SchoolPolicy.VALID_GRADES
    val streams = SchoolPolicy.VALID_STREAMS
    val classes = listOf("All Classes") + (if (dbClasses.isNotEmpty()) dbClasses.map { "Class ${it.className}" }.distinct() else listOf("Class A", "Class B", "Class C"))
    val subjects = if (availableSubjects.isNotEmpty()) availableSubjects else SchoolPolicy.getDefaultSubjectNamesForGrade(selectedGrade, selectedStream)

    // Auto select first available assessment if none selected
    LaunchedEffect(availableAssessments) {
        if (selectedAssessment == null && availableAssessments.isNotEmpty()) {
            val firstAsm = availableAssessments.first()
            marksViewModel.selectedAssessment.value = firstAsm
            if (firstAsm.subjectName.isNotBlank()) {
                marksViewModel.selectedSubject.value = firstAsm.subjectName
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(msg)
                }
            }
        },
        bottomBar = {
            // BOTTOM BUTTONS: Save, Submit, Cancel
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel / Edit Button
                    OutlinedButton(
                        onClick = {
                            if (isLocked) {
                                isLocked = false
                                marksViewModel.searchQuery.value = ""
                                focusManager.clearFocus()
                                snackbarMessage = "Marks returned to editable state."
                            } else {
                                marksViewModel.searchQuery.value = ""
                                focusManager.clearFocus()
                                snackbarMessage = "Marks entry reset."
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 38.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.Edit else Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isLocked) "Edit" else "Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            marksViewModel.saveAllMarks(currentUser?.fullName ?: "Teacher") { success, msg ->
                                if (success) {
                                    isLocked = true
                                    snackbarMessage = "Marks saved and locked from further editing."
                                } else {
                                    snackbarMessage = msg
                                }
                            }
                        },
                        enabled = !isLocked,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            marksViewModel.saveAllMarks(currentUser?.fullName ?: "Teacher") { success, msg ->
                                if (success) {
                                    isLocked = true
                                    snackbarMessage = "Marks confirmed & submitted."
                                } else {
                                    snackbarMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Lock Status Banner
            if (isLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Record saved and locked. Click 'Cancel / Edit' to modify again.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            // TOP SECTION: Compact Dropdowns
            // Row 1: Academic Year, Grade, Class
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactDropdown(
                    label = "Academic Year",
                    value = academicYear,
                    items = academicYears,
                    onItemSelected = { marksViewModel.academicYear.value = it },
                    modifier = Modifier.weight(1f)
                )
                CompactDropdown(
                    label = "Grade",
                    value = selectedGrade,
                    items = grades,
                    onItemSelected = { g ->
                        marksViewModel.selectedGrade.value = g
                        marksViewModel.selectedAssessment.value = null
                    },
                    modifier = Modifier.weight(1f)
                )
                if (SchoolPolicy.isHighSchool(selectedGrade)) {
                    CompactDropdown(
                        label = "Stream",
                        value = selectedStream,
                        items = streams,
                        onItemSelected = { st -> marksViewModel.selectedStream.value = st },
                        modifier = Modifier.weight(1f)
                    )
                }
                CompactDropdown(
                    label = "Class",
                    value = selectedClass,
                    items = classes,
                    onItemSelected = { c -> marksViewModel.selectedClass.value = c },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Subject, Assessment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactDropdown(
                    label = "Subject",
                    value = selectedSubject,
                    items = subjects,
                    onItemSelected = { s -> marksViewModel.selectedSubject.value = s },
                    modifier = Modifier.weight(1f)
                )

                val assessmentNames = if (availableAssessments.isEmpty()) {
                    listOf("No Assessments")
                } else {
                    availableAssessments.map { it.assessmentName }
                }

                CompactDropdown(
                    label = "Assessment",
                    value = selectedAssessment?.assessmentName ?: "Select Assessment",
                    items = assessmentNames,
                    onItemSelected = { asmName ->
                        val asm = availableAssessments.find { it.assessmentName == asmName }
                        if (asm != null) {
                            marksViewModel.selectedAssessment.value = asm
                            if (asm.subjectName.isNotBlank()) {
                                marksViewModel.selectedSubject.value = asm.subjectName
                            }
                        }
                    },
                    modifier = Modifier.weight(1.5f)
                )
            }

            // SEARCH: Single search bar above student list
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { marksViewModel.searchQuery.value = it },
                placeholder = { Text("Search by Student Name or Student ID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { marksViewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            // STUDENT LIST TABLE
            Surface(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Row
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Student ID", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.0f))
                            Text("Student Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2.0f))
                            Text("Mark", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.6f))
                            Text("Result (Auto)", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Rows
                    if (displayedRows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No students match '$searchQuery'" else "No students found",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedRows, key = { it.studentId }) { row ->
                                StudentMarkRowItem(
                                    row = row,
                                    isLocked = isLocked,
                                    onMarkChange = { input ->
                                        marksViewModel.updateObtainedMark(row.studentId, input)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDropdown(
    label: String,
    value: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value.ifBlank { "Select" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, fontSize = 12.sp) },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentMarkRowItem(
    row: EditableMarkRow,
    isLocked: Boolean,
    onMarkChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Student ID (Code or Roll formatted)
        val studentIdDisplay = if (row.studentCode.isNotBlank()) row.studentCode else String.format("%03d", row.rollNo)
        Text(
            text = studentIdDisplay,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.0f)
        )

        // Student Name
        Text(
            text = row.studentName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(2.0f)
        )

        // Mark (Editable Number Field - Increased Width & Unclipped)
        Box(
            modifier = Modifier
                .weight(1.6f)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = row.obtainedText,
                onValueChange = onMarkChange,
                enabled = !isLocked,
                readOnly = isLocked,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .border(
                                width = if (row.errorMessage != null) 1.5.dp else 1.dp,
                                color = if (row.errorMessage != null) {
                                    MaterialTheme.colorScheme.error
                                } else if (isLocked) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(
                                color = if (row.errorMessage != null) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                } else if (isLocked) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (row.obtainedText.isEmpty()) {
                            Text(
                                text = "0",
                                fontSize = 14.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Result (Auto: Distinction / Pass / Fail)
        Box(
            modifier = Modifier.weight(1.4f),
            contentAlignment = Alignment.Center
        ) {
            if (row.obtainedMarks == null || row.obtainedText.isBlank()) {
                Text(
                    text = "-",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else if (row.isDistinction) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF8E1),
                    border = BorderStroke(1.dp, Color(0xFFFFB300))
                ) {
                    Text(
                        text = "Distinction",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (row.isPassed) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xE8E8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF81C784))
                ) {
                    Text(
                        text = "Pass",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(1.dp, Color(0xFFE57373))
                ) {
                    Text(
                        text = "Fail",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

