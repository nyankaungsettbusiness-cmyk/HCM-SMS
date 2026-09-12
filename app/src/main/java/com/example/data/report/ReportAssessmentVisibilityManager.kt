package com.example.data.report

import android.content.Context
import android.content.SharedPreferences

object ReportAssessmentVisibilityManager {
    private const val PREF_NAME = "report_assessment_visibility_prefs"

    const val KEY_MONTHLY_TEST = "show_monthly_test"
    const val KEY_WEEKLY_TEST = "show_weekly_test"
    const val KEY_PILOT_TEST = "show_pilot_test"
    const val KEY_CET_TEST = "show_cet_test"
    const val KEY_LCT = "show_lct"
    const val KEY_CUSTOM_EXAM = "show_custom_exam"

    // Specific sub-keys
    const val KEY_PILOT_1 = "show_pilot_1"
    const val KEY_PILOT_2 = "show_pilot_2"
    const val KEY_PILOT_3 = "show_pilot_3"
    const val KEY_PILOT_4 = "show_pilot_4"
    const val KEY_CET_1 = "show_cet_1"
    const val KEY_CET_2 = "show_cet_2"
    const val KEY_CET_3 = "show_cet_3"
    const val KEY_CET_4 = "show_cet_4"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isTypeVisible(context: Context, typeNameOrKey: String): Boolean {
        val prefs = getPrefs(context)
        val upper = typeNameOrKey.trim().uppercase()

        if (upper.contains("PILOT 1") || upper.contains("PILOT TEST 1")) {
            return prefs.getBoolean(KEY_PILOT_1, prefs.getBoolean(KEY_PILOT_TEST, true))
        }
        if (upper.contains("PILOT 2") || upper.contains("PILOT TEST 2")) {
            return prefs.getBoolean(KEY_PILOT_2, prefs.getBoolean(KEY_PILOT_TEST, true))
        }
        if (upper.contains("CET 1") || upper.contains("CET1")) {
            return prefs.getBoolean(KEY_CET_1, prefs.getBoolean(KEY_CET_TEST, true))
        }
        if (upper.contains("CET 2") || upper.contains("CET2")) {
            return prefs.getBoolean(KEY_CET_2, prefs.getBoolean(KEY_CET_TEST, true))
        }

        return when {
            upper.contains("MONTHLY") -> prefs.getBoolean(KEY_MONTHLY_TEST, true)
            upper.contains("WEEKLY") -> prefs.getBoolean(KEY_WEEKLY_TEST, true)
            upper.contains("PILOT") -> prefs.getBoolean(KEY_PILOT_TEST, true)
            upper.contains("CET") -> prefs.getBoolean(KEY_CET_TEST, true)
            upper.contains("LESSON") || upper.contains("UNIT") || upper.contains("LCT") -> prefs.getBoolean(KEY_LCT, true)
            upper.contains("CUSTOM") || upper.contains("INTERNATIONAL") -> prefs.getBoolean(KEY_CUSTOM_EXAM, true)
            else -> true
        }
    }

    fun setVisibility(context: Context, key: String, visible: Boolean) {
        getPrefs(context).edit().putBoolean(key, visible).apply()
    }

    fun getVisibility(context: Context, key: String): Boolean {
        return getPrefs(context).getBoolean(key, true)
    }

    fun setAllVisibilities(context: Context, visible: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_MONTHLY_TEST, visible)
            putBoolean(KEY_WEEKLY_TEST, visible)
            putBoolean(KEY_PILOT_TEST, visible)
            putBoolean(KEY_PILOT_1, visible)
            putBoolean(KEY_PILOT_2, visible)
            putBoolean(KEY_PILOT_3, visible)
            putBoolean(KEY_PILOT_4, visible)
            putBoolean(KEY_CET_TEST, visible)
            putBoolean(KEY_CET_1, visible)
            putBoolean(KEY_CET_2, visible)
            putBoolean(KEY_CET_3, visible)
            putBoolean(KEY_CET_4, visible)
            putBoolean(KEY_LCT, visible)
            putBoolean(KEY_CUSTOM_EXAM, visible)
        }.apply()
    }

    fun getAllVisibilities(context: Context): Map<String, Boolean> {
        val prefs = getPrefs(context)
        return mapOf(
            KEY_MONTHLY_TEST to prefs.getBoolean(KEY_MONTHLY_TEST, true),
            KEY_WEEKLY_TEST to prefs.getBoolean(KEY_WEEKLY_TEST, true),
            KEY_PILOT_TEST to prefs.getBoolean(KEY_PILOT_TEST, true),
            KEY_CET_TEST to prefs.getBoolean(KEY_CET_TEST, true),
            KEY_LCT to prefs.getBoolean(KEY_LCT, true),
            KEY_CUSTOM_EXAM to prefs.getBoolean(KEY_CUSTOM_EXAM, true)
        )
    }
}

