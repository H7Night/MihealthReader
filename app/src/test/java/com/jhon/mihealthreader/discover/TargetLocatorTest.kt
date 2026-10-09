package com.jhon.mihealthreader.discover

import com.jhon.mihealthreader.root.RootShell
import com.jhon.mihealthreader.root.ShellOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TargetLocatorTest {

    private fun shellReturning(dirExistsFor: Set<String>) = object : RootShell {
        override fun run(vararg commands: String): ShellOutput {
            val path = commands[1]
            val pkg = path.removePrefix("/data/data/").removeSuffix("/databases")
            return if (pkg in dirExistsFor) {
                ShellOutput(
                    0,
                    listOf("$path/fitness_sync", "$path/2476836171/cn/fitness_data"),
                    emptyList(),
                )
            } else {
                ShellOutput(1, emptyList(), listOf("find: No such file or directory"))
            }
        }
    }

    @Test
    fun `finds priority package with databases`() {
        val locator = TargetLocator(shellReturning(setOf("com.xiaomi.wearable")), { emptyList() })
        val result = locator.findCandidates()
        assertEquals(listOf("com.xiaomi.wearable"), result.map { it.packageName })
        assertEquals(
            listOf("fitness_sync", "2476836171/cn/fitness_data"),
            result.first().dbFiles,
        )
        assertEquals("/data/data/com.xiaomi.wearable/databases", result.first().databasesPath)
    }

    @Test
    fun `finds installed package by name hint`() {
        val locator = TargetLocator(
            shellReturning(setOf("com.acme.health")),
            { listOf("com.acme.health", "com.other.app") }
        )
        assertEquals(listOf("com.acme.health"), locator.findCandidates().map { it.packageName })
    }

    @Test
    fun `ignores packages without databases dir`() {
        val locator = TargetLocator(shellReturning(emptySet()), { listOf("com.acme.health") })
        assertTrue(locator.findCandidates().isEmpty())
    }

    @Test
    fun `priority package comes first and dedups`() {
        val locator = TargetLocator(
            shellReturning(setOf("com.xiaomi.wearable", "com.acme.health")),
            { listOf("com.xiaomi.wearable", "com.acme.health") }
        )
        assertEquals(
            listOf("com.xiaomi.wearable", "com.acme.health"),
            locator.findCandidates().map { it.packageName }
        )
    }
}
