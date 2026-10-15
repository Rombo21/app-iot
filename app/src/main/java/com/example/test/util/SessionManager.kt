package com.example.test.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona la sesión del usuario logueado usando SharedPreferences.
 * Permite saber quién inició sesión para evitar auto-eliminación y permitir logout.
 */
object SessionManager {

    private const val PREFS_NAME   = "app_session"
    private const val KEY_EMAIL    = "session_email"
    private const val KEY_NOMBRES  = "session_nombres"
    private const val KEY_LOGGED   = "session_logged_in"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(context: Context, email: String, nombres: String) {
        prefs(context).edit()
            .putString(KEY_EMAIL,   email)
            .putString(KEY_NOMBRES, nombres)
            .putBoolean(KEY_LOGGED, true)
            .apply()
    }

    fun getEmail(context: Context): String? =
        prefs(context).getString(KEY_EMAIL, null)

    fun getNombres(context: Context): String? =
        prefs(context).getString(KEY_NOMBRES, null)

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED, false)

    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
