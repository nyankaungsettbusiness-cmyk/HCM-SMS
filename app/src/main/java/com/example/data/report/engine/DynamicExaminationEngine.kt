package com.example.data.report.engine

import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.StudentMarkEntity
import com.example.data.report.DynamicExamCategoryTable
import com.example.data.report.DynamicSubjectScore
import com.example.data.report.PrimaryCetBlock
import com.example.data.report.PrimaryCustomExamRow
import com.example.data.report.PrimaryExamRow
import com.example.data.report.PrimaryReportSummary
import kotlin.math.roundToInt

object DynamicExaminationEngine {

    fun getCohortActiveStudentIds(
        allStudents: List<StudentEntity>,
        gradeName: String,
        studentClass: String,
        academicYear: String,
        studentMarks: List<StudentMarkEntity>,
        fallbackStudentId: Long
    ): List<Long> {
        val normGrade = gradeName.trim().lowercase()
        val isKg = normGrade == "kg" || normGrade.contains("kindergarten") || normGrade.contains("preschool")

        val cohort = allStudents.filter { st ->
            if (st.isDeleted || st.status.equals("Inactive", ignoreCase = true) || st.status.equals("Transferred", ignoreCase = true)) return@filter false
            val stGrade = st.gradeName.trim().lowercase()
            val gradeMatch = if (isKg) {
                stGrade == "kg" || stGrade.contains("kindergarten") || stGrade.contains("preschool")
            } else {
                stGrade == normGrade
            }
            val classMatch = studentClass.isBlank() || st.className.trim().equals(studentClass.trim(), ignoreCase = true)
            gradeMatch && classMatch
        }.map { it.id }.distinct()

        return if (cohort.isNotEmpty()) {
            cohort
        } else {
            val markedIds = studentMarks.map { it.studentId }.distinct()
            if (markedIds.isNotEmpty()) markedIds else listOf(fallbackStudentId)
        }
    }

    fun calculateStudentRank(
        studentId: Long,
        assessmentIds: Set<Long>,
        cohortStudentIds: List<Long>,
        studentMarks: List<StudentMarkEntity>
    ): Pair<Int?, Int> {
        val totalCohort = if (cohortStudentIds.isNotEmpty()) cohortStudentIds.size else 1

        val studentTotals: List<Pair<Long, Double>> = cohortStudentIds.mapNotNull { stId ->
            val marks = studentMarks.filter { it.studentId == stId && it.assessmentId in assessmentIds }
            val validMarks = marks.mapNotNull { it.obtainedMarks }
            if (validMarks.isNotEmpty()) {
                val totalObtained: Double = validMarks.sum()
                Pair(stId, totalObtained)
            } else {
                null
            }
        }

        if (studentTotals.isEmpty()) {
            return Pair(null, totalCohort)
        }

        val sorted = studentTotals.sortedByDescending { it.second }
        val rankIdx = sorted.indexOfFirst { it.first == studentId }
        val rank = if (rankIdx >= 0) rankIdx + 1 else null

        return Pair(rank, totalCohort)
    }

