package com.jhon.mihealthreader.extract

import com.jhon.mihealthreader.discover.TargetApp
import java.io.File
import java.time.LocalDateTime

data class Snapshot(
    val directory: File,
    val sourcePackage: String,
    val files: List<File>,
)

interface Extractor {
    fun extract(target: TargetApp, exportsDir: File): Snapshot
}

class DbExtractor(
    private val sourceReader: SourceReader,
    private val timestamp: () -> String = { LocalDateTime.now().toString() },
) : Extractor {

    override fun extract(target: TargetApp, exportsDir: File): Snapshot {
        val directory = File(exportsDir, "${timestamp()}/raw/${target.packageName}/databases")
        directory.mkdirs()

        val files = target.dbFiles.map { name ->
            val dest = File(directory, name)
            sourceReader.open("${target.databasesPath}/$name").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest
        }

        return Snapshot(directory, target.packageName, files)
    }
}
