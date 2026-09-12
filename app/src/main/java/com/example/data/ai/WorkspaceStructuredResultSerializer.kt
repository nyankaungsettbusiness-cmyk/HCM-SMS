package com.example.data.ai

import com.example.ui.screens.ai.workspace.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust, zero-dependency JSON serializer and deserializer for [WorkspaceStructuredResult].
 * Encodes complete curriculum grounding, source citations, question options, rubrics,
 * manual edit flags, and timestamps for persistent storage in [AiHistoryEntity.generatedResult].
 */
object WorkspaceStructuredResultSerializer {

    fun serialize(
        result: WorkspaceStructuredResult,
        teacherUsername: String = "",
        createdAt: Long = result.createdAt,
        editedAt: Long = result.editedAt
    ): String {
        val root = JSONObject()
        root.put("title", result.title)
        root.put("academicYear", result.academicYear)
        root.put("grade", result.grade)
        root.put("subject", result.subject)
        root.put("examType", result.examType)
        root.put("durationMinutes", result.durationMinutes)
        root.put("totalMarks", result.totalMarks)
        root.put("isCurriculumVerified", result.isCurriculumVerified)
        root.put("validationSummary", result.validationSummary)
        root.put("styleReferenceNote", result.styleReferenceNote)
        root.put("rawContentMarkdown", result.rawContentMarkdown)
        root.put("teacherUsername", teacherUsername)
        root.put("createdAt", createdAt)
        root.put("editedAt", editedAt)

        val hasManualEdit = result.sections.any { s -> s.questions.any { it.isManuallyEdited } }
        root.put("isManuallyEdited", hasManualEdit)

        // General Instructions
        val instrArr = JSONArray()
        result.generalInstructions.forEach { instrArr.put(it) }
        root.put("generalInstructions", instrArr)

        // Source Citations
        val citationsArr = JSONArray()
        result.sourceCitations.forEach { c ->
            val cObj = JSONObject()
            cObj.put("documentTitle", c.documentTitle)
            cObj.put("gradeLevel", c.gradeLevel)
            cObj.put("subject", c.subject)
            cObj.put("chapterUnit", c.chapterUnit)
            cObj.put("sectionTopic", c.sectionTopic)
            cObj.put("pageRange", c.pageRange)
            cObj.put("tier", c.tier.name)
            citationsArr.put(cObj)
        }
        root.put("sourceCitations", citationsArr)

        // Sections & Questions
        val sectionsArr = JSONArray()
        result.sections.forEach { s ->
            val sObj = JSONObject()
            sObj.put("sectionName", s.sectionName)
            sObj.put("sectionInstruction", s.sectionInstruction)
            sObj.put("sectionMarks", s.sectionMarks)

            val qArr = JSONArray()
            s.questions.forEach { q ->
                val qObj = JSONObject()
                qObj.put("id", q.id)
                qObj.put("questionNumber", q.questionNumber)
                qObj.put("sectionName", q.sectionName)
                qObj.put("questionType", q.questionType)
                qObj.put("questionText", q.questionText)
                qObj.put("correctAnswer", q.correctAnswer)
                qObj.put("markingGuide", q.markingGuide)
                qObj.put("marks", q.marks)
                qObj.put("difficulty", q.difficulty)
                qObj.put("isManuallyEdited", q.isManuallyEdited)

                val optArr = JSONArray()
                q.options.forEach { optArr.put(it) }
                qObj.put("options", optArr)

                val matchArr = JSONArray()
                q.matchingPairs.forEach { pair ->
                    val mObj = JSONObject()
                    mObj.put("left", pair.first)
                    mObj.put("right", pair.second)
                    matchArr.put(mObj)
                }
                qObj.put("matchingPairs", matchArr)

                if (q.sourceCitation != null) {
                    val qcObj = JSONObject()
                    qcObj.put("documentTitle", q.sourceCitation.documentTitle)
                    qcObj.put("gradeLevel", q.sourceCitation.gradeLevel)
                    qcObj.put("subject", q.sourceCitation.subject)
                    qcObj.put("chapterUnit", q.sourceCitation.chapterUnit)
                    qcObj.put("sectionTopic", q.sourceCitation.sectionTopic)
                    qcObj.put("pageRange", q.sourceCitation.pageRange)
                    qcObj.put("tier", q.sourceCitation.tier.name)
                    qObj.put("sourceCitation", qcObj)
                }

                qArr.put(qObj)
            }
            sObj.put("questions", qArr)
            sectionsArr.put(sObj)
        }
        root.put("sections", sectionsArr)

        return root.toString(2)
    }

