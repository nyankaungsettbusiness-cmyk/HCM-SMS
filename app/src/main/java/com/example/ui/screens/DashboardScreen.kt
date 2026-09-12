package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.ui.components.*
import com.example.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUser: UserEntity?,
    dashboardViewModel: DashboardViewModel,
    schoolName: String,
    onNavigate: (String) -> Unit
) {
    val analyticsState by dashboardViewModel.analyticsState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Students", "Attendance", "Academics", "HCM & SGI", "Reports & Log")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Search & Filter Header
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = analyticsState.searchQuery,
                        onValueChange = { dashboardViewModel.setSearchQuery(it) },
                        placeholder = { Text("Search Student, Grade, Class, Year...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (analyticsState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { dashboardViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Filter Dropdowns Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterDropdownChip(
                            label = "Year: ${analyticsState.selectedAcademicYear}",
                            options = analyticsState.availableAcademicYears,
                            onSelect = { dashboardViewModel.setSelectedAcademicYear(it) }
                        )
                    }
                    item {
                        FilterDropdownChip(
                            label = "Grade: ${analyticsState.selectedGrade}",
                            options = analyticsState.availableGrades,
                            onSelect = { dashboardViewModel.setSelectedGrade(it) }
                        )
                    }
                    item {
                        FilterDropdownChip(
                            label = "Class: ${analyticsState.selectedClass}",
                            options = analyticsState.availableClasses,
                            onSelect = { dashboardViewModel.setSelectedClass(it) }
                        )
                    }
                    item {
                        FilterDropdownChip(
                            label = "Assessment: ${analyticsState.selectedAssessmentType}",
                            options = analyticsState.availableAssessmentTypes,
                            onSelect = { dashboardViewModel.setSelectedAssessmentType(it) }
                        )
                    }
                }
            }
        }

        // Section Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header
            item {
                RoleWelcomeHeaderCard(
                    currentUser = currentUser,
                    schoolName = schoolName,
                    activeYear = analyticsState.activeAcademicYearName
                )
            }

            when (selectedTab) {
                0 -> { // Overview
                    item { OverviewCardsSection(analyticsState, onNavigate) }
                    item { ModuleQuickAccessGrid(onNavigate) }
                    item { QuickActivityTimelineSection(analyticsState.recentActivities) }
                }
                1 -> { // Student Analytics
                    item { StudentAnalyticsSection(analyticsState) }
                }
                2 -> { // Attendance Analytics
                    item { AttendanceAnalyticsSection(analyticsState) }
                }
                3 -> { // Academic Analytics
                    item { AcademicAnalyticsSection(analyticsState) }
                }
                4 -> { // HCM & SGI Analytics
                    item { HcmSgiAnalyticsSection(analyticsState) }
                }
                5 -> { // Reports & Log Analytics
                    item { ReportAnalyticsSection(analyticsState, onNavigate) }
                }
            }
        }
    }
}

