package com.jhon.mihealthreader.discover

import com.jhon.mihealthreader.root.RootShell

data class TargetApp(
    val packageName: String,
    val databasesPath: String,
    val dbFiles: List<String>,
)

fun interface TargetFinder {
    fun findCandidates(): List<TargetApp>
}

class TargetLocator(
    private val rootShell: RootShell,
    private val scanner: PackageScanner,
    private val priority: List<String> = DEFAULT_PRIORITY,
) : TargetFinder {

    override fun findCandidates(): List<TargetApp> {
        val candidates = LinkedHashSet<String>()
        candidates.addAll(priority)
        scanner.installedPackages()
            .filter { pkg -> NAME_HINTS.any { pkg.contains(it, ignoreCase = true) } }
            .forEach { candidates.add(it) }

        return candidates.mapNotNull { pkg ->
            val path = "/data/data/$pkg/databases"
            val output = rootShell.run("find", path, "-type", "f")
            if (output.exitCode == 0 && output.stdout.isNotEmpty()) {
                val relativeFiles = output.stdout
                    .filter { it.startsWith("$path/") }
                    .map { it.removePrefix("$path/") }
                if (relativeFiles.isEmpty()) null else TargetApp(pkg, path, relativeFiles)
            } else {
                null
            }
        }
    }

    companion object {
        val DEFAULT_PRIORITY = listOf("com.xiaomi.wearable", "com.mi.health", "com.xiaomi.hm.health")
        val NAME_HINTS = listOf("xiaomi", "health", "wearable", "mifit", "zepp")
    }
}
