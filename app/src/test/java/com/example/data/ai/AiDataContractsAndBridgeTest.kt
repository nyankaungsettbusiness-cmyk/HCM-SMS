package com.example.data.ai

import com.example.data.local.entity.UserRole
import org.junit.Assert.*
import org.junit.Test

class AiDataContractsAndBridgeTest {

    @Test
    fun testAdminPermissionScope_hasUniversalAccess() {
        val superAdminScope = AiPermissionScope(
            username = "superadmin",
            userRole = UserRole.SUPER_ADMIN
        )
        assertTrue(superAdminScope.isSuperAdminOrAdmin)
        assertTrue(superAdminScope.canAccessStudent("Grade 5", "A"))
        assertTrue(superAdminScope.canAccessStudent("Grade 10", "B"))
        assertTrue(superAdminScope.canAccessSubject("Physics"))
        assertTrue(superAdminScope.canAccessSubject("Economics"))

        val adminScope = AiPermissionScope(
            username = "school_admin",
            userRole = UserRole.ADMIN
        )
        assertTrue(adminScope.isSuperAdminOrAdmin)
        assertTrue(adminScope.canAccessStudent("Grade 1", "A"))
    }

    @Test
    fun testTeacherPermissionScope_isStrictlyIsolatedToAssignedClassAndSubject() {
        val teacherScope = AiPermissionScope(
            username = "daw_hla",
            userRole = UserRole.TEACHER,
            linkedTeacherId = 101L,
            teacherName = "Daw Hla",
            assignedGrades = listOf("Grade 5", "G5"),
            assignedClasses = listOf("A"),
            assignedSubjects = listOf("English", "Science")
        )

        assertFalse(teacherScope.isSuperAdminOrAdmin)
        
        // Allowed
        assertTrue(teacherScope.canAccessStudent("Grade 5", "A"))
        assertTrue(teacherScope.canAccessSubject("English"))
        assertTrue(teacherScope.canAccessSubject("Science"))

        // Blocked - Other Grades or Classes
        assertFalse(teacherScope.canAccessStudent("Grade 5", "B"))
        assertFalse(teacherScope.canAccessStudent("Grade 10", "A"))
        assertFalse(teacherScope.canAccessStudent("Grade 7", "C"))

        // Blocked - Other Subjects
        assertFalse(teacherScope.canAccessSubject("Physics"))
        assertFalse(teacherScope.canAccessSubject("Economics"))
    }

    @Test
    fun testVerifiedAcademicFacts_percentagesAndGrades() {
        val subjectScores = listOf(
            SubjectScoreFact(
                subjectName = "Myanmar",
                rawScore = 85.0,
                fullMarks = 100.0,
                percentage = 85.0,
                letterGrade = "A",
                passStatus = "PASSED",
                isDistinction = true
            ),
            SubjectScoreFact(
                subjectName = "English",
                rawScore = 72.0,
                fullMarks = 100.0,
                percentage = 72.0,
                letterGrade = "B",
                passStatus = "PASSED",
                isDistinction = false
            ),
            SubjectScoreFact(
                subjectName = "Mathematics",
                rawScore = 35.0,
                fullMarks = 100.0,
                percentage = 35.0,
                letterGrade = "F",
                passStatus = "FAILED",
                isDistinction = false
            )
        )

        val total = subjectScores.sumOf { it.rawScore }
        val maxTotal = subjectScores.sumOf { it.fullMarks }
        val overallPct = (total / maxTotal) * 100.0

        val facts = VerifiedStudentAcademicFacts(
            studentId = 1L,
            studentName = "Mg Mg",
            rollNumber = 1,
            studentCode = "HCM-2026-001",
            grade = "Grade 5",
            className = "A",
            academicYear = "2026-2027",
            assessmentName = "Term 1",
            subjectScores = subjectScores,
            totalScore = total,
            maxPossibleScore = maxTotal,
            overallPercentage = overallPct,
            overallGrade = "C",
            strongestSubjects = listOf("Myanmar"),
            subjectsNeedingSupport = listOf("Mathematics"),
            isAtRisk = false
        )

        assertEquals(192.0, facts.totalScore, 0.01)
        assertEquals(64.0, facts.overallPercentage, 0.01)
        assertEquals("Myanmar", facts.strongestSubjects.first())
        assertEquals("Mathematics", facts.subjectsNeedingSupport.first())
    }
}
