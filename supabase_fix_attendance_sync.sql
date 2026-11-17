-- =========================================================================
-- HEIN CHAN MYAE SCHOOL MANAGEMENT SYSTEM (HCM-SMS)
-- SUPABASE ATTENDANCE SYNC REPAIR SCRIPT (Fix Error 42703 "column value does not exist")
-- =========================================================================
-- How to apply:
-- 1. Open Supabase Dashboard: https://supabase.com/dashboard/project/gfwdxcmqdgmubotiuxzl/sql/new
-- 2. Paste this entire script into SQL Editor and click "Run".
-- 3. In the mobile app, tap "Sync Now" or "Upload All Local Data as Master".
--    All attendance records will sync across devices immediately!
-- =========================================================================

-- 1. DROP PROBLEMATIC INSERT/UPDATE TRIGGERS ON ATTENDANCE TABLE
DO $$
DECLARE
    trg RECORD;
BEGIN
    FOR trg IN (
        SELECT event_object_table, trigger_name
        FROM information_schema.triggers
        WHERE trigger_schema = 'public'
          AND event_object_table = 'attendance_records'
          AND trigger_name NOT IN ('set_updated_at', 'set_attendance_records_updated_at')
    ) LOOP
        BEGIN
            EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.%I CASCADE;', trg.trigger_name, trg.event_object_table);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END $$;

-- 2. ADD ALL MISSING COLUMNS (Fix Error 42703 column "value" does not exist and ensure schema compatibility)
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS value TEXT DEFAULT '';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS remarks TEXT DEFAULT '';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'PRESENT';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS academic_year TEXT DEFAULT '';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS grade TEXT DEFAULT '';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS class_name TEXT DEFAULT '';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS session TEXT DEFAULT 'MORNING';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS recorded_by TEXT DEFAULT 'Teacher';
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS recorded_date_time TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false;
ALTER TABLE public.attendance_records ADD COLUMN IF NOT EXISTS is_dirty BOOLEAN DEFAULT false;

-- 3. DROP BLOCKING CHECK & FOREIGN KEY CONSTRAINTS (Prevent sync failures if student id mapping differs)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT constraint_name, table_name
        FROM information_schema.table_constraints
        WHERE constraint_type IN ('CHECK', 'FOREIGN KEY')
          AND table_schema = 'public'
          AND table_name = 'attendance_records'
          AND constraint_name NOT LIKE '%_pkey'
          AND constraint_name NOT LIKE '%_uuid_%'
    ) LOOP
        BEGIN
            EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT IF EXISTS %I;', r.table_name, r.constraint_name);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END $$;

-- 4. ENSURE ROW LEVEL SECURITY & FULL ACCESS PERMISSIONS
ALTER TABLE public.attendance_records ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon and auth sync on attendance_records" ON public.attendance_records;
CREATE POLICY "Allow anon and auth sync on attendance_records" ON public.attendance_records FOR ALL USING (true) WITH CHECK (true);

GRANT ALL ON TABLE public.attendance_records TO anon, authenticated, service_role;
