package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.BodyMeasurement
import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ChartRange { DAY, WEEK, MONTH, THREE_MONTHS, YEAR }

data class ChartBucket(
    val start: LocalDate,
    val end: LocalDate,
    val value: Float,
    val hour: Int? = null,
)

data class ChartWindow(
    val start: LocalDate,
    val end: LocalDate,
    val buckets: List<ChartBucket>,
)

object ChartSeries {

    fun shift(range: ChartRange, anchor: LocalDate, delta: Long): LocalDate = when (range) {
        ChartRange.DAY -> anchor.plusDays(delta)
        ChartRange.WEEK -> anchor.plusWeeks(delta)
        ChartRange.MONTH -> anchor.plusMonths(delta)
        ChartRange.THREE_MONTHS -> anchor.plusMonths(delta * 3)
        ChartRange.YEAR -> anchor.plusYears(delta)
    }

    fun window(
        range: ChartRange,
        anchor: LocalDate,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): Pair<LocalDate, LocalDate> = when (range) {
        ChartRange.DAY -> anchor to anchor
        ChartRange.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            start to start.plusDays(6)
        }
        ChartRange.MONTH -> anchor.withDayOfMonth(1) to anchor.withDayOfMonth(anchor.lengthOfMonth())
        ChartRange.THREE_MONTHS -> {
            val end = anchor.withDayOfMonth(anchor.lengthOfMonth())
            end.withDayOfMonth(1).minusMonths(2) to end
        }
        ChartRange.YEAR -> anchor.withDayOfYear(1) to anchor.withDayOfYear(anchor.lengthOfYear())
    }

    fun steps(
        activity: List<DailyActivity>,
        range: ChartRange,
        anchor: LocalDate,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): ChartWindow {
        val daily = activity.mapNotNull { item ->
            runCatching { LocalDate.parse(item.date) }.getOrNull()
                ?.let { date -> date to item.steps.toFloat() }
        }.toMap()
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = true)
    }

    fun workouts(
        workouts: List<Workout>,
        range: ChartRange,
        anchor: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): ChartWindow {
        val daily = workouts.groupBy { instant(it.start, zone).toLocalDate() }
            .mapValues { (_, list) -> list.sumOf { it.durationSec }.toFloat() / 60f }
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = true)
    }

    fun heartRate(
        samples: List<MetricSample>,
        range: ChartRange,
        anchor: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): ChartWindow = samplesWindow(samples, range, anchor, zone, firstDayOfWeek)

    fun spo2(
        samples: List<MetricSample>,
        range: ChartRange,
        anchor: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): ChartWindow = samplesWindow(samples, range, anchor, zone, firstDayOfWeek)

    fun weight(
        measurements: List<BodyMeasurement>,
        range: ChartRange,
        anchor: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): ChartWindow {
        val daily = measurements
            .mapNotNull { m -> m.weightKg?.let { instant(m.time, zone).toLocalDate() to it.toFloat() } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.average().toFloat() }
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = false)
    }

    private fun samplesWindow(
        samples: List<MetricSample>,
        range: ChartRange,
        anchor: LocalDate,
        zone: ZoneId,
        firstDayOfWeek: DayOfWeek,
    ): ChartWindow {
        if (range == ChartRange.DAY) {
            val byHour = samples
                .filter { instant(it.time, zone).toLocalDate() == anchor }
                .groupBy { instant(it.time, zone).hour }
                .toSortedMap()
                .map { (hour, list) ->
                    ChartBucket(anchor, anchor, list.map { it.value }.average().toFloat(), hour)
                }
            return ChartWindow(anchor, anchor, byHour)
        }
        val daily = samples
            .groupBy { instant(it.time, zone).toLocalDate() }
            .mapValues { (_, list) -> list.map { it.value }.average().toFloat() }
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = false)
    }

    private fun aggregateDaily(
        values: Map<LocalDate, Float>,
        range: ChartRange,
        anchor: LocalDate,
        firstDayOfWeek: DayOfWeek,
        sum: Boolean,
    ): ChartWindow {
        val (start, end) = window(range, anchor, firstDayOfWeek)
        val inWindow = values.filterKeys { it in start..end }
        val buckets = when (range) {
            ChartRange.DAY -> listOfNotNull(
                inWindow[anchor]?.let { ChartBucket(anchor, anchor, it) },
            )
            ChartRange.WEEK, ChartRange.MONTH -> inWindow.entries
                .sortedBy { it.key }
                .map { (date, value) -> ChartBucket(date, date, value) }
            ChartRange.THREE_MONTHS -> inWindow.entries
                .groupBy { weekStart(it.key, firstDayOfWeek) }
                .toSortedMap()
                .map { (week, entries) ->
                    ChartBucket(
                        start = maxOf(week, start),
                        end = minOf(week.plusDays(6), end),
                        value = combine(entries.map { it.value }, sum),
                    )
                }
            ChartRange.YEAR -> inWindow.entries
                .groupBy { YearMonth.from(it.key) }
                .toSortedMap()
                .map { (month, entries) ->
                    ChartBucket(month.atDay(1), month.atEndOfMonth(), combine(entries.map { it.value }, sum))
                }
        }
        return ChartWindow(start, end, buckets)
    }

    private fun combine(values: List<Float>, sum: Boolean): Float =
        if (sum) values.sum() else values.average().toFloat()

    private fun weekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    private fun instant(epochSeconds: Long, zone: ZoneId) =
        Instant.ofEpochSecond(epochSeconds).atZone(zone)
}

