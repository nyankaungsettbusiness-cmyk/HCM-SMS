import urllib.request, json
from concurrent.futures import ThreadPoolExecutor

API_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdmd2R4Y21xZGdtdWJvdGl1eHpsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4NzY0MjUsImV4cCI6MjEwNDQ1MjQyNX0.l4kxnVAPmeBF1OS6_nCj36bPRZdHKPDuhZiY3yAJU7o'
BASE_URL = 'https://gfwdxcmqdgmubotiuxzl.supabase.co/rest/v1'

def check(tbl, col):
    url = f'{BASE_URL}/{tbl}?select={col}&limit=0'
    req = urllib.request.Request(url, headers={'apikey': API_KEY, 'Authorization': f'Bearer {API_KEY}'})
    try:
        with urllib.request.urlopen(req) as resp:
            return (col, True)
    except:
        return (col, False)

candidates_by_table = {
    'users': ['id', 'uuid', 'username', 'password_hash', 'display_name', 'role', 'email', 'phone', 'is_active', 'created_at', 'last_login_timestamp', 'last_logout_timestamp', 'must_change_password', 'failed_login_attempts', 'is_deleted', 'updated_at', 'is_dirty', 'name'],
    'school_settings': ['id', 'uuid', 'school_name_en', 'school_name_my', 'motto', 'address', 'phone', 'email', 'website', 'principal_name', 'logo_path', 'current_academic_year', 'system_notifications_enabled', 'backup_reminders_enabled', 'academic_year_reminder_enabled', 'assessment_reminder_enabled', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'school_name'],
    'grades': ['id', 'uuid', 'grade_name', 'education_level', 'report_card_template', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'code', 'order_index'],
    'school_classes': ['id', 'uuid', 'grade_id', 'class_name', 'capacity', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'code', 'room'],
    'subjects': ['id', 'uuid', 'name', 'subject_code', 'sub_track', 'is_enabled', 'is_editable', 'is_custom', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'code'],
    'grade_subject_cross_ref': ['grade_id', 'subject_id'],
    'assessment_types': ['id', 'uuid', 'name', 'education_level', 'is_custom', 'is_enabled', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'code', 'weight'],
    'custom_exams': ['id', 'uuid', 'grade_id', 'exam_name', 'is_enabled', 'description', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'title'],
    'grading_policies': ['id', 'uuid', 'education_level', 'subject_name', 'max_mark', 'pass_mark', 'distinction_mark', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'grade_id', 'subject_id', 'min_mark', 'name'],
    'students': ['id', 'uuid', 'student_id', 'name', 'grade', 'class_name', 'gender', 'date_of_birth', 'parent_name', 'parent_phone', 'address', 'status', 'photo_avatar_index', 'stream', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'dob', 'roll_number', 'roll_no', 'phone', 'student_code', 'grade_id', 'class_id'],
    'teachers': ['id', 'uuid', 'teacher_id', 'name', 'phone', 'email', 'assigned_grade', 'assigned_class', 'assigned_subjects', 'employment_status', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'teacher_code', 'is_active', 'qualification', 'address', 'role'],
    'assessments': ['id', 'uuid', 'title', 'grade', 'subject', 'assessment_type', 'assessment_period', 'academic_year', 'date_conducted', 'max_marks', 'description', 'status', 'created_by', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'date', 'grade_id', 'subject_id', 'period_id', 'academic_year_id'],
    'student_marks': ['id', 'uuid', 'assessment_id', 'student_id', 'marks_obtained', 'is_absent', 'is_exempt', 'is_passed', 'is_distinction', 'remarks', 'updated_by', 'updated_at', 'is_dirty', 'is_deleted', 'marks', 'created_at'],
    'assessment_results': ['id', 'uuid', 'student_id', 'assessment_period', 'academic_year', 'grade', 'class_name', 'total_marks', 'max_possible_marks', 'percentage', 'overall_result', 'distinction_count', 'updated_at', 'is_dirty', 'is_deleted', 'period_id', 'academic_year_id', 'grade_id', 'class_id', 'created_at', 'status', 'rank'],
    'holistic_categories': ['id', 'uuid', 'domain', 'category_name', 'max_stars', 'description', 'is_enabled', 'is_default', 'order_index', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'title'],
    'holistic_results': ['id', 'uuid', 'student_id', 'category_id', 'assessment_period', 'academic_year', 'rating_stars', 'max_stars', 'updated_by', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'rating', 'stars', 'period_id', 'academic_year_id'],
    'sgi_categories': ['id', 'uuid', 'category_name', 'max_stars', 'description', 'is_enabled', 'order_index', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'title'],
    'sgi_results': ['id', 'uuid', 'student_id', 'category_id', 'assessment_period', 'academic_year', 'rating_stars', 'max_stars', 'updated_by', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'rating', 'stars', 'period_id', 'academic_year_id'],
    'teacher_comments': ['id', 'uuid', 'student_id', 'assessment_period', 'academic_year', 'strength_comment', 'need_improvement_comment', 'general_comment', 'future_recommendation', 'updated_by', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'comment', 'remarks', 'period_id', 'academic_year_id'],
    'assessment_periods': ['id', 'uuid', 'period_name', 'education_level', 'period_category', 'is_enabled', 'order_index', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'title', 'start_date', 'end_date'],
    'attendance_records': ['id', 'uuid', 'student_id', 'date', 'status', 'academic_year', 'grade', 'class_name', 'recorded_by', 'recorded_date_time', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'academic_year_id', 'grade_id', 'class_id'],
    'academic_years': ['id', 'uuid', 'year_name', 'start_date', 'end_date', 'status', 'is_current_active', 'closed_date', 'closed_by', 'created_at', 'updated_at', 'is_dirty', 'is_deleted', 'name', 'is_active', 'active', 'academic_year'],
    'promotion_history': ['id', 'uuid', 'student_id', 'from_academic_year', 'to_academic_year', 'from_grade', 'to_grade', 'from_class', 'to_class', 'action_type', 'promoted_date', 'promoted_by', 'remarks', 'created_at', 'updated_at', 'is_dirty', 'is_deleted']
}

for tbl, cands in candidates_by_table.items():
    with ThreadPoolExecutor(max_workers=16) as ex:
        res = list(ex.map(lambda c: check(tbl, c), cands))
    found = [c for c, ok in res if ok]
    print(f'MAPPING:{tbl}:{sorted(found)}')
