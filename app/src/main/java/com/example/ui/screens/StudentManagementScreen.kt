package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.HolisticResultEntity
import com.example.data.local.entity.TeacherCommentEntity
import com.example.data.local.entity.AssessmentResultSummaryEntity
import com.example.data.local.entity.ReportGenerationHistoryEntity
import com.example.data.local.entity.StudentMarkEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.ExamQuestionBlueprintEntity
import com.example.data.local.entity.QuestionBlueprintItem
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.HolisticRepository
import com.example.data.repository.MarksRepository
import com.example.data.repository.ReportRepository
import com.example.data.repository.AssessmentRepository
import com.example.data.policy.SchoolPolicy
import com.example.data.policy.ExamQuestionBlueprintHelper
import com.example.ui.util.StudentPhotoUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

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
    deletionVerificationEvent: kotlinx.coroutines.flow.SharedFlow<com.example.data.sync.DeletionVerificationResult>? = null,
    attendanceRepository: AttendanceRepository? = null,
    holisticRepository: HolisticRepository? = null,
    marksRepository: MarksRepository? = null,
    reportRepository: ReportRepository? = null,
    assessmentRepository: AssessmentRepository? = null
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
    var showMockConfirmDialog by remember { mutableStateOf(false) }

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

                    if (currentUser?.role == com.example.data.local.entity.UserRole.SUPER_ADMIN) {
                        IconButton(
                            onClick = { showMockConfirmDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Import Demo Students (Admin Only)",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            academicYear = academicYear,
            grades = gradeNames.filter { it != "All" },
            classes = listOf("A", "B", "C", "D"),
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedStudent ->
                onSaveStudent(updatedStudent)
                showAddEditDialog = false
            }
        )
    }

    // Real Attendance records for the student currently being viewed
    val studentAttendanceRecords by produceState<List<AttendanceRecordEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.id,
        key2 = academicYear
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && attendanceRepository != null) {
            attendanceRepository.getAttendanceForStudent(currentStudent.id, academicYear).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Real Holistic records for the student currently being viewed
    val studentHolisticResults by produceState<List<HolisticResultEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.id,
        key2 = academicYear
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && holisticRepository != null) {
            holisticRepository.getHolisticResultsForStudentAllPeriods(currentStudent.id, academicYear).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Real Teacher comments for the student currently being viewed
    val studentTeacherComments by produceState<List<TeacherCommentEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.id,
        key2 = academicYear
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && holisticRepository != null) {
            holisticRepository.getTeacherCommentsForStudent(currentStudent.id, academicYear).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Real Assessment results for the student currently being viewed
    val studentAssessmentResults by produceState<List<AssessmentResultSummaryEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.id
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && marksRepository != null) {
            marksRepository.getAssessmentResultsForStudent(currentStudent.id).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Real Generated Report history for the student currently being viewed
    val studentReportHistory by produceState<List<ReportGenerationHistoryEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.id
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && reportRepository != null) {
            reportRepository.getGenerationHistoryForStudent(currentStudent.id).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Real Student Marks for the student currently being viewed
    val studentMarks by produceState<List<StudentMarkEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.id
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && marksRepository != null) {
            marksRepository.getMarksForStudent(currentStudent.id).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Question Blueprints for the student's grade
    val gradeBlueprints by produceState<List<ExamQuestionBlueprintEntity>>(
        initialValue = emptyList(),
        key1 = studentToViewDetail?.gradeName
    ) {
        val currentStudent = studentToViewDetail
        if (currentStudent != null && marksRepository != null) {
            marksRepository.getBlueprintsForGrade(currentStudent.gradeName).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // Assessments for the academic year
    val allAssessments by produceState<List<AssessmentEntity>>(
        initialValue = emptyList(),
        key1 = academicYear
    ) {
        if (assessmentRepository != null) {
            assessmentRepository.getAssessmentsForAcademicYear(academicYear).collect {
                value = it
            }
        } else {
            value = emptyList()
        }
    }

    // View Student Profile Detail Dialog
    studentToViewDetail?.let { student ->
        StudentDetailDialog(
            student = student,
            academicYear = academicYear,
            canDelete = canDeleteStudent,
            attendanceRecords = studentAttendanceRecords,
            holisticResults = studentHolisticResults,
            teacherComments = studentTeacherComments,
            assessmentResults = studentAssessmentResults,
            reportHistory = studentReportHistory,
            studentMarks = studentMarks,
            assessments = allAssessments,
            blueprints = gradeBlueprints,
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
        val exportContext = LocalContext.current
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Student Records", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exporting ${students.size} student records in HCM-SMS format.", fontSize = 13.sp)
                    Text("• Student Master Sheet (CSV/Excel)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("• Includes Student ID, NRC, Parents, Class, Roll Number and Contact info.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        try {
                            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                            val exportDir = try {
                                val docs = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                                val dir = java.io.File(docs, "HCM_SMS_Exports")
                                if (!dir.exists()) dir.mkdirs()
                                dir
                            } catch (_: Exception) {
                                val dir = java.io.File(exportContext.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "HCM_SMS_Exports")
                                if (!dir.exists()) dir.mkdirs()
                                dir
                            }
                            val file = java.io.File(exportDir, "students_roster_$timeStamp.csv")
                            val sb = java.lang.StringBuilder()
                            sb.append("Student Code,Full Name,Gender,Date of Birth,Grade,Class,Roll No,NRC,Father Name,Mother Name,Parent Phone,Secondary Phone,Address,Status\n")
                            students.forEach { st ->
                                sb.append("${st.studentCode},\"${st.name}\",${st.gender},${st.dateOfBirth},${st.gradeName},${st.className},${st.rollNumber},\"${st.studentNrc}\",\"${st.fatherName}\",\"${st.motherName}\",\"${st.phone}\",\"${st.secondaryPhone}\",\"${st.address}\",${st.status}\n")
                            }
                            file.writeText(sb.toString())

                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                exportContext,
                                "${exportContext.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            exportContext.startActivity(android.content.Intent.createChooser(shareIntent, "Share Student Master Sheet (CSV)"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(exportContext, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export & Share CSV", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        )
    }

    // Import Mock Data Confirmation Dialog (Super Admin Only)
    if (showMockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMockConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Import Demo Students?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Warning: This will add 4 mock students (G6, G7, G9, G11) to the active student list for testing purposes.",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Do not perform this action if your school database is already operating in live production.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMockConfirmDialog = false
                        onImportMockData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Import Demo Data", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMockConfirmDialog = false }) {
                    Text("Cancel", fontSize = 12.sp)
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

            // Row 2: Avatar + Name + Roll/Parent/NRC + Quick Actions
            val context = LocalContext.current
            val photoBitmap = remember(student.photoUrl) {
                StudentPhotoUtils.loadStudentPhotoImageBitmap(context, student.photoUrl)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = "Photo of ${student.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = student.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val parentDisplay = student.parentName.ifBlank {
                        when {
                            student.fatherName.isNotBlank() && student.motherName.isNotBlank() -> "${student.fatherName} / ${student.motherName}"
                            student.fatherName.isNotBlank() -> student.fatherName
                            student.motherName.isNotBlank() -> student.motherName
                            else -> "N/A"
                        }
                    }
                    val nrcSuffix = if (student.studentNrc.isNotBlank()) " • NRC: ${student.studentNrc}" else ""
                    Text(
                        text = "Roll #${student.rollNumber} • Parent: $parentDisplay$nrcSuffix",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Actions (Touch friendly min 40dp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val callTarget = student.phone.ifBlank { student.secondaryPhone }
                    if (callTarget.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:${callTarget.trim()}")
                                    }
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Cannot open dialer: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Parent",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
    academicYear: String = "2026-2027",
    grades: List<String>,
    classes: List<String>,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    val context = LocalContext.current
    val yearPrefix = academicYear.substringBefore('-').ifBlank { "2026" }
    var studentCode by remember { mutableStateOf(student?.studentCode ?: "HCM-$yearPrefix-${(100..999).random()}") }
    var name by remember { mutableStateOf(student?.name ?: "") }
    var gender by remember { mutableStateOf(student?.gender ?: "Male") }
    var gradeName by remember { mutableStateOf(student?.gradeName ?: (grades.firstOrNull() ?: "G1")) }
    val defaultBirthYear = when (gradeName) {
        "KG" -> 2021
        "G1" -> 2020
        "G2" -> 2019
        "G3" -> 2018
        "G4" -> 2017
        "G5" -> 2016
        "G6" -> 2015
        "G7" -> 2014
        "G8" -> 2013
        "G9" -> 2012
        "G10" -> 2011
        "G11" -> 2010
        "G12" -> 2009
        else -> 2016
    }
    var dob by remember { mutableStateOf(student?.dateOfBirth?.ifBlank { "$defaultBirthYear-06-01" } ?: "$defaultBirthYear-06-01") }
    var stream by remember { mutableStateOf(student?.stream ?: if (SchoolPolicy.isHighSchool(gradeName)) "STEAMS-1" else "") }
    var className by remember { mutableStateOf(student?.className ?: "A") }
    var rollNumber by remember { mutableStateOf(student?.rollNumber?.toString() ?: "1") }
    var photoUrl by remember { mutableStateOf(student?.photoUrl ?: "") }
    var studentNrc by remember { mutableStateOf(student?.studentNrc ?: "") }
    var fatherName by remember { mutableStateOf(student?.fatherName ?: "") }
    var fatherNrc by remember { mutableStateOf(student?.fatherNrc ?: "") }
    var motherName by remember { mutableStateOf(student?.motherName ?: "") }
    var motherNrc by remember { mutableStateOf(student?.motherNrc ?: "") }
    var phone by remember { mutableStateOf(student?.phone ?: "") }
    var secondaryPhone by remember { mutableStateOf(student?.secondaryPhone ?: "") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var status by remember { mutableStateOf(student?.status ?: "Active") }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedFileName = StudentPhotoUtils.savePhotoFromUri(context, uri)
            if (savedFileName != null) {
                photoUrl = savedFileName
            }
        }
    }

    val studentPhotoBitmap = remember(photoUrl) {
        StudentPhotoUtils.loadStudentPhotoImageBitmap(context, photoUrl)
    }

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
                // Photo Picker Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    photoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (studentPhotoBitmap != null) {
                                Image(
                                    bitmap = studentPhotoBitmap,
                                    contentDescription = "Student Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = "Select Photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("Photo", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (photoUrl.isBlank()) "Choose Photo" else "Change Photo", fontSize = 11.sp)
                            }
                            if (photoUrl.isNotBlank()) {
                                TextButton(
                                    onClick = { photoUrl = "" },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Remove", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

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
                        label = { Text("Student Full Name *", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = studentNrc,
                        onValueChange = { studentNrc = it },
                        label = { Text("Student NRC (e.g. 12/KAMAYA(N)123456)", fontSize = 12.sp) },
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
                    Text(
                        text = "Parents / Guardians Information",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = fatherNrc,
                        onValueChange = { fatherNrc = it },
                        label = { Text("Father's NRC (e.g. 12/KAMAYA(N)123456)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("Mother's Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = motherNrc,
                        onValueChange = { motherNrc = it },
                        label = { Text("Mother's NRC (e.g. 12/KAMAYA(N)654321)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Primary Phone Number (Phone 1)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 09-123456789", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        label = { Text("Secondary Phone / Emergency (Phone 2)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 09-987654321", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                        val computedParentName = when {
                            fatherName.isNotBlank() && motherName.isNotBlank() -> "$fatherName / $motherName"
                            fatherName.isNotBlank() -> fatherName
                            motherName.isNotBlank() -> motherName
                            else -> student?.parentName ?: ""
                        }
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
                                parentName = computedParentName,
                                phone = phone,
                                secondaryPhone = secondaryPhone,
                                address = address,
                                status = status,
                                photoAvatarIndex = student?.photoAvatarIndex ?: 0,
                                stream = if (SchoolPolicy.isHighSchool(gradeName)) (if (stream.isNotBlank()) stream else "STEAMS-1") else "",
                                photoUrl = photoUrl,
                                studentNrc = studentNrc,
                                fatherName = fatherName,
                                fatherNrc = fatherNrc,
                                motherName = motherName,
                                motherNrc = motherNrc,
                                uuid = student?.uuid ?: java.util.UUID.randomUUID().toString(),
                                createdAt = student?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                isDirty = true
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
    attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    holisticResults: List<HolisticResultEntity> = emptyList(),
    teacherComments: List<TeacherCommentEntity> = emptyList(),
    assessmentResults: List<AssessmentResultSummaryEntity> = emptyList(),
    reportHistory: List<ReportGenerationHistoryEntity> = emptyList(),
    studentMarks: List<StudentMarkEntity> = emptyList(),
    assessments: List<AssessmentEntity> = emptyList(),
    blueprints: List<ExamQuestionBlueprintEntity> = emptyList(),
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Profile", "Academic", "Attendance", "HCM Holistic", "Reports")
    val scrollState = rememberScrollState()

    // Reset scroll when switching tabs so each tab starts at the top
    LaunchedEffect(selectedTab) {
        scrollState.scrollTo(0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
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
                    val detailContext = LocalContext.current
                    val detailPhotoBitmap = remember(student.photoUrl) {
                        StudentPhotoUtils.loadStudentPhotoImageBitmap(detailContext, student.photoUrl)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (student.gender == "Male") MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (detailPhotoBitmap != null) {
                            Image(
                                bitmap = detailPhotoBitmap,
                                contentDescription = "Photo of ${student.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = if (student.gender == "Male") Icons.Default.Face else Icons.Default.Face3,
                                contentDescription = null,
                                tint = if (student.gender == "Male") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${student.studentCode} • Roll #${student.rollNumber}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row {
                    val callTarget = student.phone.ifBlank { student.secondaryPhone }
                    if (callTarget.isNotBlank()) {
                        val headerCtx = LocalContext.current
                        IconButton(
                            onClick = {
                                try {
                                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:${callTarget.trim()}")
                                    }
                                    headerCtx.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(headerCtx, "Cannot open dialer: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Quick Call", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        }
                    }
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
                    .heightIn(min = 320.dp, max = 560.dp),
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

                // Scrollable container for tab content so all information is completely visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                ) {
                    when (selectedTab) {
                        0 -> ProfileAndParentTab(student)
                        1 -> AcademicDataTab(
                            student = student,
                            academicYear = academicYear,
                            studentMarks = studentMarks,
                            assessments = assessments,
                            blueprints = blueprints
                        )
                        2 -> AttendanceDataTab(student, academicYear, attendanceRecords)
                        3 -> HcmHolisticTab(student, academicYear, holisticResults, teacherComments)
                        4 -> ReportInfoTab(student, academicYear, assessmentResults, reportHistory)
                    }
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
    val context = LocalContext.current
    val photoBitmap = remember(student.photoUrl) {
        StudentPhotoUtils.loadStudentPhotoImageBitmap(context, student.photoUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "Student Profile") {
            if (photoBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = "Photo of ${student.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            DetailRow("Student ID / Code", student.studentCode)
            DetailRow("Full Name", student.name)
            DetailRow("Student NRC", student.studentNrc.ifBlank { "N/A" })
            DetailRow("Gender", student.gender)
            DetailRow("Date of Birth", student.dateOfBirth)
            DetailRow("Status", student.status)
        }

        DetailSectionCard(title = "Parent / Guardian Information") {
            if (student.fatherName.isNotBlank() || student.fatherNrc.isNotBlank()) {
                DetailRow("Father's Name", student.fatherName.ifBlank { "N/A" })
                DetailRow("Father's NRC", student.fatherNrc.ifBlank { "N/A" })
            }
            if (student.motherName.isNotBlank() || student.motherNrc.isNotBlank()) {
                DetailRow("Mother's Name", student.motherName.ifBlank { "N/A" })
                DetailRow("Mother's NRC", student.motherNrc.ifBlank { "N/A" })
            }
            if (student.fatherName.isBlank() && student.motherName.isBlank()) {
                DetailRow("Parent / Guardian", student.parentName.ifBlank { "N/A" })
            }
            PhoneDetailRow("Primary Phone (Phone 1)", student.phone)
            PhoneDetailRow("Secondary Phone (Phone 2)", student.secondaryPhone)
            DetailRow("Residential Address", student.address.ifBlank { "N/A" })
        }
    }
}

data class SubjectQuestionBreakdownItem(
    val qNo: String,
    val title: String,
    val obtained: Double,
    val maxMark: Double,
    val percentage: Double,
    val isWeakness: Boolean
)

private data class SubjectStatusBadgeColors(
    val text: String,
    val bg: Color,
    val textColor: Color,
    val border: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicDataTab(
    student: StudentEntity,
    academicYear: String,
    studentMarks: List<StudentMarkEntity> = emptyList(),
    assessments: List<AssessmentEntity> = emptyList(),
    blueprints: List<ExamQuestionBlueprintEntity> = emptyList()
) {
    val level = when (student.gradeName) {
        "KG" -> "Kindergarten"
        "G1", "G2", "G3", "G4", "G5" -> "Primary Level (G1-G5)"
        "G6", "G7", "G8", "G9" -> "Secondary Level (G6-G9)"
        "G10", "G11", "G12" -> "High School Level (G10-G12)"
        else -> "General Level"
    }

    val subjects = remember(student.gradeName, student.stream) {
        try {
            SchoolPolicy.getDefaultSubjectNamesForGrade(student.gradeName, student.stream)
        } catch (e: Exception) {
            listOf("Myanmar", "English", "Mathematics", "Science")
        }
    }

    // Assessments relevant to student marks or academic year
    val relevantAssessments = remember(assessments, studentMarks) {
        val markAssessmentIds = studentMarks.map { it.assessmentId }.toSet()
        if (markAssessmentIds.isNotEmpty()) {
            val matched = assessments.filter { it.id in markAssessmentIds }
            if (matched.isNotEmpty()) matched else assessments
        } else {
            assessments
        }
    }

    // Track selected assessment filter (if assessments exist)
    var selectedAssessmentId by remember(relevantAssessments, studentMarks) {
        mutableStateOf(
            relevantAssessments.firstOrNull { it.id in studentMarks.map { m -> m.assessmentId } }?.id
                ?: relevantAssessments.firstOrNull()?.id
        )
    }

    // Filter student marks by selected assessment
    val filteredMarks = remember(studentMarks, selectedAssessmentId) {
        if (selectedAssessmentId != null) {
            studentMarks.filter { it.assessmentId == selectedAssessmentId }
        } else {
            studentMarks
        }
    }

    // Track expanded state for each subject card (Option A: Accordion)
    val expandedSubjects = remember { mutableStateMapOf<String, Boolean>() }

    // Precalculate weaknesses across all subjects for summary badge
    val allWeakQuestions = remember(subjects, filteredMarks, blueprints) {
        val list = mutableListOf<Triple<String, String, Double>>()
        subjects.forEach { subj ->
            val mark = filteredMarks.firstOrNull { it.subjectName.equals(subj, ignoreCase = true) }
            if (mark != null && !mark.questionMarksJson.isNullOrBlank() && mark.questionMarksJson != "{}") {
                val qMarks = ExamQuestionBlueprintHelper.parseQuestionMarks(mark.questionMarksJson)
                val bp = blueprints.firstOrNull { it.subjectName.equals(subj, ignoreCase = true) }
                val qItems = if (bp != null) {
                    val p = ExamQuestionBlueprintHelper.parseQuestions(bp.questionsJson)
                    if (p.isNotEmpty()) p else ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(subj)
                } else {
                    ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(subj)
                }

                qItems.forEach { item ->
                    val obt = qMarks[item.qNo] ?: 0.0
                    val max = if (item.maxMark > 0.0) item.maxMark else 20.0
                    val pct = (obt / max) * 100.0
                    if (pct < 50.0) {
                        list.add(Triple(subj, "${item.qNo} ${item.title.take(15)}", pct))
                    }
                }
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Current Enrolled Status Card
        DetailSectionCard(title = "Current Enrolled Status") {
            DetailRow("Academic Year", academicYear)
            DetailRow("Education Level", level)
            DetailRow("Grade & Class", "${student.gradeName} (${student.className})")
            DetailRow("Roll Number", "#${student.rollNumber}")
            if (SchoolPolicy.isHighSchool(student.gradeName)) {
                DetailRow("Track / Stream", student.stream.ifBlank { "STEAMS-1 (Biology Track)" })
            }
        }

        // 2. Exam Period / Assessment Selector (if multiple assessments available)
        if (relevantAssessments.size > 1) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Exam Period:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        relevantAssessments.forEach { asm ->
                            val isSelected = selectedAssessmentId == asm.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAssessmentId = asm.id },
                                label = {
                                    Text(
                                        asm.assessmentName.ifBlank { asm.assessmentType },
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.height(28.dp),
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // 3. Question Weakness Overall Focus Area Banner
        if (allWeakQuestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFEBEE),
                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "အထူးဂရုပြုရန် အားနည်းချက်များ (${allWeakQuestions.size} ပိုင်း)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                    Text(
                        text = "အောက်ပါ မေးခွန်းအပိုင်းများသည် ကျောင်းသားရမှတ် ၅၀% အောက် ဖြစ်နေပါသည် -",
                        fontSize = 10.sp,
                        color = Color(0xFFB71C1C)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allWeakQuestions.take(6).forEach { (subj, qTitle, pct) ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White,
                                border = BorderStroke(0.5.dp, Color(0xFFEF9A9A))
                            ) {
                                Text(
                                    text = "$subj • $qTitle (${pct.toInt()}%)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (filteredMarks.any { !it.questionMarksJson.isNullOrBlank() && it.questionMarksJson != "{}" }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "အားလုံးသော မေးခွန်းအပိုင်းများတွင် ၅၀% နှင့်အထက် ရရှိထားပါသည် ✨",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }

        // 4. Section Title with Expand / Collapse All control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ဘာသာရပ်အလိုက် မေးခွန်းရမှတ်နှင့် အားနည်းချက်",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val allExpanded = subjects.all { expandedSubjects[it] == true }
            TextButton(
                onClick = {
                    val target = !allExpanded
                    subjects.forEach { expandedSubjects[it] = target }
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = if (allExpanded) "အားလုံးပိတ်ရန်" else "အားလုံးဖွင့်ရန်",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 5. Expandable Subject Cards (Option A Accordion Pattern)
        subjects.forEach { subjectName ->
            val mark = filteredMarks.firstOrNull { it.subjectName.equals(subjectName, ignoreCase = true) }
            val blueprint = blueprints.firstOrNull { it.subjectName.equals(subjectName, ignoreCase = true) }
            val isExpanded = expandedSubjects[subjectName] ?: false

            SubjectQuestionAccordionCard(
                subjectName = subjectName,
                mark = mark,
                blueprint = blueprint,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedSubjects[subjectName] = !(expandedSubjects[subjectName] ?: false)
                }
            )
        }
    }
}

@Composable
fun SubjectQuestionAccordionCard(
    subjectName: String,
    mark: StudentMarkEntity?,
    blueprint: ExamQuestionBlueprintEntity?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    // Parse question items and marks
    val questionStats = remember(mark?.questionMarksJson, blueprint?.questionsJson, subjectName) {
        val qMarks = mark?.questionMarksJson?.let { ExamQuestionBlueprintHelper.parseQuestionMarks(it) } ?: emptyMap()
        val bpItems = if (blueprint != null) {
            val parsed = ExamQuestionBlueprintHelper.parseQuestions(blueprint.questionsJson)
            if (parsed.isNotEmpty()) parsed else ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(subjectName)
        } else {
            ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(subjectName)
        }

        bpItems.map { item ->
            val obtained = qMarks[item.qNo] ?: 0.0
            val max = if (item.maxMark > 0.0) item.maxMark else 20.0
            val pct = if (max > 0.0) (obtained / max) * 100.0 else 0.0
            SubjectQuestionBreakdownItem(
                qNo = item.qNo,
                title = item.title,
                obtained = obtained,
                maxMark = max,
                percentage = pct,
                isWeakness = pct < 50.0
            )
        }
    }

    val hasQuestionMarks = remember(mark?.questionMarksJson) {
        !mark?.questionMarksJson.isNullOrBlank() && mark?.questionMarksJson != "{}"
    }

    val criticalWeaknesses = remember(questionStats, hasQuestionMarks) {
        if (hasQuestionMarks) questionStats.filter { it.isWeakness } else emptyList()
    }

    val scoreDisplay = when {
        mark != null && mark.obtainedMarks != null -> {
            val pct = (mark.obtainedMarks / mark.maxMarks.coerceAtLeast(1)) * 100.0
            "${mark.obtainedMarks.toInt()}/${mark.maxMarks} (${pct.toInt()}%)"
        }
        else -> "Pending"
    }

    val statusBadge = when {
        mark == null || mark.obtainedMarks == null -> SubjectStatusBadgeColors("Pending", Color(0xFFF5F5F5), Color.Gray, Color.LightGray)
        mark.isDistinction -> SubjectStatusBadgeColors("Distinction", Color(0xFFFFF8E1), Color(0xFFE65100), Color(0xFFFFB300))
        mark.isPassed -> SubjectStatusBadgeColors("Pass", Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF81C784))
        else -> SubjectStatusBadgeColors("Fail", Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFE57373))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (criticalWeaknesses.isNotEmpty()) Color(0xFFEF9A9A)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (criticalWeaknesses.isNotEmpty()) Color(0xFFFFFBFA) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable for Accordion Expansion)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Subject Name & Weakness Quick Badge
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = subjectName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Compact Alert tag shown right in collapsed header (Option A Requirement)
                    if (hasQuestionMarks) {
                        if (criticalWeaknesses.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Weak: ${criticalWeaknesses.joinToString { "${it.qNo} (${it.percentage.toInt()}%)" }}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC62828),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = "✨ အပိုင်းအားလုံး အခြေအနေကောင်းမွန်ပါသည်",
                                fontSize = 9.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    } else if (mark?.obtainedMarks != null) {
                        Text(
                            text = "မေးခွန်းအလိုက် အမှတ်ခွဲခြမ်းစိတ်ဖြာချက် မရှိသေးပါ",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Right: Marks Badge, Status Pill, Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = scoreDisplay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBadge.bg,
                        border = BorderStroke(0.8.dp, statusBadge.border)
                    ) {
                        Text(
                            text = statusBadge.text,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusBadge.textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded Breakdown Content (Option A Accordion)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.8.dp
                    )

                    if (hasQuestionMarks && questionStats.isNotEmpty()) {
                        // Breakdown Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "မေးခွန်းနံပါတ်နှင့် ခေါင်းစဉ်",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "ရမှတ်နှင့် စွမ်းဆောင်ရည်",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Question rows with progress bars
                        questionStats.forEach { qStat ->
                            val progress = (qStat.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                            val barColor = when {
                                qStat.percentage < 40.0 -> Color(0xFFD32F2F)
                                qStat.percentage < 70.0 -> Color(0xFFF57C00)
                                else -> Color(0xFF2E7D32)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (qStat.isWeakness) Color(0xFFFFEBEE).copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = if (qStat.isWeakness) BorderStroke(0.5.dp, Color(0xFFEF9A9A)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = qStat.qNo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (qStat.isWeakness) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "• ${qStat.title.ifBlank { "Section ${qStat.qNo}" }}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${if (qStat.obtained % 1.0 == 0.0) qStat.obtained.toInt() else qStat.obtained}/${qStat.maxMark.toInt()}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "(${qStat.percentage.toInt()}%)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = barColor
                                            )
                                            if (qStat.percentage < 40.0) {
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = Color(0xFFFFCDD2)
                                                ) {
                                                    Text(
                                                        text = "အားနည်း",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB71C1C),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else if (qStat.percentage >= 75.0) {
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = Color(0xFFC8E6C9)
                                                ) {
                                                    Text(
                                                        text = "ကျွမ်းကျင်",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1B5E20),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Progress bar
                                    LinearProgressIndicator(
                                        progress = progress,
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }

                        // Pedagogical Diagnostic Insight / Recommendation Card
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (criticalWeaknesses.isNotEmpty()) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                            border = BorderStroke(0.5.dp, if (criticalWeaknesses.isNotEmpty()) Color(0xFFFFB74D) else Color(0xFFA5D6A7)),
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (criticalWeaknesses.isNotEmpty()) Icons.Default.Lightbulb else Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (criticalWeaknesses.isNotEmpty()) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (criticalWeaknesses.isNotEmpty()) "ဆရာ/ဆရာမများအတွက် သင်ကြားရေး အကြံပြုချက်:"
                                        else "စွမ်းဆောင်ရည် သုံးသပ်ချက်:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (criticalWeaknesses.isNotEmpty()) Color(0xFFE65100) else Color(0xFF2E7D32)
                                    )
                                    val adviceText = if (criticalWeaknesses.isNotEmpty()) {
                                        "ကျောင်းသားသည် ${criticalWeaknesses.joinToString(", ") { "${it.qNo} (${it.title.ifBlank { "အပိုင်း" }})" }} တွင် အမှတ်နည်းပါးနေပါသည်။ အဆိုပါအပိုင်း၏ အခြေခံသဘောတရားများနှင့် မေးခွန်းဟောင်းများကို သီးသန့်ပြန်လည်လေ့ကျင့်ပေးရန် အကြံပြုပါသည်။"
                                    } else {
                                        "ဤဘာသာရပ်၏ မေးခွန်းအပိုင်းအားလုံးတွင် မျှတစွာ ကောင်းမွန်သော စွမ်းဆောင်ရည်ပြသထားပါသည်။ လက်ရှိ သင်ကြားရေးနည်းစနစ်အတိုင်း ဆက်လက်ထိန်းသိမ်းရန် အကြံပြုပါသည်။"
                                    }
                                    Text(
                                        text = adviceText,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    } else if (mark?.obtainedMarks != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "မေးခွန်းတစ်ခုချင်း အမှတ်ခွဲခြမ်းစိတ်ဖြာချက် မရှိသေးပါ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ဤဘာသာရပ်အတွက် စုစုပေါင်းရမှတ် (${mark.obtainedMarks.toInt()}/${mark.maxMarks}) ထည့်သွင်းထားပြီးဖြစ်သော်လည်း မေးခွန်းအလိုက် အမှတ်များ ထည့်သွင်းမထားသေးပါ။ Marks Entry မော်ဂျူးတွင် မေးခွန်းအလိုက် အမှတ်များ ဖြည့်သွင်းနိုင်ပါသည်။",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ဤဘာသာရပ်အတွက် ရမှတ်များ ထည့်သွင်းထားခြင်း မရှိသေးပါ။",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceDataTab(
    student: StudentEntity,
    academicYear: String,
    attendanceRecords: List<AttendanceRecordEntity>
) {
    val totalRecords = attendanceRecords.size
    val presentCount = attendanceRecords.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = attendanceRecords.count { it.status == AttendanceStatus.ABSENT }
    val leaveCount = attendanceRecords.count { it.status == AttendanceStatus.LEAVE }
    val lateCount = attendanceRecords.count { it.status == AttendanceStatus.LATE }

    val rateStr = if (totalRecords > 0) {
        val pct = ((presentCount.toDouble() + (lateCount.toDouble() * 0.5)) / totalRecords.toDouble()) * 100.0
        String.format(java.util.Locale.US, "%.1f%%", pct)
    } else {
        "N/A"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailSectionCard(title = "Attendance Summary ($academicYear)") {
            if (totalRecords == 0) {
                // Clear and accurate empty state when no attendance records exist yet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No Attendance Records Recorded",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No attendance has been recorded for ${student.name} in academic year $academicYear yet. Once marked in the Attendance module, statistics and session logs will appear here.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceStatBadge("Present", "$presentCount days", Color(0xFF2E7D32))
                    AttendanceStatBadge("Absent", "$absentCount days", Color(0xFFC62828))
                    AttendanceStatBadge("Leave", "$leaveCount days", Color(0xFFEF6C00))
                    AttendanceStatBadge("Late", "$lateCount days", Color(0xFFF57C00))
                    AttendanceStatBadge("Rate", rateStr, MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (totalRecords > 0) {
            DetailSectionCard(title = "Recent Sessions (${attendanceRecords.size} Total)") {
                val recentRecords = remember(attendanceRecords) {
                    attendanceRecords.sortedByDescending { it.date + "_" + it.session.name }.take(8)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentRecords.forEach { record ->
                        val (badgeColor, badgeBg) = when (record.status) {
                            AttendanceStatus.PRESENT -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
                            AttendanceStatus.ABSENT -> Color(0xFFC62828) to Color(0xFFFFEBEE)
                            AttendanceStatus.LATE -> Color(0xFFF57C00) to Color(0xFFFFF3E0)
                            AttendanceStatus.LEAVE -> Color(0xFF1976D2) to Color(0xFFE3F2FD)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = record.date,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "(${record.session.name.lowercase().replaceFirstChar { it.uppercase() }})",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = record.status.name,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            DetailSectionCard(title = "Punctuality & Schedule") {
                DetailRow("Morning Session", "Regular (08:00 AM - 12:00 PM)")
                DetailRow("Afternoon Session", "Regular (12:30 PM - 03:30 PM)")
                DetailRow("Status", "Awaiting first attendance check-in")
            }
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
fun HcmHolisticTab(
    student: StudentEntity,
    academicYear: String,
    holisticResults: List<HolisticResultEntity> = emptyList(),
    teacherComments: List<TeacherCommentEntity> = emptyList()
) {
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
        if (holisticResults.isEmpty() && teacherComments.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No Holistic Assessment Recorded",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No holistic evaluation ($level) has been assigned or recorded for this student in $academicYear yet.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val groupedByPeriod = holisticResults.groupBy { it.assessmentPeriod }
            groupedByPeriod.forEach { (period, results) ->
                DetailSectionCard(title = "HCM Holistic Assessment - $period ($level)") {
                    results.forEach { result ->
                        PillarRatingRow(
                            pillarName = result.categoryName.ifBlank { result.pillar },
                            stars = result.ratingStars,
                            maxStars = result.maxStars
                        )
                    }
                }
            }

            if (teacherComments.isNotEmpty()) {
                teacherComments.forEach { comment ->
                    DetailSectionCard(title = "Teacher Evaluation (${comment.assessmentPeriod})") {
                        if (comment.generalComment.isNotBlank()) {
                            DetailRow("General Remarks", comment.generalComment)
                        }
                        if (comment.positiveComments.isNotBlank()) {
                            DetailRow("Strengths", comment.positiveComments)
                        }
                        if (comment.areasForImprovement.isNotBlank()) {
                            DetailRow("Areas to Improve", comment.areasForImprovement)
                        }
                        if (comment.futureRecommendation.isNotBlank()) {
                            DetailRow("Recommendations", comment.futureRecommendation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PillarRatingRow(pillarName: String, stars: Int, maxStars: Int = 5) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pillarName, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Row {
            repeat(maxStars.coerceAtLeast(1)) { index ->
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
fun ReportInfoTab(
    student: StudentEntity,
    academicYear: String,
    assessmentResults: List<AssessmentResultSummaryEntity> = emptyList(),
    reportHistory: List<ReportGenerationHistoryEntity> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (assessmentResults.isEmpty() && reportHistory.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No Assessment or Report Records",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No exam marks, assessment summaries, or generated report cards exist for this student in $academicYear yet.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (assessmentResults.isNotEmpty()) {
                DetailSectionCard(title = "Assessment Results ($academicYear)") {
                    assessmentResults.forEachIndexed { idx, result ->
                        DetailRow("Total Obtained", "${result.totalObtained.toInt()} / ${result.totalMax}")
                        DetailRow("Percentage", String.format(java.util.Locale.US, "%.1f%%", result.percentage))
                        DetailRow("Standing", result.overallResult)
                        if (idx < assessmentResults.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (reportHistory.isNotEmpty()) {
                DetailSectionCard(title = "Generated Report Cards") {
                    reportHistory.forEach { history ->
                        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
                            .format(java.util.Date(history.generatedAtTimestamp))
                        DetailRow(
                            label = "${history.assessmentPeriodName} (${history.academicYear})",
                            value = "${history.status} • $dateStr"
                        )
                    }
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

@Composable
fun PhoneDetailRow(label: String, phoneNumber: String) {
    val context = LocalContext.current
    val hasValidPhone = phoneNumber.isNotBlank() && phoneNumber != "N/A"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(
                text = if (hasValidPhone) phoneNumber else "N/A",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (hasValidPhone) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
        if (hasValidPhone) {
            FilledTonalButton(
                onClick = {
                    try {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:${phoneNumber.trim()}")
                        }
                        context.startActivity(dialIntent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Cannot open dialer: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call $label",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
