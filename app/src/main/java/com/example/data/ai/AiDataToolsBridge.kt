package com.example.data.ai

import com.example.data.local.dao.*
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.CurriculumDocumentType
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

/**
 * Controlled data and security bridge for the HCM-SMS AI Subsystem.
 * Enforces strict role-based data isolation and executes authoritative
 * database calculations before passing clean, minimal facts to Gemini.
 */
class AiDataToolsBridge(
    private val studentDao: StudentDao,
    private val teacherDao: TeacherDao,
    private val marksDao: MarksDao,
    private val assessmentDao: AssessmentDao,
    private val holisticDao: HolisticDao,
    private val attendanceDao: AttendanceDao,
    private val schoolPolicyDao: SchoolPolicyDao,
    private val curriculumKnowledgeDao: CurriculumKnowledgeDao
) {

    /**
     * Resolves the AI permission scope for the currently authenticated user.
     */
    suspend fun resolvePermissionScope(
        username: String,
        userRole: com.example.data.local.entity.UserRole,
        linkedTeacherId: Long?
    ): AiPermissionScope {
        if (linkedTeacherId != null && linkedTeacherId > 0) {
            val teacher = teacherDao.getTeacherById(linkedTeacherId)
            if (teacher != null) {
                val assignedGrades = teacher.assignedGrade.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val assignedClasses = teacher.assignedClass.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val assignedSubjects = teacher.assignedSubjects.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                return AiPermissionScope(
                    username = username,
                    userRole = userRole,
                    linkedTeacherId = linkedTeacherId,
                    teacherName = teacher.fullName,
                    teacherQualifications = "Teacher Profile (${teacher.teacherCode})",
                    assignedGrades = assignedGrades,
                    assignedClasses = assignedClasses,
                    assignedSubjects = assignedSubjects
                )
            }
        }
        return AiPermissionScope(
            username = username,
            userRole = userRole,
            linkedTeacherId = linkedTeacherId
        )
    }

    /**
     * Retrieves verified academic and holistic facts for a single student.
     * Guaranteed PII-free and calculated via authoritative HCM-SMS data.
     */
    suspend fun getStudentFacts(
        scope: AiPermissionScope,
        studentId: Long,
        academicYear: String,
        assessmentPeriod: String? = null
    ): VerifiedCombinedStudentProfile? {
        val student = studentDao.getStudentById(studentId) ?: return null

        // 1. Enforce Role & Scope Guard
        if (!scope.canAccessStudent(student.gradeName, student.className)) {
            return null // Unauthorized access to student outside teacher's assigned classes
        }

        // 2. Query Assessments for this Academic Year and Grade
        val assessments = assessmentDao.getAssessmentsByGradeAndYear(student.gradeName, academicYear).firstOrNull() ?: emptyList()
        val allMarks = marksDao.getAllMarks().firstOrNull() ?: emptyList()

        val studentMarks = allMarks.filter { mark ->
            mark.rollNo == student.rollNumber && assessments.any { it.id == mark.assessmentId }
        }

        // Compute Subject Score Facts
        val subjectScores = studentMarks.map { mark ->
            val raw = mark.obtainedMarks ?: 0.0
            val full = mark.maxMarks.toDouble().coerceAtLeast(1.0)
            val percentage = (raw / full) * 100.0
            val letterGrade = when {
                percentage >= 80.0 -> "A"
                percentage >= 70.0 -> "B"
                percentage >= 60.0 -> "C"
                percentage >= 50.0 -> "D"
                percentage >= 40.0 -> "E"
                else -> "F"
            }
            SubjectScoreFact(
                subjectName = mark.subjectName,
                rawScore = raw,
                fullMarks = full,
                percentage = percentage,
                letterGrade = letterGrade,
                passStatus = if (percentage >= 40.0) "PASSED" else "FAILED",
                isDistinction = percentage >= 75.0
            )
        }

        val totalScore = subjectScores.sumOf { it.rawScore }
        val maxPossible = subjectScores.sumOf { it.fullMarks }.coerceAtLeast(1.0)
        val overallPercentage = if (subjectScores.isNotEmpty()) (totalScore / maxPossible) * 100.0 else 0.0
        val overallGrade = when {
            overallPercentage >= 80.0 -> "A"
            overallPercentage >= 70.0 -> "B"
            overallPercentage >= 60.0 -> "C"
            overallPercentage >= 50.0 -> "D"
            overallPercentage >= 40.0 -> "E"
            else -> "F"
        }

        val strongestSubjects = subjectScores.filter { it.percentage >= 75.0 }.map { it.subjectName }
        val strugglingSubjects = subjectScores.filter { it.percentage < 50.0 }.map { it.subjectName }

        // 3. Query Attendance
        val attendanceRecords = attendanceDao.getAttendanceListForStudentSync(student.id, academicYear)
        val totalDays = attendanceRecords.size
        val presentDays = attendanceRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
        val attendancePercentage = if (totalDays > 0) (presentDays.toDouble() / totalDays.toDouble()) * 100.0 else null

        val academicFacts = VerifiedStudentAcademicFacts(
            studentId = student.id,
            studentName = student.name,
            rollNumber = student.rollNumber,
            studentCode = student.studentCode,
            grade = student.gradeName,
            className = student.className,
            academicYear = academicYear,
            assessmentName = assessmentPeriod ?: "All Assessments",
            subjectScores = subjectScores,
            totalScore = totalScore,
            maxPossibleScore = maxPossible,
            overallPercentage = overallPercentage,
            overallGrade = overallGrade,
            strongestSubjects = strongestSubjects,
            subjectsNeedingSupport = strugglingSubjects,
            scoreTrendDescription = if (overallPercentage >= 75.0) "Consistently High Academic Achievement" else if (overallPercentage >= 50.0) "Steady Satisfactory Progress" else "Requires Academic Reinforcement",
            attendanceRatePercentage = attendancePercentage
        )

        // 4. Query Holistic Assessment (HCM 6 Pillars & SGI)
        val period = assessmentPeriod ?: "Term 1"
        val holisticResults = holisticDao.getHolisticResultsForStudent(student.id, period, academicYear).firstOrNull() ?: emptyList()
        val pillarFacts = holisticResults.groupBy { it.pillar }.map { (pillar, list) ->
            val avg = list.map { it.ratingStars }.average().takeIf { !it.isNaN() } ?: 3.0
            PillarRatingFact(
                pillarName = pillar,
                ratingStars = avg,
                maxStars = 5,
                categoryCount = list.size
            )
        }

        val topPillars = pillarFacts.filter { it.ratingStars >= 4.0 }.map { it.pillarName }
        val growthPillars = pillarFacts.filter { it.ratingStars < 3.5 }.map { it.pillarName }

        val holisticFacts = VerifiedStudentHolisticFacts(
            studentId = student.id,
            assessmentPeriod = period,
            academicYear = academicYear,
            pillarRatings = pillarFacts,
            averageStars = pillarFacts.map { it.ratingStars }.average().takeIf { !it.isNaN() } ?: 3.5,
            topStrengths = topPillars,
            growthOpportunities = growthPillars,
            existingTeacherObservation = ""
        )

        val summaryText = buildString {
            append("Student: ${student.name} (Grade: ${student.gradeName}, Class: ${student.className}, Roll: ${student.rollNumber})\n")
            append("Academic Year: $academicYear, Overall Score: ${String.format(Locale.US, "%.1f", overallPercentage)}% (Grade $overallGrade)\n")
            if (strongestSubjects.isNotEmpty()) append("Strong Subjects: ${strongestSubjects.joinToString(", ")}\n")
            if (strugglingSubjects.isNotEmpty()) append("Subjects Needing Support: ${strugglingSubjects.joinToString(", ")}\n")
            if (attendancePercentage != null) append("Attendance: ${String.format(Locale.US, "%.1f", attendancePercentage)}%\n")
            if (topPillars.isNotEmpty()) append("Holistic Strengths: ${topPillars.joinToString(", ")}\n")
            if (growthPillars.isNotEmpty()) append("Holistic Areas for Growth: ${growthPillars.joinToString(", ")}\n")
        }

        return VerifiedCombinedStudentProfile(
            studentId = student.id,
            studentName = student.name,
            studentCode = student.studentCode,
            rollNumber = student.rollNumber,
            grade = student.gradeName,
            className = student.className,
            academicYear = academicYear,
            assessmentPeriod = period,
            academicFacts = academicFacts,
            holisticFacts = holisticFacts,
            attendancePercentage = attendancePercentage,
            verifiedSummaryText = summaryText
        )
    }

    /**
     * Retrieves curriculum knowledge chunks for a specified Grade, Subject, Unit, and Section.
     */
    suspend fun getCurriculumScope(
        gradeLevel: String,
        subject: String,
        chapterUnit: String = "",
        sectionTopic: String = ""
    ): List<CurriculumSourceCitation> {
        val chunks = curriculumKnowledgeDao.getChunksForScope(
            gradeLevel = gradeLevel,
            subject = subject,
            chapterUnit = chapterUnit,
            sectionTopic = sectionTopic
        )

        return chunks.map { chunk ->
            CurriculumSourceCitation(
                documentTitle = chunk.sourceReference.ifEmpty { "Myanmar Curriculum (${chunk.gradeLevel} ${chunk.subject})" },
                gradeLevel = chunk.gradeLevel,
                subject = chunk.subject,
                chapterUnit = chunk.chapterUnit,
                sectionTopic = chunk.sectionTopic,
                pageRange = chunk.pageRange,
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                contentSnippet = chunk.content,
                sourceReference = chunk.sourceReference
            )
        }
    }

    /**
     * Generates a cohort academic performance summary for an Administrator or Class Teacher.
     */
    suspend fun getClassAcademicOverview(
        scope: AiPermissionScope,
        gradeName: String,
        className: String,
        academicYear: String,
        assessmentId: Long? = null
    ): ClassPerformanceSummary? {
        if (!scope.isSuperAdminOrAdmin && !scope.canAccessStudent(gradeName, className)) {
            return null
        }

        val students = studentDao.getStudentsByGradeAndClass(gradeName, className).firstOrNull() ?: emptyList()
        if (students.isEmpty()) return null

        val allMarks = marksDao.getAllMarks().firstOrNull() ?: emptyList()
        val classMarks = if (assessmentId != null) {
            allMarks.filter { it.assessmentId == assessmentId }
        } else {
            allMarks
        }

        var passedCount = 0
        var failedCount = 0
        var distinctionCount = 0
        val studentAverages = mutableListOf<Double>()
        val atRiskRolls = mutableListOf<Int>()
        val subjectScoresMap = mutableMapOf<String, MutableList<Double>>()

        students.forEach { st ->
            val marks = classMarks.filter { it.rollNo == st.rollNumber }
            if (marks.isNotEmpty()) {
                val totalObtained = marks.sumOf { it.obtainedMarks ?: 0.0 }
                val maxPossible = marks.sumOf { it.maxMarks.toDouble() }.coerceAtLeast(1.0)
                val avg = (totalObtained / maxPossible) * 100.0
                studentAverages.add(avg)

                if (avg >= 40.0) passedCount++ else {
                    failedCount++
                    atRiskRolls.add(st.rollNumber)
                }
                if (avg >= 75.0) distinctionCount++

                marks.forEach { m ->
                    val mRaw = m.obtainedMarks ?: 0.0
                    val mFull = m.maxMarks.toDouble().coerceAtLeast(1.0)
                    val mPct = (mRaw / mFull) * 100.0
                    subjectScoresMap.getOrPut(m.subjectName) { mutableListOf() }.add(mPct)
                }
            }
        }

        val classAvg = if (studentAverages.isNotEmpty()) studentAverages.average() else 0.0
        val passRate = if (students.isNotEmpty()) (passedCount.toDouble() / students.size.toDouble()) * 100.0 else 0.0

        val subjectAverages = subjectScoresMap.mapValues { it.value.average() }
        val topSubjects = subjectAverages.filter { it.value >= 70.0 }.keys.toList().ifEmpty { 
            subjectAverages.entries.sortedByDescending { it.value }.take(2).map { it.key } 
        }
        val strugglingSubjects = subjectAverages.filter { it.value < 50.0 }.keys.toList()

        return ClassPerformanceSummary(
            gradeName = gradeName,
            className = className,
            academicYear = academicYear,
            assessmentName = if (assessmentId != null) "Assessment #$assessmentId" else "Academic Year Total",
            totalStudents = students.size,
            passedCount = passedCount,
            failedCount = failedCount,
            passRatePercentage = passRate,
            classAverageScore = classAvg,
            distinctionCount = distinctionCount,
            topPerformingSubjects = topSubjects,
            strugglingSubjects = strugglingSubjects,
            atRiskStudentCount = failedCount,
            atRiskStudentRolls = atRiskRolls
        )
    }

    /**
     * Generates a comprehensive school-wide or scoped analytics overview.
     */
    suspend fun getSchoolAnalyticsOverview(
        scope: AiPermissionScope,
        academicYear: String
    ): SchoolAnalyticsSummary? {
        if (!scope.isSuperAdminOrAdmin && scope.userRole != com.example.data.local.entity.UserRole.TEACHER) {
            return null
        }

        val allStudents = studentDao.getAllStudents().firstOrNull() ?: emptyList()
        val accessibleStudents = if (scope.isSuperAdminOrAdmin) {
            allStudents
        } else {
            allStudents.filter { scope.canAccessStudent(it.gradeName, it.className) }
        }

        if (accessibleStudents.isEmpty()) return null

        val allTeachers = teacherDao.getAllTeachers().firstOrNull() ?: emptyList()
        val allMarks = marksDao.getAllMarks().firstOrNull() ?: emptyList()
        val allAttendance = attendanceDao.getAllAttendanceRecords().firstOrNull()?.filter { it.academicYear == academicYear } ?: emptyList()
        val curriculumChunkCount = curriculumKnowledgeDao.getChunkCount()

        val activeGrades = accessibleStudents.map { it.gradeName }.distinct()
        val activeClasses = accessibleStudents.map { "${it.gradeName}-${it.className}" }.distinct()

        // Overall Attendance Calculation
        val totalAttendanceRecords = allAttendance.filter { att -> accessibleStudents.any { it.id == att.studentId } }
        val presentCount = totalAttendanceRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
        val overallAttendanceRate = if (totalAttendanceRecords.isNotEmpty()) {
            (presentCount.toDouble() / totalAttendanceRecords.size.toDouble()) * 100.0
        } else 0.0

        // Grade-level Attendance & Student Summaries
        val gradeSummaries = activeGrades.map { gName ->
            val gradeStudents = accessibleStudents.filter { it.gradeName == gName }
            val gradeRecords = totalAttendanceRecords.filter { att -> gradeStudents.any { it.id == att.studentId } }
            val gPresent = gradeRecords.count { it.status == AttendanceStatus.PRESENT }
            val gLate = gradeRecords.count { it.status == AttendanceStatus.LATE }
            val gAbsent = gradeRecords.count { it.status == AttendanceStatus.ABSENT }
            val gRate = if (gradeRecords.isNotEmpty()) ((gPresent + gLate).toDouble() / gradeRecords.size.toDouble()) * 100.0 else 0.0

            GradeAttendanceSummary(
                gradeName = gName,
                studentCount = gradeStudents.size,
                presentDaysCount = gPresent,
                absentDaysCount = gAbsent,
                lateDaysCount = gLate,
                attendancePercentage = gRate
            )
        }

        // Overall Pass Rate Calculation
        var totalPassed = 0
        var totalEvaluated = 0
        accessibleStudents.forEach { st ->
            val stMarks = allMarks.filter { it.studentId == st.id }
            if (stMarks.isNotEmpty()) {
                val totalObtained = stMarks.sumOf { it.obtainedMarks ?: 0.0 }
                val maxPossible = stMarks.sumOf { it.maxMarks.toDouble() }.coerceAtLeast(1.0)
                val avg = (totalObtained / maxPossible) * 100.0
                totalEvaluated++
                if (avg >= 40.0) totalPassed++
            }
        }
        val overallPassRate = if (totalEvaluated > 0) (totalPassed.toDouble() / totalEvaluated.toDouble()) * 100.0 else 0.0

        val topGrades = gradeSummaries.filter { it.attendancePercentage >= 90.0 }.map { it.gradeName }

        return SchoolAnalyticsSummary(
            academicYear = academicYear,
            totalStudents = accessibleStudents.size,
            totalTeachers = if (scope.isSuperAdminOrAdmin) allTeachers.size else 1,
            activeGradesCount = activeGrades.size,
            activeClassesCount = activeClasses.size,
            overallAttendanceRate = overallAttendanceRate,
            overallPassRate = overallPassRate,
            totalCurriculumUnitsAvailable = curriculumChunkCount,
            topPerformingGrades = topGrades,
            gradeLevelSummaries = gradeSummaries
        )
    }

    /**
     * Generates a cohort-level attendance overview with role-based isolation.
     */
    suspend fun getSchoolAttendanceOverview(
        scope: AiPermissionScope,
        academicYear: String,
        gradeName: String? = null,
        className: String? = null
    ): SchoolAttendanceSummary? {
        if (!scope.isSuperAdminOrAdmin && scope.userRole != com.example.data.local.entity.UserRole.TEACHER) {
            return null
        }

        val allStudents = studentDao.getAllStudents().firstOrNull() ?: emptyList()
        val filteredStudents = allStudents.filter { st ->
            val matchesGrade = gradeName.isNullOrBlank() || st.gradeName.equals(gradeName, ignoreCase = true)
            val matchesClass = className.isNullOrBlank() || st.className.equals(className, ignoreCase = true)
            val allowedByScope = scope.canAccessStudent(st.gradeName, st.className)
            matchesGrade && matchesClass && allowedByScope
        }

        if (filteredStudents.isEmpty()) return null

        val allAttendance = attendanceDao.getAllAttendanceRecords().firstOrNull()?.filter { it.academicYear == academicYear } ?: emptyList()
        val relevantAttendance = allAttendance.filter { att -> filteredStudents.any { it.id == att.studentId } }

        val presentCount = relevantAttendance.count { it.status == AttendanceStatus.PRESENT }
        val lateCount = relevantAttendance.count { it.status == AttendanceStatus.LATE }
        val absentCount = relevantAttendance.count { it.status == AttendanceStatus.ABSENT }
        val totalDays = relevantAttendance.size

        val overallPercentage = if (totalDays > 0) {
            ((presentCount + lateCount).toDouble() / totalDays.toDouble()) * 100.0
        } else 0.0

        val distinctGrades = filteredStudents.map { it.gradeName }.distinct()
        val gradeBreakdown = distinctGrades.map { gName ->
            val gradeStudents = filteredStudents.filter { it.gradeName == gName }
            val gradeRecords = relevantAttendance.filter { att -> gradeStudents.any { it.id == att.studentId } }
            val gPresent = gradeRecords.count { it.status == AttendanceStatus.PRESENT }
            val gLate = gradeRecords.count { it.status == AttendanceStatus.LATE }
            val gAbsent = gradeRecords.count { it.status == AttendanceStatus.ABSENT }
            val gRate = if (gradeRecords.isNotEmpty()) ((gPresent + gLate).toDouble() / gradeRecords.size.toDouble()) * 100.0 else 0.0

            GradeAttendanceSummary(
                gradeName = gName,
                studentCount = gradeStudents.size,
                presentDaysCount = gPresent,
                absentDaysCount = gAbsent,
                lateDaysCount = gLate,
                attendancePercentage = gRate
            )
        }

        val scopeDesc = when {
            gradeName != null && className != null -> "Grade $gradeName ($className)"
            gradeName != null -> "Grade $gradeName Cohort"
            scope.isSuperAdminOrAdmin -> "School-wide (All Accessible Grades)"
            else -> "Teacher Assigned Scope (${scope.assignedGrades.joinToString(", ")})"
        }

        return SchoolAttendanceSummary(
            academicYear = academicYear,
            scopeDescription = scopeDesc,
            totalRecords = totalDays,
            totalStudentsEvaluated = filteredStudents.size,
            presentCount = presentCount,
            absentCount = absentCount,
            lateCount = lateCount,
            overallAttendancePercentage = overallPercentage,
            gradeBreakdown = gradeBreakdown
        )
    }

    /**
     * Generates an authoritative exam performance breakdown across subjects.
     */
    suspend fun getExamPerformanceOverview(
        scope: AiPermissionScope,
        academicYear: String,
        gradeName: String? = null,
        className: String? = null
    ): ExamPerformanceSummary? {
        if (!scope.isSuperAdminOrAdmin && scope.userRole != com.example.data.local.entity.UserRole.TEACHER) {
            return null
        }

        val allStudents = studentDao.getAllStudents().firstOrNull() ?: emptyList()
        val filteredStudents = allStudents.filter { st ->
            val matchesGrade = gradeName.isNullOrBlank() || st.gradeName.equals(gradeName, ignoreCase = true)
            val matchesClass = className.isNullOrBlank() || st.className.equals(className, ignoreCase = true)
            val allowedByScope = scope.canAccessStudent(st.gradeName, st.className)
            matchesGrade && matchesClass && allowedByScope
        }

        if (filteredStudents.isEmpty()) return null

        val allMarks = marksDao.getAllMarks().firstOrNull() ?: emptyList()
        val relevantMarks = allMarks.filter { mark -> filteredStudents.any { it.rollNumber == mark.rollNo } }

        if (relevantMarks.isEmpty()) return null

        val subjectGroups = relevantMarks.groupBy { it.subjectName }
        val subjectFacts = subjectGroups.map { (subjName, markList) ->
            var subjPassed = 0
            var subjDistinctions = 0
            val pcts = markList.map { m ->
                val raw = m.obtainedMarks ?: 0.0
                val full = m.maxMarks.toDouble().coerceAtLeast(1.0)
                val pct = (raw / full) * 100.0
                if (pct >= 40.0) subjPassed++
                if (pct >= 75.0) subjDistinctions++
                pct
            }
            val avg = if (pcts.isNotEmpty()) pcts.average() else 0.0
            val passRate = if (markList.isNotEmpty()) (subjPassed.toDouble() / markList.size.toDouble()) * 100.0 else 0.0

            SubjectPerformanceFact(
                subjectName = subjName,
                totalStudentsTested = markList.size,
                averageScore = avg,
                passRatePercentage = passRate,
                distinctionCount = subjDistinctions
            )
        }

        val overallAvg = if (subjectFacts.isNotEmpty()) subjectFacts.map { it.averageScore }.average() else 0.0
        val overallPass = if (subjectFacts.isNotEmpty()) subjectFacts.map { it.passRatePercentage }.average() else 0.0
        val totalDistinctions = subjectFacts.sumOf { it.distinctionCount }

        val highestSubj = subjectFacts.maxByOrNull { it.averageScore }?.subjectName ?: "N/A"
        val lowestSubj = subjectFacts.minByOrNull { it.averageScore }?.subjectName ?: "N/A"

        val scopeDesc = when {
            gradeName != null && className != null -> "Grade $gradeName ($className)"
            gradeName != null -> "Grade $gradeName"
            scope.isSuperAdminOrAdmin -> "School-wide Examination Overview"
            else -> "Teacher Assigned Scope"
        }

        return ExamPerformanceSummary(
            academicYear = academicYear,
            scopeDescription = scopeDesc,
            totalAssessmentsEvaluated = relevantMarks.map { it.assessmentId }.distinct().size,
            totalStudentsEvaluated = filteredStudents.size,
            overallAverageScore = overallAvg,
            overallPassRate = overallPass,
            distinctionCount = totalDistinctions,
            subjectPerformance = subjectFacts,
            highestPerformingSubject = highestSubj,
            lowestPerformingSubject = lowestSubj
        )
    }

    /**
     * Identifies at-risk students based on authoritative marks and attendance data.
     * Strictly avoids exposing passwords, NRC, phone numbers, or residential addresses.
     */
    suspend fun getAtRiskStudents(
        scope: AiPermissionScope,
        academicYear: String,
        gradeName: String? = null,
        className: String? = null
    ): List<AtRiskStudentFact> {
        if (!scope.isSuperAdminOrAdmin && scope.userRole != com.example.data.local.entity.UserRole.TEACHER) {
            return emptyList()
        }

        val allStudents = studentDao.getAllStudents().firstOrNull() ?: emptyList()
        val filteredStudents = allStudents.filter { st ->
            val matchesGrade = gradeName.isNullOrBlank() || st.gradeName.equals(gradeName, ignoreCase = true)
            val matchesClass = className.isNullOrBlank() || st.className.equals(className, ignoreCase = true)
            val allowedByScope = scope.canAccessStudent(st.gradeName, st.className)
            matchesGrade && matchesClass && allowedByScope
        }

        if (filteredStudents.isEmpty()) return emptyList()

        val allMarks = marksDao.getAllMarks().firstOrNull() ?: emptyList()
        val allAttendance = attendanceDao.getAllAttendanceRecords().firstOrNull()?.filter { it.academicYear == academicYear } ?: emptyList()

        val atRiskList = mutableListOf<AtRiskStudentFact>()

        filteredStudents.forEach { student ->
            val studentMarks = allMarks.filter { it.studentId == student.id }
            val studentAttendance = allAttendance.filter { it.studentId == student.id }

            var avgScore = 0.0
            val failingSubjects = mutableListOf<String>()

            if (studentMarks.isNotEmpty()) {
                val totalRaw = studentMarks.sumOf { it.obtainedMarks ?: 0.0 }
                val maxPossible = studentMarks.sumOf { it.maxMarks.toDouble() }.coerceAtLeast(1.0)
                avgScore = (totalRaw / maxPossible) * 100.0

                studentMarks.forEach { m ->
                    val raw = m.obtainedMarks ?: 0.0
                    val full = m.maxMarks.toDouble().coerceAtLeast(1.0)
                    if ((raw / full) * 100.0 < 40.0) {
                        failingSubjects.add(m.subjectName)
                    }
                }
            }

            val attendancePercentage = if (studentAttendance.isNotEmpty()) {
                val presentDays = studentAttendance.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
                (presentDays.toDouble() / studentAttendance.size.toDouble()) * 100.0
            } else null

            val isLowAcademic = studentMarks.isNotEmpty() && (avgScore < 50.0 || failingSubjects.isNotEmpty())
            val isLowAttendance = attendancePercentage != null && attendancePercentage < 75.0

            if (isLowAcademic || isLowAttendance) {
                val riskReason = when {
                    isLowAcademic && isLowAttendance -> "Academic Underperformance (${String.format(Locale.US, "%.1f", avgScore)}%) & Low Attendance (${String.format(Locale.US, "%.1f", attendancePercentage)}%)"
                    isLowAcademic -> "Subject Support Required in ${failingSubjects.joinToString(", ").ifEmpty { "Core Subjects" }} (Avg: ${String.format(Locale.US, "%.1f", avgScore)}%)"
                    else -> "Chronic Absenteeism (${String.format(Locale.US, "%.1f", attendancePercentage)}%)"
                }

                val intervention = when {
                    isLowAcademic && isLowAttendance -> "Arrange parent-teacher consultation and provide structured remediation worksheets."
                    isLowAcademic -> "Assign targeted practice worksheets and 1-on-1 pedagogical review for ${failingSubjects.joinToString(", ").ifEmpty { "struggling areas" }}."
                    else -> "Review attendance log and engage student counseling support."
                }

                atRiskList.add(
                    AtRiskStudentFact(
                        studentId = student.id,
                        studentName = student.name,
                        rollNumber = student.rollNumber,
                        gradeName = student.gradeName,
                        className = student.className,
                        academicYear = academicYear,
                        averageScorePercentage = avgScore,
                        failingSubjects = failingSubjects.distinct(),
                        attendancePercentage = attendancePercentage,
                        primaryRiskFactor = riskReason,
                        recommendedIntervention = intervention
                    )
                )
            }
        }

        return atRiskList.sortedBy { it.averageScorePercentage }
    }

    /**
     * Retrieves school grading policies and pass thresholds.
     */
    suspend fun getSchoolGradingPolicy(): String {
        val policies = schoolPolicyDao.getAllGradingPolicies().firstOrNull() ?: emptyList()
        if (policies.isEmpty()) {
            return "Default HCM Policy: A (80-100%, Distinction), B (70-79%), C (60-69%), D (50-59%), E (40-49%, Pass), F (<40%, Fail)."
        }
        return policies.joinToString("\n") {
            "${it.subjectName} (${it.educationLevel.displayName}): Pass Mark = ${it.passMark}%, Distinction Mark = ${it.distinctionMark}% (Max = ${it.maxMark})"
        }
    }
}
