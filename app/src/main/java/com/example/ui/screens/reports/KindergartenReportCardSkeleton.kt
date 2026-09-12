package com.example.ui.screens.reports

import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.report.ReportAssessmentVisibilityManager
import com.example.data.report.GeneratedReportCardData
import com.example.data.report.DynamicExamCategoryTable
import com.example.data.policy.SchoolPolicy

private val PrimaryPrintHeaderColor = Color(0xFF5E507A)

/**
 * Dedicated A4 Portrait Printable Report Card Document.
 * Designed specifically for paper export, printing, and crisp A4 preview.
 */
@Composable
fun KindergartenReportCardSkeleton(
    data: GeneratedReportCardData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================= A4 PAGE 1 =================
        A4PaperSheet(pageNumber = 1, totalPages = 2) {
            // 1. School Header
            val logoBitmap = com.example.ui.util.rememberSchoolLogo()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap,
                            contentDescription = "School Logo",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "School Emblem",
                            tint = PrimaryPrintHeaderColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "HEIN CHAN MYAE PRIVATE SCHOOL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPrintHeaderColor,
                        letterSpacing = 0.5.sp
                    )
                }
                val reportTitleText = when {
                    data.gradeName.equals("KG", ignoreCase = true) || data.ruleResult.selectedTemplate.name.contains("KINDERGARTEN", ignoreCase = true) ->
                        "KINDERGARTEN PROGRESS REPORT CARD"
                    data.gradeName.contains("G1") || data.gradeName.contains("G2") || data.gradeName.contains("G3") || data.gradeName.contains("G4") || data.gradeName.contains("G5") ->
                        "PRIMARY PROGRESS REPORT CARD"
                    data.gradeName.contains("G6") || data.gradeName.contains("G7") || data.gradeName.contains("G8") || data.gradeName.contains("G9") ->
                        "SECONDARY PROGRESS REPORT CARD"
                    else ->
                        "${data.gradeName.uppercase()} PROGRESS REPORT CARD"
                }
                Text(
                    text = reportTitleText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151),
                    letterSpacing = 0.2.sp
                )
                Text(
                    text = "Academic Year: ${data.academicYear}   |   Report Month: ${data.selectedMonth}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
            }

            PrintDivider()

            // 2. Student Information (Compact Two-Column Table - Roll Number removed)
            PrintSectionTitle(title = "1. Student Information")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    PrintCellKeyVal("Student Name:", data.student.name, Modifier.weight(1.2f))
                    PrintCellKeyVal("Student ID:", data.student.studentCode, Modifier.weight(1f))
                }
                HorizontalDivider(color = Color(0xFFD1D5DB))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    PrintCellKeyVal("Grade / Class:", "${data.student.gradeName} / ${data.className}", Modifier.weight(1.2f))
                    PrintCellKeyVal("Parent Name:", data.student.parentName, Modifier.weight(1f))
                }
            }

            // 3. Attendance (Compact Table)
            PrintSectionTitle(title = "2. Attendance Record")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryPrintHeaderColor)
                        .padding(vertical = 3.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PrintTableHeaderCell("Total Days", Modifier.weight(1f))
                    PrintTableHeaderCell("Present Days", Modifier.weight(1f))
                    PrintTableHeaderCell("Absent Days", Modifier.weight(1f))
                    PrintTableHeaderCell("Leave Days", Modifier.weight(1f))
                    PrintTableHeaderCell("Attendance %", Modifier.weight(1.2f))
                }
                HorizontalDivider(color = Color(0xFF9CA3AF))
                // Table Data Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrintTableCell("${data.attendanceSummary.monthTotalDays}", Modifier.weight(1f))
                    PrintTableCell("${data.attendanceSummary.monthPresentDays}", Modifier.weight(1f))
                    PrintTableCell("${data.attendanceSummary.monthAbsentDays}", Modifier.weight(1f))
                    PrintTableCell("${data.attendanceSummary.monthLeaveDays}", Modifier.weight(1f))
                    PrintTableCell(
                        text = "${data.attendanceSummary.monthAttendancePercentage}%",
                        modifier = Modifier.weight(1.2f),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPrintHeaderColor
                    )
                }
            }

            // 4. SGI (Student Growth Index) - Ultra Compact, ONE SINGLE LINE
            PrintSectionTitle(title = "3. Student Growth Index (SGI)")
            val sgiAcadPct = data.kgSgiSummary?.academicComponentPct?.toInt() ?: 92
            val sgiAttPct = data.kgSgiSummary?.attendanceComponentPct?.toInt() ?: data.attendanceSummary.monthAttendancePercentage.toInt()
            val sgiHcmPct = data.kgSgiSummary?.hcmComponentPct?.toInt() ?: 94
            val sgiOverallPct = data.kgSgiSummary?.overallSgiPercentage?.toInt() ?: 94

            Text(
                text = "SGI | Academic $sgiAcadPct% | Attendance $sgiAttPct% | HCM $sgiHcmPct% | Overall $sgiOverallPct%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPrintHeaderColor,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )

            // 5. Academic Assessment (Compact Marks Table)
            PrintSectionTitle(title = "4. Academic Assessment")
            val reportMonthTitle = if (data.selectedMonth.contains("Assessment", ignoreCase = true)) {
                data.selectedMonth
            } else {
                "${data.selectedMonth.ifEmpty { "June" }} Assessment"
            }

            val monthlyTable = data.dynamicExamTables.find { it.categoryKey == "MONTHLY_TESTS" }
                ?: data.dynamicExamTables.firstOrNull()

            val normGrade = data.gradeName.trim().lowercase()
            val isKg = normGrade == "kg" || normGrade.contains("kindergarten") || normGrade.contains("preschool")
            val isG1to3 = normGrade.contains("g1") || normGrade.contains("g2") || normGrade.contains("g3") ||
                    normGrade.contains("grade 1") || normGrade.contains("grade 2") || normGrade.contains("grade 3")
            val isG4 = normGrade.contains("g4") || normGrade.contains("grade 4")
            val isG5 = normGrade == "g5" || normGrade.contains("g5") || normGrade.contains("grade 5") || normGrade.contains("primary 5")
            val isG6to9 = normGrade.contains("g6") || normGrade.contains("g7") || normGrade.contains("g8") || normGrade.contains("g9") ||
                    normGrade.contains("grade 6") || normGrade.contains("grade 7") || normGrade.contains("grade 8") || normGrade.contains("grade 9")
            val isG10to12 = normGrade.contains("g10") || normGrade.contains("g11") || normGrade.contains("g12") ||
                    normGrade.contains("grade 10") || normGrade.contains("grade 11") || normGrade.contains("grade 12")

            val primarySubjects = listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")

            val showMonthly = ReportAssessmentVisibilityManager.isTypeVisible(context, "MONTHLY")
            val showWeekly = ReportAssessmentVisibilityManager.isTypeVisible(context, "WEEKLY")
            val showPilot = ReportAssessmentVisibilityManager.isTypeVisible(context, "PILOT")
            val showCet = ReportAssessmentVisibilityManager.isTypeVisible(context, "CET")
            val showLct = ReportAssessmentVisibilityManager.isTypeVisible(context, "LCT")
            val showCustom = ReportAssessmentVisibilityManager.isTypeVisible(context, "CUSTOM")

            fun hasValidScores(table: DynamicExamCategoryTable?): Boolean {
                if (table == null || table.categoryKey == "NO_DATA") return false
                return table.subjectRows.any { r ->
                    (r.maxPossible > 0 && r.totalObtained > 0.0) || r.examScores.values.any { it != null && it > 0.0 }
                }
            }

            val hasMonthly = data.dynamicExamTables.any {
                (it.categoryKey.uppercase().contains("MONTHLY") || it.categoryTitle.uppercase().contains("MONTHLY")) &&
                        !it.categoryKey.uppercase().contains("CET") && !it.categoryKey.uppercase().contains("PILOT") &&
                        hasValidScores(it)
            } || hasValidScores(monthlyTable)

            val hasWeekly = data.dynamicExamTables.any {
                (it.categoryKey.uppercase().contains("WEEKLY") || it.categoryTitle.uppercase().contains("WEEKLY") || it.categoryKey.uppercase().contains("WT")) &&
                        hasValidScores(it)
            }

            val hasPilot = data.dynamicExamTables.any {
                (it.categoryKey.uppercase().contains("PILOT") || it.categoryTitle.uppercase().contains("PILOT")) &&
                        hasValidScores(it)
            }

            val hasCet = data.dynamicExamTables.any {
                (it.categoryKey.uppercase().contains("CET") || it.categoryTitle.uppercase().contains("CET")) &&
                        hasValidScores(it)
            }

            val hasLct = data.dynamicExamTables.any {
                (it.categoryKey.uppercase().contains("LESSON") || it.categoryKey.uppercase().contains("UNIT") ||
                        it.categoryTitle.uppercase().contains("LESSON") || it.categoryTitle.uppercase().contains("UNIT") ||
                        it.categoryTitle.uppercase().contains("LCT")) &&
                        hasValidScores(it)
            }

            val hasCustom = data.dynamicExamTables.any {
                (it.categoryKey.uppercase().contains("CUSTOM") || it.categoryTitle.uppercase().contains("CUSTOM") ||
                        it.categoryTitle.uppercase().contains("INTERNATIONAL")) &&
                        hasValidScores(it)
            }

            if (isKg) {
                if (showMonthly && hasMonthly) {
                    // Kindergarten Academic Assessment
                    var coreTotalObtained = 0.0
                    var coreTotalMax = 0.0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF374151))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subject Name",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = reportMonthTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        HorizontalDivider(color = Color(0xFF9CA3AF))

                        val requiredSubjects = listOf(
                            "Myanmar" to true,
                            "English (Phonics)" to true,
                            "English (Language)" to true,
                            "Mathematics" to true,
                            "PE (Additional)" to false,
                            "Music (Additional)" to false
                        )

                        requiredSubjects.forEachIndexed { idx, (subName, isCore) ->
                            val displayScore = if (!isCore) {
                                "(-)"
                            } else {
                                val row = monthlyTable?.subjectRows?.find { s ->
                                    s.subjectName.equals(subName, ignoreCase = true) ||
                                            (subName == "English (Language)" && s.subjectName.equals("English", ignoreCase = true)) ||
                                            (subName == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                                            (subName == "English (Phonics)" && s.subjectName.contains("Phonics", ignoreCase = true))
                                }

                                if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) {
                                    val scoreVal = row.totalObtained
                                    val maxVal = row.maxPossible
                                    coreTotalObtained += scoreVal
                                    coreTotalMax += maxVal

                                    if (scoreVal % 1.0 == 0.0) "${scoreVal.toInt()}" else "$scoreVal"
                                } else {
                                    "-"
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 1) Color(0xFFF9FAFB) else Color.White)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCore) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isCore) Color(0xFF111827) else Color(0xFF6B7280),
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = displayScore,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCore) FontWeight.Bold else FontWeight.Normal,
                                    color = Color(0xFF111827),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }

                            if (idx < requiredSubjects.size - 1) {
                                HorizontalDivider(color = Color(0xFFE5E7EB))
                            }
                        }

                        HorizontalDivider(color = Color(0xFF9CA3AF))

                        val totalMarksStr = if (coreTotalMax > 0) {
                            val obtStr = if (coreTotalObtained % 1.0 == 0.0) "${coreTotalObtained.toInt()}" else "$coreTotalObtained"
                            val maxStr = if (coreTotalMax % 1.0 == 0.0) "${coreTotalMax.toInt()}" else "$coreTotalMax"
                            "$obtStr / $maxStr"
                        } else "-"

                        val rankStr = if (monthlyTable?.classRank != null && monthlyTable.totalStudentsInClass != null) {
                            "${monthlyTable.classRank} / ${monthlyTable.totalStudentsInClass}"
                        } else if (monthlyTable?.classRank != null) {
                            "${monthlyTable.classRank}"
                        } else "-"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrintSummaryItem("Total Marks:", totalMarksStr, Modifier.weight(1f))
                            PrintSummaryItem("Rank:", rankStr, Modifier.weight(1f))
                            PrintSummaryItem("Distinction:", "-", Modifier.weight(1f))
                        }
                    }
                }
            } else if (isG1to3 || isG4) {
                // 1. Monthly Test Section
                if (showMonthly && hasMonthly) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF374151))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subject Name",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = reportMonthTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        HorizontalDivider(color = Color(0xFF9CA3AF))

                        var mObtained = 0.0
                        var mMax = 0.0
                        var mDistCount = 0

                        primarySubjects.forEachIndexed { idx, subName ->
                            val row = monthlyTable?.subjectRows?.find { s ->
                                s.subjectName.equals(subName, ignoreCase = true) ||
                                        (subName == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                                        (subName == "Social Studies" && s.subjectName.equals("Social", ignoreCase = true))
                            }

                            val scoreD = if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) row.totalObtained else null
                            val maxD = row?.maxPossible ?: 0.0

                            val scoreStr = if (scoreD != null) {
                                mObtained += scoreD
                                mMax += maxD
                                if (maxD > 0 && (scoreD / maxD) >= 0.80) {
                                    mDistCount++
                                }
                                if (scoreD % 1.0 == 0.0) "${scoreD.toInt()}" else "$scoreD"
                            } else {
                                "-"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 1) Color(0xFFF9FAFB) else Color.White)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827),
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = scoreStr,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }

                            if (idx < primarySubjects.size - 1) {
                                HorizontalDivider(color = Color(0xFFE5E7EB))
                            }
                        }

                        HorizontalDivider(color = Color(0xFF9CA3AF))

                        val totalMarksStr = if (mMax > 0) {
                            val obtStr = if (mObtained % 1.0 == 0.0) "${mObtained.toInt()}" else "$mObtained"
                            val maxStr = if (mMax % 1.0 == 0.0) "${mMax.toInt()}" else "$mMax"
                            "$obtStr / $maxStr"
                        } else "-"

                        val rankStr = if (monthlyTable?.classRank != null && monthlyTable.totalStudentsInClass != null) {
                            "${monthlyTable.classRank}/${monthlyTable.totalStudentsInClass}"
                        } else if (monthlyTable?.classRank != null) {
                            "${monthlyTable.classRank}"
                        } else {
                            "-"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrintSummaryItem("Total Marks:", totalMarksStr, Modifier.weight(1f))
                            PrintSummaryItem("Rank:", rankStr, Modifier.weight(1f))
                            PrintSummaryItem("Distinction:", if (mMax > 0) "$mDistCount" else "-", Modifier.weight(1f))
                        }
                    }
                }

                // 2. Weekly Test
                if (showWeekly && hasWeekly) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PrintWeeklyTable(data)
                }

                // 3. Pilot Test
                if (showPilot && hasPilot) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PrintPilotTable(data, isG6to9 = false)
                }

                // 4. CET Table
                if (showCet && hasCet) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PrintCetTable(data, isG6to9 = false, isG10to12 = false)
                }

                // 5. LCT Table
                if (showLct && hasLct) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PrintLessonCompletionTable(data)
                }

                // 6. Custom Exam Section (English International)
                val customExamTable = data.dynamicExamTables.find {
                    it.categoryKey == "CUSTOM_EXAMS" || it.categoryTitle.uppercase().contains("CUSTOM") || it.categoryTitle.uppercase().contains("INTERNATIONAL")
                }
                val customRows = customExamTable?.subjectRows?.filter { it.totalObtained > 0.0 } ?: emptyList()

                if (showCustom && hasCustom && customRows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4B5563))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Exam (English International)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "Marks / Rank",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        HorizontalDivider(color = Color(0xFF9CA3AF))

                        customRows.forEach { cRow ->
                            val scoreStr = if (cRow.totalObtained % 1.0 == 0.0) "${cRow.totalObtained.toInt()}" else "${cRow.totalObtained}"
                            val rStr = if (customExamTable?.classRank != null && customExamTable.totalStudentsInClass != null) {
                                "${customExamTable.classRank}/${customExamTable.totalStudentsInClass}"
                            } else if (customExamTable?.classRank != null) {
                                "${customExamTable.classRank}"
                            } else "-"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cRow.subjectName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827),
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = "$scoreStr  |  Rank: $rStr",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            } else if (isG5 || isG6to9) {
                if (showMonthly && hasMonthly) {
                    val defaultSubjects = if (isG6to9) listOf("Myanmar", "English", "Mathematics", "Science", "Geography", "History") else primarySubjects
                    val defaultRows = getOneLineExamRowsPreview(data, defaultSubjects)
                    if (defaultRows.isNotEmpty()) {
                        PrintOneLineExamTable(subjectTitles = defaultSubjects, rows = defaultRows)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                if (showWeekly && hasWeekly) {
                    PrintWeeklyTable(data)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showPilot && hasPilot) {
                    PrintPilotTable(data, isG6to9 = isG6to9)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showCet && hasCet) {
                    PrintCetTable(data, isG6to9 = isG6to9)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showLct && hasLct) {
                    PrintLessonCompletionTable(data)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showCustom && hasCustom) {
                    PrintGrade5EnglishInternationalTable(data)
                }
            } else if (isG10to12) {
                if (showMonthly && hasMonthly) {
                    val g10Subjects = SchoolPolicy.getDefaultSubjectNamesForGrade(data.gradeName, data.student.stream)
                    val defaultRows = getOneLineExamRowsPreview(data, g10Subjects)
                    if (defaultRows.isNotEmpty()) {
                        PrintOneLineExamTable(subjectTitles = g10Subjects, rows = defaultRows)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                if (showWeekly && hasWeekly) {
                    PrintWeeklyTable(data)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showLct && hasLct) {
                    PrintLessonCompletionTable(data)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showPilot && hasPilot) {
                    PrintPilotTable(data, isG6to9 = false, isG10to12 = true)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showCet && hasCet) {
                    PrintCetTable(data, isG6to9 = false, isG10to12 = true)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (showCustom && hasCustom) {
                    PrintGrade5EnglishInternationalTable(data)
                }
            } else {
                if (showMonthly && hasMonthly) {
                    val defaultSubjects = listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")
                    val defaultRows = getOneLineExamRowsPreview(data, defaultSubjects)
                    if (defaultRows.isNotEmpty()) {
                        PrintOneLineExamTable(subjectTitles = defaultSubjects, rows = defaultRows)
                    }
                }
            }
        }

        // ================= A4 PAGE 2 =================
        A4PaperSheet(pageNumber = 2, totalPages = 2) {
            // 6. Holistic Assessment (HCM)
            PrintSectionTitle(title = "5. Holistic Assessment (HCM)")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.hcmSummary.pillars.forEach { pillarSummary ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryPrintHeaderColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${pillarSummary.pillarName} (${pillarSummary.myanmarPillarName})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        HorizontalDivider(color = Color(0xFF9CA3AF))
                        pillarSummary.items.take(5).forEachIndexed { itemIdx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (itemIdx % 2 == 1) Color(0xFFF9FAFB) else Color.White)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.categoryName,
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.weight(1f)
                                )
                                CompactStarRatingRow(rating = item.starRating, maxStars = item.maxStars)
                            }
                            if (itemIdx < pillarSummary.items.take(5).size - 1) {
                                HorizontalDivider(color = Color(0xFFF3F4F6))
                            }
                        }
                    }
                }
            }

            // 7. Student Strength
            PrintSectionTitle(title = "6. Student Strength")
            PrintTextBox(text = data.teacherComments.positiveComments)

            // 8. Teacher Comment
            PrintSectionTitle(title = "7. Teacher Comment")
            PrintTextBox(text = data.teacherComments.generalComment)

            // 9. Parent Support / Recommendation
            PrintSectionTitle(title = "8. Parent Recommendation & Support")
            PrintTextBox(text = data.teacherComments.futureRecommendation)

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // 10. Signatures Block - NO "Signatures" heading, four signature lines directly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PrintSignatureLine("Class Teacher", Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                PrintSignatureLine("Academic Head", Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                PrintSignatureLine("Principal", Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                PrintSignatureLine("Parent", Modifier.weight(1f))
            }
        }
    }
}

