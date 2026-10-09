package com.jhon.mihealthreader.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExportRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun snapshot(name: String): File {
        val dir = File(tmp.root, name).apply { mkdirs() }
        File(dir, "json/mi.db").mkdirs()
        File(dir, "json/mi.db/t_sleep.json").writeText("""[{"id":1,"stage":"deep"}]""")
        File(dir, "manifest.json").writeText(
            """{"databases":[{"name":"mi.db","tables":[{"name":"t_sleep","rowCount":1,"jsonFile":"json/mi.db/t_sleep.json"}]}]}"""
        )
        return dir
    }

    @Test
    fun `lists snapshots newest first`() {
        snapshot("2026-01-01T00-00-00")
        snapshot("2026-02-01T00-00-00")
        val repo = ExportRepository(tmp.root)
        assertEquals("2026-02-01T00-00-00", repo.latestSnapshot()!!.name)
    }

    @Test
    fun `reads table entries and rows`() {
        val snap = snapshot("2026-01-01T00-00-00")
        val repo = ExportRepository(tmp.root)
        val entry = repo.tables(snap).single()
        assertEquals("t_sleep", entry.name)
        assertEquals(1, entry.rowCount)
        assertEquals("deep", repo.rows(entry).single().getString("stage"))
    }

    @Test
    fun `missing exports dir yields empty list`() {
        val repo = ExportRepository(File(tmp.root, "nope"))
        assertEquals(emptyList<File>(), repo.listSnapshots())
    }
}
