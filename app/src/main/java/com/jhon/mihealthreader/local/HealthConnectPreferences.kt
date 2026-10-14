package com.jhon.mihealthreader.local

import android.content.Context

/** Persists the user's opt-in for writing to Health Connect. */
class HealthConnectPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun enabled(): Boolean = prefs.getBoolean(KEY, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val KEY = "hc_enabled"
    }
}