/**
 * A4 Paper Sheet Container.
 * Represents a physical sheet of paper with standard print margins (15–18 mm).
 */
@Composable
private fun A4PaperSheet(
    pageNumber: Int,
    totalPages: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(2.dp)),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Compact 15-18 mm print margins
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()

            Spacer(modifier = Modifier.height(4.dp))

            // A4 Page Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hein Chan Myae Private School • Official Report Card Document",
                    fontSize = 8.sp,
                    color = Color(0xFF9CA3AF)
                )
                Text(
                    text = "Page $pageNumber of $totalPages",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun PrintSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp, // 12-13pt section title requirement
        fontWeight = FontWeight.Bold,
        color = PrimaryPrintHeaderColor,
        modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)
    )
}

@Composable
private fun PrintDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 1.dp),
        thickness = 1.dp,
        color = PrimaryPrintHeaderColor
    )
}

@Composable
private fun PrintCellKeyVal(key: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontSize = 10.sp, // 10.5-11pt body font size requirement
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            color = Color(0xFF111827)
        )
    }
}

@Composable
private fun PrintTableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun PrintTableCell(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color(0xFF111827)
) {
    Text(
        text = text,
        fontSize = 9.5.sp,
        fontWeight = fontWeight,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun PrintSgiMetric(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4B5563)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) PrimaryPrintHeaderColor else Color(0xFF111827)
        )
    }
}

