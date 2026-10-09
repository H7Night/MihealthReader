package com.jhon.mihealthreader.parse

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class NormalizerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `writes envelope with sleep and activity`() {
        val dest = File(tmp.root, "parsed/normalized.json")
        writeNormalizedEnvelope(
            sleep = listOf(
                SleepSession(100, 200, 2, 1, 1, 60, 50, 70, listOf(SleepStage(100, 160, 3, "deep")))
            ),
            activity = listOf(DailyActivity("2026-06-12", 100, 80, 5)),
            skipped = 1,
            dest = dest,
            generatedAt = "2026-06-12T00:00:00Z",
        )

        val root = JSONObject(dest.readText())
        assertEquals("1.0", root.getString("schema_version"))
        assertEquals("mihealthreader", root.getString("source"))
        assertEquals(1, root.getInt("skipped"))
        val sleep = root.getJSONObject("records").getJSONArray("sleep").getJSONObject(0)
        assertEquals("deep", sleep.getJSONArray("stages").getJSONObject(0).getString("state_name"))
        assertEquals(
            100,
            root.getJSONObject("records").getJSONArray("daily_activity").getJSONObject(0).getInt("steps"),
        )
    }
}
