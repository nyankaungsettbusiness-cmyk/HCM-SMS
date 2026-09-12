package com.example.data.ai

import com.example.data.local.entity.UserRole

/**
 * Data contracts and factual representations for the HCM-SMS AI Subsystem.
 * All mathematical calculations, marks, and rankings are verified before being
 * packaged into these contracts. Gemini ONLY interprets these verified facts.
 */

// --- Permission and Security Scope ---

data class AiPermissionScope(
    val username: String,
    val userRole: UserRole,
    val linkedTeacherId: Long? = null,
    val teacherName: String = "",
    val teacherQualifications: String = "",
    val assignedGrades: List<String> = emptyList(),
    val assignedClasses: List<String> = emptyList(),
    val assignedSubjects: List<String> = emptyList(),
    val preferredLanguage: String = "English & Myanmar"
) {
    val isSuperAdminOrAdmin: Boolean
        get() = userRole == UserRole.SUPER_ADMIN || userRole == UserRole.ADMIN

    fun canAccessGrade(grade: String): Boolean {
        if (isSuperAdminOrAdmin) return true
        if (userRole == UserRole.TEACHER) {
            if (assignedGrades.isEmpty()) return false
            return assignedGrades.any {
                it.equals(grade, ignoreCase = true) || it.contains(grade, ignoreCase = true) || grade.contains(it, ignoreCase = true)
            }
        }
        return false
    }

    fun canAccessClass(className: String): Boolean {
        if (isSuperAdminOrAdmin) return true
        if (userRole == UserRole.TEACHER) {
            if (assignedClasses.isEmpty()) return false
            return assignedClasses.any {
                it.equals(className, ignoreCase = true) || it.contains(className, ignoreCase = true) || className.contains(it, ignoreCase = true)
            }
        }
        return false
    }

    fun canGenerateQuestions(grade: String, subject: String): Boolean {
        if (isSuperAdminOrAdmin) return true
        if (userRole == UserRole.TEACHER) {
            return canAccessGrade(grade) && canAccessSubject(subject)
        }
        return false
    }

    fun canAccessStudent(studentGrade: String, studentClass: String): Boolean {
        if (isSuperAdminOrAdmin) return true
        if (userRole == UserRole.TEACHER) {
            // Strict Deny-By-Default: Unconfigured/empty assignments must NOT grant access
            if (assignedGrades.isEmpty() || assignedClasses.isEmpty()) return false
            val gradeMatches = assignedGrades.any { 
                it.equals(studentGrade, ignoreCase = true) || it.contains(studentGrade, ignoreCase = true) || studentGrade.contains(it, ignoreCase = true) 
            }
            val classMatches = assignedClasses.any { 
                it.equals(studentClass, ignoreCase = true) || it.contains(studentClass, ignoreCase = true) || studentClass.contains(it, ignoreCase = true)
            }
            return gradeMatches && classMatches
        }
        return false
    }

    fun canAccessSubject(subject: String): Boolean {
        if (isSuperAdminOrAdmin) return true
        if (userRole == UserRole.TEACHER) {
            // Strict Deny-By-Default: Unconfigured/empty assignments must NOT grant access
            if (assignedSubjects.isEmpty()) return false
            return assignedSubjects.any {
                it.equals(subject, ignoreCase = true) || it.contains(subject, ignoreCase = true) || subject.contains(it, ignoreCase = true)
            }
        }
        return false
    }
}

// --- Verified Student Academic Facts (Calculated by existing engines) ---

data class SubjectScoreFact(
    val subjectName: String,
    val rawScore: Double,
    val fullMarks: Double = 100.0,
    val percentage: Double = if (fullMarks > 0) (rawScore / fullMarks) * 100.0 else rawScore,
    val letterGrade: String = "",
    val passStatus: String = if (percentage >= 40.0) "PASSED" else "FAILED",
    val isDistinction: Boolean = percentage >= 80.0
)

data class VerifiedStudentAcademicFacts(
    val studentId: Long,
    val studentName: String,
    val rollNumber: Int,
    val studentCode: String,
    val grade: String,
    val className: String,
    val academicYear: String,
    val assessmentName: String,
    val subjectScores: List<SubjectScoreFact>,
    val totalScore: Double,
    val maxPossibleScore: Double,
    val overallPercentage: Double,
    val overallGrade: String,
    val rankInClass: Int? = null,
    val totalStudentsInClass: Int? = null,
    val strongestSubjects: List<String>,
    val subjectsNeedingSupport: List<String>,
    val scoreTrendDescription: String = "", // e.g. "+6% improvement compared to Term 1"
    val attendanceRatePercentage: Double? = null,
    val isAtRisk: Boolean = overallPercentage < 40.0 || (attendanceRatePercentage != null && attendanceRatePercentage < 75.0)
)