    fun generateDynamicExamTables(
        studentId: Long,
        gradeName: String,
        selectedMonthOrPeriod: String,
        allAssessments: List<AssessmentEntity>,
        studentMarks: List<StudentMarkEntity>,
        academicYear: String = "",
        allStudents: List<StudentEntity> = emptyList(),
        studentClass: String = ""
    ): List<DynamicExamCategoryTable> {
        val normalizedGrade = gradeName.trim().lowercase()
        val isKg = normalizedGrade == "kg" || normalizedGrade.contains("kindergarten") || normalizedGrade.contains("preschool")
        val isPrimary = normalizedGrade in listOf("g1", "g2", "g3", "g4", "g5", "grade 1", "grade 2", "grade 3", "grade 4", "grade 5")

        val cohortIds = getCohortActiveStudentIds(
            allStudents = allStudents,
            gradeName = gradeName,
            studentClass = studentClass,
            academicYear = academicYear,
            studentMarks = studentMarks,
            fallbackStudentId = studentId
        )

        if (isKg) {
            return generateKgExamTables(
                studentId = studentId,
                selectedMonthOrPeriod = selectedMonthOrPeriod,
                allAssessments = allAssessments,
                studentMarks = studentMarks,
                academicYear = academicYear,
                cohortIds = cohortIds
            )
        }

        if (isPrimary) {
            return generatePrimaryExamTables(
                studentId = studentId,
                gradeName = gradeName,
                selectedMonthOrPeriod = selectedMonthOrPeriod,
                allAssessments = allAssessments,
                studentMarks = studentMarks,
                academicYear = academicYear,
                cohortIds = cohortIds
            )
        }

        // 1. Filter assessments matching grade and academic year strictly
        val gradeAssessments = allAssessments.filter { asm ->
            if (asm.isDeleted) return@filter false
            val asmGrade = asm.grade.trim().uppercase()
            val normG = normalizedGrade.uppercase()

            val matchesGrade = when {
                asmGrade.isBlank() || asmGrade == "ALL GRADES" || asmGrade == "ALL" -> true
                asmGrade == normG -> true
                normG in listOf("G6", "G7", "G8", "G9") -> (asmGrade in listOf("G6", "G7", "G8", "G9") && (asmGrade == normG || asmGrade == "SECONDARY" || asmGrade == "G6-G9")) || (asmGrade == "SECONDARY" || asmGrade == "G6-G9")
                normG in listOf("G10", "G11", "G12") -> (asmGrade in listOf("G10", "G11", "G12") && (asmGrade == normG || asmGrade == "HIGH SCHOOL" || asmGrade == "G10-G12")) || (asmGrade == "HIGH SCHOOL" || asmGrade == "G10-G12")
                normG in listOf("G1", "G2", "G3", "G4", "G5") -> (asmGrade in listOf("G1", "G2", "G3", "G4", "G5") && (asmGrade == normG || asmGrade == "PRIMARY" || asmGrade == "G1-G5")) || (asmGrade == "PRIMARY" || asmGrade == "G1-G5")
                else -> false
            }

            val matchesYear = asm.academicYear.isBlank() || asm.academicYear.equals(academicYear, ignoreCase = true)

            matchesGrade && matchesYear
        }

        // 2. Filter assessments by selected month/period if specified (Weekly & Monthly filter by month; Pilot & CET do NOT)
        val filteredAssessments = if (selectedMonthOrPeriod.isBlank() ||
            selectedMonthOrPeriod.equals("All Months", ignoreCase = true) ||
            selectedMonthOrPeriod.equals("All Periods", ignoreCase = true)
        ) {
            gradeAssessments
        } else {
            val term = selectedMonthOrPeriod.lowercase()
            gradeAssessments.filter { asm ->
                val cat = asm.assessmentType.trim().uppercase()
                // Pilot Test and CET do NOT filter by report month
                if (cat.contains("PILOT") || cat.contains("CET")) {
                    true
                } else {
                    asm.month.lowercase() == term ||
                            asm.assessmentName.lowercase().contains(term) ||
                            asm.assessmentType.lowercase().contains(term) ||
                            asm.academicYear.lowercase().contains(term)
                }
            }
        }

        // 3. Find marks for this student strictly by studentId with non-null obtainedMarks
        val marksForStudent = studentMarks.filter { it.studentId == studentId && it.obtainedMarks != null && !it.isDeleted }
        val markedAssessmentIds = marksForStudent.map { it.assessmentId }.toSet()

        // Completed assessments for this student MUST have actual student marks
        val completedAssessments = filteredAssessments.filter { asm ->
            markedAssessmentIds.contains(asm.id)
        }

        if (completedAssessments.isEmpty()) {
            return emptyList()
        }

        // 4. Group completed assessments dynamically by Category
        val groupedByCategory = completedAssessments.groupBy { asm ->
            val cat = asm.assessmentType.trim().uppercase()
            when {
                cat.contains("MONTHLY") -> "MONTHLY_TESTS"
                cat.contains("CET") -> "CET_EXAMINATIONS"
                cat.contains("PILOT") -> "PILOT_TESTS"
                cat.contains("WEEKLY") -> "WEEKLY_TESTS"
                cat.contains("UNIT") || cat.contains("LESSON") -> "UNIT_ASSESSMENTS"
                else -> "OTHER_EXAMINATIONS"
            }
        }

        val resultTables = mutableListOf<DynamicExamCategoryTable>()

        groupedByCategory.forEach { (catKey, assessmentsInCat) ->
            val catTitle = when (catKey) {
                "MONTHLY_TESTS" -> "Monthly Examinations Record"
                "CET_EXAMINATIONS" -> "CET (Comprehensive Evaluation Test) Record"
                "PILOT_TESTS" -> "Pilot Exam Performance Record"
                "WEEKLY_TESTS" -> "Weekly Test Performance Record"
                "UNIT_ASSESSMENTS" -> "Unit & Lesson Completion Assessments"
                else -> "Academic Examinations Record"
            }

            val isWeekly = catKey == "WEEKLY_TESTS"
            val examTitles: List<String> = if (isWeekly) listOf("WT1", "WT2", "WT3", "WT4") else assessmentsInCat.map { asm -> asm.assessmentName }.distinct()
            val asmIdsInCat = assessmentsInCat.map { it.id }.toSet()

            // Find all marks for these assessments
            val categoryMarks = marksForStudent.filter { it.assessmentId in asmIdsInCat }

            // Group marks by Subject Name
            val subjectNames = categoryMarks.map { it.subjectName }.distinct().sorted()

            val subjectRows = mutableListOf<DynamicSubjectScore>()
            var sumObtainedAllSubjects = 0.0
            var sumMaxAllSubjects = 0.0

            subjectNames.forEach { subName ->
                val subMarks = categoryMarks.filter { it.subjectName == subName }
                val scoreMap = mutableMapOf<String, Double?>()
                var subObtained = 0.0
                var subMax = 0.0

                if (isWeekly) {
                    val fixedWeeks = listOf("WT1", "WT2", "WT3", "WT4")
                    fixedWeeks.forEach { wtKey ->
                        val matchedAsm = assessmentsInCat.find { asm ->
                            asm.weekNumber.equals(wtKey, ignoreCase = true) ||
                                    asm.assessmentName.contains(wtKey, ignoreCase = true) ||
                                    asm.assessmentType.contains(wtKey, ignoreCase = true)
                        }
                        val markObj = if (matchedAsm != null) subMarks.find { it.assessmentId == matchedAsm.id } else null
                        val markVal: Double? = markObj?.obtainedMarks
                        scoreMap[wtKey] = markVal
                        if (markVal != null) {
                            subObtained += markVal
                            subMax += markObj?.maxMarks?.toDouble() ?: 50.0
                        }
                    }
                } else {
                    assessmentsInCat.forEach { asm ->
                        val markObj = subMarks.find { it.assessmentId == asm.id }
                        val markVal: Double? = markObj?.obtainedMarks
                        scoreMap[asm.assessmentName] = markVal
                        if (markVal != null) {
                            subObtained += markVal
                            subMax += markObj.maxMarks.toDouble()
                        }
                    }
                }

                val avgPct = if (subMax > 0) (subObtained / subMax) * 100.0 else 0.0
                val gradeLetter = computeGradeLetter(avgPct)
                val isPass = avgPct >= 50.0
                val distinction = when {
                    avgPct >= 85.0 -> "Distinction (D)"
                    avgPct >= 75.0 -> "Merit (M)"
                    avgPct >= 50.0 -> "Pass (P)"
                    else -> "Needs Improvement"
                }

                subjectRows.add(
                    DynamicSubjectScore(
                        subjectName = subName,
                        category = "ACADEMIC",
                        examScores = scoreMap,
                        totalObtained = (subObtained * 10.0).roundToInt() / 10.0,
                        maxPossible = subMax,
                        averagePercentage = (avgPct * 10.0).roundToInt() / 10.0,
                        gradeLetter = gradeLetter,
                        isPass = isPass,
                        distinctionBadge = distinction
                    )
                )

                sumObtainedAllSubjects += subObtained
                sumMaxAllSubjects += subMax
            }

            val catAvg = if (sumMaxAllSubjects > 0) (sumObtainedAllSubjects / sumMaxAllSubjects) * 100.0 else 0.0

            val (rankVal, totalCohort) = calculateStudentRank(
                studentId = studentId,
                assessmentIds = asmIdsInCat,
                cohortStudentIds = cohortIds,
                studentMarks = studentMarks
            )

            resultTables.add(
                DynamicExamCategoryTable(
                    categoryKey = catKey,
                    categoryTitle = catTitle,
                    discoveredExams = examTitles,
                    subjectRows = subjectRows,
                    totalMarksAcrossExams = (sumObtainedAllSubjects * 10.0).roundToInt() / 10.0,
                    maxPossibleAcrossExams = sumMaxAllSubjects,
                    overallCategoryAverage = (catAvg * 10.0).roundToInt() / 10.0,
                    overallCategoryGrade = computeGradeLetter(catAvg),
                    classRank = rankVal,
                    totalStudentsInClass = totalCohort
                )
            )
        }

        return resultTables
    }

