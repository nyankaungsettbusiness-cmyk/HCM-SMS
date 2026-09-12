package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    TEACHER("Teacher"),
    OFFICE_STAFF("Office Staff")
}

enum class EducationLevel(val displayName: String) {
    KINDERGARTEN("Kindergarten Level (KG)"),
    PRIMARY("Primary Level (G1 - G5)"),
    SECONDARY("Secondary Level (G6 - G9)"),
    HIGH_SCHOOL("High School Level (G10 - G12)")
}

enum class SubjectCategory {
    ACADEMIC,
    ADDITIONAL
}

enum class PermissionAction {
    VIEW,
    CREATE,
    EDIT,
    DELETE,
    PRINT,
    EXPORT_PDF,
    EXPORT,
    BACKUP,
    RESTORE
}

enum class UserStatus(val displayName: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    LOCKED("Locked"),
    SUSPENDED("Suspended")
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val email: String,
    val phone: String = "",
    val isActive: Boolean = true,
    val status: UserStatus = UserStatus.ACTIVE,
    val linkedTeacherName: String = "",
    val linkedStaffName: String = "",
    val passwordHash: String = "cfa1cd8d3caf45902bfc19e5bdc131abca5208f2f4698bc327c5115212d7142e",
    val salt: String = "HCM_SALT_2026",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginTimestamp: Long? = System.currentTimeMillis() - 86400000L,
    val lastLogoutTimestamp: Long? = null,
    val mustChangePassword: Boolean = false,
    val failedLoginAttempts: Int = 0,
    val isDeleted: Boolean = false,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val managedPassword: String = ""
)

@Entity(tableName = "login_history")
data class LoginHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val username: String,
    val displayName: String,
    val loginTime: Long = System.currentTimeMillis(),
    val logoutTime: Long? = null,
    val device: String = "Android Terminal",
    val ipAddress: String = "192.168.1.10",
    val status: String = "SUCCESS", // SUCCESS, FAILED_INVALID_PASSWORD, FAILED_LOCKED, LOGOUT
    val failureReason: String = ""
)

@Entity(tableName = "security_policy")
data class SecurityPolicyEntity(
    @PrimaryKey val id: Int = 1,
    val maxFailedAttempts: Int = 5,
    val forcePasswordChangeOnFirstLogin: Boolean = true,
    val sessionTimeoutMinutes: Int = 30,
    val minPasswordLength: Int = 6,
    val requireSpecialChar: Boolean = false
)

@Entity(tableName = "role_permissions", primaryKeys = ["role", "permissionKey"])
data class RolePermissionEntity(
    val role: UserRole,
    val permissionKey: String, // e.g. "STUDENTS_VIEW", "STUDENTS_CREATE", "POLICY_EDIT"
    val isAllowed: Boolean
)

@Entity(tableName = "school_settings")
data class SchoolSettingEntity(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "Hein Chan Myae",
    val academicYear: String = "",
    val contactPhone: String = "+95 9 123456789",
    val email: String = "info@heinchanmyae.edu.mm",
    val address: String = "Yangon, Myanmar",
    val logoText: String = "HCM-SMS",
    val logoUri: String? = null,
    val website: String = "https://heinchanmyae.edu.mm",
    val principalName: String = "U Chan Myae",
    val motto: String = "Excellence, Wisdom, Integrity",
    val schoolSeal: String = "SEAL_VALIDATED",
    val defaultLanguage: String = "English",
    val dateFormat: String = "yyyy-MM-dd",
    val timeFormat: String = "24-Hour (HH:mm)",
    val currency: String = "MMK (Ks)",
    val systemNotificationsEnabled: Boolean = true,
    val backupRemindersEnabled: Boolean = true,
    val academicYearReminderEnabled: Boolean = true,
    val assessmentReminderEnabled: Boolean = true,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "grades",
    indices = [Index(value = ["gradeName"], unique = true)]
)
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gradeName: String, // KG, G1, G2, ..., G12
    val educationLevel: EducationLevel,
    val reportCardTemplate: String = "Standard",
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "school_classes",
    foreignKeys = [
        ForeignKey(
            entity = GradeEntity::class,
            parentColumns = ["id"],
            childColumns = ["gradeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("gradeId"),
        Index(value = ["gradeId", "className"], unique = true)
    ]
)
data class SchoolClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gradeId: Long,
    val className: String, // A, B, C, D...
    val capacity: Int = 40,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: SubjectCategory,
    val educationLevel: EducationLevel,
    val subTrack: String = "", // e.g. STEAMS-1, STEAMS-2, General
    val isEnabled: Boolean = true,
    val isEditable: Boolean = true,
    val isCustom: Boolean = false,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "grade_subject_cross_ref", primaryKeys = ["gradeId", "subjectId"])
