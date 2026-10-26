package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChartSeriesTest {
    private val zone = ZoneId.of("UTC")
    private val anchor = LocalDate.of(2026, 10, 21) // Wednesday

    @Test
    fun `week window starts on monday`() {
        val (start, end) = ChartSeries.window(ChartRange.WEEK, anchor)
        assertEquals(LocalDate.of(2026, 10, 19), start)
        assertEquals(LocalDate.of(2026, 10, 25), end)
    }

    @Test
    fun `three month window includes the current month`() {
        val (start, end) = ChartSeries.window(ChartRange.THREE_MONTHS, anchor)
        assertEquals(LocalDate.of(2026, 8, 1), start)
        assertEquals(LocalDate.of(2026, 10, 31), end)
    }

    @Test
    fun `year window covers the calendar year`() {
        val (start, end) = ChartSeries.window(ChartRange.YEAR, anchor)
        assertEquals(LocalDate.of(2026, 1, 1), start)
        assertEquals(LocalDate.of(2026, 12, 31), end)
    }

    @Test
    fun `week steps sum per day`() {
        val window = ChartSeries.steps(
            listOf(
                DailyActivity("2026-10-19", 1000, 800, 40),
                DailyActivity("2026-10-20", 2000, 1600, 80),
            ),
            ChartRange.WEEK,
            anchor,
        )
        assertEquals(2, window.buckets.size)
        assertEquals(1000f, window.buckets[0].value, 0f)
        assertEquals(2000f, window.buckets[1].value, 0f)
    }

    @Test
    fun `month steps stay inside the month`() {
        val window = ChartSeries.steps(
            listOf(
                DailyActivity("2026-09-30", 9000, 7000, 300),
                DailyActivity("2026-10-01", 1000, 800, 40),
                DailyActivity("2026-10-31", 2000, 1600, 80),
            ),
            ChartRange.MONTH,
            anchor,
        )
        assertEquals(2, window.buckets.size)
        assertEquals(3000f, window.buckets.sumOf { it.value.toDouble() }.toFloat(), 0f)
    }

    @Test
    fun `day heart rate buckets by hour`() {
        val day = anchor.atStartOfDay(zone).toEpochSecond()
        val window = ChartSeries.heartRate(
            listOf(
                MetricSample("heart_rate", "dev", day + 8 * 3600, 60.0),
                MetricSample("heart_rate", "dev", day + 8 * 3600 + 600, 80.0),
                MetricSample("heart_rate", "dev", day + 9 * 3600, 70.0),
            ),
            ChartRange.DAY,
            anchor,
            zone,
        )
        assertEquals(2, window.buckets.size)
        assertEquals(8, window.buckets[0].hour)
        assertEquals(70f, window.buckets[0].value, 0f)
    }

    @Test
    fun `year heart rate averages by month`() {
        val jan = LocalDate.of(2026, 1, 10).atStartOfDay(zone).toEpochSecond()
        val feb = LocalDate.of(2026, 2, 10).atStartOfDay(zone).toEpochSecond()
        val window = ChartSeries.heartRate(
            listOf(
                MetricSample("heart_rate", "dev", jan, 60.0),
                MetricSample("heart_rate", "dev", jan + 86400, 80.0),
                MetricSample("heart_rate", "dev", feb, 70.0),
            ),
            ChartRange.YEAR,
            anchor,
            zone,
        )
        assertEquals(2, window.buckets.size)
        assertEquals(70f, window.buckets[0].value, 0f)
        assertEquals(70f, window.buckets[1].value, 0f)
    }

    @Test
    fun `week workouts sum minutes`() {
        val start = LocalDate.of(2026, 10, 19).atStartOfDay(zone).toEpochSecond()
        val window = ChartSeries.workouts(
            listOf(
                Workout("dev", start, start + 600, 600, 100, null, null, null, null, "running", "Running"),
                Workout("dev", start + 3600, start + 4200, 600, 100, null, null, null, null, "running", "Running"),
            ),
            ChartRange.WEEK,
            anchor,
            zone,
        )
        assertEquals(20f, window.buckets.single().value, 0f)
    }

    @Test
    fun `three month buckets clamp the first week to the window`() {
        val window = ChartSeries.steps(
            listOf(DailyActivity("2026-08-01", 1000, 0, 0)),
            ChartRange.THREE_MONTHS,
            anchor,
        )
        assertEquals(LocalDate.of(2026, 8, 1), window.buckets.single().start)
        assertEquals(LocalDate.of(2026, 8, 2), window.buckets.single().end)
    }

    @Test
    fun `malformed dates are ignored`() {
        val window = ChartSeries.steps(
            listOf(DailyActivity("not-a-date", 1000, 0, 0)),
            ChartRange.WEEK,
            anchor,
        )
        assertEquals(0, window.buckets.size)
    }

    @Test
    fun `empty data produces no buckets`() {
        assertEquals(0, ChartSeries.steps(emptyList(), ChartRange.WEEK, anchor).buckets.size)
    }

    @Test
    fun `shift moves by the range unit`() {
        assertEquals(LocalDate.of(2026, 10, 28), ChartSeries.shift(ChartRange.WEEK, anchor, 1))
        assertEquals(LocalDate.of(2026, 11, 21), ChartSeries.shift(ChartRange.MONTH, anchor, 1))
        assertEquals(LocalDate.of(2027, 1, 21), ChartSeries.shift(ChartRange.THREE_MONTHS, anchor, 1))
    }
}

