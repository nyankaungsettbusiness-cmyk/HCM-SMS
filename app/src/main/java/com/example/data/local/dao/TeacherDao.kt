package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {

    @Query("SELECT * FROM teachers WHERE isDeleted = 0 ORDER BY teacherCode ASC")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE isDeleted = 0 ORDER BY teacherCode ASC")
    suspend fun getAllTeachersList(): List<TeacherEntity>

    @Query("SELECT * FROM teachers ORDER BY teacherCode ASC")
    suspend fun getAllTeachersIncludingDeleted(): List<TeacherEntity>

    @Query("SELECT * FROM teachers WHERE isDeleted = 1 ORDER BY teacherCode ASC")
    suspend fun getSoftDeletedTeachers(): List<TeacherEntity>

    @Query("SELECT * FROM teachers WHERE isDirty = 1")
    suspend fun getTeachersForSync(): List<TeacherEntity>

    @Query("SELECT * FROM teachers WHERE uuid = :uuid LIMIT 1")
    suspend fun getTeacherByUuid(uuid: String): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE teacherCode = :teacherCode LIMIT 1")
    suspend fun getTeacherByCode(teacherCode: String): TeacherEntity?

    @Query("UPDATE teachers SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markTeacherSynced(id: Long, uuid: String)

    @Query("SELECT * FROM teachers WHERE id = :id")
    suspend fun getTeacherById(id: Long): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE isDeleted = 0 AND (fullName LIKE '%' || :query || '%' OR teacherCode LIKE '%' || :query || '%' OR assignedSubjects LIKE '%' || :query || '%' OR assignedGrade LIKE '%' || :query || '%') ORDER BY fullName ASC")
    fun searchTeachers(query: String): Flow<List<TeacherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<TeacherEntity>)

    @Update
    suspend fun updateTeacher(teacher: TeacherEntity)

    @Delete
    suspend fun deleteTeacher(teacher: TeacherEntity)

    @Query("UPDATE teachers SET isDeleted = 1, isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTeacher(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE teachers SET isDeleted = 1, isDirty = 1, updatedAt = :timestamp")
    suspend fun softDeleteAllTeachers(timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacherById(id: Long)

    @Query("SELECT COUNT(*) FROM teachers WHERE isDeleted = 0")
    fun getTeacherCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM teachers WHERE isDeleted = 0 AND employmentStatus = 'Active'")
    fun getActiveTeacherCount(): Flow<Int>
}