    private fun generateKgExamTables(
        studentId: Long,
        selectedMonthOrPeriod: String,
        allAssessments: List<AssessmentEntity>,
        studentMarks: List<StudentMarkEntity>,
        academicYear: String = "",
        cohortIds: List<Long> = emptyList()
    ): List<DynamicExamCategoryTable> {
        // Filter assessments for KG that are strictly MONTHLY TESTS only and match academic year
        val kgAssessments = allAssessments.filter { asm ->
            val g = asm.grade.trim().lowercase()
            val matchesGrade = (g == "kg" || g.contains("kindergarten") || g.contains("preschool") || g.isBlank()) &&
                    (asm.assessmentType.uppercase().contains("MONTHLY") || asm.assessmentName.uppercase().contains("MONTHLY")) &&
                    !asm.assessmentType.uppercase().contains("CET") &&
                    !asm.assessmentType.uppercase().contains("PILOT") &&
                    !asm.assessmentType.uppercase().contains("WEEKLY") &&
                    !asm.assessmentType.uppercase().contains("LESSON") &&
                    !asm.assessmentType.uppercase().contains("CUSTOM")
            val matchesYear = asm.academicYear.isBlank() || asm.academicYear.equals(academicYear, ignoreCase = true)
            matchesGrade && matchesYear
        }

        // Filter by selected month/period if specified
        val filteredMonthlyAssessments = if (selectedMonthOrPeriod.isBlank() ||
            selectedMonthOrPeriod.equals("All Months", ignoreCase = true) ||
            selectedMonthOrPeriod.equals("All Periods", ignoreCase = true)
        ) {
            kgAssessments
        } else {
            val term = selectedMonthOrPeriod.lowercase()
            kgAssessments.filter { asm ->
                asm.assessmentName.lowercase().contains(term) ||
                        asm.assessmentType.lowercase().contains(term) ||
                        asm.academicYear.lowercase().contains(term)
            }
        }

        val marksForStudent = studentMarks.filter { it.studentId == studentId && it.obtainedMarks != null && !it.isDeleted }
        val markedAssessmentIds = marksForStudent.map { it.assessmentId }.toSet()

        val completedAssessments = filteredMonthlyAssessments.filter { asm ->
            !asm.isDeleted && markedAssessmentIds.contains(asm.id)
        }

        // Rule: If there is no Monthly Test in the selected month, hide the Academic Assessment table (return empty list)
        if (completedAssessments.isEmpty()) {
            return emptyList()
        }

        val examTitles = completedAssessments.map { it.assessmentName }.distinct()
        val asmIds = completedAssessments.map { it.id }.toSet()
        val categoryMarks = marksForStudent.filter { it.assessmentId in asmIds }

        // Define KG Mandatory Subject Orders & Categories
        val academicSubjectsDef = listOf("Myanmar", "English (Phonics)", "English (Language)", "Mathematics")
        val additionalSubjectsDef = listOf("PE", "Music")

        val allKgSubjects = academicSubjectsDef + additionalSubjectsDef

        val subjectRows = mutableListOf<DynamicSubjectScore>()
        var sumObtainedAcademic = 0.0
        var sumMaxAcademic = 0.0

        allKgSubjects.forEach { subName ->
            val isAcademic = academicSubjectsDef.contains(subName)
            val subCategory = if (isAcademic) "ACADEMIC" else "ADDITIONAL"

            val subMarks = categoryMarks.filter { mark ->
                mark.subjectName.equals(subName, ignoreCase = true) ||
                        (subName == "English (Language)" && mark.subjectName.equals("English", ignoreCase = true)) ||
                        (subName == "Mathematics" && mark.subjectName.equals("Math", ignoreCase = true))
            }

            val scoreMap = mutableMapOf<String, Double?>()
            var subObtained = 0.0
            var subMax = 0.0
            var hasAnyMark = false

            completedAssessments.forEach { asm ->
                val markObj = subMarks.find { it.assessmentId == asm.id }
                val markVal: Double? = markObj?.obtainedMarks
                scoreMap[asm.assessmentName] = markVal
                if (markVal != null) {
                    subObtained += markVal
                    subMax += markObj.maxMarks.toDouble()
                    hasAnyMark = true
                }
            }

            val avgPct = if (subMax > 0) (subObtained / subMax) * 100.0 else 0.0
            val gradeLetter = if (hasAnyMark) computeGradeLetter(avgPct) else "-"
            val isPass = avgPct >= 50.0
            val distinction = when {
                !hasAnyMark -> "-"
                avgPct >= 85.0 -> "Distinction (D)"
                avgPct >= 75.0 -> "Merit (M)"
                avgPct >= 50.0 -> "Pass (P)"
                else -> "Needs Improvement"
            }

            subjectRows.add(
                DynamicSubjectScore(
                    subjectName = subName,
                    category = subCategory,
                    examScores = scoreMap,
                    totalObtained = if (hasAnyMark) (subObtained * 10.0).roundToInt() / 10.0 else 0.0,
                    maxPossible = if (hasAnyMark) subMax else 0.0,
                    averagePercentage = if (hasAnyMark) (avgPct * 10.0).roundToInt() / 10.0 else 0.0,
                    gradeLetter = gradeLetter,
                    isPass = isPass,
                    distinctionBadge = distinction
                )
            )

            // Include ONLY the academic subjects with actual marks in Total Marks
            if (isAcademic && hasAnyMark) {
                sumObtainedAcademic += subObtained
                sumMaxAcademic += subMax
            }
        }

        val catAvg = if (sumMaxAcademic > 0) (sumObtainedAcademic / sumMaxAcademic) * 100.0 else 0.0

        val (rankVal, totalCohort) = calculateStudentRank(
            studentId = studentId,
            assessmentIds = asmIds,
            cohortStudentIds = cohortIds,
            studentMarks = studentMarks
        )

        return listOf(
            DynamicExamCategoryTable(
                categoryKey = "MONTHLY_TESTS",
                categoryTitle = "Monthly Assessment (Kindergarten)",
                discoveredExams = examTitles,
                subjectRows = subjectRows,
                totalMarksAcrossExams = (sumObtainedAcademic * 10.0).roundToInt() / 10.0,
                maxPossibleAcrossExams = sumMaxAcademic,
                overallCategoryAverage = (catAvg * 10.0).roundToInt() / 10.0,
                overallCategoryGrade = computeGradeLetter(catAvg),
                classRank = rankVal,
                totalStudentsInClass = totalCohort
            )
        )
    }

