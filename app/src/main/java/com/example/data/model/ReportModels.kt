package com.example.data.model

import com.example.data.local.entity.HolisticResultEntity
import com.example.data.local.entity.ReportSettingEntity
import com.example.data.local.entity.ReportTemplateType
import com.example.data.local.entity.ReportType
import com.example.data.local.entity.SchoolSettingEntity
import com.example.data.local.entity.SgiResultEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeacherCommentEntity

data class SubjectReportData(
    val subjectId: Long = 0,
    val subjectName: String,
    val category: String, // "ACADEMIC", "ADDITIONAL"
    val markObtained: Double? = null,
    val maxMark: Double? = 100.0,
    val gradeLetter: String? = null,
    val isPassed: Boolean = true,
    val remarks: String = ""
)

data class AttendanceRecord(
    val totalDays: Int = 120,
    val presentDays: Int = 115,
    val absentDays: Int = 5,
    val attendancePercentage: Double = 95.8
)

// Primary Report Card (KG-G4) Specific Detailed Models
data class PrimarySubjectPerformance(
    val subjectName: String,
    val ratingStars: Int, // 1..5
    val teacherComment: String
)

data class MonthlyTestRow(
    val periodName: String,
    val subjectMarks: Map<String, Double?>,
    val totalMarks: Double,
    val grade: String,
    val rank: Int,
    val totalStudentsInClass: Int = 30,
    val distinction: String = "",
    val isCompleted: Boolean = true
)

data class CetTestRow(
    val examName: String,
    val subjectMarks: Map<String, Double?>,
    val totalMarks: Double,
    val grade: String,
    val rank: Int,
    val totalStudentsInClass: Int = 30,
    val distinction: String = "",
    val isCompleted: Boolean = true
)

data class PilotTestRow(
    val examName: String,
    val subjectMarks: Map<String, Double?>,
    val totalMarks: Double,
    val grade: String,
    val rank: Int,
    val totalStudentsInClass: Int = 30,
    val distinction: String = "",
    val isCompleted: Boolean = true
)

data class CustomExamRow(
    val examName: String,
    val subjectName: String = "English (International)",
    val markObtained: Double?,
    val rank: Int = 2,
    val totalStudentsInClass: Int = 30,
    val isCompleted: Boolean = true
)

data class WeeklyTestRow(
    val testName: String,
    val subjectMarks: Map<String, Double?>,
    val totalMarks: Double,
    val grade: String,
    val rank: Int,
    val totalStudentsInClass: Int = 30,
    val distinction: String = "",
    val isCompleted: Boolean = true
)

data class LessonCompletionTestRow(
    val testName: String,
    val subjectMarks: Map<String, Double?>,
    val totalMarks: Double,
    val grade: String,
    val rank: Int,
    val totalStudentsInClass: Int = 30,
    val distinction: String = "",
    val isCompleted: Boolean = true
)

data class CetOverallSummary(
    val subjectAverages: Map<String, Double>,
    val overallTotal: Double,
    val overallGrade: String,
    val overallRank: Int
)

data class HcmItemRating(
    val itemName: String,
    val ratingStars: Int,
    val averageStars: Int = 4,
    val maxStars: Int = 5
)

data class HcmCategoryGroup(
    val pillarName: String, // Honesty, Curiosity, Mindfulness
    val myanmarPillarName: String,
    val items: List<HcmItemRating>,
    val averageRating: Double
)

data class HcmHolisticReportData(
    val groups: List<HcmCategoryGroup>,
    val overallScore: Double,
    val overallLevel: String,
    val honestyAverage: Double = 0.0,
    val curiosityAverage: Double = 0.0,
    val mindfulnessAverage: Double = 0.0
)

data class SgiDisplayItem(
    val categoryName: String,
    val ratingStars: Int,
    val maxStars: Int = 5,
    val levelText: String
)

data class SgiOverallSummary(
    val items: List<SgiDisplayItem> = emptyList(),
    val overallScore: Double = 0.0,
    val overallLevel: String = "",
    val description: String = "",
    val calculatedSgiPercentage: Double = 95.0
)

data class PrimaryAttendanceSummary(
    val selectedPeriodName: String,
    val selectedPeriodPresentDays: Int,
    val selectedPeriodTotalDays: Int,
    val selectedPeriodPercentage: Double,
    val averageAttendancePercentage: Double,
    val cumulativeMonthsLabel: String,
    val isAnnual: Boolean = false,
    val overallTotalDays: Int = 180,
    val overallPresentDays: Int = 175,
    val overallPercentage: Double = 97.2
)

