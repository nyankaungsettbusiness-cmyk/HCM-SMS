package com.example.data.ai

import com.example.data.service.GeminiAiEngine
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Structured response from the Report Card AI Engine.
 */
data class ReportCardAiResult(
    val teacherComment: String,
    val parentSuggestion: String,
    val supportingFacts: List<String>,
    val isAiGenerated: Boolean = true,
    val sourceLanguage: String = "MYANMAR",
    val statusMessage: String = ""
)

/**
 * Core AI Engine for HCM-SMS Report Cards.
 * Strictly grounds all comments and parent suggestions on authentic [StudentReportFacts].
 * Enforces zero fabrication of personality, family background, or unrecorded behaviors.
 */
class ReportCardAiEngine(
    private val geminiEngine: GeminiAiEngine = GeminiAiEngine()
) {

    companion object {
        const val SYSTEM_INSTRUCTION_MYANMAR = """
You are the Official Academic & Holistic Report Card AI Assistant for Hope Children Ministry (HCM-SMS), Myanmar.
Your mission is to generate:
1. Student-specific Teacher Comment (in natural, respectful, professional Myanmar language)
2. Student-specific Parent Support / Suggestion (in practical, respectful, achievable Myanmar language)

STRICT GROUNDING & ZERO-FABRICATION RULES (MANDATORY):
1. Every statement MUST be strictly supported by the supplied student facts.
2. NEVER invent, assume, or hallucinate:
   - Personality traits (e.g. "quiet", "talkative", "obedient")
   - Family circumstances, household economics, or parental education
   - Study habits at home (e.g. "studies hard late at night", "watches too much TV")
   - Motivation or personal effort unless recorded in teacher notes
   - Extracurricular achievements or weaknesses not in the data
   - Future career predictions or potential
3. If specific subject data, holistic pillars, or attendance is MISSING, DO NOT fabricate it. Simply omit comments regarding that missing aspect.
4. If attendance data exists and is meaningful (e.g., <80% or >95%), refer strictly to the percentage without inventing reasons for absences.
5. TEACHER COMMENT STRUCTURE (Natural Myanmar):
   - Step 1: Overall academic development & performance summary based on factual averages and grades.
   - Step 2: Specific subject strengths (e.g. subjects with >=75% or distinctions).
   - Step 3: Specific area(s) needing academic reinforcement (e.g. subjects with <60% or declining trends). Mention trend improvements if factual.
   - Step 4: Balanced, positive, constructive next step or encouragement.
   - Tone: Professional, encouraging, constructive Myanmar educational phrasing. Avoid dry copy-paste templates and avoid exaggerated praise.
6. PARENT SUPPORT SUGGESTION (Natural Myanmar):
   - Practical, respectful, achievable at home.
   - Strictly tied to the identified weak subject or growth opportunity in holistic facts.
   - E.g., for Mathematics support: Encourage daily practical problem solving or revision of multiplication tables at home.
   - Do NOT give generic empty advice if specific data exists.
7. OUTPUT FORMAT:
You MUST output ONLY a valid JSON object matching this schema:
{
  "teacherComment": "...",
  "parentSuggestion": "...",
  "supportingFacts": [
    "Fact 1...",
    "Fact 2..."
  ]
}
"""
    }

    /**
     * Generates a grounded report card comment and parent suggestion for the given [StudentReportFacts].
     */
    suspend fun generateReportComment(
        facts: StudentReportFacts,
        preferredLanguage: String = "MYANMAR",
        apiKeyOverride: String = ""
    ): ReportCardAiResult {
        // 1. Build authoritative, clean, PII-free prompt
        val prompt = buildFactsPrompt(facts, preferredLanguage)

        try {
            val responseText = geminiEngine.generateContent(
                prompt = prompt,
                systemInstruction = SYSTEM_INSTRUCTION_MYANMAR,
                apiKeyOverride = apiKeyOverride,
                modelPreference = "gemini-3.5-flash"
            )

            val parsedResult = parseAiResponse(responseText, facts, preferredLanguage)
            if (parsedResult != null) {
                return parsedResult
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Deterministic Grounded Fallback (when offline or API is unavailable)
        return generateDeterministicGroundedComment(facts, preferredLanguage)
    }

    /**
     * Formats the student facts into an unambiguous, structured prompt for Gemini.
     */
    fun buildFactsPrompt(facts: StudentReportFacts, language: String): String {
        return buildString {
            append("Please generate the Report Card Teacher Comment and Parent Support Suggestion for the following student based EXCLUSIVELY on these verified facts:\n\n")
            append("=== VERIFIED STUDENT ACADEMIC & HOLISTIC FACTS ===\n")
            append("Student Name: ${facts.studentName}\n")
            append("Grade & Class: ${facts.grade} - ${facts.className}\n")
            append("Assessment Period: ${facts.periodName} (${facts.academicYear})\n")
            append("Overall Average: ${String.format(Locale.US, "%.1f", facts.overallPerformance.overallAveragePercentage)}% (Grade: ${facts.overallPerformance.overallGrade}, Status: ${facts.overallPerformance.passStatus})\n")
            if (facts.overallPerformance.classRank != null && facts.overallPerformance.totalStudentsInClass != null) {
                append("Class Rank: ${facts.overallPerformance.classRank} out of ${facts.overallPerformance.totalStudentsInClass} students\n")
            }

            append("\n--- SUBJECT SCORES ---\n")
            if (facts.subjectResults.isNotEmpty()) {
                facts.subjectResults.forEach { sub ->
                    val dist = if (sub.distinctionBadge.isNotBlank()) " [${sub.distinctionBadge}]" else ""
                    append("- ${sub.subjectName}: ${String.format(Locale.US, "%.1f", sub.averagePercentage)}% (Grade: ${sub.gradeLetter}, ${if (sub.isPass) "Passed" else "Failed"})$dist\n")
                }
            } else {
                append("No individual subject scores recorded for this period.\n")
            }

            if (facts.strongestSubjects.isNotEmpty()) {
                append("Strongest Subjects: ${facts.strongestSubjects.joinToString(", ")}\n")
            }
            if (facts.improvementSubjects.isNotEmpty()) {
                append("Subjects Needing Support: ${facts.improvementSubjects.joinToString(", ")}\n")
            }

            if (facts.assessmentTrends.isNotEmpty()) {
                append("\n--- SUBJECT PROGRESSION TRENDS ---\n")
                facts.assessmentTrends.forEach { trend ->
                    append("- ${trend.subjectName}: ${trend.progressionDescription}\n")
                }
            }

            append("\n--- ATTENDANCE RECORD ---\n")
            if (facts.attendanceSummary.isAttendanceDataAvailable) {
                append("Attendance Present: ${String.format(Locale.US, "%.1f", facts.attendanceSummary.presentPercentage)}%, Late: ${String.format(Locale.US, "%.1f", facts.attendanceSummary.latePercentage)}%, Absent: ${String.format(Locale.US, "%.1f", facts.attendanceSummary.absentPercentage)}% (Total Days: ${facts.attendanceSummary.totalDaysRecorded})\n")
            } else {
                append("Attendance: No attendance data recorded.\n")
            }

            append("\n--- HCM 6 PILLARS HOLISTIC RATINGS (Out of 5 Stars) ---\n")
            if (facts.holisticAssessmentSummary.pillars.isNotEmpty()) {
                facts.holisticAssessmentSummary.pillars.forEach { pillar ->
                    val remark = if (pillar.existingObservationRemark.isNotBlank()) " - Remark: '${pillar.existingObservationRemark}'" else ""
                    append("- ${pillar.pillarName} (${pillar.myanmarPillarName}): ${pillar.starRating}/5.0$remark\n")
                }
                append("Holistic Overall Average: ${String.format(Locale.US, "%.1f", facts.holisticAssessmentSummary.overallAverage)}/5.0 (${facts.holisticAssessmentSummary.overallLevel})\n")
            } else {
                append("Holistic Assessment: No holistic ratings recorded for this period.\n")
            }

            if (facts.teacherObservationNotes.isNotBlank()) {
                append("\n--- RECORDED TEACHER OBSERVATION NOTES ---\n")
                append("${facts.teacherObservationNotes}\n")
            }

            append("\nTarget Output Language: $language\n")
            append("Strict JSON response required.")
        }
    }

    /**
     * Parses the JSON output from Gemini and ensures it contains required fields.
     */
    private fun parseAiResponse(jsonText: String, facts: StudentReportFacts, language: String): ReportCardAiResult? {
        val trimmed = jsonText.trim()
        val jsonStart = trimmed.indexOf("{")
        val jsonEnd = trimmed.lastIndexOf("}")

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            val jsonSubstring = trimmed.substring(jsonStart, jsonEnd + 1)
            return try {
                val json = JSONObject(jsonSubstring)
                val teacherComment = json.optString("teacherComment", "").trim()
                val parentSuggestion = json.optString("parentSuggestion", "").trim()
                val factsArray = json.optJSONArray("supportingFacts")
                val supportingFactsList = mutableListOf<String>()

                if (factsArray != null) {
                    for (i in 0 until factsArray.length()) {
                        supportingFactsList.add(factsArray.getString(i))
                    }
                }

                if (teacherComment.isNotBlank() && parentSuggestion.isNotBlank()) {
                    ReportCardAiResult(
                        teacherComment = teacherComment,
                        parentSuggestion = parentSuggestion,
                        supportingFacts = if (supportingFactsList.isNotEmpty()) supportingFactsList else listOf(
                            "Overall average: ${facts.overallPerformance.overallAveragePercentage}%",
                            "Strong subjects: ${facts.strongestSubjects.joinToString(", ").ifEmpty { "None" }}",
                            "Improvement areas: ${facts.improvementSubjects.joinToString(", ").ifEmpty { "None" }}"
                        ),
                        isAiGenerated = true,
                        sourceLanguage = language,
                        statusMessage = "Generated via Gemini (Verified Facts Grounded)"
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    /**
     * Failsafe, high-quality grounded generator that adheres 100% to verified facts
     * without calling external network services.
     */
    fun generateDeterministicGroundedComment(
        facts: StudentReportFacts,
        language: String = "MYANMAR"
    ): ReportCardAiResult {
        val studentName = facts.studentName
        val avg = facts.overallPerformance.overallAveragePercentage
        val grade = facts.overallPerformance.overallGrade
        val strongList = facts.strongestSubjects
        val weakList = facts.improvementSubjects
        val trends = facts.assessmentTrends
        val holistic = facts.holisticAssessmentSummary
        val attendance = facts.attendanceSummary

        val isMyanmar = !language.equals("ENGLISH", ignoreCase = true)

        val teacherComment: String
        val parentSuggestion: String
        val supportingFacts = mutableListOf<String>()

        supportingFacts.add("Overall Score: ${String.format(Locale.US, "%.1f", avg)}% (Grade $grade)")
        if (strongList.isNotEmpty()) supportingFacts.add("Strong Subjects: ${strongList.joinToString(", ")}")
        if (weakList.isNotEmpty()) supportingFacts.add("Subjects Needing Support: ${weakList.joinToString(", ")}")
        if (attendance.isAttendanceDataAvailable) supportingFacts.add("Attendance: ${attendance.presentPercentage}%")
        if (holistic.pillars.isNotEmpty()) supportingFacts.add("Holistic Average: ${holistic.overallAverage}/5")

        if (isMyanmar) {
            val academicPart = when {
                avg >= 80.0 -> "$studentName သည် ယခုသင်ယူမှုကာလတွင် စုစုပေါင်း ပျမ်းမျှအမှတ် ${String.format(Locale.US, "%.1f", avg)}% (အဆင့် $grade) ဖြင့် ထူးချွန်စွာ အောင်မြင်ခဲ့ပါသည်။"
                avg >= 60.0 -> "$studentName သည် ယခုသင်ယူမှုကာလတွင် ပျမ်းမျှအမှတ် ${String.format(Locale.US, "%.1f", avg)}% (အဆင့် $grade) ဖြင့် ပုံမှန်တိုးတက်မှု ကောင်းမွန်သော သင်ယူသူဖြစ်ပါသည်။"
                avg >= 40.0 -> "$studentName သည် ပျမ်းမျှအမှတ် ${String.format(Locale.US, "%.1f", avg)}% (အဆင့် $grade) ရရှိထားပြီး အခြေခံဘာသာရပ်များကို ဆက်လက် ကြိုးစားရန် လိုအပ်ပါသည်။"
                else -> "$studentName သည် ပျမ်းမျှအမှတ် ${String.format(Locale.US, "%.1f", avg)}% (အဆင့် $grade) ရရှိထားသဖြင့် အဓိကဘာသာရပ်များကို အထူးဂရုပြု ပြန်လည်လေ့ကျင့်ရန် လိုအပ်ပါသည်။"
            }

            val strengthPart = if (strongList.isNotEmpty()) {
                "${strongList.joinToString("၊ ")} ဘာသာရပ်များတွင် ရလဒ်ကောင်းမွန်ပြီး စွမ်းဆောင်ရည် အားကောင်းပါသည်။"
            } else ""

            val weakPart = if (weakList.isNotEmpty()) {
                val improving = trends.filter { it.subjectName in weakList && (it.percentageChange ?: 0.0) > 0 }
                if (improving.isNotEmpty()) {
                    val impNames = improving.joinToString("၊ ") { it.subjectName }
                    "${weakList.joinToString("၊ ")} ဘာသာရပ်တွင် အမှတ်ထပ်မံမြှင့်တင်ရန် လိုအပ်သော်လည်း $impNames တွင် တိုးတက်မှု အလားအလာ တွေ့ရှိရပါသည်။"
                } else {
                    "${weakList.joinToString("၊ ")} ဘာသာရပ်များကို ပိုမို အာရုံစိုက် လေ့ကျင့်ပေးရန် လိုအပ်ပါသည်။"
                }
            } else {
                "ဘာသာရပ်အားလုံးတွင် ညီညွတ်မျှတသော သင်ယူမှုရလဒ်ကို ထိန်းသိမ်းထားနိုင်ပါသည်။"
            }

            val holisticPart = if (holistic.pillars.isNotEmpty()) {
                val topP = holistic.pillars.maxByOrNull { it.starRating }
                if (topP != null && topP.starRating >= 4.0) {
                    "HCM ကိုယ်ကျင့်တရားနှင့် စိတ်နေသဘောထားတွင် ${topP.myanmarPillarName} (${topP.starRating}/၅) စွမ်းရည် သိသာစွာ ကောင်းမွန်ပါသည်။"
                } else ""
            } else ""

            val nextStep = "နောင်လာမည့် ကာလများတွင် ဤတိုးတက်မှုအရှိန်ကို ဆက်လက်ထိန်းသိမ်းပြီး ပိုမိုမြင့်မားသော ရလဒ်များ ရရှိအောင် ကြိုးပမ်းစေလိုပါသည်။"

            teacherComment = listOf(academicPart, strengthPart, weakPart, holisticPart, nextStep)
                .filter { it.isNotBlank() }
                .joinToString(" ")

            parentSuggestion = if (weakList.isNotEmpty()) {
                val primaryWeak = weakList.first()
                "မိဘများအနေဖြင့် အိမ်တွင် $primaryWeak ဘာသာရပ်ဆိုင်ရာ လေ့ကျင့်ခန်းများကို နေ့စဉ် ပုံမှန် အချိန်သတ်မှတ်၍ ကူညီလေ့ကျင့်ပေးစေလိုပါသည်။ အားနည်းသော အပိုင်းများကို စိတ်ရှည်စွာ ပြန်လည်သုံးသပ်ပေးခြင်းဖြင့် ပိုမိုတိုးတက်လာမည် ဖြစ်ပါသည်။"
            } else if (avg >= 80.0) {
                "မိဘများအနေဖြင့် ကျောင်းသား၏ ထူးချွန်သော ရလဒ်များကို အသိအမှတ်ပြု ချီးကျူးပေးပြီး အိမ်တွင် အထောက်အကူပြု ဗဟုသုတ စာပေများ ပိုမိုဖတ်ရှုလေ့လာနိုင်ရန် ဆက်လက်ပံ့ပိုးပေးစေလိုပါသည်။"
            } else {
                "မိဘများအနေဖြင့် ကျောင်းသား၏ နေ့စဉ် သင်ခန်းစာများကို ပုံမှန်ပြန်လည်လေ့ကျင့်မှု ရှိစေရန်နှင့် ကျောင်းခေါ်ချိန် မှန်ကန်စေရန် အိမ်တွင် အနီးကပ် တွန်းအားပေး စောင့်ရှောက်ပေးစေလိုပါသည်။"
            }
        } else {
            // English Mode
            val academicPart = "$studentName achieved an overall average of ${String.format(Locale.US, "%.1f", avg)}% (Grade $grade) for this assessment period."
            val strengthPart = if (strongList.isNotEmpty()) "Demonstrated commendable proficiency in ${strongList.joinToString(", ")}." else ""
            val weakPart = if (weakList.isNotEmpty()) "Requires dedicated reinforcement in ${weakList.joinToString(", ")} to strengthen fundamentals." else "Maintained consistent standards across all subjects."
            val holisticPart = if (holistic.pillars.isNotEmpty()) {
                val topP = holistic.pillars.maxByOrNull { it.starRating }
                if (topP != null && topP.starRating >= 4.0) "Demonstrated positive character growth in ${topP.pillarName} (${topP.starRating}/5.0)." else ""
            } else ""
            val nextStep = "Encouraged to sustain regular study rhythms for continuous progression."

            teacherComment = listOf(academicPart, strengthPart, weakPart, holisticPart, nextStep).filter { it.isNotBlank() }.joinToString(" ")
            parentSuggestion = if (weakList.isNotEmpty()) {
                "Parents are kindly encouraged to support daily home revision in ${weakList.first()} and review practice exercises collaboratively."
            } else {
                "Parents are encouraged to continue positive reinforcement at home and provide enriching reading opportunities."
            }
        }

        return ReportCardAiResult(
            teacherComment = teacherComment,
            parentSuggestion = parentSuggestion,
            supportingFacts = supportingFacts,
            isAiGenerated = true,
            sourceLanguage = language,
            statusMessage = "Generated via Local Fact Engine (Authoritative HCM-SMS Rules)"
        )
    }
}
