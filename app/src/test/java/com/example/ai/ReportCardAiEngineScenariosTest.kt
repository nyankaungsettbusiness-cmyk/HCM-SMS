package com.example.ai

import com.example.data.ai.*
import com.example.data.local.entity.StudentEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying the HCM-SMS Report Card AI Engine and StudentFactsAggregator
 * covering all required testing scenarios A through K:
 *
 * A. High-performing student across all subjects
 * B. Average student with one weak subject
 * C. Student with low attendance
 * D. Student with missing attendance data
 * E. Student with high academic scores but low holistic ratings
 * F. Student with high holistic ratings but low academic scores
 * G. Student with improving trend
 * H. Student with declining trend
 * I. Student with missing holistic assessment data
 * J. Output in Myanmar language
 * K. Output in English language
 */
class ReportCardAiEngineScenariosTest {

    private val aiEngine = ReportCardAiEngine()

    private fun createBaseStudent(id: Long = 101L, name: String = "Mg Mg"): StudentEntity {
        return StudentEntity(
            id = id,
            studentCode = "STU-001",
            name = name,
            gender = "Male",
            dateOfBirth = "2015-05-12",
            gradeName = "G4",
            className = "Room A",
            rollNumber = 1,
            parentName = "U Ba",
            phone = "0912345678",
            address = "Yangon"
        )
    }

    // Scenario A: High-performing student across all subjects
    @Test
    fun testScenarioA_HighPerformingStudent() {
        val student = createBaseStudent(101L, "Mg Mg")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Myanmar", 95.0, 100.0, 95.0, "A", true, "Distinction"),
                SubjectFact("English", 90.0, 100.0, 90.0, "A", true, "Distinction"),
                SubjectFact("Mathematics", 98.0, 100.0, 98.0, "A", true, "Distinction"),
                SubjectFact("Science", 92.0, 100.0, 92.0, "A", true, "Distinction")
            ),
            strongestSubjects = listOf("Mathematics", "Myanmar", "Science", "English"),
            improvementSubjects = emptyList(),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(375.0, 400.0, 93.8, "A", 1, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 98.0, 2.0, 0.0, 25, true, "Excellent Attendance"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(
                pillars = listOf(
                    HolisticPillarFact("Honesty", "ရိုးသားဖြောင့်မတ်မှု", 4.8),
                    HolisticPillarFact("Curiosity", "စူးစမ်းရှာဖွေလိုစိတ်", 5.0)
                ),
                honestyAvg = 4.8,
                curiosityAvg = 5.0,
                mindfulnessAvg = 4.5,
                overallAverage = 4.8,
                overallLevel = "Excellent",
                topStrengths = listOf("Curiosity (5.0/5)", "Honesty (4.8/5)"),
                growthAreas = emptyList()
            )
        )

