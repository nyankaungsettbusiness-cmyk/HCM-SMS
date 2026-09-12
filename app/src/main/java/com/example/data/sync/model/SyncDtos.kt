package com.example.data.sync.model

import com.example.data.local.entity.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class StudentSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: String = "",
    val name: String = "",
    val grade: String = "",
    @SerialName("class_name") val className: String = "",
    val gender: String = "",
    @SerialName("date_of_birth") val dateOfBirth: String = "",
    @SerialName("parent_name") val parentName: String = "",
    @SerialName("parent_phone") val parentPhone: String = "",
    val address: String = "",
    val status: String = "Active",
    @SerialName("photo_avatar_index") val photoAvatarIndex: Int = 0,
    val stream: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0, existingRollNumber: Int = 0): StudentEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedCreatedAt = parseIsoToMillis(createdAt)
        return StudentEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            studentCode = studentId,
            name = name,
            gender = gender,
            dateOfBirth = dateOfBirth,
            gradeName = grade,
            className = className,
            rollNumber = existingRollNumber,
            parentName = parentName,
            phone = parentPhone,
            address = address,
            status = status,
            photoAvatarIndex = photoAvatarIndex,
            stream = stream,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (parsedCreatedAt > 0) parsedCreatedAt else System.currentTimeMillis(),
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: StudentEntity): StudentSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return StudentSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentCode,
                name = entity.name,
                grade = entity.gradeName,
                className = entity.className,
                gender = entity.gender,
                dateOfBirth = entity.dateOfBirth,
                parentName = entity.parentName,
                parentPhone = entity.phone,
                address = entity.address,
                status = entity.status,
                photoAvatarIndex = entity.photoAvatarIndex,
                stream = entity.stream,
                createdAt = millisToIso(entity.createdAt),
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class TeacherSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("teacher_id") val teacherId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    @SerialName("assigned_grade") val assignedGrade: String = "KG",
    @SerialName("assigned_class") val assignedClass: String = "A",
    @SerialName("assigned_subjects") val assignedSubjects: String = "Myanmar, English",
    @SerialName("employment_status") val employmentStatus: String = "Active",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0, existingAddress: String = ""): TeacherEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedCreatedAt = parseIsoToMillis(createdAt)
        return TeacherEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            teacherCode = teacherId,
            fullName = name,
            phone = phone,
            email = email,
            address = existingAddress,
            assignedGrade = assignedGrade,
            assignedClass = assignedClass,
            assignedSubjects = assignedSubjects,
            employmentStatus = employmentStatus,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (parsedCreatedAt > 0) parsedCreatedAt else System.currentTimeMillis(),
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: TeacherEntity): TeacherSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return TeacherSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                teacherId = entity.teacherCode,
                name = entity.fullName,
                phone = entity.phone,
                email = entity.email,
                assignedGrade = entity.assignedGrade,
                assignedClass = entity.assignedClass,
                assignedSubjects = entity.assignedSubjects,
                employmentStatus = entity.employmentStatus,
                createdAt = millisToIso(entity.createdAt),
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class UserSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    val username: String = "",
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("password_hash") val passwordHash: String? = null,
    val role: String? = "TEACHER",
    val email: String? = "",
    val phone: String? = "",
    val status: String? = "ACTIVE",
    @SerialName("school_id") val schoolId: String? = "default_school",
    @SerialName("is_deleted") val isDeleted: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toEntity(existingLocalId: Long = 0): UserEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedCreatedAt = parseIsoToMillis(createdAt)
        val parsedRole = try {
            UserRole.valueOf(role?.trim()?.uppercase() ?: "TEACHER")
        } catch (e: Exception) {
            when (role?.trim()?.uppercase()) {
                "SUPER_ADMIN", "SUPERADMIN" -> UserRole.SUPER_ADMIN
                "ADMIN" -> UserRole.ADMIN
                "TEACHER" -> UserRole.TEACHER
                "OFFICE_STAFF", "STAFF" -> UserRole.OFFICE_STAFF
                else -> UserRole.TEACHER
            }
        }
        val resolvedName = (fullName?.ifBlank { null } ?: username).ifBlank { "User" }
        val resolvedPassword = if (!passwordHash.isNullOrBlank()) passwordHash else "cfa1cd8d3caf45902bfc19e5bdc131abca5208f2f4698bc327c5115212d7142e"
        val resolvedStatus = when (status?.trim()?.uppercase()) {
            "INACTIVE" -> UserStatus.INACTIVE
            "LOCKED" -> UserStatus.LOCKED
            "SUSPENDED" -> UserStatus.SUSPENDED
            else -> UserStatus.ACTIVE
        }
        val isAct = (resolvedStatus == UserStatus.ACTIVE) && !(isDeleted ?: false)
        return UserEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            username = username.trim(),
            fullName = resolvedName,
            role = parsedRole,
            email = email ?: "",
            phone = phone ?: "",
            salt = "HCM_SALT_2026",
            status = resolvedStatus,
            isActive = isAct,
            passwordHash = resolvedPassword,
            createdAt = if (parsedCreatedAt > 0) parsedCreatedAt else System.currentTimeMillis(),
            lastLoginTimestamp = System.currentTimeMillis() - 86400000L,
            lastLogoutTimestamp = null,
            mustChangePassword = false,
            failedLoginAttempts = 0,
            isDeleted = isDeleted ?: false,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false
        )
    }

    companion object {
        fun fromEntity(entity: UserEntity): UserSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return UserSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                username = entity.username,
                passwordHash = entity.passwordHash,
                fullName = entity.fullName,
                role = entity.role.name,
                email = entity.email,
                phone = entity.phone,
                status = entity.status.name,
                schoolId = "default_school",
                isDeleted = entity.isDeleted,
                createdAt = millisToIso(entity.createdAt),
                updatedAt = millisToIso(entity.updatedAt)
            )
        }
    }
}

