package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.BodyMeasurement
import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.Workout
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DayValue(val date: String, val value: Float)

data class SleepBar(val date: String, val deep: Float, val light: Float, val awake: Float) {
    val total: Float get() = deep + light + awake
}

object ChartData {

    fun localDate(epochSeconds: Long, zone: ZoneId): String =
        Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDate().toString()

    /** Days that have step data, ascending by date. */
    fun stepsSeries(activity: List<DailyActivity>): List<DayValue> =
        activity.sortedBy { it.date }.map { DayValue(it.date, it.steps.toFloat()) }

    /** Daily average of point metrics, only days that have data, ascending. */
    fun metricSeries(
        samples: List<MetricSample>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayValue> =
        samples.groupBy { localDate(it.time, zone) }
            .map { (date, list) -> DayValue(date, list.map { it.value }.average().toFloat()) }
            .sortedBy { it.date }

    /** Sleep per night, only nights with data, ascending. */
    fun sleepSeries(
        sessions: List<SleepSession>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<SleepBar> =
        sessions.groupBy { localDate(it.start, zone) }
            .map { (date, list) ->
                SleepBar(
                    date = date,
                    deep = list.sumOf { it.deepMinutes }.toFloat(),
                    light = list.sumOf { it.lightMinutes }.toFloat(),
                    awake = list.sumOf {
                        (it.durationMinutes - it.deepMinutes - it.lightMinutes).coerceAtLeast(0)
                    }.toFloat(),
                )
            }
            .sortedBy { it.date }

    /** Workout minutes per day, only days with data, ascending. */
    fun workoutSeries(
        workouts: List<Workout>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayValue> =
        workouts.groupBy { localDate(it.start, zone) }
            .map { (date, list) -> DayValue(date, list.sumOf { it.durationSec } / 60f) }
            .sortedBy { it.date }

    /** Weight per measurement, ascending by time. */
    fun bodySeries(
        body: List<BodyMeasurement>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayValue> =
        body.filter { it.weightKg != null }
            .sortedBy { it.time }
            .map { DayValue(localDate(it.time, zone), it.weightKg!!.toFloat()) }

    fun <T> lastN(list: List<T>, n: Int = 10): List<T> =
        if (list.size <= n) list else list.takeLast(n)

    fun <T> inMonth(list: List<T>, yearMonth: YearMonth, dateOf: (T) -> String): List<T> =
        list.filter {
            runCatching { YearMonth.from(LocalDate.parse(dateOf(it))) == yearMonth }.getOrDefault(false)
        }
}
