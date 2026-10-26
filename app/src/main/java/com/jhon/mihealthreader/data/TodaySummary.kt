package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TodaySummary {
    data class WeeklyWorkouts(val days: Int, val minutes: Int)

    fun weeklyWorkouts(
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): WeeklyWorkouts {
        val start = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val end = start.plusDays(6)
        val inWeek = workouts.filter {
            Instant.ofEpochSecond(it.start).atZone(zone).toLocalDate() in start..end
        }
        val days = inWeek
            .map { Instant.ofEpochSecond(it.start).atZone(zone).toLocalDate() }
            .distinct()
            .size
        return WeeklyWorkouts(days, inWeek.sumOf { it.durationSec } / 60)
    }
}