        val resultMyan = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertTrue(resultMyan.teacherComment.contains("ထူးချွန်စွာ အောင်မြင်ခဲ့ပါသည်"))
        assertTrue(resultMyan.teacherComment.contains("Mathematics"))
        assertEquals(0, facts.improvementSubjects.size)

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Overall Average: 93.8%"))
        assertTrue(prompt.contains("Class Rank: 1 out of 30"))
    }

    // Scenario B: Average student with one weak subject
    @Test
    fun testScenarioB_AverageStudentWithWeakSubject() {
        val student = createBaseStudent(102L, "Aung Aung")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Myanmar", 75.0, 100.0, 75.0, "B", true, ""),
                SubjectFact("English", 70.0, 100.0, 70.0, "B", true, ""),
                SubjectFact("Mathematics", 45.0, 100.0, 45.0, "D", true, ""),
                SubjectFact("Science", 68.0, 100.0, 68.0, "C", true, "")
            ),
            strongestSubjects = listOf("Myanmar"),
            improvementSubjects = listOf("Mathematics"),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(258.0, 400.0, 64.5, "C", 18, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 92.0, 8.0, 0.0, 25, true, "Satisfactory"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(
                pillars = emptyList(),
                honestyAvg = 0.0, curiosityAvg = 0.0, mindfulnessAvg = 0.0,
                overallAverage = 0.0, overallLevel = "No Holistic Data", topStrengths = emptyList(), growthAreas = emptyList()
            )
        )

        val result = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertTrue(result.teacherComment.contains("Mathematics"))
        assertTrue(result.parentSuggestion.contains("Mathematics"))
        assertTrue(result.supportingFacts.any { it.contains("Subjects Needing Support: Mathematics") })
    }

    // Scenario C: Student with low attendance
    @Test
    fun testScenarioC_LowAttendanceStudent() {
        val student = createBaseStudent(103L, "Kyaw Kyaw")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Myanmar", 60.0, 100.0, 60.0, "C", true, "")
            ),
            strongestSubjects = emptyList(),
            improvementSubjects = listOf("Myanmar"),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(60.0, 100.0, 60.0, "C", 25, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact(
                periodName = "July",
                presentPercentage = 68.0,
                absentPercentage = 32.0,
                latePercentage = 0.0,
                totalDaysRecorded = 25,
                isAttendanceDataAvailable = true,
                attendanceStatusDescription = "Attendance Needs Attention (68.0%)"
            ),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Attendance Present: 68.0%"))
        assertTrue(prompt.contains("Absent: 32.0%"))
        // Zero fabrication of reasons for absences
        assertFalse(prompt.contains("sick"))
        assertFalse(prompt.contains("health"))
    }

    // Scenario D: Student with missing attendance data
    @Test
    fun testScenarioD_MissingAttendanceData() {
        val student = createBaseStudent(104L, "Su Su")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(SubjectFact("Science", 80.0, 100.0, 80.0, "A", true, "")),
            strongestSubjects = listOf("Science"),
            improvementSubjects = emptyList(),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(80.0, 100.0, 80.0, "A", 5, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 0.0, 0.0, 0.0, 0, false, "No attendance records recorded for this academic year."),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Attendance: No attendance data recorded."))
        val result = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertNotNull(result.teacherComment)
    }

    // Scenario E: High academic scores but low holistic ratings
    @Test
    fun testScenarioE_HighAcademicLowHolistic() {
        val student = createBaseStudent(105L, "Hla Hla")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Mathematics", 95.0, 100.0, 95.0, "A", true, "Distinction")
            ),
            strongestSubjects = listOf("Mathematics"),
            improvementSubjects = emptyList(),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(95.0, 100.0, 95.0, "A", 2, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 96.0, 4.0, 0.0, 25, true, "Excellent"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(
                pillars = listOf(
                    HolisticPillarFact("Honesty", "ရိုးသားဖြောင့်မတ်မှု", 2.0),
                    HolisticPillarFact("Mindfulness", "သတိတရားရှိမှု", 2.2)
                ),
                honestyAvg = 2.0,
                curiosityAvg = 0.0,
                mindfulnessAvg = 2.2,
                overallAverage = 2.1,
                overallLevel = "Developing",
                topStrengths = emptyList(),
                growthAreas = listOf("Honesty (2.0/5)", "Mindfulness (2.2/5)")
            )
        )

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Overall Average: 95.0%"))
        assertTrue(prompt.contains("Honesty (ရိုးသားဖြောင့်မတ်မှု): 2.0/5.0"))
        assertTrue(prompt.contains("Holistic Overall Average: 2.1/5.0"))
    }

    // Scenario F: High holistic ratings but low academic scores
    @Test
    fun testScenarioF_HighHolisticLowAcademic() {
        val student = createBaseStudent(106L, "Zaw Zaw")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Mathematics", 42.0, 100.0, 42.0, "E", true, ""),
                SubjectFact("English", 38.0, 100.0, 38.0, "F", false, "")
            ),
            strongestSubjects = emptyList(),
            improvementSubjects = listOf("English", "Mathematics"),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(80.0, 200.0, 40.0, "E", 28, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 95.0, 5.0, 0.0, 25, true, "Excellent"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(
                pillars = listOf(
                    HolisticPillarFact("Honesty", "ရိုးသားဖြောင့်မတ်မှု", 5.0),
                    HolisticPillarFact("Responsibility", "တာဝန်ယူမှုတာဝန်ခံမှု", 4.8)
                ),
                honestyAvg = 5.0,
                curiosityAvg = 0.0,
                mindfulnessAvg = 0.0,
                overallAverage = 4.9,
                overallLevel = "Excellent",
                topStrengths = listOf("Honesty (5.0/5)", "Responsibility (4.8/5)"),
                growthAreas = emptyList()
            )
        )

        val result = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertTrue(result.teacherComment.contains("English") || result.teacherComment.contains("Mathematics"))
        assertTrue(result.teacherComment.contains("ရိုးသားဖြောင့်မတ်မှု"))
    }

    // Scenario G: Student with improving trend
    @Test
    fun testScenarioG_ImprovingTrendStudent() {
        val student = createBaseStudent(107L, "Bo Bo")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Mathematics", 62.0, 100.0, 62.0, "C", true, "")
            ),
            strongestSubjects = emptyList(),
            improvementSubjects = listOf("Mathematics"),
            assessmentResults = emptyList(),
            assessmentTrends = listOf(
                AssessmentTrendFact(
                    subjectName = "Mathematics",
                    progressionDescription = "Improving (+12.0% from 50% to 62%)",
                    previousScore = 50.0,
                    currentScore = 62.0,
                    percentageChange = 12.0
                )
            ),
            overallPerformance = OverallPerformanceFact(62.0, 100.0, 62.0, "C", 20, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 90.0, 10.0, 0.0, 25, true, "Satisfactory"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Mathematics: Improving (+12.0% from 50% to 62%)"))

        val result = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertTrue(result.teacherComment.contains("တိုးတက်မှု"))
    }

    // Scenario H: Student with declining trend
    @Test
    fun testScenarioH_DecliningTrendStudent() {
        val student = createBaseStudent(108L, "Nilar")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Science", 55.0, 100.0, 55.0, "D", true, "")
            ),
            strongestSubjects = emptyList(),
            improvementSubjects = listOf("Science"),
            assessmentResults = emptyList(),
            assessmentTrends = listOf(
                AssessmentTrendFact(
                    subjectName = "Science",
                    progressionDescription = "Declining (-15.0% from 70% to 55%)",
                    previousScore = 70.0,
                    currentScore = 55.0,
                    percentageChange = -15.0
                )
            ),
            overallPerformance = OverallPerformanceFact(55.0, 100.0, 55.0, "D", 22, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 90.0, 10.0, 0.0, 25, true, "Satisfactory"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Science: Declining (-15.0% from 70% to 55%)"))
    }

    // Scenario I: Missing holistic assessment data
    @Test
    fun testScenarioI_MissingHolisticData() {
        val student = createBaseStudent(109L, "Phyu Phyu")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Myanmar", 85.0, 100.0, 85.0, "A", true, "")
            ),
            strongestSubjects = listOf("Myanmar"),
            improvementSubjects = emptyList(),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(85.0, 100.0, 85.0, "A", 3, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 94.0, 6.0, 0.0, 25, true, "Satisfactory"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val prompt = aiEngine.buildFactsPrompt(facts, "MYANMAR")
        assertTrue(prompt.contains("Holistic Assessment: No holistic ratings recorded for this period."))
        val result = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertNotNull(result.teacherComment)
    }

    // Scenario J: Myanmar language output
    @Test
    fun testScenarioJ_MyanmarLanguageOutput() {
        val student = createBaseStudent(110L, "Thant Zin")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("English", 82.0, 100.0, 82.0, "A", true, "")
            ),
            strongestSubjects = listOf("English"),
            improvementSubjects = emptyList(),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(82.0, 100.0, 82.0, "A", 4, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 95.0, 5.0, 0.0, 25, true, "Excellent"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val result = aiEngine.generateDeterministicGroundedComment(facts, "MYANMAR")
        assertEquals("MYANMAR", result.sourceLanguage)
        assertTrue(result.teacherComment.contains("ပျမ်းမျှအမှတ် 82.0%"))
        assertTrue(result.parentSuggestion.contains("မိဘများအနေဖြင့်"))
    }

    // Scenario K: English language output
    @Test
    fun testScenarioK_EnglishLanguageOutput() {
        val student = createBaseStudent(111L, "David")
        val facts = StudentReportFacts(
            studentId = student.id,
            studentName = student.name,
            academicYear = "2026-2027",
            grade = "G4",
            className = "Room A",
            periodName = "July",
            subjectResults = listOf(
                SubjectFact("Mathematics", 88.0, 100.0, 88.0, "A", true, "Distinction"),
                SubjectFact("Science", 58.0, 100.0, 58.0, "D", true, "")
            ),
            strongestSubjects = listOf("Mathematics"),
            improvementSubjects = listOf("Science"),
            assessmentResults = emptyList(),
            assessmentTrends = emptyList(),
            overallPerformance = OverallPerformanceFact(146.0, 200.0, 73.0, "B", 10, 30, "PASSED"),
            attendanceSummary = AttendanceSummaryFact("July", 96.0, 4.0, 0.0, 25, true, "Excellent"),
            holisticAssessmentSummary = HolisticAssessmentSummaryFact(emptyList(), 0.0, 0.0, 0.0, 0.0, "No Holistic Data", emptyList(), emptyList())
        )

        val result = aiEngine.generateDeterministicGroundedComment(facts, "ENGLISH")
        assertEquals("ENGLISH", result.sourceLanguage)
        assertTrue(result.teacherComment.contains("David achieved an overall average of 73.0% (Grade B)"))
        assertTrue(result.teacherComment.contains("Demonstrated commendable proficiency in Mathematics"))
        assertTrue(result.teacherComment.contains("Requires dedicated reinforcement in Science"))
        assertTrue(result.parentSuggestion.contains("Parents are kindly encouraged to support daily home revision in Science"))
    }
}
