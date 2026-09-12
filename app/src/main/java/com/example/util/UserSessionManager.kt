package com.example.util

import android.content.Context
import android.content.SharedPreferences

object UserSessionManager {
    private const val PREF_NAME = "hcm_sms_auth_session"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(context: Context, username: String) {
        getPrefs(context).edit()
            .putString(KEY_SAVED_USERNAME, username)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getSavedUsername(context: Context): String? {
        val prefs = getPrefs(context)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        return if (isLoggedIn) prefs.getString(KEY_SAVED_USERNAME, null) else null
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_SAVED_USERNAME)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
