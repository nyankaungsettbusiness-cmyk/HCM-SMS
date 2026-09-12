package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TeacherEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel
import com.example.ui.viewmodel.TeacherViewModel
import com.example.data.policy.SchoolPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManagementScreen(
    teacherViewModel: TeacherViewModel,
    currentUser: UserEntity?,
    policyViewModel: SchoolPolicyViewModel,
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val teachers by teacherViewModel.teachersList.collectAsState()
    val searchQuery by teacherViewModel.searchQuery.collectAsState()
    val statusFilter by teacherViewModel.statusFilter.collectAsState()
    val gradeFilter by teacherViewModel.gradeFilter.collectAsState()
    val toastMessage by teacherViewModel.toastMessage.collectAsState()

    val allUsers by (authViewModel?.allUsers?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    var showAddEditDialog by remember { mutableStateOf(false) }
    var teacherToEdit by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherToViewDetail by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherToDelete by remember { mutableStateOf<TeacherEntity?>(null) }

    val dbGrades by policyViewModel.grades.collectAsState()

    val statusOptions = listOf("All", "Active", "On Leave", "Resigned")
    val gradeOptions = listOf("All Grades") + (if (dbGrades.isNotEmpty()) dbGrades.map { it.gradeName } else SchoolPolicy.VALID_GRADES)

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            teacherViewModel.clearToastMessage()
        }
    }

    val canEditTeachers = currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ADMIN
    val canDeleteTeachers = currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ADMIN

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (canEditTeachers) {
                FloatingActionButton(
                    onClick = {
                        teacherToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Teacher", modifier = Modifier.size(22.dp))
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
            // 1. Compact Header Bar
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
                        text = "Teachers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${teachers.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "• Faculty Roster",
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
                            android.widget.Toast.makeText(localContext, "Syncing Teachers...", android.widget.Toast.LENGTH_SHORT).show()
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

                    if (canEditTeachers) {
                        FilledTonalIconButton(
                            onClick = {
                                teacherToEdit = null
                                showAddEditDialog = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Add Teacher",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Full-Width Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { teacherViewModel.setSearchQuery(it) },
                placeholder = { Text("Search name, ID, subject, grade...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { teacherViewModel.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeacherFilterDropdownChip(
                    label = "Grade: $gradeFilter",
                    options = gradeOptions,
                    selectedOption = gradeFilter,
                    onOptionSelected = { teacherViewModel.setGradeFilter(it) }
                )
                TeacherFilterDropdownChip(
                    label = "Status: $statusFilter",
                    options = statusOptions,
                    selectedOption = statusFilter,
                    onOptionSelected = { teacherViewModel.setStatusFilter(it) }
                )
                if (statusFilter != "All" || gradeFilter != "All Grades" || searchQuery.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            teacherViewModel.setSearchQuery("")
                            teacherViewModel.setStatusFilter("All")
                            teacherViewModel.setGradeFilter("All Grades")
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

            // 3. Dynamic Mobile Teacher List
            if (teachers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No teachers found matching current search/filters.",
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
                    itemsIndexed(teachers, key = { _, t -> t.id }) { _, teacher ->
                        ModernTeacherCard(
                            teacher = teacher,
                            canEdit = canEditTeachers,
                            canDelete = canDeleteTeachers,
                            onViewDetail = { teacherToViewDetail = teacher },
                            onEdit = {
                                teacherToEdit = teacher
                                showAddEditDialog = true
                            },
                            onDelete = { teacherToDelete = teacher }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Teacher Dialog
    if (showAddEditDialog) {
        AddEditTeacherDialog(
            teacher = teacherToEdit,
            allUsers = allUsers,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedTeacher, createAccountRequested, username, password, mustChange ->
                teacherViewModel.saveTeacher(updatedTeacher, currentUser) {
                    if (createAccountRequested && authViewModel != null && username.isNotBlank()) {
                        authViewModel.createUser(
                            username = username.trim(),
                            fullName = updatedTeacher.fullName,
                            role = UserRole.TEACHER,
                            email = updatedTeacher.email.ifBlank { "${username.trim()}@hcm-school.edu.mm" },
                            phone = updatedTeacher.phone,
                            linkedTeacherName = "${updatedTeacher.fullName} (${updatedTeacher.teacherCode})",
                            initialPassword = password.ifBlank { "123456" },
                            mustChangePassword = mustChange
                        ) {
                            Toast.makeText(context, "Login account created for @${username.trim()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showAddEditDialog = false
                }
            }
        )
    }

    // View Teacher Detail Dialog
    teacherToViewDetail?.let { teacher ->
        val linkedUser = allUsers.find {
            it.linkedTeacherName.contains(teacher.teacherCode, ignoreCase = true) ||
            (it.fullName.equals(teacher.fullName, ignoreCase = true) && it.role == UserRole.TEACHER)
        }

        TeacherDetailDialog(
            teacher = teacher,
            linkedUser = linkedUser,
            canEdit = canEditTeachers,
            canDelete = canDeleteTeachers,
            onEdit = {
                teacherToEdit = teacher
                teacherToViewDetail = null
                showAddEditDialog = true
            },
            onDelete = {
                teacherToDelete = teacher
                teacherToViewDetail = null
            },
            onDismiss = { teacherToViewDetail = null }
        )
    }

    // Delete Teacher Confirmation Dialog
    teacherToDelete?.let { teacher ->
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text("Delete Teacher Profile?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${teacher.fullName} (${teacher.teacherCode})? This action cannot be undone.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        if (canDeleteTeachers) {
                            teacherViewModel.deleteTeacher(teacher, currentUser)
                        } else {
                            Toast.makeText(context, "Permission Denied: Only Administrators can delete teachers.", Toast.LENGTH_SHORT).show()
                        }
                        teacherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { teacherToDelete = null }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun ModernTeacherCard(
    teacher: TeacherEntity,
    canEdit: Boolean = true,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Code badge, Grades badge, Status badge
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
                            text = teacher.teacherCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (teacher.assignedGrade.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Grades: ${teacher.assignedGrade}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Status Badge
                val (bgColor, textColor) = when (teacher.employmentStatus.lowercase()) {
                    "active" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    "on leave" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                    else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor
                ) {
                    Text(
                        text = teacher.employmentStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Row 2: Avatar + Name + Contact + Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teacher.fullName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacher.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Subjects: ${teacher.assignedSubjects.ifBlank { "Unassigned" }}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val contactInfo = listOfNotNull(
                        teacher.phone.takeIf { it.isNotBlank() },
                        teacher.email.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (contactInfo.isNotBlank()) {
                        Text(
                            text = contactInfo,
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Actions (Touch friendly min 38dp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (canEdit) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Teacher",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Teacher",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTeacherDialog(
    teacher: TeacherEntity?,
    allUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (teacher: TeacherEntity, createAccount: Boolean, username: String, pass: String, mustChange: Boolean) -> Unit
) {
    var teacherCode by remember { mutableStateOf(teacher?.teacherCode ?: "TCH-2025-${(100..999).random()}") }
    var fullName by remember { mutableStateOf(teacher?.fullName ?: "") }
    var phone by remember { mutableStateOf(teacher?.phone ?: "") }
    var email by remember { mutableStateOf(teacher?.email ?: "") }
    var address by remember { mutableStateOf(teacher?.address ?: "") }
    var assignedGrade by remember { mutableStateOf(teacher?.assignedGrade ?: "G5, G8") }
    var assignedClass by remember { mutableStateOf(teacher?.assignedClass ?: "A") }
    var assignedSubjects by remember { mutableStateOf(teacher?.assignedSubjects ?: "Mathematics, Science") }
    var employmentStatus by remember { mutableStateOf(teacher?.employmentStatus ?: "Active") }

    // Account Creation Options (for new teacher)
    var createLoginAccount by remember { mutableStateOf(false) }
    var accountUsername by remember { mutableStateOf("") }
    var accountPassword by remember { mutableStateOf("123456") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mustChangePassword by remember { mutableStateOf(true) }

    val availableGrades = remember { SchoolPolicy.VALID_GRADES }
    val availableSubjects = remember { listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies", "Physics", "Chemistry", "Biology", "Economics", "Geography", "History", "ICT", "Physical Education") }

    var selectedGradesSet by remember {
        mutableStateOf(assignedGrade.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet())
    }
    var selectedSubjectsSet by remember {
        mutableStateOf(assignedSubjects.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet())
    }

    val usernameDuplicate = remember(accountUsername, allUsers) {
        accountUsername.isNotBlank() && allUsers.any { it.username.equals(accountUsername.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (teacher == null) "Add New Teacher" else "Edit Teacher Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = teacherCode,
                        onValueChange = { teacherCode = it },
                        label = { Text("Teacher Code / ID *", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (accountUsername.isBlank() && it.isNotBlank()) {
                                accountUsername = it.lowercase().replace(" ", "").replace(".", "")
                            }
                        },
                        label = { Text("Full Name *", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Assigned Grades (Select Multiple):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        OptInFlowRow(availableGrades, selectedGradesSet) { grade ->
                            val newSet = if (selectedGradesSet.contains(grade)) selectedGradesSet - grade else selectedGradesSet + grade
                            selectedGradesSet = newSet
                            assignedGrade = newSet.joinToString(", ")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = assignedClass,
                        onValueChange = { assignedClass = it },
                        label = { Text("Class Rooms / Sections (e.g. A, B)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Assigned Subjects (Select Multiple):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        OptInFlowRow(availableSubjects, selectedSubjectsSet) { subject ->
                            val newSet = if (selectedSubjectsSet.contains(subject)) selectedSubjectsSet - subject else selectedSubjectsSet + subject
                            selectedSubjectsSet = newSet
                            assignedSubjects = newSet.joinToString(", ")
                        }
                    }
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

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Employment Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Active", "On Leave", "Resigned").forEach { st ->
                                FilterChip(
                                    selected = employmentStatus == st,
                                    onClick = { employmentStatus = st },
                                    label = { Text(st, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Optional Login Account Creation (For New Teachers)
                if (teacher == null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("Create System Login Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Checkbox(
                                        checked = createLoginAccount,
                                        onCheckedChange = { createLoginAccount = it }
                                    )
                                }

                                if (createLoginAccount) {
                                    OutlinedTextField(
                                        value = accountUsername,
                                        onValueChange = { accountUsername = it },
                                        label = { Text("Login Username *", fontSize = 11.sp) },
                                        isError = usernameDuplicate,
                                        supportingText = {
                                            if (usernameDuplicate) Text("Username already taken!", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )

                                    OutlinedTextField(
                                        value = accountPassword,
                                        onValueChange = { accountPassword = it },
                                        label = { Text("Initial Password *", fontSize = 11.sp) },
                                        singleLine = true,
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(24.dp)) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle Password",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Role: TEACHER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = mustChangePassword, onCheckedChange = { mustChangePassword = it })
                                            Text("Force pwd change", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && teacherCode.isNotBlank()) {
                        val updated = (teacher ?: TeacherEntity(teacherCode = "", fullName = "")).copy(
                            teacherCode = teacherCode,
                            fullName = fullName,
                            phone = phone,
                            email = email,
                            address = address,
                            assignedGrade = assignedGrade,
                            assignedClass = assignedClass,
                            assignedSubjects = assignedSubjects,
                            employmentStatus = employmentStatus
                        )
                        onSave(updated, createLoginAccount, accountUsername, accountPassword, mustChangePassword)
                    }
                }
            ) {
                Text(if (teacher == null) "Create Teacher" else "Save Changes", fontSize = 12.sp)
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
fun TeacherDetailDialog(
    teacher: TeacherEntity,
    linkedUser: UserEntity?,
    canEdit: Boolean = true,
    canDelete: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Profile", "Login Account", "Grades", "Subjects")

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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacher.fullName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(teacher.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${teacher.teacherCode} • ${teacher.employmentStatus}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row {
                    if (canEdit) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
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
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
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
                    0 -> TeacherProfileTab(teacher)
                    1 -> TeacherLoginAccountTab(linkedUser, teacher)
                    2 -> TeacherAssignedGradesTab(teacher)
                    3 -> TeacherAssignedSubjectsTab(teacher)
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
fun TeacherProfileTab(teacher: TeacherEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeacherDetailSectionCard(title = "Faculty Identification") {
            TeacherDetailRow("Teacher ID / Code", teacher.teacherCode)
            TeacherDetailRow("Full Name", teacher.fullName)
            TeacherDetailRow("Employment Status", teacher.employmentStatus)
        }

        TeacherDetailSectionCard(title = "Contact Information") {
            TeacherDetailRow("Phone Number", teacher.phone.ifBlank { "Not specified" })
            TeacherDetailRow("Email Address", teacher.email.ifBlank { "Not specified" })
            TeacherDetailRow("Residential Address", teacher.address.ifBlank { "Not specified" })
        }
    }
}

@Composable
fun TeacherLoginAccountTab(linkedUser: UserEntity?, teacher: TeacherEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeacherDetailSectionCard(title = "System Authentication Status") {
            if (linkedUser != null) {
                TeacherDetailRow("Linked Username", "@${linkedUser.username}")
                TeacherDetailRow("Assigned Role", linkedUser.role.displayName)
                TeacherDetailRow("Account Status", linkedUser.status.name)
                TeacherDetailRow("Password Reset Req", if (linkedUser.mustChangePassword) "Yes (Temp Password)" else "No")
            } else {
                Text(
                    text = "No system login account linked to ${teacher.fullName}.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "You can link or create an account in User & Role Management (Module 11).",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TeacherAssignedGradesTab(teacher: TeacherEntity) {
    val gradeList = teacher.assignedGrade.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeacherDetailSectionCard(title = "Assigned Teaching Grades (${gradeList.size})") {
            if (gradeList.isEmpty()) {
                Text("No grade levels currently assigned.", fontSize = 11.sp, color = Color.Gray)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeList.forEach { g ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(g, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        TeacherDetailSectionCard(title = "Classroom Sections") {
            TeacherDetailRow("Assigned Section/Class", teacher.assignedClass.ifBlank { "All Sections" })
        }
    }
}

@Composable
fun TeacherAssignedSubjectsTab(teacher: TeacherEntity) {
    val subjectList = teacher.assignedSubjects.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeacherDetailSectionCard(title = "Assigned Academic Subjects (${subjectList.size})") {
            if (subjectList.isEmpty()) {
                Text("No subjects currently assigned.", fontSize = 11.sp, color = Color.Gray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subjectList.forEach { subj ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(subj, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowRow(
    items: List<String>,
    selectedSet: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = selectedSet.contains(item)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(item) },
                label = { Text(item, fontSize = 10.sp) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherFilterDropdownChip(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedOption != "All" && selectedOption != "All Grades",
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
private fun TeacherDetailSectionCard(
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
private fun TeacherDetailRow(
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

