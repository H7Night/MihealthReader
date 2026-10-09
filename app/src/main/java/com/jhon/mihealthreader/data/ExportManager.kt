package com.jhon.mihealthreader.data

import java.io.File

class ExportManager(private val exportsDir: File) {

    fun latestNormalized(): File? {
        val latest = exportsDir.listFiles { file -> file.isDirectory }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?: return null
        val file = File(latest, "parsed/normalized.json")
        return if (file.isFile) file else null
    }

    fun defaultExportName(): String = "mihealth-normalized.json"
}
