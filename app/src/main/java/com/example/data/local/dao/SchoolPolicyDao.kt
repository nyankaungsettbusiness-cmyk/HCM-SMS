package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolPolicyDao {
    // School Settings
    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    fun getSchoolSettings(): Flow<SchoolSettingEntity?>

    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    suspend fun getSchoolSettingsSync(): SchoolSettingEntity?

    @Query("SELECT * FROM school_settings WHERE isDirty = 1")
    suspend fun getSchoolSettingsForSync(): List<SchoolSettingEntity>

    @Query("SELECT * FROM school_settings WHERE uuid = :uuid LIMIT 1")
    suspend fun getSchoolSettingByUuid(uuid: String): SchoolSettingEntity?

    @Query("UPDATE school_settings SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markSchoolSettingSynced(id: Int, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSchoolSettings(settings: SchoolSettingEntity)

    // Grades
    @Query("SELECT * FROM grades ORDER BY id ASC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades ORDER BY id ASC")
    suspend fun getAllGradesSync(): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE isDirty = 1")
    suspend fun getGradesForSync(): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE uuid = :uuid LIMIT 1")
    suspend fun getGradeByUuid(uuid: String): GradeEntity?

    @Query("SELECT * FROM grades WHERE gradeName = :gradeName LIMIT 1")
    suspend fun getGradeByName(gradeName: String): GradeEntity?

    @Query("UPDATE grades SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markGradeSynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Query("DELETE FROM grades WHERE id = :gradeId")
    suspend fun deleteGrade(gradeId: Long)

    // School Classes
    @Query("SELECT * FROM school_classes WHERE gradeId = :gradeId ORDER BY className ASC")
    fun getClassesForGrade(gradeId: Long): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM school_classes WHERE gradeId = :gradeId ORDER BY className ASC")
    suspend fun getClassesForGradeSync(gradeId: Long): List<SchoolClassEntity>

    @Query("SELECT * FROM school_classes ORDER BY id ASC")
    fun getAllClasses(): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM school_classes ORDER BY id ASC")
    suspend fun getAllClassesSync(): List<SchoolClassEntity>

    @Query("SELECT * FROM school_classes WHERE isDirty = 1")
    suspend fun getClassesForSync(): List<SchoolClassEntity>

    @Query("SELECT * FROM school_classes WHERE uuid = :uuid LIMIT 1")
    suspend fun getClassByUuid(uuid: String): SchoolClassEntity?

    @Query("UPDATE school_classes SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markClassSynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClassEntity): Long

    @Query("DELETE FROM school_classes WHERE id = :classId")
    suspend fun deleteClass(classId: Long)

    // Subjects
    @Query("SELECT * FROM subjects WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE category = :category AND isDeleted = 0 ORDER BY id ASC")
    fun getSubjectsByCategory(category: SubjectCategory): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isDirty = 1")
    suspend fun getSubjectsForSync(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE uuid = :uuid LIMIT 1")
    suspend fun getSubjectByUuid(uuid: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun getSubjectByName(name: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name AND educationLevel = :level LIMIT 1")
    suspend fun getSubjectByNameAndLevel(name: String, level: EducationLevel): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name AND educationLevel = :level AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveSubjectByNameAndLevel(name: String, level: EducationLevel): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("UPDATE subjects SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markSubjectSynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET isDeleted = 1, isDirty = 1, updatedAt = :updatedAt WHERE id = :subjectId")
    suspend fun softDeleteSubject(subjectId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubject(subjectId: Long)

    // Grade Subject Cross Ref
    @Query("SELECT * FROM grade_subject_cross_ref")
    suspend fun getGradeSubjectCrossRefs(): List<GradeSubjectCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeSubjectCrossRefs(refs: List<GradeSubjectCrossRef>)

    // Assessment Types
    @Query("SELECT * FROM assessment_types ORDER BY id ASC")
    fun getAllAssessmentTypes(): Flow<List<AssessmentTypeEntity>>

    @Query("SELECT * FROM assessment_types WHERE isDirty = 1")
    suspend fun getAssessmentTypesForSync(): List<AssessmentTypeEntity>

    @Query("SELECT * FROM assessment_types WHERE uuid = :uuid LIMIT 1")
    suspend fun getAssessmentTypeByUuid(uuid: String): AssessmentTypeEntity?

    @Query("UPDATE assessment_types SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markAssessmentTypeSynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentType(type: AssessmentTypeEntity): Long

    @Query("DELETE FROM assessment_types WHERE id = :id")
    suspend fun deleteAssessmentType(id: Long)

    // Custom Exams
    @Query("SELECT * FROM custom_exams ORDER BY id ASC")
    fun getAllCustomExams(): Flow<List<CustomExamEntity>>

    @Query("SELECT * FROM custom_exams WHERE isDirty = 1")
    suspend fun getCustomExamsForSync(): List<CustomExamEntity>

    @Query("SELECT * FROM custom_exams WHERE uuid = :uuid LIMIT 1")
    suspend fun getCustomExamByUuid(uuid: String): CustomExamEntity?

    @Query("UPDATE custom_exams SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markCustomExamSynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomExam(exam: CustomExamEntity): Long

    @Query("DELETE FROM custom_exams WHERE id = :id")
    suspend fun deleteCustomExam(id: Long)

    // Grading Policies
    @Query("SELECT * FROM grading_policies ORDER BY id ASC")
    fun getAllGradingPolicies(): Flow<List<GradingPolicyEntity>>

    @Query("SELECT * FROM grading_policies WHERE isDirty = 1")
    suspend fun getGradingPoliciesForSync(): List<GradingPolicyEntity>

    @Query("SELECT * FROM grading_policies WHERE uuid = :uuid LIMIT 1")
    suspend fun getGradingPolicyByUuid(uuid: String): GradingPolicyEntity?

    @Query("UPDATE grading_policies SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markGradingPolicySynced(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradingPolicy(policy: GradingPolicyEntity): Long

    @Update
    suspend fun updateGradingPolicy(policy: GradingPolicyEntity)

    @Query("DELETE FROM grading_policies WHERE id = :id")
    suspend fun deleteGradingPolicy(id: Long)
}
