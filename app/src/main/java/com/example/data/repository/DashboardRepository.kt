package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class DashboardRepository(
    val studentDao: StudentDao,
    val teacherDao: TeacherDao,
    val attendanceDao: AttendanceDao,
    val marksDao: MarksDao,
    val assessmentDao: AssessmentDao,
    val holisticDao: HolisticDao,
    val reportDao: ReportDao,
    val academicYearDao: AcademicYearDao,
    val userDao: UserDao,
    val schoolPolicyDao: SchoolPolicyDao
) {
    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents()
    val allTeachers: Flow<List<TeacherEntity>> = teacherDao.getAllTeachers()
    val totalTeachers: Flow<Int> = teacherDao.getTeacherCount()
    val activeTeachers: Flow<Int> = teacherDao.getActiveTeacherCount()
    val allAttendance: Flow<List<AttendanceRecordEntity>> = attendanceDao.getAllAttendanceRecords()
    val allMarks: Flow<List<StudentMarkEntity>> = marksDao.getAllMarks()
    val allAssessments: Flow<List<AssessmentEntity>> = assessmentDao.getAllAssessments()
    val allHolisticResults: Flow<List<HolisticResultEntity>> = holisticDao.getAllHolisticResults()
    val allSgiResults: Flow<List<SgiResultEntity>> = holisticDao.getAllSgiResults()
    val allReportGenerations: Flow<List<ReportGenerationHistoryEntity>> = reportDao.getAllGenerationHistory()
    val allPdfExports: Flow<List<PdfExportHistoryEntity>> = reportDao.getAllExportHistory()
    val allPrintLogs: Flow<List<PrintHistoryEntity>> = reportDao.getAllPrintHistory()
    val allAcademicYears: Flow<List<AcademicYearEntity>> = academicYearDao.getAllAcademicYears()
    val activeAcademicYear: Flow<AcademicYearEntity?> = academicYearDao.getActiveAcademicYear()
    val allStudentAcademicHistories: Flow<List<StudentAcademicHistoryEntity>> = academicYearDao.getAllStudentAcademicHistories()
    val allPromotions: Flow<List<PromotionHistoryEntity>> = academicYearDao.getAllPromotionHistory()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = userDao.getRecentAuditLogs()
    val allGrades: Flow<List<GradeEntity>> = schoolPolicyDao.getAllGrades()
    val allSubjects: Flow<List<SubjectEntity>> = schoolPolicyDao.getAllSubjects()
}

