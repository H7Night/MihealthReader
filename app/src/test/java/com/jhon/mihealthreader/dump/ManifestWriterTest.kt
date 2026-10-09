package com.jhon.mihealthreader.dump

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ManifestWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `writes manifest with tables`() {
        val data = ManifestData(
            exportedAt = "2026-10-08T00:00:00Z",
            sourcePackage = "com.xiaomi.wearable",
            androidVersion = 34,
            appVersion = "0.1.0",
            databases = listOf(
                DbDumpResult("mi.db", true, listOf(TableDump("t_sleep", 3, File("json/mi.db/t_sleep.json"))))
            ),
        )
        val dest = File(tmp.root, "manifest.json")
        ManifestWriter().write(data, dest)

        val root = JSONObject(dest.readText())
        assertEquals("com.xiaomi.wearable", root.getString("sourcePackage"))
        assertEquals(
            3,
            root.getJSONArray("databases").getJSONObject(0)
                .getJSONArray("tables").getJSONObject(0).getInt("rowCount"),
        )
    }

    @Test
    fun `writes unsupported database entry`() {
        val data = ManifestData(
            "t", "p", 34, "0.1.0",
            listOf(DbDumpResult("x.db", false, emptyList(), "unsupported/encrypted")),
        )
        val dest = File(tmp.root, "manifest.json")
        ManifestWriter().write(data, dest)

        assertEquals(
            "unsupported/encrypted",
            JSONObject(dest.readText()).getJSONArray("databases").getJSONObject(0).getString("error"),
        )
    }
}
