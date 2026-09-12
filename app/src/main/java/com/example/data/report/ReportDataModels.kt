package com.example.data.report

import com.example.data.local.entity.ReportTemplateType
import com.example.data.local.entity.StudentEntity

data class ReportRuleEvaluationResult(
    val selectedTemplate: ReportTemplateType,
    val ruleMatched: String,
    val gradeName: String,
    val educationLevel: String,
    val appliedCriteria: List<String>,
    val templateDescription: String
)

data class DynamicSubjectScore(
    val subjectName: String,
    val category: String, // "ACADEMIC", "ADDITIONAL"
    val examScores: Map<String, Double?>, // Exam Name -> Obtained Mark
    val totalObtained: Double,
    val maxPossible: Double,
    val averagePercentage: Double,
    val gradeLetter: String,
    val isPass: Boolean,
    val distinctionBadge: String
)

data class DynamicExamCategoryTable(
    val categoryKey: String,
    val categoryTitle: String,
    val discoveredExams: List<String>,
    val subjectRows: List<DynamicSubjectScore>,
    val totalMarksAcrossExams: Double,
    val maxPossibleAcrossExams: Double,
    val overallCategoryAverage: Double,
    val overallCategoryGrade: String,
    val classRank: Int? = null,
    val totalStudentsInClass: Int? = null
)

data class ReportAttendanceSummary(
    val selectedMonth: String,
    val monthTotalDays: Int,
    val monthPresentDays: Int,
    val monthAbsentDays: Int,
    val monthLeaveDays: Int,
    val monthAttendancePercentage: Double,
    val cumulativeTotalDays: Int,
    val cumulativePresentDays: Int,
    val cumulativeAbsentDays: Int,
    val cumulativePercentage: Double,
    val statusBadge: String
)

data class ReportHcmPillarItem(
    val categoryName: String,
    val pillar: String, // HONESTY, CURIOSITY, MINDFULNESS
    val starRating: Int,
    val maxStars: Int = 5
)

data class ReportHcmPillarSummary(
    val pillarName: String,
    val myanmarPillarName: String,
    val items: List<ReportHcmPillarItem>,
    val averageStars: Double
)

data class ReportHcmSummary(
    val pillars: List<ReportHcmPillarSummary>,
    val honestyAvg: Double,
    val curiosityAvg: Double,
    val mindfulnessAvg: Double,
    val overallScore: Double,
    val overallLevel: String,
    val sgiGrowthPercentage: Double? = null
)

data class ReportTeacherComments(
    val positiveComments: String,
    val areasForImprovement: String,
    val generalComment: String,
    val futureRecommendation: String,
    val updatedBy: String = "Class Teacher",
    val updatedAt: Long = 0L
)

data class ReportSgiSummary(
    val academicComponentPct: Double,
    val attendanceComponentPct: Double,
    val hcmComponentPct: Double,
    val overallSgiPercentage: Double,
    val sgiGrade: String,
    val isAutoCalculated: Boolean = true
)

data class PrimaryExamRow(
    val examName: String,
    val examType: String,
    val totalObtainedCore: Double,
    val maxPossibleCore: Double,
    val classRank: String,
    val distinctionBadge: String,
    val coreSubjectScores: Map<String, Double?> = emptyMap()
)

data class PrimaryCetBlock(
    val cetTitle: String,
    val subjectRows: List<DynamicSubjectScore>,
    val totalObtainedCore: Double,
    val maxPossibleCore: Double,
    val classRank: String,
    val distinctionBadge: String
)

data class PrimaryCustomExamRow(
    val examName: String,
    val subjectName: String,
    val obtainedMark: Double,
    val maxMark: Double,
    val classRank: String
)

data class PrimaryReportSummary(
    val monthlyTests: List<PrimaryExamRow>,
    val unitTests: List<PrimaryExamRow>,
    val cetBlocks: List<PrimaryCetBlock>,
    val customExams: List<PrimaryCustomExamRow>,
    val isG4: Boolean
)

data class GeneratedReportCardData(
    val student: StudentEntity,
    val gradeName: String,
    val className: String,
    val selectedMonth: String,
    val academicYear: String,
    val ruleResult: ReportRuleEvaluationResult,
    val dynamicExamTables: List<DynamicExamCategoryTable>,
    val attendanceSummary: ReportAttendanceSummary,
    val hcmSummary: ReportHcmSummary,
    val teacherComments: ReportTeacherComments,
    val kgSgiSummary: ReportSgiSummary? = null,
    val primaryReportSummary: PrimaryReportSummary? = null,
    val generatedAt: Long = 0L
)
