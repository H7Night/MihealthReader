package com.jhon.mihealthreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun snapshot(name: String, withNormalized: Boolean = true) {
        val parsed = File(File(tmp.root, name), "parsed").apply { mkdirs() }
        if (withNormalized) File(parsed, "normalized.json").writeText("{}")
    }

    @Test
    fun `finds latest normalized`() {
        snapshot("2026-01-01T00-00-00")
        snapshot("2026-02-01T00-00-00")
        val file = ExportManager(tmp.root).latestNormalized()!!
        assertEquals("2026-02-01T00-00-00", file.parentFile.parentFile.name)
    }

    @Test
    fun `null when none`() {
        assertNull(ExportManager(File(tmp.root, "nope")).latestNormalized())
    }

    @Test
    fun `null when latest snapshot has no normalized`() {
        snapshot("2026-03-01T00-00-00", withNormalized = false)
        assertNull(ExportManager(tmp.root).latestNormalized())
    }
}
