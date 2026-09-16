package com.example.ui.screens.reports

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ReportTemplateType
import com.example.data.local.entity.StudentEntity
import com.example.data.policy.SchoolPolicy
import com.example.data.report.GeneratedReportCardData
import com.example.data.report.ReportAssessmentVisibilityManager
import com.example.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReportCrashDetails(
    val exceptionType: String,
    val exceptionMessage: String,
    val fileName: String,
    val lineNumber: Int,
    val functionName: String,
    val fullStackTrace: String
)

object ReportCrashHandler {
    var caughtCrashDetails by mutableStateOf<ReportCrashDetails?>(null)

    fun captureException(e: Throwable) {
        val stackTrace = e.stackTrace
        val relevantElement = stackTrace.firstOrNull { 
            it.className.contains("com.example") 
        } ?: stackTrace.firstOrNull()

        val details = ReportCrashDetails(
            exceptionType = e.javaClass.canonicalName ?: e.javaClass.name,
            exceptionMessage = e.message ?: "No error message provided",
            fileName = relevantElement?.fileName ?: "Unknown file",
            lineNumber = relevantElement?.lineNumber ?: -1,
            functionName = relevantElement?.methodName ?: "Unknown function",
            fullStackTrace = e.stackTraceToString()
        )
        caughtCrashDetails = details
        Log.e("ReportModuleCrash", "CRASH IN REPORT MODULE:\nType: ${details.exceptionType}\nMsg: ${details.exceptionMessage}\nFile: ${details.fileName}:${details.lineNumber}\nFunc: ${details.functionName}\n${details.fullStackTrace}", e)
    }

    fun setupGlobalHandler() {
        val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            captureException(throwable)
            existingHandler?.uncaughtException(thread, throwable)
        }
    }
}

