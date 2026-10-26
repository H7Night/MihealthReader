package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodaySummaryTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 21)

    private fun workout(day: LocalDate, minutes: Int): Workout {
        val start = day.atStartOfDay(zone).toEpochSecond()
        return Workout(
            "dev",
            start,
            start + minutes * 60L,
            minutes * 60,
            100,
            null,
            null,
            null,
            null,
            "running",
            "Running",
        )
    }

    @Test
    fun `counts distinct days and total minutes`() {
        val result = TodaySummary.weeklyWorkouts(
            listOf(
                workout(LocalDate.of(2026, 10, 19), 30),
                workout(LocalDate.of(2026, 10, 19), 20),
                workout(LocalDate.of(2026, 10, 21), 40),
                workout(LocalDate.of(2026, 10, 12), 60),
            ),
            today,
            zone,
        )
        assertEquals(2, result.days)
        assertEquals(90, result.minutes)
    }

    @Test
    fun `empty workouts are zero`() {
        val result = TodaySummary.weeklyWorkouts(emptyList(), today, zone)
        assertEquals(0, result.days)
        assertEquals(0, result.minutes)
    }
}
