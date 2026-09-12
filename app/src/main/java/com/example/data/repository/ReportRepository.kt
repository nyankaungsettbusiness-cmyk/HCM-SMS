package com.example.data.repository

import com.example.data.local.dao.AcademicYearDao
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.HolisticDao
import com.example.data.local.dao.MarksDao
import com.example.data.local.dao.ReportDao
import com.example.data.local.dao.SchoolPolicyDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.ReportGenerationHistoryEntity
import com.example.data.local.entity.ReportSettingEntity
import com.example.data.local.entity.StudentAcademicHistoryEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.report.GeneratedReportCardData
import com.example.data.report.engine.ReportGeneratorEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ReportRepository(
    private val reportDao: ReportDao,
    private val studentDao: StudentDao,
    private val marksDao: MarksDao,
    private val assessmentDao: AssessmentDao,
    private val attendanceDao: AttendanceDao,
    private val holisticDao: HolisticDao,
    private val schoolPolicyDao: SchoolPolicyDao,
    private val academicYearDao: AcademicYearDao? = null
) {
    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents().map { list ->
        list.distinctBy { if (it.studentCode.isNotBlank()) it.studentCode else "${it.name}_${it.gradeName}_${it.className}_${it.id}" }
    }
    val allAcademicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearDao?.getAllStudentAcademicHistories()
        ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allGrades: Flow<List<GradeEntity>> = schoolPolicyDao.getAllGrades()
    val reportSettings: Flow<ReportSettingEntity?> = reportDao.getReportSettingsFlow()
    val generationHistory: Flow<List<ReportGenerationHistoryEntity>> = reportDao.getAllGenerationHistory()

    suspend fun ensureG4Student() {
        // No-op: Do not resurrect or inject hardcoded demo students
    }

    fun generateReportCard(
        studentId: Long,
        selectedMonth: String,
        academicYear: String
    ): Flow<GeneratedReportCardData?> {
        val studentFlow = allStudents.map { list -> list.find { it.id == studentId } }
        val assessmentsFlow = assessmentDao.getAssessmentsByAcademicYear(academicYear)
        val studentMarksFlow = marksDao.getAllMarks()
        val attendanceFlow = attendanceDao.getAttendanceForStudent(studentId, academicYear)
        val holisticCategoriesFlow = holisticDao.getAllHolisticCategories()
        val holisticResultsFlow = holisticDao.getAllHolisticResults()
        val sgiResultsFlow = holisticDao.getAllSgiResults()
        val teacherCommentFlow = holisticDao.getTeacherCommentForStudent(studentId, selectedMonth, academicYear)

        return combine(
            studentFlow,
            assessmentsFlow,
            studentMarksFlow,
            attendanceFlow,
            holisticCategoriesFlow,
            holisticResultsFlow,
            sgiResultsFlow,
            teacherCommentFlow,
            allStudents
        ) { array ->
            val student = array[0] as StudentEntity? ?: return@combine null
            @Suppress("UNCHECKED_CAST")
            val assessments = array[1] as List<com.example.data.local.entity.AssessmentEntity>
            @Suppress("UNCHECKED_CAST")
            val marks = array[2] as List<com.example.data.local.entity.StudentMarkEntity>
            @Suppress("UNCHECKED_CAST")
            val attendance = array[3] as List<com.example.data.local.entity.AttendanceRecordEntity>
            @Suppress("UNCHECKED_CAST")
            val categories = array[4] as List<com.example.data.local.entity.HolisticCategoryEntity>
            @Suppress("UNCHECKED_CAST")
            val hResults = array[5] as List<com.example.data.local.entity.HolisticResultEntity>
            @Suppress("UNCHECKED_CAST")
            val sgiResults = array[6] as List<com.example.data.local.entity.SgiResultEntity>
            val comment = array[7] as com.example.data.local.entity.TeacherCommentEntity?
            @Suppress("UNCHECKED_CAST")
            val studentsList = array[8] as List<StudentEntity>

            ReportGeneratorEngine.generateReportCard(
                student = student,
                selectedMonth = selectedMonth,
                academicYear = academicYear,
                allAssessments = assessments,
                studentMarks = marks,
                attendanceRecords = attendance,
                holisticCategories = categories,
                holisticResults = hResults,
                sgiResults = sgiResults,
                teacherComment = comment,
                allStudents = studentsList
            )
        }
    }

    suspend fun saveGenerationLog(history: ReportGenerationHistoryEntity) {
        reportDao.insertGenerationHistory(history)
    }

    suspend fun saveReportSettings(settings: ReportSettingEntity) {
        reportDao.saveReportSettings(settings)
    }

    suspend fun saveTeacherComment(comment: com.example.data.local.entity.TeacherCommentEntity) {
        holisticDao.insertOrUpdateTeacherComment(comment)
    }

    suspend fun getExistingTeacherComment(studentId: Long, period: String, academicYear: String): com.example.data.local.entity.TeacherCommentEntity? {
        return holisticDao.getTeacherCommentForStudent(studentId, period, academicYear).firstOrNull()
    }

    fun getStudentFactsAggregator(): com.example.data.ai.StudentFactsAggregator {
        return com.example.data.ai.StudentFactsAggregator(
            studentDao = studentDao,
            assessmentDao = assessmentDao,
            marksDao = marksDao,
            holisticDao = holisticDao,
            attendanceDao = attendanceDao,
            schoolPolicyDao = schoolPolicyDao
        )
    }
}
