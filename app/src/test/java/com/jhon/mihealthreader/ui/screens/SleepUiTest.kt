package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.parse.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepUiTest {
    @Test
    fun `stage percentages sum to one hundred`() {
        val session = SleepSession(0, 0, 100, 20, 70, null, null, null, emptyList())
        val b = sleepStageBreakdown(session)
        assertEquals(20, b.deep)
        assertEquals(70, b.light)
        assertEquals(10, b.awake)
        assertEquals(100, b.deep + b.light + b.awake)
    }

    @Test
    fun `zero duration is safe`() {
        val session = SleepSession(0, 0, 0, 0, 0, null, null, null, emptyList())
        val b = sleepStageBreakdown(session)
        assertEquals(100, b.deep + b.light + b.awake)
    }

    @Test
    fun `sleep quality mirrors the old thresholds`() {
        assertEquals(SleepQuality.Good, sleepQuality(SleepSession(0, 0, 480, 140, 300, null, null, null, emptyList())))
        assertEquals(SleepQuality.LowDeep, sleepQuality(SleepSession(0, 0, 480, 40, 400, null, null, null, emptyList())))
        assertEquals(SleepQuality.Short, sleepQuality(SleepSession(0, 0, 300, 70, 200, null, null, null, emptyList())))
        assertEquals(SleepQuality.Fair, sleepQuality(SleepSession(0, 0, 400, 90, 270, null, null, null, emptyList())))
    }
}
