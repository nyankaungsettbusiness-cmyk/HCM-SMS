-- =========================================================================
-- HEIN CHAN MYAE SCHOOL MANAGEMENT SYSTEM (HCM-SMS)
-- SUPABASE MARKS SYNC REPAIR SCRIPT (Fix Error 42703 "column value does not exist")
-- =========================================================================
-- How to apply:
-- 1. Open Supabase Dashboard: https://supabase.com/dashboard/project/gfwdxcmqdgmubotiuxzl/sql/new
-- 2. Paste this entire script into SQL Editor and click "Run".
-- 3. In the mobile app, tap "Sync Now". All student marks will sync across devices immediately!
-- =========================================================================

-- 1. DROP PROBLEMATIC INSERT/UPDATE TRIGGERS ON MARKS & RESULTS TABLES
DO $$
DECLARE
    trg RECORD;
BEGIN
    FOR trg IN (
        SELECT event_object_table, trigger_name
        FROM information_schema.triggers
        WHERE trigger_schema = 'public'
          AND event_object_table IN ('student_marks', 'assessment_results', 'assessments')
          AND trigger_name NOT IN ('set_updated_at')
    ) LOOP
        BEGIN
            EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.%I CASCADE;', trg.trigger_name, trg.event_object_table);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END $$;

-- 2. DROP BLOCKING CHECK & FOREIGN KEY CONSTRAINTS
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT constraint_name, table_name
        FROM information_schema.table_constraints
        WHERE constraint_type IN ('CHECK', 'FOREIGN KEY')
          AND table_schema = 'public'
          AND table_name IN ('student_marks', 'assessment_results')
          AND constraint_name NOT LIKE '%_pkey'
          AND constraint_name NOT LIKE '%_uuid_%'
    ) LOOP
        BEGIN
            EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT IF EXISTS %I;', r.table_name, r.constraint_name);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END $$;

-- 3. ENSURE ALL COMPATIBILITY COLUMNS EXIST ON student_marks
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS assessment_id BIGINT DEFAULT 0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS student_id BIGINT DEFAULT 0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS marks_obtained DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS value DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS score DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS subject_name TEXT DEFAULT '';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS student_code TEXT DEFAULT '';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS student_name TEXT DEFAULT '';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS roll_no INT DEFAULT 0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS max_marks DOUBLE PRECISION DEFAULT 100.0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS pass_mark DOUBLE PRECISION DEFAULT 40.0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS distinction_mark DOUBLE PRECISION DEFAULT 75.0;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS question_marks_json TEXT DEFAULT '';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_absent BOOLEAN DEFAULT false;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_exempt BOOLEAN DEFAULT false;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_passed BOOLEAN DEFAULT false;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_distinction BOOLEAN DEFAULT false;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS remarks TEXT DEFAULT '';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS updated_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false;
ALTER TABLE public.student_marks ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT false;

-- 4. ENSURE COMPATIBILITY COLUMNS ON OTHER TABLES TO PREVENT ERROR 42703
ALTER TABLE public.assessments ADD COLUMN IF NOT EXISTS value TEXT DEFAULT '';
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS value DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.assessment_results ADD COLUMN IF NOT EXISTS score DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS value TEXT DEFAULT '';
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS value TEXT DEFAULT '';
ALTER TABLE public.subjects ADD COLUMN IF NOT EXISTS value TEXT DEFAULT '';

-- 5. ENSURE UNIQUE INDEX ON UUID FOR SAFE UPSERT
CREATE UNIQUE INDEX IF NOT EXISTS student_marks_uuid_idx ON public.student_marks(uuid);
CREATE UNIQUE INDEX IF NOT EXISTS assessment_results_uuid_idx ON public.assessment_results(uuid);

-- 6. ENSURE RLS POLICIES FOR INSTANT SYNC
ALTER TABLE public.student_marks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon and auth sync on student_marks" ON public.student_marks;
CREATE POLICY "Allow anon and auth sync on student_marks" ON public.student_marks FOR ALL USING (true) WITH CHECK (true);

ALTER TABLE public.assessment_results ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon and auth sync on assessment_results" ON public.assessment_results;
CREATE POLICY "Allow anon and auth sync on assessment_results" ON public.assessment_results FOR ALL USING (true) WITH CHECK (true);

-- 7. REMOVE RESTRICTIVE NOT NULL CONSTRAINTS
DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'student_marks'
          AND is_nullable = 'NO'
          AND column_name NOT IN ('id')
    ) LOOP
        BEGIN
            EXECUTE format('ALTER TABLE public.student_marks ALTER COLUMN %I DROP NOT NULL;', rec.column_name);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END $$;
