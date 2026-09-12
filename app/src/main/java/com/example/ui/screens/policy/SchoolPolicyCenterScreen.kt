package com.example.ui.screens.policy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolPolicyCenterScreen(
    policyViewModel: SchoolPolicyViewModel,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAuthorized = currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ADMIN

    if (!isAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Access Restricted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "School Policy Center is restricted to Administrators only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        "Grades & Classes",
        "Subjects",
        "Grading Policy",
        "Role Permissions",
        "School Info"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            Column {
                Text(
                    text = "School Policy Center",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
                Text(
                    text = "Admin Configuration Engine",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Scrollable Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Contents
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> GradeConfigTab(policyViewModel)
                1 -> SubjectConfigTab(policyViewModel)
                2 -> GradingPolicyTab(policyViewModel)
                3 -> PermissionConfigTab(authViewModel)
                4 -> SchoolInfoTab(policyViewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Grade Configuration Tab
// -------------------------------------------------------------
@Composable
fun GradeConfigTab(viewModel: SchoolPolicyViewModel) {
    val grades by viewModel.grades.collectAsState()
    val classes by viewModel.classes.collectAsState()

    var showAddGradeDialog by remember { mutableStateOf(false) }
    var showAddClassDialogForGrade by remember { mutableStateOf<GradeEntity?>(null) }
    var showEditClassDialogForClass by remember { mutableStateOf<SchoolClassEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grade Structure (KG, G1 - G12)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(onClick = { showAddGradeDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Grade")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(grades, key = { it.id }) { grade ->
                val gradeClasses = classes.filter { it.gradeId == grade.id }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = grade.gradeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Column {
                                    Text(text = grade.educationLevel.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "Template: ${grade.reportCardTemplate}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row {
                                OutlinedButton(
                                    onClick = { showAddClassDialogForGrade = grade },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ Class", fontSize = 11.sp)
                                }
                                IconButton(onClick = { viewModel.deleteGrade(grade.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Classes:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (gradeClasses.isEmpty()) {
                                Text("No classes created", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                gradeClasses.forEach { cls ->
                                    InputChip(
                                        selected = true,
                                        onClick = { showEditClassDialogForClass = cls },
                                        label = { Text("Class ${cls.className}") },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.deleteClass(cls.id) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Delete Class",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGradeDialog) {
        var name by remember { mutableStateOf("") }
        var level by remember { mutableStateOf(EducationLevel.PRIMARY) }

        AlertDialog(
            onDismissRequest = { showAddGradeDialog = false },
            title = { Text("Add Grade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Grade Name (e.g. G10, G13)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Education Level:", fontWeight = FontWeight.Bold)
                    EducationLevel.values().forEach { lvl ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = level == lvl,
                                onClick = { level = lvl }
                            )
                            Text(lvl.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addGrade(name, level, "Standard")
                        showAddGradeDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddGradeDialog = false }) { Text("Cancel") }
            }
        )
    }

    showAddClassDialogForGrade?.let { grade ->
        var className by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddClassDialogForGrade = null },
            title = { Text("Add Class to ${grade.gradeName}") },
            text = {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class Name (e.g., 10-A, 10-B, A, B...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (className.isNotBlank()) {
                        viewModel.addClass(grade.id, className, 40)
                        showAddClassDialogForGrade = null
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddClassDialogForGrade = null }) { Text("Cancel") }
            }
        )
    }

    showEditClassDialogForClass?.let { cls ->
        var editedName by remember { mutableStateOf(cls.className) }
        var editedCapacity by remember { mutableStateOf(cls.capacity.toString()) }
        AlertDialog(
            onDismissRequest = { showEditClassDialogForClass = null },
            title = { Text("Edit Class") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Class Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedCapacity,
                        onValueChange = { editedCapacity = it },
                        label = { Text("Capacity") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editedName.isNotBlank()) {
                        val cap = editedCapacity.toIntOrNull() ?: cls.capacity
                        viewModel.updateClass(cls.copy(className = editedName, capacity = cap))
                        showEditClassDialogForClass = null
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditClassDialogForClass = null }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// 2. Subject Configuration Tab (Compact List/Table with Edit & Category)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectConfigTab(viewModel: SchoolPolicyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var selectedLevelFilter by remember { mutableStateOf<EducationLevel?>(null) }

    val filteredSubjects = subjects.filter { subj ->
        selectedLevelFilter == null || subj.educationLevel == selectedLevelFilter
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header and Add Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Subject Management", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Assigned Academic & Additional subjects (${filteredSubjects.size})", fontSize = 11.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddSubjectDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Subject", fontSize = 12.sp)
            }
        }

        // Compact Level Filter Chips (Horizontally Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedLevelFilter == null,
                onClick = { selectedLevelFilter = null },
                label = { Text("All Levels", fontSize = 11.sp) },
                modifier = Modifier.height(28.dp)
            )
            EducationLevel.values().forEach { level ->
                val shortLabel = when (level) {
                    EducationLevel.KINDERGARTEN -> "KG"
                    EducationLevel.PRIMARY -> "Primary (G1-5)"
                    EducationLevel.SECONDARY -> "Secondary (G6-9)"
                    EducationLevel.HIGH_SCHOOL -> "High School (G10-12)"
                }
                FilterChip(
                    selected = selectedLevelFilter == level,
                    onClick = { selectedLevelFilter = level },
                    label = { Text(shortLabel, fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Compact Subject Table / List Header
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subject Name", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                Text("Category", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.4f))
                Text("Target Level", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                Text("Status", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                Text("Actions", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(68.dp))
            }
        }

        // List View
        if (filteredSubjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No subjects found for selected level.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filteredSubjects, key = { it.id }) { subj ->
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (subj.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Name & Track
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(subj.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (subj.subTrack.isNotEmpty()) {
                                    Text(subj.subTrack, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Category Label (Academic Subject / Additional Subject)
                            Box(modifier = Modifier.weight(1.4f)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (subj.category == SubjectCategory.ACADEMIC)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = if (subj.category == SubjectCategory.ACADEMIC) "Academic Subject" else "Additional Subject",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Level
                            Text(
                                text = subj.educationLevel.displayName,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.weight(1.2f)
                            )

                            // Status Switch
                            Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = subj.isEnabled,
                                    onCheckedChange = { viewModel.toggleSubjectEnabled(subj) },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            // Actions (Edit / Delete)
                            Row(modifier = Modifier.width(68.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { subjectToEdit = subj },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { subjectToDelete = subj },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(SubjectCategory.ACADEMIC) }
        var level by remember { mutableStateOf(EducationLevel.PRIMARY) }
        var track by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Configure New Subject", fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subject Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = category == SubjectCategory.ACADEMIC,
                            onClick = { category = SubjectCategory.ACADEMIC },
                            label = { Text("Academic Subject", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = category == SubjectCategory.ADDITIONAL,
                            onClick = { category = SubjectCategory.ADDITIONAL },
                            label = { Text("Additional Subject", fontSize = 11.sp) }
                        )
                    }

                    Text("Target Education Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EducationLevel.values().forEach { lvl ->
                            val shortLabel = when (lvl) {
                                EducationLevel.KINDERGARTEN -> "KG"
                                EducationLevel.PRIMARY -> "Primary"
                                EducationLevel.SECONDARY -> "Secondary"
                                EducationLevel.HIGH_SCHOOL -> "High School"
                            }
                            FilterChip(
                                selected = level == lvl,
                                onClick = { level = lvl },
                                label = { Text(shortLabel, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (level == EducationLevel.HIGH_SCHOOL) {
                        var trackExpanded by remember { mutableStateOf(false) }
                        val streamOptions = listOf("All Streams (Common)", "STEAMS-1", "STEAMS-2")
                        ExposedDropdownMenuBox(
                            expanded = trackExpanded,
                            onExpandedChange = { trackExpanded = !trackExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = when (track) {
                                    "STEAMS-1" -> "STEAMS-1"
                                    "STEAMS-2" -> "STEAMS-2"
                                    else -> "All Streams (Common)"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("High School Stream *", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = trackExpanded,
                                onDismissRequest = { trackExpanded = false }
                            ) {
                                streamOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, fontSize = 12.sp) },
                                        onClick = {
                                            track = when (opt) {
                                                "STEAMS-1" -> "STEAMS-1"
                                                "STEAMS-2" -> "STEAMS-2"
                                                else -> ""
                                            }
                                            trackExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addSubject(name, category, level, track, isCustom = true)
                        showAddSubjectDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Subject Dialog
    subjectToEdit?.let { currentSubj ->
        var editName by remember { mutableStateOf(currentSubj.name) }
        var editCategory by remember { mutableStateOf(currentSubj.category) }
        var editLevel by remember { mutableStateOf(currentSubj.educationLevel) }
        var editTrack by remember { mutableStateOf(currentSubj.subTrack) }

        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Edit Subject: ${currentSubj.name}", fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subject Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = editCategory == SubjectCategory.ACADEMIC,
                            onClick = { editCategory = SubjectCategory.ACADEMIC },
                            label = { Text("Academic Subject", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = editCategory == SubjectCategory.ADDITIONAL,
                            onClick = { editCategory = SubjectCategory.ADDITIONAL },
                            label = { Text("Additional Subject", fontSize = 11.sp) }
                        )
                    }

                    Text("Education Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EducationLevel.values().forEach { lvl ->
                            val shortLabel = when (lvl) {
                                EducationLevel.KINDERGARTEN -> "KG"
                                EducationLevel.PRIMARY -> "Primary"
                                EducationLevel.SECONDARY -> "Secondary"
                                EducationLevel.HIGH_SCHOOL -> "High School"
                            }
                            FilterChip(
                                selected = editLevel == lvl,
                                onClick = { editLevel = lvl },
                                label = { Text(shortLabel, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (editLevel == EducationLevel.HIGH_SCHOOL) {
                        var trackExpanded by remember { mutableStateOf(false) }
                        val streamOptions = listOf("All Streams (Common)", "STEAMS-1", "STEAMS-2")
                        ExposedDropdownMenuBox(
                            expanded = trackExpanded,
                            onExpandedChange = { trackExpanded = !trackExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = when (editTrack) {
                                    "STEAMS-1" -> "STEAMS-1"
                                    "STEAMS-2" -> "STEAMS-2"
                                    else -> "All Streams (Common)"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("High School Stream", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = trackExpanded,
                                onDismissRequest = { trackExpanded = false }
                            ) {
                                streamOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, fontSize = 12.sp) },
                                        onClick = {
                                            editTrack = when (opt) {
                                                "STEAMS-1" -> "STEAMS-1"
                                                "STEAMS-2" -> "STEAMS-2"
                                                else -> ""
                                            }
                                            trackExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateSubject(
                            currentSubj.copy(
                                name = editName,
                                category = editCategory,
                                educationLevel = editLevel,
                                subTrack = editTrack
                            )
                        )
                        subjectToEdit = null
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                OutlinedButton(onClick = { subjectToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Subject Confirmation Dialog
    subjectToDelete?.let { subj ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject") },
            text = {
                Text("Are you sure you want to delete '${subj.name}' (${subj.educationLevel.displayName})? This will remove the subject from active academic policies.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subj.id)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 3. Assessment & Custom Exams Tab (Single Source of Truth Focus)
// -------------------------------------------------------------
@Composable
fun AssessmentAndExamTab(viewModel: SchoolPolicyViewModel) {
    val assessmentTypes by viewModel.assessmentTypes.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Single Source of Truth", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Assessment Management module is the sole creator and controller for exams, scheduling, marks entry, ranking, and report card synchronization.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        item {
            Text("Configured Policy Assessment Types (${assessmentTypes.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(assessmentTypes, key = { it.id }) { type ->
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(type.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Level: ${type.educationLevel.displayName}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Active", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Grading Policy Tab
// -------------------------------------------------------------
@Composable
fun GradingPolicyTab(viewModel: SchoolPolicyViewModel) {
    val gradingPolicies by viewModel.gradingPolicies.collectAsState()
    var policyToEdit by remember { mutableStateOf<GradingPolicyEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Grading & Distinction Policy Matrix", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Maximum Mark (Default 100), Pass Mark (Default 40), Level & Subject Distinction Thresholds.", fontSize = 11.sp, color = Color.Gray)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(gradingPolicies, key = { it.id }) { policy ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${policy.educationLevel.displayName} • ${policy.subjectName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Max: ${policy.maxMark} | Pass: ${policy.passMark} | Distinction: ${policy.distinctionMark} marks", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = { policyToEdit = policy }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Policy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    policyToEdit?.let { policy ->
        var maxMark by remember { mutableStateOf(policy.maxMark.toString()) }
        var passMark by remember { mutableStateOf(policy.passMark.toString()) }
        var distMark by remember { mutableStateOf(policy.distinctionMark.toString()) }

        AlertDialog(
            onDismissRequest = { policyToEdit = null },
            title = { Text("Edit Policy: ${policy.subjectName}", fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxMark,
                        onValueChange = { maxMark = it },
                        label = { Text("Maximum Mark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passMark,
                        onValueChange = { passMark = it },
                        label = { Text("Pass Mark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = distMark,
                        onValueChange = { distMark = it },
                        label = { Text("Distinction Threshold") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateGradingPolicy(
                        policy.copy(
                            maxMark = maxMark.toIntOrNull() ?: 100,
                            passMark = passMark.toIntOrNull() ?: 40,
                            distinctionMark = distMark.toIntOrNull() ?: 75
                        )
                    )
                    policyToEdit = null
                }) { Text("Update") }
            },
            dismissButton = {
                OutlinedButton(onClick = { policyToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// 5. Permission Configuration Tab
// -------------------------------------------------------------
@Composable
fun PermissionConfigTab(authViewModel: AuthViewModel) {
    val permissions by authViewModel.rolePermissions.collectAsState()
    var selectedRole by remember { mutableStateOf(UserRole.ADMIN) }

    val rolePermissionsFiltered = permissions.filter { it.role == selectedRole }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dynamic Role & Permission Matrix", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Configure permissions for each user role.", fontSize = 11.sp, color = Color.Gray)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            UserRole.values().forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    label = { Text(role.displayName, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).height(30.dp)
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(rolePermissionsFiltered, key = { "${it.role}_${it.permissionKey}" }) { perm ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(perm.permissionKey, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Switch(
                            checked = perm.isAllowed,
                            onCheckedChange = { allowed ->
                                authViewModel.updatePermission(perm.copy(isAllowed = allowed))
                            },
                            enabled = selectedRole != UserRole.SUPER_ADMIN,
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. School Info Tab
// -------------------------------------------------------------
@Composable
fun SchoolInfoTab(viewModel: SchoolPolicyViewModel) {
    val settings by viewModel.schoolSettings.collectAsState()

    var name by remember(settings) { mutableStateOf(settings?.schoolName ?: "Hein Chan Myae") }
    var year by remember(settings) { mutableStateOf(settings?.academicYear ?: "2026-2027") }
    var phone by remember(settings) { mutableStateOf(settings?.contactPhone ?: "") }
    var email by remember(settings) { mutableStateOf(settings?.email ?: "") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("School Information & Branding", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("School Name", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Academic Year", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Contact Phone", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("School Email", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                android.util.Log.d("AcademicYearDebug", "UI_CLICK: Save School Settings clicked with year='$year'")
                viewModel.updateSchoolSettings(name, year, phone, email, address)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save School Settings", fontSize = 12.sp)
        }
    }
}
