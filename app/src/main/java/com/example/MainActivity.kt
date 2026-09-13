package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.db.AppDatabase
import com.example.data.local.entity.AcademicYearStatus
import com.example.data.local.entity.UserRole
import com.example.data.repository.AcademicYearRepository
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AssessmentRepository
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.DashboardRepository
import com.example.data.repository.SystemSettingsRepository
import com.example.data.repository.HolisticRepository
import com.example.data.repository.MarksRepository
import com.example.data.repository.ReportRepository
import com.example.data.repository.SchoolPolicyRepository
import com.example.data.repository.StudentRepository
import com.example.data.repository.TeacherRepository
import com.example.data.sync.SyncManager

import com.example.ui.components.AppNavigationDrawerContent
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.HcmTopAppBar
import com.example.ui.components.ScreenRoute
import com.example.data.util.AppUpdateManager
import com.example.data.util.AppUpdateInfo
import com.example.ui.screens.*
import com.example.ui.screens.academicyear.AcademicYearPromotionScreen
import com.example.ui.screens.ai.AiAssistantScreen
import com.example.ui.screens.assessment.AssessmentManagementScreen
import com.example.ui.screens.attendance.AttendanceManagementScreen
import com.example.ui.screens.holistic.HolisticAssessmentScreen
import com.example.ui.screens.marks.MarksEntryScreen
import com.example.ui.screens.policy.SchoolPolicyCenterScreen
import com.example.ui.screens.ranking.ExamRankingScreen
import com.example.ui.screens.reports.ReportCardsScreen
import com.example.ui.theme.HcmSmsTheme
import com.example.ui.viewmodel.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val logoFile = com.example.ui.util.SchoolLogoUtils.getSchoolLogoFile(applicationContext)
        if (!logoFile.exists() || logoFile.length() == 0L) {
            try {
                @Suppress("ResourceType")
                applicationContext.resources.openRawResource(R.drawable.ic_school_logo).use { input ->
                    java.io.FileOutputStream(logoFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val db = AppDatabase.getInstance(applicationContext)
        val authRepo = AuthRepository(db.userDao())
        val policyRepo = SchoolPolicyRepository(db.schoolPolicyDao(), db.academicYearDao())
        val studentRepo = StudentRepository(db.studentDao(), applicationContext)

        // Initialize Smart On-Demand Sync & FCM Token on app launch
        SyncManager.init(applicationContext)
        SyncManager.schedulePeriodicSync(applicationContext, intervalMinutes = 360)
        SyncManager.triggerSyncAsync(applicationContext, forceImmediate = false)
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        com.example.data.service.FcmSyncService.saveCachedToken(applicationContext, token)
                        com.example.data.service.FcmSyncService.registerTokenWithSupabase(applicationContext, token)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "FCM token initialization note: ${e.message}")
        }
        val teacherRepo = TeacherRepository(db.teacherDao(), authRepo)
        val assessmentRepo = AssessmentRepository(db.assessmentDao())
        val marksRepo = MarksRepository(db.marksDao(), db.studentDao(), db.schoolPolicyDao())
        val holisticRepo = HolisticRepository(db.holisticDao(), db.assessmentPeriodDao())
        val attendanceRepo = AttendanceRepository(db.attendanceDao(), db.studentDao())
        val academicYearRepo = AcademicYearRepository(db.academicYearDao(), db.studentDao(), db.schoolPolicyDao())
        val dashboardRepo = DashboardRepository(
            studentDao = db.studentDao(),
            teacherDao = db.teacherDao(),
            attendanceDao = db.attendanceDao(),
            marksDao = db.marksDao(),
            assessmentDao = db.assessmentDao(),
            holisticDao = db.holisticDao(),
            reportDao = db.reportDao(),
            academicYearDao = db.academicYearDao(),
            userDao = db.userDao(),
            schoolPolicyDao = db.schoolPolicyDao()
        )

        val aiRepo = AiAssistantRepository(
            aiDao = db.aiDao(),
            aiChatDao = db.aiChatDao(),
            curriculumDao = db.curriculumKnowledgeDao(),
            studentDao = db.studentDao(),
            teacherDao = db.teacherDao(),
            marksDao = db.marksDao(),
            assessmentDao = db.assessmentDao(),
            holisticDao = db.holisticDao(),
            attendanceDao = db.attendanceDao(),
            schoolPolicyDao = db.schoolPolicyDao()
        )
        val reportRepo = ReportRepository(
            reportDao = db.reportDao(),
            studentDao = db.studentDao(),
            marksDao = db.marksDao(),
            assessmentDao = db.assessmentDao(),
            attendanceDao = db.attendanceDao(),
            holisticDao = db.holisticDao(),
            schoolPolicyDao = db.schoolPolicyDao(),
            academicYearDao = db.academicYearDao()
        )
        val systemSettingsRepo = SystemSettingsRepository(
            context = applicationContext,
            db = db,
            schoolPolicyDao = db.schoolPolicyDao(),
            reportDao = db.reportDao(),
            aiDao = db.aiDao(),
            userDao = db.userDao(),
            studentDao = db.studentDao(),
            assessmentDao = db.assessmentDao(),
            marksDao = db.marksDao(),
            attendanceDao = db.attendanceDao(),
            academicYearDao = db.academicYearDao()
        )

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            HcmSmsTheme(darkTheme = isDarkTheme) {
                HcmMainApp(
                    db = db,
                    authRepo = authRepo,
                    policyRepo = policyRepo,
                    studentRepo = studentRepo,
                    teacherRepo = teacherRepo,
                    assessmentRepo = assessmentRepo,
                    marksRepo = marksRepo,
                    holisticRepo = holisticRepo,
                    attendanceRepo = attendanceRepo,
                    academicYearRepo = academicYearRepo,
                    dashboardRepo = dashboardRepo,
                    aiRepo = aiRepo,
                    reportRepo = reportRepo,
                    systemSettingsRepo = systemSettingsRepo,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SyncManager.triggerSyncAsync(applicationContext)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HcmMainApp(
    db: AppDatabase,
    authRepo: AuthRepository,
    policyRepo: SchoolPolicyRepository,
    studentRepo: StudentRepository,
    teacherRepo: TeacherRepository,
    assessmentRepo: AssessmentRepository,
    marksRepo: MarksRepository,
    holisticRepo: HolisticRepository,
    attendanceRepo: AttendanceRepository,
    academicYearRepo: AcademicYearRepository,
    dashboardRepo: DashboardRepository,
    aiRepo: AiAssistantRepository,
    reportRepo: ReportRepository,
    systemSettingsRepo: SystemSettingsRepository,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepo))
    val policyViewModel: SchoolPolicyViewModel = viewModel(factory = SchoolPolicyViewModelFactory(policyRepo))
    val studentViewModel: StudentViewModel = viewModel(factory = StudentViewModelFactory(studentRepo, academicYearRepo))
    val teacherViewModel: TeacherViewModel = viewModel(factory = TeacherViewModelFactory(teacherRepo))
    val assessmentViewModel: AssessmentViewModel = viewModel(factory = AssessmentViewModelFactory(assessmentRepo, policyRepo))

    val marksViewModel: MarksViewModel = viewModel(factory = MarksViewModelFactory(marksRepo, studentRepo, assessmentRepo, academicYearRepo))
    val examRankingViewModel: ExamRankingViewModel = viewModel(
        factory = ExamRankingViewModelFactory(
            assessmentDao = db.assessmentDao(),
            marksDao = db.marksDao(),
            studentDao = db.studentDao(),
            academicYearDao = db.academicYearDao()
        )
    )
    val holisticViewModel: HolisticViewModel = viewModel(factory = HolisticViewModelFactory(holisticRepo, studentRepo, academicYearRepo))
    val attendanceViewModel: AttendanceViewModel = viewModel(factory = AttendanceViewModelFactory(attendanceRepo, studentRepo, academicYearRepo))
    val academicYearViewModel: AcademicYearViewModel = viewModel(factory = AcademicYearViewModelFactory(academicYearRepo, studentRepo))
    val dashboardViewModel: DashboardViewModel = viewModel(factory = com.example.ui.viewmodel.DashboardViewModelFactory(dashboardRepo))
    val aiViewModel: AiAssistantViewModel = viewModel(
        factory = AiAssistantViewModelFactory(
            repository = aiRepo,
            studentDao = db.studentDao(),
            marksDao = db.marksDao(),
            assessmentDao = db.assessmentDao(),
            holisticDao = db.holisticDao(),
            curriculumDao = db.curriculumKnowledgeDao()
        )
    )
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(reportRepo))
    val systemSettingsViewModel: SystemSettingsViewModel = viewModel(factory = SystemSettingsViewModelFactory(systemSettingsRepo, context))

    val currentUser by authViewModel.currentUser.collectAsState()
    val schoolSettings by policyViewModel.schoolSettings.collectAsState()
    val students by studentViewModel.studentsList.collectAsState()
    val grades by policyViewModel.grades.collectAsState()
    val subjects by policyViewModel.subjects.collectAsState()
    val auditLogs by authViewModel.auditLogs.collectAsState()

    val searchQuery by studentViewModel.searchQuery.collectAsState()
    val selectedGradeFilter by studentViewModel.selectedGradeFilter.collectAsState()
    val selectedClassFilter by studentViewModel.selectedClassFilter.collectAsState()
    val selectedStatusFilter by studentViewModel.selectedStatusFilter.collectAsState()

    // In-App GitHub Auto-Update State
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ScreenRoute.Dashboard.route

    val activeAcademicYearEntity by academicYearViewModel.activeAcademicYear.collectAsState()
    val allAcademicYearsList by academicYearViewModel.allAcademicYears.collectAsState()

    val academicYear = activeAcademicYearEntity?.yearCode 
        ?: allAcademicYearsList.firstOrNull { it.isCurrentActive }?.yearCode
        ?: allAcademicYearsList.firstOrNull { it.status == AcademicYearStatus.ACTIVE }?.yearCode
        ?: schoolSettings?.academicYear?.takeIf { it.isNotBlank() }
        ?: "2026-2027"
    val availableAcademicYearCodes = (listOf("2026-2027", "2027-2028", "2028-2029") + allAcademicYearsList.map { it.yearCode } + if (academicYear.isNotBlank()) listOf(academicYear) else emptyList())
        .filterNot { it == "2024-2025" || it == "2025-2026" }
        .distinct()

    LaunchedEffect(activeAcademicYearEntity) {
        android.util.Log.d("AcademicYearTrace", "MAIN_ACTIVITY_OBSERVE: Active academic year from Room = ${activeAcademicYearEntity?.yearCode} (isCurrentActive=${activeAcademicYearEntity?.isCurrentActive})")
    }

    LaunchedEffect(academicYear) {
        android.util.Log.d("AcademicYearTrace", "MAIN_ACTIVITY_EFFECT: Selected academicYear string changed to '$academicYear'")
        studentViewModel.setAcademicYear(academicYear)
        dashboardViewModel.setSelectedAcademicYear(academicYear)
        marksViewModel.academicYear.value = academicYear
        assessmentViewModel.setAcademicYearFilter(if (academicYear.isNotBlank()) academicYear else "ALL")
        attendanceViewModel.setAcademicYear(academicYear)
        reportViewModel.setAcademicYear(academicYear)
        holisticViewModel.academicYear.value = academicYear
        examRankingViewModel.setAcademicYear(academicYear)
    }

    LaunchedEffect(Unit) {
        authViewModel.cleanUpMockAccounts()
        authViewModel.restoreSession(context)
        authViewModel.syncCloudUsers()

        // Check for updates asynchronously without blocking or consuming DB quota
        try {
            val updateResult = AppUpdateManager.checkForUpdates()
            if (updateResult.hasUpdate) {
                updateInfo = updateResult
            }
        } catch (_: Exception) {}
    }

    // Require credentials login screen on launch
    if (currentUser == null) {
        LoginScreen(
            authViewModel = authViewModel,
            schoolSettings = schoolSettings
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppNavigationDrawerContent(
                    currentRoute = currentRoute,
                    currentUser = currentUser,
                    schoolName = schoolSettings?.schoolName ?: "Hein Chan Myae",
                    schoolLogoUri = schoolSettings?.logoUri,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(ScreenRoute.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.statusBars,
                topBar = {
                    HcmTopAppBar(
                        schoolName = schoolSettings?.schoolName ?: "Hein Chan Myae",
                        academicYear = academicYear,
                        availableYears = availableAcademicYearCodes,
                        currentUser = currentUser,
                        schoolLogoUri = schoolSettings?.logoUri,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onAcademicYearChanged = { year ->
                            android.util.Log.d("AcademicYearDebug", "MAIN_ACTIVITY_EVENT: onAcademicYearChanged called with year='$year'")
                            academicYearViewModel.activateAcademicYearByCode(year)
                            schoolSettings?.let { setting ->
                                policyViewModel.updateSchoolSettings(
                                    schoolName = setting.schoolName,
                                    academicYear = year,
                                    phone = setting.contactPhone,
                                    email = setting.email,
                                    address = setting.address
                                )
                            }
                        },
                        onLogoutClicked = { authViewModel.logout(context) },
                        onRoleSwitchClicked = { role -> authViewModel.loginAsRole(role) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                        .consumeWindowInsets(WindowInsets.ime)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ScreenRoute.Dashboard.route
                    ) {
                        composable(ScreenRoute.Dashboard.route) {
                            DashboardScreen(
                                currentUser = currentUser,
                                dashboardViewModel = dashboardViewModel,
                                schoolName = schoolSettings?.schoolName ?: "Hein Chan Myae",
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }

                        composable(ScreenRoute.Students.route) {
                            StudentManagementScreen(
                                students = students,
                                grades = grades,
                                searchQuery = searchQuery,
                                selectedGradeFilter = selectedGradeFilter,
                                selectedClassFilter = selectedClassFilter,
                                selectedStatusFilter = selectedStatusFilter,
                                onSearchQueryChanged = { studentViewModel.onSearchQueryChanged(it) },
                                onGradeFilterChanged = { studentViewModel.setGradeFilter(it) },
                                onClassFilterChanged = { studentViewModel.setClassFilter(it) },
                                onStatusFilterChanged = { studentViewModel.setStatusFilter(it) },
                                onSaveStudent = { studentViewModel.saveStudent(it) },
                                onDeleteStudent = { studentViewModel.deleteStudent(it) },
                                onImportMockData = { studentViewModel.importMockData() },
                                academicYear = academicYear,
                                currentUser = currentUser,
                                deletionVerificationEvent = studentViewModel.deletionVerificationEvent
                            )
                        }

                        composable(ScreenRoute.Teachers.route) {
                            TeacherManagementScreen(
                                teacherViewModel = teacherViewModel,
                                currentUser = currentUser,
                                policyViewModel = policyViewModel
                            )
                        }


                        composable(ScreenRoute.PolicyCenter.route) {
                            SchoolPolicyCenterScreen(
                                policyViewModel = policyViewModel,
                                authViewModel = authViewModel
                            )
                        }

                        composable(ScreenRoute.Users.route) {
                            val teachersList by teacherViewModel.teachersList.collectAsState()
                            UsersScreen(
                                authViewModel = authViewModel,
                                teachers = teachersList
                            )
                        }

                        composable(ScreenRoute.Settings.route) {
                            SettingsScreen(
                                viewModel = systemSettingsViewModel,
                                currentUserRole = currentUser?.role ?: UserRole.SUPER_ADMIN,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme
                            )
                        }

                        composable(ScreenRoute.About.route) {
                            AboutScreen(
                                schoolName = schoolSettings?.schoolName ?: "Hein Chan Myae",
                                schoolLogoUri = schoolSettings?.logoUri
                            )
                        }

                        // Module 3, 4 & 5
                        composable(ScreenRoute.Assessment.route) {
                            AssessmentManagementScreen(
                                assessmentViewModel = assessmentViewModel,
                                currentUser = currentUser,
                                academicYear = academicYear,
                                availableAcademicYears = availableAcademicYearCodes,
                                policyViewModel = policyViewModel
                            )
                        }
                        composable(ScreenRoute.MarksEntry.route) {
                            MarksEntryScreen(
                                marksViewModel = marksViewModel,
                                currentUser = currentUser,
                                availableAcademicYears = availableAcademicYearCodes,
                                policyViewModel = policyViewModel
                            )
                        }
                        composable(ScreenRoute.ExamRanking.route) {
                            ExamRankingScreen(
                                viewModel = examRankingViewModel,
                                schoolName = schoolSettings?.schoolName ?: "Hein Chan Myae Private School",
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable(ScreenRoute.HolisticAssessment.route) {
                            HolisticAssessmentScreen(
                                holisticViewModel = holisticViewModel,
                                currentUser = currentUser,
                                availableAcademicYears = availableAcademicYearCodes,
                                policyViewModel = policyViewModel
                            )
                        }
                        composable(ScreenRoute.Attendance.route) {
                            AttendanceManagementScreen(
                                attendanceViewModel = attendanceViewModel,
                                currentUser = currentUser,
                                availableAcademicYears = availableAcademicYearCodes,
                                policyViewModel = policyViewModel
                            )
                        }
                        composable(ScreenRoute.AcademicYear.route) {
                            AcademicYearPromotionScreen(
                                academicYearViewModel = academicYearViewModel,
                                currentUser = currentUser,
                                policyViewModel = policyViewModel
                            )
                        }
                        composable(ScreenRoute.AiAssistant.route) {
                            AiAssistantScreen(
                                viewModel = aiViewModel,
                                currentUser = currentUser,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable(ScreenRoute.Reports.route) {
                            ReportCardsScreen(
                                reportViewModel = reportViewModel,
                                availableAcademicYears = availableAcademicYearCodes
                            )
                        }
                    }
                }
            }
        }
    }

    // Force Update Dialog Overlay (Applies to both login and logged-in states)
    updateInfo?.let { info ->
        AppUpdateDialog(
            updateInfo = info,
            onDismiss = {
                updateInfo = null
            }
        )
    }
}

@Composable
fun ModulePlaceholderScreen(moduleTitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = moduleTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Module 3 Placeholder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Module 1 (Project Foundation) & Module 2 (Grade Config, Subjects, Exams, Policy & Student Management) are fully implemented.\n\n$moduleTitle will be integrated in Module 3.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
