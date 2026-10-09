package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.Workout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayValue(val date: String, val value: Float)

data class SleepBar(val date: String, val deep: Float, val light: Float, val awake: Float)

object ChartData {

    private fun lastNDates(days: Int, today: LocalDate): List<String> =
        (days - 1 downTo 0).map { today.minusDays(it.toLong()).toString() }

    fun localDate(epochSeconds: Long, zone: ZoneId): String =
        Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDate().toString()

    fun dailySteps(
        activity: List<DailyActivity>,
        days: Int = 30,
        today: LocalDate = LocalDate.now(),
    ): List<DayValue> {
        val byDate = activity.associate { it.date to it.steps.toFloat() }
        return lastNDates(days, today).map { DayValue(it, byDate[it] ?: 0f) }
    }

    fun dailyMetricAvg(
        samples: List<MetricSample>,
        days: Int = 30,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayValue> {
        val grouped = samples.groupBy { localDate(it.time, zone) }
        return lastNDates(days, today).map { date ->
            val values = grouped[date].orEmpty()
            DayValue(date, if (values.isEmpty()) 0f else values.map { it.value }.average().toFloat())
        }
    }

    fun sleepDurations(
        sessions: List<SleepSession>,
        days: Int = 30,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<SleepBar> {
        val byDate = sessions.groupBy { localDate(it.start, zone) }
        return lastNDates(days, today).map { date ->
            val daySessions = byDate[date].orEmpty()
            val deep = daySessions.sumOf { it.deepMinutes }.toFloat()
            val light = daySessions.sumOf { it.lightMinutes }.toFloat()
            val awake = daySessions.sumOf { (it.durationMinutes - it.deepMinutes - it.lightMinutes).coerceAtLeast(0) }
                .toFloat()
            SleepBar(date, deep, light, awake)
        }
    }

    fun workoutMinutesPerDay(
        workouts: List<Workout>,
        days: Int = 30,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayValue> {
        val byDate = workouts.groupBy { localDate(it.start, zone) }
            .mapValues { (_, list) -> list.sumOf { it.durationSec } / 60f }
        return lastNDates(days, today).map { DayValue(it, byDate[it] ?: 0f) }
    }
}
