package com.jhon.mihealthreader.dump

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ManifestData(
    val exportedAt: String,
    val sourcePackage: String,
    val androidVersion: Int,
    val appVersion: String,
    val databases: List<DbDumpResult>,
)

class ManifestWriter {

    fun write(data: ManifestData, dest: File) {
        val root = JSONObject()
        root.put("exportedAt", data.exportedAt)
        root.put("sourcePackage", data.sourcePackage)
        root.put("androidVersion", data.androidVersion)
        root.put("appVersion", data.appVersion)

        val base = dest.parentFile
        val databases = JSONArray()
        data.databases.forEach { db ->
            val dbObject = JSONObject()
            dbObject.put("name", db.dbName)
            dbObject.put("isSqlite", db.isSqlite)
            db.error?.let { dbObject.put("error", it) }

            val tables = JSONArray()
            db.tables.forEach { table ->
                val tableObject = JSONObject()
                tableObject.put("name", table.name)
                tableObject.put("rowCount", table.rowCount)
                tableObject.put("jsonFile", relativeJsonPath(table.jsonFile, base))
                tables.put(tableObject)
            }
            dbObject.put("tables", tables)
            databases.put(dbObject)
        }
        root.put("databases", databases)

        dest.parentFile?.mkdirs()
        dest.writeText(root.toString(2))
    }

    private fun relativeJsonPath(jsonFile: File, base: File?): String =
        if (base != null) jsonFile.relativeToOrSelf(base).path else jsonFile.path
}
