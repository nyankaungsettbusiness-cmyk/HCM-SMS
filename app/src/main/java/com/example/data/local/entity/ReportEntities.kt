package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReportTemplateType(val displayName: String, val gradeRange: String) {
    KINDERGARTEN("Kindergarten Template", "KG"),
    PRIMARY("Primary Template", "G1 – G4"),
    SECONDARY("Secondary Template", "G5 – G9"),
    HIGH_SCHOOL("High School Template", "G10 – G12")
}

enum class ReportType(val displayName: String) {
    ASSESSMENT_PERIOD("Assessment Period Report"),
    ACADEMIC_YEAR_SUMMARY("Academic Year Summary Report")
}

@Entity(tableName = "report_templates")
data class ReportTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g. "PRIMARY_TEMPLATE", "SECONDARY_TEMPLATE", "HIGH_SCHOOL_TEMPLATE"
    val templateName: String,
    val targetLevel: String, // "PRIMARY", "SECONDARY", "HIGH_SCHOOL"
    val description: String = "",
    val isSystemDefault: Boolean = true,
    val isActive: Boolean = true,
    val configurationJson: String = "{}"
)

@Entity(tableName = "report_settings")
data class ReportSettingEntity(
    @PrimaryKey val id: Long = 1,
    val academicYear: String = "2026-2027",
    val showStudentPhoto: Boolean = true,
    val showSchoolLogo: Boolean = true,
    val showAttendance: Boolean = true,
    val showSgi: Boolean = true,
    val showHcm: Boolean = true,
    val showExtracurricular: Boolean = true,
    val showTeacherComment: Boolean = true,
    val showRecommendation: Boolean = true,
    val showParentSignature: Boolean = true,
    val showPrincipalSignature: Boolean = true,
    val showClassTeacherSignature: Boolean = true,
    val showSchoolSeal: Boolean = true,
    val pdfNamingPattern: String = "{Template}_{Grade}_{StudentName}_{Period}_{Year}.pdf",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "report_generation_history")
data class ReportGenerationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val studentCode: String = "",
    val gradeName: String,
    val className: String,
    val templateType: String, // "PRIMARY", "SECONDARY", "HIGH_SCHOOL"
    val reportType: String, // "ASSESSMENT_PERIOD", "ACADEMIC_YEAR_SUMMARY"
    val assessmentPeriodName: String = "",
    val academicYear: String = "2026-2027",
    val generatedBy: String = "Teacher",
    val generatedAtTimestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS"
)

@Entity(tableName = "pdf_export_history")
data class PdfExportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportHistoryId: Long = 0,
    val studentId: Long,
    val studentName: String = "",
    val gradeName: String = "",
    val className: String = "",
    val academicYear: String = "2026-2027",
    val reportType: String = "",
    val assessmentPeriodName: String = "",
    val fileName: String,
    val fileSizeBytes: Long = 0,
    val exportedBy: String = "User",
    val exportedAtTimestamp: Long = System.currentTimeMillis(),
    val exportFormat: String = "PDF"
)

@Entity(tableName = "print_history")
data class PrintHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportHistoryId: Long = 0,
    val studentId: Long,
    val studentName: String = "",
    val gradeName: String = "",
    val className: String = "",
    val academicYear: String = "2026-2027",
    val reportType: String = "",
    val assessmentPeriodName: String = "",
    val templateUsed: String = "",
    val printedBy: String = "User",
    val printedAtTimestamp: Long = System.currentTimeMillis(),
    val numberOfCopies: Int = 1
)
