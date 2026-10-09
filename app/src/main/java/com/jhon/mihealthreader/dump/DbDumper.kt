package com.jhon.mihealthreader.dump

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import org.json.JSONObject
import java.io.File

data class TableDump(
    val name: String,
    val rowCount: Int,
    val jsonFile: File,
)

data class DbDumpResult(
    val dbName: String,
    val isSqlite: Boolean,
    val tables: List<TableDump>,
    val error: String? = null,
)

fun interface Dumper {
    fun dump(dbFile: File, jsonDir: File): DbDumpResult
}

class DbDumper(private val rowLimit: Int = Int.MAX_VALUE) : Dumper {

    fun isSqlite(dbFile: File): Boolean {
        if (!dbFile.isFile) return false
        val header = ByteArray(SQLITE_HEADER.size)
        dbFile.inputStream().use { input ->
            var read = 0
            while (read < header.size) {
                val n = input.read(header, read, header.size - read)
                if (n < 0) return false
                read += n
            }
        }
        return header.contentEquals(SQLITE_HEADER)
    }

    override fun dump(dbFile: File, jsonDir: File): DbDumpResult {
        val dbName = dbFile.name
        if (!isSqlite(dbFile)) {
            return DbDumpResult(dbName, isSqlite = false, tables = emptyList(), error = "unsupported/encrypted")
        }

        val tables = mutableListOf<TableDump>()
        var db: SQLiteDatabase? = null
        try {
            db = SqliteSupport.openReadOnly(dbFile)
            val names = mutableListOf<String>()
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) names.add(cursor.getString(0))
            }
            jsonDir.mkdirs()
            for (name in names) {
                val outFile = File(jsonDir, "$name.json")
                tables.add(TableDump(name, dumpTable(db, name, outFile), outFile))
            }
        } catch (e: Exception) {
            return DbDumpResult(dbName, isSqlite = true, tables = tables, error = e.message ?: e.toString())
        } finally {
            db?.close()
        }
        return DbDumpResult(dbName, isSqlite = true, tables = tables)
    }

    private fun dumpTable(db: SQLiteDatabase, table: String, outFile: File): Int {
        val limitClause = if (rowLimit == Int.MAX_VALUE) "" else " LIMIT $rowLimit"
        var count = 0
        outFile.bufferedWriter().use { writer ->
            writer.write("[")
            db.rawQuery("SELECT * FROM ${quote(table)}$limitClause", null).use { cursor ->
                while (cursor.moveToNext()) {
                    if (count > 0) writer.write(",")
                    val row = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        row.put(cursor.getColumnName(i), valueOf(cursor, i))
                    }
                    writer.write(row.toString())
                    count++
                }
            }
            writer.write("]")
        }
        return count
    }

    private fun valueOf(cursor: Cursor, index: Int): Any = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
        Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
        Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP)
        else -> JSONObject.NULL
    }

    private fun quote(identifier: String): String =
        "\"" + identifier.replace("\"", "\"\"") + "\""

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