data class GradeSubjectCrossRef(
    val gradeId: Long,
    val subjectId: Long,
    val isAdditional: Boolean = false
)

@Entity(tableName = "assessment_types")
data class AssessmentTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val educationLevel: EducationLevel,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "custom_exams")
data class CustomExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gradeId: Long, // 0 for all grades
    val examName: String,
    val isEnabled: Boolean = true,
    val description: String = "",
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "grading_policies")
data class GradingPolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val educationLevel: EducationLevel,
    val subjectName: String, // e.g. "Mathematics", "Myanmar", "English", "ALL"
    val maxMark: Int = 100,
    val passMark: Int = 40,
    val distinctionMark: Int = 75,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentCode: String, // e.g. HCM-2025-001
    val name: String,
    val gender: String, // Male, Female
    val dateOfBirth: String, // YYYY-MM-DD
    val gradeName: String,
    val className: String,
    val rollNumber: Int,
    val parentName: String,
    val phone: String,
    val address: String,
    val status: String = "Active", // Active, Inactive, Transferred
    val photoAvatarIndex: Int = 0,
    val stream: String = "", // STEAMS-1, STEAMS-2, or ""
    val uuid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

enum class AssessmentStatus(val displayName: String) {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userName: String,
    val roleName: String,
    val action: String,
    val details: String
)

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val academicYear: String = "",
    val grade: String,
    val className: String = "All Classes",
    val subjectName: String,
    val subjectType: String = "Academic Subject", // Academic Subject, Additional Subject, Custom Exam
    val assessmentType: String, // Monthly Test, CET 1, Pilot Test 1, etc.
    val assessmentName: String,
    val maxMarks: Int = 100,
    val assessmentDate: String, // YYYY-MM-DD
    val month: String = "", // e.g. "June", "July", etc.
    val weekNumber: String = "", // e.g. "WT1", "WT2", "WT3", "WT4"
    val description: String = "",
    val status: AssessmentStatus = AssessmentStatus.DRAFT,
    val createdBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis(),
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "assessment_schedules",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assessmentId")]
)
data class AssessmentScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val examTime: String = "09:00 AM - 10:30 AM",
    val roomNumber: String = "Room 101",
    val supervisor: String = "Tr. U Ba Mg"
)

@Entity(
    tableName = "assessment_subjects",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assessmentId")]
)
data class AssessmentSubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val subjectName: String,
    val maxMarks: Int = 100
)

@Entity(
    tableName = "student_marks",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["assessmentId", "studentId", "subjectName"], unique = true)
    ]
)
data class StudentMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val rollNo: Int,
    val subjectName: String,
    val maxMarks: Int = 100,
    val obtainedMarks: Double? = null, // null means pending entry
    val passMark: Int = 40,
    val distinctionMark: Int = 75,
    val isPassed: Boolean = false,
    val isDistinction: Boolean = false,
    val remarks: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Teacher",
    val uuid: String = "",
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "assessment_results",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["assessmentId", "studentId"], unique = true)
    ]
)
data class AssessmentResultSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val rollNo: Int,
    val totalObtained: Double = 0.0,
    val totalMax: Int = 100,
    val percentage: Double = 0.0,
    val overallResult: String = "Pending", // Pass, Fail, Distinction
    val distinctionCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val uuid: String = "",
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "assessment_lock_statuses",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assessmentId"], unique = true)]
)
data class AssessmentLockStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val isLocked: Boolean = false,
    val lockedBy: String = "",
    val lockedAt: Long = 0L,
    val lockNote: String = ""
)

// Module 5: Holistic Assessment & Student Growth Indicators (SGI)

enum class HcmPillar(val displayName: String, val shortCode: String) {
    HONESTY("Honesty", "H"),
    CURIOSITY("Curiosity", "C"),
    MINDFULNESS("Mindfulness", "M")
}

