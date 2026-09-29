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
import androidx.compose.material.icons.filled.*
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
import com.example.data.local.entity.ExamQuestionBlueprintEntity
import com.example.data.local.entity.QuestionBlueprintItem
import com.example.data.local.entity.UserEntity
import com.example.data.policy.ExamQuestionBlueprintHelper
import com.example.data.policy.SchoolPolicy
import com.example.ui.viewmodel.AssessmentQuestionAnalytics
import com.example.ui.viewmodel.EditableMarkRow
import com.example.ui.viewmodel.MarksViewModel
import com.example.ui.viewmodel.QuestionAnalysisStat
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

    val currentBlueprint by marksViewModel.currentBlueprint.collectAsState()
    val questionAnalytics by marksViewModel.questionAnalytics.collectAsState()
    var activeStudentForBreakdown by remember { mutableStateOf<EditableMarkRow?>(null) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }

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

            // EXAM BLUEPRINT BANNER & QUESTION ANALYSIS BUTTON
            if (currentBlueprint != null) {
                val questions = remember(currentBlueprint?.questionsJson) {
                    ExamQuestionBlueprintHelper.parseQuestions(currentBlueprint?.questionsJson)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                Icons.Default.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "မေးခွန်းပုံစံ: ${currentBlueprint?.gradeName} ${currentBlueprint?.subjectName} (${questions.size} ပုဒ် - ${currentBlueprint?.totalMarks} မှတ်)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { showAnalyticsDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("မေးခွန်းအလိုက် စိစစ်ချက် (Analysis)", fontSize = 11.sp)
                        }
                    }
                }
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
                            if (currentBlueprint != null) {
                                Text("Student ID", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.9f))
                                Text("Student Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.8f))
                                Text("Mark", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f))
                                Text("Result (Auto)", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.2f))
                                Text("Q-Marks", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            } else {
                                Text("Student ID", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.0f))
                                Text("Student Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2.0f))
                                Text("Mark", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.6f))
                                Text("Result (Auto)", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f))
                            }
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
                                    hasBlueprint = currentBlueprint != null,
                                    onMarkChange = { input ->
                                        marksViewModel.updateObtainedMark(row.studentId, input)
                                    },
                                    onOpenBreakdown = {
                                        activeStudentForBreakdown = row
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

    // Question Breakdown Dialog
    activeStudentForBreakdown?.let { student ->
        currentBlueprint?.let { bp ->
            QuestionBreakdownEntryDialog(
                student = student,
                blueprint = bp,
                allStudents = displayedRows,
                isLocked = isLocked,
                onSave = { marksMap ->
                    marksViewModel.updateQuestionMarks(student.studentId, marksMap)
                    activeStudentForBreakdown = null
                    snackbarMessage = "${student.studentName} ၏ မေးခွန်းအလိုက် အမှတ်များ ထည့်သွင်းပြီးပါပြီ"
                },
                onNavigateStudent = { nextStudent ->
                    activeStudentForBreakdown = nextStudent
                },
                onDismiss = { activeStudentForBreakdown = null }
            )
        }
    }

    // Question Weakness Analytics Dialog
    if (showAnalyticsDialog) {
        currentBlueprint?.let { bp ->
            QuestionAnalyticsDialog(
                blueprint = bp,
                analytics = questionAnalytics,
                onDismiss = { showAnalyticsDialog = false }
            )
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
    hasBlueprint: Boolean = false,
    onMarkChange: (String) -> Unit,
    onOpenBreakdown: () -> Unit = {}
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
            modifier = Modifier.weight(if (hasBlueprint) 0.9f else 1.0f)
        )

        // Student Name
        Text(
            text = row.studentName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(if (hasBlueprint) 1.8f else 2.0f)
        )

        // Mark (Editable Number Field)
        Box(
            modifier = Modifier
                .weight(if (hasBlueprint) 1.4f else 1.6f)
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
            modifier = Modifier.weight(if (hasBlueprint) 1.2f else 1.4f),
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

        // Q-Breakdown Button
        if (hasBlueprint) {
            Box(
                modifier = Modifier.weight(0.8f),
                contentAlignment = Alignment.Center
            ) {
                val hasBreakdown = row.questionMarks.isNotEmpty()
                IconButton(
                    onClick = onOpenBreakdown,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (hasBreakdown) Icons.Default.FactCheck else Icons.Default.FormatListNumbered,
                        contentDescription = "Question breakdown",
                        tint = if (hasBreakdown) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionBreakdownEntryDialog(
    student: EditableMarkRow,
    blueprint: ExamQuestionBlueprintEntity,
    allStudents: List<EditableMarkRow>,
    isLocked: Boolean,
    onSave: (Map<String, Double>) -> Unit,
    onNavigateStudent: (EditableMarkRow) -> Unit,
    onDismiss: () -> Unit
) {
    val questions = remember(blueprint.questionsJson) {
        val parsed = ExamQuestionBlueprintHelper.parseQuestions(blueprint.questionsJson)
        if (parsed.isNotEmpty()) parsed else ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(blueprint.subjectName)
    }

    // Keep track of marks entered per question number
    val initialMarks = remember(student.studentId, student.questionMarksJson) {
        questions.associate { q ->
            val existing = student.questionMarks[q.qNo]
            val text = existing?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format(java.util.Locale.US, "%.1f", it)
            } ?: ""
            q.qNo to text
        }.toMutableMap()
    }

    var markInputs by remember(student.studentId) { mutableStateOf(initialMarks) }

    // Calculate current total
    val parsedScores = markInputs.mapNotNull { (qNo, text) ->
        val num = text.toDoubleOrNull()
        if (num != null) qNo to num else null
    }.toMap()

    val totalScore = parsedScores.values.sum()
    val isPassed = totalScore >= student.passMark
    val isDistinction = totalScore >= student.distinctionMark

    // Navigation indexes
    val currentIndex = allStudents.indexOfFirst { it.studentId == student.studentId }
    val hasPrev = currentIndex > 0
    val hasNext = currentIndex != -1 && currentIndex < allStudents.size - 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${student.studentName} (${if (student.studentCode.isNotBlank()) student.studentCode else "Roll ${student.rollNo}"})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${blueprint.gradeName} - ${blueprint.subjectName} မေးခွန်းအလိုက် အမှတ်ထည့်သွင်းခြင်း",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Score Summary Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDistinction) Color(0xFFFFF8E1) else if (isPassed) Color(0xE8E8F5E9) else Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, if (isDistinction) Color(0xFFFFB300) else if (isPassed) Color(0xFF81C784) else Color(0xFFE57373))
                    ) {
                        Text(
                            text = "စုစုပေါင်း: ${if (totalScore % 1.0 == 0.0) totalScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", totalScore)} / ${blueprint.totalMarks}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDistinction) Color(0xFFE65100) else if (isPassed) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(questions.size) { idx ->
                        val q = questions[idx]
                        val currentText = markInputs[q.qNo] ?: ""
                        val num = currentText.toDoubleOrNull()
                        val hasError = currentText.isNotBlank() && (num == null || num < 0 || num > q.maxMark)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = q.qNo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = q.title.ifBlank { "မေးခွန်း ${q.qNo}" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Max Mark: ${q.maxMark.toInt()} မှတ်",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    if (hasError) {
                                        Text(
                                            text = "0 မှ ${q.maxMark.toInt()} ကြားသာ ထည့်ပါ",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = currentText,
                                    onValueChange = { newVal ->
                                        if (!isLocked) {
                                            markInputs = markInputs.toMutableMap().also { it[q.qNo] = newVal }
                                        }
                                    },
                                    enabled = !isLocked,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(70.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }

                // Student Navigation Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (hasPrev) {
                                onSave(parsedScores)
                                onNavigateStudent(allStudents[currentIndex - 1])
                            }
                        },
                        enabled = hasPrev,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ရှေ့ကျောင်းသား", fontSize = 11.sp)
                    }

                    Text(
                        text = "${currentIndex + 1} / ${allStudents.size}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    TextButton(
                        onClick = {
                            if (hasNext) {
                                onSave(parsedScores)
                                onNavigateStudent(allStudents[currentIndex + 1])
                            }
                        },
                        enabled = hasNext,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("နောက်ကျောင်းသား", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(14.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(parsedScores) },
                enabled = !isLocked
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ထည့်သွင်းသိမ်းဆည်းမည် (Save & Apply)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@Composable
private fun QuestionAnalyticsDialog(
    blueprint: ExamQuestionBlueprintEntity,
    analytics: AssessmentQuestionAnalytics?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "မေးခွန်းအလိုက် အားနည်းချက် ခွဲခြမ်းစိတ်ဖြာချက် (Question Analytics)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${blueprint.gradeName} - ${blueprint.subjectName} (${analytics?.totalEvaluatedStudents ?: 0} ဦး အမှတ်စိစစ်ပြီး)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            if (analytics == null || analytics.questions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "မေးခွန်းအလိုက် အမှတ်ထည့်သွင်းထားသော ကျောင်းသား မရှိသေးပါ。\nကျောင်းသားများ၏ Q-Marks ခလုတ်ကို နှိပ်၍ အမှတ်များ ထည့်သွင်းပေးပါရန်။",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Highlights: Weakest & Strongest
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Weakest Section
                        analytics.weakestQuestion?.let { w ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                        Text("အားအနည်းဆုံး အပိုင်း", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${w.qNo} - ${w.title}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ပျမ်းမျှ: ${String.format(java.util.Locale.US, "%.1f", w.averageScore)} / ${w.maxMark.toInt()} (${String.format(java.util.Locale.US, "%.1f", w.percentage)}%)",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB71C1C),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Strongest Section
                        analytics.strongestQuestion?.let { s ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xE8E8F5E9)),
                                border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        Text("အားအကောင်းဆုံး အပိုင်း", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${s.qNo} - ${s.title}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ပျမ်းမျှ: ${String.format(java.util.Locale.US, "%.1f", s.averageScore)} / ${s.maxMark.toInt()} (${String.format(java.util.Locale.US, "%.1f", s.percentage)}%)",
                                        fontSize = 10.sp,
                                        color = Color(0xFF1B5E20),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // List of questions with progress bar and diagnosis
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(analytics.questions.size) { idx ->
                            val qStat = analytics.questions[idx]
                            val pct = qStat.percentage
                            val barColor = if (pct >= 75.0) Color(0xFF2E7D32) else if (pct >= 40.0) Color(0xFFF57C00) else Color(0xFFD32F2F)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = qStat.qNo,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = qStat.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", qStat.averageScore)} / ${qStat.maxMark.toInt()} (${String.format(java.util.Locale.US, "%.1f", pct)}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }

                                    // Progress Bar
                                    LinearProgressIndicator(
                                        progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )

                                    // Insight / Advice
                                    val diagnosisText = when {
                                        pct < 40.0 -> "⚠️ အားနည်းချက်ကြီးမားပါသည် - ကျောင်းသားအများစု အမှတ်နည်းနေသဖြင့် ထပ်မံလေ့ကျင့်သင်ကြားပေးရန် လိုအပ်သည်"
                                        pct < 60.0 -> "အလယ်အလတ်အဆင့် - လေ့ကျင့်ခန်းများ ထပ်ဆောင်းပေးသင့်သည်"
                                        pct >= 75.0 -> "🌟 အထူးကောင်းမွန်ပါသည် - သင်ရိုးနားလည်မှု အားကောင်းသည်"
                                        else -> "ကျေနပ်ဖွယ်အခြေအနေရှိသည်"
                                    }
                                    Text(
                                        text = diagnosisText,
                                        fontSize = 10.sp,
                                        color = if (pct < 40.0) Color(0xFFC62828) else Color.Gray,
                                        fontWeight = if (pct < 40.0) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("နားလည်ပါပြီ (OK)") }
        }
    )
}


