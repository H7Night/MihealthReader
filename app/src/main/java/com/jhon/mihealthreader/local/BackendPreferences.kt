package com.jhon.mihealthreader.local

import android.content.Context

enum class BackendType { ROOT, SHIZUKU }

class BackendPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun get(): BackendType =
        BackendType.entries.firstOrNull { it.name == prefs.getString(KEY, null) } ?: BackendType.ROOT

    fun set(type: BackendType) {
        prefs.edit().putString(KEY, type.name).apply()
    }

    private companion object {
        const val KEY = "backend"
    }
}
