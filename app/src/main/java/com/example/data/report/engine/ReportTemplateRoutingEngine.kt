package com.example.data.report.engine

import com.example.data.local.entity.ReportTemplateType
import com.example.data.local.entity.StudentEntity
import com.example.data.report.rules.ReportSelectionRules

/**
 * Report Template Routing Decision resulting from Grade, Student, and Month selections.
 */
data class ReportTemplateRoutingDecision(
    val selectedTemplate: ReportTemplateType,
    val templateName: String,
    val gradeName: String,
    val studentId: Long?,
    val studentName: String?,
    val selectedMonth: String,
    val ruleMatched: String,
    val templateDescription: String
)

/**
 * Report Template Engine responsible solely for resolving template selection logic
 * based on Grade, Student, and Month choices.
 *
 * Routing Rules:
 * - Kindergarten (Grade KG) -> Kindergarten Report Template
 * - Primary (Grades G1, G2, G3, G4) -> Primary Report Template
 * - Secondary (Grades G5, G6, G7, G8, G9) -> Secondary Report Template
 * - High School (Grades G10, G11, G12) -> High School Report Template
 */
object ReportTemplateRoutingEngine {

    /**
     * Determines which Report Template to use when Grade, Student, and Month are selected.
     */
    fun routeTemplate(
        gradeName: String,
        studentId: Long? = null,
        studentName: String? = null,
        selectedMonth: String = "All Months"
    ): ReportTemplateRoutingDecision {
        val ruleResult = ReportSelectionRules.evaluateTemplate(gradeName = gradeName)

        return ReportTemplateRoutingDecision(
            selectedTemplate = ruleResult.selectedTemplate,
            templateName = ruleResult.selectedTemplate.displayName,
            gradeName = gradeName,
            studentId = studentId,
            studentName = studentName,
            selectedMonth = selectedMonth,
            ruleMatched = ruleResult.ruleMatched,
            templateDescription = ruleResult.templateDescription
        )
    }

    /**
     * Helper overload for a given StudentEntity and Month.
     */
    fun routeTemplateForStudent(
        student: StudentEntity,
        selectedMonth: String = "All Months"
    ): ReportTemplateRoutingDecision {
        return routeTemplate(
            gradeName = student.gradeName,
            studentId = student.id,
            studentName = student.name,
            selectedMonth = selectedMonth
        )
    }
}
