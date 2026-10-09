package com.jhon.mihealthreader.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ParsedRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun writeSnapshot(name: String, json: String) {
        val dir = File(tmp.root, name)
        File(dir, "parsed").mkdirs()
        File(dir, "parsed/normalized.json").writeText(json)
    }

    private val sample = """
        {"schema_version":"1.0","source":"mihealthreader","generated_at":"t","records":{
          "sleep":[{"start":200,"end":300,"duration_min":1,"deep_min":0,"light_min":1,"avg_hr":60,"min_hr":55,"max_hr":65,
            "stages":[{"start":200,"end":300,"state":2,"state_name":"light"}]},
            {"start":100,"end":160,"duration_min":1,"deep_min":1,"light_min":0,"avg_hr":50,"min_hr":45,"max_hr":70,
            "stages":[]}],
          "daily_activity":[{"date":"2026-04-02","steps":100,"distance_m":80,"active_kcal":5},
            {"date":"2026-05-01","steps":200,"distance_m":150,"active_kcal":9}]}}
    """.trimIndent()

    @Test
    fun `reads daily activity newest first`() {
        writeSnapshot("2026-05-01T00-00-00", sample)
        val activity = ParsedRepository(tmp.root).dailyActivity()
        assertEquals(2, activity.size)
        assertEquals("2026-05-01", activity.first().date)
        assertEquals(200, activity.first().steps)
    }

    @Test
    fun `reads sleep sessions newest first with stages`() {
        writeSnapshot("2026-05-01T00-00-00", sample)
        val sleep = ParsedRepository(tmp.root).sleepSessions()
        assertEquals(2, sleep.size)
        assertEquals(200L, sleep.first().start)
        assertEquals("light", sleep.first().stages.single().stateName)
    }

    @Test
    fun `empty when no normalized file`() {
        val repo = ParsedRepository(tmp.root)
        assertEquals(0, repo.dailyActivity().size)
        assertEquals(0, repo.sleepSessions().size)
    }
}
