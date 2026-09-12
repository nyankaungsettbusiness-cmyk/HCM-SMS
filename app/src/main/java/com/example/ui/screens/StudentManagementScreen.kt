package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.policy.SchoolPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    students: List<StudentEntity>,
    grades: List<GradeEntity>,
    searchQuery: String,
    selectedGradeFilter: String,
    selectedClassFilter: String,
    selectedStatusFilter: String,
    onSearchQueryChanged: (String) -> Unit,
    onGradeFilterChanged: (String) -> Unit,
    onClassFilterChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onSaveStudent: (StudentEntity) -> Unit,
    onDeleteStudent: (Long) -> Unit,
    onImportMockData: () -> Unit,
    academicYear: String = "2026-2027",
    currentUser: com.example.data.local.entity.UserEntity? = null,
    deletionVerificationEvent: kotlinx.coroutines.flow.SharedFlow<com.example.data.sync.DeletionVerificationResult>? = null
) {
    val canDeleteStudent = currentUser?.role == com.example.data.local.entity.UserRole.SUPER_ADMIN ||
            currentUser?.role == com.example.data.local.entity.UserRole.ADMIN
    val snackbarHostState = remember { SnackbarHostState() }
    var latestVerificationResult by remember { mutableStateOf<com.example.data.sync.DeletionVerificationResult?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToViewDetail by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deletionVerificationEvent) {
        deletionVerificationEvent?.collect { result ->
            latestVerificationResult = result
            val msg = when (result) {
                is com.example.data.sync.DeletionVerificationResult.VerifiedPurged ->
                    "✅ Supabase Verified: ${result.message}"
                is com.example.data.sync.DeletionVerificationResult.VerifiedTombstoned ->
                    "✅ Supabase Verified: ${result.message}"
                is com.example.data.sync.DeletionVerificationResult.ServerRejectedOrIgnored ->
                    "⚠️ Supabase Server Rejected/Ignored: ${result.reason}"
                is com.example.data.sync.DeletionVerificationResult.OfflineQueued ->
                    "📶 Saved Locally: Queued for Supabase sync when online."
            }
            snackbarHostState.showSnackbar(msg, withDismissAction = true)
        }
    }

    val gradeNames = listOf("All") + grades.map { it.gradeName }
    val classNames = listOf("All", "A", "B", "C", "D")
    val statusNames = listOf("All", "Active", "Inactive", "Transferred")

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    studentToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Student", modifier = Modifier.size(22.dp))
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
            // 1. Compact Header & Actions (Responsive Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Students",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${students.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "• $academicYear",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val localContext = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            android.widget.Toast.makeText(localContext, "Smart Sync: Syncing on demand...", android.widget.Toast.LENGTH_SHORT).show()
                            com.example.data.sync.SyncManager.triggerSyncAsync(localContext, forceImmediate = true)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onImportMockData,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Import Mock Data",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Export Records",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            studentToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add Student",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Full-Width Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search ID, Name, Parent, Roll...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactFilterDropdownChip(
                    label = "Grade: $selectedGradeFilter",
                    options = gradeNames,
                    selectedOption = selectedGradeFilter,
                    onOptionSelected = onGradeFilterChanged
                )
                CompactFilterDropdownChip(
                    label = "Class: $selectedClassFilter",
                    options = classNames,
                    selectedOption = selectedClassFilter,
                    onOptionSelected = onClassFilterChanged
                )
                CompactFilterDropdownChip(
                    label = "Status: $selectedStatusFilter",
                    options = statusNames,
                    selectedOption = selectedStatusFilter,
                    onOptionSelected = onStatusFilterChanged
                )
                if (selectedGradeFilter != "All" || selectedClassFilter != "All" || selectedStatusFilter != "All" || searchQuery.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onSearchQueryChanged("")
                            onGradeFilterChanged("All")
                            onClassFilterChanged("All")
                            onStatusFilterChanged("All")
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }
            }

            // 3. Dynamic Mobile Student List
            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No students found matching filters.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(students, key = { _, s -> s.id }) { _, student ->
                        ModernStudentCard(
                            student = student,
                            canDelete = canDeleteStudent,
                            onViewDetail = { studentToViewDetail = student },
                            onEdit = {
                                studentToEdit = student
                                showAddEditDialog = true
                            },
                            onDelete = {
                                if (canDeleteStudent) {
                                    studentToDelete = student
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Student Dialog
    if (showAddEditDialog) {
        AddEditStudentDialog(
            student = studentToEdit,
            grades = gradeNames.filter { it != "All" },
            classes = listOf("A", "B", "C", "D"),
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedStudent ->
                onSaveStudent(updatedStudent)
                showAddEditDialog = false
            }
        )
    }

    // View Student Profile Detail Dialog
    studentToViewDetail?.let { student ->
        StudentDetailDialog(
            student = student,
            academicYear = academicYear,
            canDelete = canDeleteStudent,
            onEdit = {
                studentToEdit = student
                studentToViewDetail = null
                showAddEditDialog = true
            },
            onDelete = {
                if (canDeleteStudent) {
                    studentToDelete = student
                    studentToViewDetail = null
                }
            },
            onDismiss = { studentToViewDetail = null }
        )
    }

    // Delete Student Confirmation Dialog
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student Record?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${student.name} (${student.studentCode})? This action cannot be undone.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStudent(student.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentToDelete = null }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Student Records", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exporting ${students.size} student records in HCM-SMS format.", fontSize = 13.sp)
                    Text("• Student Directory Register (PDF)", fontSize = 12.sp, color = Color.Gray)
                    Text("• Student Master Sheet (CSV/Excel)", fontSize = 12.sp, color = Color.Gray)
                    Text("• Class & Roll Number Roster", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) {
                    Text("Download PDF/CSV", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun ModernStudentCard(
    student: StudentEntity,
    canDelete: Boolean = true,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Student Code, Grade/Class badge, Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = student.studentCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    val gradeDisplay = if (SchoolPolicy.isHighSchool(student.gradeName) && student.stream.isNotBlank()) {
                        "${student.gradeName}-${student.className} [${student.stream}]"
                    } else {
                        "${student.gradeName} - ${student.className}"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = gradeDisplay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status Badge
                val (bgColor, textColor) = when (student.status) {
                    "Active" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    "Transferred" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                    else -> Color(0xFFEEEEEE) to Color(0xFF616161)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor
                ) {
                    Text(
                        text = student.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Row 2: Avatar + Name + Roll/Parent + Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Roll #${student.rollNumber} • Parent: ${student.parentName.ifBlank { "N/A" }}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Actions (Touch friendly min 40dp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Student",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Student",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactFilterDropdownChip(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(label, fontSize = 10.sp, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 12.sp) },
                    onClick = {
                        onOptionSelected(opt)
                        expanded = false
                    },
                    leadingIcon = {
                        if (opt == selectedOption) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentDialog(
    student: StudentEntity?,
    grades: List<String>,
    classes: List<String>,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    var studentCode by remember { mutableStateOf(student?.studentCode ?: "HCM-2025-${(100..999).random()}") }
    var name by remember { mutableStateOf(student?.name ?: "") }
    var gender by remember { mutableStateOf(student?.gender ?: "Male") }
    var dob by remember { mutableStateOf(student?.dateOfBirth ?: "2015-01-01") }
    var gradeName by remember { mutableStateOf(student?.gradeName ?: "G5") }
    var stream by remember { mutableStateOf(student?.stream ?: if (SchoolPolicy.isHighSchool(gradeName)) "STEAMS-1" else "") }
    var className by remember { mutableStateOf(student?.className ?: "A") }
    var rollNumber by remember { mutableStateOf(student?.rollNumber?.toString() ?: "1") }
    var parentName by remember { mutableStateOf(student?.parentName ?: "") }
    var phone by remember { mutableStateOf(student?.phone ?: "") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var status by remember { mutableStateOf(student?.status ?: "Active") }

    var gradeExpanded by remember { mutableStateOf(false) }
    var streamExpanded by remember { mutableStateOf(false) }

    val isHighSchool = SchoolPolicy.isHighSchool(gradeName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "Add New Student" else "Edit Student Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = studentCode,
                        onValueChange = { studentCode = it },
                        label = { Text("Student Code / ID", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gender:", fontSize = 12.sp)
                        FilterChip(
                            selected = gender == "Male",
                            onClick = { gender = "Male" },
                            label = { Text("Male", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = gender == "Female",
                            onClick = { gender = "Female" },
                            label = { Text("Female", fontSize = 11.sp) }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = gradeExpanded,
                            onExpandedChange = { gradeExpanded = !gradeExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = gradeName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Grade *", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                            )
                            ExposedDropdownMenu(
                                expanded = gradeExpanded,
                                onDismissRequest = { gradeExpanded = false }
                            ) {
                                val displayGrades = if (grades.isNotEmpty()) grades else SchoolPolicy.VALID_GRADES
                                displayGrades.forEach { gOpt ->
                                    DropdownMenuItem(
                                        text = { Text(gOpt, fontSize = 12.sp) },
                                        onClick = {
                                            gradeName = gOpt
                                            gradeExpanded = false
                                            if (SchoolPolicy.isHighSchool(gOpt)) {
                                                if (stream.isBlank()) stream = "STEAMS-1"
                                            } else {
                                                stream = ""
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = className,
                            onValueChange = { className = it },
                            label = { Text("Class", fontSize = 12.sp) },
                            modifier = Modifier.weight(0.9f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )

                        OutlinedTextField(
                            value = rollNumber,
                            onValueChange = { rollNumber = it },
                            label = { Text("Roll #", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }
                }

                if (isHighSchool) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = streamExpanded,
                            onExpandedChange = { streamExpanded = !streamExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (stream.isNotBlank()) stream else "STEAMS-1",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Stream * (G10-G12)", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = streamExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                            )
                            ExposedDropdownMenu(
                                expanded = streamExpanded,
                                onDismissRequest = { streamExpanded = false }
                            ) {
                                SchoolPolicy.VALID_STREAMS.forEach { stOpt ->
                                    DropdownMenuItem(
                                        text = { Text(stOpt, fontSize = 12.sp) },
                                        onClick = {
                                            stream = stOpt
                                            streamExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent / Guardian Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone Number", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            StudentEntity(
                                id = student?.id ?: 0L,
                                studentCode = studentCode,
                                name = name,
                                gender = gender,
                                dateOfBirth = dob,
                                gradeName = gradeName,
                                className = className,
                                rollNumber = rollNumber.toIntOrNull() ?: 1,
                                parentName = parentName,
                                phone = phone,
                                address = address,
                                status = status,
                                stream = if (SchoolPolicy.isHighSchool(gradeName)) (if (stream.isNotBlank()) stream else "STEAMS-1") else ""
                            )
                        )
                    }
                }
            ) {
                Text("Save", fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailDialog(
    student: StudentEntity,
    academicYear: String,
    canDelete: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Profile", "Academic", "Attendance", "HCM Holistic", "Reports")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (student.gender == "Male") MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (student.gender == "Male") Icons.Default.Face else Icons.Default.Face3,
                            contentDescription = null,
                            tint = if (student.gender == "Male") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${student.studentCode} • Roll #${student.rollNumber}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
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
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> ProfileAndParentTab(student)
                    1 -> AcademicDataTab(student, academicYear)
                    2 -> AttendanceDataTab(student)
                    3 -> HcmHolisticTab(student)
                    4 -> ReportInfoTab(student, academicYear)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Close", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun ProfileAndParentTab(student: StudentEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "Student Profile") {
            DetailRow("Student ID / Code", student.studentCode)
            DetailRow("Full Name", student.name)
            DetailRow("Gender", student.gender)
            DetailRow("Date of Birth", student.dateOfBirth)
            DetailRow("Status", student.status)
        }

        DetailSectionCard(title = "Parent / Guardian Information") {
            DetailRow("Parent / Guardian", student.parentName.ifBlank { "N/A" })
            DetailRow("Contact Phone", student.phone.ifBlank { "N/A" })
            DetailRow("Residential Address", student.address.ifBlank { "N/A" })
        }
    }
}

@Composable
fun AcademicDataTab(student: StudentEntity, academicYear: String) {
    val level = when (student.gradeName) {
        "KG" -> "Kindergarten"
        "G1", "G2", "G3", "G4", "G5" -> "Primary Level (G1-G5)"
        "G6", "G7", "G8", "G9" -> "Secondary Level (G6-G9)"
        "G10", "G11", "G12" -> "High School Level (G10-G12)"
        else -> "General Level"
    }

    val subjects = try {
        SchoolPolicy.getDefaultSubjectNamesForGrade(student.gradeName, student.stream)
    } catch (e: Exception) {
        listOf("Myanmar", "English", "Mathematics", "Science")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "Current Enrolled Status") {
            DetailRow("Academic Year", academicYear)
            DetailRow("Education Level", level)
            DetailRow("Grade & Class", "${student.gradeName} (${student.className})")
            DetailRow("Roll Number", "#${student.rollNumber}")
            if (SchoolPolicy.isHighSchool(student.gradeName)) {
                DetailRow("Track / Stream", student.stream.ifBlank { "STEAMS-1 (Biology Track)" })
            }
        }

        DetailSectionCard(title = "Mapped Academic Subjects (${subjects.size})") {
            Text(
                text = subjects.joinToString(" • "),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun AttendanceDataTab(student: StudentEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "Attendance Summary") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttendanceStatBadge("Present", "172 days", Color(0xFF2E7D32))
                AttendanceStatBadge("Absent", "4 days", Color(0xFFC62828))
                AttendanceStatBadge("Leave", "2 days", Color(0xFFEF6C00))
                AttendanceStatBadge("Rate", "96.6%", MaterialTheme.colorScheme.primary)
            }
        }

        DetailSectionCard(title = "Punctuality & Notes") {
            DetailRow("Morning Session", "Regular (08:00 AM - 12:00 PM)")
            DetailRow("Afternoon Session", "Regular (12:30 PM - 03:30 PM)")
            DetailRow("Overall Record", "Good attendance record for current term.")
        }
    }
}

@Composable
fun AttendanceStatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun HcmHolisticTab(student: StudentEntity) {
    val level = when (student.gradeName) {
        "KG" -> "Kindergarten"
        "G1", "G2", "G3", "G4", "G5" -> "Primary Level"
        "G6", "G7", "G8", "G9" -> "Secondary Level"
        else -> "High School Level"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "HCM Holistic Assessment ($level)") {
            PillarRatingRow("1. Moral & Ethics (ဗလ ၅ တန်)", 5)
            PillarRatingRow("2. Intellectual Growth", 4)
            PillarRatingRow("3. Physical Development", 5)
            PillarRatingRow("4. Social & Leadership", 4)
            PillarRatingRow("5. Aesthetics & Culture", 5)
        }

        DetailSectionCard(title = "Teacher Overall Evaluation") {
            DetailRow("Conduct Grade", "A (Excellent)")
            DetailRow("General Remarks", "Demonstrates strong leadership, positive behavior, and active engagement.")
        }
    }
}

@Composable
fun PillarRatingRow(pillarName: String, stars: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pillarName, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Row {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < stars) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = null,
                    tint = if (index < stars) Color(0xFFFFB300) else Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ReportInfoTab(student: StudentEntity, academicYear: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "Report Card Status ($academicYear)") {
            DetailRow("Monthly Tests Status", "Completed & Verified")
            DetailRow("Assessment Standing", "PASS (Overall Marks: 485/600)")
            DetailRow("Class Rank", "#${student.rollNumber}")
            DetailRow("Report Card Generation", "Ready for PDF Export")
        }

        DetailSectionCard(title = "PDF & Document Actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Generate Student Report Card PDF", fontSize = 11.sp)
                OutlinedButton(
                    onClick = { },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Card", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun DetailSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
