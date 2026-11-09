package com.example.data.report.engine

import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.HolisticCategoryEntity
import com.example.data.local.entity.HolisticResultEntity
import com.example.data.local.entity.SgiResultEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.StudentMarkEntity
import com.example.data.local.entity.TeacherCommentEntity
import com.example.data.report.GeneratedReportCardData
import com.example.data.report.ReportAttendanceSummary
import com.example.data.report.ReportHcmPillarItem
import com.example.data.report.ReportHcmPillarSummary
import com.example.data.report.ReportHcmSummary
import com.example.data.report.ReportTeacherComments
import com.example.data.report.rules.ReportSelectionRules
import kotlin.math.roundToInt

object ReportGeneratorEngine {

    fun generateReportCard(
        student: StudentEntity,
        selectedMonth: String,
        academicYear: String,
        allAssessments: List<AssessmentEntity>,
        studentMarks: List<StudentMarkEntity>,
        attendanceRecords: List<AttendanceRecordEntity>,
        holisticCategories: List<HolisticCategoryEntity>,
        holisticResults: List<HolisticResultEntity>,
        sgiResults: List<SgiResultEntity>,
        teacherComment: TeacherCommentEntity? = null,
        allStudents: List<StudentEntity> = emptyList()
    ): GeneratedReportCardData? {

        // Check if student has any records in assessments/marks, attendance, or HCM
        val studentMarksForStudent = studentMarks.filter { it.studentId == student.id }
        val attendanceForStudent = attendanceRecords.filter {
            it.studentId == student.id && (academicYear.isBlank() || it.academicYear.equals(academicYear, ignoreCase = true))
        }
        val holisticForStudent = holisticResults.filter { it.studentId == student.id }

        val hasAssessmentData = studentMarksForStudent.isNotEmpty()
        val hasAttendanceData = attendanceForStudent.isNotEmpty()
        val hasHcmData = holisticForStudent.isNotEmpty()

        // If no assessment, no attendance, and no HCM assessment record, do NOT show / generate report card
        if (!hasAssessmentData && !hasAttendanceData && !hasHcmData) {
            return null
        }

        // 1. Evaluate Template Selection Rule based on Student Grade
        val ruleResult = ReportSelectionRules.evaluateTemplate(
            gradeName = student.gradeName,
            educationLevel = null
        )

        // 2. Dynamic Examination Processing (NEVER hardcoded tables)
        val dynamicExamTables = DynamicExaminationEngine.generateDynamicExamTables(
            studentId = student.id,
            gradeName = student.gradeName,
            selectedMonthOrPeriod = selectedMonth,
            allAssessments = allAssessments,
            studentMarks = studentMarks,
            academicYear = academicYear,
            allStudents = allStudents,
            studentClass = student.className
        )

        // 3. Attendance Data Mapping (Selected Month + Cumulative Year-To-Date)
        val attendanceSummary = processAttendanceData(
            studentId = student.id,
            selectedMonth = selectedMonth,
            academicYear = academicYear,
            attendanceRecords = attendanceRecords
        )

        // 4. HCM Assessment Mapping (Honesty, Curiosity, Mindfulness)
        val hcmSummary = processHcmData(
            studentId = student.id,
            gradeName = student.gradeName,
            selectedMonth = selectedMonth,
            holisticCategories = holisticCategories,
            holisticResults = holisticResults,
            sgiResults = sgiResults
        )

        // 5. Teacher Comments Mapping
        val comments = processTeacherComments(
            teacherComment = teacherComment,
            studentName = student.name,
            overallAverage = dynamicExamTables.firstOrNull()?.overallCategoryAverage ?: 80.0
        )

        // 6. Calculate Automatic SGI for KG (Grade = KG)
        val isKg = student.gradeName.trim().equals("KG", ignoreCase = true) ||
                ruleResult.selectedTemplate == com.example.data.local.entity.ReportTemplateType.KINDERGARTEN

        val hasAttendance = attendanceSummary.monthTotalDays > 0
        val hasAcademicMarks = dynamicExamTables.any { it.maxPossibleAcrossExams > 0 && it.totalMarksAcrossExams > 0 }
        val hasHcmRatings = holisticForStudent.isNotEmpty()

        val kgSgiSummary = if (isKg && (hasAttendance || hasAcademicMarks || hasHcmRatings)) {
            val academicCompPct = if (hasAcademicMarks) {
                dynamicExamTables.firstOrNull()?.overallCategoryAverage ?: 0.0
            } else 0.0
            val attendanceCompPct = if (hasAttendance) attendanceSummary.monthAttendancePercentage else 0.0
            val hcmCompPct = if (hasHcmRatings) (hcmSummary.overallScore / 5.0) * 100.0 else 0.0

            val isComplete = hasAttendance && (hasAcademicMarks || hasHcmRatings)
            val rawSgiVal = if (isComplete) {
                (academicCompPct * 0.4) + (attendanceCompPct * 0.3) + (hcmCompPct * 0.3)
            } else 0.0
            val overallSgiPct = (rawSgiVal * 10.0).roundToInt() / 10.0

            val sgiGrade = when {
                !isComplete -> "-"
                overallSgiPct >= 90.0 -> "S+ (Exceptional Growth)"
                overallSgiPct >= 80.0 -> "S (Advanced Growth)"
                overallSgiPct >= 70.0 -> "A (Proficient Growth)"
                overallSgiPct >= 60.0 -> "B (Developing Growth)"
                else -> "C (Needs Encouragement)"
            }

            com.example.data.report.ReportSgiSummary(
                academicComponentPct = if (hasAcademicMarks) (academicCompPct * 10.0).roundToInt() / 10.0 else 0.0,
                attendanceComponentPct = if (hasAttendance) (attendanceCompPct * 10.0).roundToInt() / 10.0 else 0.0,
                hcmComponentPct = if (hasHcmRatings) (hcmCompPct * 10.0).roundToInt() / 10.0 else 0.0,
                overallSgiPercentage = overallSgiPct,
                sgiGrade = sgiGrade,
                isAutoCalculated = true
            )
        } else null

        // 7. Calculate Primary Report Summary (Grades G1 to G4)
        val normGrade = student.gradeName.trim().uppercase()
        val isPrimary = normGrade in listOf("G1", "G2", "G3", "G4", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4") ||
                ruleResult.selectedTemplate == com.example.data.local.entity.ReportTemplateType.PRIMARY

        val primaryReportSummary = if (isPrimary) {
            DynamicExaminationEngine.buildPrimaryReportSummary(
                studentId = student.id,
                gradeName = student.gradeName,
                selectedMonthOrPeriod = selectedMonth,
                allAssessments = allAssessments,
                studentMarks = studentMarks
            )
        } else null

        // 8. Return Clean Generated Data Package
        return GeneratedReportCardData(
            student = student,
            gradeName = student.gradeName,
            className = student.className,
            selectedMonth = selectedMonth,
            academicYear = academicYear,
            ruleResult = ruleResult,
            dynamicExamTables = dynamicExamTables,
            attendanceSummary = attendanceSummary,
            hcmSummary = hcmSummary,
            teacherComments = comments,
            kgSgiSummary = kgSgiSummary,
            primaryReportSummary = primaryReportSummary,
            generatedAt = 0L
        )
    }

    private fun processAttendanceData(
        studentId: Long,
        selectedMonth: String,
        academicYear: String,
        attendanceRecords: List<AttendanceRecordEntity>
    ): ReportAttendanceSummary {
        val studentRecords = attendanceRecords.filter {
            it.studentId == studentId && (it.academicYear == academicYear || academicYear.isBlank())
        }

        // Cumulative totals
        val cumTotal = studentRecords.size.coerceAtLeast(1)
        val cumPresent = studentRecords.count { it.status == AttendanceStatus.PRESENT }
        val cumAbsent = studentRecords.count { it.status == AttendanceStatus.ABSENT }
        val cumLeave = studentRecords.count { it.status == AttendanceStatus.LEAVE }
        val cumPct = (cumPresent.toDouble() / cumTotal.toDouble()) * 100.0

        // Selected month records
        val monthRecords = if (selectedMonth.isBlank() || selectedMonth.contains("All", ignoreCase = true)) {
            studentRecords
        } else {
            studentRecords.filter { record ->
                record.date.contains(selectedMonth, ignoreCase = true) ||
                        record.academicYear.contains(selectedMonth, ignoreCase = true)
            }.ifEmpty { studentRecords }
        }

        val monthTotal = monthRecords.size.coerceAtLeast(1)
        val monthPresent = monthRecords.count { it.status == AttendanceStatus.PRESENT }
        val monthAbsent = monthRecords.count { it.status == AttendanceStatus.ABSENT }
        val monthLeave = monthRecords.count { it.status == AttendanceStatus.LEAVE }
        val monthPct = (monthPresent.toDouble() / monthTotal.toDouble()) * 100.0

        val badge = when {
            monthPct >= 95.0 -> "Excellent Attendance (${(monthPct * 10).roundToInt() / 10.0}%)"
            monthPct >= 85.0 -> "Good Attendance (${(monthPct * 10).roundToInt() / 10.0}%)"
            else -> "Needs Attention (${(monthPct * 10).roundToInt() / 10.0}%)"
        }

        return ReportAttendanceSummary(
            selectedMonth = selectedMonth,
            monthTotalDays = monthTotal,
            monthPresentDays = monthPresent,
            monthAbsentDays = monthAbsent,
            monthLeaveDays = monthLeave,
            monthAttendancePercentage = (monthPct * 10.0).roundToInt() / 10.0,
            cumulativeTotalDays = cumTotal,
            cumulativePresentDays = cumPresent,
            cumulativeAbsentDays = cumAbsent,
            cumulativePercentage = (cumPct * 10.0).roundToInt() / 10.0,
            statusBadge = badge
        )
    }

    private fun processHcmData(
        studentId: Long,
        gradeName: String,
        selectedMonth: String,
        holisticCategories: List<HolisticCategoryEntity>,
        holisticResults: List<HolisticResultEntity>,
        sgiResults: List<SgiResultEntity>
    ): ReportHcmSummary {
        val studentResults = holisticResults.filter { it.studentId == studentId }

        val normGrade = gradeName.trim().uppercase()
        val studentLevel = when {
            normGrade == "KG" || normGrade.contains("KINDERGARTEN") -> "KINDERGARTEN"
            normGrade in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5", "PRIMARY 1", "PRIMARY 2", "PRIMARY 3", "PRIMARY 4", "PRIMARY 5") || normGrade.startsWith("PRIMARY") -> "PRIMARY"
            normGrade in listOf("G6", "G7", "G8", "G9", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") || normGrade.startsWith("SECONDARY") -> "SECONDARY"
            normGrade in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") || normGrade.startsWith("HIGH") -> "HIGH_SCHOOL"
            else -> "PRIMARY"
        }

        val levelCategories = holisticCategories.filter { it.educationLevel.equals(studentLevel, ignoreCase = true) }

        val pillarsDef = listOf(
            Triple("HONESTY", "ရိုးသားဖြောင့်မတ်မှု", "HONESTY"),
            Triple("CURIOSITY", "စူးစမ်းလိုစိတ်", "CURIOSITY"),
            Triple("MINDFULNESS", "သတိတရားနှင့်ကိုယ်ကျင့်တရား", "MINDFULNESS")
        )

        val pillarSummaries = mutableListOf<ReportHcmPillarSummary>()
        var sumHonesty = 0.0
        var countHonesty = 0
        var sumCuriosity = 0.0
        var countCuriosity = 0
        var sumMindfulness = 0.0
        var countMindfulness = 0

        pillarsDef.forEach { (pillarKey, myanmarName, pillarEnum) ->
            val categoriesInPillar = levelCategories.filter { it.pillar.equals(pillarKey, ignoreCase = true) }
            val items = mutableListOf<ReportHcmPillarItem>()
            var pillarSum = 0

            if (categoriesInPillar.isNotEmpty()) {
                categoriesInPillar.forEach { cat ->
                    val result = studentResults.find { it.categoryId == cat.id }
                    val stars = result?.ratingStars ?: 0 // Default 0-star (unrated) if not entered
                    items.add(
                        ReportHcmPillarItem(
                            categoryName = cat.categoryName,
                            pillar = pillarEnum,
                            starRating = stars,
                            maxStars = cat.maxStars
                        )
                    )
                    pillarSum += stars
                }
            } else {
                val defaults = when (studentLevel.uppercase()) {
                    "KINDERGARTEN" -> when (pillarKey) {
                        "HONESTY" -> listOf("မိမိကိုယ်ကို ယုံကြည်မှုရှိခြင်း", "စည်းကမ်းလိုက်နာခြင်း", "ဆရာ/ဆရာမ၏ ညွှန်ကြားချက်ကို လိုက်နာခြင်း")
                        "CURIOSITY" -> listOf("သင်ယူလိုစိတ်ရှိခြင်း", "စူးစမ်းမေးမြန်းခြင်း")
                        else -> listOf("သူငယ်ချင်းများနှင့် ပူးပေါင်းဆောင်ရွက်ခြင်း", "အခြေခံတစ်ကိုယ်ရေသန့်ရှင်းရေးကို ထိန်းသိမ်းခြင်း")
                    }
                    "PRIMARY" -> when (pillarKey) {
                        "HONESTY" -> listOf("တာဝန်ယူမှုရှိခြင်း", "အချိန်ကို တန်ဖိုးထားအသုံးပြုခြင်း")
                        "CURIOSITY" -> listOf("သင်ယူမှုတွင် စိတ်ပါဝင်စားခြင်း")
                        else -> listOf("အဖွဲ့လိုက်လုပ်ဆောင်နိုင်ခြင်း", "ရိုသေလေးစားမှုနှင့် ယဉ်ကျေးပျူငှာမှုရှိခြင်း")
                    }
                    "SECONDARY" -> when (pillarKey) {
                        "HONESTY" -> listOf("ခေါင်းဆောင်မှုစွမ်းရည်")
                        "CURIOSITY" -> listOf("ဝေဖန်စဉ်းစားနိုင်ခြင်း", "ပြဿနာဖြေရှင်းနိုင်စွမ်းရှိခြင်း")
                        else -> listOf("မိမိကိုယ်ကို စီမံခန့်ခွဲနိုင်ခြင်း", "လူမှုဆက်ဆံရေးကောင်းမွန်ခြင်း")
                    }
                    "HIGH_SCHOOL" -> when (pillarKey) {
                        "HONESTY" -> listOf("စည်းကမ်းနှင့် တာဝန်ယူမှု")
                        "CURIOSITY" -> listOf("မိမိရည်မှန်းချက်အတွက် ကြိုးစားအားထုတ်မှု", "ကိုယ်ပိုင်ဆုံးဖြတ်ချက်ချနိုင်မှု")
                        else -> listOf("ခေါင်းဆောင်မှုနှင့် ပူးပေါင်းဆောင်ရွက်မှု", "လူ့ကျင့်ဝတ်နှင့် ပတ်ဝန်းကျင်ဆိုင်ရာ သတိပြုမှု")
                    }
                    else -> when (pillarKey) {
                        "HONESTY" -> listOf("တာဝန်ယူမှုရှိခြင်း", "အချိန်ကို တန်ဖိုးထားအသုံးပြုခြင်း")
                        "CURIOSITY" -> listOf("သင်ယူမှုတွင် စိတ်ပါဝင်စားခြင်း")
                        else -> listOf("အဖွဲ့လိုက်လုပ်ဆောင်နိုင်ခြင်း", "ရိုသေလေးစားမှုနှင့် ယဉ်ကျေးပျူငှာမှုရှိခြင်း")
                    }
                }
                defaults.forEach { d ->
                    items.add(ReportHcmPillarItem(categoryName = d, pillar = pillarEnum, starRating = 0))
                }
            }

            val pillarAvg = if (items.isNotEmpty()) pillarSum.toDouble() / items.size else 0.0
            pillarSummaries.add(
                ReportHcmPillarSummary(
                    pillarName = pillarKey,
                    myanmarPillarName = myanmarName,
                    items = items,
                    averageStars = (pillarAvg * 10.0).roundToInt() / 10.0
                )
            )

            when (pillarKey) {
                "HONESTY" -> { sumHonesty += pillarAvg; countHonesty++ }
                "CURIOSITY" -> { sumCuriosity += pillarAvg; countCuriosity++ }
                "MINDFULNESS" -> { sumMindfulness += pillarAvg; countMindfulness++ }
            }
        }

        val honestyAvg = if (countHonesty > 0) sumHonesty / countHonesty else 0.0
        val curiosityAvg = if (countCuriosity > 0) sumCuriosity / countCuriosity else 0.0
        val mindfulnessAvg = if (countMindfulness > 0) sumMindfulness / countMindfulness else 0.0
        val overallScore = ((honestyAvg + curiosityAvg + mindfulnessAvg) / 3.0 * 10.0).roundToInt() / 10.0

        val overallLevel = when {
            overallScore >= 4.5 -> "Outstanding (ထူးချွန်)"
            overallScore >= 3.5 -> "Proficient (ကောင်းမွန်)"
            overallScore > 0 -> "Developing (တိုးတက်ရန်လို)"
            else -> "Pending (အကဲဖြတ်ဆဲ)"
        }

        val studentSgi = sgiResults.filter { it.studentId == studentId }
        val sgiPct = if (studentSgi.isNotEmpty()) {
            studentSgi.map { it.ratingStars * 20.0 }.average()
        } else if (overallScore > 0.0) {
            (overallScore / 5.0) * 100.0
        } else {
            0.0
        }

        return ReportHcmSummary(
            pillars = pillarSummaries,
            honestyAvg = (honestyAvg * 10.0).roundToInt() / 10.0,
            curiosityAvg = (curiosityAvg * 10.0).roundToInt() / 10.0,
            mindfulnessAvg = (mindfulnessAvg * 10.0).roundToInt() / 10.0,
            overallScore = overallScore,
            overallLevel = overallLevel,
            sgiGrowthPercentage = (sgiPct * 10.0).roundToInt() / 10.0
        )
    }

    private fun processTeacherComments(
        teacherComment: TeacherCommentEntity?,
        studentName: String,
        overallAverage: Double
    ): ReportTeacherComments {
        if (teacherComment != null && (teacherComment.positiveComments.isNotBlank() || teacherComment.generalComment.isNotBlank() || teacherComment.areasForImprovement.isNotBlank() || teacherComment.futureRecommendation.isNotBlank())) {
            return ReportTeacherComments(
                positiveComments = teacherComment.positiveComments,
                areasForImprovement = teacherComment.areasForImprovement,
                generalComment = teacherComment.generalComment,
                futureRecommendation = teacherComment.futureRecommendation,
                updatedBy = teacherComment.updatedBy,
                updatedAt = teacherComment.updatedAt
            )
        }

        // Leave empty when comments have not been entered by teacher yet
        return ReportTeacherComments(
            positiveComments = "",
            areasForImprovement = "",
            generalComment = "",
            futureRecommendation = "",
            updatedBy = "",
            updatedAt = teacherComment?.updatedAt ?: 0L
        )
    }
}
