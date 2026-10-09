package com.jhon.mihealthreader.extract

import com.jhon.mihealthreader.discover.TargetApp
import java.io.File
import java.time.LocalDateTime

data class Snapshot(
    val root: File,
    val directory: File,
    val sourcePackage: String,
    val files: List<File>,
)

fun interface Extractor {
    fun extract(target: TargetApp, exportsDir: File): Snapshot
}

class DbExtractor(
    private val sourceReader: SourceReader,
    private val timestamp: () -> String = { LocalDateTime.now().toString() },
) : Extractor {

    override fun extract(target: TargetApp, exportsDir: File): Snapshot {
        val root = File(exportsDir, timestamp())
        val directory = File(root, "raw/${target.packageName}/databases")
        directory.mkdirs()

        val files = target.dbFiles
            .filterNot { it.endsWith("-shm") }
            .map { relative ->
                val dest = File(directory, relative)
                dest.parentFile?.mkdirs()
                sourceReader.open("${target.databasesPath}/$relative").use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest
            }

        return Snapshot(root, directory, target.packageName, files)
    }
}