@Serializable
data class SchoolSettingSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("school_name_en") val schoolNameEn: String = "Hein Chan Myae",
    @SerialName("school_name_my") val schoolNameMy: String = "",
    val motto: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    @SerialName("principal_name") val principalName: String = "",
    @SerialName("logo_path") val logoPath: String = "",
    @SerialName("current_academic_year") val currentAcademicYear: String = "",
    @SerialName("system_notifications_enabled") val systemNotificationsEnabled: Boolean = true,
    @SerialName("backup_reminders_enabled") val backupRemindersEnabled: Boolean = true,
    @SerialName("academic_year_reminder_enabled") val academicYearReminderEnabled: Boolean = true,
    @SerialName("assessment_reminder_enabled") val assessmentReminderEnabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Int = 1): SchoolSettingEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return SchoolSettingEntity(
            id = existingLocalId,
            schoolName = schoolNameEn,
            academicYear = currentAcademicYear,
            contactPhone = phone,
            email = email,
            address = address,
            website = website,
            principalName = principalName,
            motto = motto,
            logoUri = logoPath.ifBlank { null },
            systemNotificationsEnabled = systemNotificationsEnabled,
            backupRemindersEnabled = backupRemindersEnabled,
            academicYearReminderEnabled = academicYearReminderEnabled,
            assessmentReminderEnabled = assessmentReminderEnabled,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: SchoolSettingEntity): SchoolSettingSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return SchoolSettingSupabaseDto(
                id = entity.id.toLong(),
                uuid = validUuid,
                schoolNameEn = entity.schoolName,
                schoolNameMy = "",
                motto = entity.motto,
                address = entity.address,
                phone = entity.contactPhone,
                email = entity.email,
                website = entity.website,
                principalName = entity.principalName,
                logoPath = entity.logoUri ?: "",
                currentAcademicYear = entity.academicYear,
                systemNotificationsEnabled = entity.systemNotificationsEnabled,
                backupRemindersEnabled = entity.backupRemindersEnabled,
                academicYearReminderEnabled = entity.academicYearReminderEnabled,
                assessmentReminderEnabled = entity.assessmentReminderEnabled,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class GradeSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("grade_name") val gradeName: String = "",
    @SerialName("education_level") val educationLevel: String = "PRIMARY",
    @SerialName("report_card_template") val reportCardTemplate: String = "Standard",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): GradeEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val level = try { EducationLevel.valueOf(educationLevel) } catch (e: Exception) { EducationLevel.PRIMARY }
        return GradeEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            gradeName = gradeName,
            educationLevel = level,
            reportCardTemplate = reportCardTemplate,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: GradeEntity): GradeSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return GradeSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                gradeName = entity.gradeName,
                educationLevel = entity.educationLevel.name,
                reportCardTemplate = entity.reportCardTemplate,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class SchoolClassSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("grade_id") val gradeId: Long = 0L,
    @SerialName("class_name") val className: String = "",
    val capacity: Int = 40,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0, resolvedGradeId: Long = gradeId): SchoolClassEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return SchoolClassEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            gradeId = resolvedGradeId,
            className = className,
            capacity = capacity,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: SchoolClassEntity): SchoolClassSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return SchoolClassSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                gradeId = entity.gradeId,
                className = entity.className,
                capacity = entity.capacity,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class SubjectSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("subject_code") val subjectCode: String = "",
    val name: String = "",
    val category: String = "ACADEMIC",
    @SerialName("education_level") val educationLevel: String = "PRIMARY",
    @SerialName("sub_track") val subTrack: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_editable") val isEditable: Boolean = true,
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): SubjectEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val cat = try { SubjectCategory.valueOf(category.trim().uppercase()) } catch (e: Exception) { SubjectCategory.ACADEMIC }
        val level = try { EducationLevel.valueOf(educationLevel.trim().uppercase()) } catch (e: Exception) { EducationLevel.PRIMARY }
        return SubjectEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            name = name,
            category = cat,
            educationLevel = level,
            subTrack = subTrack,
            isEnabled = isEnabled,
            isEditable = isEditable,
            isCustom = isCustom,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: SubjectEntity): SubjectSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return SubjectSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                subjectCode = if (entity.uuid.isNotBlank()) entity.uuid else entity.name,
                name = entity.name,
                category = entity.category.name,
                educationLevel = entity.educationLevel.name,
                subTrack = entity.subTrack,
                isEnabled = entity.isEnabled,
                isEditable = entity.isEditable,
                isCustom = entity.isCustom,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class SubjectMinimalSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    val name: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class GradeSubjectCrossRefSupabaseDto(
    @SerialName("grade_id") val gradeId: Long = 0L,
    @SerialName("subject_id") val subjectId: Long = 0L
) {
    fun toEntity(): GradeSubjectCrossRef = GradeSubjectCrossRef(gradeId = gradeId, subjectId = subjectId)

    companion object {
        fun fromEntity(ref: GradeSubjectCrossRef): GradeSubjectCrossRefSupabaseDto =
            GradeSubjectCrossRefSupabaseDto(gradeId = ref.gradeId, subjectId = ref.subjectId)
    }
}

