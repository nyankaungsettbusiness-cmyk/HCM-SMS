package com.example.ui.screens.academicyear

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.data.policy.SchoolPolicy
import com.example.ui.viewmodel.AcademicYearViewModel
import com.example.ui.viewmodel.SchoolPolicyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicYearPromotionScreen(
    academicYearViewModel: AcademicYearViewModel,
    currentUser: UserEntity?,
    policyViewModel: SchoolPolicyViewModel,
    modifier: Modifier = Modifier
) {
    val allAcademicYears by academicYearViewModel.allAcademicYears.collectAsState()
    val activeAcademicYear by academicYearViewModel.activeAcademicYear.collectAsState()
    val promotionHistoryList by academicYearViewModel.promotionHistoryList.collectAsState()
    val filteredStudents by academicYearViewModel.filteredStudents.collectAsState()

    val selectedFromYear by academicYearViewModel.selectedFromYear.collectAsState()
    val selectedToYear by academicYearViewModel.selectedToYear.collectAsState()
    val filterGrade by academicYearViewModel.filterGrade.collectAsState()
    val filterClass by academicYearViewModel.filterClass.collectAsState()
    val searchQuery by academicYearViewModel.searchQuery.collectAsState()

    val statusMessage by academicYearViewModel.statusMessage.collectAsState()
    val isProcessing by academicYearViewModel.isProcessing.collectAsState()

    val canManage = academicYearViewModel.canManage(currentUser)

    var mainTab by remember { mutableStateOf(0) } // 0: Academic Years, 1: Promotion Wizard, 2: History

    // Dialog States
    var showCreateYearDialog by remember { mutableStateOf(false) }
    var showPromoteSingleDialog by remember { mutableStateOf<StudentEntity?>(null) }
    var showPromoteClassConfirm by remember { mutableStateOf(false) }
    var showPromoteGradeConfirm by remember { mutableStateOf(false) }

    val dbGrades by policyViewModel.grades.collectAsState()
    val dbClasses by policyViewModel.classes.collectAsState()

    val gradesList = if (dbGrades.isNotEmpty()) dbGrades.map { it.gradeName } else SchoolPolicy.VALID_GRADES
    val classesList = if (dbClasses.isNotEmpty()) dbClasses.map { it.className }.distinct() else listOf("A", "B", "C", "D")

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (mainTab == 0 && canManage) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateYearDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("New Academic Year", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_add_academic_year")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Compact Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Academic Years",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Academic Year & Promotion",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Active: ${activeAcademicYear?.displayName ?: "None Selected"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Role Badge
                    val roleLabel = currentUser?.role?.displayName ?: "User"
                    val badgeBg = if (canManage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    val badgeFg = if (canManage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (canManage) "$roleLabel (Admin)" else "$roleLabel (Read)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = mainTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = mainTab == 0,
                    onClick = { mainTab = 0 },
                    text = { Text("Academic Years", fontSize = 12.sp, fontWeight = if (mainTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_academic_years")
                )
                Tab(
                    selected = mainTab == 1,
                    onClick = { mainTab = 1 },
                    text = { Text("Promotion", fontSize = 12.sp, fontWeight = if (mainTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_student_promotion")
                )
                Tab(
                    selected = mainTab == 2,
                    onClick = { mainTab = 2 },
                    text = { Text("History", fontSize = 12.sp, fontWeight = if (mainTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_promotion_history")
                )
            }
            // Toast / Status Message
            statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { academicYearViewModel.clearStatusMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            when (mainTab) {
                0 -> {
                    // TAB 0: ACADEMIC YEARS MANAGEMENT
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Academic Years & Sessions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Only ONE Academic Year can be Active at a time. Closing an Academic Year unlocks Student Promotion.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(allAcademicYears, key = { it.id }) { yr ->
                            AcademicYearCard(
                                year = yr,
                                canManage = canManage,
                                onActivate = {
                                    android.util.Log.d("AcademicYearDebug", "UI_CLICK: AcademicYearPromotionScreen Activate clicked for yearId=${yr.id}, displayName='${yr.displayName}'")
                                    academicYearViewModel.activateAcademicYear(yr.id, yr.displayName)
                                },
                                onClose = { academicYearViewModel.closeAcademicYear(yr.id, yr.displayName, currentUser?.fullName ?: "Admin") },
                                onArchive = { academicYearViewModel.archiveAcademicYear(yr.id, yr.displayName) }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: STUDENT PROMOTION WIZARD
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        // Header / Promotion Settings
                        Surface(
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "1. ACADEMIC YEAR PROMOTION TARGETS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selectedFromYear,
                                        onValueChange = { academicYearViewModel.setFromYear(it) },
                                        label = { Text("From Year", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_from_year")
                                    )

                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterVertically).size(18.dp)
                                    )

                                    OutlinedTextField(
                                        value = selectedToYear,
                                        onValueChange = { academicYearViewModel.setToYear(it) },
                                        label = { Text("To Year", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_to_year")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "2. GRADE & CLASS SELECTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Grade Chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(gradesList) { gr ->
                                        FilterChip(
                                            selected = filterGrade == gr,
                                            onClick = { academicYearViewModel.setFilterGrade(gr) },
                                            label = { Text(gr, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Class Chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(classesList) { cl ->
                                        FilterChip(
                                            selected = filterClass == cl,
                                            onClick = { academicYearViewModel.setFilterClass(cl) },
                                            label = { Text("Class $cl", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Search Bar & Bulk Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { academicYearViewModel.setSearchQuery(it) },
                                placeholder = { Text("Search by name or student ID...", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_search_promotion_student")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bulk Actions Bar
                        if (canManage && filteredStudents.isNotEmpty()) {
                            val nextGr = academicYearViewModel.getNextGrade(filterGrade)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Bulk Actions (${filteredStudents.size} students in $filterGrade-$filterClass):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Target: $nextGr-$filterClass",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { showPromoteClassConfirm = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("btn_promote_whole_class")
                                        ) {
                                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Class", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { showPromoteGradeConfirm = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("btn_promote_whole_grade")
                                        ) {
                                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Grade", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Student Cards List
                        if (filteredStudents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No students found matching current filters.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredStudents, key = { it.id }) { student ->
                                    StudentPromotionCard(
                                        student = student,
                                        nextGrade = academicYearViewModel.getNextGrade(student.gradeName),
                                        canManage = canManage,
                                        onPromoteSingle = { showPromoteSingleDialog = student }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: PROMOTION HISTORY LOG
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text = "Complete Promotion Audit History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "All historical grade advancements, repeat years, transfers, and graduations.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (promotionHistoryList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No promotion records logged yet.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(promotionHistoryList, key = { it.id }) { log ->
                                PromotionHistoryRowCard(log)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: Create Academic Year Dialog
    if (showCreateYearDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("2026-06-01") }
        var endDate by remember { mutableStateOf("2027-03-31") }

        AlertDialog(
            onDismissRequest = { showCreateYearDialog = false },
            title = { Text("Create New Academic Year") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it
                            if (name.isBlank()) name = "$it Academic Year"
                        },
                        label = { Text("Year Code (e.g. 2026-2027)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_year_code")
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        academicYearViewModel.createAcademicYear(code, name, startDate, endDate)
                        showCreateYearDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_create_year")
                ) {
                    Text("Create Year")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateYearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG 2: Promote Individual Student Dialog
    showPromoteSingleDialog?.let { student ->
        val defaultNextGr = academicYearViewModel.getNextGrade(student.gradeName)
        var targetGr by remember { mutableStateOf(defaultNextGr) }
        var targetCl by remember { mutableStateOf(student.className) }
        var targetRoll by remember { mutableStateOf(student.rollNumber.toString()) }
        var actionType by remember { mutableStateOf(PromotionAction.PROMOTED) }
        var remarks by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPromoteSingleDialog = null },
            title = { Text("Promote / Update Student: ${student.name}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Student ID: ${student.studentCode} | Current: ${student.gradeName}-${student.className} (Roll ${student.rollNumber})", fontSize = 12.sp)

                    Text("Promotion Action:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = actionType == PromotionAction.PROMOTED,
                            onClick = {
                                actionType = PromotionAction.PROMOTED
                                targetGr = defaultNextGr
                            },
                            label = { Text("Promote", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = actionType == PromotionAction.RETAINED,
                            onClick = {
                                actionType = PromotionAction.RETAINED
                                targetGr = student.gradeName
                            },
                            label = { Text("Repeat", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = actionType == PromotionAction.TRANSFERRED,
                            onClick = {
                                actionType = PromotionAction.TRANSFERRED
                                targetGr = student.gradeName
                            },
                            label = { Text("Transfer", fontSize = 10.sp) }
                        )
                        if (student.gradeName == "G12") {
                            FilterChip(
                                selected = actionType == PromotionAction.GRADUATED,
                                onClick = {
                                    actionType = PromotionAction.GRADUATED
                                    targetGr = "Alumni"
                                },
                                label = { Text("Graduate", fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = targetGr,
                        onValueChange = { targetGr = it },
                        label = { Text("Target Grade") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetCl,
                        onValueChange = { targetCl = it },
                        label = { Text("Target Class") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetRoll,
                        onValueChange = { targetRoll = it },
                        label = { Text("Target Roll Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val roll = targetRoll.toIntOrNull() ?: student.rollNumber
                        academicYearViewModel.promoteIndividual(
                            student = student,
                            targetGrade = targetGr,
                            targetClass = targetCl,
                            targetRollNumber = roll,
                            actionType = actionType,
                            promotedBy = currentUser?.fullName ?: "Admin",
                            remarks = remarks
                        )
                        showPromoteSingleDialog = null
                    },
                    modifier = Modifier.testTag("btn_confirm_promote_single")
                ) {
                    Text("Apply Promotion")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteSingleDialog = null }) { Text("Cancel") }
            }
        )
    }

    // DIALOG 3: Confirm Whole Class Promotion
    if (showPromoteClassConfirm) {
        val count = filteredStudents.size
        val nextGr = academicYearViewModel.getNextGrade(filterGrade)

        AlertDialog(
            onDismissRequest = { showPromoteClassConfirm = false },
            title = { Text("Confirm Bulk Class Promotion") },
            text = {
                Text("Are you sure you want to promote all $count students in $filterGrade-$filterClass to Grade $nextGr Class $filterClass for academic year $selectedToYear?\n\nAll historical records will remain preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        academicYearViewModel.promoteWholeClass(
                            targetGrade = nextGr,
                            targetClass = filterClass,
                            promotedBy = currentUser?.fullName ?: "Admin"
                        )
                        showPromoteClassConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Yes, Promote Class")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteClassConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG 4: Confirm Whole Grade Promotion
    if (showPromoteGradeConfirm) {
        val nextGr = academicYearViewModel.getNextGrade(filterGrade)

        AlertDialog(
            onDismissRequest = { showPromoteGradeConfirm = false },
            title = { Text("Confirm Bulk Grade Promotion") },
            text = {
                Text("Are you sure you want to promote ALL students in Grade $filterGrade to Grade $nextGr for academic year $selectedToYear?\n\nClass sections and student profiles will advance automatically while keeping historical records intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        academicYearViewModel.promoteWholeGrade(
                            promotedBy = currentUser?.fullName ?: "Admin"
                        )
                        showPromoteGradeConfirm = false
                    }
                ) {
                    Text("Yes, Promote Grade")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteGradeConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AcademicYearCard(
    year: AcademicYearEntity,
    canManage: Boolean,
    onActivate: () -> Unit,
    onClose: () -> Unit,
    onArchive: () -> Unit
) {
    val statusColor = when (year.status) {
        AcademicYearStatus.ACTIVE -> Color(0xFF15803D)
        AcademicYearStatus.UPCOMING -> Color(0xFF0284C7)
        AcademicYearStatus.CLOSED -> Color(0xFFD97706)
        AcademicYearStatus.ARCHIVED -> Color(0xFF64748B)
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(year.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (year.isCurrentActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF15803D),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ACTIVE SESSION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Code: ${year.yearCode} | Period: ${year.startDate} to ${year.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = year.status.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (canManage) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!year.isCurrentActive && year.status != AcademicYearStatus.ARCHIVED) {
                        Button(
                            onClick = onActivate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_activate_${year.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Activate", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (year.status == AcademicYearStatus.ACTIVE) {
                        OutlinedButton(
                            onClick = onClose,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_close_${year.id}")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Close Session", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (year.status == AcademicYearStatus.CLOSED) {
                        OutlinedButton(
                            onClick = onArchive,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Archive", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentPromotionCard(
    student: StudentEntity,
    nextGrade: String,
    canManage: Boolean,
    onPromoteSingle: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${student.rollNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "ID: ${student.studentCode} | Grade ${student.gradeName}-${student.className}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (canManage) {
                OutlinedButton(
                    onClick = onPromoteSingle,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_promote_student_${student.id}")
                ) {
                    Text("Promote to $nextGrade", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun PromotionHistoryRowCard(log: PromotionHistoryEntity) {
    val actionColor = when (log.actionType) {
        PromotionAction.PROMOTED -> Color(0xFF15803D)
        PromotionAction.RETAINED -> Color(0xFFD97706)
        PromotionAction.TRANSFERRED -> Color(0xFF65558F)
        PromotionAction.GRADUATED -> Color(0xFF0D9488)
    }

    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(log.promotedDate))

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(log.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("ID: ${log.studentCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                Surface(
                    color = actionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = log.actionType.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = actionColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${log.fromAcademicYear} (${log.fromGrade}-${log.fromClass})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = "To: ${log.toAcademicYear} (${log.toGrade}-${log.toClass})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (log.remarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Note: ${log.remarks}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Promoted By: ${log.promotedBy} on $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