@Composable
private fun PrintSummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$label ",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4B5563)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}

@Composable
private fun PrintTextBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(3.dp))
            .background(Color(0xFFFAFAFA))
            .padding(4.dp)
    ) {
        Text(
            text = text.ifBlank { "-" },
            fontSize = 9.5.sp,
            color = Color(0xFF1F2937),
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun CompactStarRatingRow(
    rating: Int,
    maxStars: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (i <= rating) Color(0xFFD97706) else Color(0xFFD1D5DB),
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

private fun getOneLineExamRowsPreview(
    data: GeneratedReportCardData,
    subjects: List<String>
): List<OneLineExamRow> {
    val rows = mutableListOf<OneLineExamRow>()

    // STRICT FILTER: Only strictly MONTHLY tests belong to the one-line Monthly Examinations table!
    // Never include CET, PILOT, WEEKLY, UNIT, or CUSTOM in Monthly tests.
    val monthlyTables = data.dynamicExamTables.filter { table ->
        val key = table.categoryKey.uppercase()
        val title = table.categoryTitle.uppercase()
        (key == "MONTHLY_TESTS" || title.contains("MONTHLY")) &&
                !key.contains("CET") && !key.contains("PILOT") &&
                !key.contains("WEEKLY") && !key.contains("UNIT") &&
                !key.contains("LESSON") && !key.contains("CUSTOM") &&
                !title.contains("CET") && !title.contains("PILOT") &&
                !title.contains("WEEKLY") && !title.contains("UNIT") &&
                !title.contains("INTERNATIONAL")
    }

    if (monthlyTables.isNotEmpty()) {
        monthlyTables.forEach { table ->
            if (table.discoveredExams.isNotEmpty()) {
                table.discoveredExams.forEach { examName ->
                    val scoreList = mutableListOf<Double?>()
                    subjects.forEach { sub ->
                        val row = table.subjectRows.find { s ->
                            s.subjectName.equals(sub, ignoreCase = true) ||
                                    (sub == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                                    (sub == "Social Studies" && (s.subjectName.contains("Social", ignoreCase = true) || s.subjectName.contains("Geo", ignoreCase = true))) ||
                                    (sub == "Geography" && (s.subjectName.contains("Geo", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                    (sub == "History" && (s.subjectName.contains("Hist", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                    (sub.contains("Bio", ignoreCase = true) && s.subjectName.contains("Bio", ignoreCase = true)) ||
                                    (sub.contains("Econ", ignoreCase = true) && s.subjectName.contains("Econ", ignoreCase = true)) ||
                                    (sub == "Chemistry" && s.subjectName.contains("Chem", ignoreCase = true)) ||
                                    (sub == "Physics" && s.subjectName.contains("Phys", ignoreCase = true))
                        }
                        val sc = row?.examScores?.get(examName)
                        scoreList.add(sc)
                    }

                    val validScores = scoreList.filterNotNull()
                    if (validScores.isNotEmpty()) {
                        val totalVal = validScores.sum()
                        val distCount = validScores.count { it >= 80.0 }
                        val rankStr = if (table.classRank != null && table.totalStudentsInClass != null) {
                            "${table.classRank}/${table.totalStudentsInClass}"
                        } else if (table.classRank != null) {
                            "${table.classRank}"
                        } else {
                            "-"
                        }

                        fun fmt(d: Double?) = if (d == null) "-" else if (d % 1.0 == 0.0) "${d.toInt()}" else "$d"

                        rows.add(
                            OneLineExamRow(
                                examName = examName,
                                subjectScores = scoreList.map { fmt(it) },
                                total = fmt(totalVal),
                                rank = rankStr,
                                distinction = "$distCount"
                            )
                        )
                    }
                }
            } else {
                val examName = table.categoryTitle
                val scoreList = mutableListOf<Double?>()

                subjects.forEach { sub ->
                    val row = table.subjectRows.find { s ->
                        s.subjectName.equals(sub, ignoreCase = true) ||
                                (sub == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                                (sub == "Social Studies" && (s.subjectName.contains("Social", ignoreCase = true) || s.subjectName.contains("Geo", ignoreCase = true))) ||
                                (sub == "Geography" && (s.subjectName.contains("Geo", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                (sub == "History" && (s.subjectName.contains("Hist", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                (sub.contains("Bio", ignoreCase = true) && s.subjectName.contains("Bio", ignoreCase = true)) ||
                                (sub.contains("Econ", ignoreCase = true) && s.subjectName.contains("Econ", ignoreCase = true)) ||
                                (sub == "Chemistry" && s.subjectName.contains("Chem", ignoreCase = true)) ||
                                (sub == "Physics" && s.subjectName.contains("Phys", ignoreCase = true))
                    }
                    val sc = if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) row.totalObtained else null
                    scoreList.add(sc)
                }

                val validScores = scoreList.filterNotNull()
                val totalVal = validScores.sum()
                val distCount = validScores.count { it >= 80.0 }

                val rankStr = if (table.classRank != null && table.totalStudentsInClass != null) {
                    "${table.classRank}/${table.totalStudentsInClass}"
                } else if (table.classRank != null) {
                    "${table.classRank}"
                } else {
                    "-"
                }

                fun fmt(d: Double?) = if (d == null) "-" else if (d % 1.0 == 0.0) "${d.toInt()}" else "$d"

                if (validScores.isNotEmpty()) {
                    rows.add(
                        OneLineExamRow(
                            examName = examName,
                            subjectScores = scoreList.map { fmt(it) },
                            total = fmt(totalVal),
                            rank = rankStr,
                            distinction = "$distCount"
                        )
                    )
                }
            }
        }
    }
    return rows
}

@Composable
private fun PrintOneLineExamTable(
    subjectTitles: List<String>,
    rows: List<OneLineExamRow>
) {
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
    ) {
        // Table Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exam", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White, modifier = Modifier.weight(1.2f))
            subjectTitles.forEach { titleText ->
                val displayTitle = when {
                    titleText.equals("Mathematics", ignoreCase = true) -> "Math"
                    titleText.equals("Social Studies", ignoreCase = true) -> "Social"
                    titleText.equals("Geography", ignoreCase = true) -> "Geo"
                    titleText.equals("History", ignoreCase = true) -> "Hist"
                    titleText.equals("Chemistry", ignoreCase = true) -> "Chem"
                    titleText.equals("Physics", ignoreCase = true) -> "Phys"
                    titleText.equals("Economics", ignoreCase = true) -> "Econ"
                    else -> titleText
                }
                Text(displayTitle, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Text("Total", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("Rank", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("Distinction", fontWeight = FontWeight.Bold, fontSize = 7.5.sp, color = Color.White, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
        }

        HorizontalDivider(color = Color(0xFF9CA3AF))

        // Exam Rows
        rows.forEachIndexed { idx, r ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 1) Color(0xFFF9FAFB) else Color.White)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(r.examName, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF111827), modifier = Modifier.weight(1.2f))
                r.subjectScores.forEach { sVal ->
                    Text(sVal, fontSize = 8.5.sp, color = Color(0xFF111827), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
                Text(r.total, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF111827), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(r.rank, fontSize = 8.5.sp, color = Color(0xFF111827), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(r.distinction, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF111827), modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
            }

            if (idx < rows.size - 1) {
                HorizontalDivider(color = Color(0xFFE5E7EB))
            }
        }
    }
}

@Composable
private fun PrintSignatureLine(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Space above for physical signing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF374151))
        )
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111827),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrintWeeklyTable(data: GeneratedReportCardData) {
    val normGrade = data.gradeName.trim().lowercase()
    val isG10to12 = normGrade.contains("g10") || normGrade.contains("g11") || normGrade.contains("g12") ||
            normGrade.contains("grade 10") || normGrade.contains("grade 11") || normGrade.contains("grade 12")
    val isG6to9 = normGrade.contains("g6") || normGrade.contains("g7") || normGrade.contains("g8") || normGrade.contains("g9") ||
            normGrade.contains("grade 6") || normGrade.contains("grade 7") || normGrade.contains("grade 8") || normGrade.contains("grade 9")
    val subjects = if (isG10to12) {
        SchoolPolicy.getDefaultSubjectNamesForGrade(data.gradeName, data.student.stream)
    } else if (isG6to9) {
        listOf("Myanmar", "English", "Mathematics", "Science", "Geography", "History")
    } else {
        listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")
    }
    val weeklyTables = data.dynamicExamTables.filter {
        it.categoryKey.uppercase().contains("WEEKLY") || it.categoryTitle.uppercase().contains("WEEKLY")
    }

    val completedCols = if (weeklyTables.isNotEmpty()) {
        val cols = mutableListOf<MultiExamColData>()
        weeklyTables.forEachIndexed { idx, t ->
            val scores = mutableMapOf<String, Double>()
            subjects.forEach { sub ->
                val row = t.subjectRows.find { s ->
                    s.subjectName.equals(sub, ignoreCase = true) ||
                            (sub == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                            (sub.contains("Bio", ignoreCase = true) && s.subjectName.contains("Bio", ignoreCase = true)) ||
                            (sub.contains("Econ", ignoreCase = true) && s.subjectName.contains("Econ", ignoreCase = true)) ||
                            (sub == "Chemistry" && s.subjectName.contains("Chem", ignoreCase = true)) ||
                            (sub == "Physics" && s.subjectName.contains("Phys", ignoreCase = true))
                }
                if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) {
                    scores[sub] = row.totalObtained
                }
            }
            if (scores.isNotEmpty()) {
                val totalVal = scores.values.sum()
                val rStr = if (t.classRank != null && t.totalStudentsInClass != null) "${t.classRank}/${t.totalStudentsInClass}" else if (t.classRank != null) "${t.classRank}" else "-"
                cols.add(
                    MultiExamColData(
                        title = if (t.categoryTitle.isNotBlank()) t.categoryTitle else "Week ${idx + 1}",
                        subjectScores = scores,
                        totalStr = if (totalVal % 1.0 == 0.0) "${totalVal.toInt()}" else "$totalVal",
                        rankStr = rStr,
                        distinctionStr = "-"
                    )
                )
            }
        }
        cols
    } else {
        emptyList()
    }

    if (completedCols.isNotEmpty()) {
        PrintMultiColumnAssessmentTable(
            sectionTitle = "Weekly Test",
            subjects = subjects,
            completedCols = completedCols,
            showAverageColumn = false,
            rankLabel = "Rank over Grade",
            showDistinctionRow = false
        )
    }
}

@Composable
private fun PrintLessonCompletionTable(data: GeneratedReportCardData) {
    val normGrade = data.gradeName.trim().lowercase()
    val isG10to12 = normGrade.contains("g10") || normGrade.contains("g11") || normGrade.contains("g12") ||
            normGrade.contains("grade 10") || normGrade.contains("grade 11") || normGrade.contains("grade 12")
    val isG6to9 = normGrade.contains("g6") || normGrade.contains("g7") || normGrade.contains("g8") || normGrade.contains("g9") ||
            normGrade.contains("grade 6") || normGrade.contains("grade 7") || normGrade.contains("grade 8") || normGrade.contains("grade 9")
    val subjects = if (isG10to12) {
        SchoolPolicy.getDefaultSubjectNamesForGrade(data.gradeName, data.student.stream)
    } else if (isG6to9) {
        listOf("Myanmar", "English", "Mathematics", "Science", "Geography", "History")
    } else {
        listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")
    }
    val lessonTables = data.dynamicExamTables.filter {
        it.categoryKey.uppercase().contains("LESSON") || it.categoryKey.uppercase().contains("UNIT") ||
                it.categoryTitle.uppercase().contains("LESSON") || it.categoryTitle.uppercase().contains("UNIT") || it.categoryTitle.uppercase().contains("LCT")
    }

    val completedCols = if (lessonTables.isNotEmpty()) {
        val cols = mutableListOf<MultiExamColData>()
        lessonTables.forEachIndexed { idx, t ->
            val scores = mutableMapOf<String, Double>()
            subjects.forEach { sub ->
                val row = t.subjectRows.find { s ->
                    s.subjectName.equals(sub, ignoreCase = true) ||
                            (sub == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                            (sub.contains("Bio", ignoreCase = true) && s.subjectName.contains("Bio", ignoreCase = true)) ||
                            (sub.contains("Econ", ignoreCase = true) && s.subjectName.contains("Econ", ignoreCase = true)) ||
                            (sub == "Chemistry" && s.subjectName.contains("Chem", ignoreCase = true)) ||
                            (sub == "Physics" && s.subjectName.contains("Phys", ignoreCase = true))
                }
                if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) {
                    scores[sub] = row.totalObtained
                }
            }
            if (scores.isNotEmpty()) {
                val totalVal = scores.values.sum()
                val rStr = if (t.classRank != null && t.totalStudentsInClass != null) "${t.classRank}/${t.totalStudentsInClass}" else if (t.classRank != null) "${t.classRank}" else "-"
                cols.add(
                    MultiExamColData(
                        title = "${idx + 1}",
                        subjectScores = scores,
                        totalStr = if (totalVal % 1.0 == 0.0) "${totalVal.toInt()}" else "$totalVal",
                        rankStr = rStr,
                        distinctionStr = "-"
                    )
                )
            }
        }
        cols
    } else {
        emptyList()
    }

    if (completedCols.isNotEmpty()) {
        PrintMultiColumnAssessmentTable(
            sectionTitle = "Lesson Completion Test",
            subjects = subjects,
            completedCols = completedCols,
            showAverageColumn = false,
            rankLabel = "Rank over Grade",
            showDistinctionRow = false
        )
    }
}

@Composable
private fun PrintPilotTable(
    data: GeneratedReportCardData,
    isG6to9: Boolean = false,
    isG10to12: Boolean = false
) {
    val subjects = if (isG10to12) {
        SchoolPolicy.getDefaultSubjectNamesForGrade(data.gradeName, data.student.stream)
    } else if (isG6to9) {
        listOf("Myanmar", "English", "Mathematics", "Science", "Geography", "History")
    } else {
        listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")
    }

    val pilotTables = data.dynamicExamTables.filter {
        it.categoryKey.uppercase().contains("PILOT") || it.categoryTitle.uppercase().contains("PILOT")
    }

    val completedCols = if (pilotTables.isNotEmpty()) {
        val cols = mutableListOf<MultiExamColData>()
        pilotTables.forEach { t ->
            if (t.discoveredExams.isNotEmpty()) {
                t.discoveredExams.forEachIndexed { idx, examName ->
                    val scores = mutableMapOf<String, Double>()
                    subjects.forEach { sub ->
                        val row = t.subjectRows.find { s ->
                            s.subjectName.equals(sub, ignoreCase = true) ||
                                    (sub == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                                    (sub == "Social Studies" && (s.subjectName.contains("Social", ignoreCase = true) || s.subjectName.contains("Geo", ignoreCase = true))) ||
                                    (sub == "Geography" && (s.subjectName.contains("Geo", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                    (sub == "History" && (s.subjectName.contains("Hist", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                    (sub.contains("Bio", ignoreCase = true) && s.subjectName.contains("Bio", ignoreCase = true)) ||
                                    (sub.contains("Econ", ignoreCase = true) && s.subjectName.contains("Econ", ignoreCase = true)) ||
                                    (sub == "Chemistry" && s.subjectName.contains("Chem", ignoreCase = true)) ||
                                    (sub == "Physics" && s.subjectName.contains("Phys", ignoreCase = true))
                        }
                        // STRICT ISOLATION: Only get marks for this exact examName!
                        val scoreForExam = row?.examScores?.get(examName)
                        if (scoreForExam != null && scoreForExam > 0.0) {
                            scores[sub] = scoreForExam
                        }
                    }
                    if (scores.isNotEmpty()) {
                        val totalVal = scores.values.sum()
                        val rStr = if (t.classRank != null && t.totalStudentsInClass != null) "${t.classRank}/${t.totalStudentsInClass}" else if (t.classRank != null) "${t.classRank}" else "-"
                        val dist = scores.values.count { it >= 80.0 }
                        val displayTitle = examName.replace("Secondary ", "").replace("High School ", "").replace("G12 ", "").replace("G10 ", "")
                        cols.add(
                            MultiExamColData(
                                title = if (displayTitle.isNotBlank()) displayTitle else "Pilot Test ${idx + 1}",
                                subjectScores = scores,
                                totalStr = if (totalVal % 1.0 == 0.0) "${totalVal.toInt()}" else "$totalVal",
                                rankStr = rStr,
                                distinctionStr = "$dist"
                            )
                        )
                    }
                }
            } else {
                val scores = mutableMapOf<String, Double>()
                subjects.forEach { sub ->
                    val row = t.subjectRows.find { s -> s.subjectName.equals(sub, ignoreCase = true) }
                    if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) {
                        scores[sub] = row.totalObtained
                    }
                }
                if (scores.isNotEmpty()) {
                    val totalVal = scores.values.sum()
                    val dist = scores.values.count { it >= 80.0 }
                    val rStr = if (t.classRank != null && t.totalStudentsInClass != null) "${t.classRank}/${t.totalStudentsInClass}" else if (t.classRank != null) "${t.classRank}" else "-"
                    cols.add(
                        MultiExamColData(
                            title = t.categoryTitle.ifBlank { "Pilot Test" },
                            subjectScores = scores,
                            totalStr = if (totalVal % 1.0 == 0.0) "${totalVal.toInt()}" else "$totalVal",
                            rankStr = rStr,
                            distinctionStr = "$dist"
                        )
                    )
                }
            }
        }
        cols
    } else {
        emptyList()
    }

    if (completedCols.isNotEmpty()) {
        PrintMultiColumnAssessmentTable(
            sectionTitle = "Pilot Test",
            subjects = subjects,
            completedCols = completedCols,
            showAverageColumn = (completedCols.size >= 4),
            rankLabel = "Rank",
            showDistinctionRow = true
        )
    }
}

@Composable
private fun PrintCetTable(
    data: GeneratedReportCardData,
    isG6to9: Boolean = false,
    isG10to12: Boolean = false
) {
    val subjects = if (isG10to12) {
        SchoolPolicy.getDefaultSubjectNamesForGrade(data.gradeName, data.student.stream)
    } else if (isG6to9) {
        listOf("Myanmar", "English", "Mathematics", "Science", "Geography", "History")
    } else {
        listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")
    }

    val cetTables = data.dynamicExamTables.filter {
        it.categoryKey.uppercase().contains("CET") || it.categoryTitle.uppercase().contains("CET")
    }

    val completedCols = if (cetTables.isNotEmpty()) {
        val cols = mutableListOf<MultiExamColData>()
        cetTables.forEach { t ->
            if (t.discoveredExams.isNotEmpty()) {
                t.discoveredExams.forEachIndexed { idx, examName ->
                    val scores = mutableMapOf<String, Double>()
                    subjects.forEach { sub ->
                        val row = t.subjectRows.find { s ->
                            s.subjectName.equals(sub, ignoreCase = true) ||
                                    (sub == "Mathematics" && s.subjectName.equals("Math", ignoreCase = true)) ||
                                    (sub == "Social Studies" && (s.subjectName.contains("Social", ignoreCase = true) || s.subjectName.contains("Geo", ignoreCase = true))) ||
                                    (sub == "Geography" && (s.subjectName.contains("Geo", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                    (sub == "History" && (s.subjectName.contains("Hist", ignoreCase = true) || s.subjectName.contains("Social", ignoreCase = true))) ||
                                    (sub.contains("Bio", ignoreCase = true) && s.subjectName.contains("Bio", ignoreCase = true)) ||
                                    (sub.contains("Econ", ignoreCase = true) && s.subjectName.contains("Econ", ignoreCase = true)) ||
                                    (sub == "Chemistry" && s.subjectName.contains("Chem", ignoreCase = true)) ||
                                    (sub == "Physics" && s.subjectName.contains("Phys", ignoreCase = true))
                        }
                        val scoreForExam = row?.examScores?.get(examName)
                        if (scoreForExam != null && scoreForExam > 0.0) {
                            scores[sub] = scoreForExam
                        }
                    }
                    if (scores.isNotEmpty()) {
                        val totalVal = scores.values.sum()
                        val rStr = if (t.classRank != null && t.totalStudentsInClass != null) "${t.classRank}/${t.totalStudentsInClass}" else if (t.classRank != null) "${t.classRank}" else "-"
                        val dist = scores.values.count { it >= 80.0 }
                        val displayTitle = examName.replace("Secondary ", "").replace("High School ", "").replace("G12 ", "").replace("G10 ", "")
                        cols.add(
                            MultiExamColData(
                                title = if (displayTitle.isNotBlank()) displayTitle else "CET ${idx + 1}",
                                subjectScores = scores,
                                totalStr = if (totalVal % 1.0 == 0.0) "${totalVal.toInt()}" else "$totalVal",
                                rankStr = rStr,
                                distinctionStr = "$dist"
                            )
                        )
                    }
                }
            } else {
                val scores = mutableMapOf<String, Double>()
                subjects.forEach { sub ->
                    val row = t.subjectRows.find { s -> s.subjectName.equals(sub, ignoreCase = true) }
                    if (row != null && row.maxPossible > 0 && row.totalObtained > 0.0) {
                        scores[sub] = row.totalObtained
                    }
                }
                if (scores.isNotEmpty()) {
                    val totalVal = scores.values.sum()
                    val dist = scores.values.count { it >= 80.0 }
                    val rStr = if (t.classRank != null && t.totalStudentsInClass != null) "${t.classRank}/${t.totalStudentsInClass}" else if (t.classRank != null) "${t.classRank}" else "-"
                    cols.add(
                        MultiExamColData(
                            title = t.categoryTitle.ifBlank { "CET" },
                            subjectScores = scores,
                            totalStr = if (totalVal % 1.0 == 0.0) "${totalVal.toInt()}" else "$totalVal",
                            rankStr = rStr,
                            distinctionStr = "$dist"
                        )
                    )
                }
            }
        }
        cols
    } else {
        emptyList()
    }

    if (completedCols.isNotEmpty()) {
        PrintMultiColumnAssessmentTable(
            sectionTitle = "CET",
            subjects = subjects,
            completedCols = completedCols,
            showAverageColumn = (completedCols.size >= 4),
            rankLabel = "Rank",
            showDistinctionRow = true
        )
    }
}

@Composable
private fun PrintGrade5EnglishInternationalTable(data: GeneratedReportCardData) {
    val customTable = data.dynamicExamTables.find {
        it.categoryKey.uppercase().contains("CUSTOM") || it.categoryTitle.uppercase().contains("CUSTOM") || it.categoryTitle.uppercase().contains("INTERNATIONAL")
    }

    val customExams = if (customTable != null && customTable.subjectRows.isNotEmpty()) {
        customTable.subjectRows.filter { it.totalObtained > 0.0 }.mapIndexed { idx, s ->
            CustomExamColData(
                seq = "${idx + 1}",
                mark = if (s.totalObtained % 1.0 == 0.0) "${s.totalObtained.toInt()}" else "${s.totalObtained}",
                rank = if (customTable.classRank != null) "${customTable.classRank}/${customTable.totalStudentsInClass ?: 35}" else "${idx + 1}/35"
            )
        }
    } else {
        emptyList()
    }

    if (customExams.isNotEmpty()) {
        PrintEnglishInternationalTable(customExams = customExams)
    }
}

@Composable
private fun PrintMultiColumnAssessmentTable(
    sectionTitle: String,
    subjects: List<String>,
    completedCols: List<MultiExamColData>,
    showAverageColumn: Boolean = false,
    rankLabel: String = "Rank",
    showDistinctionRow: Boolean = true
) {
    if (completedCols.isEmpty()) return

    val subjectWeight = if (completedCols.size > 8) 2.0f else 1.5f
    val fontSizeSmall = if (completedCols.size > 8) 8.sp else 10.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryPrintHeaderColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
        }

        HorizontalDivider(color = Color(0xFF9CA3AF))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subject",
                fontWeight = FontWeight.Bold,
                fontSize = fontSizeSmall,
                color = Color.White,
                modifier = Modifier.weight(subjectWeight)
            )
            completedCols.forEach { col ->
                Text(
                    text = col.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            if (showAverageColumn) {
                Text(
                    text = "Average",
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = Color(0xFF9CA3AF))

        subjects.forEachIndexed { idx, subName ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 1) Color(0xFFF9FAFB) else Color.White)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subName,
                    fontSize = fontSizeSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(subjectWeight)
                )

                var sumScore = 0.0
                var validCount = 0
                completedCols.forEach { col ->
                    val sc = col.subjectScores[subName] ?: 0.0
                    sumScore += sc
                    validCount++
                    val scStr = if (sc % 1.0 == 0.0) "${sc.toInt()}" else "$sc"
                    Text(
                        text = scStr,
                        fontSize = fontSizeSmall,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }

                if (showAverageColumn) {
                    val avgVal = if (validCount > 0) sumScore / validCount else 0.0
                    val avgStr = if (avgVal % 1.0 == 0.0) "${avgVal.toInt()}" else String.format("%.1f", avgVal)
                    Text(
                        text = avgStr,
                        fontSize = fontSizeSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))
        }

        HorizontalDivider(color = Color(0xFF9CA3AF))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                fontSize = fontSizeSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(subjectWeight)
            )

            var sumTotal = 0.0
            completedCols.forEach { col ->
                Text(
                    text = col.totalStr,
                    fontSize = fontSizeSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                sumTotal += (col.totalStr.toDoubleOrNull() ?: 0.0)
            }

            if (showAverageColumn) {
                val avgTotalVal = if (completedCols.isNotEmpty()) sumTotal / completedCols.size else 0.0
                val avgTotalStr = if (avgTotalVal % 1.0 == 0.0) "${avgTotalVal.toInt()}" else String.format("%.1f", avgTotalVal)
                Text(
                    text = avgTotalStr,
                    fontSize = fontSizeSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = Color(0xFFE5E7EB))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rankLabel,
                fontSize = fontSizeSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(subjectWeight)
            )

            completedCols.forEach { col ->
                Text(
                    text = col.rankStr,
                    fontSize = fontSizeSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            if (showAverageColumn) {
                val avgRankStr = completedCols.lastOrNull()?.rankStr ?: "-"
                Text(
                    text = avgRankStr,
                    fontSize = fontSizeSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showDistinctionRow) {
            HorizontalDivider(color = Color(0xFFE5E7EB))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distinction",
                    fontSize = fontSizeSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(subjectWeight)
                )

                completedCols.forEach { col ->
                    Text(
                        text = col.distinctionStr,
                        fontSize = fontSizeSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }

                if (showAverageColumn) {
                    var avgDistCount = 0
                    subjects.forEach { subName ->
                        var subSum = 0.0
                        completedCols.forEach { col -> subSum += (col.subjectScores[subName] ?: 0.0) }
                        val subAvg = if (completedCols.isNotEmpty()) subSum / completedCols.size else 0.0
                        if (subAvg >= 80.0) avgDistCount++
                    }

                    Text(
                        text = "$avgDistCount",
                        fontSize = fontSizeSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PrintEnglishInternationalTable(
    customExams: List<CustomExamColData>
) {
    if (customExams.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryPrintHeaderColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "English (International)",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
        }

        HorizontalDivider(color = Color(0xFF9CA3AF))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "English (Intl)",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier.weight(1.5f)
            )
            customExams.forEach { col ->
                Text(
                    text = col.seq,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = Color(0xFF9CA3AF))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Marks",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(1.5f)
            )
            customExams.forEach { col ->
                Text(
                    text = col.mark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = Color(0xFFE5E7EB))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rank",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(1.5f)
            )
            customExams.forEach { col ->
                Text(
                    text = col.rank,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
