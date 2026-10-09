package com.jhon.mihealthreader.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TableEntry(
    val dbName: String,
    val name: String,
    val rowCount: Int,
    val jsonFile: File,
)

class ExportRepository(private val exportsDir: File) {

    fun listSnapshots(): List<File> =
        exportsDir.listFiles { file -> file.isDirectory }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    fun latestSnapshot(): File? = listSnapshots().firstOrNull()

    fun tables(snapshot: File): List<TableEntry> {
        val manifest = File(snapshot, "manifest.json")
        if (!manifest.isFile) return emptyList()
        val root = JSONObject(manifest.readText())
        val databases = root.optJSONArray("databases") ?: return emptyList()

        val result = mutableListOf<TableEntry>()
        for (i in 0 until databases.length()) {
            val db = databases.getJSONObject(i)
            val dbName = db.getString("name")
            val tables = db.optJSONArray("tables") ?: continue
            for (j in 0 until tables.length()) {
                val table = tables.getJSONObject(j)
                result.add(
                    TableEntry(
                        dbName = dbName,
                        name = table.getString("name"),
                        rowCount = table.getInt("rowCount"),
                        jsonFile = File(snapshot, table.getString("jsonFile")),
                    )
                )
            }
        }
        return result
    }

    fun rows(entry: TableEntry): List<JSONObject> {
        if (!entry.jsonFile.isFile) return emptyList()
        val array = JSONArray(entry.jsonFile.readText())
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
}
