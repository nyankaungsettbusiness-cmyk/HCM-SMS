package com.example.ui.screens.ranking

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssessmentEntity
import com.example.ui.viewmodel.ExamRankingViewModel
import com.example.ui.viewmodel.RankingResultData
import com.example.ui.viewmodel.StudentRankingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamRankingScreen(
    viewModel: ExamRankingViewModel,
    schoolName: String = "Hein Chan Myae Private School",
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedAcademicYear by viewModel.selectedAcademicYear.collectAsState()
    val selectedGrade by viewModel.selectedGrade.collectAsState()
    val selectedAssessmentId by viewModel.selectedAssessmentId.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()

    val availableYears by viewModel.availableAcademicYears.collectAsState()
    val availableGrades = viewModel.availableGrades
    val availableAssessments by viewModel.availableAssessments.collectAsState()
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val rankingData by viewModel.rankingData.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStudentDetail by remember { mutableStateOf<StudentRankingItem?>(null) }

    var isExportingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val isHighSchool = selectedGrade in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12")

    val filteredRankingItems = remember(rankingData?.rankingItems, searchQuery) {
        val items = rankingData?.rankingItems ?: emptyList()
        if (searchQuery.isBlank()) items
        else {
            items.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.studentName.contains(searchQuery, ignoreCase = true) ||
                        it.studentCode.contains(searchQuery, ignoreCase = true) ||
                        it.rollNo.toString() == searchQuery.trim()
            }
        }
    }

    val totalStudents = rankingData?.rankingItems?.size ?: 0
    val passCount = rankingData?.rankingItems?.count { it.isPassed } ?: 0
    val failCount = totalStudents - passCount
    val passPercentage = if (totalStudents > 0) String.format("%.1f", (passCount.toDouble() / totalStudents) * 100) else "0.0"
    val totalDistinctions = rankingData?.rankingItems?.sumOf { it.distinctionCount } ?: 0

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
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
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Exam Ranking & Results",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredRankingItems.size} students ranked",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Compact Export PDF Action
                if (rankingData != null && rankingData!!.rankingItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isExportingPdf = true
                                val file = withContext(Dispatchers.IO) {
                                    try {
                                        ExamRankingPdfGenerator.generatePdf(context, schoolName, rankingData!!)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                }
                                isExportingPdf = false
                                if (file != null) {
                                    generatedPdfFile = file
                                    showPdfDialog = true
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF report", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isExportingPdf,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exporting", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Compact Stat Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RankingCompactStatChip("Ranked", totalStudents.toString(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                RankingCompactStatChip("Pass Rate", "$passPercentage%", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
                RankingCompactStatChip("Pass / Fail", "$passCount / $failCount", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                RankingCompactStatChip("Distinctions", "${totalDistinctions}D", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }

            // Search and Filters Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student name or roll no...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Compact Filter Chips Horizontal Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RankingFilterDropdownChip(
                        label = "Year: $selectedAcademicYear",
                        options = availableYears,
                        selectedOption = selectedAcademicYear,
                        onOptionSelected = { viewModel.setAcademicYear(it) }
                    )
                    RankingFilterDropdownChip(
                        label = "Grade: $selectedGrade",
                        options = availableGrades,
                        selectedOption = selectedGrade,
                        onOptionSelected = { viewModel.setGrade(it) }
                    )
                    RankingExamDropdownChip(
                        label = "Exam: ${rankingData?.assessment?.assessmentName ?: "Select Exam"}",
                        assessments = availableAssessments,
                        selectedAssessmentId = selectedAssessmentId,
                        onAssessmentSelected = { viewModel.setAssessmentId(it) }
                    )
                    RankingFilterDropdownChip(
                        label = "Subject: $selectedSubject",
                        options = availableSubjects,
                        selectedOption = selectedSubject,
                        onOptionSelected = { viewModel.setSubject(it) }
                    )
                }
            }

            // High School combined stream note
            if (isHighSchool) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "High School ($selectedGrade): STEAMS-1 & STEAMS-2 combined ranking. Eco students marked with '(Eco)'.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Compact Table List Card
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
                        Text("Rank", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                        Text("Student Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                        Text("Class/Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Text("Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Text("Pct", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f))
                        Text("Result", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Table Body
                    if (rankingData == null || rankingData?.assessment == null) {
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
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                                Text("Select a Grade and Exam with marks to view ranking.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else if (filteredRankingItems.isEmpty()) {
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
                                Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                                Text("No student records found.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredRankingItems, key = { it.studentId }) { item ->
                                RankingTableRow(
                                    item = item,
                                    selectedGrade = selectedGrade,
                                    onClick = { selectedStudentDetail = item }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Student Detail Dialog
    if (selectedStudentDetail != null) {
        val item = selectedStudentDetail!!
        val pct = if (item.totalMax > 0) (item.totalObtained / item.totalMax * 100.0) else 0.0

        AlertDialog(
            onDismissRequest = { selectedStudentDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Roll #${item.rollNo} • ${item.studentCode} • Rank #${item.rank}", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { selectedStudentDetail = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary Specifications
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Class Rank:", fontSize = 11.sp, color = Color.Gray)
                                Text("#${item.rank}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Marks:", fontSize = 11.sp, color = Color.Gray)
                                Text("${if (item.totalObtained % 1.0 == 0.0) item.totalObtained.toInt() else String.format("%.1f", item.totalObtained)} / ${item.totalMax}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Percentage:", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.1f", pct)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Result Status:", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = "${item.resultStatus} (${item.distinctionText})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isPassed) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                )
                            }
                        }
                    }

                    // Subject Marks Breakdown Table
                    Text("Subject Marks Breakdown", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Subject", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                Text("Score", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }
                            HorizontalDivider()

                            val displaySubjects = rankingData?.subjects ?: emptyList()
                            displaySubjects.forEach { sub ->
                                val scoreVal = item.subjectMarks[sub]
                                val scoreStr = if (scoreVal != null) {
                                    if (scoreVal % 1.0 == 0.0) scoreVal.toInt().toString() else String.format("%.1f", scoreVal)
                                } else "-"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sub, fontSize = 11.sp, modifier = Modifier.weight(2f))
                                    Text(scoreStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedStudentDetail = null },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Close", fontSize = 11.sp)
                }
            }
        )
    }

    // PDF Export Ready Dialog
    if (showPdfDialog && generatedPdfFile != null) {
        val pdfFile = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            icon = {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text(text = "PDF Export Ready", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exam Ranking PDF report has been generated successfully:", fontSize = 12.sp)
                    Text(
                        text = pdfFile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Size: ${pdfFile.length() / 1024} KB",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                pdfFile
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open Exam Ranking PDF"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "PDF saved at: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
                        }
                        showPdfDialog = false
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Open PDF", fontSize = 11.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPdfDialog = false },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Close", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun RankingCompactStatChip(
    title: String,
    value: String,
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
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingFilterDropdownChip(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedOption != "ALL" && selectedOption != "2026-2027",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingExamDropdownChip(
    label: String,
    assessments: List<AssessmentEntity>,
    selectedAssessmentId: Long?,
    onAssessmentSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedAssessmentId != null,
            onClick = { expanded = true },
            label = { Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
            modifier = Modifier.height(32.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (assessments.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No assessments available", fontSize = 12.sp) },
                    onClick = { expanded = false }
                )
            } else {
                assessments.forEach { asm ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(asm.assessmentName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${asm.grade} (${asm.className}) • ${asm.assessmentType} • Max ${asm.maxMarks}", fontSize = 10.sp, color = Color.Gray)
                            }
                        },
                        onClick = {
                            onAssessmentSelected(asm.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingTableRow(
    item: StudentRankingItem,
    selectedGrade: String,
    onClick: () -> Unit
) {
    val pct = if (item.totalMax > 0) (item.totalObtained / item.totalMax * 100.0) else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge / Text
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val (badgeBg, badgeFg) = when (item.rank) {
                1 -> Color(0xFFFFD700) to Color.Black
                2 -> Color(0xFFC0C0C0) to Color.Black
                3 -> Color(0xFFCD7F32) to Color.White
                else -> Color.Transparent to MaterialTheme.colorScheme.onSurface
            }

            if (item.rank <= 3) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = "#${item.rank}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "#${item.rank}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Student Name & Roll No
        Column(modifier = Modifier.weight(2.2f)) {
            Text(
                text = item.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Roll #${item.rollNo} • Code: ${item.studentCode}",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Grade / Class / Stream
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = selectedGrade,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (item.stream.isNotBlank()) {
                Text(
                    text = item.stream,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }

        // Total Score
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = "${if (item.totalObtained % 1.0 == 0.0) item.totalObtained.toInt() else String.format("%.1f", item.totalObtained)} / ${item.totalMax}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        // Percentage
        Text(
            text = "${String.format("%.1f", pct)}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.0f),
            maxLines = 1
        )

        // Result Status Badge
        Box(modifier = Modifier.weight(1.2f)) {
            val (statusBg, statusFg) = if (item.isPassed) {
                Pair(Color(0xFFC8E6C9), Color(0xFF1B5E20))
            } else {
                Pair(Color(0xFFFFCDD2), Color(0xFFB71C1C))
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusBg
            ) {
                Text(
                    text = if (item.distinctionCount > 0) "${item.resultStatus} (${item.distinctionText})" else item.resultStatus,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusFg,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