@Entity(tableName = "holistic_categories")
data class HolisticCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryName: String,
    val description: String = "",
    val pillar: String = "HONESTY", // "HONESTY", "CURIOSITY", "MINDFULNESS"
    val educationLevel: String = "PRIMARY", // "PRIMARY", "SECONDARY", "HIGH_SCHOOL"
    val maxStars: Int = 5,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "holistic_results",
    indices = [
        Index(value = ["studentId", "assessmentPeriod", "academicYear", "categoryId"], unique = true)
    ]
)
data class HolisticResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val assessmentPeriod: String,
    val academicYear: String,
    val grade: String,
    val className: String,
    val categoryId: Long,
    val categoryName: String,
    val pillar: String = "HONESTY",
    val educationLevel: String = "PRIMARY",
    val ratingStars: Int = 0,
    val maxStars: Int = 5,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Teacher",
    val uuid: String = "",
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "sgi_categories")
data class SgiCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val indicatorName: String,
    val description: String = "",
    val maxStars: Int = 5,
    val isEnabled: Boolean = true,
    val orderIndex: Int = 0,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "sgi_results",
    indices = [
        Index(value = ["studentId", "assessmentPeriod", "academicYear", "sgiCategoryId"], unique = true)
    ]
)
data class SgiResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val assessmentPeriod: String,
    val academicYear: String,
    val grade: String,
    val className: String,
    val sgiCategoryId: Long,
    val indicatorName: String,
    val ratingStars: Int = 0,
    val maxStars: Int = 5,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Teacher",
    val uuid: String = "",
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "teacher_comments",
    indices = [
        Index(value = ["studentId", "assessmentPeriod", "academicYear"], unique = true)
    ]
)
data class TeacherCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val assessmentPeriod: String,
    val academicYear: String,
    val grade: String,
    val className: String,
    val positiveComments: String = "",
    val areasForImprovement: String = "",
    val generalComment: String = "",
    val futureRecommendation: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Teacher",
    val uuid: String = "",
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "assessment_periods")
data class AssessmentPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodName: String,
    val educationLevel: String, // "PRIMARY", "SECONDARY", "HIGH_SCHOOL"
    val periodCategory: String = "", // e.g. "Monthly", "Pilot Test", "CET", "Weekly Test", "Lesson Completion Test"
    val isEnabled: Boolean = true,
    val orderIndex: Int = 0,
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

enum class AttendanceStatus(val displayName: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    LATE("Late"),
    LEAVE("Leave")
}

enum class AttendanceSession(val displayName: String) {
    MORNING("Morning"),
    EVENING("Evening")
}

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["academicYear", "date", "session", "grade", "className", "studentId"], unique = true),
        Index(value = ["studentId", "academicYear"]),
        Index(value = ["date", "session"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val academicYear: String = "",
    val date: String, // YYYY-MM-DD
    val session: AttendanceSession,
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val grade: String,
    val className: String,
    val status: AttendanceStatus,
    val recordedBy: String = "Teacher",
    val recordedDateTime: Long = System.currentTimeMillis(),
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

enum class AcademicYearStatus(val displayName: String) {
    ACTIVE("Active"),
    UPCOMING("Upcoming"),
    CLOSED("Closed"),
    ARCHIVED("Archived")
}

@Entity(
    tableName = "academic_years",
    indices = [Index(value = ["yearCode"], unique = true)]
)
data class AcademicYearEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearCode: String, // e.g. "2026-2027"
    val displayName: String, // e.g. "2026-2027 Academic Year"
    val startDate: String = "2026-06-01",
    val endDate: String = "2027-03-31",
    val status: AcademicYearStatus = AcademicYearStatus.UPCOMING,
    val isCurrentActive: Boolean = false,
    val closedDate: Long = 0L,
    val closedBy: String = "",
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

enum class PromotionAction(val displayName: String) {
    PROMOTED("Promoted"),
    RETAINED("Repeat Grade"),
    TRANSFERRED("Transferred Class"),
    GRADUATED("Graduated (Alumni)")
}

@Entity(
    tableName = "promotion_history",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["fromAcademicYear"]),
        Index(value = ["toAcademicYear"])
    ]
)
data class PromotionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentCode: String,
    val studentName: String,
    val fromAcademicYear: String,
    val toAcademicYear: String,
    val fromGrade: String,
    val toGrade: String,
    val fromClass: String,
    val toClass: String,
    val fromRollNumber: Int,
    val toRollNumber: Int,
    val actionType: PromotionAction,
    val promotedDate: Long = System.currentTimeMillis(),
    val promotedBy: String = "Admin",
    val remarks: String = "",
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "student_academic_history",
    indices = [
        Index(value = ["studentId", "academicYear"], unique = true)
    ]
)
data class StudentAcademicHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val academicYear: String,
    val gradeName: String,
    val className: String,
    val rollNumber: Int,
    val status: String = "Active"
)

@Entity(
    tableName = "teachers",
    indices = [
        Index(value = ["teacherCode"], unique = true)
    ]
)
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherCode: String,
    val fullName: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val assignedGrade: String = "Grade 1",
    val assignedClass: String = "A",
    val assignedSubjects: String = "Myanmar, English",
    val employmentStatus: String = "Active",
    val createdAt: Long = System.currentTimeMillis(),
    val uuid: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)






