package com.example.data.report.rules

import com.example.data.local.entity.ReportTemplateType
import com.example.data.report.ReportRuleEvaluationResult

object ReportSelectionRules {

    fun evaluateTemplate(gradeName: String, educationLevel: String? = null): ReportRuleEvaluationResult {
        val normalizedGrade = gradeName.trim().uppercase()
        val normalizedLevel = educationLevel?.trim()?.uppercase() ?: ""

        return when {
            // Kindergarten Rule
            normalizedGrade == "KG" || normalizedGrade.contains("KINDERGARTEN") || normalizedGrade.contains("PRESCHOOL") -> {
                ReportRuleEvaluationResult(
                    selectedTemplate = ReportTemplateType.KINDERGARTEN,
                    ruleMatched = "RULE-KG-01: Kindergarten Foundational Learning Rule",
                    gradeName = gradeName,
                    educationLevel = "PRIMARY (KG)",
                    appliedCriteria = listOf(
                        "Foundational literacy & numeracy observation",
                        "Behavioral HCM Assessment (Honesty, Curiosity, Mindfulness)",
                        "Monthly attendance tracking & habit building",
                        "Teacher qualitative development feedback"
                    ),
                    templateDescription = "Kindergarten Template tailored for early childhood development and foundational learning without traditional letter grades."
                )
            }

            // Primary Rule (G1 to G4)
            normalizedGrade in listOf("G1", "G2", "G3", "G4", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4") ||
                    (normalizedLevel == "PRIMARY" && normalizedGrade != "G5") -> {
                ReportRuleEvaluationResult(
                    selectedTemplate = ReportTemplateType.PRIMARY,
                    ruleMatched = "RULE-PRI-02: Primary Education Performance Rule (G1–G4)",
                    gradeName = gradeName,
                    educationLevel = "PRIMARY",
                    appliedCriteria = listOf(
                        "Academic subject proficiency (Myanmar, English, Math, Science, Social)",
                        "Dynamically discovered Monthly & CET Progress Tests",
                        "Cumulative attendance & monthly attendance breakdown",
                        "HCM Behavioral ratings (5-star scale)",
                        "Teacher strength & improvement feedback"
                    ),
                    templateDescription = "Primary Template designed for Grades 1 to 4 with subject rating stars, monthly test progress, and growth indicators."
                )
            }

            // Secondary Rule (G5 to G9)
            normalizedGrade in listOf("G5", "G6", "G7", "G8", "G9", "GRADE 5", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") ||
                    normalizedLevel == "SECONDARY" -> {
                ReportRuleEvaluationResult(
                    selectedTemplate = ReportTemplateType.SECONDARY,
                    ruleMatched = "RULE-SEC-03: Secondary Academic & Pilot Assessment Rule (G5–G9)",
                    gradeName = gradeName,
                    educationLevel = "SECONDARY",
                    appliedCriteria = listOf(
                        "Multi-subject academic scoring with class ranking",
                        "Dynamically integrated Monthly, Pilot, and CET Examinations",
                        "SGI (Student Growth Index) & HCM holistic assessment",
                        "Attendance tracking & term breakdown",
                        "Class teacher guidance & parent recommendations"
                    ),
                    templateDescription = "Secondary Template designed for Middle School / Secondary Grades (G5–G9) featuring comprehensive test performance and SGI metrics."
                )
            }

            // High School Rule (G10 to G12)
            normalizedGrade in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") ||
                    normalizedLevel == "HIGH_SCHOOL" -> {
                ReportRuleEvaluationResult(
                    selectedTemplate = ReportTemplateType.HIGH_SCHOOL,
                    ruleMatched = "RULE-HS-01: High School Academic, Weekly & Pilot Assessment Rule (G10–G12)",
                    gradeName = gradeName,
                    educationLevel = "HIGH_SCHOOL",
                    appliedCriteria = listOf(
                        "Multi-subject academic scoring with track subjects (STEAMS)",
                        "Weekly Tests & Lesson Completion Tests",
                        "Pilot Tests & CET Examinations",
                        "SGI & HCM holistic assessment",
                        "Attendance tracking & teacher recommendations"
                    ),
                    templateDescription = "High School Template for Grades G10–G12."
                )
            }

            // Default Fallback Rule based on grade string pattern
            else -> {
                val numericPart = normalizedGrade.replace(Regex("[^0-9]"), "").toIntOrNull()
                val selectedType = when {
                    numericPart == null -> ReportTemplateType.PRIMARY
                    numericPart <= 4 -> ReportTemplateType.PRIMARY
                    else -> ReportTemplateType.SECONDARY
                }
                ReportRuleEvaluationResult(
                    selectedTemplate = selectedType,
                    ruleMatched = "RULE-GEN-05: Dynamic Grade Pattern Recognition Rule",
                    gradeName = gradeName,
                    educationLevel = selectedType.gradeRange,
                    appliedCriteria = listOf(
                        "Dynamic pattern matching on grade '$gradeName'",
                        "Automated template binding to ${selectedType.displayName}",
                        "Dynamic examination record aggregation"
                    ),
                    templateDescription = "Dynamic template assigned by grade string matching rules."
                )
            }
        }
    }
}
