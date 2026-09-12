package com.example.data.repository

import com.example.data.local.dao.TeacherDao
import com.example.data.local.entity.TeacherEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.sync.DeletionVerificationResult
import com.example.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class TeacherRepository(
    private val teacherDao: TeacherDao,
    private val authRepository: AuthRepository? = null
) {
    val allTeachers: Flow<List<TeacherEntity>> = teacherDao.getAllTeachers()
    val activeTeacherCount: Flow<Int> = teacherDao.getActiveTeacherCount()
    val totalTeacherCount: Flow<Int> = teacherDao.getTeacherCount()

    fun searchTeachers(query: String): Flow<List<TeacherEntity>> {
        return if (query.isBlank()) {
            teacherDao.getAllTeachers()
        } else {
            teacherDao.searchTeachers(query.trim())
        }
    }

    suspend fun getTeacherById(id: Long): TeacherEntity? {
        return teacherDao.getTeacherById(id)
    }

    suspend fun saveTeacher(teacher: TeacherEntity, performer: UserEntity? = null): Long {
        val current = performer ?: authRepository?.currentUser?.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN) {
            if (teacher.id == 0L) {
                throw SecurityException("Permission denied: Only Administrators can create teacher profiles.")
            }
        }

        val dirtyTeacher = teacher.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        val resultId = if (teacher.id == 0L) {
            teacherDao.insertTeacher(dirtyTeacher)
        } else {
            teacherDao.updateTeacher(dirtyTeacher)
            teacher.id
        }
        triggerBackgroundSync()
        return resultId
    }

    suspend fun deleteTeacher(
        teacher: TeacherEntity,
        performer: UserEntity? = null,
        onVerificationResult: ((DeletionVerificationResult) -> Unit)? = null
    ): DeletionVerificationResult {
        val current = performer ?: authRepository?.currentUser?.value
        if (current != null && (current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN)) {
            throw SecurityException("Permission denied: Teachers and Staff cannot delete teacher records.")
        }

        val updatedTeacher = teacher.copy(
            isDeleted = true,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        if (updatedTeacher.id == 0L) {
            teacherDao.insertTeacher(updatedTeacher)
        } else {
            teacherDao.updateTeacher(updatedTeacher)
        }
        triggerBackgroundSync()

        val verification = SyncManager.verifyAndPropagateDeletion(
            tableName = "teachers",
            uuid = teacher.uuid,
            codeOrKey = teacher.teacherCode,
            fallbackRemoteId = teacher.id,
            preferHardDelete = true
        )
        onVerificationResult?.invoke(verification)
        return verification
    }

    private fun triggerBackgroundSync() {
        SyncManager.triggerTableSyncAsync("teachers")
    }
}
