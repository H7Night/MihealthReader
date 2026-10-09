package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SleepSummaryTest {

    private val zone = ZoneId.of("UTC")

    private fun session() = SleepSession(
        start = 0,
        end = 8 * 3600,
        durationMinutes = 480,
        deepMinutes = 120,
        lightMinutes = 340,
        avgHr = 50,
        minHr = 45,
        maxHr = 60,
        stages = listOf(
            SleepStage(0, 3600, 2, "deep"),
            SleepStage(3600, 7200, 3, "light"),
            SleepStage(7200, 7260, 5, "awake"),
            SleepStage(7260, 8 * 3600, 2, "deep"),
        ),
    )

    @Test
    fun `deep ratio percent`() {
        assertEquals(25, SleepSummary.deepRatioPct(session()))
    }

    @Test
    fun `awake count`() {
        assertEquals(1, SleepSummary.awakeCount(session()))
    }

    @Test
    fun `describe mentions duration and deep ratio`() {
        val text = SleepSummary.describe(session(), zone)
        assertTrue(text.contains("8 小时 0 分钟"))
        assertTrue(text.contains("深睡占比 25%"))
        assertTrue(text.contains("00:00 入睡"))
    }

    @Test
    fun `low deep ratio flagged`() {
        val low = session().copy(deepMinutes = 30)
        assertTrue(SleepSummary.describe(low, zone).contains("深睡偏少"))
    }

    @Test
    fun `bedtime regularity average and std`() {
        // Bedtimes 23:00 and 23:30 -> since-18:00 = 300 and 330 (avg 315, std 15)
        val a = session().copy(start = 23 * 3600)
        val b = session().copy(start = 23 * 3600 + 1800)
        val reg = SleepSummary.bedtimeRegularity(listOf(a, b), zone)!!
        assertEquals(315, reg.averageMinutes)
        assertEquals(15, reg.stdMinutes)
        assertEquals("23:15", SleepSummary.formatBedtime(reg.averageMinutes))
        assertEquals(2, reg.nights)
    }

    @Test
    fun `regularity label thresholds`() {
        assertEquals("规律", SleepSummary.regularityLabel(20))
        assertEquals("较规律", SleepSummary.regularityLabel(45))
        assertEquals("不规律", SleepSummary.regularityLabel(90))
    }
}