@Composable
fun ReportCrashDisplay(
    crashDetails: ReportCrashDetails,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Report Module Runtime Exception",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f))

            Text(
                text = "Exception Type: ${crashDetails.exceptionType}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Message: ${crashDetails.exceptionMessage}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "File Name: ${crashDetails.fileName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Line Number: ${if (crashDetails.lineNumber > 0) crashDetails.lineNumber else "N/A"}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Function Name: ${crashDetails.functionName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Full Stack Trace:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = crashDetails.fullStackTrace,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.Green,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCardsScreen(
    reportViewModel: ReportViewModel,
    availableAcademicYears: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        ReportCrashHandler.setupGlobalHandler()
    }

    val activeCrash = ReportCrashHandler.caughtCrashDetails
    if (activeCrash != null) {
        ReportCrashDisplay(crashDetails = activeCrash, modifier = modifier)
        return
    }

    ReportCardsScreenContent(
        reportViewModel = reportViewModel,
        availableAcademicYears = availableAcademicYears,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportCardsScreenContent(
    reportViewModel: ReportViewModel,
    availableAcademicYears: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val students by reportViewModel.students.collectAsState()
    val selectedGradeFilter by reportViewModel.selectedGradeFilter.collectAsState()
    val selectedStudent by reportViewModel.selectedStudent.collectAsState()
    val selectedMonth by reportViewModel.selectedMonth.collectAsState()
    val academicYear by reportViewModel.academicYear.collectAsState()

    val liveData by reportViewModel.liveSourceData.collectAsState()
    val archivedData by reportViewModel.archivedReportData.collectAsState()

    val hasSourceDataChanged by reportViewModel.hasSourceDataChanged.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showPreviewModal by remember { mutableStateOf(false) }
    var showTestManagerModal by remember { mutableStateOf(false) }
    val visibilityTrigger by reportViewModel.visibilityTrigger.collectAsState()

    fun showMessage(msg: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isGeneratingAiComment by reportViewModel.isGeneratingAiComment.collectAsState()
    val aiReportCommentResult by reportViewModel.aiReportCommentResult.collectAsState()
    val aiErrorMessage by reportViewModel.aiErrorMessage.collectAsState()

    LaunchedEffect(aiErrorMessage) {
        aiErrorMessage?.let {
            showMessage(it)
            reportViewModel.clearAiErrorMessage()
        }
    }

    val filteredStudents = remember(students, selectedGradeFilter) {
        val list = if (selectedGradeFilter == "All") {
            students
        } else {
            students.filter { it.gradeName.equals(selectedGradeFilter, ignoreCase = true) }
        }
        list.distinctBy { if (it.studentCode.isNotBlank()) it.studentCode else "${it.name}_${it.gradeName}_${it.className}_${it.id}" }
    }

    LaunchedEffect(filteredStudents, selectedStudent) {
        if (selectedStudent == null && filteredStudents.isNotEmpty()) {
            reportViewModel.selectStudent(filteredStudents.first())
        } else if (selectedStudent != null && filteredStudents.isNotEmpty() && filteredStudents.none { it.id == selectedStudent?.id }) {
            reportViewModel.selectStudent(filteredStudents.firstOrNull())
        }
    }

    val availableMonths = remember {
        listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
    }

    val availableYears = remember(availableAcademicYears, academicYear) {
        if (availableAcademicYears.isNotEmpty()) {
            (availableAcademicYears + academicYear).filter { it.isNotBlank() }.distinct()
        } else {
            listOf(academicYear).filter { it.isNotBlank() }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Report Cards Archive",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (archivedData != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "PDF Archived",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else if (liveData != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Live PDF Ready",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Selectors Layout (Filters)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 640.dp

                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AcademicYearSelector(
                                        academicYear = academicYear,
                                        availableYears = availableYears,
                                        onYearSelected = { reportViewModel.setAcademicYear(it) },
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    GradeSelector(
                                        selectedGradeFilter = selectedGradeFilter,
                                        onGradeSelected = { reportViewModel.setSelectedGradeFilter(it) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StudentSelector(
                                        selectedStudent = selectedStudent,
                                        filteredStudents = filteredStudents,
                                        onStudentSelected = { reportViewModel.selectStudent(it) },
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    MonthSelector(
                                        selectedMonth = selectedMonth,
                                        availableMonths = availableMonths,
                                        onMonthSelected = { reportViewModel.setSelectedMonth(it) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AcademicYearSelector(
                                    academicYear = academicYear,
                                    availableYears = availableYears,
                                    onYearSelected = { reportViewModel.setAcademicYear(it) },
                                    modifier = Modifier.weight(1.1f)
                                )
                                GradeSelector(
                                    selectedGradeFilter = selectedGradeFilter,
                                    onGradeSelected = { reportViewModel.setSelectedGradeFilter(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                StudentSelector(
                                    selectedStudent = selectedStudent,
                                    filteredStudents = filteredStudents,
                                    onStudentSelected = { reportViewModel.selectStudent(it) },
                                    modifier = Modifier.weight(1.4f)
                                )
                                MonthSelector(
                                    selectedMonth = selectedMonth,
                                    availableMonths = availableMonths,
                                    onMonthSelected = { reportViewModel.setSelectedMonth(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area
            val data = archivedData ?: liveData
            val currentStudent = selectedStudent
            val context = LocalContext.current

            // Safely generate PDF file asynchronously on Dispatchers.IO
            val pdfFileState = produceState<File?>(initialValue = null, key1 = data, key2 = visibilityTrigger) {
                if (data == null) {
                    value = null
                } else {
                    val file = withContext(Dispatchers.IO) {
                        try {
                            ReportCardGenerator(context).generateReportCardPdf(data)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            null
                        }
                    }
                    if (file != null) {
                        value = file
                    }
                }
            }
            val pdfFile = pdfFileState.value

            if (currentStudent == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Please select a student to view or generate their report card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (data == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "No Report Card Available",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No assessment marks, attendance entries, or HCM assessment records were found for '${currentStudent.name}' in $academicYear ($selectedMonth).\n\nA report card cannot be generated without assessment, attendance, or HCM data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (pdfFile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Rendering PDF report card for ${currentStudent.name}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // PDF / Report Card Data ready -> Action toolbar and PDF preview
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action Toolbar: Preview / Download / Print action buttons (Scrollable Row for phone/compact screens)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                showPreviewModal = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                reportViewModel.logPdfExport(data)
                                val (success, message) = ReportCardGenerator(context).saveToDownloads(pdfFile, data)
                                showMessage(message)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showTestManagerModal = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage Tests", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                reportViewModel.logPrint(data)
                                printPdfFile(context, pdfFile, "ReportCard_${data.student.studentCode}")
                                showMessage("Printing Report Card...")
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                reportViewModel.generateAiCommentForSelectedStudent(language = "MYANMAR")
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isGeneratingAiComment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generating...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Comment", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    // Source Data Changed Warning Banner
                    if (hasSourceDataChanged) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Report data has changed.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Regenerate report?",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        reportViewModel.regenerateReport()
                                        showMessage("Report Card regenerated with latest module data.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiaryContainer)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Regenerate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiaryContainer)
                                }
                            }
                        }
                    }

                    // PDF Preview
                    if (pdfFile != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            PdfFilePreview(
                                pdfFile = pdfFile,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unable to generate PDF preview.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Full-Screen Preview Dialog
            if (showPreviewModal && data != null && pdfFile != null) {
                Dialog(
                    onDismissRequest = { showPreviewModal = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Dialog Top Header Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PDF Document - ${data.student.name} (${data.selectedMonth})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { showTestManagerModal = true }) {
                                        Icon(Icons.Default.Tune, contentDescription = "Manage Tests")
                                    }
                                    IconButton(
                                        onClick = {
                                            reportViewModel.logPdfExport(data)
                                            val (success, message) = ReportCardGenerator(context).saveToDownloads(pdfFile, data)
                                            showMessage(message)
                                        }
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download PDF")
                                    }
                                    IconButton(
                                        onClick = {
                                            reportViewModel.logPrint(data)
                                            printPdfFile(context, pdfFile, "ReportCard_${data.student.studentCode}")
                                            showMessage("Printing Report Card...")
                                        }
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Print PDF")
                                    }
                                    IconButton(onClick = { showPreviewModal = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Preview")
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Full Page Preview Content Rendered from PDF File
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfFilePreview(
                                    pdfFile = pdfFile,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            if (showTestManagerModal) {
                TestVisibilityDialog(
                    reportViewModel = reportViewModel,
                    onDismiss = { showTestManagerModal = false }
                )
            }

            // AI Report Card Comment Review Dialog
            val activeAiResult = aiReportCommentResult
            if (activeAiResult != null) {
                AiReportCommentReviewDialog(
                    reportViewModel = reportViewModel,
                    facts = activeAiResult.first,
                    aiResult = activeAiResult.second,
                    onDismiss = { reportViewModel.dismissAiReviewDialog() },
                    onSavedSuccessfully = {
                        showMessage("Teacher comment & parent suggestion saved to Report Card.")
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademicYearSelector(
    academicYear: String,
    availableYears: List<String>,
    onYearSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = academicYear,
            onValueChange = {},
            readOnly = true,
            label = { Text("Academic Year", fontSize = 10.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("2025–2026", "2024–2025").forEach { yr ->
                DropdownMenuItem(
                    text = { Text(yr, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    onClick = {
                        onYearSelected(yr)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeSelector(
    selectedGradeFilter: String,
    onGradeSelected: (String) -> Unit,
    availableGrades: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayGradeText = if (selectedGradeFilter == "All") "All Grades" else "Grade $selectedGradeFilter"
    val gradesToDisplay = if (availableGrades.isNotEmpty()) availableGrades else SchoolPolicy.VALID_GRADES

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayGradeText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Grade", fontSize = 10.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Grades", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                onClick = {
                    onGradeSelected("All")
                    expanded = false
                }
            )
            gradesToDisplay.forEach { g ->
                DropdownMenuItem(
                    text = { Text("Grade $g", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    onClick = {
                        onGradeSelected(g)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentSelector(
    selectedStudent: StudentEntity?,
    filteredStudents: List<StudentEntity>,
    onStudentSelected: (StudentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val studentText = selectedStudent?.name ?: "Select Student"

    Box(modifier = modifier) {
        OutlinedTextField(
            value = studentText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Student", fontSize = 10.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (filteredStudents.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No students found", fontSize = 13.sp) },
                    onClick = { expanded = false }
                )
            } else {
                filteredStudents.forEach { st ->
                    DropdownMenuItem(
                        text = { Text("${st.name} (${st.gradeName})", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            onStudentSelected(st)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthSelector(
    selectedMonth: String,
    availableMonths: List<String>,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedMonth,
            onValueChange = {},
            readOnly = true,
            label = { Text("Month", fontSize = 10.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableMonths.forEach { mMonth ->
                DropdownMenuItem(
                    text = { Text(mMonth, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    onClick = {
                        onMonthSelected(mMonth)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TestVisibilityDialog(
    reportViewModel: ReportViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var monthlyVisible by remember { mutableStateOf(ReportAssessmentVisibilityManager.getVisibility(context, ReportAssessmentVisibilityManager.KEY_MONTHLY_TEST)) }
    var weeklyVisible by remember { mutableStateOf(ReportAssessmentVisibilityManager.getVisibility(context, ReportAssessmentVisibilityManager.KEY_WEEKLY_TEST)) }
    var pilotVisible by remember { mutableStateOf(ReportAssessmentVisibilityManager.getVisibility(context, ReportAssessmentVisibilityManager.KEY_PILOT_TEST)) }
    var cetVisible by remember { mutableStateOf(ReportAssessmentVisibilityManager.getVisibility(context, ReportAssessmentVisibilityManager.KEY_CET_TEST)) }
    var lctVisible by remember { mutableStateOf(ReportAssessmentVisibilityManager.getVisibility(context, ReportAssessmentVisibilityManager.KEY_LCT)) }
    var customVisible by remember { mutableStateOf(ReportAssessmentVisibilityManager.getVisibility(context, ReportAssessmentVisibilityManager.KEY_CUSTOM_EXAM)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage Test Visibility",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select which assessment sections to include on the report card:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                VisibilityToggleRow(
                    label = "Monthly Assessment",
                    checked = monthlyVisible,
                    onCheckedChange = {
                        monthlyVisible = it
                        ReportAssessmentVisibilityManager.setVisibility(context, ReportAssessmentVisibilityManager.KEY_MONTHLY_TEST, it)
                        reportViewModel.refreshVisibility()
                    }
                )

                VisibilityToggleRow(
                    label = "Weekly Test",
                    checked = weeklyVisible,
                    onCheckedChange = {
                        weeklyVisible = it
                        ReportAssessmentVisibilityManager.setVisibility(context, ReportAssessmentVisibilityManager.KEY_WEEKLY_TEST, it)
                        reportViewModel.refreshVisibility()
                    }
                )

                VisibilityToggleRow(
                    label = "Pilot Test",
                    checked = pilotVisible,
                    onCheckedChange = {
                        pilotVisible = it
                        ReportAssessmentVisibilityManager.setVisibility(context, ReportAssessmentVisibilityManager.KEY_PILOT_TEST, it)
                        reportViewModel.refreshVisibility()
                    }
                )

                VisibilityToggleRow(
                    label = "CET Exam",
                    checked = cetVisible,
                    onCheckedChange = {
                        cetVisible = it
                        ReportAssessmentVisibilityManager.setVisibility(context, ReportAssessmentVisibilityManager.KEY_CET_TEST, it)
                        reportViewModel.refreshVisibility()
                    }
                )

                VisibilityToggleRow(
                    label = "Lesson Completion Test (LCT)",
                    checked = lctVisible,
                    onCheckedChange = {
                        lctVisible = it
                        ReportAssessmentVisibilityManager.setVisibility(context, ReportAssessmentVisibilityManager.KEY_LCT, it)
                        reportViewModel.refreshVisibility()
                    }
                )

                VisibilityToggleRow(
                    label = "Custom / International Exam",
                    checked = customVisible,
                    onCheckedChange = {
                        customVisible = it
                        ReportAssessmentVisibilityManager.setVisibility(context, ReportAssessmentVisibilityManager.KEY_CUSTOM_EXAM, it)
                        reportViewModel.refreshVisibility()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun VisibilityToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