data class ExtracurricularAchievementRecord(
    val periodName: String,
    val activityName: String,
    val competition: String,
    val level: String,
    val award: String,
    val certNumber: String = "",
    val description: String = ""
)

data class PrimaryReportCardDetails(
    val studentNameMm: String,
    val gradeAndClassMm: String,
    val studentCodeMm: String,
    val parentNameMm: String,
    val sgiSummary: SgiOverallSummary,
    val attendanceSummary: PrimaryAttendanceSummary,
    val subjectPerformances: List<PrimarySubjectPerformance>,
    val monthlyTestRows: List<MonthlyTestRow>,
    val unitTestRows: List<MonthlyTestRow> = emptyList(),
    val cetTestRows: List<CetTestRow>,
    val customExamRows: List<CustomExamRow> = emptyList(),
    val cetOverallSummary: CetOverallSummary,
    val hcmSummary: HcmHolisticReportData,
    val strengthsMm: String,
    val teacherRemarkMm: String,
    val parentRecommendationMm: String,
    val achievements: List<ExtracurricularAchievementRecord>,
    val rankingSubjectsList: List<String>
)

data class SecondaryReportCardDetails(
    val studentNameMm: String,
    val gradeAndClassMm: String,
    val studentCodeMm: String,
    val parentNameMm: String,
    val sgiSummary: SgiOverallSummary,
    val attendanceSummary: PrimaryAttendanceSummary,
    val subjectPerformances: List<PrimarySubjectPerformance>,
    val monthlyTestRows: List<MonthlyTestRow>,
    val pilotTestRows: List<PilotTestRow>,
    val cetTestRows: List<CetTestRow>,
    val customExamRows: List<CustomExamRow> = emptyList(),
    val cetOverallSummary: CetOverallSummary,
    val hcmSummary: HcmHolisticReportData,
    val strengthsMm: String,
    val teacherRemarkMm: String,
    val parentRecommendationMm: String,
    val achievements: List<ExtracurricularAchievementRecord>,
    val rankingSubjectsList: List<String>
)

data class HighSchoolReportCardDetails(
    val studentNameMm: String,
    val gradeAndClassMm: String,
    val studentCodeMm: String,
    val parentNameMm: String,
    val sgiSummary: SgiOverallSummary,
    val attendanceSummary: PrimaryAttendanceSummary,
    val subjectPerformances: List<PrimarySubjectPerformance>,
    val weeklyTestRows: List<WeeklyTestRow>,
    val lessonTestRows: List<LessonCompletionTestRow>,
    val pilotTestRows: List<PilotTestRow>,
    val cetTestRows: List<CetTestRow>,
    val cetOverallSummary: CetOverallSummary,
    val hcmSummary: HcmHolisticReportData,
    val strengthsMm: String,
    val teacherRemarkMm: String,
    val parentRecommendationMm: String,
    val achievements: List<ExtracurricularAchievementRecord>,
    val rankingSubjectsList: List<String>
)

data class ReportDataPackage(
    val student: StudentEntity,
    val schoolSetting: SchoolSettingEntity,
    val gradeName: String,
    val className: String,
    val templateType: ReportTemplateType,
    val reportType: ReportType,
    val assessmentPeriodName: String, // Empty if ACADEMIC_YEAR_SUMMARY
    val academicYear: String,
    val subjects: List<SubjectReportData>,
    val attendance: AttendanceRecord,
    val hcmResults: List<HolisticResultEntity>,
    val sgiResults: List<SgiResultEntity>,
    val teacherComment: TeacherCommentEntity?,
    val extracurriculars: List<String>,
    val reportSettings: ReportSettingEntity,
    val primaryDetails: PrimaryReportCardDetails? = null,
    val secondaryDetails: SecondaryReportCardDetails? = null,
    val highSchoolDetails: HighSchoolReportCardDetails? = null,
    val generatedAtTimestamp: Long = System.currentTimeMillis()
)

data class ProcessedReportContent(
    val templateType: ReportTemplateType,
    val reportTitle: String,
    val studentName: String,
    val studentCode: String,
    val gradeAndClass: String,
    val periodOrYearLabel: String,
    val summaryHighlights: List<String>,
    val totalSubjectsCount: Int,
    val averageScore: Double,
    val overallGrade: String,
    val activeSections: List<String>,
    val rawDataPackage: ReportDataPackage
)

