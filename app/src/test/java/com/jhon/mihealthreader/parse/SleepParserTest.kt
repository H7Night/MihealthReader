package com.jhon.mihealthreader.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SleepParserTest {

    @Test
    fun `parses value into session`() {
        val value = """{"avg_hr":50,"min_hr":45,"max_hr":67,"bedtime":1000,"device_wake_up_time":1060,"duration":1,"sleep_deep_duration":1,"sleep_light_duration":0,"items":[{"start_time":1000,"end_time":1060,"state":3}]}"""
        val session = SleepParser.parseValue(value)!!
        assertEquals(1000L, session.start)
        assertEquals(1060L, session.end)
        assertEquals(1, session.deepMinutes)
        assertEquals(50, session.avgHr)
        assertEquals("light", session.stages.single().stateName)
    }

    @Test
    fun `returns null on invalid json`() {
        assertNull(SleepParser.parseValue("not json"))
    }

    @Test
    fun `returns null when missing boundaries`() {
        assertNull(SleepParser.parseValue("""{"avg_hr":50}"""))
    }

    @Test
    fun `state name mapping`() {
        assertEquals("deep", SleepParser.stateName(2))
        assertEquals("light", SleepParser.stateName(3))
        assertEquals("awake", SleepParser.stateName(5))
        assertEquals("unknown", SleepParser.stateName(9))
    }
}