    private fun computeGradeLetter(pct: Double): String {
        return when {
            pct >= 90.0 -> "A+"
            pct >= 80.0 -> "A"
            pct >= 70.0 -> "B"
            pct >= 60.0 -> "C"
            pct >= 50.0 -> "D"
            else -> "F"
        }
    }

    private fun generatePrimaryExamTables(
        studentId: Long,
        gradeName: String,
        selectedMonthOrPeriod: String,
        allAssessments: List<AssessmentEntity>,
        studentMarks: List<StudentMarkEntity>,
        academicYear: String = "",
        cohortIds: List<Long> = emptyList()
    ): List<DynamicExamCategoryTable> {
        val summary = buildPrimaryReportSummary(
            studentId = studentId,
            gradeName = gradeName,
            selectedMonthOrPeriod = selectedMonthOrPeriod,
            allAssessments = allAssessments,
            studentMarks = studentMarks,
            academicYear = academicYear,
            cohortIds = cohortIds
        )

        val resultTables = mutableListOf<DynamicExamCategoryTable>()

        // 1. Monthly Tests Category Table
        if (summary.monthlyTests.isNotEmpty()) {
            val rows = summary.monthlyTests.map { row ->
                DynamicSubjectScore(
                    subjectName = row.examName,
                    category = "ACADEMIC",
                    examScores = mapOf("Total" to row.totalObtainedCore),
                    totalObtained = row.totalObtainedCore,
                    maxPossible = row.maxPossibleCore,
                    averagePercentage = if (row.maxPossibleCore > 0) (row.totalObtainedCore / row.maxPossibleCore) * 100.0 else 0.0,
                    gradeLetter = computeGradeLetter(if (row.maxPossibleCore > 0) (row.totalObtainedCore / row.maxPossibleCore) * 100.0 else 0.0),
                    isPass = row.totalObtainedCore >= (row.maxPossibleCore * 0.5),
                    distinctionBadge = row.distinctionBadge
                )
            }
            val avg = if (rows.isNotEmpty()) rows.map { it.averagePercentage }.average() else 0.0

            val asmIds = allAssessments.filter { asm ->
                val cat = asm.assessmentType.uppercase()
                val name = asm.assessmentName.uppercase()
                (cat.contains("MONTHLY") || name.contains("MONTHLY")) && !cat.contains("CET") && !cat.contains("UNIT") && !cat.contains("CUSTOM")
            }.map { it.id }.toSet()

            val (rankVal, totalCohort) = calculateStudentRank(
                studentId = studentId,
                assessmentIds = asmIds,
                cohortStudentIds = cohortIds,
                studentMarks = studentMarks
            )

            resultTables.add(
                DynamicExamCategoryTable(
                    categoryKey = "MONTHLY_TESTS",
                    categoryTitle = "Monthly Examinations (Primary G1-G4)",
                    discoveredExams = summary.monthlyTests.map { it.examName },
                    subjectRows = rows,
                    totalMarksAcrossExams = rows.sumOf { it.totalObtained },
                    maxPossibleAcrossExams = rows.sumOf { it.maxPossible },
                    overallCategoryAverage = (avg * 10.0).roundToInt() / 10.0,
                    overallCategoryGrade = computeGradeLetter(avg),
                    classRank = rankVal,
                    totalStudentsInClass = totalCohort
                )
            )
        }

        // 2. Unit Tests Category Table (if completed)
        if (summary.unitTests.isNotEmpty()) {
            val rows = summary.unitTests.map { row ->
                DynamicSubjectScore(
                    subjectName = row.examName,
                    category = "ACADEMIC",
                    examScores = mapOf("Total" to row.totalObtainedCore),
                    totalObtained = row.totalObtainedCore,
                    maxPossible = row.maxPossibleCore,
                    averagePercentage = if (row.maxPossibleCore > 0) (row.totalObtainedCore / row.maxPossibleCore) * 100.0 else 0.0,
                    gradeLetter = computeGradeLetter(if (row.maxPossibleCore > 0) (row.totalObtainedCore / row.maxPossibleCore) * 100.0 else 0.0),
                    isPass = row.totalObtainedCore >= (row.maxPossibleCore * 0.5),
                    distinctionBadge = row.distinctionBadge
                )
            }
            val avg = if (rows.isNotEmpty()) rows.map { it.averagePercentage }.average() else 0.0

            val unitAsmIds = allAssessments.filter { asm ->
                val cat = asm.assessmentType.uppercase()
                val name = asm.assessmentName.uppercase()
                cat.contains("UNIT") || name.contains("UNIT") || cat.contains("LESSON")
            }.map { it.id }.toSet()

            val (rankVal, totalCohort) = calculateStudentRank(
                studentId = studentId,
                assessmentIds = unitAsmIds,
                cohortStudentIds = cohortIds,
                studentMarks = studentMarks
            )

            resultTables.add(
                DynamicExamCategoryTable(
                    categoryKey = "UNIT_TESTS",
                    categoryTitle = "Unit Tests (Primary G1-G4)",
                    discoveredExams = summary.unitTests.map { it.examName },
                    subjectRows = rows,
                    totalMarksAcrossExams = rows.sumOf { it.totalObtained },
                    maxPossibleAcrossExams = rows.sumOf { it.maxPossible },
                    overallCategoryAverage = (avg * 10.0).roundToInt() / 10.0,
                    overallCategoryGrade = computeGradeLetter(avg),
                    classRank = rankVal,
                    totalStudentsInClass = totalCohort
                )
            )
        }

        // 3. CET Blocks Category Table (G4 only)
        if (summary.isG4 && summary.cetBlocks.isNotEmpty()) {
            summary.cetBlocks.forEach { cet ->
                resultTables.add(
                    DynamicExamCategoryTable(
                        categoryKey = "CET_EXAMINATIONS",
                        categoryTitle = cet.cetTitle,
                        discoveredExams = listOf(cet.cetTitle),
                        subjectRows = cet.subjectRows,
                        totalMarksAcrossExams = cet.totalObtainedCore,
                        maxPossibleAcrossExams = cet.maxPossibleCore,
                        overallCategoryAverage = if (cet.maxPossibleCore > 0) (cet.totalObtainedCore / cet.maxPossibleCore) * 100.0 else 0.0,
                        overallCategoryGrade = computeGradeLetter(if (cet.maxPossibleCore > 0) (cet.totalObtainedCore / cet.maxPossibleCore) * 100.0 else 0.0),
                        classRank = cet.classRank.split("/").firstOrNull()?.toIntOrNull(),
                        totalStudentsInClass = cet.classRank.split("/").getOrNull(1)?.toIntOrNull() ?: cohortIds.size
                    )
                )
            }
        }

        // 4. Custom Exam (English International) Category Table
        if (summary.customExams.isNotEmpty()) {
            val rows = summary.customExams.map { row ->
                DynamicSubjectScore(
                    subjectName = "${row.subjectName} (${row.examName})",
                    category = "ADDITIONAL",
                    examScores = mapOf("Score" to row.obtainedMark),
                    totalObtained = row.obtainedMark,
                    maxPossible = row.maxMark,
                    averagePercentage = if (row.maxMark > 0) (row.obtainedMark / row.maxMark) * 100.0 else 0.0,
                    gradeLetter = computeGradeLetter(if (row.maxMark > 0) (row.obtainedMark / row.maxMark) * 100.0 else 0.0),
                    isPass = row.obtainedMark >= (row.maxMark * 0.5),
                    distinctionBadge = "-"
                )
            }
            resultTables.add(
                DynamicExamCategoryTable(
                    categoryKey = "CUSTOM_EXAMS",
                    categoryTitle = "Custom Exam (English International)",
                    discoveredExams = summary.customExams.map { it.examName },
                    subjectRows = rows,
                    totalMarksAcrossExams = rows.sumOf { it.totalObtained },
                    maxPossibleAcrossExams = rows.sumOf { it.maxPossible },
                    overallCategoryAverage = if (rows.isNotEmpty()) rows.map { it.averagePercentage }.average() else 0.0,
                    overallCategoryGrade = "-",
                    classRank = summary.customExams.firstOrNull()?.classRank?.split("/")?.firstOrNull()?.toIntOrNull(),
                    totalStudentsInClass = summary.customExams.firstOrNull()?.classRank?.split("/")?.getOrNull(1)?.toIntOrNull() ?: cohortIds.size
                )
            )
        }

        // 5. Pilot Tests for Primary (if created with marks)
        val pilotAsms = allAssessments.filter { asm ->
            val cat = asm.assessmentType.uppercase()
            val name = asm.assessmentName.uppercase()
            cat.contains("PILOT") || name.contains("PILOT")
        }
        val marksForStudent = studentMarks.filter { it.studentId == studentId }
        val pilotAsmsWithMarks = pilotAsms.filter { asm -> marksForStudent.any { it.assessmentId == asm.id } }
        if (pilotAsmsWithMarks.isNotEmpty()) {
            val pilotGrouped = pilotAsmsWithMarks.groupBy { it.assessmentName }
            pilotGrouped.forEach { (pName, asms) ->
                val pAsmIds = asms.map { it.id }.toSet()
                val coreSubjectsDef = listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")
                val subRows = mutableListOf<DynamicSubjectScore>()
                var totalObt = 0.0
                var totalMax = 0.0

                coreSubjectsDef.forEach { subName ->
                    val markObj = marksForStudent.find { mark ->
                        mark.assessmentId in pAsmIds && (
                            mark.subjectName.equals(subName, ignoreCase = true) ||
                            (subName == "Mathematics" && mark.subjectName.equals("Math", ignoreCase = true)) ||
                            (subName == "Social Studies" && mark.subjectName.equals("Social", ignoreCase = true))
                        )
                    }
                    val obt = markObj?.obtainedMarks
                    val maxM = markObj?.maxMarks?.toDouble() ?: 0.0
                    if (obt != null) {
                        totalObt += obt
                        totalMax += maxM
                        val pct = if (maxM > 0) (obt / maxM) * 100.0 else 0.0
                        subRows.add(
                            DynamicSubjectScore(
                                subjectName = subName,
                                category = "ACADEMIC",
                                examScores = mapOf(pName to obt),
                                totalObtained = obt,
                                maxPossible = maxM,
                                averagePercentage = (pct * 10.0).roundToInt() / 10.0,
                                gradeLetter = computeGradeLetter(pct),
                                isPass = pct >= 50.0,
                                distinctionBadge = if (pct >= 80.0) "Distinction (D)" else "-"
                            )
                        )
                    }
                }
                if (subRows.isNotEmpty()) {
                    val (rankVal, totalCohort) = calculateStudentRank(
                        studentId = studentId,
                        assessmentIds = pAsmIds,
                        cohortStudentIds = cohortIds,
                        studentMarks = studentMarks
                    )
                    resultTables.add(
                        DynamicExamCategoryTable(
                            categoryKey = "PILOT_TESTS",
                            categoryTitle = pName,
                            discoveredExams = listOf(pName),
                            subjectRows = subRows,
                            totalMarksAcrossExams = (totalObt * 10.0).roundToInt() / 10.0,
                            maxPossibleAcrossExams = totalMax,
                            overallCategoryAverage = if (totalMax > 0) (totalObt / totalMax) * 100.0 else 0.0,
                            overallCategoryGrade = computeGradeLetter(if (totalMax > 0) (totalObt / totalMax) * 100.0 else 0.0),
                            classRank = rankVal,
                            totalStudentsInClass = totalCohort
                        )
                    )
                }
            }
        }

        return resultTables
    }

