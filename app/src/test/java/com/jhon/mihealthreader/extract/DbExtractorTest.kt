package com.jhon.mihealthreader.extract

import com.jhon.mihealthreader.discover.TargetApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class DbExtractorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `copies every file into timestamped snapshot`() {
        val content = mapOf(
            "/data/data/com.xiaomi.wearable/databases/mi.db" to "DB".toByteArray(),
            "/data/data/com.xiaomi.wearable/databases/mi.db-wal" to "WAL".toByteArray(),
        )
        val reader = SourceReader { path -> ByteArrayInputStream(content.getValue(path)) }
        val target = TargetApp(
            "com.xiaomi.wearable",
            "/data/data/com.xiaomi.wearable/databases",
            listOf("mi.db", "mi.db-wal"),
        )

        val snapshot = DbExtractor(reader) { "T1" }.extract(target, tmp.root)

        assertEquals(2, snapshot.files.size)
        assertTrue(snapshot.directory.endsWith(File("raw/com.xiaomi.wearable/databases")))
        assertEquals("DB", snapshot.files.first { it.name == "mi.db" }.readText())
        assertEquals("WAL", snapshot.files.first { it.name == "mi.db-wal" }.readText())
    }

    @Test
    fun `propagates read failure`() {
        val reader = SourceReader { throw IOException("nope") }
        val target = TargetApp("p", "/data/data/p/databases", listOf("x.db"))

        assertThrows(IOException::class.java) {
            DbExtractor(reader) { "T1" }.extract(target, tmp.root)
        }
    }
}
