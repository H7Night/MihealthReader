package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase

internal fun SQLiteDatabase.tableExists(table: String): Boolean {
    rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
        return cursor.moveToFirst()
    }
}

internal fun SQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    if (!tableExists(table)) return false
    rawQuery("PRAGMA table_info($table)", null).use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex).equals(column, ignoreCase = true)) return true
        }
    }
    return false
}
