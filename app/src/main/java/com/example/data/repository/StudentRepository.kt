package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.StudentEntity
import com.example.data.sync.DeletionVerificationResult
import com.example.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepository(
    private val studentDao: StudentDao,
    private val context: Context? = null
) {

    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents().map { list ->
        list.distinctBy { if (it.studentCode.isNotBlank()) it.studentCode else "${it.name}_${it.gradeName}_${it.className}_${it.id}" }
    }

    fun getStudentsByGradeAndClass(gradeName: String, className: String): Flow<List<StudentEntity>> {
        return if (gradeName.isEmpty() || gradeName == "All") {
            studentDao.getAllStudents()
        } else if (className.isEmpty() || className == "All") {
            studentDao.getStudentsByGrade(gradeName)
        } else {
            studentDao.getStudentsByGradeAndClass(gradeName, className)
        }
    }

    fun searchStudents(query: String): Flow<List<StudentEntity>> {
        return studentDao.searchStudents(query)
    }

    suspend fun saveStudent(student: StudentEntity): Long {
        val studentToSave = student.copy(
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        val id = if (studentToSave.id == 0L) {
            studentDao.insertStudent(studentToSave)
        } else {
            studentDao.updateStudent(studentToSave)
            studentToSave.id
        }
        triggerBackgroundSync()
        return id
    }

    suspend fun deleteStudent(
        studentId: Long,
        onVerificationResult: ((DeletionVerificationResult) -> Unit)? = null
    ): DeletionVerificationResult {
        val student = studentDao.getStudentById(studentId)
        val uuid = student?.uuid ?: ""
        val code = student?.studentCode ?: ""

        if (student != null) {
            val deletedStudent = student.copy(
                isDeleted = true,
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            )
            studentDao.updateStudent(deletedStudent)
        } else {
            studentDao.deleteStudent(studentId)
        }
        triggerBackgroundSync()

        val verification = SyncManager.verifyAndPropagateDeletion(
            context = context,
            tableName = "students",
            uuid = uuid,
            codeOrKey = code,
            fallbackRemoteId = studentId,
            preferHardDelete = true
        )
        onVerificationResult?.invoke(verification)
        return verification
    }

    suspend fun importMockStudents(students: List<StudentEntity>) {
        val dirtyStudents = students.map {
            it.copy(isDirty = true, updatedAt = System.currentTimeMillis())
        }
        studentDao.insertStudents(dirtyStudents)
        triggerBackgroundSync()
    }

    suspend fun clearAll(
        onVerificationResult: ((DeletionVerificationResult) -> Unit)? = null
    ): DeletionVerificationResult {
        val now = System.currentTimeMillis()
        studentDao.softDeleteAllStudents(now)
        triggerBackgroundSync()

        val verification = SyncManager.verifyAndPropagateDeletion(
            context = context,
            tableName = "students",
            uuid = "",
            codeOrKey = "ALL_STUDENTS",
            preferHardDelete = true
        )
        onVerificationResult?.invoke(verification)
        return verification
    }

    private fun triggerBackgroundSync() {
        SyncManager.triggerTableSyncAsync("students", context)
    }
}
