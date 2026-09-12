package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AiDao
import com.example.data.local.dao.AiChatDao
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.AssessmentPeriodDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AcademicYearDao
import com.example.data.local.dao.CurriculumKnowledgeDao
import com.example.data.local.dao.HolisticDao
import com.example.data.local.dao.MarksDao
import com.example.data.local.dao.ReportDao
import com.example.data.local.dao.SchoolPolicyDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.dao.TeacherDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.*
import com.example.data.ai.CurriculumKnowledgeSeeder
import com.example.util.PasswordHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        RolePermissionEntity::class,
        SchoolSettingEntity::class,
        GradeEntity::class,
        SchoolClassEntity::class,
        SubjectEntity::class,
        GradeSubjectCrossRef::class,
        AssessmentTypeEntity::class,
        CustomExamEntity::class,
        GradingPolicyEntity::class,
        StudentEntity::class,
        TeacherEntity::class,
        AuditLogEntity::class,
        AssessmentEntity::class,
        AssessmentScheduleEntity::class,
        AssessmentSubjectEntity::class,
        StudentMarkEntity::class,
        AssessmentResultSummaryEntity::class,
        AssessmentLockStatusEntity::class,
        HolisticCategoryEntity::class,
        HolisticResultEntity::class,
        SgiCategoryEntity::class,
        SgiResultEntity::class,
        TeacherCommentEntity::class,
        AssessmentPeriodEntity::class,
        ReportTemplateEntity::class,
        ReportSettingEntity::class,
        ReportGenerationHistoryEntity::class,
        PdfExportHistoryEntity::class,
        PrintHistoryEntity::class,
        AttendanceRecordEntity::class,
        AcademicYearEntity::class,
        PromotionHistoryEntity::class,
        StudentAcademicHistoryEntity::class,
        AiHistoryEntity::class,
        AiSavedQuestionEntity::class,
        AiSettingEntity::class,
        CurriculumDocumentEntity::class,
        CurriculumChunkEntity::class,
        AiChatSessionEntity::class,
        AiChatMessageEntity::class,
        LoginHistoryEntity::class,
        SecurityPolicyEntity::class,
        SyncOutboxEntity::class,
        SyncMetadataEntity::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun schoolPolicyDao(): SchoolPolicyDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao
    abstract fun assessmentDao(): AssessmentDao

    abstract fun marksDao(): MarksDao
    abstract fun holisticDao(): HolisticDao
    abstract fun assessmentPeriodDao(): AssessmentPeriodDao
    abstract fun reportDao(): ReportDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun academicYearDao(): AcademicYearDao
    abstract fun aiDao(): AiDao
    abstract fun curriculumKnowledgeDao(): CurriculumKnowledgeDao
    abstract fun aiChatDao(): AiChatDao
    abstract fun syncOutboxDao(): com.example.data.local.dao.SyncOutboxDao
    abstract fun syncMetadataDao(): com.example.data.local.dao.SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // users
                db.execSQL("ALTER TABLE users ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")

                // school_settings
                db.execSQL("ALTER TABLE school_settings ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE school_settings ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE school_settings ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE school_settings ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // grades
                db.execSQL("ALTER TABLE grades ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE grades ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grades ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grades ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // school_classes
                db.execSQL("ALTER TABLE school_classes ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE school_classes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE school_classes ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE school_classes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // subjects
                db.execSQL("ALTER TABLE subjects ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE subjects ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subjects ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subjects ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // assessment_types
                db.execSQL("ALTER TABLE assessment_types ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assessment_types ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessment_types ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessment_types ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // custom_exams
                db.execSQL("ALTER TABLE custom_exams ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE custom_exams ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE custom_exams ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE custom_exams ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // grading_policies
                db.execSQL("ALTER TABLE grading_policies ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE grading_policies ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grading_policies ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grading_policies ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // students
                db.execSQL("ALTER TABLE students ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE students ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE students ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE students ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE students ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // teachers
                db.execSQL("ALTER TABLE teachers ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE teachers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE teachers ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE teachers ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // assessments
                db.execSQL("ALTER TABLE assessments ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assessments ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessments ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // student_marks
                db.execSQL("ALTER TABLE student_marks ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE student_marks ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE student_marks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // assessment_results
                db.execSQL("ALTER TABLE assessment_results ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assessment_results ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessment_results ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // holistic_categories
                db.execSQL("ALTER TABLE holistic_categories ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE holistic_categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE holistic_categories ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE holistic_categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // holistic_results
                db.execSQL("ALTER TABLE holistic_results ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE holistic_results ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE holistic_results ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // sgi_categories
                db.execSQL("ALTER TABLE sgi_categories ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sgi_categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sgi_categories ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sgi_categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // sgi_results
                db.execSQL("ALTER TABLE sgi_results ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sgi_results ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sgi_results ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // teacher_comments
                db.execSQL("ALTER TABLE teacher_comments ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE teacher_comments ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE teacher_comments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // assessment_periods
                db.execSQL("ALTER TABLE assessment_periods ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assessment_periods ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessment_periods ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assessment_periods ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // attendance_records
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // academic_years
                db.execSQL("ALTER TABLE academic_years ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE academic_years ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE academic_years ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE academic_years ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                // promotion_history
                db.execSQL("ALTER TABLE promotion_history ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promotion_history ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promotion_history ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promotion_history ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. curriculum_documents
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS curriculum_documents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        gradeLevel TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        documentType TEXT NOT NULL,
                        authorOrPublisher TEXT NOT NULL DEFAULT 'Ministry of Education / School',
                        isApproved INTEGER NOT NULL DEFAULT 1,
                        isSystemDefault INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 2. curriculum_chunks
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS curriculum_chunks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL DEFAULT 0,
                        gradeLevel TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        chapterUnit TEXT NOT NULL,
                        sectionTopic TEXT NOT NULL,
                        pageRange TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL,
                        learningObjectives TEXT NOT NULL DEFAULT '',
                        vocabularyWords TEXT NOT NULL DEFAULT '',
                        keywords TEXT NOT NULL DEFAULT '',
                        sourceReference TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_curriculum_chunks_gradeLevel_subject ON curriculum_chunks(gradeLevel, subject)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_curriculum_chunks_chapterUnit ON curriculum_chunks(chapterUnit)")

                // 3. ai_chat_sessions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_chat_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionTitle TEXT NOT NULL,
                        userRole TEXT NOT NULL,
                        teacherUsername TEXT NOT NULL,
                        academicYear TEXT NOT NULL DEFAULT '2026-2027',
                        grade TEXT NOT NULL DEFAULT 'All',
                        subject TEXT NOT NULL DEFAULT 'General',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 4. ai_chat_messages
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        senderRole TEXT NOT NULL,
                        content TEXT NOT NULL,
                        messageType TEXT NOT NULL DEFAULT 'TEXT',
                        metadataJson TEXT NOT NULL DEFAULT '{}',
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_chat_messages_sessionId ON ai_chat_messages(sessionId)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN managedPassword TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hcm_sms_database.db"
                )
                    .addMigrations(MIGRATION_15_16, MIGRATION_17_18, MIGRATION_19_20)
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                populateInitialData(database)
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                populateInitialData(database)
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                try {
                    // Seed curriculum chunks if empty
                    CurriculumKnowledgeSeeder.seedCurriculumIfEmpty(database.curriculumKnowledgeDao())

                    // 1. Fix and normalize High School elective subjects in local SQLite
                    db.execSQL("UPDATE subjects SET name = 'Biology', subTrack = 'STEAMS-1' WHERE (name LIKE 'Biology%' OR name = 'Biology / Computer Science') AND educationLevel = 'HIGH_SCHOOL'")
                    db.execSQL("UPDATE subjects SET name = 'Economics', subTrack = 'STEAMS-2' WHERE (name LIKE '%Economics%' OR name LIKE '%History & Economics%' OR name LIKE '%Economics / History%' OR name LIKE '%Economics/History%') AND educationLevel = 'HIGH_SCHOOL'")

                    // 2. Migrate existing student marks so previously entered marks for Biology or Economics are preserved and visible
                    db.execSQL("UPDATE student_marks SET subjectName = 'Biology' WHERE subjectName LIKE 'Biology%' OR subjectName = 'Biology / Computer Science'")
                    db.execSQL("UPDATE student_marks SET subjectName = 'Economics' WHERE subjectName LIKE '%Economics%' OR subjectName LIKE '%History & Economics%' OR subjectName LIKE '%Economics / History%' OR subjectName LIKE '%Economics/History%'")

                    // 3. Migrate custom exam / assessment subjects
                    db.execSQL("UPDATE assessment_subjects SET subjectName = 'Biology' WHERE subjectName LIKE 'Biology%' OR subjectName = 'Biology / Computer Science'")
                    db.execSQL("UPDATE assessment_subjects SET subjectName = 'Economics' WHERE subjectName LIKE '%Economics%' OR subjectName LIKE '%History & Economics%' OR subjectName LIKE '%Economics / History%' OR subjectName LIKE '%Economics/History%'")

                    val policyDao = database.schoolPolicyDao()
                    if (policyDao.getSchoolSettings() == null) {
                        populateInitialData(database)
                    } else {
                        // Ensure default report settings exist
                        val reportDao = database.reportDao()
                        if (reportDao.getReportSettings() == null) {
                            reportDao.saveReportSettings(ReportSettingEntity())
                        }
                        // Ensure default report templates exist
                        ensureDefaultReportTemplates(reportDao)
                    }

                    // Auto-seed initial default admin ONLY if users table is completely empty
                    val userDao = database.userDao()
                    val allUsers = userDao.getAllUsersList()
                    if (allUsers.isEmpty()) {
                        seedDefaultAdminUsers(userDao)
                    }

                    // Auto-assign clean UUIDs to any pre-seeded system rows so they never appear as pending changes
                    try {
                        val dbWriter = database.openHelper.writableDatabase
                        dbWriter.execSQL("UPDATE grades SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE school_classes SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE subjects SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE school_settings SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE academic_years SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE assessment_types SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE grading_policies SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE custom_exams SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE assessment_periods SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE holistic_categories SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE sgi_categories SET uuid = lower(hex(randomblob(16))) WHERE uuid = '' OR uuid IS NULL")
                        dbWriter.execSQL("UPDATE grades SET isDirty = 0 WHERE isDirty = 1 AND isDeleted = 0")
                        dbWriter.execSQL("UPDATE school_classes SET isDirty = 0 WHERE isDirty = 1 AND isDeleted = 0")
                        dbWriter.execSQL("UPDATE subjects SET isDirty = 0 WHERE isDirty = 1 AND isDeleted = 0")
                        dbWriter.execSQL("UPDATE school_settings SET isDirty = 0 WHERE isDirty = 1")
                        dbWriter.execSQL("UPDATE academic_years SET isDirty = 0 WHERE isDirty = 1")
                        dbWriter.execSQL("UPDATE holistic_categories SET isDirty = 0 WHERE isDirty = 1 AND isDeleted = 0")
                        dbWriter.execSQL("UPDATE sgi_categories SET isDirty = 0 WHERE isDirty = 1 AND isDeleted = 0")
                        dbWriter.execSQL("UPDATE users SET isDirty = 0 WHERE isDirty = 1 AND isDeleted = 0")
                    } catch (sqlEx: Exception) {
                        // Safe ignore if tables are in creation
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun populateInitialData(db: AppDatabase) {
            val userDao = db.userDao()
            val policyDao = db.schoolPolicyDao()
            val studentDao = db.studentDao()

            // 1. Default Permissions Matrix for 15 Modules x 6 Actions
            val permissionsList = mutableListOf<RolePermissionEntity>()
            val actions = listOf("VIEW", "CREATE", "EDIT", "DELETE", "PRINT", "EXPORT")
            val modules = listOf(
                "DASHBOARD", "STUDENTS", "TEACHERS", "ATTENDANCE", "ASSESSMENT",
                "MARKS_ENTRY", "HCM_ASSESSMENT", "SGI", "REPORT_CARDS", "ACADEMIC_YEAR",
                "PROMOTION", "SCHOOL_POLICY", "AI_ASSISTANT", "USER_MANAGEMENT", "SYSTEM_SETTINGS"
            )

            UserRole.values().forEach { role ->
                modules.forEach { module ->
                    actions.forEach { action ->
                        val key = "${module}_${action}"
                        val isAllowed = when (role) {
                            UserRole.SUPER_ADMIN -> true
                            UserRole.ADMIN -> module != "SYSTEM_SETTINGS" || action in listOf("VIEW", "PRINT")
                            UserRole.TEACHER -> action in listOf("VIEW", "CREATE", "EDIT", "PRINT") && module !in listOf("USER_MANAGEMENT", "SYSTEM_SETTINGS", "SCHOOL_POLICY")
                            UserRole.OFFICE_STAFF -> action in listOf("VIEW", "PRINT", "EXPORT") && module !in listOf("USER_MANAGEMENT", "SYSTEM_SETTINGS")
                        }
                        permissionsList.add(RolePermissionEntity(role = role, permissionKey = key, isAllowed = isAllowed))
                    }
                }
            }
            userDao.insertRolePermissions(permissionsList)

            // Security Policy
            userDao.saveSecurityPolicy(
                SecurityPolicyEntity(
                    id = 1,
                    maxFailedAttempts = 5,
                    forcePasswordChangeOnFirstLogin = true,
                    sessionTimeoutMinutes = 30,
                    minPasswordLength = 6,
                    requireSpecialChar = false
                )
            )

            // 2. School Settings
            policyDao.updateSchoolSettings(
                SchoolSettingEntity(
                    id = 1,
                    schoolName = "Hein Chan Myae Private School",
                    academicYear = "2026-2027",
                    contactPhone = "+95 9 790001122",
                    email = "contact@heinchanmyae.edu.mm",
                    address = "No. 123, Pyay Road, Kamayut, Yangon",
                    logoText = "HCM-SMS"
                )
            )

            // 4. Grades & Classes
            val grades = listOf(
                GradeEntity(id = 1, gradeName = "KG", educationLevel = EducationLevel.KINDERGARTEN),
                GradeEntity(id = 2, gradeName = "G1", educationLevel = EducationLevel.PRIMARY),
                GradeEntity(id = 3, gradeName = "G2", educationLevel = EducationLevel.PRIMARY),
                GradeEntity(id = 4, gradeName = "G3", educationLevel = EducationLevel.PRIMARY),
                GradeEntity(id = 5, gradeName = "G4", educationLevel = EducationLevel.PRIMARY),
                GradeEntity(id = 6, gradeName = "G5", educationLevel = EducationLevel.PRIMARY),
                GradeEntity(id = 7, gradeName = "G6", educationLevel = EducationLevel.SECONDARY),
                GradeEntity(id = 8, gradeName = "G7", educationLevel = EducationLevel.SECONDARY),
                GradeEntity(id = 9, gradeName = "G8", educationLevel = EducationLevel.SECONDARY),
                GradeEntity(id = 10, gradeName = "G9", educationLevel = EducationLevel.SECONDARY),
                GradeEntity(id = 11, gradeName = "G10", educationLevel = EducationLevel.HIGH_SCHOOL),
                GradeEntity(id = 12, gradeName = "G11", educationLevel = EducationLevel.HIGH_SCHOOL),
                GradeEntity(id = 13, gradeName = "G12", educationLevel = EducationLevel.HIGH_SCHOOL)
            )
            for (grade in grades) {
                val gId = policyDao.insertGrade(grade)
                // Add default classes A, B, C for each grade
                policyDao.insertClass(SchoolClassEntity(gradeId = gId, className = "A"))
                policyDao.insertClass(SchoolClassEntity(gradeId = gId, className = "B"))
                policyDao.insertClass(SchoolClassEntity(gradeId = gId, className = "C"))
            }

            // 5. Academic Subjects
            val academicSubjects = listOf(
                // Kindergarten
                SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
                SubjectEntity(name = "English (Phonics)", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
                SubjectEntity(name = "English (Language)", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
                SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
                // Primary
                SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "English", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "Science", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "Social Studies", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
                // Secondary
                SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
                SubjectEntity(name = "English", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
                SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
                SubjectEntity(name = "Science", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
                SubjectEntity(name = "History", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
                SubjectEntity(name = "Geography", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
                // High School Common
                SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = ""),
                SubjectEntity(name = "English", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = ""),
                SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = ""),
                SubjectEntity(name = "Physics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = ""),
                SubjectEntity(name = "Chemistry", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = ""),
                // High School Electives
                SubjectEntity(name = "Biology", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = "STEAMS-1"),
                SubjectEntity(name = "Economics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL, subTrack = "STEAMS-2")
            )
            academicSubjects.forEach { policyDao.insertSubject(it) }

            // 6. Additional Subjects
            val additionalSubjects = listOf(
                SubjectEntity(name = "Physical Education (PE)", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.KINDERGARTEN),
                SubjectEntity(name = "Music & Performing Arts", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.KINDERGARTEN),
                SubjectEntity(name = "English (International)", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "Physical Education (PE)", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "Music & Performing Arts", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.PRIMARY),
                SubjectEntity(name = "Coding & Robotics", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.SECONDARY, isCustom = true)
            )
            additionalSubjects.forEach { policyDao.insertSubject(it) }

            // 7. Assessment Types
            val assessmentTypes = listOf(
                // Primary
                AssessmentTypeEntity(name = "Monthly Test", educationLevel = EducationLevel.PRIMARY),
                AssessmentTypeEntity(name = "CET 1", educationLevel = EducationLevel.PRIMARY),
                AssessmentTypeEntity(name = "CET 2", educationLevel = EducationLevel.PRIMARY),
                AssessmentTypeEntity(name = "CET 3", educationLevel = EducationLevel.PRIMARY),
                AssessmentTypeEntity(name = "CET 4", educationLevel = EducationLevel.PRIMARY),
                // Secondary
                AssessmentTypeEntity(name = "Monthly Test", educationLevel = EducationLevel.SECONDARY),
                AssessmentTypeEntity(name = "Pilot Test 1", educationLevel = EducationLevel.SECONDARY),
                AssessmentTypeEntity(name = "Pilot Test 2", educationLevel = EducationLevel.SECONDARY),
                AssessmentTypeEntity(name = "CET 1", educationLevel = EducationLevel.SECONDARY),
                AssessmentTypeEntity(name = "CET 2", educationLevel = EducationLevel.SECONDARY),
                AssessmentTypeEntity(name = "CET 3", educationLevel = EducationLevel.SECONDARY),
                AssessmentTypeEntity(name = "CET 4", educationLevel = EducationLevel.SECONDARY),
                // High School
                AssessmentTypeEntity(name = "Weekly Test", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "Lesson Completion Test", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "Pilot Test 1", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "Pilot Test 2", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "Pilot Test 3", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "Pilot Test 4", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "CET 1", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "CET 2", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "CET 3", educationLevel = EducationLevel.HIGH_SCHOOL),
                AssessmentTypeEntity(name = "CET 4", educationLevel = EducationLevel.HIGH_SCHOOL)
            )
            assessmentTypes.forEach { policyDao.insertAssessmentType(it) }

            // 8. Custom Exams Defaults
            val customExams = listOf(
                CustomExamEntity(gradeId = 0, examName = "Reading Test", description = "Oral reading evaluation"),
                CustomExamEntity(gradeId = 0, examName = "Speaking Test", description = "English fluency test"),
                CustomExamEntity(gradeId = 0, examName = "Vocabulary Test", description = "Spelling & word quiz"),
                CustomExamEntity(gradeId = 0, examName = "Project Work", description = "Group activity & research"),
                CustomExamEntity(gradeId = 0, examName = "Mid-term Practice", description = "Half-yearly mock exam"),
                CustomExamEntity(gradeId = 0, examName = "Final Practice", description = "Year-end preparation")
            )
            customExams.forEach { policyDao.insertCustomExam(it) }

            // 9. Grading Policies Defaults
            val gradingPolicies = listOf(
                // Primary
                GradingPolicyEntity(educationLevel = EducationLevel.PRIMARY, subjectName = "Mathematics", maxMark = 100, passMark = 40, distinctionMark = 80),
                GradingPolicyEntity(educationLevel = EducationLevel.PRIMARY, subjectName = "Other Subjects", maxMark = 100, passMark = 40, distinctionMark = 75),
                // Secondary
                GradingPolicyEntity(educationLevel = EducationLevel.SECONDARY, subjectName = "Mathematics", maxMark = 100, passMark = 40, distinctionMark = 80),
                GradingPolicyEntity(educationLevel = EducationLevel.SECONDARY, subjectName = "Other Subjects", maxMark = 100, passMark = 40, distinctionMark = 75),
                // High School
                GradingPolicyEntity(educationLevel = EducationLevel.HIGH_SCHOOL, subjectName = "Myanmar", maxMark = 100, passMark = 40, distinctionMark = 75),
                GradingPolicyEntity(educationLevel = EducationLevel.HIGH_SCHOOL, subjectName = "English", maxMark = 100, passMark = 40, distinctionMark = 75),
                GradingPolicyEntity(educationLevel = EducationLevel.HIGH_SCHOOL, subjectName = "Other Subjects", maxMark = 100, passMark = 40, distinctionMark = 80)
            )
            gradingPolicies.forEach { policyDao.insertGradingPolicy(it) }

            // 10. Default HCM Core Value Items per Education Level
            val holisticDao = db.holisticDao()

            val defaultHcmItems = mutableListOf<HolisticCategoryEntity>()
            var orderCounter = 1

            // Kindergarten items (5 items: KG)
            val kgItems = listOf(
                Triple("မိမိကိုယ်ကို ယုံကြည်မှုရှိခြင်း", "Self-confidence and self-reliance in daily activities", "HONESTY"),
                Triple("စည်းကမ်းလိုက်နာခြင်း", "Adherence to classroom rules and teacher guidance", "HONESTY"),
                Triple("သူငယ်ချင်းများနှင့် ပူးပေါင်းဆောင်ရွက်ခြင်း", "Peer cooperation and friendly interaction", "MINDFULNESS"),
                Triple("ဆရာ/ဆရာမ၏ ညွှန်ကြားချက်ကို လိုက်နာခြင်း", "Following instructions attentiveness", "HONESTY"),
                Triple("အခြေခံတစ်ကိုယ်ရေသန့်ရှင်းရေးကို ထိန်းသိမ်းခြင်း", "Basic personal hygiene and cleanliness habits", "MINDFULNESS")
            )
            kgItems.forEach { (title, desc, pillar) ->
                defaultHcmItems.add(
                    HolisticCategoryEntity(
                        categoryName = title,
                        description = desc,
                        pillar = pillar,
                        educationLevel = "KINDERGARTEN",
                        maxStars = 5,
                        isEnabled = true,
                        isDefault = true,
                        orderIndex = orderCounter++
                    )
                )
            }

            // Primary items (5 items: G1-G5)
            val primaryItems = listOf(
                Triple("တာဝန်ယူမှုရှိခြင်း", "Personal responsibility and duty fulfillment", "HONESTY"),
                Triple("သင်ယူမှုတွင် စိတ်ပါဝင်စားခြင်း", "Enthusiasm and active engagement in learning", "CURIOSITY"),
                Triple("အဖွဲ့လိုက်လုပ်ဆောင်နိုင်ခြင်း", "Teamwork and active group participation", "MINDFULNESS"),
                Triple("အချိန်ကို တန်ဖိုးထားအသုံးပြုခြင်း", "Punctuality and efficient time management", "HONESTY"),
                Triple("ရိုသေလေးစားမှုနှင့် ယဉ်ကျေးပျူငှာမှုရှိခြင်း", "Respect towards elders and polite social etiquette", "MINDFULNESS")
            )
            primaryItems.forEach { (title, desc, pillar) ->
                defaultHcmItems.add(
                    HolisticCategoryEntity(
                        categoryName = title,
                        description = desc,
                        pillar = pillar,
                        educationLevel = "PRIMARY",
                        maxStars = 5,
                        isEnabled = true,
                        isDefault = true,
                        orderIndex = orderCounter++
                    )
                )
            }

            // Secondary items (5 items: G6-G9)
            val secondaryItems = listOf(
                Triple("မိမိကိုယ်ကို စီမံခန့်ခွဲနိုင်ခြင်း", "Self-management and emotional regulation", "MINDFULNESS"),
                Triple("ဝေဖန်စဉ်းစားနိုင်ခြင်း", "Critical thinking and logical analysis", "CURIOSITY"),
                Triple("ခေါင်းဆောင်မှုစွမ်းရည်", "Leadership skills and positive initiative", "HONESTY"),
                Triple("လူမှုဆက်ဆံရေးကောင်းမွန်ခြင်း", "Effective communication and interpersonal relationship", "MINDFULNESS"),
                Triple("ပြဿနာဖြေရှင်းနိုင်စွမ်းရှိခြင်း", "Problem solving capability and resilience", "CURIOSITY")
            )
            secondaryItems.forEach { (title, desc, pillar) ->
                defaultHcmItems.add(
                    HolisticCategoryEntity(
                        categoryName = title,
                        description = desc,
                        pillar = pillar,
                        educationLevel = "SECONDARY",
                        maxStars = 5,
                        isEnabled = true,
                        isDefault = true,
                        orderIndex = orderCounter++
                    )
                )
            }

            // High School items (5 items: G10-G12)
            val highSchoolItems = listOf(
                Triple("စည်းကမ်းနှင့် တာဝန်ယူမှု", "High integrity, discipline, and ethical responsibility", "HONESTY"),
                Triple("မိမိရည်မှန်းချက်အတွက် ကြိုးစားအားထုတ်မှု", "Goal-oriented effort and academic dedication", "CURIOSITY"),
                Triple("ခေါင်းဆောင်မှုနှင့် ပူးပေါင်းဆောင်ရွက်မှု", "Advanced leadership and strategic collaboration", "MINDFULNESS"),
                Triple("ကိုယ်ပိုင်ဆုံးဖြတ်ချက်ချနိုင်မှု", "Independent decision-making and maturity", "CURIOSITY"),
                Triple("လူ့ကျင့်ဝတ်နှင့် ပတ်ဝန်းကျင်ဆိုင်ရာ သတိပြုမှု", "Ethical conduct and civic/environmental awareness", "MINDFULNESS")
            )
            highSchoolItems.forEach { (title, desc, pillar) ->
                defaultHcmItems.add(
                    HolisticCategoryEntity(
                        categoryName = title,
                        description = desc,
                        pillar = pillar,
                        educationLevel = "HIGH_SCHOOL",
                        maxStars = 5,
                        isEnabled = true,
                        isDefault = true,
                        orderIndex = orderCounter++
                    )
                )
            }
            holisticDao.insertHolisticCategories(defaultHcmItems)

            // 13. Default SGI (Student Growth Indicators) Categories
            val defaultSgiCategories = listOf(
                "Attendance" to "Punctuality, daily attendance consistency, and minimal unexcused absence",
                "Leadership" to "Initiative in student council, class monitor role, or team activities",
                "Discipline" to "Adherence to school dress code, code of conduct, and daily routines",
                "Responsibility" to "Care for school equipment, textbook maintenance, and duty rosters",
                "Reading Habit" to "Library usage, storybook reading logs, and independent reading interest",
                "Communication" to "Polite speech, effective listening, and bilingual confidence",
                "Cooperation" to "Participation in school events, sports day, and community service",
                "Respect" to "Respect for elders, teachers, cultural diversity, and peer support",
                "Creativity" to "Involvement in art, music, science fair, and cultural performances",
                "Confidence" to "Active participation in debates, speeches, and stage performances"
            )
            val sgiEntities = defaultSgiCategories.mapIndexed { index, (name, desc) ->
                SgiCategoryEntity(
                    indicatorName = name,
                    description = desc,
                    maxStars = 5,
                    isEnabled = true,
                    orderIndex = index + 1
                )
            }
            holisticDao.insertSgiCategories(sgiEntities)

            // 14. Default Assessment Periods per Grade/Education Level
            val periodDao = db.assessmentPeriodDao()
            val defaultPeriods = mutableListOf<AssessmentPeriodEntity>()
            var periodOrder = 1

            // Primary Level (KG - G4 / G5)
            val standardMonths = listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
            standardMonths.forEach { m ->
                defaultPeriods.add(
                    AssessmentPeriodEntity(
                        periodName = m,
                        educationLevel = "PRIMARY",
                        periodCategory = "Monthly",
                        isEnabled = true,
                        orderIndex = periodOrder++
                    )
                )
            }

            // Secondary Level (G5/G6 - G9)
            standardMonths.forEach { m ->
                defaultPeriods.add(
                    AssessmentPeriodEntity(
                        periodName = m,
                        educationLevel = "SECONDARY",
                        periodCategory = "Monthly",
                        isEnabled = true,
                        orderIndex = periodOrder++
                    )
                )
            }

            // High School Level (G10 - G12)
            standardMonths.forEach { m ->
                defaultPeriods.add(
                    AssessmentPeriodEntity(
                        periodName = m,
                        educationLevel = "HIGH_SCHOOL",
                        periodCategory = "Monthly",
                        isEnabled = true,
                        orderIndex = periodOrder++
                    )
                )
            }

            periodDao.insertAssessmentPeriods(defaultPeriods)

            // Initial Report Settings and Templates
            val reportDao = db.reportDao()
            reportDao.saveReportSettings(ReportSettingEntity())
            ensureDefaultReportTemplates(reportDao)

            // Initial Academic Years
            val academicYearDao = db.academicYearDao()
            val initialYears = listOf(
                AcademicYearEntity(
                    yearCode = "2026-2027",
                    displayName = "2026–2027 Academic Year",
                    startDate = "2026-06-01",
                    endDate = "2027-03-31",
                    status = AcademicYearStatus.ACTIVE,
                    isCurrentActive = true
                ),
                AcademicYearEntity(
                    yearCode = "2027-2028",
                    displayName = "2027–2028 Academic Year",
                    startDate = "2027-06-01",
                    endDate = "2028-03-31",
                    status = AcademicYearStatus.UPCOMING,
                    isCurrentActive = false
                ),
                AcademicYearEntity(
                    yearCode = "2028-2029",
                    displayName = "2028–2029 Academic Year",
                    startDate = "2028-06-01",
                    endDate = "2029-03-31",
                    status = AcademicYearStatus.UPCOMING,
                    isCurrentActive = false
                )
            )
            initialYears.forEach { academicYearDao.insertAcademicYear(it) }

            // 15. Pre-seed standard Myanmar KG-G12 Curriculum Knowledge Chunks
            CurriculumKnowledgeSeeder.seedCurriculumIfEmpty(db.curriculumKnowledgeDao())

            // Audit log
            userDao.insertAuditLog(
                AuditLogEntity(
                    userName = "System",
                    roleName = "SUPER_ADMIN",
                    action = "INITIALIZE_DATABASE",
                    details = "Pre-populated database with standard Myanmar Grades (KG-G12), subjects, assessment policies, demo users, holistic categories, SGI categories, configurable Assessment Periods, and Report Card Engine Foundation templates."
                )
            )
        }

        private suspend fun ensureDefaultReportTemplates(reportDao: ReportDao) {
            val templates = listOf(
                ReportTemplateEntity(
                    code = "PRIMARY_TEMPLATE",
                    templateName = "Primary Report Card Template",
                    targetLevel = "PRIMARY",
                    description = "Automated report card template for Kindergarten through Grade 4 (KG–G4) featuring Primary academic subjects, HCM pillars, and attendance."
                ),
                ReportTemplateEntity(
                    code = "SECONDARY_TEMPLATE",
                    templateName = "Secondary Report Card Template",
                    targetLevel = "SECONDARY",
                    description = "Automated report card template for Grades 5 through 9 (G5–G9) covering Middle School subjects, Pilot/CET assessments, SGI, and teacher reviews."
                ),
                ReportTemplateEntity(
                    code = "HIGH_SCHOOL_TEMPLATE",
                    templateName = "High School Report Card Template",
                    targetLevel = "HIGH_SCHOOL",
                    description = "Automated report card template for Grades 10 through 12 (G10–G12) with High School track subjects, Weekly Tests, Lesson Completion, Pilot/CET tests, and recommendations."
                )
            )
            reportDao.insertTemplates(templates)
        }

        suspend fun seedDefaultAdminUsers(userDao: UserDao) {
            val allUsers = userDao.getAllUsersList()
            // If the system has any users registered, never auto-seed
            if (allUsers.isNotEmpty()) return

            val existingAdmin = userDao.getUserByUsernameIncludingDeleted("admin")
            // If admin was ever created (even if soft-deleted), respect user deletion and never recreate
            if (existingAdmin != null) return

            userDao.insertUser(
                UserEntity(
                    id = 0,
                    username = "admin",
                    fullName = "Super Administrator",
                    role = UserRole.SUPER_ADMIN,
                    email = "admin@hcm.edu.mm",
                    phone = "+95 9 790001122",
                    passwordHash = com.example.util.PasswordHasher.hashPassword("Password123!"),
                    salt = com.example.util.PasswordHasher.DEFAULT_SALT,
                    isActive = true,
                    status = UserStatus.ACTIVE,
                    mustChangePassword = false,
                    failedLoginAttempts = 0,
                    isDeleted = false,
                    uuid = java.util.UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isDirty = false,
                    managedPassword = "Password123!"
                )
            )
        }
    }
}
