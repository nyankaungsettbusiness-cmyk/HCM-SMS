package com.example.data.local.db

import androidx.room.TypeConverter
import com.example.data.local.entity.EducationLevel
import com.example.data.local.entity.SubjectCategory
import com.example.data.local.entity.UserRole
import com.example.data.local.entity.UserStatus

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.ADMIN
    }

    @TypeConverter
    fun fromUserStatus(value: UserStatus): String = value.name

    @TypeConverter
    fun toUserStatus(value: String): UserStatus = try {
        UserStatus.valueOf(value)
    } catch (e: Exception) {
        UserStatus.ACTIVE
    }

    @TypeConverter
    fun fromEducationLevel(value: EducationLevel): String = value.name

    @TypeConverter
    fun toEducationLevel(value: String): EducationLevel = try {
        EducationLevel.valueOf(value)
    } catch (e: Exception) {
        EducationLevel.PRIMARY
    }

    @TypeConverter
    fun fromSubjectCategory(value: SubjectCategory): String = value.name

    @TypeConverter
    fun toSubjectCategory(value: String): SubjectCategory = try {
        SubjectCategory.valueOf(value)
    } catch (e: Exception) {
        SubjectCategory.ACADEMIC
    }
}
