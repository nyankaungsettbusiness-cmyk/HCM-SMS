package com.example.data.policy

import com.example.data.local.entity.ExamQuestionBlueprintEntity
import com.example.data.local.entity.QuestionBlueprintItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ExamQuestionBlueprintHelper {

    fun parseQuestions(json: String?): List<QuestionBlueprintItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<QuestionBlueprintItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    QuestionBlueprintItem(
                        qNo = obj.optString("qNo", "Q${i + 1}"),
                        title = obj.optString("title", ""),
                        maxMark = obj.optDouble("maxMark", 10.0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeQuestions(items: List<QuestionBlueprintItem>): String {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("qNo", item.qNo.trim())
                put("title", item.title.trim())
                put("maxMark", item.maxMark)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseQuestionMarks(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optDouble(key, 0.0)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeQuestionMarks(marks: Map<String, Double>): String {
        val obj = JSONObject()
        marks.forEach { (k, v) ->
            obj.put(k, v)
        }
        return obj.toString()
    }

    /**
     * Standard Myanmar Curriculum Question Presets for High School Subjects (G10, G11, G12)
     */
    fun getDefaultQuestionsForSubject(subjectName: String): List<QuestionBlueprintItem> {
        val lower = subjectName.lowercase().trim()
        return when {
            lower.contains("myanmar") -> listOf(
                QuestionBlueprintItem("Q1", "စကားပြေ (Prose)", 15.0),
                QuestionBlueprintItem("Q2", "ကဗျာ (Poetry)", 15.0),
                QuestionBlueprintItem("Q3", "သဒ္ဒါ (Grammar)", 20.0),
                QuestionBlueprintItem("Q4", "အလင်္ကာ (Rhetoric)", 10.0),
                QuestionBlueprintItem("Q5", "စာစီစာကုံး (Essay)", 20.0),
                QuestionBlueprintItem("Q6", "ဝတ္ထုတို / ပြဇာတ် (Literature)", 20.0)
            )
            lower.contains("english") -> listOf(
                QuestionBlueprintItem("Q1", "Grammar & Vocabulary", 20.0),
                QuestionBlueprintItem("Q2", "Reading Comprehension", 20.0),
                QuestionBlueprintItem("Q3", "Poetry / Literature", 15.0),
                QuestionBlueprintItem("Q4", "Letter / Email Writing", 15.0),
                QuestionBlueprintItem("Q5", "Essay Writing", 20.0),
                QuestionBlueprintItem("Q6", "Summary Writing", 10.0)
            )
            lower.contains("math") -> listOf(
                QuestionBlueprintItem("Q1", "Short Questions & Logic", 20.0),
                QuestionBlueprintItem("Q2", "Algebra & Functions", 20.0),
                QuestionBlueprintItem("Q3", "Calculus & Limits", 20.0),
                QuestionBlueprintItem("Q4", "Trigonometry & Geometry", 20.0),
                QuestionBlueprintItem("Q5", "Statistics & Probability", 20.0)
            )
            lower.contains("chem") -> listOf(
                QuestionBlueprintItem("Q1", "Multiple Choice & Fill-in", 20.0),
                QuestionBlueprintItem("Q2", "Physical Chemistry", 20.0),
                QuestionBlueprintItem("Q3", "Inorganic Chemistry", 20.0),
                QuestionBlueprintItem("Q4", "Organic Chemistry", 20.0),
                QuestionBlueprintItem("Q5", "Problem Solving & Equations", 20.0)
            )
            lower.contains("phys") -> listOf(
                QuestionBlueprintItem("Q1", "Conceptual Questions", 20.0),
                QuestionBlueprintItem("Q2", "Mechanics & Dynamics", 20.0),
                QuestionBlueprintItem("Q3", "Heat & Thermodynamics", 20.0),
                QuestionBlueprintItem("Q4", "Waves & Electricity", 20.0),
                QuestionBlueprintItem("Q5", "Modern & Nuclear Physics", 20.0)
            )
            lower.contains("bio") -> listOf(
                QuestionBlueprintItem("Q1", "Objective & Definitions", 20.0),
                QuestionBlueprintItem("Q2", "Cell Biology & Genetics", 20.0),
                QuestionBlueprintItem("Q3", "Plant & Animal Physiology", 20.0),
                QuestionBlueprintItem("Q4", "Ecology & Biosystem", 20.0),
                QuestionBlueprintItem("Q5", "Diagrams & Descriptions", 20.0)
            )
            lower.contains("econ") -> listOf(
                QuestionBlueprintItem("Q1", "Basic Economic Concepts", 20.0),
                QuestionBlueprintItem("Q2", "Microeconomics", 25.0),
                QuestionBlueprintItem("Q3", "Macroeconomics", 25.0),
                QuestionBlueprintItem("Q4", "Applied Economic Essay", 30.0)
            )
            lower.contains("hist") -> listOf(
                QuestionBlueprintItem("Q1", "Chronology & Dates", 20.0),
                QuestionBlueprintItem("Q2", "Ancient & Medieval Era", 25.0),
                QuestionBlueprintItem("Q3", "Modern World History", 25.0),
                QuestionBlueprintItem("Q4", "Historical Essay", 30.0)
            )
            lower.contains("geo") -> listOf(
                QuestionBlueprintItem("Q1", "Map Reading & Symbols", 20.0),
                QuestionBlueprintItem("Q2", "Physical Geography", 25.0),
                QuestionBlueprintItem("Q3", "Human & Economic Geography", 25.0),
                QuestionBlueprintItem("Q4", "Regional Geography & Essay", 30.0)
            )
            else -> listOf(
                QuestionBlueprintItem("Q1", "Section A", 20.0),
                QuestionBlueprintItem("Q2", "Section B", 20.0),
                QuestionBlueprintItem("Q3", "Section C", 20.0),
                QuestionBlueprintItem("Q4", "Section D", 20.0),
                QuestionBlueprintItem("Q5", "Section E", 20.0)
            )
        }
    }

    /**
     * Generate standard default blueprints for Grade 10, Grade 11, or Grade 12
     */
    fun getDefaultBlueprintsForGrade(grade: String): List<ExamQuestionBlueprintEntity> {
        val subjects = listOf("Myanmar", "English", "Mathematics", "Physics", "Chemistry", "Biology", "Economics")
        return subjects.map { subj ->
            val questions = getDefaultQuestionsForSubject(subj)
            val total = questions.sumOf { it.maxMark }.toInt()
            ExamQuestionBlueprintEntity(
                gradeName = grade,
                subjectName = subj,
                totalMarks = total,
                questionsJson = serializeQuestions(questions),
                uuid = UUID.randomUUID().toString(),
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
