package com.example.data.ai

/**
 * Structured facts model produced by [StudentFactsAggregator].
 * Fully verified by HCM-SMS authoritative engines before passing to the AI layer.
 * Zero PII exposure.
 */
data class SubjectFact(
    val subjectName: String,
    val totalObtained: Double,
    val maxPossible: Double,
    val averagePercentage: Double,
    val gradeLetter: String,
    val isPass: Boolean,
    val distinctionBadge: String
)

data class AssessmentScoreFact(
    val assessmentName: String,
    val assessmentType: String,
    val obtainedMark: Double,
    val maxMark: Double,
    val percentage: Double,
    val rankInClass: String = ""
)

data class AssessmentTrendFact(
    val subjectName: String,
    val progressionDescription: String,
    val previousScore: Double? = null,
    val currentScore: Double? = null,
    val percentageChange: Double? = null
)

data class HolisticPillarFact(
    val pillarName: String, // "Honesty", "Curiosity", "Mindfulness", etc.
    val myanmarPillarName: String,
    val starRating: Double,
    val maxStars: Int = 5,
    val existingObservationRemark: String = ""
)

data class HolisticAssessmentSummaryFact(
    val pillars: List<HolisticPillarFact>,
    val honestyAvg: Double,
    val curiosityAvg: Double,
    val mindfulnessAvg: Double,
    val overallAverage: Double,
    val overallLevel: String,
    val topStrengths: List<String>,
    val growthAreas: List<String>
)

data class AttendanceSummaryFact(
    val periodName: String,
    val presentPercentage: Double,
    val absentPercentage: Double,
    val latePercentage: Double,
    val totalDaysRecorded: Int,
    val isAttendanceDataAvailable: Boolean,
    val attendanceStatusDescription: String
)

data class OverallPerformanceFact(
    val totalObtained: Double,
    val maxPossible: Double,
    val overallAveragePercentage: Double,
    val overallGrade: String,
    val classRank: Int? = null,
    val totalStudentsInClass: Int? = null,
    val passStatus: String
)

data class StudentReportFacts(
    val studentId: Long,
    val studentName: String,
    val academicYear: String,
    val grade: String,
    val className: String,
    val periodName: String,

    val subjectResults: List<SubjectFact>,
    val strongestSubjects: List<String>,
    val improvementSubjects: List<String>,

    val assessmentResults: List<AssessmentScoreFact>,
    val assessmentTrends: List<AssessmentTrendFact>,

    val overallPerformance: OverallPerformanceFact,
    val attendanceSummary: AttendanceSummaryFact,
    val holisticAssessmentSummary: HolisticAssessmentSummaryFact,
    val teacherObservationNotes: String = ""
)