@Composable
fun FilterDropdownChip(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RoleWelcomeHeaderCard(
    currentUser: UserEntity?,
    schoolName: String,
    activeYear: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Welcome back, ${currentUser?.fullName ?: "Administrator"}!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Text(
                    text = "Role: ${currentUser?.role?.displayName ?: "Super Admin"} • Year $activeYear • $schoolName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )

                val roleMessage = when (currentUser?.role) {
                    UserRole.SUPER_ADMIN -> "Full Access: School-wide System Analytics"
                    UserRole.ADMIN -> "School Admin Dashboard Access"
                    UserRole.TEACHER -> "Teacher Mode: Filtered for assigned classes"
                    UserRole.OFFICE_STAFF -> "Office Staff: Administrative & Report Analytics"
                    else -> "School Analytics Active"
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = roleMessage,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            val logoBitmap = com.example.ui.util.rememberSchoolLogo()

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (logoBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = logoBitmap,
                        contentDescription = "School Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewCardsSection(
    state: DashboardAnalyticsState,
    onNavigate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "School Overview Cards",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        val cards = listOf(
            OverviewStatCardData("Total Students", "${state.totalStudents}", "Active Students", Icons.Default.People, MaterialTheme.colorScheme.primaryContainer, "students"),
            OverviewStatCardData("Total Teachers", "${state.totalTeachers}", "Teaching Faculty", Icons.Default.School, MaterialTheme.colorScheme.secondaryContainer, "users"),
            OverviewStatCardData("Total Classes", "${state.totalClasses}", "Active Sections", Icons.Default.Class, MaterialTheme.colorScheme.tertiaryContainer, "policy_center"),
            OverviewStatCardData("Total Grades", "${state.totalGrades}", "Education Levels", Icons.Default.Domain, MaterialTheme.colorScheme.surfaceVariant, "policy_center"),
            OverviewStatCardData("Academic Year", state.activeAcademicYearName, "Current Period", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primaryContainer, "academic_year"),
            OverviewStatCardData("Total Subjects", "${state.totalSubjects}", "Curriculum", Icons.Default.MenuBook, MaterialTheme.colorScheme.secondaryContainer, "policy_center"),
            OverviewStatCardData("Today's Attendance", "%.1f%%".format(state.todayAttendancePercentage), "Daily Register", Icons.Default.HowToReg, MaterialTheme.colorScheme.tertiaryContainer, "attendance"),
            OverviewStatCardData("Monthly Attendance", "%.1f%%".format(state.monthlyAttendancePercentage), "Month Average", Icons.Default.EventAvailable, MaterialTheme.colorScheme.primaryContainer, "attendance"),
            OverviewStatCardData("Pending Exams", "${state.pendingAssessmentsCount}", "Assessment Queue", Icons.Default.Assignment, MaterialTheme.colorScheme.secondaryContainer, "assessment"),
            OverviewStatCardData("Report Cards", "${state.generatedReportCardsCount}", "Generated Cards", Icons.Default.Grade, MaterialTheme.colorScheme.tertiaryContainer, "reports")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCards.forEach { card ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate(card.route) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = card.containerColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            card.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = card.value,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = card.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = card.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class OverviewStatCardData(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val containerColor: Color,
    val route: String
)

@Composable
fun ModuleQuickAccessGrid(onNavigate: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "System Navigation",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickNavIconButton("Students", Icons.Default.People, "students", onNavigate)
                QuickNavIconButton("Attendance", Icons.Default.HowToReg, "attendance", onNavigate)
                QuickNavIconButton("Assessment", Icons.Default.Assignment, "assessment", onNavigate)
                QuickNavIconButton("Marks", Icons.Default.FactCheck, "marks_entry", onNavigate)
                QuickNavIconButton("Holistic", Icons.Default.Psychology, "holistic_assessment", onNavigate)
                QuickNavIconButton("Reports", Icons.Default.Grade, "reports", onNavigate)
            }
        }
    }
}

@Composable
fun QuickNavIconButton(
    label: String,
    icon: ImageVector,
    route: String,
    onNavigate: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onNavigate(route) }
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun StudentAnalyticsSection(state: DashboardAnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Active / Transferred / Graduated
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadgeCard("Active Students", "${state.activeStudentsCount}", Icons.Default.CheckCircle, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
            MetricBadgeCard("Transferred", "${state.transferredStudentsCount}", Icons.Default.SwapHoriz, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
            MetricBadgeCard("Graduated", "${state.graduatedStudentsCount}", Icons.Default.School, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
        }

        // Students by Grade Bar Chart
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Students Distribution by Grade", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))
                BarChart(
                    data = state.studentsByGradeMap.mapValues { it.value.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    valueFormatter = { "%.0f".format(it) }
                )
            }
        }

        // Gender Distribution & Students by Class
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Gender Ratio", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    PieDonutChart(
                        data = state.genderDistributionMap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceAnalyticsSection(state: DashboardAnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Attendance Circular Gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularGaugeCard(
                percentage = state.todayAttendancePercentage,
                title = "Today's Register",
                subtitle = "Present Rate",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary
            )
            CircularGaugeCard(
                percentage = state.monthlyAttendancePercentage,
                title = "Monthly Average",
                subtitle = "Current Month",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Today's Status Breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Today's Attendance Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AttendanceCountBadge("Present", state.todayAttendancePresentCount, MaterialTheme.colorScheme.primaryContainer)
                    AttendanceCountBadge("Late", state.todayAttendanceLateCount, MaterialTheme.colorScheme.tertiaryContainer)
                    AttendanceCountBadge("Absent", state.todayAttendanceAbsentCount, MaterialTheme.colorScheme.errorContainer)
                    AttendanceCountBadge("Leave", state.todayAttendanceLeaveCount, MaterialTheme.colorScheme.secondaryContainer)
                }
            }
        }

        // Attendance Trend Line Chart
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Attendance Monthly Trends", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                LineChart(
                    data = state.attendanceTrendsMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        // Low Attendance & Top Regular Lists
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Low Attendance (<75%)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Low Attendance (<75%)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (state.lowAttendanceStudents.isEmpty()) {
                        Text("No students below 75%", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        state.lowAttendanceStudents.take(3).forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("%.1f%%".format(s.percentage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Top Regular (>=95%)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Most Regular (>=95%)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (state.topRegularStudents.isEmpty()) {
                        Text("All students logged", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        state.topRegularStudents.take(3).forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("%.1f%%".format(s.percentage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceCountBadge(label: String, count: Int, containerColor: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun AcademicAnalyticsSection(state: DashboardAnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Overall Academic Performance Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Overall Academic Performance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("School-wide average mark percentage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
                Text(
                    text = "%.1f%%".format(state.overallAcademicAverage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Average Score by Grade Bar Chart
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Average Marks by Grade", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                BarChart(
                    data = state.averageScoreByGradeMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    valueFormatter = { "%.1f".format(it) }
                )
            }
        }

        // Assessment Type Statistics (Monthly Test, Pilot Test, CET, etc.)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Performance by Assessment Type", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                val perfMap = state.assessmentTypePerformanceMap

                if (perfMap.isEmpty()) {
                    Text("No assessment performance data available", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    perfMap.forEach { (type, avg) ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(type, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("%.1f%%".format(avg), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(
                                progress = { (avg / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
        }

        // Highest & Lowest Score Highlights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Highest Score", fontSize = 11.sp, color = Color.Gray)
                    Text(state.highestScoreStudent?.first ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(if (state.highestScoreStudent != null) "%.1f Marks".format(state.highestScoreStudent.second) else "-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Lowest Score", fontSize = 11.sp, color = Color.Gray)
                    Text(state.lowestScoreStudent?.first ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(if (state.lowestScoreStudent != null) "%.1f Marks".format(state.lowestScoreStudent.second) else "-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun HcmSgiAnalyticsSection(state: DashboardAnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // HCM Score Overview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hein Chan Myae (HCM) Rating", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Honesty • Curiosity • Mindfulness", fontSize = 11.sp)
                    }
                    Text("%.1f / 5.0".format(state.overallHcmScore), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HcmScorePill("Honesty", state.averageHonesty)
                    HcmScorePill("Curiosity", state.averageCuriosity)
                    HcmScorePill("Mindfulness", state.averageMindfulness)
                }
            }
        }

        // SGI Analytics Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Student Growth Index (SGI) Analytics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMiniBox("Average SGI", "%.1f".format(state.averageSgi))
                    MetricMiniBox("Highest SGI", "%.1f".format(state.highestSgi))
                    MetricMiniBox("Lowest SGI", "%.1f".format(state.lowestSgi))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("SGI Growth Trends", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LineChart(
                    data = state.sgiGrowthTrendsMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }

        // Top HCM Performing Students & Support Required
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Top HCM Students", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (state.topHcmStudents.isEmpty()) {
                        Text("No ratings logged", fontSize = 10.sp, color = Color.Gray)
                    } else {
                        state.topHcmStudents.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.name, fontSize = 11.sp, maxLines = 1)
                                Text("%.1f".format(s.overallScore), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Requiring Support", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (state.supportRequiredHcmStudents.isEmpty()) {
                        Text("No students pending", fontSize = 10.sp, color = Color.Gray)
                    } else {
                        state.supportRequiredHcmStudents.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.name, fontSize = 11.sp, maxLines = 1)
                                Text("%.1f".format(s.overallScore), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HcmScorePill(title: String, score: Float) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("%.1f".format(score), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun MetricMiniBox(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(title, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ReportAnalyticsSection(
    state: DashboardAnalyticsState,
    onNavigate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Report Cards Summary
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Report Cards & Export Analytics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBadgeCard("Generated", "${state.totalReportsGenerated}", Icons.Default.Grade, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                    MetricBadgeCard("Monthly", "${state.monthlyReportsCount}", Icons.Default.Event, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                    MetricBadgeCard("PDF Exports", "${state.pdfExportCount}", Icons.Default.PictureAsPdf, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                    MetricBadgeCard("Prints", "${state.printCount}", Icons.Default.Print, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                }
            }
        }

        // Timeline Activities
        QuickActivityTimelineSection(state.recentActivities)
    }
}

@Composable
fun QuickActivityTimelineSection(activities: List<RecentActivityItem>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Recent System Activity", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            HorizontalDivider()

            if (activities.isEmpty()) {
                Text("No recent activities logged.", fontSize = 12.sp, color = Color.Gray)
            } else {
                activities.take(6).forEach { act ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(act.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(act.subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        }
                        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(act.timestamp))
                        Text(formattedTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadgeCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(title, fontSize = 9.sp, color = Color.DarkGray, maxLines = 1)
        }
    }
}
