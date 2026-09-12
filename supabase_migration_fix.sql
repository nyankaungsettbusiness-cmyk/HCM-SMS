-- ====================================================================
-- SCHEMA MIGRATION & RLS REPAIR SCRIPT FOR SUPABASE PROJECT gfwdxcmqdgmubotiuxzl
-- 1. Adds all missing schema columns with default values
-- 2. Configures Row Level Security (RLS) policies for full sync access
-- ====================================================================

-- Table: users
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS display_name TEXT ;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_login_timestamp BIGINT DEFAULT NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_logout_timestamp BIGINT DEFAULT NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: school_settings
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS school_name_en TEXT DEFAULT 'Harvest Christian Academy';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS school_name_my TEXT DEFAULT '';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS motto TEXT DEFAULT 'Faith, Excellence, Leadership';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS address TEXT DEFAULT 'Yangon, Myanmar';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS website TEXT DEFAULT 'www.hcm.edu.mm';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS logo_path TEXT DEFAULT '';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS current_academic_year TEXT DEFAULT '2025-2026';
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS system_notifications_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS backup_reminders_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS academic_year_reminder_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS assessment_reminder_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.school_settings ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: grades
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS grade_name TEXT ;
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS education_level TEXT ;
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS report_card_template TEXT DEFAULT 'Standard';
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: school_classes
ALTER TABLE public.school_classes ADD COLUMN IF NOT EXISTS class_name TEXT ;
ALTER TABLE public.school_classes ADD COLUMN IF NOT EXISTS capacity INT DEFAULT 40;
ALTER TABLE public.school_classes ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: subjects
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS subject_code TEXT UNIQUE ;
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS sub_track TEXT DEFAULT '';
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS is_editable BOOLEAN DEFAULT TRUE;
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS is_custom BOOLEAN DEFAULT FALSE;
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: assessment_types
ALTER TABLE public.assessment_types ADD COLUMN IF NOT EXISTS education_level TEXT ;
ALTER TABLE public.assessment_types ADD COLUMN IF NOT EXISTS is_custom BOOLEAN DEFAULT FALSE;
ALTER TABLE public.assessment_types ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.assessment_types ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: custom_exams
ALTER TABLE public.custom_exams ADD COLUMN IF NOT EXISTS grade_id BIGINT DEFAULT 0;
ALTER TABLE public.custom_exams ADD COLUMN IF NOT EXISTS exam_name TEXT ;
ALTER TABLE public.custom_exams ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.custom_exams ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.custom_exams ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: grading_policies
ALTER TABLE public.grading_policies ADD COLUMN IF NOT EXISTS education_level TEXT ;
ALTER TABLE public.grading_policies ADD COLUMN IF NOT EXISTS subject_name TEXT ;
ALTER TABLE public.grading_policies ADD COLUMN IF NOT EXISTS max_mark INT DEFAULT 100;
ALTER TABLE public.grading_policies ADD COLUMN IF NOT EXISTS pass_mark INT DEFAULT 40;
ALTER TABLE public.grading_policies ADD COLUMN IF NOT EXISTS distinction_mark INT DEFAULT 75;
ALTER TABLE public.grading_policies ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: students
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS student_id TEXT UNIQUE ;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS grade TEXT ;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS class_name TEXT ;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS date_of_birth TEXT ;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS parent_name TEXT ;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS parent_phone TEXT ;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS photo_avatar_index INT DEFAULT 0;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS stream TEXT DEFAULT '';
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: teachers
ALTER TABLE public.teachers ADD COLUMN IF NOT EXISTS teacher_id TEXT UNIQUE ;
ALTER TABLE public.teachers ADD COLUMN IF NOT EXISTS assigned_grade TEXT DEFAULT 'KG';
ALTER TABLE public.teachers ADD COLUMN IF NOT EXISTS assigned_class TEXT DEFAULT 'A';
ALTER TABLE public.teachers ADD COLUMN IF NOT EXISTS assigned_subjects TEXT DEFAULT 'Myanmar, English';
ALTER TABLE public.teachers ADD COLUMN IF NOT EXISTS employment_status TEXT DEFAULT 'Active';
ALTER TABLE public.teachers ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: assessments
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS grade TEXT ;
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS subject TEXT ;
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS assessment_type TEXT ;
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS assessment_period TEXT DEFAULT 'Monthly';
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS academic_year TEXT DEFAULT '2025-2026';
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS date_conducted TEXT ;
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'DRAFT';
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS created_by TEXT DEFAULT 'Admin';
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: student_marks
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_exempt BOOLEAN DEFAULT FALSE;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_passed BOOLEAN DEFAULT FALSE;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_distinction BOOLEAN DEFAULT FALSE;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS updated_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: assessment_results
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS assessment_period TEXT ;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS academic_year TEXT ;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS grade TEXT ;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS class_name TEXT ;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS max_possible_marks DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS overall_result TEXT DEFAULT 'Pending';
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS distinction_count INT DEFAULT 0;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: holistic_categories
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS domain TEXT ;
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS category_name TEXT ;
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS max_stars INT DEFAULT 5;
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0;
ALTER TABLE public.holistic_categories ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: holistic_results
ALTER TABLE public.holistic_results ADD COLUMN IF NOT EXISTS assessment_period TEXT ;
ALTER TABLE public.holistic_results ADD COLUMN IF NOT EXISTS academic_year TEXT ;
ALTER TABLE public.holistic_results ADD COLUMN IF NOT EXISTS rating_stars INT DEFAULT 0;
ALTER TABLE public.holistic_results ADD COLUMN IF NOT EXISTS max_stars INT DEFAULT 5;
ALTER TABLE public.holistic_results ADD COLUMN IF NOT EXISTS updated_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.holistic_results ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: sgi_categories
ALTER TABLE public.sgi_categories ADD COLUMN IF NOT EXISTS category_name TEXT ;
ALTER TABLE public.sgi_categories ADD COLUMN IF NOT EXISTS max_stars INT DEFAULT 5;
ALTER TABLE public.sgi_categories ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.sgi_categories ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0;
ALTER TABLE public.sgi_categories ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: sgi_results
ALTER TABLE public.sgi_results ADD COLUMN IF NOT EXISTS assessment_period TEXT ;
ALTER TABLE public.sgi_results ADD COLUMN IF NOT EXISTS academic_year TEXT ;
ALTER TABLE public.sgi_results ADD COLUMN IF NOT EXISTS rating_stars INT DEFAULT 0;
ALTER TABLE public.sgi_results ADD COLUMN IF NOT EXISTS max_stars INT DEFAULT 5;
ALTER TABLE public.sgi_results ADD COLUMN IF NOT EXISTS updated_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.sgi_results ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: teacher_comments
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS assessment_period TEXT ;
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS academic_year TEXT ;
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS strength_comment TEXT DEFAULT '';
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS need_improvement_comment TEXT DEFAULT '';
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS general_comment TEXT DEFAULT '';
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS future_recommendation TEXT DEFAULT '';
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS updated_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.teacher_comments ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: assessment_periods
ALTER TABLE public.assessment_periods ADD COLUMN IF NOT EXISTS period_name TEXT ;
ALTER TABLE public.assessment_periods ADD COLUMN IF NOT EXISTS education_level TEXT ;
ALTER TABLE public.assessment_periods ADD COLUMN IF NOT EXISTS period_category TEXT DEFAULT '';
ALTER TABLE public.assessment_periods ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE public.assessment_periods ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0;
ALTER TABLE public.assessment_periods ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: attendance_records
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS academic_year TEXT ;
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS grade TEXT ;
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS class_name TEXT ;
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS recorded_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS recorded_date_time TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: academic_years
ALTER TABLE public.academic_years ADD COLUMN IF NOT EXISTS year_name TEXT UNIQUE ;
ALTER TABLE public.academic_years ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'UPCOMING';
ALTER TABLE public.academic_years ADD COLUMN IF NOT EXISTS is_current_active BOOLEAN DEFAULT FALSE;
ALTER TABLE public.academic_years ADD COLUMN IF NOT EXISTS closed_date BIGINT DEFAULT 0;
ALTER TABLE public.academic_years ADD COLUMN IF NOT EXISTS closed_by TEXT DEFAULT '';
ALTER TABLE public.academic_years ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- Table: promotion_history
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS from_academic_year TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS to_academic_year TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS from_grade TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS to_grade TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS from_class TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS to_class TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS action_type TEXT ;
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS promoted_by TEXT DEFAULT 'Admin';
ALTER TABLE public.promotion_history ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT FALSE;

-- ====================================================================
-- RLS POLICIES FOR SYNC (Allow read & write for anon and authenticated)
-- ====================================================================
DO $$ 
DECLARE 
    tbl text;
BEGIN
    FOR tbl IN 
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema='public' AND table_type='BASE TABLE'
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY;', tbl);
        EXECUTE format('DROP POLICY IF EXISTS "Allow anon and auth sync on %I" ON public.%I;', tbl, tbl);
        EXECUTE format('CREATE POLICY "Allow anon and auth sync on %I" ON public.%I FOR ALL USING (true) WITH CHECK (true);', tbl, tbl);
    END LOOP;
END $$;
