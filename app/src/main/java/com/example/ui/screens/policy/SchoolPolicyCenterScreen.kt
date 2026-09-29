package com.example.ui.screens.policy

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
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
import com.example.data.policy.ExamQuestionBlueprintHelper
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
        "Exam Blueprints (G10-G12)"
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
                3 -> ExamBlueprintsTab(policyViewModel)
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Grade & Class Structure", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Manage KG, Primary (G1-G5), Secondary (G6-G9) & High School (G10-G12)", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddGradeDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Grade", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add New Academic Grade", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Grade Code / Name (e.g. G1, G10)") },
                        placeholder = { Text("e.g. G1, G7, G11") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text("Education Level:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EducationLevel.values().forEach { lvl ->
                            val isSelected = level == lvl
                            FilterChip(
                                selected = isSelected,
                                onClick = { level = lvl },
                                label = { Text(lvl.displayName, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addGrade(name.trim(), level, "Standard")
                            showAddGradeDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = name.isNotBlank()
                ) {
                    Text("Create Grade")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddGradeDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Subject Management", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Assigned Academic & Additional subjects (${filteredSubjects.size})", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddSubjectDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Subject", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add Academic Subject", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name *") },
                        placeholder = { Text("e.g. Mathematics, Myanmar, Chemistry") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text("Subject Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = category == SubjectCategory.ACADEMIC,
                            onClick = { category = SubjectCategory.ACADEMIC },
                            label = { Text("Academic Subject", fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = category == SubjectCategory.ADDITIONAL,
                            onClick = { category = SubjectCategory.ADDITIONAL },
                            label = { Text("Additional Subject", fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Text("Target Education Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EducationLevel.values().forEach { lvl ->
                            val shortLabel = when (lvl) {
                                EducationLevel.KINDERGARTEN -> "KG"
                                EducationLevel.PRIMARY -> "Primary (G1-5)"
                                EducationLevel.SECONDARY -> "Secondary (G6-9)"
                                EducationLevel.HIGH_SCHOOL -> "High School (G10-12)"
                            }
                            val isSelected = level == lvl
                            FilterChip(
                                selected = isSelected,
                                onClick = { level = lvl },
                                label = { Text(shortLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp)
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
                                shape = RoundedCornerShape(8.dp),
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
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addSubject(name.trim(), category, level, track, isCustom = true)
                            showAddSubjectDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = name.isNotBlank()
                ) {
                    Text("Create Subject")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddSubjectDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
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
// 4. Exam Question Blueprints Tab (G10 - G12 Admin Configurable)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamBlueprintsTab(viewModel: SchoolPolicyViewModel) {
    val selectedGrade by viewModel.selectedBlueprintGrade.collectAsState()
    val blueprints by viewModel.blueprintsForSelectedGrade.collectAsState()

    var blueprintToEdit by remember { mutableStateOf<ExamQuestionBlueprintEntity?>(null) }
    var showAddBlueprintDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val highSchoolGrades = listOf("G10", "G11", "G12")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header & Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "မေးခွန်းနံပါတ် သတ်မှတ်ချက်များ (Exam Question Blueprints)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Grade 10, 11, 12 ဘာသာရပ်အလိုက် မေးခွန်းနံပါတ်၊ အကြောင်းအရာနှင့် အမှတ်အများဆုံးကို လိုသလို ပြင်ဆင်သတ်မှတ်နိုင်သည်",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Preset", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showAddBlueprintDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Subject", fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Grade Selector Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Grade ရွေးချယ်ရန်:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    highSchoolGrades.forEach { g ->
                        FilterChip(
                            selected = selectedGrade.equals(g, ignoreCase = true),
                            onClick = { viewModel.selectedBlueprintGrade.value = g },
                            label = { Text(g, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            leadingIcon = if (selectedGrade.equals(g, ignoreCase = true)) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // List of Blueprints for Selected Grade
        if (blueprints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$selectedGrade အတွက် မေးခွန်းပုံစံများ မရှိသေးပါ",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Button(
                        onClick = { viewModel.resetToDefaultBlueprints(selectedGrade) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("မြန်မာသင်ရိုးစံ မေးခွန်းပုံစံများ ထည့်သွင်းမည် (Seed Defaults)")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(blueprints, key = { it.id }) { blueprint ->
                    val questions = remember(blueprint.questionsJson) {
                        ExamQuestionBlueprintHelper.parseQuestions(blueprint.questionsJson)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Subject Title & Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = blueprint.gradeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = blueprint.subjectName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "စုစုပေါင်း: ${blueprint.totalMarks} မှတ်",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { blueprintToEdit = blueprint },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteExamBlueprint(blueprint.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Question Items List
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                questions.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = item.qNo,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = item.title.ifBlank { "မေးခွန်း ${item.qNo}" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "${item.maxMark.toInt()} မှတ်",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Edit Blueprint Questions
    blueprintToEdit?.let { bp ->
        EditBlueprintQuestionsDialog(
            blueprint = bp,
            onDismiss = { blueprintToEdit = null },
            onSave = { updatedQuestions ->
                viewModel.saveExamBlueprint(bp.gradeName, bp.subjectName, updatedQuestions)
                blueprintToEdit = null
            }
        )
    }

    // Dialog: Add Custom Blueprint
    if (showAddBlueprintDialog) {
        AddCustomBlueprintDialog(
            gradeName = selectedGrade,
            onDismiss = { showAddBlueprintDialog = false },
            onAdd = { subjectName, questions ->
                viewModel.saveExamBlueprint(selectedGrade, subjectName, questions)
                showAddBlueprintDialog = false
            }
        )
    }

    // Dialog: Confirm Reset to Defaults
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Standard Myanmar Presets") },
            text = { Text("$selectedGrade အတွက် စံမေးခွန်းပုံစံများကို နဂိုမူလအတိုင်း ပြန်လည်သတ်မှတ်မည် သေချာပါသလား?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaultBlueprints(selectedGrade)
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("သေချာပါသည် (Reset)") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) { Text("မလုပ်တော့ပါ") }
            }
        )
    }
}

@Composable
private fun EditBlueprintQuestionsDialog(
    blueprint: ExamQuestionBlueprintEntity,
    onDismiss: () -> Unit,
    onSave: (List<QuestionBlueprintItem>) -> Unit
) {
    val initialQuestions = remember(blueprint) {
        val parsed = ExamQuestionBlueprintHelper.parseQuestions(blueprint.questionsJson)
        if (parsed.isNotEmpty()) parsed else ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(blueprint.subjectName)
    }

    var questions by remember { mutableStateOf(initialQuestions) }

    val totalMaxMark = questions.sumOf { it.maxMark }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${blueprint.gradeName} - ${blueprint.subjectName} မေးခွန်းများ ပြင်ဆင်ခြင်း",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "စုစုပေါင်းအမှတ်: ${totalMaxMark.toInt()} မှတ်",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("မေးခွန်းအလိုက် အချက်အလက်များ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = {
                            val nextNo = "Q${questions.size + 1}"
                            questions = questions + QuestionBlueprintItem(nextNo, "Section ${questions.size + 1}", 20.0)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ မေးခွန်းအသစ်ထည့်", fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(questions.size) { idx ->
                        val item = questions[idx]
                        var qNoText by remember(item.qNo) { mutableStateOf(item.qNo) }
                        var titleText by remember(item.title) { mutableStateOf(item.title) }
                        var markText by remember(item.maxMark) { mutableStateOf(item.maxMark.toInt().toString()) }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Q No
                                OutlinedTextField(
                                    value = qNoText,
                                    onValueChange = {
                                        qNoText = it
                                        questions = questions.toMutableList().also { list ->
                                            list[idx] = list[idx].copy(qNo = it)
                                        }
                                    },
                                    label = { Text("No", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.width(60.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )

                                // Title
                                OutlinedTextField(
                                    value = titleText,
                                    onValueChange = {
                                        titleText = it
                                        questions = questions.toMutableList().also { list ->
                                            list[idx] = list[idx].copy(title = it)
                                        }
                                    },
                                    label = { Text("Title / Section", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                )

                                // Max Mark
                                OutlinedTextField(
                                    value = markText,
                                    onValueChange = {
                                        markText = it
                                        val num = it.toDoubleOrNull() ?: 0.0
                                        questions = questions.toMutableList().also { list ->
                                            list[idx] = list[idx].copy(maxMark = num)
                                        }
                                    },
                                    label = { Text("Max", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.width(65.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )

                                // Delete
                                IconButton(
                                    onClick = {
                                        questions = questions.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(questions) },
                enabled = questions.isNotEmpty()
            ) {
                Text("သိမ်းဆည်းမည် (Save)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ") }
        }
    )
}

@Composable
private fun AddCustomBlueprintDialog(
    gradeName: String,
    onDismiss: () -> Unit,
    onAdd: (String, List<QuestionBlueprintItem>) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    val defaultQuestions = remember(subjectName) {
        if (subjectName.isNotBlank()) ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(subjectName)
        else listOf(
            QuestionBlueprintItem("Q1", "Section A", 20.0),
            QuestionBlueprintItem("Q2", "Section B", 20.0),
            QuestionBlueprintItem("Q3", "Section C", 20.0),
            QuestionBlueprintItem("Q4", "Section D", 20.0),
            QuestionBlueprintItem("Q5", "Section E", 20.0)
        )
    }

    var questions by remember { mutableStateOf(defaultQuestions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$gradeName အတွက် ဘာသာရပ်မေးခွန်းပုံစံ အသစ်ထည့်ခြင်း", fontSize = 15.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = {
                        subjectName = it
                        questions = ExamQuestionBlueprintHelper.getDefaultQuestionsForSubject(it)
                    },
                    label = { Text("ဘာသာရပ် အမည် (Subject Name)") },
                    placeholder = { Text("e.g. Physics, History, Computing") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "သတ်မှတ်ထားသော မေးခွန်းများ (${questions.size} ခု - စုစုပေါင်း ${questions.sumOf { it.maxMark }.toInt()} မှတ်)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isNotBlank()) {
                        onAdd(subjectName.trim(), questions)
                    }
                },
                enabled = subjectName.isNotBlank()
            ) {
                Text("ထည့်သွင်းမည် (Add)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ") }
        }
    )
}


