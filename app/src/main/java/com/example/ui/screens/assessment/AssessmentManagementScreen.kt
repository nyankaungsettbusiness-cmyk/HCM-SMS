package com.example.ui.screens.assessment

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AssessmentScheduleEntity
import com.example.data.local.entity.AssessmentStatus
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.policy.SchoolPolicy
import com.example.ui.viewmodel.AssessmentSortOption
import com.example.ui.viewmodel.AssessmentViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentManagementScreen(
    assessmentViewModel: AssessmentViewModel,
    currentUser: UserEntity?,
    academicYear: String = "2026-2027",
    availableAcademicYears: List<String> = emptyList(),
    policyViewModel: SchoolPolicyViewModel
) {
    val filteredAssessments by assessmentViewModel.filteredAssessments.collectAsState()
    val totalCount by assessmentViewModel.totalAssessmentsCount.collectAsState()
    val upcomingCount by assessmentViewModel.upcomingAssessmentsCount.collectAsState()
    val completedCount by assessmentViewModel.completedAssessmentsCount.collectAsState()
    val draftCount by assessmentViewModel.draftAssessmentsCount.collectAsState()

    val searchQuery by assessmentViewModel.searchQuery.collectAsState()
    val selectedAcademicYearFilter by assessmentViewModel.selectedAcademicYearFilter.collectAsState()
    val selectedGradeFilter by assessmentViewModel.selectedGradeFilter.collectAsState()
    val selectedSubjectFilter by assessmentViewModel.selectedSubjectFilter.collectAsState()
    val selectedStatusFilter by assessmentViewModel.selectedStatusFilter.collectAsState()
    val selectedTypeFilter by assessmentViewModel.selectedTypeFilter.collectAsState()
    val sortBy by assessmentViewModel.sortBy.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAssessment by remember { mutableStateOf<AssessmentEntity?>(null) }
    var selectedAssessmentForDetail by remember { mutableStateOf<AssessmentEntity?>(null) }
    var scheduleAssessment by remember { mutableStateOf<AssessmentEntity?>(null) }
    var deletingAssessment by remember { mutableStateOf<AssessmentEntity?>(null) }

    // Permissions - Only Administrators can create, edit, or delete assessment configurations
    val canCreate = currentUser?.role == UserRole.SUPER_ADMIN ||
            currentUser?.role == UserRole.ADMIN

    val canDelete = currentUser?.role == UserRole.SUPER_ADMIN ||
            currentUser?.role == UserRole.ADMIN

    val canEdit = currentUser?.role == UserRole.SUPER_ADMIN ||
            currentUser?.role == UserRole.ADMIN

    val dbGrades by policyViewModel.grades.collectAsState()

    // Available options for compact dropdowns
    val academicYearOptions = remember(availableAcademicYears, academicYear) {
        val dynamicYears = (availableAcademicYears + if (academicYear.isNotBlank()) listOf(academicYear) else emptyList())
            .filter { it.isNotBlank() && it != "2024-2025" && it != "2025-2026" }
            .distinct()
        if (dynamicYears.isNotEmpty()) {
            listOf("ALL") + dynamicYears
        } else {
            listOf("ALL", "2026-2027", "2027-2028", "2028-2029")
        }
    }
    val gradeOptions = listOf("ALL") + (if (dbGrades.isNotEmpty()) dbGrades.map { it.gradeName } else SchoolPolicy.VALID_GRADES)
    val subjectOptions = listOf("ALL", "All Subjects", "Myanmar", "English", "Mathematics", "Science", "Physics", "Chemistry", "Biology")
    val typeOptions = listOf("ALL", "Monthly Test", "Weekly Test", "Pilot Test", "CET", "Lesson Completion Test", "Custom Exam")
    val statusOptions = listOf("ALL", "DRAFT", "PUBLISHED", "COMPLETED", "ARCHIVED")

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (canCreate) {
                SmallFloatingActionButton(
                    onClick = {
                        editingAssessment = null
                        showCreateDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Assessment")
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
            // Header Bar
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Exam Management",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredAssessments.size} exams • $academicYear",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (canCreate) {
                    Button(
                        onClick = {
                            editingAssessment = null
                            showCreateDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Exam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Compact Dashboard Stat Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssessmentCompactStatChip("Total", totalCount, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                AssessmentCompactStatChip("Published", upcomingCount, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
                AssessmentCompactStatChip("Completed", completedCount, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                AssessmentCompactStatChip("Drafts", draftCount, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }

            // Compact Search & Filter Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { assessmentViewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search exam, grade, or subject...", fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { assessmentViewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Sort Menu
                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            AssessmentSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName, fontSize = 12.sp) },
                                    onClick = {
                                        assessmentViewModel.setSortBy(option)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (sortBy == option) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Compact Horizontal Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssessmentFilterDropdownChip(
                        label = "Year: $selectedAcademicYearFilter",
                        options = academicYearOptions,
                        selectedOption = selectedAcademicYearFilter,
                        onOptionSelected = { assessmentViewModel.setAcademicYearFilter(it) }
                    )
                    AssessmentFilterDropdownChip(
                        label = "Grade: $selectedGradeFilter",
                        options = gradeOptions,
                        selectedOption = selectedGradeFilter,
                        onOptionSelected = { assessmentViewModel.setGradeFilter(it) }
                    )
                    AssessmentFilterDropdownChip(
                        label = "Subject: $selectedSubjectFilter",
                        options = subjectOptions,
                        selectedOption = selectedSubjectFilter,
                        onOptionSelected = { assessmentViewModel.setSubjectFilter(it) }
                    )
                    AssessmentFilterDropdownChip(
                        label = "Type: $selectedTypeFilter",
                        options = typeOptions,
                        selectedOption = selectedTypeFilter,
                        onOptionSelected = { assessmentViewModel.setTypeFilter(it) }
                    )
                    AssessmentFilterDropdownChip(
                        label = "Status: $selectedStatusFilter",
                        options = statusOptions,
                        selectedOption = selectedStatusFilter,
                        onOptionSelected = { assessmentViewModel.setStatusFilter(it) }
                    )
                }
            }

            // Compact Table View
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Assessment Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.5f))
                        Text("Grade & Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                        Text("Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Text("Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                        Box(modifier = Modifier.width(28.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Table Body List
                    if (filteredAssessments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    text = "No assessments match filters.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                if (canCreate) {
                                    Button(
                                        onClick = {
                                            editingAssessment = null
                                            showCreateDialog = true
                                        },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Create Assessment", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredAssessments, key = { it.id }) { assessment ->
                                AssessmentTableRow(
                                    assessment = assessment,
                                    canEdit = canEdit,
                                    canDelete = canDelete,
                                    onSelect = { selectedAssessmentForDetail = assessment },
                                    onEdit = {
                                        editingAssessment = assessment
                                        showCreateDialog = true
                                    },
                                    onDuplicate = {
                                        assessmentViewModel.duplicateAssessment(
                                            assessment,
                                            currentUser?.fullName ?: "Admin"
                                        )
                                    },
                                    onSchedule = { scheduleAssessment = assessment },
                                    onArchive = { assessmentViewModel.archiveAssessment(assessment.id) },
                                    onDelete = { deletingAssessment = assessment },
                                    onStatusChange = { newStatus ->
                                        assessmentViewModel.updateStatus(assessment.id, newStatus)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog when selecting an exam
    if (selectedAssessmentForDetail != null) {
        AssessmentDetailDialog(
            assessment = selectedAssessmentForDetail!!,
            canEdit = canEdit,
            canDelete = canDelete,
            assessmentViewModel = assessmentViewModel,
            onDismiss = { selectedAssessmentForDetail = null },
            onEdit = {
                editingAssessment = selectedAssessmentForDetail
                selectedAssessmentForDetail = null
                showCreateDialog = true
            },
            onSchedule = {
                scheduleAssessment = selectedAssessmentForDetail
                selectedAssessmentForDetail = null
            },
            onDuplicate = {
                assessmentViewModel.duplicateAssessment(
                    selectedAssessmentForDetail!!,
                    currentUser?.fullName ?: "Admin"
                )
                selectedAssessmentForDetail = null
            },
            onDelete = {
                deletingAssessment = selectedAssessmentForDetail
                selectedAssessmentForDetail = null
            }
        )
    }

    // Create / Edit Dialog
    if (showCreateDialog) {
        AssessmentFormDialog(
            assessment = editingAssessment,
            academicYear = academicYear,
            assessmentViewModel = assessmentViewModel,
            currentUserName = currentUser?.fullName ?: "Admin",
            onDismiss = { showCreateDialog = false },
            onSave = { assessment ->
                assessmentViewModel.saveAssessment(assessment)
                // Ensure default filters allow visibility of newly created exam
                assessmentViewModel.setGradeFilter("ALL")
                assessmentViewModel.setStatusFilter("ALL")
                assessmentViewModel.setSubjectFilter("ALL")
                assessmentViewModel.setAcademicYearFilter(if (academicYear.isNotBlank()) academicYear else "ALL")
                showCreateDialog = false
            }
        )
    }

    // Schedule Dialog
    if (scheduleAssessment != null) {
        ScheduleExamDialog(
            assessment = scheduleAssessment!!,
            assessmentViewModel = assessmentViewModel,
            onDismiss = { scheduleAssessment = null }
        )
    }

    // Delete Confirmation Dialog
    if (deletingAssessment != null) {
        AlertDialog(
            onDismissRequest = { deletingAssessment = null },
            title = { Text("Delete Assessment", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${deletingAssessment?.assessmentName}'? This action cannot be undone.", fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        deletingAssessment?.let { assessmentViewModel.deleteAssessment(it.id) }
                        deletingAssessment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingAssessment = null }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun AssessmentCompactStatChip(
    title: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 10.sp, color = contentColor.copy(alpha = 0.9f))
            Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssessmentFilterDropdownChip(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedOption != "ALL",
            onClick = { expanded = true },
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
            modifier = Modifier.height(32.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            fontSize = 12.sp,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AssessmentTableRow(
    assessment: AssessmentEntity,
    canEdit: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onSchedule: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (AssessmentStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Assessment Name
        Column(modifier = Modifier.weight(2.5f)) {
            Text(
                text = assessment.assessmentName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Max: ${assessment.maxMarks} marks • Month: ${assessment.month.ifBlank { "N/A" }}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        // Grade & Subject
        Column(modifier = Modifier.weight(1.8f)) {
            Text(
                text = "${assessment.grade} (${assessment.className})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = assessment.subjectName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Type Badge
        Box(modifier = Modifier.weight(1.2f)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = assessment.assessmentType,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Status Badge
        Box(modifier = Modifier.weight(1.2f)) {
            val (statusBg, statusFg) = when (assessment.status) {
                AssessmentStatus.DRAFT -> Pair(Color(0xFFFFECB3), Color(0xFF8D6E63))
                AssessmentStatus.PUBLISHED -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                AssessmentStatus.COMPLETED -> Pair(Color(0xFFC8E6C9), Color(0xFF1B5E20))
                AssessmentStatus.ARCHIVED -> Pair(Color(0xFFFFCDD2), Color(0xFFB71C1C))
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusBg
            ) {
                Text(
                    text = assessment.status.displayName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusFg,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }

        // Date
        Text(
            text = assessment.assessmentDate,
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1.1f),
            maxLines = 1
        )

        // Options Action Menu
        Box(modifier = Modifier.width(28.dp)) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("View Full Details", fontSize = 12.sp) },
                    onClick = {
                        showMenu = false
                        onSelect()
                    },
                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                if (canEdit) {
                    DropdownMenuItem(
                        text = { Text("Edit Details", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    DropdownMenuItem(
                        text = { Text("Exam Room Schedule", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            onSchedule()
                        },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    DropdownMenuItem(
                        text = { Text("Duplicate Assessment", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (canEdit) {
                    if (assessment.status == AssessmentStatus.DRAFT) {
                        DropdownMenuItem(
                            text = { Text("Publish Exam", fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                onStatusChange(AssessmentStatus.PUBLISHED)
                            },
                            leadingIcon = { Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    } else if (assessment.status == AssessmentStatus.PUBLISHED) {
                        DropdownMenuItem(
                            text = { Text("Mark Completed", fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                onStatusChange(AssessmentStatus.COMPLETED)
                            },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (assessment.status != AssessmentStatus.ARCHIVED) {
                        DropdownMenuItem(
                            text = { Text("Archive Assessment", fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                onArchive()
                            },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                if (canDelete) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentDetailDialog(
    assessment: AssessmentEntity,
    canEdit: Boolean,
    canDelete: Boolean,
    assessmentViewModel: AssessmentViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSchedule: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val schedule by assessmentViewModel.getSchedule(assessment.id).collectAsState(initial = null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(assessment.assessmentName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("ID: #${assessment.id} • ${assessment.academicYear}", fontSize = 11.sp, color = Color.Gray)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssessmentDetailSectionCard("Assessment Specifications") {
                    AssessmentDetailRow("Academic Year", assessment.academicYear)
                    AssessmentDetailRow("Grade Level & Class", "${assessment.grade} (${assessment.className})")
                    AssessmentDetailRow("Subject Name", assessment.subjectName)
                    AssessmentDetailRow("Subject Type", assessment.subjectType)
                    AssessmentDetailRow("Assessment Type", assessment.assessmentType)
                    AssessmentDetailRow("Max Marks", "${assessment.maxMarks} Marks")
                    AssessmentDetailRow("Scheduled Date", assessment.assessmentDate)
                    AssessmentDetailRow("Target Month / Week", "${assessment.month} ${if (assessment.weekNumber.isNotBlank()) "(${assessment.weekNumber})" else ""}")
                    AssessmentDetailRow("Status", assessment.status.displayName)
                    AssessmentDetailRow("Created By", assessment.createdBy)
                }

                if (assessment.description.isNotBlank()) {
                    AssessmentDetailSectionCard("Description & Instructions") {
                        Text(assessment.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                AssessmentDetailSectionCard("Exam Room & Invigilator Schedule") {
                    if (schedule != null) {
                        AssessmentDetailRow("Exam Time Slot", schedule!!.examTime)
                        AssessmentDetailRow("Room / Hall Number", schedule!!.roomNumber)
                        AssessmentDetailRow("Invigilator Teacher", schedule!!.supervisor)
                    } else {
                        Text("No exam room schedule configured yet.", fontSize = 11.sp, color = Color.Gray)
                        if (canEdit) {
                            TextButton(
                                onClick = onSchedule,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+ Schedule Exam Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canEdit) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = onSchedule,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Close", fontSize = 11.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentFormDialog(
    assessment: AssessmentEntity?,
    academicYear: String,
    assessmentViewModel: AssessmentViewModel,
    currentUserName: String,
    onDismiss: () -> Unit,
    onSave: (AssessmentEntity) -> Unit
) {
    val allAssessments by assessmentViewModel.allAssessments.collectAsState()

    var selectedGrade by remember { mutableStateOf(assessment?.grade ?: "G5") }
    var selectedGrades by remember { mutableStateOf(setOf(assessment?.grade ?: "G5")) }
    var selectedClass by remember { mutableStateOf(assessment?.className ?: "All Classes") }
    var selectedSubject by remember { mutableStateOf(assessment?.subjectName ?: "Mathematics") }
    var selectedSubjectType by remember { mutableStateOf(assessment?.subjectType ?: "Academic Subject") }

    var selectedAssessmentType by remember {
        mutableStateOf(
            when {
                assessment == null -> "Monthly Test"
                assessment.assessmentType.startsWith("Pilot", ignoreCase = true) -> "Pilot Test"
                assessment.assessmentType.startsWith("CET", ignoreCase = true) -> "CET"
                else -> assessment.assessmentType
            }
        )
    }

    var selectedPilotNumber by remember {
        mutableStateOf(
            if (assessment != null && assessment.assessmentType.startsWith("Pilot", ignoreCase = true)) {
                if (assessment.assessmentType in listOf("Pilot Test 1", "Pilot Test 2", "Pilot Test 3", "Pilot Test 4")) {
                    assessment.assessmentType
                } else "Pilot Test 1"
            } else "Pilot Test 1"
        )
    }

    var selectedCetNumber by remember {
        mutableStateOf(
            if (assessment != null && assessment.assessmentType.startsWith("CET", ignoreCase = true)) {
                if (assessment.assessmentType in listOf("CET 1", "CET 2", "CET 3", "CET 4")) {
                    assessment.assessmentType
                } else "CET 1"
            } else "CET 1"
        )
    }

    var selectedStream by remember { mutableStateOf("STEAMS-1") }
    var selectedMonth by remember { mutableStateOf(if (assessment?.month.orEmpty().isNotBlank()) assessment!!.month else "June") }
    var selectedWeekNumber by remember { mutableStateOf(if (assessment?.weekNumber.orEmpty().isNotBlank()) assessment!!.weekNumber else "WT1") }
    var assessmentName by remember { mutableStateOf(assessment?.assessmentName ?: "") }
    var maxMarks by remember { mutableStateOf(assessment?.maxMarks?.toString() ?: "100") }
    var assessmentDate by remember { mutableStateOf(assessment?.assessmentDate ?: "2026-08-15") }
    var description by remember { mutableStateOf(assessment?.description ?: "") }
    var status by remember { mutableStateOf(assessment?.status ?: AssessmentStatus.DRAFT) }

    val monthsList = listOf("June", "July", "August", "September", "October", "November", "December", "January", "February", "March", "April", "May")
    val weekNumbersList = listOf("WT1", "WT2", "WT3", "WT4")

    val isWeeklyTest = selectedAssessmentType.contains("Weekly", ignoreCase = true)
    val isFullGradeAssessment = selectedAssessmentType.contains("Monthly", ignoreCase = true) ||
            selectedAssessmentType.contains("Pilot", ignoreCase = true) ||
            selectedAssessmentType.contains("CET", ignoreCase = true)

    // Automatically set subject to "All Subjects" for full-grade assessments
    LaunchedEffect(isFullGradeAssessment) {
        if (isFullGradeAssessment) {
            selectedSubject = "All Subjects"
            selectedSubjectType = "All"
        }
    }

    // Dynamic Lists based on selected grade
    val availableSubjects = remember(selectedGrade, selectedStream) {
        assessmentViewModel.getSubjectsForGrade(selectedGrade, selectedStream)
    }

    // Duplicate check for Weekly Test
    val duplicateErrorMessage = remember(
        academicYear, selectedGrades, selectedSubject, selectedMonth, selectedWeekNumber, selectedAssessmentType, allAssessments, assessment
    ) {
        if (!isWeeklyTest) null
        else {
            val dups = allAssessments.filter { existing ->
                existing.id != (assessment?.id ?: 0L) &&
                        existing.academicYear.equals(academicYear, ignoreCase = true) &&
                        existing.grade in selectedGrades &&
                        existing.subjectName.equals(selectedSubject, ignoreCase = true) &&
                        existing.month.equals(selectedMonth, ignoreCase = true) &&
                        (existing.weekNumber.equals(selectedWeekNumber, ignoreCase = true) || existing.assessmentName.contains(selectedWeekNumber, ignoreCase = true)) &&
                        existing.assessmentType.contains("Weekly", ignoreCase = true)
            }
            if (dups.isNotEmpty()) {
                val dupGrades = dups.map { it.grade }.distinct().joinToString(", ")
                "Duplicate Weekly Test ($selectedWeekNumber) for $selectedMonth ($selectedSubject) already exists for Grade $dupGrades."
            } else null
        }
    }

    // Auto update assessment name if blank
    LaunchedEffect(selectedGrade, selectedSubject, selectedAssessmentType, selectedPilotNumber, selectedCetNumber, selectedMonth, selectedWeekNumber, isFullGradeAssessment) {
        if (assessment == null || assessmentName.isBlank()) {
            assessmentName = when {
                isWeeklyTest -> "$selectedGrade $selectedSubject Weekly Test ($selectedWeekNumber - $selectedMonth)"
                selectedAssessmentType == "Pilot Test" -> "$selectedGrade $selectedPilotNumber ($selectedMonth)"
                selectedAssessmentType == "CET" -> "$selectedGrade $selectedCetNumber ($selectedMonth)"
                isFullGradeAssessment -> "$selectedGrade $selectedAssessmentType ($selectedMonth)"
                else -> "$selectedGrade $selectedSubject $selectedAssessmentType ($selectedMonth)"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (assessment == null) "Create Assessment" else "Edit Assessment", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Academic Year & Mandatory Month
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = academicYear,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Month Dropdown (Mandatory)
                        var monthExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedMonth,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Month *", fontSize = 10.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                monthsList.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, fontSize = 12.sp) },
                                        onClick = {
                                            selectedMonth = m
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Grade Selection
                item {
                    Text("Select Grade(s):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val allGrades: List<String> = SchoolPolicy.VALID_GRADES
                        items(items = allGrades) { g: String ->
                            val isSel = g in selectedGrades
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    val nextSet = if (isSel) {
                                        if (selectedGrades.size > 1) selectedGrades - g else selectedGrades
                                    } else {
                                        selectedGrades + g
                                    }
                                    selectedGrades = nextSet
                                    selectedGrade = g
                                },
                                label = { Text(g, fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                if (SchoolPolicy.isHighSchool(selectedGrade)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Stream:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            SchoolPolicy.VALID_STREAMS.forEach { st: String ->
                                FilterChip(
                                    selected = selectedStream == st,
                                    onClick = { selectedStream = st },
                                    label = { Text(st, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }

                // Class & Subject Category
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Class Dropdown
                        var classExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = classExpanded,
                            onExpandedChange = { classExpanded = !classExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class", fontSize = 10.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                                modifier = Modifier.menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = classExpanded,
                                onDismissRequest = { classExpanded = false }
                            ) {
                                listOf("All Classes", "Class A", "Class B", "Class C").forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls, fontSize = 12.sp) },
                                        onClick = {
                                            selectedClass = cls
                                            classExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Subject Dropdown
                        if (!isFullGradeAssessment) {
                            var subjectExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = subjectExpanded,
                                onExpandedChange = { subjectExpanded = !subjectExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subject *", fontSize = 10.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = subjectExpanded,
                                    onDismissRequest = { subjectExpanded = false }
                                ) {
                                    availableSubjects.forEach { pair ->
                                        DropdownMenuItem(
                                            text = { Text("${pair.first} (${pair.second})", fontSize = 12.sp) },
                                            onClick = {
                                                selectedSubject = pair.first
                                                selectedSubjectType = pair.second
                                                subjectExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Assessment Type & Week Number
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        var typeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded },
                            modifier = Modifier.weight(if (isWeeklyTest) 1f else 2f)
                        ) {
                            OutlinedTextField(
                                value = selectedAssessmentType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type", fontSize = 10.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                listOf("Monthly Test", "Weekly Test", "Pilot Test", "CET", "Lesson Completion Test", "Custom Exam").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, fontSize = 12.sp) },
                                        onClick = {
                                            selectedAssessmentType = type
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (isWeeklyTest) {
                            var weekExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = weekExpanded,
                                onExpandedChange = { weekExpanded = !weekExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedWeekNumber,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Week *", fontSize = 10.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = weekExpanded,
                                    onDismissRequest = { weekExpanded = false }
                                ) {
                                    weekNumbersList.forEach { wk ->
                                        DropdownMenuItem(
                                            text = { Text(wk, fontSize = 12.sp) },
                                            onClick = {
                                                selectedWeekNumber = wk
                                                weekExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pilot Test Number Selector
                if (selectedAssessmentType == "Pilot Test") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Pilot Test Number *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Pilot Test 1", "Pilot Test 2", "Pilot Test 3", "Pilot Test 4").forEach { num ->
                                    FilterChip(
                                        selected = selectedPilotNumber == num,
                                        onClick = { selectedPilotNumber = num },
                                        label = { Text(num.replace("Pilot Test ", "Pilot "), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        modifier = Modifier.weight(1f).height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // CET Number Selector
                if (selectedAssessmentType == "CET") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("CET Number *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("CET 1", "CET 2", "CET 3", "CET 4").forEach { num ->
                                    FilterChip(
                                        selected = selectedCetNumber == num,
                                        onClick = { selectedCetNumber = num },
                                        label = { Text(num, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        modifier = Modifier.weight(1f).height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Warning
                if (duplicateErrorMessage != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = duplicateErrorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }

                // Assessment Name
                item {
                    OutlinedTextField(
                        value = assessmentName,
                        onValueChange = { assessmentName = it },
                        label = { Text("Assessment Name", fontSize = 10.sp) },
                        placeholder = { Text("e.g. G5 Mathematics Monthly Test", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Max Marks & Date
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = maxMarks,
                            onValueChange = { maxMarks = it },
                            label = { Text("Max Marks", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = assessmentDate,
                            onValueChange = { assessmentDate = it },
                            label = { Text("Date (YYYY-MM-DD)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                // Status Selection
                item {
                    Text("Status:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AssessmentStatus.values().forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.displayName, fontSize = 9.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = duplicateErrorMessage == null && assessmentName.isNotBlank() && (isFullGradeAssessment || selectedSubject.isNotBlank()),
                onClick = {
                    if (duplicateErrorMessage == null && assessmentName.isNotBlank() && (isFullGradeAssessment || selectedSubject.isNotBlank())) {
                        val finalSubjectName = if (isFullGradeAssessment) "All Subjects" else selectedSubject
                        val finalSubjectType = if (isFullGradeAssessment) "All" else selectedSubjectType
                        val finalAssessmentType = when (selectedAssessmentType) {
                            "Pilot Test" -> selectedPilotNumber
                            "CET" -> selectedCetNumber
                            else -> selectedAssessmentType
                        }

                        selectedGrades.forEach { gradeName ->
                            val finalName = if (selectedGrades.size > 1 && assessmentName.contains(selectedGrade)) {
                                assessmentName.replace(selectedGrade, gradeName)
                            } else assessmentName

                            val newEntity = AssessmentEntity(
                                id = if (assessment != null && selectedGrades.size == 1) assessment.id else 0L,
                                academicYear = academicYear,
                                grade = gradeName,
                                className = selectedClass,
                                subjectName = finalSubjectName,
                                subjectType = finalSubjectType,
                                assessmentType = finalAssessmentType,
                                assessmentName = finalName,
                                maxMarks = maxMarks.toIntOrNull() ?: 100,
                                assessmentDate = assessmentDate,
                                month = selectedMonth,
                                weekNumber = if (isWeeklyTest) selectedWeekNumber else "",
                                description = description,
                                status = status,
                                createdBy = assessment?.createdBy ?: currentUserName
                            )
                            onSave(newEntity)
                        }
                    }
                }
            ) {
                Text(if (assessment == null) "Create Assessment" else "Save Changes", fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun ScheduleExamDialog(
    assessment: AssessmentEntity,
    assessmentViewModel: AssessmentViewModel,
    onDismiss: () -> Unit
) {
    val existingSchedule by assessmentViewModel.getSchedule(assessment.id)
        .collectAsState(initial = null)

    var examTime by remember { mutableStateOf(existingSchedule?.examTime ?: "09:00 AM - 10:30 AM") }
    var roomNumber by remember { mutableStateOf(existingSchedule?.roomNumber ?: "Room 201") }
    var supervisor by remember { mutableStateOf(existingSchedule?.supervisor ?: "Tr. U Ba Mg") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Examination Room", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Assessment: ${assessment.assessmentName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Grade: ${assessment.grade} • Subject: ${assessment.subjectName}", fontSize = 11.sp, color = Color.Gray)

                HorizontalDivider()

                OutlinedTextField(
                    value = examTime,
                    onValueChange = { examTime = it },
                    label = { Text("Exam Time / Duration", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room / Hall Number", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = supervisor,
                    onValueChange = { supervisor = it },
                    label = { Text("Invigilator / Supervisor Teacher", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val schedule = AssessmentScheduleEntity(
                        id = existingSchedule?.id ?: 0L,
                        assessmentId = assessment.id,
                        examTime = examTime,
                        roomNumber = roomNumber,
                        supervisor = supervisor
                    )
                    assessmentViewModel.saveSchedule(schedule)
                    onDismiss()
                }
            ) {
                Text("Save Schedule", fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun AssessmentDetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
private fun AssessmentDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
