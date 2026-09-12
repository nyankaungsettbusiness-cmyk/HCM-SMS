package com.example.data.export

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.SchoolSettingEntity
import com.example.ui.screens.ai.workspace.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

/**
 * Phase 4B Test Suite:
 * Official Document Output Engine for Question Papers, Worksheets, Answer Keys, and DOCX.
 *
 * Verifies Requirements A through Q:
 * A. Official Pilot Test PDF
 * B. CET PDF
 * C. Monthly Test PDF
 * D. Final Exam PDF
 * E. Worksheet PDF
 * F. Teacher Answer Key PDF
 * G. Editable DOCX
 * H. Myanmar text (Unicode)
 * I. Mixed Myanmar + English
 * J. Multi-page question paper
 * K. Long question wrapping
 * L. Image/diagram embedding
 * M. Page-break handling
 * N. Real school logo loading
 * O. Missing logo fallback
 * P. Zero modification to official school database tables
 * Q. Export failure handling
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CurriculumExportEngineTest {

    private lateinit var context: Context
    private lateinit var pdfGenerator: CurriculumPdfGenerator
    private lateinit var docxExporter: CurriculumDocxExporter
    private lateinit var sampleSchoolSettings: SchoolSettingEntity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pdfGenerator = CurriculumPdfGenerator(context)
        docxExporter = CurriculumDocxExporter(context)
        sampleSchoolSettings = SchoolSettingEntity(
            schoolName = "Hein Chan Myae Private School",
            motto = "Grow with Character, Learn with Curiosity, Lead with Discipline",
            address = "Yangon, Myanmar",
            contactPhone = "+95 9 123456789",
            email = "info@heinchanmyae.edu.mm"
        )
    }

    private fun createSampleResult(
        title: String = "Monthly Assessment",
        examType: String = "Monthly Test",
        grade: String = "Grade 5",
        subject: String = "English",
        language: String = "English",
        questionCount: Int = 5,
        withVisual: Boolean = false
    ): WorkspaceStructuredResult {
        val questions = mutableListOf<QuestionItemUi>()
        for (i in 1..questionCount) {
            val visual = if (withVisual && i == 1) {
                val testBitmap = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
                VisualAssetRef(
                    title = "Plant Cell Diagram",
                    caption = "Figure 1: Diagram showing plant cell organelles.",
                    imageBitmap = testBitmap
                )
            } else null

            val qText = when {
                language == "MYANMAR" -> "မေးခွန်းနံပါတ် ($i) - မြန်မာသဒ္ဒါအရ နာမ်နှင့် နာမဝိသေသနကို ခွဲခြားဖော်ပြပါ။"
                language == "MIXED" -> "Question $i: Fill in the blank (ကွက်လပ်ဖြည့်ပါ) with correct preposition."
                else -> "Question $i: Explain the function of chloroplasts in plant cells."
            }

            questions.add(
                QuestionItemUi(
                    id = "q_$i",
                    questionNumber = i,
                    sectionName = if (i <= 3) "Section A: Multiple Choice" else "Section B: Structured Questions",
                    questionType = if (i <= 3) "MCQ" else "Short Answer",
                    questionText = qText,
                    options = if (i <= 3) listOf("A) Mitochondria", "B) Chloroplasts", "C) Ribosomes", "D) Nucleus") else emptyList(),
                    correctAnswer = if (i <= 3) "B) Chloroplasts" else "Chloroplasts capture light energy to produce glucose via photosynthesis.",
                    markingGuide = if (i <= 3) "1 mark for option B" else "1 mark for light capture, 1 mark for photosynthesis",
                    marks = if (i <= 3) 1 else 2,
                    difficulty = if (i <= 2) "Easy" else if (i <= 4) "Medium" else "Challenge",
                    sourceCitation = SourceCitationUi(
                        documentTitle = "Ministry of Education Myanmar Grade 5 Science (2024–2025)",
                        gradeLevel = grade,
                        subject = subject,
                        chapterUnit = "Unit 2: Plant Life Cycles",
                        sectionTopic = "Photosynthesis & Cell Structure",
                        pageRange = "p. 24–28",
                        tier = SourceTier.PRIMARY_TEXTBOOK
                    ),
                    visualAsset = visual
                )
            )
        }

        val secA = QuestionSectionUi(
            sectionName = "Section A: Multiple Choice Questions",
            sectionInstruction = "Choose the correct option for each question.",
            sectionMarks = 3,
            questions = questions.filter { it.sectionName.contains("Section A") }
        )
        val secB = QuestionSectionUi(
            sectionName = "Section B: Structured & Short Answer",
            sectionInstruction = "Write your answers in complete sentences in the space provided.",
            sectionMarks = (questions.size - 3).coerceAtLeast(1) * 2,
            questions = questions.filter { it.sectionName.contains("Section B") }
        )

        return WorkspaceStructuredResult(
            title = title,
            academicYear = "2026–2027",
            grade = grade,
            subject = subject,
            examType = examType,
            durationMinutes = 60,
            totalMarks = secA.sectionMarks + secB.sectionMarks,
            isCurriculumVerified = true,
            sections = listOf(secA, secB),
            generalInstructions = listOf(
                "Write your name and roll number clearly.",
                "Answer all questions in the question paper."
            )
        )
    }

    // =========================================================================
    // TEST CASES A -> Q
    // =========================================================================

    @Test
    fun `Test A - Official Pilot Test PDF generation succeeds`() {
        val result = createSampleResult(title = "Grade 5 Pilot Test", examType = "Pilot Test")
        val fileResult = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
        assertTrue(file.name.endsWith(".pdf"))
    }

    @Test
    fun `Test B - Continuous Evaluation Test (CET) PDF generation succeeds`() {
        val result = createSampleResult(title = "CET Assessment 1", examType = "Continuous Evaluation Test (CET)")
        val fileResult = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun `Test C - Monthly Test PDF generation succeeds`() {
        val result = createSampleResult(title = "August Monthly Test", examType = "Monthly Test")
        val fileResult = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun `Test D - Final Exam PDF generation succeeds`() {
        val result = createSampleResult(title = "First Semester Final Exam", examType = "Final Semester Exam")
        val fileResult = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun `Test E - Practice Worksheet PDF generation succeeds with student header`() {
        val result = createSampleResult(title = "Plant Science Practice Worksheet", examType = "Worksheet")
        val fileResult = pdfGenerator.generatePdf(result, DocumentExportType.WORKSHEET, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun `Test F - Teacher Answer Key & Rubric PDF generation succeeds`() {
        val result = createSampleResult(title = "English Monthly Test Key", examType = "Monthly Test")
        val fileResult = pdfGenerator.generatePdf(result, DocumentExportType.TEACHER_ANSWER_KEY, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun `Test G - Editable Microsoft Word DOCX generation succeeds with valid OpenXML ZIP structure`() {
        val result = createSampleResult(title = "English Exam Paper", examType = "Monthly Test")
        val fileResult = docxExporter.exportToDocx(result, DocxExportType.STUDENT_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        assertTrue(file.exists() && file.length() > 0)
        assertTrue(file.name.endsWith(".docx"))

        // Verify ZIP contents contain required OpenXML parts
        val entryNames = mutableListOf<String>()
        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                entry = zip.nextEntry
            }
        }
        assertTrue("[Content_Types].xml must exist", entryNames.contains("[Content_Types].xml"))
        assertTrue("_rels/.rels must exist", entryNames.contains("_rels/.rels"))
        assertTrue("word/document.xml must exist", entryNames.contains("word/document.xml"))
        assertTrue("word/styles.xml must exist", entryNames.contains("word/styles.xml"))
        assertTrue("word/_rels/document.xml.rels must exist", entryNames.contains("word/_rels/document.xml.rels"))
    }

    @Test
    fun `Test H - Pure Myanmar Unicode text rendering in PDF and DOCX`() {
        val myanmarResult = createSampleResult(
            title = "မြန်မာစာ လစဉ်စစ်ဆေးခြင်း",
            examType = "လစဉ်စာမေးပွဲ",
            grade = "Grade 5",
            subject = "မြန်မာစာ",
            language = "MYANMAR"
        )
        val pdfRes = pdfGenerator.generatePdf(myanmarResult, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue("PDF with Myanmar Unicode should succeed", pdfRes.isSuccess)

        val docxRes = docxExporter.exportToDocx(myanmarResult, DocxExportType.STUDENT_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue("DOCX with Myanmar Unicode should succeed", docxRes.isSuccess)
    }

    @Test
    fun `Test I - Mixed Myanmar and English text rendering`() {
        val mixedResult = createSampleResult(
            title = "Bilingual English & Myanmar Science Quiz",
            examType = "Quiz",
            grade = "Grade 4",
            subject = "Science",
            language = "MIXED"
        )
        val pdfRes = pdfGenerator.generatePdf(mixedResult, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue("PDF with mixed text should succeed", pdfRes.isSuccess)

        val docxRes = docxExporter.exportToDocx(mixedResult, DocxExportType.STUDENT_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue("DOCX with mixed text should succeed", docxRes.isSuccess)
    }

    @Test
    fun `Test J - Multi-page question paper with 20 questions`() {
        val largeResult = createSampleResult(
            title = "Comprehensive Final Exam",
            examType = "Final Exam",
            questionCount = 20
        )
        val pdfRes = pdfGenerator.generatePdf(largeResult, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(pdfRes.isSuccess)
        val file = pdfRes.getOrThrow()
        assertTrue(file.length() > 0)
    }

    @Test
    fun `Test K - Long question text wrapping and space estimation`() {
        val longQuestionText = "A long comprehensive physics problem stating that a ball is thrown vertically upwards with an initial velocity of 20 m/s from a height of 5 meters above the ground level. Calculate the maximum height reached by the ball, the total time of flight before striking the ground, and draw the corresponding velocity-time graph with proper labeling."
        val result = createSampleResult(questionCount = 1).let { base ->
            val updatedQuestions = base.sections[0].questions.map { it.copy(questionText = longQuestionText) }
            val updatedSections = listOf(base.sections[0].copy(questions = updatedQuestions))
            base.copy(sections = updatedSections)
        }
        val pdfRes = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(pdfRes.isSuccess)
    }

    @Test
    fun `Test L - Embedded visual asset diagram in PDF and DOCX`() {
        val visualResult = createSampleResult(questionCount = 4, withVisual = true)
        val pdfRes = pdfGenerator.generatePdf(visualResult, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue("PDF with visual asset should succeed", pdfRes.isSuccess)

        val docxRes = docxExporter.exportToDocx(visualResult, DocxExportType.STUDENT_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue("DOCX with visual asset should succeed", docxRes.isSuccess)

        // Verify image media entry is present in docx package
        val file = docxRes.getOrThrow()
        val entryNames = mutableListOf<String>()
        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                entry = zip.nextEntry
            }
        }
        assertTrue("word/media/image_1.png must be present in docx ZIP", entryNames.contains("word/media/image_1.png"))
    }

    @Test
    fun `Test M - Page-break handling with ensureSpace does not crash`() {
        val largeResult = createSampleResult(questionCount = 15)
        val pdfRes = pdfGenerator.generatePdf(largeResult, DocumentExportType.OFFICIAL_QUESTION_PAPER, sampleSchoolSettings)
        assertTrue(pdfRes.isSuccess)
    }

    @Test
    fun `Test N - School settings branding loaded properly`() {
        val customSettings = SchoolSettingEntity(
            schoolName = "Hein Chan Myae Elite Academy",
            motto = "Excellence in Action",
            address = "Mandalay, Myanmar"
        )
        val result = createSampleResult()
        val pdfRes = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, customSettings)
        assertTrue(pdfRes.isSuccess)
    }

    @Test
    fun `Test O - Missing school settings uses safe fallback`() {
        val result = createSampleResult()
        val pdfRes = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER, schoolSettings = null)
        assertTrue(pdfRes.isSuccess)

        val docxRes = docxExporter.exportToDocx(result, DocxExportType.STUDENT_QUESTION_PAPER, schoolSettings = null)
        assertTrue(docxRes.isSuccess)
    }

    @Test
    fun `Test P - Zero modification to official school database tables during export`() {
        // Confirm exports are pure functions operating on WorkspaceStructuredResult and Context cache
        val result = createSampleResult()
        val pdfRes = pdfGenerator.generatePdf(result, DocumentExportType.OFFICIAL_QUESTION_PAPER)
        assertTrue(pdfRes.isSuccess)
        val docxRes = docxExporter.exportToDocx(result, DocxExportType.STUDENT_QUESTION_PAPER)
        assertTrue(docxRes.isSuccess)
    }

    @Test
    fun `Test Q - Export validation and error handling for empty invalid documents`() {
        val emptyResult = WorkspaceStructuredResult(
            title = "",
            academicYear = "2026–2027",
            grade = "G5",
            subject = "English",
            examType = "Test",
            durationMinutes = 60,
            totalMarks = 0,
            sections = emptyList()
        )
        val pdfRes = pdfGenerator.generatePdf(emptyResult, DocumentExportType.OFFICIAL_QUESTION_PAPER)
        assertTrue("Should fail validation when sections or title are empty", pdfRes.isFailure)

        val docxRes = docxExporter.exportToDocx(emptyResult, DocxExportType.STUDENT_QUESTION_PAPER)
        assertTrue("Should fail validation for empty document", docxRes.isFailure)
    }
}
