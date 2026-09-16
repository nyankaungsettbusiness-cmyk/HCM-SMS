package com.example.ui.screens.holistic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.data.policy.SchoolPolicy
import com.example.ui.components.StarRatingWidget
import com.example.ui.viewmodel.HolisticViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolisticAssessmentScreen(
    holisticViewModel: HolisticViewModel,
    currentUser: UserEntity?,
    availableAcademicYears: List<String> = emptyList(),
    policyViewModel: com.example.ui.viewmodel.SchoolPolicyViewModel,
    modifier: Modifier = Modifier
) {
    val selectedAcademicYear by holisticViewModel.academicYear.collectAsState()
    val selectedGrade by holisticViewModel.selectedGrade.collectAsState()
    val selectedClass by holisticViewModel.selectedClass.collectAsState()
    val selectedPeriod by holisticViewModel.selectedPeriod.collectAsState()
    val searchQuery by holisticViewModel.searchQuery.collectAsState()

    val activeLevel by holisticViewModel.activeEducationLevel.collectAsState()
    val adminLevel by holisticViewModel.adminConfigLevel.collectAsState()

    val filteredStudents by holisticViewModel.filteredStudents.collectAsState()
    val currentSelectedStudent by holisticViewModel.selectedStudent.collectAsState()

    val enabledHolisticCategories by holisticViewModel.enabledHolisticCategoriesForLevel.collectAsState()
    val enabledSgiCategories by holisticViewModel.enabledSgiCategories.collectAsState()

    val allHolisticCategories by holisticViewModel.allHolisticCategories.collectAsState()
    val adminHolisticCategories by holisticViewModel.adminHolisticCategories.collectAsState()
    val availablePeriods by holisticViewModel.availablePeriods.collectAsState()
    val adminAssessmentPeriods by holisticViewModel.adminAssessmentPeriods.collectAsState()

    val holisticRatings by holisticViewModel.holisticRatings.collectAsState()
    val sgiRatings by holisticViewModel.sgiRatings.collectAsState()

    val positiveComments by holisticViewModel.positiveComments.collectAsState()
    val areasForImprovement by holisticViewModel.areasForImprovement.collectAsState()
    val generalComment by holisticViewModel.generalComment.collectAsState()

    val overallHcmScore by holisticViewModel.overallHcmScore.collectAsState()
    val sgiAverageScore by holisticViewModel.sgiAverageScore.collectAsState()

    val statusMessage by holisticViewModel.statusMessage.collectAsState()

    val userRole = currentUser?.role ?: UserRole.TEACHER
    val isAdmin = userRole == UserRole.SUPER_ADMIN || userRole == UserRole.ADMIN
    val canEdit = userRole != UserRole.OFFICE_STAFF

    var activeTab by remember { mutableStateOf(0) } // 0: Rating Entry, 1: Admin Config (Admin only)
    if (!isAdmin && activeTab != 0) {
        activeTab = 0
    }
    val focusManager = LocalFocusManager.current

    // Dialog states for Admin Category Management
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var periodToEdit by remember { mutableStateOf<AssessmentPeriodEntity?>(null) }

    var showAddHolisticDialog by remember { mutableStateOf(false) }
    var holisticToEdit by remember { mutableStateOf<HolisticCategoryEntity?>(null) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }

    LaunchedEffect(currentSelectedStudent, selectedPeriod, selectedGrade, selectedClass, selectedAcademicYear) {
        isLocked = false
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarMessage = it
            holisticViewModel.clearStatusMessage()
        }
    }

    val dbGrades by policyViewModel.grades.collectAsState()
    val dbClasses by policyViewModel.classes.collectAsState()

    val academicYears = if (availableAcademicYears.isNotEmpty()) (availableAcademicYears + selectedAcademicYear).filter { it.isNotBlank() }.distinct() else listOf(selectedAcademicYear).filter { it.isNotBlank() }
    val grades = if (dbGrades.isNotEmpty()) dbGrades.map { it.gradeName } else SchoolPolicy.VALID_GRADES
    val classes = if (dbClasses.isNotEmpty()) dbClasses.map { it.className }.distinct() else listOf("A", "B", "C", "D")

    // Categories to display for HCM rating entry
    val displayCategories = if (enabledHolisticCategories.isNotEmpty()) {
        enabledHolisticCategories
    } else {
        listOf(
            HolisticCategoryEntity(id = -1L, categoryName = "Honesty", pillar = "HONESTY", maxStars = 5),
            HolisticCategoryEntity(id = -2L, categoryName = "Curiosity", pillar = "CURIOSITY", maxStars = 5),
            HolisticCategoryEntity(id = -3L, categoryName = "Mindfulness", pillar = "MINDFULNESS", maxStars = 5)
        )
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
        modifier = modifier.testTag("holistic_assessment_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // TOP SECTION: Dropdowns (Academic Year, Grade, Class, Assessment Period)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactDropdown(
                    label = "Academic Year",
                    value = selectedAcademicYear,
                    items = academicYears,
                    onItemSelected = {
                        holisticViewModel.academicYear.value = it
                        holisticViewModel.onFilterChanged()
                    },
                    modifier = Modifier.weight(1f)
                )

                CompactDropdown(
                    label = "Grade",
                    value = selectedGrade,
                    items = grades,
                    onItemSelected = {
                        holisticViewModel.selectedGrade.value = it
                        holisticViewModel.onFilterChanged()
                    },
                    modifier = Modifier.weight(1f)
                )

                CompactDropdown(
                    label = "Class",
                    value = selectedClass,
                    items = classes,
                    onItemSelected = {
                        holisticViewModel.selectedClass.value = it
                        holisticViewModel.onFilterChanged()
                    },
                    modifier = Modifier.weight(1f)
                )

                CompactDropdown(
                    label = "Period",
                    value = selectedPeriod,
                    items = availablePeriods,
                    onItemSelected = {
                        holisticViewModel.selectedPeriod.value = it
                        holisticViewModel.onFilterChanged()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // NAVIGATION TABS: Rating Entry / Admin Configuration (Admin only)
            if (isAdmin) {
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Rating Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Rating Entry", modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Admin Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Admin Config", modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (activeTab == 0) {
                // RATING ENTRY TAB
                if (currentSelectedStudent == null) {
                    // STEP 1: SHOW STUDENTS LIST ONLY
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // SEARCH BAR
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { holisticViewModel.searchQuery.value = it },
                            placeholder = { Text("Search by Student Name or Student ID...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { holisticViewModel.searchQuery.value = "" }) {
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

                        // LIST INSTRUCTION BANNER
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select a student from Grade $selectedGrade $selectedClass to enter HCM assessment ratings:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (filteredStudents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No students match '$searchQuery'" else "No students found in Grade $selectedGrade Class $selectedClass",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredStudents, key = { it.id }) { student ->
                                    StudentListItemCard(
                                        student = student,
                                        onSelect = {
                                            focusManager.clearFocus()
                                            holisticViewModel.selectStudent(student)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // STEP 2: SHOW DESIRED STUDENT'S ASSESSMENT ITEMS ONLY
                    val student = currentSelectedStudent!!
                    val studentIndex = filteredStudents.indexOfFirst { it.id == student.id }
                    val nextStudent = if (studentIndex != -1 && studentIndex < filteredStudents.size - 1) filteredStudents[studentIndex + 1] else null

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // BACK BUTTON & SELECTED STUDENT HEADER CARD
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { holisticViewModel.selectStudent(null) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back to Student List",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val codeDisplay = if (student.studentCode.isNotBlank()) student.studentCode else String.format("%03d", student.rollNumber)
                                    Text(
                                        text = "Grade ${student.gradeName} - ${student.className}  |  ID: $codeDisplay  |  Period: $selectedPeriod",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (overallHcmScore > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Avg: ${String.format("%.1f", overallHcmScore)} ⭐",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
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

                        // ASSESSMENT ITEMS SCROLLABLE CONTENT
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // HCM ASSESSMENT ITEMS SECTION
                            item {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "HCM Assessment Items (Pillars)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (overallHcmScore > 0) {
                                                Text(
                                                    text = "HCM Score: ${String.format("%.1f", overallHcmScore)} / 5.0",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        val pillars = listOf(
                                            "HONESTY" to "HONESTY (ရိုးသားဖြောင့်မတ်မှု)",
                                            "CURIOSITY" to "CURIOSITY (စူးစမ်းလိုစိတ်)",
                                            "MINDFULNESS" to "MINDFULNESS (သတိတရားနှင့်ကိုယ်ကျင့်တရား)"
                                        )

                                        pillars.forEach { (pillarKey, pillarTitle) ->
                                            val itemsInPillar = displayCategories.filter { it.pillar.equals(pillarKey, ignoreCase = true) }
                                            if (itemsInPillar.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = pillarTitle,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                itemsInPillar.forEachIndexed { idx, cat ->
                                                    val currentRating = holisticRatings[cat.id] ?: 0
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}။ ${cat.categoryName}",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        StarRatingWidget(
                                                            currentRating = currentRating,
                                                            maxStars = cat.maxStars,
                                                            onRatingSelected = { stars ->
                                                                if (canEdit && !isLocked) {
                                                                    holisticViewModel.updateHolisticRating(cat.id, stars)
                                                                }
                                                            },
                                                            starSize = 22.dp,
                                                            readOnly = !canEdit || isLocked
                                                        )
                                                    }
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // TEACHER REMARKS / COMMENTS SECTION
                            item {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Teacher Comments & Remarks",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        OutlinedTextField(
                                            value = positiveComments,
                                            onValueChange = { holisticViewModel.positiveComments.value = it },
                                            label = { Text("Positive Strengths / Praise", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            minLines = 2,
                                            readOnly = isLocked,
                                            enabled = canEdit && !isLocked
                                        )

                                        OutlinedTextField(
                                            value = areasForImprovement,
                                            onValueChange = { holisticViewModel.areasForImprovement.value = it },
                                            label = { Text("Areas for Improvement", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            minLines = 2,
                                            readOnly = isLocked,
                                            enabled = canEdit && !isLocked
                                        )

                                        OutlinedTextField(
                                            value = generalComment,
                                            onValueChange = { holisticViewModel.generalComment.value = it },
                                            label = { Text("General Remark / Feedback", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            minLines = 2,
                                            readOnly = isLocked,
                                            enabled = canEdit && !isLocked
                                        )
                                    }
                                }
                            }
                        }

                        // ACTION BUTTONS BAR: Save & Record, Save & Next, Cancel / Edit / Back
                        Surface(
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isLocked) {
                                            isLocked = false
                                            snackbarMessage = "Assessment returned to editable state."
                                        } else {
                                            holisticViewModel.selectStudent(null)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        if (isLocked) Icons.Default.Edit else Icons.Default.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (isLocked) "Edit" else "Back", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        holisticViewModel.saveCurrentStudentAssessment(
                                            currentUser?.fullName ?: "Teacher"
                                        )
                                        isLocked = true
                                        snackbarMessage = "Assessment saved and locked from accidental touch."
                                    },
                                    enabled = canEdit && !isLocked,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                if (nextStudent != null) {
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            holisticViewModel.saveCurrentStudentAssessment(
                                                currentUser?.fullName ?: "Teacher"
                                            )
                                            isLocked = false
                                            holisticViewModel.selectStudent(nextStudent)
                                        },
                                        enabled = canEdit,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ADMIN CONFIGURATION TAB
                AdminConfigurationContent(
                    categories = adminHolisticCategories,
                    selectedLevel = adminLevel,
                    onLevelSelected = { holisticViewModel.adminConfigLevel.value = it },
                    onAddItem = { name, pillar -> holisticViewModel.addHolisticCategoryForPillar(name, pillar, adminLevel) },
                    onEditItem = { category, newName -> holisticViewModel.updateHolisticCategoryName(category, newName) },
                    onDeleteItem = { category -> holisticViewModel.deleteHolisticCategoryGroup(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialogs
    if (showAddHolisticDialog) {
        AddEditHolisticCategoryDialog(
            categoryToEdit = holisticToEdit,
            initialEducationLevel = adminLevel,
            onDismiss = {
                showAddHolisticDialog = false
                holisticToEdit = null
            },
            onConfirm = { name, description, pillar, level, maxStars ->
                if (holisticToEdit == null) {
                    holisticViewModel.addHolisticCategory(name, description, pillar, level, maxStars)
                } else {
                    holisticViewModel.updateHolisticCategory(
                        holisticToEdit!!.copy(
                            categoryName = name,
                            description = description,
                            pillar = pillar,
                            educationLevel = level,
                            maxStars = maxStars
                        )
                    )
                }
                showAddHolisticDialog = false
                holisticToEdit = null
            }
        )
    }

    if (showAddPeriodDialog) {
        AddEditAssessmentPeriodDialog(
            periodToEdit = periodToEdit,
            initialEducationLevel = adminLevel,
            onDismiss = {
                showAddPeriodDialog = false
                periodToEdit = null
            },
            onConfirm = { name, level, category ->
                if (periodToEdit == null) {
                    holisticViewModel.addAssessmentPeriod(name, level, category)
                } else {
                    holisticViewModel.updateAssessmentPeriod(
                        periodToEdit!!.copy(
                            periodName = name,
                            educationLevel = level,
                            periodCategory = category
                        )
                    )
                }
                showAddPeriodDialog = false
                periodToEdit = null
            }
        )
    }
}

@Composable
private fun StudentListItemCard(
    student: StudentEntity,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle Avatar Initials
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = student.name.take(1).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = student.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val codeDisplay = if (student.studentCode.isNotBlank()) student.studentCode else String.format("%03d", student.rollNumber)
                    Text(
                        text = "ID: $codeDisplay  |  Roll #${student.rollNumber}  |  ${student.gradeName}-${student.className}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assess", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminConfigurationContent(
    categories: List<HolisticCategoryEntity>,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    onAddItem: (name: String, pillar: String) -> Unit,
    onEditItem: (category: HolisticCategoryEntity, newName: String) -> Unit,
    onDeleteItem: (category: HolisticCategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCategory by remember { mutableStateOf<HolisticCategoryEntity?>(null) }
    var addingPillar by remember { mutableStateOf<String?>(null) }

    val sections = listOf(
        Triple("HONESTY", "HONESTY (ရိုးသားဖြောင့်မတ်မှု)", Icons.Default.CheckCircle),
        Triple("CURIOSITY", "CURIOSITY (စူးစမ်းလိုစိတ်)", Icons.Default.Search),
        Triple("MINDFULNESS", "MINDFULNESS (သတိတရားနှင့်ကိုယ်ကျင့်တရား)", Icons.Default.Star)
    )

    // Filter distinct categories per pillar
    val distinctCategories = remember(categories) {
        categories.distinctBy { Pair(it.pillar.uppercase(), it.categoryName) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HCM Assessment Administration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Manage monthly behavioural evaluation criteria for Honesty, Curiosity, and Mindfulness by Education Level.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Assessment Level Selector Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Assessment Level Configuration",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val levels = listOf(
                            "KINDERGARTEN" to "Kindergarten",
                            "PRIMARY" to "Primary (G1-G5)",
                            "SECONDARY" to "Secondary (G6-G9)",
                            "HIGH_SCHOOL" to "High School (G10-G12)"
                        )
                        for ((code, label) in levels) {
                            FilterChip(
                                selected = selectedLevel.equals(code, ignoreCase = true),
                                onClick = { onLevelSelected(code) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        sections.forEach { (pillarKey, pillarTitle, icon) ->
            val pillarItems = distinctCategories.filter { it.pillar.equals(pillarKey, ignoreCase = true) }

            item(key = pillarKey) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = pillarTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Assessment Rows
                        if (pillarItems.isEmpty()) {
                            Text(
                                text = "အကဲဖြတ်အချက် မရှိသေးပါ။",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            )
                        } else {
                            pillarItems.forEachIndexed { index, catItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. ${catItem.categoryName}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { editingCategory = catItem },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteItem(catItem) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (index < pillarItems.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Item Button
                        OutlinedButton(
                            onClick = { addingPillar = pillarKey },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Item", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Edit Item Dialog
    editingCategory?.let { cat ->
        var textValue by remember { mutableStateOf(cat.categoryName) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = {
                Text("Edit Assessment Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Assessment Text (Myanmar)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textValue.isNotBlank()) {
                            onEditItem(cat, textValue.trim())
                        }
                        editingCategory = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Item Dialog
    addingPillar?.let { pillar ->
        var textValue by remember { mutableStateOf("") }
        val titleText = when (pillar.uppercase()) {
            "HONESTY" -> "Add Item - HONESTY (ရိုးသားဖြောင့်မတ်မှု)"
            "CURIOSITY" -> "Add Item - CURIOSITY (စူးစမ်းလိုစိတ်)"
            else -> "Add Item - MINDFULNESS (သတိတရားနှင့်ကိုယ်ကျင့်တရား)"
        }
        AlertDialog(
            onDismissRequest = { addingPillar = null },
            title = {
                Text(titleText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Assessment Text (Myanmar)") },
                    placeholder = { Text("အကဲဖြတ်ချက် စာသား ရိုက်ထည့်ပါ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textValue.isNotBlank()) {
                            onAddItem(textValue.trim(), pillar)
                        }
                        addingPillar = null
                    }
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { addingPillar = null }) {
                    Text("Cancel")
                }
            }
        )
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