@Serializable
data class AssessmentTypeSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    val name: String = "",
    @SerialName("education_level") val educationLevel: String = "PRIMARY",
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): AssessmentTypeEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val level = try { EducationLevel.valueOf(educationLevel) } catch (e: Exception) { EducationLevel.PRIMARY }
        return AssessmentTypeEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            name = name,
            educationLevel = level,
            isCustom = isCustom,
            isEnabled = isEnabled,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: AssessmentTypeEntity): AssessmentTypeSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AssessmentTypeSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                name = entity.name,
                educationLevel = entity.educationLevel.name,
                isCustom = entity.isCustom,
                isEnabled = entity.isEnabled,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class CustomExamSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("grade_id") val gradeId: Long = 0L,
    @SerialName("exam_name") val examName: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    val description: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): CustomExamEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return CustomExamEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            gradeId = gradeId,
            examName = examName,
            isEnabled = isEnabled,
            description = description,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: CustomExamEntity): CustomExamSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return CustomExamSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                gradeId = entity.gradeId,
                examName = entity.examName,
                isEnabled = entity.isEnabled,
                description = entity.description,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class GradingPolicySupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("education_level") val educationLevel: String = "PRIMARY",
    @SerialName("subject_name") val subjectName: String = "",
    @SerialName("max_mark") val maxMark: Int = 100,
    @SerialName("pass_mark") val passMark: Int = 40,
    @SerialName("distinction_mark") val distinctionMark: Int = 75,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): GradingPolicyEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val level = try { EducationLevel.valueOf(educationLevel) } catch (e: Exception) { EducationLevel.PRIMARY }
        return GradingPolicyEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            educationLevel = level,
            subjectName = subjectName,
            maxMark = maxMark,
            passMark = passMark,
            distinctionMark = distinctionMark,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: GradingPolicyEntity): GradingPolicySupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return GradingPolicySupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                educationLevel = entity.educationLevel.name,
                subjectName = entity.subjectName,
                maxMark = entity.maxMark,
                passMark = entity.passMark,
                distinctionMark = entity.distinctionMark,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AssessmentSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    val title: String = "",
    val grade: String = "",
    val subject: String = "",
    @SerialName("assessment_type") val assessmentType: String = "",
    @SerialName("assessment_period") val assessmentPeriod: String = "Monthly",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("max_marks") val maxMarks: Double = 100.0,
    @SerialName("date_conducted") val dateConducted: String = "",
    val description: String = "",
    val status: String = "DRAFT",
    @SerialName("created_by") val createdBy: String = "Admin",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): AssessmentEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedCreatedAt = parseIsoToMillis(createdAt)
        val parsedStatus = try { AssessmentStatus.valueOf(status) } catch (e: Exception) { AssessmentStatus.DRAFT }
        return AssessmentEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            academicYear = academicYear,
            grade = grade,
            className = "All Classes",
            subjectName = subject,
            subjectType = "Academic Subject",
            assessmentType = assessmentType,
            assessmentName = title,
            maxMarks = maxMarks.toInt(),
            assessmentDate = dateConducted,
            month = assessmentPeriod,
            description = description,
            status = parsedStatus,
            createdBy = createdBy,
            createdAt = if (parsedCreatedAt > 0) parsedCreatedAt else System.currentTimeMillis(),
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: AssessmentEntity): AssessmentSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AssessmentSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                title = entity.assessmentName,
                grade = entity.grade,
                subject = entity.subjectName,
                assessmentType = entity.assessmentType,
                assessmentPeriod = entity.month.ifBlank { "Monthly" },
                academicYear = entity.academicYear,
                maxMarks = entity.maxMarks.toDouble(),
                dateConducted = entity.assessmentDate,
                description = entity.description,
                status = entity.status.name,
                createdBy = entity.createdBy,
                createdAt = millisToIso(entity.createdAt),
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class StudentMarkSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("assessment_id") val assessmentId: Long = 0L,
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("marks_obtained") val marksObtained: Double = 0.0,
    @SerialName("is_absent") val isAbsent: Boolean = false,
    @SerialName("is_exempt") val isExempt: Boolean = false,
    @SerialName("is_passed") val isPassed: Boolean = false,
    @SerialName("is_distinction") val isDistinction: Boolean = false,
    val remarks: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedAssessmentId: Long = assessmentId,
        resolvedStudentId: Long = studentId,
        studentCode: String = "",
        studentName: String = "",
        rollNo: Int = 0,
        subjectName: String = ""
    ): StudentMarkEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return StudentMarkEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            assessmentId = resolvedAssessmentId,
            studentId = resolvedStudentId,
            studentCode = studentCode,
            studentName = studentName,
            rollNo = rollNo,
            subjectName = subjectName,
            obtainedMarks = if (isAbsent || isExempt) null else marksObtained,
            isPassed = isPassed,
            isDistinction = isDistinction,
            remarks = remarks,
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            updatedBy = updatedBy,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(
            entity: StudentMarkEntity,
            targetStudentId: Long = entity.studentId,
            targetAssessmentId: Long = entity.assessmentId,
            remoteId: Long? = null
        ): StudentMarkSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return StudentMarkSupabaseDto(
                id = remoteId ?: (if (entity.id > 0) entity.id else null),
                uuid = validUuid,
                assessmentId = targetAssessmentId,
                studentId = targetStudentId,
                marksObtained = entity.obtainedMarks ?: 0.0,
                isAbsent = entity.obtainedMarks == null,
                isExempt = false,
                isPassed = entity.isPassed,
                isDistinction = entity.isDistinction,
                remarks = entity.remarks,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class StudentMarkPushDto(
    val uuid: String = "",
    @SerialName("assessment_id") val assessmentId: Long = 0L,
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("marks_obtained") val marksObtained: Double = 0.0,
    @SerialName("is_absent") val isAbsent: Boolean = false,
    @SerialName("is_exempt") val isExempt: Boolean = false,
    @SerialName("is_passed") val isPassed: Boolean = false,
    @SerialName("is_distinction") val isDistinction: Boolean = false,
    val remarks: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: StudentMarkEntity,
            targetStudentId: Long = entity.studentId,
            targetAssessmentId: Long = entity.assessmentId
        ): StudentMarkPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return StudentMarkPushDto(
                uuid = validUuid,
                assessmentId = targetAssessmentId,
                studentId = targetStudentId,
                marksObtained = entity.obtainedMarks ?: 0.0,
                isAbsent = entity.obtainedMarks == null,
                isExempt = false,
                isPassed = entity.isPassed,
                isDistinction = entity.isDistinction,
                remarks = entity.remarks,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AssessmentResultSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "Monthly",
    @SerialName("academic_year") val academicYear: String = "",
    val grade: String = "",
    @SerialName("class_name") val className: String = "",
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    @SerialName("max_possible_marks") val maxPossibleMarks: Double = 100.0,
    val percentage: Double = 0.0,
    @SerialName("overall_result") val overallResult: String = "Pending",
    @SerialName("distinction_count") val distinctionCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedAssessmentId: Long = 0,
        resolvedStudentId: Long = studentId,
        studentCode: String = "",
        studentName: String = "",
        rollNo: Int = 0
    ): AssessmentResultSummaryEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return AssessmentResultSummaryEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            assessmentId = resolvedAssessmentId,
            studentId = resolvedStudentId,
            studentCode = studentCode,
            studentName = studentName,
            rollNo = rollNo,
            totalObtained = totalMarks,
            totalMax = maxPossibleMarks.toInt(),
            percentage = percentage,
            overallResult = overallResult,
            distinctionCount = distinctionCount,
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: AssessmentResultSummaryEntity): AssessmentResultSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AssessmentResultSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentId,
                assessmentPeriod = "Monthly",
                academicYear = "2026-2027",
                grade = "Grade 1",
                className = "A",
                totalMarks = entity.totalObtained,
                maxPossibleMarks = entity.totalMax.toDouble(),
                percentage = entity.percentage,
                overallResult = entity.overallResult,
                distinctionCount = entity.distinctionCount,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AssessmentResultPushDto(
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "Monthly",
    @SerialName("academic_year") val academicYear: String = "",
    val grade: String = "",
    @SerialName("class_name") val className: String = "",
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    @SerialName("max_possible_marks") val maxPossibleMarks: Double = 100.0,
    val percentage: Double = 0.0,
    @SerialName("overall_result") val overallResult: String = "Pending",
    @SerialName("distinction_count") val distinctionCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: AssessmentResultSummaryEntity,
            targetStudentId: Long = entity.studentId
        ): AssessmentResultPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AssessmentResultPushDto(
                uuid = validUuid,
                studentId = targetStudentId,
                assessmentPeriod = "Monthly",
                academicYear = "2026-2027",
                grade = "Grade 1",
                className = "A",
                totalMarks = entity.totalObtained,
                maxPossibleMarks = entity.totalMax.toDouble(),
                percentage = entity.percentage,
                overallResult = entity.overallResult,
                distinctionCount = entity.distinctionCount,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class HolisticCategorySupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    val name: String = "",
    val domain: String = "",
    @SerialName("category_name") val categoryName: String = "",
    val description: String = "",
    @SerialName("max_stars") val maxStars: Int = 5,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_default") val isDefault: Boolean = true,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): HolisticCategoryEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val resolvedName = categoryName.ifBlank { name }
        return HolisticCategoryEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            categoryName = resolvedName,
            description = description,
            pillar = domain,
            educationLevel = "PRIMARY",
            maxStars = maxStars,
            isEnabled = isEnabled,
            isDefault = isDefault,
            orderIndex = orderIndex,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: HolisticCategoryEntity): HolisticCategorySupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            val validName = entity.categoryName
            return HolisticCategorySupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                name = validName,
                domain = entity.pillar,
                categoryName = validName,
                description = entity.description,
                maxStars = entity.maxStars,
                isEnabled = entity.isEnabled,
                isDefault = entity.isDefault,
                orderIndex = entity.orderIndex,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class HolisticResultSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("category_id") val categoryId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("rating_stars") val ratingStars: Int = 0,
    @SerialName("max_stars") val maxStars: Int = 5,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedStudentId: Long = studentId,
        resolvedCategoryId: Long = categoryId,
        categoryName: String = "",
        grade: String = "",
        className: String = ""
    ): HolisticResultEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return HolisticResultEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            studentId = resolvedStudentId,
            assessmentPeriod = assessmentPeriod,
            academicYear = academicYear,
            grade = grade,
            className = className,
            categoryId = resolvedCategoryId,
            categoryName = categoryName,
            ratingStars = ratingStars,
            maxStars = maxStars,
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            updatedBy = updatedBy,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: HolisticResultEntity): HolisticResultSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return HolisticResultSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentId,
                categoryId = entity.categoryId,
                assessmentPeriod = entity.assessmentPeriod,
                academicYear = entity.academicYear,
                ratingStars = entity.ratingStars,
                maxStars = entity.maxStars,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class HolisticResultPushDto(
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("category_id") val categoryId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("rating_stars") val ratingStars: Int = 0,
    @SerialName("max_stars") val maxStars: Int = 5,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: HolisticResultEntity,
            targetStudentId: Long = entity.studentId,
            targetCategoryId: Long = entity.categoryId
        ): HolisticResultPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return HolisticResultPushDto(
                uuid = validUuid,
                studentId = targetStudentId,
                categoryId = targetCategoryId,
                assessmentPeriod = entity.assessmentPeriod,
                academicYear = entity.academicYear,
                ratingStars = entity.ratingStars,
                maxStars = entity.maxStars,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class SgiCategorySupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    val name: String = "",
    @SerialName("category_name") val categoryName: String = "",
    val description: String = "",
    @SerialName("max_stars") val maxStars: Int = 5,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): SgiCategoryEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val resolvedName = categoryName.ifBlank { name }
        return SgiCategoryEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            indicatorName = resolvedName,
            description = description,
            maxStars = maxStars,
            isEnabled = isEnabled,
            orderIndex = orderIndex,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: SgiCategoryEntity): SgiCategorySupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            val validName = entity.indicatorName
            return SgiCategorySupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                name = validName,
                categoryName = validName,
                description = entity.description,
                maxStars = entity.maxStars,
                isEnabled = entity.isEnabled,
                orderIndex = entity.orderIndex,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class SgiResultSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("category_id") val categoryId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("rating_stars") val ratingStars: Int = 0,
    @SerialName("max_stars") val maxStars: Int = 5,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedStudentId: Long = studentId,
        resolvedCategoryId: Long = categoryId,
        indicatorName: String = "",
        grade: String = "",
        className: String = ""
    ): SgiResultEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return SgiResultEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            studentId = resolvedStudentId,
            assessmentPeriod = assessmentPeriod,
            academicYear = academicYear,
            grade = grade,
            className = className,
            sgiCategoryId = resolvedCategoryId,
            indicatorName = indicatorName,
            ratingStars = ratingStars,
            maxStars = maxStars,
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            updatedBy = updatedBy,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: SgiResultEntity): SgiResultSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return SgiResultSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentId,
                categoryId = entity.sgiCategoryId,
                assessmentPeriod = entity.assessmentPeriod,
                academicYear = entity.academicYear,
                ratingStars = entity.ratingStars,
                maxStars = entity.maxStars,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class SgiResultPushDto(
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("category_id") val categoryId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("rating_stars") val ratingStars: Int = 0,
    @SerialName("max_stars") val maxStars: Int = 5,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: SgiResultEntity,
            targetStudentId: Long = entity.studentId,
            targetCategoryId: Long = entity.sgiCategoryId
        ): SgiResultPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return SgiResultPushDto(
                uuid = validUuid,
                studentId = targetStudentId,
                categoryId = targetCategoryId,
                assessmentPeriod = entity.assessmentPeriod,
                academicYear = entity.academicYear,
                ratingStars = entity.ratingStars,
                maxStars = entity.maxStars,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class TeacherCommentSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("strength_comment") val strengthComment: String = "",
    @SerialName("need_improvement_comment") val needImprovementComment: String = "",
    @SerialName("general_comment") val generalComment: String = "",
    @SerialName("future_recommendation") val futureRecommendation: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedStudentId: Long = studentId,
        grade: String = "",
        className: String = ""
    ): TeacherCommentEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return TeacherCommentEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            studentId = resolvedStudentId,
            assessmentPeriod = assessmentPeriod,
            academicYear = academicYear,
            grade = grade,
            className = className,
            positiveComments = strengthComment,
            areasForImprovement = needImprovementComment,
            generalComment = generalComment,
            futureRecommendation = futureRecommendation,
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            updatedBy = updatedBy,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: TeacherCommentEntity): TeacherCommentSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return TeacherCommentSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentId,
                assessmentPeriod = entity.assessmentPeriod,
                academicYear = entity.academicYear,
                strengthComment = entity.positiveComments,
                needImprovementComment = entity.areasForImprovement,
                generalComment = entity.generalComment,
                futureRecommendation = entity.futureRecommendation,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class TeacherCommentPushDto(
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("assessment_period") val assessmentPeriod: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    @SerialName("strength_comment") val strengthComment: String = "",
    @SerialName("need_improvement_comment") val needImprovementComment: String = "",
    @SerialName("general_comment") val generalComment: String = "",
    @SerialName("future_recommendation") val futureRecommendation: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String = "Teacher",
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: TeacherCommentEntity,
            targetStudentId: Long = entity.studentId
        ): TeacherCommentPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return TeacherCommentPushDto(
                uuid = validUuid,
                studentId = targetStudentId,
                assessmentPeriod = entity.assessmentPeriod,
                academicYear = entity.academicYear,
                strengthComment = entity.positiveComments,
                needImprovementComment = entity.areasForImprovement,
                generalComment = entity.generalComment,
                futureRecommendation = entity.futureRecommendation,
                updatedAt = millisToIso(entity.updatedAt),
                updatedBy = entity.updatedBy,
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AssessmentPeriodSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("period_name") val periodName: String = "",
    @SerialName("education_level") val educationLevel: String = "PRIMARY",
    @SerialName("period_category") val periodCategory: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): AssessmentPeriodEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        return AssessmentPeriodEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            periodName = periodName,
            educationLevel = educationLevel,
            periodCategory = periodCategory,
            isEnabled = isEnabled,
            orderIndex = orderIndex,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: AssessmentPeriodEntity): AssessmentPeriodSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AssessmentPeriodSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                periodName = entity.periodName,
                educationLevel = entity.educationLevel,
                periodCategory = entity.periodCategory,
                isEnabled = entity.isEnabled,
                orderIndex = entity.orderIndex,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AttendanceRecordSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    val date: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    val grade: String = "",
    @SerialName("class_name") val className: String = "",
    val status: String = "PRESENT",
    @SerialName("recorded_by") val recordedBy: String = "Teacher",
    @SerialName("recorded_date_time") val recordedDateTime: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedStudentId: Long = studentId,
        studentCode: String = "",
        studentName: String = ""
    ): AttendanceRecordEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedRecTime = parseIsoToMillis(recordedDateTime)
        val parsedStatus = try { AttendanceStatus.valueOf(status) } catch (e: Exception) { AttendanceStatus.PRESENT }
        return AttendanceRecordEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            academicYear = academicYear,
            date = date,
            session = AttendanceSession.MORNING,
            studentId = resolvedStudentId,
            studentCode = studentCode,
            studentName = studentName,
            grade = grade,
            className = className,
            status = parsedStatus,
            recordedBy = recordedBy,
            recordedDateTime = if (parsedRecTime > 0) parsedRecTime else System.currentTimeMillis(),
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: AttendanceRecordEntity): AttendanceRecordSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AttendanceRecordSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentId,
                date = entity.date,
                academicYear = entity.academicYear,
                grade = entity.grade,
                className = entity.className,
                status = entity.status.name,
                recordedBy = entity.recordedBy,
                recordedDateTime = millisToIso(entity.recordedDateTime),
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AttendanceRecordPushDto(
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    val date: String = "",
    @SerialName("academic_year") val academicYear: String = "",
    val grade: String = "",
    @SerialName("class_name") val className: String = "",
    val status: String = "PRESENT",
    @SerialName("recorded_by") val recordedBy: String = "Teacher",
    @SerialName("recorded_date_time") val recordedDateTime: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: AttendanceRecordEntity,
            targetStudentId: Long = entity.studentId
        ): AttendanceRecordPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AttendanceRecordPushDto(
                uuid = validUuid,
                studentId = targetStudentId,
                date = entity.date,
                academicYear = entity.academicYear,
                grade = entity.grade,
                className = entity.className,
                status = entity.status.name,
                recordedBy = entity.recordedBy,
                recordedDateTime = millisToIso(entity.recordedDateTime),
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class AcademicYearSupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("year_name") val yearName: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = "",
    val status: String = "UPCOMING",
    @SerialName("is_current_active") val isCurrentActive: Boolean = false,
    @SerialName("closed_date") val closedDate: Long? = 0L,
    @SerialName("closed_by") val closedBy: String? = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(existingLocalId: Long = 0): AcademicYearEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedStatus = try { AcademicYearStatus.valueOf(status) } catch (e: Exception) { AcademicYearStatus.UPCOMING }
        return AcademicYearEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            yearCode = yearName,
            displayName = if (yearName.isNotBlank()) "$yearName Academic Year" else "Academic Year",
            startDate = startDate,
            endDate = endDate,
            status = parsedStatus,
            isCurrentActive = isCurrentActive,
            closedDate = closedDate ?: 0L,
            closedBy = closedBy ?: "",
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: AcademicYearEntity): AcademicYearSupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return AcademicYearSupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                yearName = entity.yearCode,
                startDate = entity.startDate,
                endDate = entity.endDate,
                status = entity.status.name,
                isCurrentActive = entity.isCurrentActive,
                closedDate = entity.closedDate,
                closedBy = entity.closedBy,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class PromotionHistorySupabaseDto(
    val id: Long? = null,
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("from_academic_year") val fromAcademicYear: String = "",
    @SerialName("to_academic_year") val toAcademicYear: String = "",
    @SerialName("from_grade") val fromGrade: String = "",
    @SerialName("to_grade") val toGrade: String = "",
    @SerialName("from_class") val fromClass: String = "",
    @SerialName("to_class") val toClass: String = "",
    @SerialName("action_type") val actionType: String = "PROMOTED",
    @SerialName("promoted_date") val promotedDate: String? = null,
    @SerialName("promoted_by") val promotedBy: String = "Admin",
    val remarks: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(
        existingLocalId: Long = 0,
        resolvedStudentId: Long = studentId,
        studentCode: String = "",
        studentName: String = ""
    ): PromotionHistoryEntity {
        val parsedUpdatedAt = parseIsoToMillis(updatedAt)
        val parsedPromotedDate = parseIsoToMillis(promotedDate)
        val parsedAction = try { PromotionAction.valueOf(actionType) } catch (e: Exception) { PromotionAction.PROMOTED }
        return PromotionHistoryEntity(
            id = if (existingLocalId > 0) existingLocalId else (id ?: 0),
            studentId = resolvedStudentId,
            studentCode = studentCode,
            studentName = studentName,
            fromAcademicYear = fromAcademicYear,
            toAcademicYear = toAcademicYear,
            fromGrade = fromGrade,
            toGrade = toGrade,
            fromClass = fromClass,
            toClass = toClass,
            fromRollNumber = 0,
            toRollNumber = 0,
            actionType = parsedAction,
            promotedDate = if (parsedPromotedDate > 0) parsedPromotedDate else System.currentTimeMillis(),
            promotedBy = promotedBy,
            remarks = remarks,
            uuid = uuid.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (parsedUpdatedAt > 0) parsedUpdatedAt else System.currentTimeMillis(),
            isDirty = false,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromEntity(entity: PromotionHistoryEntity): PromotionHistorySupabaseDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return PromotionHistorySupabaseDto(
                id = if (entity.id > 0) entity.id else null,
                uuid = validUuid,
                studentId = entity.studentId,
                fromAcademicYear = entity.fromAcademicYear,
                toAcademicYear = entity.toAcademicYear,
                fromGrade = entity.fromGrade,
                toGrade = entity.toGrade,
                fromClass = entity.fromClass,
                toClass = entity.toClass,
                actionType = entity.actionType.name,
                promotedDate = millisToIso(entity.promotedDate),
                promotedBy = entity.promotedBy,
                remarks = entity.remarks,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Serializable
data class PromotionHistoryPushDto(
    val uuid: String = "",
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("from_academic_year") val fromAcademicYear: String = "",
    @SerialName("to_academic_year") val toAcademicYear: String = "",
    @SerialName("from_grade") val fromGrade: String = "",
    @SerialName("to_grade") val toGrade: String = "",
    @SerialName("from_class") val fromClass: String = "",
    @SerialName("to_class") val toClass: String = "",
    @SerialName("action_type") val actionType: String = "PROMOTED",
    @SerialName("promoted_date") val promotedDate: String? = null,
    @SerialName("promoted_by") val promotedBy: String = "Admin",
    val remarks: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(
            entity: PromotionHistoryEntity,
            targetStudentId: Long = entity.studentId
        ): PromotionHistoryPushDto {
            val validUuid = entity.uuid.ifBlank { UUID.randomUUID().toString() }
            return PromotionHistoryPushDto(
                uuid = validUuid,
                studentId = targetStudentId,
                fromAcademicYear = entity.fromAcademicYear,
                toAcademicYear = entity.toAcademicYear,
                fromGrade = entity.fromGrade,
                toGrade = entity.toGrade,
                fromClass = entity.fromClass,
                toClass = entity.toClass,
                actionType = entity.actionType.name,
                promotedDate = millisToIso(entity.promotedDate),
                promotedBy = entity.promotedBy,
                remarks = entity.remarks,
                updatedAt = millisToIso(entity.updatedAt),
                isDeleted = entity.isDeleted
            )
        }
    }
}

@Suppress("NewApi")
fun parseIsoToMillis(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return 0L
    return try {
        Instant.parse(isoString).toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}

@Suppress("NewApi")
fun millisToIso(millis: Long): String {
    val m = if (millis <= 0) System.currentTimeMillis() else millis
    return try {
        Instant.ofEpochMilli(m).toString()
    } catch (e: Exception) {
        Instant.now().toString()
    }
}
