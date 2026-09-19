package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getAllStudentsList(): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY id DESC")
    suspend fun getAllStudentsIncludingDeleted(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE isDeleted = 1 ORDER BY id DESC")
    suspend fun getSoftDeletedStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE isDirty = 1")
    suspend fun getStudentsForSync(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE uuid = :uuid LIMIT 1")
    suspend fun getStudentByUuid(uuid: String): StudentEntity?

    @Query("SELECT * FROM students WHERE studentCode = :studentCode LIMIT 1")
    suspend fun getStudentByCode(studentCode: String): StudentEntity?

    @Query("UPDATE students SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markStudentSynced(id: Long, uuid: String)

    @Query("SELECT * FROM students WHERE isDeleted = 0 AND gradeName = :gradeName ORDER BY rollNumber ASC")
    fun getStudentsByGrade(gradeName: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE isDeleted = 0 AND gradeName = :gradeName AND className = :className ORDER BY rollNumber ASC")
    fun getStudentsByGradeAndClass(gradeName: String, className: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR studentCode LIKE '%' || :query || '%' OR parentName LIKE '%' || :query || '%' OR studentNrc LIKE '%' || :query || '%' OR fatherName LIKE '%' || :query || '%' OR motherName LIKE '%' || :query || '%')")
    fun searchStudents(query: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET isDeleted = 1, isDirty = 1, updatedAt = :timestamp WHERE id = :studentId")
    suspend fun softDeleteStudent(studentId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE students SET isDeleted = 1, isDirty = 1, updatedAt = :timestamp")
    suspend fun softDeleteAllStudents(timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Long)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}
