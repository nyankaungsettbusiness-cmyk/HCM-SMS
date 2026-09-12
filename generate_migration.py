with open('supabase_schema.sql') as f:
    sql = f.read()

remote = {
    'users': ['created_at', 'email', 'id', 'is_deleted', 'password_hash', 'phone', 'role', 'updated_at', 'username', 'uuid'],
    'school_settings': ['email', 'id', 'is_deleted', 'phone', 'principal_name', 'school_name', 'updated_at', 'uuid'],
    'grades': ['created_at', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'school_classes': ['created_at', 'grade_id', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'subjects': ['code', 'created_at', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'grade_subject_cross_ref': ['grade_id', 'subject_id'],
    'assessment_types': ['code', 'created_at', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'custom_exams': ['created_at', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'grading_policies': ['created_at', 'id', 'is_deleted', 'updated_at', 'uuid'],
    'students': ['address', 'created_at', 'dob', 'gender', 'id', 'is_deleted', 'name', 'phone', 'roll_number', 'status', 'updated_at', 'uuid'],
    'teachers': ['created_at', 'email', 'id', 'is_active', 'is_deleted', 'name', 'phone', 'qualification', 'updated_at', 'uuid'],
    'assessments': ['created_at', 'date', 'grade_id', 'id', 'is_deleted', 'max_marks', 'subject_id', 'title', 'updated_at', 'uuid'],
    'student_marks': ['assessment_id', 'created_at', 'id', 'is_absent', 'is_deleted', 'marks_obtained', 'remarks', 'student_id', 'updated_at', 'uuid'],
    'assessment_results': ['created_at', 'id', 'is_deleted', 'percentage', 'rank', 'student_id', 'total_marks', 'updated_at', 'uuid'],
    'holistic_categories': ['created_at', 'description', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'holistic_results': ['category_id', 'created_at', 'id', 'is_deleted', 'rating', 'student_id', 'updated_at', 'uuid'],
    'sgi_categories': ['created_at', 'description', 'id', 'is_deleted', 'name', 'updated_at', 'uuid'],
    'sgi_results': ['category_id', 'created_at', 'id', 'is_deleted', 'rating', 'student_id', 'updated_at', 'uuid'],
    'teacher_comments': ['comment', 'created_at', 'id', 'is_deleted', 'student_id', 'updated_at', 'uuid'],
    'assessment_periods': ['created_at', 'end_date', 'id', 'is_deleted', 'name', 'start_date', 'updated_at', 'uuid'],
    'attendance_records': ['class_id', 'created_at', 'date', 'id', 'is_deleted', 'status', 'student_id', 'updated_at', 'uuid'],
    'academic_years': ['created_at', 'end_date', 'id', 'is_deleted', 'name', 'start_date', 'updated_at', 'uuid'],
    'promotion_history': ['created_at', 'id', 'is_deleted', 'promoted_date', 'remarks', 'student_id', 'updated_at', 'uuid']
}

import re
matches = re.findall(r'CREATE TABLE IF NOT EXISTS public\.([a-zA-Z0-9_]+)\s*\((.*?)\);', sql, re.DOTALL)

script = ['-- ====================================================================',
          '-- SCHEMA MIGRATION & RLS REPAIR SCRIPT FOR SUPABASE PROJECT gfwdxcmqdgmubotiuxzl',
          '-- 1. Adds all missing schema columns with default values',
          '-- 2. Configures Row Level Security (RLS) policies for full sync access',
          '-- ====================================================================\n']

for tbl, body in matches:
    existing = set(remote.get(tbl, []))
    lines_for_table = []
    for line in body.strip().split('\n'):
        line = line.strip()
        if line and not line.startswith('--') and not line.startswith('CONSTRAINT') and not line.startswith('PRIMARY'):
            col = line.split()[0]
            if col not in ['FOREIGN', 'CHECK', 'UNIQUE'] and col not in existing:
                col_def = line.rstrip(',')
                if 'NOT NULL' in col_def and 'DEFAULT' not in col_def:
                    col_def = col_def.replace('NOT NULL', '')
                lines_for_table.append(f'ALTER TABLE public.{tbl} ADD COLUMN IF NOT EXISTS {col_def};')
    if lines_for_table:
        script.append(f'-- Table: {tbl}')
        script.extend(lines_for_table)
        script.append('')

rls_block = """-- ====================================================================
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
"""
script.append(rls_block)

full_migration = '\n'.join(script)
with open('supabase_migration_fix.sql', 'w') as out:
    out.write(full_migration)

print('Generated supabase_migration_fix.sql successfully!')
