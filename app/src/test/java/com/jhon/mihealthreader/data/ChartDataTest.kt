package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChartDataTest {

    private val today = LocalDate.of(2026, 10, 9)

    @Test
    fun `daily steps window fills zeros`() {
        val activity = listOf(
            DailyActivity("2026-10-08", 100, 80, 5),
            DailyActivity("2026-10-09", 200, 150, 9),
        )
        val result = ChartData.dailySteps(activity, days = 30, today = today)
        assertEquals(30, result.size)
        assertEquals("2026-09-10", result.first().date)
        assertEquals(200f, result.last().value, 0f)
        assertEquals(100f, result[result.size - 2].value, 0f)
        assertEquals(0f, result.first().value, 0f)
    }

    @Test
    fun `daily metric avg groups by local day`() {
        val zone = ZoneId.of("UTC")
        val t1 = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toEpochSecond() + 60
        val t2 = t1 + 120
        val samples = listOf(
            MetricSample("heart_rate", "dev", t1, 60.0),
            MetricSample("heart_rate", "dev", t2, 80.0),
        )
        val result = ChartData.dailyMetricAvg(samples, days = 3, today = today, zone = zone)
        assertEquals(70f, result.last().value, 0f)
        assertEquals(0f, result.first().value, 0f)
    }

    @Test
    fun `sleep durations split stages`() {
        val zone = ZoneId.of("UTC")
        val start = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toEpochSecond() + 3600
        val session = SleepSession(start, start + 3600, 60, 20, 30, 50, 45, 60, emptyList())
        val bar = ChartData.sleepDurations(listOf(session), days = 3, today = today, zone = zone).last()
        assertEquals(20f, bar.deep, 0f)
        assertEquals(30f, bar.light, 0f)
        assertEquals(10f, bar.awake, 0f)
    }

    @Test
    fun `workout minutes per day`() {
        val zone = ZoneId.of("UTC")
        val start = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toEpochSecond() + 100
        val workout = Workout("dev", start, start + 600, 600, 100, null, null, null, null, "running", "跑步")
        val result = ChartData.workoutMinutesPerDay(listOf(workout), days = 3, today = today, zone = zone)
        assertEquals(10f, result.last().value, 0f)
    }
}
