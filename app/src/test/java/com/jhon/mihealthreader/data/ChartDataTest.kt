package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ChartDataTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `steps series sorted ascending`() {
        val activity = listOf(
            DailyActivity("2026-10-09", 200, 150, 9),
            DailyActivity("2026-10-01", 100, 80, 5),
        )
        val series = ChartData.stepsSeries(activity)
        assertEquals(listOf("2026-10-01", "2026-10-09"), series.map { it.date })
        assertEquals(200f, series.last().value, 0f)
    }

    @Test
    fun `metric series averages by local day`() {
        val t1 = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toEpochSecond() + 60
        val samples = listOf(
            MetricSample("heart_rate", "dev", t1, 60.0),
            MetricSample("heart_rate", "dev", t1 + 120, 80.0),
        )
        val series = ChartData.metricSeries(samples, zone)
        assertEquals(1, series.size)
        assertEquals(70f, series.single().value, 0f)
    }

    @Test
    fun `sleep series splits stages`() {
        val start = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toEpochSecond() + 3600
        val session = SleepSession(start, start + 3600, 60, 20, 30, 50, 45, 60, emptyList())
        val bar = ChartData.sleepSeries(listOf(session), zone).single()
        assertEquals(20f, bar.deep, 0f)
        assertEquals(30f, bar.light, 0f)
        assertEquals(10f, bar.awake, 0f)
    }

    @Test
    fun `lastN takes the most recent n`() {
        val list = (1..15).map { DayValue("2026-10-%02d".format(it), it.toFloat()) }
        val result = ChartData.lastN(list, 10)
        assertEquals(10, result.size)
        assertEquals(15f, result.last().value, 0f)
        assertEquals(6f, result.first().value, 0f)
    }

    @Test
    fun `inMonth filters by month`() {
        val list = listOf(
            DayValue("2026-09-30", 1f),
            DayValue("2026-10-01", 2f),
            DayValue("2026-10-20", 3f),
            DayValue("2026-11-01", 4f),
        )
        val result = ChartData.inMonth(list, YearMonth.of(2026, 10)) { it.date }
        assertEquals(listOf(2f, 3f), result.map { it.value })
    }

    @Test
    fun `workout minutes per day`() {
        val start = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toEpochSecond() + 100
        val workout = Workout("dev", start, start + 600, 600, 100, null, null, null, null, "running", "跑步")
        val series = ChartData.workoutSeries(listOf(workout), zone)
        assertEquals(10f, series.single().value, 0f)
    }
}
