package com.example.ui.screens.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceSession
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.UserEntity
import com.example.data.policy.SchoolPolicy
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel
import com.example.ui.viewmodel.StudentAttendanceUiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceManagementScreen(
    attendanceViewModel: AttendanceViewModel,
    currentUser: UserEntity?,
    availableAcademicYears: List<String> = emptyList(),
    policyViewModel: SchoolPolicyViewModel,
    modifier: Modifier = Modifier
) {
    val academicYear by attendanceViewModel.academicYear.collectAsState()
    val selectedDate by attendanceViewModel.selectedDate.collectAsState()
    val selectedSession by attendanceViewModel.selectedSession.collectAsState()
    val selectedGrade by attendanceViewModel.selectedGrade.collectAsState()
    val selectedClass by attendanceViewModel.selectedClass.collectAsState()
    val searchQuery by attendanceViewModel.searchQuery.collectAsState()

    val attendanceUiList by attendanceViewModel.attendanceUiList.collectAsState()
    val sessionStats by attendanceViewModel.sessionStats.collectAsState()
    val statusMessage by attendanceViewModel.statusMessage.collectAsState()
    val isSaving by attendanceViewModel.isSaving.collectAsState()

    // Screen-scoped Supabase Realtime for active live attendance collaboration
    DisposableEffect(Unit) {
        com.example.data.sync.SyncManager.subscribeScreenRealtime(
            screenKey = "attendance_screen",
            tableName = "attendance_records",
            schoolId = "default_school"
        )
        onDispose {
            com.example.data.sync.SyncManager.unsubscribeScreenRealtime("attendance_screen")
        }
    }

    val canEdit = attendanceViewModel.canEditAttendance(currentUser)
    val focusManager = LocalFocusManager.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }

    LaunchedEffect(selectedGrade, selectedClass, selectedDate, selectedSession, academicYear) {
        isLocked = false
    }

    val dbGrades by policyViewModel.grades.collectAsState()
    val dbClasses by policyViewModel.classes.collectAsState()

    val academicYears = if (availableAcademicYears.isNotEmpty()) (availableAcademicYears + academicYear).filter { it.isNotBlank() }.distinct() else listOf(academicYear).filter { it.isNotBlank() }
    val sessions = listOf("Morning", "Evening")
    val grades = if (dbGrades.isNotEmpty()) dbGrades.map { it.gradeName } else SchoolPolicy.VALID_GRADES
    val classes = if (dbClasses.isNotEmpty()) dbClasses.map { it.className }.distinct() else listOf("A", "B", "C", "D")

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
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // SUMMARY: Present : 28 | Absent : 2 | Late : 1 | Leave : 1
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Present : ${sessionStats.presentCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Absent : ${sessionStats.absentCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "Late : ${sessionStats.lateCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                            Text(
                                text = "Leave : ${sessionStats.leaveCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF65558F)
                            )
                        }
                    }

                    // BOTTOM BUTTONS: Save, Submit, Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel / Edit: Return to editable state
                        OutlinedButton(
                            onClick = {
                                isLocked = false
                                attendanceViewModel.setSearchQuery("")
                                focusManager.clearFocus()
                                snackbarMessage = "Attendance returned to editable state."
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(if (isLocked) "Edit" else "Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        // Save: Save entered attendance and lock from further editing
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                attendanceViewModel.saveAttendance(currentUser?.fullName ?: "Teacher")
                                isLocked = true
                                snackbarMessage = "Attendance saved and locked from further editing."
                            },
                            enabled = !isSaving && canEdit && !isLocked,
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

                        // Submit: Display/confirm submitted data
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                attendanceViewModel.saveAttendance(currentUser?.fullName ?: "Teacher")
                                isLocked = true
                                snackbarMessage = "Attendance confirmed & submitted."
                            },
                            enabled = !isSaving && canEdit,
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
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Message Banner
            statusMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { attendanceViewModel.clearStatusMessage() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

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

            // TOP SECTION: Dropdowns (Academic Year, Date, Session, Grade, Class)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactDropdown(
                    label = "Academic Year",
                    value = academicYear,
                    items = academicYears,
                    onItemSelected = { attendanceViewModel.setAcademicYear(it) },
                    modifier = Modifier.weight(1f)
                )

                CompactDateField(
                    label = "Date",
                    value = selectedDate,
                    onValueChange = { attendanceViewModel.setSelectedDate(it) },
                    modifier = Modifier.weight(1f)
                )

                CompactDropdown(
                    label = "Session",
                    value = if (selectedSession == AttendanceSession.MORNING) "Morning" else "Evening",
                    items = sessions,
                    onItemSelected = { sessName ->
                        val sess = if (sessName == "Morning") AttendanceSession.MORNING else AttendanceSession.EVENING
                        attendanceViewModel.setSelectedSession(sess)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactDropdown(
                    label = "Grade",
                    value = selectedGrade,
                    items = grades,
                    onItemSelected = { attendanceViewModel.setSelectedGrade(it) },
                    modifier = Modifier.weight(1f)
                )

                CompactDropdown(
                    label = "Class",
                    value = selectedClass,
                    items = classes,
                    onItemSelected = { attendanceViewModel.setSelectedClass(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { attendanceViewModel.setSearchQuery(it) },
                placeholder = { Text("Search by Student Name or Student ID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { attendanceViewModel.setSearchQuery("") }) {
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

            // QUICK ACTIONS: Mark All Present, Mark All Absent, Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { attendanceViewModel.markAllPresent() },
                    enabled = canEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = { attendanceViewModel.markAllAbsent() },
                    enabled = canEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { attendanceViewModel.resetAttendance() },
                    enabled = canEdit,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            // ATTENDANCE LIST TABLE
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
                            Text("Student ID", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.9f))
                            Text("Student Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                            Text("Attendance Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(3.4f))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Student Rows
                    if (attendanceUiList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No students match '$searchQuery'" else "No students in Grade $selectedGrade Class $selectedClass",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(attendanceUiList, key = { it.student.id }) { item ->
                                StudentAttendanceRow(
                                    uiItem = item,
                                    canEdit = canEdit && !isLocked,
                                    onStatusChanged = { newStatus ->
                                        attendanceViewModel.updateStudentStatus(item.student.id, newStatus)
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
private fun CompactDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
        Surface(
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StudentAttendanceRow(
    uiItem: StudentAttendanceUiItem,
    canEdit: Boolean,
    onStatusChanged: (AttendanceStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Student ID
        val idDisplay = if (uiItem.student.studentCode.isNotBlank()) uiItem.student.studentCode else String.format("%03d", uiItem.student.rollNumber)
        Text(
            text = idDisplay,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f)
        )

        // Student Name
        Text(
            text = uiItem.student.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Attendance Status Options (Present, Absent, Late, Leave)
        Row(
            modifier = Modifier.weight(3.4f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusOptionItem(
                label = "Present",
                isSelected = uiItem.currentStatus == AttendanceStatus.PRESENT,
                activeColor = Color(0xFF15803D),
                enabled = canEdit,
                onClick = { onStatusChanged(AttendanceStatus.PRESENT) },
                modifier = Modifier.weight(1f)
            )

            StatusOptionItem(
                label = "Absent",
                isSelected = uiItem.currentStatus == AttendanceStatus.ABSENT,
                activeColor = Color(0xFFB91C1C),
                enabled = canEdit,
                onClick = { onStatusChanged(AttendanceStatus.ABSENT) },
                modifier = Modifier.weight(1f)
            )

            StatusOptionItem(
                label = "Late",
                isSelected = uiItem.currentStatus == AttendanceStatus.LATE,
                activeColor = Color(0xFFD97706),
                enabled = canEdit,
                onClick = { onStatusChanged(AttendanceStatus.LATE) },
                modifier = Modifier.weight(1f)
            )

            StatusOptionItem(
                label = "Leave",
                isSelected = uiItem.currentStatus == AttendanceStatus.LEAVE,
                activeColor = Color(0xFF65558F),
                enabled = canEdit,
                onClick = { onStatusChanged(AttendanceStatus.LEAVE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusOptionItem(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(28.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSelected) "●" else "○",
                fontSize = 9.sp,
                color = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.padding(end = 2.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
