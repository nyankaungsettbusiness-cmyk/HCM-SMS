package com.example.ai

import com.example.data.ai.*
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AiSecurityAndDataToolsBridgeTest {

    @Test
    fun testTeacherDenyByDefaultWhenUnconfigured() {
        val unconfiguredTeacherScope = AiPermissionScope(
            username = "new_teacher",
            userRole = UserRole.TEACHER,
            assignedGrades = emptyList(),
            assignedClasses = emptyList(),
            assignedSubjects = emptyList()
        )

        // Strict deny-by-default
        assertFalse(unconfiguredTeacherScope.canAccessGrade("G5"))
        assertFalse(unconfiguredTeacherScope.canAccessClass("Room A"))
        assertFalse(unconfiguredTeacherScope.canAccessSubject("English"))
        assertFalse(unconfiguredTeacherScope.canAccessStudent("G5", "Room A"))
        assertFalse(unconfiguredTeacherScope.canGenerateQuestions("G5", "English"))
    }

    @Test
    fun testTeacherAllowedOnlyAssigned() {
        val teacherScope = AiPermissionScope(
            username = "daw_su",
            userRole = UserRole.TEACHER,
            assignedGrades = listOf("G5", "G6"),
            assignedClasses = listOf("Room A", "Room B"),
            assignedSubjects = listOf("English", "Science")
        )

        // Allowed assigned resources
        assertTrue(teacherScope.canAccessGrade("G5"))
        assertTrue(teacherScope.canAccessGrade("G6"))
        assertTrue(teacherScope.canAccessClass("Room A"))
        assertTrue(teacherScope.canAccessSubject("English"))
        assertTrue(teacherScope.canAccessStudent("G5", "Room A"))
        assertTrue(teacherScope.canGenerateQuestions("G5", "English"))

        // Denied unassigned resources
        assertFalse(teacherScope.canAccessGrade("G7"))
        assertFalse(teacherScope.canAccessClass("Room C"))
        assertFalse(teacherScope.canAccessSubject("Mathematics"))
        assertFalse(teacherScope.canAccessStudent("G7", "Room A"))
        assertFalse(teacherScope.canAccessStudent("G5", "Room C"))
        assertFalse(teacherScope.canGenerateQuestions("G5", "Mathematics"))
    }

    @Test
    fun testAdminAndSuperAdminFullAccess() {
        val adminScope = AiPermissionScope(
            username = "headmaster",
            userRole = UserRole.ADMIN
        )
        val superAdminScope = AiPermissionScope(
            username = "principal",
            userRole = UserRole.SUPER_ADMIN
        )

        listOf(adminScope, superAdminScope).forEach { scope ->
            assertTrue(scope.canAccessGrade("G1"))
            assertTrue(scope.canAccessGrade("G12"))
            assertTrue(scope.canAccessClass("Any Room"))
            assertTrue(scope.canAccessSubject("Any Subject"))
            assertTrue(scope.canAccessStudent("G10", "Room B"))
            assertTrue(scope.canGenerateQuestions("G9", "Physics"))
        }
    }

    @Test
    fun testOfficeStaffRoleRestrictions() {
        val officeStaffScope = AiPermissionScope(
            username = "clerk",
            userRole = UserRole.OFFICE_STAFF
        )

        assertFalse(officeStaffScope.canGenerateQuestions("G5", "English"))
        assertFalse(officeStaffScope.canAccessStudent("G5", "Room A"))
    }

    @Test
    fun testConversationalContextResolverAdminIntents() {
        val teacherProfile = TeacherProfileContext(
            teacherUsername = "admin_user",
            role = UserRole.ADMIN
        )

        val analyticsTurn = ConversationalContextResolver.resolveIntent(
            prompt = "Show school analytics overview for this academic year",
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.ADMIN_SCHOOL_ANALYTICS, analyticsTurn.intentType)

        val attendanceTurn = ConversationalContextResolver.resolveIntent(
            prompt = "Give me the attendance overview for Grade 5",
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.ADMIN_ATTENDANCE_OVERVIEW, attendanceTurn.intentType)
        assertEquals("G5", attendanceTurn.targetGrade)

        val examTurn = ConversationalContextResolver.resolveIntent(
            prompt = "What is the exam performance and pass rate for Grade 6?",
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.ADMIN_EXAM_PERFORMANCE, examTurn.intentType)
        assertEquals("G6", examTurn.targetGrade)

        val atRiskTurn = ConversationalContextResolver.resolveIntent(
            prompt = "List all at-risk students who need intervention",
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.ADMIN_AT_RISK_STUDENTS, atRiskTurn.intentType)

        val policyTurn = ConversationalContextResolver.resolveIntent(
            prompt = "Show the school grading policy and passing marks rules",
            teacherProfile = teacherProfile
        )
        assertEquals(ConversationIntentType.ADMIN_POLICY_OVERVIEW, policyTurn.intentType)
    }

    @Test
    fun testAiDataToolsBridgeAuthoritativeCalculations() = runBlocking {
        // Mock Students
        val students = listOf(
            StudentEntity(id = 1L, studentCode = "ST01", name = "Aung Aung", gender = "Male", dateOfBirth = "2015-01-01", gradeName = "G5", className = "Room A", rollNumber = 1, parentName = "U Mya", phone = "0912345", address = "Yangon"),
            StudentEntity(id = 2L, studentCode = "ST02", name = "Su Su", gender = "Female", dateOfBirth = "2015-02-02", gradeName = "G5", className = "Room A", rollNumber = 2, parentName = "Daw Tin", phone = "0912346", address = "Yangon"),
            StudentEntity(id = 3L, studentCode = "ST03", name = "Min Min", gender = "Male", dateOfBirth = "2014-03-03", gradeName = "G6", className = "Room B", rollNumber = 1, parentName = "U Hla", phone = "0912347", address = "Yangon")
        )

        // Mock Teachers
        val teachers = listOf(
            TeacherEntity(id = 1L, teacherCode = "T01", fullName = "U Ba", email = "uba@school.com", assignedGrade = "G5", assignedClass = "Room A", assignedSubjects = "English"),
            TeacherEntity(id = 2L, teacherCode = "T02", fullName = "Daw Hla", email = "dawhla@school.com", assignedGrade = "G6", assignedClass = "Room B", assignedSubjects = "Science")
        )

        // Mock Marks
        val marks = listOf(
            StudentMarkEntity(id = 1L, assessmentId = 101L, studentId = 1L, studentCode = "ST01", studentName = "Aung Aung", rollNo = 1, subjectName = "English", obtainedMarks = 85.0, maxMarks = 100, passMark = 40, distinctionMark = 75),
            StudentMarkEntity(id = 2L, assessmentId = 101L, studentId = 2L, studentCode = "ST02", studentName = "Su Su", rollNo = 2, subjectName = "English", obtainedMarks = 35.0, maxMarks = 100, passMark = 40, distinctionMark = 75), // Failed
            StudentMarkEntity(id = 3L, assessmentId = 102L, studentId = 3L, studentCode = "ST03", studentName = "Min Min", rollNo = 1, subjectName = "Science", obtainedMarks = 90.0, maxMarks = 100, passMark = 40, distinctionMark = 75)
        )

        // Mock Attendance
        val attendance = listOf(
            AttendanceRecordEntity(id = 1L, academicYear = "2026–2027", date = "2026-09-01", session = AttendanceSession.MORNING, studentId = 1L, studentCode = "ST01", studentName = "Aung Aung", grade = "G5", className = "Room A", status = AttendanceStatus.PRESENT),
            AttendanceRecordEntity(id = 2L, academicYear = "2026–2027", date = "2026-09-01", session = AttendanceSession.MORNING, studentId = 2L, studentCode = "ST02", studentName = "Su Su", grade = "G5", className = "Room A", status = AttendanceStatus.ABSENT),
            AttendanceRecordEntity(id = 3L, academicYear = "2026–2027", date = "2026-09-01", session = AttendanceSession.MORNING, studentId = 3L, studentCode = "ST03", studentName = "Min Min", grade = "G6", className = "Room B", status = AttendanceStatus.PRESENT)
        )

        val fakeStudentDao = object : StudentDao {
            override fun getAllStudents(): Flow<List<StudentEntity>> = flowOf(students)
            override suspend fun getAllStudentsList(): List<StudentEntity> = students
            override suspend fun getAllStudentsIncludingDeleted(): List<StudentEntity> = students
            override suspend fun getSoftDeletedStudents(): List<StudentEntity> = emptyList()
            override suspend fun getStudentsForSync(): List<StudentEntity> = emptyList()
            override suspend fun getStudentByUuid(uuid: String): StudentEntity? = null
            override suspend fun getStudentByCode(studentCode: String): StudentEntity? = null
            override suspend fun markStudentSynced(id: Long, uuid: String) {}
            override suspend fun getStudentById(id: Long): StudentEntity? = students.find { it.id == id }
            override fun getStudentsByGrade(gradeName: String): Flow<List<StudentEntity>> = flowOf(students.filter { it.gradeName == gradeName })
            override fun getStudentsByGradeAndClass(gradeName: String, className: String): Flow<List<StudentEntity>> = flowOf(students.filter { it.gradeName == gradeName && it.className == className })
            override fun searchStudents(query: String): Flow<List<StudentEntity>> = flowOf(emptyList())
            override suspend fun insertStudent(student: StudentEntity): Long = 0L
            override suspend fun insertStudents(students: List<StudentEntity>) {}
            override suspend fun updateStudent(student: StudentEntity) {}
            override suspend fun softDeleteStudent(studentId: Long, timestamp: Long) {}
            override suspend fun softDeleteAllStudents(timestamp: Long) {}
            override suspend fun deleteStudent(studentId: Long) {}
            override suspend fun deleteAllStudents() {}
        }

        val fakeTeacherDao = object : TeacherDao {
            override fun getAllTeachers(): Flow<List<TeacherEntity>> = flowOf(teachers)
            override suspend fun getAllTeachersList(): List<TeacherEntity> = teachers
            override suspend fun getAllTeachersIncludingDeleted(): List<TeacherEntity> = teachers
            override suspend fun getSoftDeletedTeachers(): List<TeacherEntity> = emptyList()
            override suspend fun getTeachersForSync(): List<TeacherEntity> = emptyList()
            override suspend fun getTeacherByUuid(uuid: String): TeacherEntity? = null
            override suspend fun getTeacherByCode(teacherCode: String): TeacherEntity? = teachers.find { it.teacherCode == teacherCode }
            override suspend fun markTeacherSynced(id: Long, uuid: String) {}
            override suspend fun getTeacherById(id: Long): TeacherEntity? = teachers.find { it.id == id }
            override fun searchTeachers(query: String): Flow<List<TeacherEntity>> = flowOf(emptyList())
            override suspend fun insertTeacher(teacher: TeacherEntity): Long = 0L
            override suspend fun insertTeachers(teachers: List<TeacherEntity>) {}
            override suspend fun updateTeacher(teacher: TeacherEntity) {}
            override suspend fun deleteTeacher(teacher: TeacherEntity) {}
            override suspend fun softDeleteTeacher(id: Long, timestamp: Long) {}
            override suspend fun softDeleteAllTeachers(timestamp: Long) {}
            override suspend fun deleteTeacherById(id: Long) {}
            override fun getTeacherCount(): Flow<Int> = flowOf(teachers.size)
            override fun getActiveTeacherCount(): Flow<Int> = flowOf(teachers.size)
        }

        val fakeMarksDao = object : MarksDao {
            override fun getAllMarks(): Flow<List<StudentMarkEntity>> = flowOf(marks)
            override fun getMarksForAssessment(assessmentId: Long): Flow<List<StudentMarkEntity>> = flowOf(marks.filter { it.assessmentId == assessmentId })
            override fun getMarksForAssessmentAndSubject(assessmentId: Long, subjectName: String): Flow<List<StudentMarkEntity>> = flowOf(marks.filter { it.assessmentId == assessmentId && it.subjectName == subjectName })
            override suspend fun insertOrUpdateMark(mark: StudentMarkEntity): Long = 0L
            override suspend fun insertOrUpdateMarks(marks: List<StudentMarkEntity>) {}
            override suspend fun deleteMarksForAssessment(assessmentId: Long) {}
            override fun getLockStatus(assessmentId: Long): Flow<AssessmentLockStatusEntity?> = flowOf(null)
            override suspend fun insertOrUpdateLockStatus(lockStatus: AssessmentLockStatusEntity) {}
            override suspend fun updateLockStatus(assessmentId: Long, isLocked: Boolean, lockedBy: String, lockedAt: Long) {}
            override fun getResultSummaries(assessmentId: Long): Flow<List<AssessmentResultSummaryEntity>> = flowOf(emptyList())
            override suspend fun getAssessmentResultsForSync(limit: Int): List<AssessmentResultSummaryEntity> = emptyList()
            override suspend fun getAssessmentResultByUuid(uuid: String): AssessmentResultSummaryEntity? = null
            override suspend fun markAssessmentResultSynced(id: Long, uuid: String) {}
            override fun getAllAssessmentResults(): Flow<List<AssessmentResultSummaryEntity>> = flowOf(emptyList())
            override suspend fun insertOrUpdateResultSummary(summary: AssessmentResultSummaryEntity) {}
            override suspend fun insertOrUpdateResultSummaries(summaries: List<AssessmentResultSummaryEntity>) {}
            override suspend fun getStudentMarksForSync(limit: Int): List<StudentMarkEntity> = emptyList()
            override suspend fun getStudentMarkByUuid(uuid: String): StudentMarkEntity? = null
            override suspend fun markStudentMarkSynced(id: Long, uuid: String) {}
        }

        val fakeAttendanceDao = object : AttendanceDao {
            override fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>> = flowOf(attendance)
            override fun getAttendanceForDate(date: String): Flow<List<AttendanceRecordEntity>> = flowOf(attendance.filter { it.date == date })
            override fun getAttendanceForStudent(studentId: Long, academicYear: String): Flow<List<AttendanceRecordEntity>> = flowOf(attendance.filter { it.studentId == studentId && it.academicYear == academicYear })
            override suspend fun getAttendanceListForStudentSync(studentId: Long, academicYear: String): List<AttendanceRecordEntity> = attendance.filter { it.studentId == studentId && it.academicYear == academicYear }
            override fun getAttendanceForSession(academicYear: String, date: String, session: AttendanceSession, grade: String, className: String): Flow<List<AttendanceRecordEntity>> = flowOf(emptyList())
            override fun getAttendanceForClass(academicYear: String, grade: String, className: String): Flow<List<AttendanceRecordEntity>> = flowOf(emptyList())
            override suspend fun insertOrUpdateAttendance(records: List<AttendanceRecordEntity>) {}
            override suspend fun insertOrUpdateRecord(record: AttendanceRecordEntity): Long = 0L
            override suspend fun getAttendanceRecordsForSync(limit: Int): List<AttendanceRecordEntity> = emptyList()
            override suspend fun getAttendanceRecordByUuid(uuid: String): AttendanceRecordEntity? = null
            override suspend fun markAttendanceRecordSynced(id: Long, uuid: String) {}
            override suspend fun resetSessionAttendance(academicYear: String, date: String, session: AttendanceSession, grade: String, className: String) {}
            override suspend fun deleteAll() {}
        }

        val fakeAssessmentDao = object : AssessmentDao {
            override fun getAllAssessments(): Flow<List<AssessmentEntity>> = flowOf(emptyList())
            override suspend fun getAllAssessmentsList(): List<AssessmentEntity> = emptyList()
            override suspend fun getAllAssessmentsIncludingDeleted(): List<AssessmentEntity> = emptyList()
            override suspend fun getSoftDeletedAssessments(): List<AssessmentEntity> = emptyList()
            override fun getAssessmentsByAcademicYear(academicYear: String): Flow<List<AssessmentEntity>> = flowOf(emptyList())
            override suspend fun getAssessmentsByAcademicYearList(academicYear: String): List<AssessmentEntity> = emptyList()
            override fun getAssessmentById(id: Long): Flow<AssessmentEntity?> = flowOf(null)
            override suspend fun getAssessmentByIdDirect(id: Long): AssessmentEntity? = null
            override fun getAssessmentsByGrade(grade: String): Flow<List<AssessmentEntity>> = flowOf(emptyList())
            override fun getAssessmentsByGradeAndYear(grade: String, academicYear: String): Flow<List<AssessmentEntity>> = flowOf(emptyList())
            override fun getAssessmentsByStatus(status: AssessmentStatus): Flow<List<AssessmentEntity>> = flowOf(emptyList())
            override fun getAssessmentsByStatusAndYear(status: AssessmentStatus, academicYear: String): Flow<List<AssessmentEntity>> = flowOf(emptyList())
            override suspend fun getAssessmentsForSync(): List<AssessmentEntity> = emptyList()
            override suspend fun getAssessmentByUuid(uuid: String): AssessmentEntity? = null
            override suspend fun markAssessmentSynced(id: Long, uuid: String) {}
            override suspend fun insertAssessment(assessment: AssessmentEntity): Long = 0L
            override suspend fun updateAssessment(assessment: AssessmentEntity) {}
            override suspend fun softDeleteAssessment(id: Long, timestamp: Long) {}
            override suspend fun deleteAssessment(id: Long) {}
            override suspend fun updateAssessmentStatus(id: Long, status: AssessmentStatus) {}
            override suspend fun insertAssessmentSchedule(schedule: AssessmentScheduleEntity): Long = 0L
            override fun getScheduleForAssessment(assessmentId: Long): Flow<AssessmentScheduleEntity?> = flowOf(null)
            override suspend fun insertAssessmentSubject(subject: AssessmentSubjectEntity): Long = 0L
            override fun getSubjectsForAssessment(assessmentId: Long): Flow<List<AssessmentSubjectEntity>> = flowOf(emptyList())
        }

        val fakeHolisticDao = object : HolisticDao {
            override fun getAllHolisticCategories(): Flow<List<HolisticCategoryEntity>> = flowOf(emptyList())
            override suspend fun getHolisticCategoriesForSync(): List<HolisticCategoryEntity> = emptyList()
            override suspend fun getHolisticCategoryByUuid(uuid: String): HolisticCategoryEntity? = null
            override suspend fun markHolisticCategorySynced(id: Long, uuid: String) {}
            override fun getEnabledHolisticCategories(): Flow<List<HolisticCategoryEntity>> = flowOf(emptyList())
            override fun getHolisticCategoriesForLevel(educationLevel: String): Flow<List<HolisticCategoryEntity>> = flowOf(emptyList())
            override fun getEnabledHolisticCategoriesForLevel(educationLevel: String): Flow<List<HolisticCategoryEntity>> = flowOf(emptyList())
            override suspend fun getHolisticCategoriesForLevelList(educationLevel: String): List<HolisticCategoryEntity> = emptyList()
            override suspend fun insertHolisticCategory(category: HolisticCategoryEntity): Long = 0L
            override suspend fun insertHolisticCategories(categories: List<HolisticCategoryEntity>) {}
            override suspend fun updateHolisticCategory(category: HolisticCategoryEntity) {}
            override suspend fun deleteHolisticCategory(category: HolisticCategoryEntity) {}
            override suspend fun deleteHolisticCategoriesForLevel(targetLevel: String) {}
            override fun getHolisticResultsForStudent(studentId: Long, period: String, academicYear: String): Flow<List<HolisticResultEntity>> = flowOf(emptyList())
            override suspend fun getHolisticResultsForSync(limit: Int): List<HolisticResultEntity> = emptyList()
            override suspend fun getHolisticResultByUuid(uuid: String): HolisticResultEntity? = null
            override suspend fun markHolisticResultSynced(id: Long, uuid: String) {}
            override fun getAllHolisticResults(): Flow<List<HolisticResultEntity>> = flowOf(emptyList())
            override suspend fun insertOrUpdateHolisticResults(results: List<HolisticResultEntity>) {}
            override fun getAllSgiCategories(): Flow<List<SgiCategoryEntity>> = flowOf(emptyList())
            override suspend fun getSgiCategoriesForSync(): List<SgiCategoryEntity> = emptyList()
            override suspend fun getSgiCategoryByUuid(uuid: String): SgiCategoryEntity? = null
            override suspend fun markSgiCategorySynced(id: Long, uuid: String) {}
            override fun getEnabledSgiCategories(): Flow<List<SgiCategoryEntity>> = flowOf(emptyList())
            override suspend fun insertSgiCategory(category: SgiCategoryEntity): Long = 0L
            override suspend fun insertSgiCategories(categories: List<SgiCategoryEntity>) {}
            override suspend fun updateSgiCategory(category: SgiCategoryEntity) {}
            override suspend fun deleteSgiCategory(category: SgiCategoryEntity) {}
            override fun getSgiResultsForStudent(studentId: Long, period: String, academicYear: String): Flow<List<SgiResultEntity>> = flowOf(emptyList())
            override suspend fun getSgiResultsForSync(limit: Int): List<SgiResultEntity> = emptyList()
            override suspend fun getSgiResultByUuid(uuid: String): SgiResultEntity? = null
            override suspend fun markSgiResultSynced(id: Long, uuid: String) {}
            override fun getAllSgiResults(): Flow<List<SgiResultEntity>> = flowOf(emptyList())
            override suspend fun insertOrUpdateSgiResults(results: List<SgiResultEntity>) {}
            override fun getAllTeacherComments(): Flow<List<TeacherCommentEntity>> = flowOf(emptyList())
            override suspend fun getTeacherCommentsForSync(limit: Int): List<TeacherCommentEntity> = emptyList()
            override suspend fun getTeacherCommentByUuid(uuid: String): TeacherCommentEntity? = null
            override suspend fun markTeacherCommentSynced(id: Long, uuid: String) {}
            override fun getTeacherCommentForStudent(studentId: Long, period: String, academicYear: String): Flow<TeacherCommentEntity?> = flowOf(null)
            override suspend fun insertOrUpdateTeacherComment(comment: TeacherCommentEntity) {}
        }

        val fakeSchoolPolicyDao = object : SchoolPolicyDao {
            override fun getSchoolSettings(): Flow<SchoolSettingEntity?> = flowOf(null)
            override suspend fun getSchoolSettingsSync(): SchoolSettingEntity? = null
            override suspend fun getSchoolSettingsForSync(): List<SchoolSettingEntity> = emptyList()
            override suspend fun getSchoolSettingByUuid(uuid: String): SchoolSettingEntity? = null
            override suspend fun markSchoolSettingSynced(id: Int, uuid: String) {}
            override suspend fun updateSchoolSettings(settings: SchoolSettingEntity) {}
            override fun getAllGrades(): Flow<List<GradeEntity>> = flowOf(emptyList())
            override suspend fun getAllGradesSync(): List<GradeEntity> = emptyList()
            override suspend fun getGradesForSync(): List<GradeEntity> = emptyList()
            override suspend fun getGradeByUuid(uuid: String): GradeEntity? = null
            override suspend fun getGradeByName(gradeName: String): GradeEntity? = null
            override suspend fun markGradeSynced(id: Long, uuid: String) {}
            override suspend fun insertGrade(grade: GradeEntity): Long = 0L
            override suspend fun deleteGrade(gradeId: Long) {}
            override fun getClassesForGrade(gradeId: Long): Flow<List<SchoolClassEntity>> = flowOf(emptyList())
            override suspend fun getClassesForGradeSync(gradeId: Long): List<SchoolClassEntity> = emptyList()
            override fun getAllClasses(): Flow<List<SchoolClassEntity>> = flowOf(emptyList())
            override suspend fun getAllClassesSync(): List<SchoolClassEntity> = emptyList()
            override suspend fun getClassesForSync(): List<SchoolClassEntity> = emptyList()
            override suspend fun getClassByUuid(uuid: String): SchoolClassEntity? = null
            override suspend fun markClassSynced(id: Long, uuid: String) {}
            override suspend fun insertClass(schoolClass: SchoolClassEntity): Long = 0L
            override suspend fun deleteClass(classId: Long) {}
            override fun getAllSubjects(): Flow<List<SubjectEntity>> = flowOf(emptyList())
            override fun getSubjectsByCategory(category: SubjectCategory): Flow<List<SubjectEntity>> = flowOf(emptyList())
            override suspend fun getSubjectsForSync(): List<SubjectEntity> = emptyList()
            override suspend fun getSubjectByUuid(uuid: String): SubjectEntity? = null
            override suspend fun getSubjectByName(name: String): SubjectEntity? = null
            override suspend fun getSubjectByNameAndLevel(name: String, level: EducationLevel): SubjectEntity? = null
            override suspend fun getActiveSubjectByNameAndLevel(name: String, level: EducationLevel): SubjectEntity? = null
            override suspend fun getSubjectById(id: Long): SubjectEntity? = null
            override suspend fun markSubjectSynced(id: Long, uuid: String) {}
            override suspend fun insertSubject(subject: SubjectEntity): Long = 0L
            override suspend fun updateSubject(subject: SubjectEntity) {}
            override suspend fun softDeleteSubject(subjectId: Long, updatedAt: Long) {}
            override suspend fun deleteSubject(subjectId: Long) {}
            override suspend fun getGradeSubjectCrossRefs(): List<GradeSubjectCrossRef> = emptyList()
            override suspend fun insertGradeSubjectCrossRefs(refs: List<GradeSubjectCrossRef>) {}
            override fun getAllAssessmentTypes(): Flow<List<AssessmentTypeEntity>> = flowOf(emptyList())
            override suspend fun getAssessmentTypesForSync(): List<AssessmentTypeEntity> = emptyList()
            override suspend fun getAssessmentTypeByUuid(uuid: String): AssessmentTypeEntity? = null
            override suspend fun markAssessmentTypeSynced(id: Long, uuid: String) {}
            override suspend fun insertAssessmentType(type: AssessmentTypeEntity): Long = 0L
            override suspend fun deleteAssessmentType(id: Long) {}
            override fun getAllCustomExams(): Flow<List<CustomExamEntity>> = flowOf(emptyList())
            override suspend fun getCustomExamsForSync(): List<CustomExamEntity> = emptyList()
            override suspend fun getCustomExamByUuid(uuid: String): CustomExamEntity? = null
            override suspend fun markCustomExamSynced(id: Long, uuid: String) {}
            override suspend fun insertCustomExam(exam: CustomExamEntity): Long = 0L
            override suspend fun deleteCustomExam(id: Long) {}
            override fun getAllGradingPolicies(): Flow<List<GradingPolicyEntity>> = flowOf(emptyList())
            override suspend fun getGradingPoliciesForSync(): List<GradingPolicyEntity> = emptyList()
            override suspend fun getGradingPolicyByUuid(uuid: String): GradingPolicyEntity? = null
            override suspend fun markGradingPolicySynced(id: Long, uuid: String) {}
            override suspend fun insertGradingPolicy(policy: GradingPolicyEntity): Long = 0L
            override suspend fun updateGradingPolicy(policy: GradingPolicyEntity) {}
            override suspend fun deleteGradingPolicy(policyId: Long) {}
        }

        val fakeCurriculumDao = object : CurriculumKnowledgeDao {
            override fun getAllDocumentsFlow(): Flow<List<CurriculumDocumentEntity>> = flowOf(emptyList())
            override suspend fun getDocumentsByGradeAndSubject(gradeLevel: String, subject: String): List<CurriculumDocumentEntity> = emptyList()
            override suspend fun getDocumentById(id: Long): CurriculumDocumentEntity? = null
            override suspend fun insertDocument(document: CurriculumDocumentEntity): Long = 0L
            override suspend fun insertDocuments(documents: List<CurriculumDocumentEntity>): List<Long> = emptyList()
            override suspend fun deleteDocument(document: CurriculumDocumentEntity) {}
            override fun getChunksByGradeAndSubjectFlow(gradeLevel: String, subject: String): Flow<List<CurriculumChunkEntity>> = flowOf(emptyList())
            override suspend fun getChunksByGradeAndSubject(gradeLevel: String, subject: String): List<CurriculumChunkEntity> = emptyList()
            override suspend fun getChunksForUnit(gradeLevel: String, subject: String, chapterUnit: String): List<CurriculumChunkEntity> = emptyList()
            override suspend fun getChunksForScope(gradeLevel: String, subject: String, chapterUnit: String, sectionTopic: String): List<CurriculumChunkEntity> = emptyList()
            override suspend fun getAvailableUnits(gradeLevel: String, subject: String): List<String> = emptyList()
            override suspend fun getAvailableSections(gradeLevel: String, subject: String, chapterUnit: String): List<String> = emptyList()
            override suspend fun searchChunks(gradeLevel: String, subject: String, query: String): List<CurriculumChunkEntity> = emptyList()
            override suspend fun getChunkCount(): Int = 12
            override suspend fun insertChunk(chunk: CurriculumChunkEntity): Long = 0L
            override suspend fun insertChunks(chunks: List<CurriculumChunkEntity>) {}
            override suspend fun deleteChunkById(id: Long) {}
            override suspend fun deleteChunksByDocumentId(documentId: Long) {}
        }

        val bridge = AiDataToolsBridge(
            studentDao = fakeStudentDao,
            teacherDao = fakeTeacherDao,
            marksDao = fakeMarksDao,
            assessmentDao = fakeAssessmentDao,
            holisticDao = fakeHolisticDao,
            attendanceDao = fakeAttendanceDao,
            schoolPolicyDao = fakeSchoolPolicyDao,
            curriculumKnowledgeDao = fakeCurriculumDao
        )

        val adminScope = AiPermissionScope(username = "admin", userRole = UserRole.ADMIN)

        // 1. School Analytics
        val analytics = bridge.getSchoolAnalyticsOverview(adminScope, "2026–2027")
        assertNotNull(analytics)
        assertEquals(3, analytics?.totalStudents)
        assertEquals(2, analytics?.totalTeachers)
        assertEquals(2, analytics?.activeGradesCount)
        // 2 passed (85, 90) out of 3 = 66.67% pass rate
        assertEquals(66.7, analytics!!.overallPassRate, 0.5)
        // 2 present out of 3 = 66.7% attendance rate
        assertEquals(66.7, analytics.overallAttendanceRate, 0.5)

        // 2. Exam Performance
        val examSummary = bridge.getExamPerformanceOverview(adminScope, "2026–2027")
        assertNotNull(examSummary)
        assertEquals(3, examSummary?.totalStudentsEvaluated)
        assertEquals(2, examSummary?.distinctionCount) // 85 and 90

        // 3. At Risk Students
        val atRiskStudents = bridge.getAtRiskStudents(adminScope, "2026–2027")
        assertEquals(1, atRiskStudents.size)
        assertEquals("Su Su", atRiskStudents.first().studentName)
        assertEquals(35.0, atRiskStudents.first().averageScorePercentage, 0.1)
        assertTrue(atRiskStudents.first().primaryRiskFactor.contains("Academic"))

        // 4. Teacher Scope Isolation
        val teacherScopeG6 = AiPermissionScope(
            username = "daw_hla",
            userRole = UserRole.TEACHER,
            assignedGrades = listOf("G6"),
            assignedClasses = listOf("Room B"),
            assignedSubjects = listOf("Science")
        )
        val g6AtRisk = bridge.getAtRiskStudents(teacherScopeG6, "2026–2027")
        // Min Min has 90% and 100% attendance, so 0 at-risk students in G6
        assertEquals(0, g6AtRisk.size)
    }
}