    fun deserialize(jsonStr: String): WorkspaceStructuredResult? {
        return try {
            val root = JSONObject(jsonStr)
            val title = root.optString("title", "Saved Assessment")
            val academicYear = root.optString("academicYear", "2026–2027")
            val grade = root.optString("grade", "G5")
            val subject = root.optString("subject", "English")
            val examType = root.optString("examType", "Assessment")
            val durationMinutes = root.optInt("durationMinutes", 60)
            val totalMarks = root.optInt("totalMarks", 50)
            val isCurriculumVerified = root.optBoolean("isCurriculumVerified", true)
            val validationSummary = root.optString("validationSummary", "")
            val styleReferenceNote = root.optString("styleReferenceNote", "")
            val rawContentMarkdown = root.optString("rawContentMarkdown", "")
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val editedAt = root.optLong("editedAt", System.currentTimeMillis())

            val generalInstructions = mutableListOf<String>()
            val instrArr = root.optJSONArray("generalInstructions")
            if (instrArr != null) {
                for (i in 0 until instrArr.length()) {
                    generalInstructions.add(instrArr.getString(i))
                }
            }

            val sourceCitations = mutableListOf<SourceCitationUi>()
            val citationsArr = root.optJSONArray("sourceCitations")
            if (citationsArr != null) {
                for (i in 0 until citationsArr.length()) {
                    val cObj = citationsArr.getJSONObject(i)
                    val tierStr = cObj.optString("tier", SourceTier.PRIMARY_TEXTBOOK.name)
                    val tier = try { SourceTier.valueOf(tierStr) } catch (e: Exception) { SourceTier.PRIMARY_TEXTBOOK }
                    sourceCitations.add(
                        SourceCitationUi(
                            documentTitle = cObj.optString("documentTitle", ""),
                            gradeLevel = cObj.optString("gradeLevel", grade),
                            subject = cObj.optString("subject", subject),
                            chapterUnit = cObj.optString("chapterUnit", ""),
                            sectionTopic = cObj.optString("sectionTopic", ""),
                            pageRange = cObj.optString("pageRange", ""),
                            tier = tier
                        )
                    )
                }
            }

            val sections = mutableListOf<QuestionSectionUi>()
            val sectionsArr = root.optJSONArray("sections")
            if (sectionsArr != null) {
                for (sIdx in 0 until sectionsArr.length()) {
                    val sObj = sectionsArr.getJSONObject(sIdx)
                    val sectionName = sObj.optString("sectionName", "")
                    val sectionInstruction = sObj.optString("sectionInstruction", "")
                    val sectionMarks = sObj.optInt("sectionMarks", 0)

                    val questions = mutableListOf<QuestionItemUi>()
                    val qArr = sObj.optJSONArray("questions")
                    if (qArr != null) {
                        for (qIdx in 0 until qArr.length()) {
                            val qObj = qArr.getJSONObject(qIdx)
                            val options = mutableListOf<String>()
                            val optArr = qObj.optJSONArray("options")
                            if (optArr != null) {
                                for (o in 0 until optArr.length()) {
                                    options.add(optArr.getString(o))
                                }
                            }

                            val matchPairs = mutableListOf<Pair<String, String>>()
                            val matchArr = qObj.optJSONArray("matchingPairs")
                            if (matchArr != null) {
                                for (m in 0 until matchArr.length()) {
                                    val mObj = matchArr.getJSONObject(m)
                                    matchPairs.add(Pair(mObj.optString("left"), mObj.optString("right")))
                                }
                            }

                            val qcObj = qObj.optJSONObject("sourceCitation")
                            val citation = if (qcObj != null) {
                                val tierStr = qcObj.optString("tier", SourceTier.PRIMARY_TEXTBOOK.name)
                                val tier = try { SourceTier.valueOf(tierStr) } catch (e: Exception) { SourceTier.PRIMARY_TEXTBOOK }
                                SourceCitationUi(
                                    documentTitle = qcObj.optString("documentTitle", ""),
                                    gradeLevel = qcObj.optString("gradeLevel", grade),
                                    subject = qcObj.optString("subject", subject),
                                    chapterUnit = qcObj.optString("chapterUnit", ""),
                                    sectionTopic = qcObj.optString("sectionTopic", ""),
                                    pageRange = qcObj.optString("pageRange", ""),
                                    tier = tier
                                )
                            } else null

                            questions.add(
                                QuestionItemUi(
                                    id = qObj.optString("id", java.util.UUID.randomUUID().toString()),
                                    questionNumber = qObj.optInt("questionNumber", qIdx + 1),
                                    sectionName = qObj.optString("sectionName", sectionName),
                                    questionType = qObj.optString("questionType", "Short Answer"),
                                    questionText = qObj.optString("questionText", ""),
                                    options = options,
                                    matchingPairs = matchPairs,
                                    correctAnswer = qObj.optString("correctAnswer", ""),
                                    markingGuide = qObj.optString("markingGuide", ""),
                                    marks = qObj.optInt("marks", 1),
                                    difficulty = qObj.optString("difficulty", "Medium"),
                                    sourceCitation = citation,
                                    isManuallyEdited = qObj.optBoolean("isManuallyEdited", false)
                                )
                            )
                        }
                    }

                    sections.add(
                        QuestionSectionUi(
                            sectionName = sectionName,
                            sectionInstruction = sectionInstruction,
                            sectionMarks = sectionMarks,
                            questions = questions
                        )
                    )
                }
            }

            WorkspaceStructuredResult(
                title = title,
                academicYear = academicYear,
                grade = grade,
                subject = subject,
                examType = examType,
                durationMinutes = durationMinutes,
                totalMarks = totalMarks,
                isCurriculumVerified = isCurriculumVerified,
                validationSummary = validationSummary,
                sourceCitations = sourceCitations,
                styleReferenceNote = styleReferenceNote,
                generalInstructions = generalInstructions,
                sections = sections,
                rawContentMarkdown = rawContentMarkdown,
                isSaved = true,
                createdAt = createdAt,
                editedAt = editedAt
            )
        } catch (e: Exception) {
            null
        }
    }
}