    fun buildPrimaryReportSummary(
        studentId: Long,
        gradeName: String,
        selectedMonthOrPeriod: String,
        allAssessments: List<AssessmentEntity>,
        studentMarks: List<StudentMarkEntity>,
        academicYear: String = "",
        cohortIds: List<Long> = emptyList()
    ): PrimaryReportSummary {
        val normGrade = gradeName.trim().uppercase()
        val isG4 = normGrade == "G4" || normGrade == "GRADE 4"

        val marksForStudent = studentMarks.filter { it.studentId == studentId && it.obtainedMarks != null && !it.isDeleted }
        val markedAssessmentIds = marksForStudent.map { it.assessmentId }.toSet()

        val primaryAssessments = allAssessments.filter { asm ->
            if (asm.isDeleted) return@filter false
            val g = asm.grade.trim().uppercase()
            val matchesGrade = g == normGrade || (normGrade in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5") && (g == "PRIMARY" || g == "KG-G5" || g.isBlank()))
            val matchesYear = asm.academicYear.isBlank() || asm.academicYear.equals(academicYear, ignoreCase = true)
            matchesGrade && matchesYear
        }

        val completedAssessments = primaryAssessments.filter { asm ->
            markedAssessmentIds.contains(asm.id)
        }

        val coreSubjectsDef = listOf("Myanmar", "English", "Mathematics", "Science", "Social Studies")

        // 1. Monthly Tests (one exam = one row)
        val monthlyAssessments = completedAssessments.filter { asm ->
            val cat = asm.assessmentType.uppercase()
            val name = asm.assessmentName.uppercase()
            (cat.contains("MONTHLY") || name.contains("MONTHLY")) &&
                    !cat.contains("CET") && !cat.contains("UNIT") && !cat.contains("CUSTOM")
        }

        val monthlyRows = if (monthlyAssessments.isNotEmpty()) {
            monthlyAssessments.map { asm ->
                val asmMarks = marksForStudent.filter { it.assessmentId == asm.id }
                var obtainedSum = 0.0
                var maxSum = 0.0
                var distCount = 0
                val scoreMap = mutableMapOf<String, Double?>()

                coreSubjectsDef.forEach { subName ->
                    val markObj = asmMarks.find { mark ->
                        mark.subjectName.equals(subName, ignoreCase = true) ||
                                (subName == "Mathematics" && mark.subjectName.equals("Math", ignoreCase = true)) ||
                                (subName == "Social Studies" && mark.subjectName.equals("Social", ignoreCase = true))
                    }
                    val markVal = markObj?.obtainedMarks
                    scoreMap[subName] = markVal
                    if (markVal != null) {
                        obtainedSum += markVal
                        val maxVal = markObj.maxMarks.toDouble()
                        maxSum += maxVal
                        val pct = if (maxVal > 0) (markVal / maxVal) * 100.0 else 0.0
                        if (pct >= 80.0) distCount++
                    }
                }

                val (rankVal, totalCohort) = calculateStudentRank(
                    studentId = studentId,
                    assessmentIds = setOf(asm.id),
                    cohortStudentIds = cohortIds,
                    studentMarks = studentMarks
                )
                val rankStr = if (rankVal != null) "$rankVal/$totalCohort" else "-"

                PrimaryExamRow(
                    examName = asm.assessmentName,
                    examType = "MONTHLY",
                    totalObtainedCore = (obtainedSum * 10.0).roundToInt() / 10.0,
                    maxPossibleCore = if (maxSum > 0) maxSum else 0.0,
                    classRank = rankStr,
                    distinctionBadge = if (distCount > 0) "${distCount}D" else "-",
                    coreSubjectScores = scoreMap
                )
            }
        } else {
            emptyList()
        }

        // 2. Unit Tests (Display only if completed)
        val unitAssessments = completedAssessments.filter { asm ->
            val cat = asm.assessmentType.uppercase()
            val name = asm.assessmentName.uppercase()
            cat.contains("UNIT") || name.contains("UNIT") || cat.contains("LESSON")
        }

        val unitRows = unitAssessments.map { asm ->
            val asmMarks = marksForStudent.filter { it.assessmentId == asm.id }
            var obtainedSum = 0.0
            var maxSum = 0.0
            var distCount = 0
            val scoreMap = mutableMapOf<String, Double?>()

            coreSubjectsDef.forEach { subName ->
                val markObj = asmMarks.find { mark ->
                    mark.subjectName.equals(subName, ignoreCase = true) ||
                            (subName == "Mathematics" && mark.subjectName.equals("Math", ignoreCase = true)) ||
                            (subName == "Social Studies" && mark.subjectName.equals("Social", ignoreCase = true))
                }
                val markVal = markObj?.obtainedMarks
                scoreMap[subName] = markVal
                if (markVal != null) {
                    obtainedSum += markVal
                    val maxVal = markObj.maxMarks.toDouble()
                    maxSum += maxVal
                    val pct = if (maxVal > 0) (markVal / maxVal) * 100.0 else 0.0
                    if (pct >= 80.0) distCount++
                }
            }

            val (rankVal, totalCohort) = calculateStudentRank(
                studentId = studentId,
                assessmentIds = setOf(asm.id),
                cohortStudentIds = cohortIds,
                studentMarks = studentMarks
            )
            val rankStr = if (rankVal != null) "$rankVal/$totalCohort" else "-"

            PrimaryExamRow(
                examName = asm.assessmentName,
                examType = "UNIT",
                totalObtainedCore = (obtainedSum * 10.0).roundToInt() / 10.0,
                maxPossibleCore = if (maxSum > 0) maxSum else 0.0,
                classRank = rankStr,
                distinctionBadge = if (distCount > 0) "${distCount}D" else "-",
                coreSubjectScores = scoreMap
            )
        }

        // 3. CET Blocks (G4 only: CET 1, CET 2, CET 3, CET 4)
        val cetBlocks = if (isG4) {
            val cetAssessments = completedAssessments.filter { asm ->
                val cat = asm.assessmentType.uppercase()
                val name = asm.assessmentName.uppercase()
                cat.contains("CET") || name.contains("CET")
            }

            val blocks = mutableListOf<PrimaryCetBlock>()

            cetAssessments.forEach { matchedAsm ->
                val asmMarks = marksForStudent.filter { it.assessmentId == matchedAsm.id }
                val subRows = mutableListOf<DynamicSubjectScore>()
                var totalObt = 0.0
                var totalMax = 0.0
                var distCount = 0

                coreSubjectsDef.forEach { subName ->
                    val markObj = asmMarks.find { mark ->
                        mark.subjectName.equals(subName, ignoreCase = true) ||
                                (subName == "Mathematics" && mark.subjectName.equals("Math", ignoreCase = true)) ||
                                (subName == "Social Studies" && mark.subjectName.equals("Social", ignoreCase = true))
                    }
                    val obt = markObj?.obtainedMarks
                    val maxM = markObj?.maxMarks?.toDouble() ?: 0.0
                    if (obt != null) {
                        totalObt += obt
                        totalMax += maxM
                        val pct = if (maxM > 0) (obt / maxM) * 100.0 else 0.0
                        if (pct >= 80.0) distCount++

                        val gradeLetter = computeGradeLetter(pct)
                        val distBadge = when {
                            pct >= 85.0 -> "Distinction (D)"
                            pct >= 75.0 -> "Merit (M)"
                            pct >= 50.0 -> "Pass (P)"
                            else -> "Needs Improvement"
                        }

                        subRows.add(
                            DynamicSubjectScore(
                                subjectName = subName,
                                category = "ACADEMIC",
                                examScores = mapOf(matchedAsm.assessmentName to obt),
                                totalObtained = obt,
                                maxPossible = maxM,
                                averagePercentage = (pct * 10.0).roundToInt() / 10.0,
                                gradeLetter = gradeLetter,
                                isPass = pct >= 50.0,
                                distinctionBadge = distBadge
                            )
                        )
                    } else {
                        subRows.add(
                            DynamicSubjectScore(
                                subjectName = subName,
                                category = "ACADEMIC",
                                examScores = mapOf(matchedAsm.assessmentName to null),
                                totalObtained = 0.0,
                                maxPossible = 0.0,
                                averagePercentage = 0.0,
                                gradeLetter = "-",
                                isPass = false,
                                distinctionBadge = "-"
                            )
                        )
                    }
                }

                val (rankVal, totalCohort) = calculateStudentRank(
                    studentId = studentId,
                    assessmentIds = setOf(matchedAsm.id),
                    cohortStudentIds = cohortIds,
                    studentMarks = studentMarks
                )
                val rankStr = if (rankVal != null) "$rankVal/$totalCohort" else "-"

                blocks.add(
                    PrimaryCetBlock(
                        cetTitle = matchedAsm.assessmentName,
                        subjectRows = subRows,
                        totalObtainedCore = (totalObt * 10.0).roundToInt() / 10.0,
                        maxPossibleCore = totalMax,
                        classRank = rankStr,
                        distinctionBadge = if (distCount > 0) "${distCount}D" else "-"
                    )
                )
            }
            blocks
        } else {
            emptyList()
        }

        // 4. Custom Exam (English International ONLY)
        val customAssessments = completedAssessments.filter { asm ->
            val cat = asm.assessmentType.uppercase()
            val name = asm.assessmentName.uppercase()
            cat.contains("CUSTOM") || name.contains("CUSTOM") || name.contains("INTERNATIONAL") || name.contains("ENGLISH (INT)")
        }

        val customRows = if (customAssessments.isNotEmpty()) {
            customAssessments.mapNotNull { asm ->
                val asmMark = marksForStudent.find { mark ->
                    mark.assessmentId == asm.id && (mark.subjectName.contains("International", ignoreCase = true) || mark.subjectName.contains("English", ignoreCase = true))
                }
                val obt = asmMark?.obtainedMarks
                val maxM = asmMark?.maxMarks?.toDouble() ?: 100.0

                if (obt != null) {
                    val (rankVal, totalCohort) = calculateStudentRank(
                        studentId = studentId,
                        assessmentIds = setOf(asm.id),
                        cohortStudentIds = cohortIds,
                        studentMarks = studentMarks
                    )
                    val rankStr = if (rankVal != null) "$rankVal/$totalCohort" else "-"

                    PrimaryCustomExamRow(
                        examName = asm.assessmentName,
                        subjectName = "English (International)",
                        obtainedMark = obt,
                        maxMark = maxM,
                        classRank = rankStr
                    )
                } else null
            }
        } else {
            emptyList()
        }

        return PrimaryReportSummary(
            monthlyTests = monthlyRows,
            unitTests = unitRows,
            cetBlocks = cetBlocks,
            customExams = customRows,
            isG4 = isG4
        )
    }
}