// --- Verified Student Holistic Facts (HCM 6 Pillars & SGI) ---

data class PillarRatingFact(
    val pillarName: String, // "Honesty", "Curiosity", "Mindfulness"
    val ratingStars: Double, // 1.0 - 5.0
    val maxStars: Int = 5,
    val categoryCount: Int = 1
)

data class VerifiedStudentHolisticFacts(
    val studentId: Long,
    val assessmentPeriod: String,
    val academicYear: String,
    val pillarRatings: List<PillarRatingFact>,
    val averageStars: Double,
    val topStrengths: List<String>,
    val growthOpportunities: List<String>,
    val existingTeacherObservation: String = ""
)

// --- Combined Verified Profile for Report Card Comment Drafting ---

data class VerifiedCombinedStudentProfile(
    val studentId: Long,
    val studentName: String,
    val studentCode: String,
    val rollNumber: Int,
    val grade: String,
    val className: String,
    val academicYear: String,
    val assessmentPeriod: String,
    val academicFacts: VerifiedStudentAcademicFacts?,
    val holisticFacts: VerifiedStudentHolisticFacts?,
    val attendancePercentage: Double?,
    val verifiedSummaryText: String
)

// --- Report Card AI Comment Result ---

data class AiGeneratedReportCardComment(
    val studentId: Long,
    val studentName: String,
    val teacherCommentMyanmar: String,
    val parentSupportSuggestionMyanmar: String,
    val teacherCommentEnglish: String = "",
    val parentSupportSuggestionEnglish: String = "",
    val verifiedFactsUsed: String,
    val confidenceNote: String = "Grounded 100% in authentic HCM-SMS assessment and holistic records."
)

// --- Curriculum Scope & Citation ---

data class CurriculumSourceCitation(
    val documentTitle: String,
    val gradeLevel: String,
    val subject: String,
    val chapterUnit: String,
    val sectionTopic: String,
    val pageRange: String,
    val documentType: String,
    val contentSnippet: String,
    val sourceReference: String
)



// --- Class / Cohort Analytics Summary for Admin ---

data class ClassPerformanceSummary(
    val gradeName: String,
    val className: String,
    val academicYear: String,
    val assessmentName: String,
    val totalStudents: Int,
    val passedCount: Int,
    val failedCount: Int,
    val passRatePercentage: Double,
    val classAverageScore: Double,
    val distinctionCount: Int,
    val topPerformingSubjects: List<String>,
    val strugglingSubjects: List<String>,
    val atRiskStudentCount: Int,
    val atRiskStudentRolls: List<Int>
)

data class SchoolAnalyticsSummary(
    val academicYear: String,
    val totalStudents: Int,
    val totalTeachers: Int,
    val activeGradesCount: Int,
    val activeClassesCount: Int,
    val overallAttendanceRate: Double,
    val overallPassRate: Double,
    val totalCurriculumUnitsAvailable: Int,
    val topPerformingGrades: List<String>,
    val gradeLevelSummaries: List<GradeAttendanceSummary>,
    val verifiedTimestamp: Long = System.currentTimeMillis()
)

data class GradeAttendanceSummary(
    val gradeName: String,
    val studentCount: Int,
    val presentDaysCount: Int,
    val absentDaysCount: Int,
    val lateDaysCount: Int,
    val attendancePercentage: Double
)

data class SchoolAttendanceSummary(
    val academicYear: String,
    val scopeDescription: String,
    val totalRecords: Int,
    val totalStudentsEvaluated: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val overallAttendancePercentage: Double,
    val gradeBreakdown: List<GradeAttendanceSummary>
)

data class SubjectPerformanceFact(
    val subjectName: String,
    val totalStudentsTested: Int,
    val averageScore: Double,
    val passRatePercentage: Double,
    val distinctionCount: Int
)

data class ExamPerformanceSummary(
    val academicYear: String,
    val scopeDescription: String,
    val totalAssessmentsEvaluated: Int,
    val totalStudentsEvaluated: Int,
    val overallAverageScore: Double,
    val overallPassRate: Double,
    val distinctionCount: Int,
    val subjectPerformance: List<SubjectPerformanceFact>,
    val highestPerformingSubject: String,
    val lowestPerformingSubject: String
)

data class AtRiskStudentFact(
    val studentId: Long,
    val studentName: String,
    val rollNumber: Int,
    val gradeName: String,
    val className: String,
    val academicYear: String,
    val averageScorePercentage: Double,
    val failingSubjects: List<String>,
    val attendancePercentage: Double?,
    val primaryRiskFactor: String, // e.g. "Low Average Score (34.5%)", "Chronic Absenteeism (68.0%)", "Failing 2 Subjects"
    val recommendedIntervention: String
)
