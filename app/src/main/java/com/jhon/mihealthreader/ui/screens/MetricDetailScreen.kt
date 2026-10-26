package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ChartBucket
import com.jhon.mihealthreader.data.ChartRange
import com.jhon.mihealthreader.data.ChartSeries
import com.jhon.mihealthreader.data.ChartWindow
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.Metric
import com.jhon.mihealthreader.ui.MetricChartKind
import com.jhon.mihealthreader.ui.charts.GhChart
import com.jhon.mihealthreader.ui.charts.GhChartStyle
import com.jhon.mihealthreader.ui.component.GhEmptyState
import com.jhon.mihealthreader.ui.component.GhListRow
import com.jhon.mihealthreader.ui.component.GhLoadingState
import com.jhon.mihealthreader.ui.component.GhRangeHeader
import com.jhon.mihealthreader.ui.component.GhSectionHeader
import com.jhon.mihealthreader.ui.component.GhSegment
import com.jhon.mihealthreader.ui.component.GhSegmentedControl
import com.jhon.mihealthreader.ui.component.GhStatusPill
import com.jhon.mihealthreader.ui.component.GhTopBar
import com.jhon.mihealthreader.ui.component.shouldShowLoading
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricDetailScreen(
    metric: Metric,
    parsedRepository: ParsedRepository,
    dataLoaded: Boolean,
    onBack: () -> Unit,
) {
    if (shouldShowLoading(dataLoaded)) {
        GhLoadingState(background = MaterialTheme.colorScheme.surface)
        return
    }
    var range by rememberSaveable { mutableStateOf(ChartRange.WEEK) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val window = remember(metric, range, anchor, activity, heart, spo2, workouts, body) {
        when (metric) {
            Metric.STEPS -> ChartSeries.steps(activity, range, anchor)
            Metric.HR -> ChartSeries.heartRate(heart, range, anchor)
            Metric.SPO2 -> ChartSeries.spo2(spo2, range, anchor)
            Metric.WORKOUT -> ChartSeries.workouts(workouts, range, anchor)
            Metric.BODY -> ChartSeries.weight(body, range, anchor)
            Metric.SLEEP -> ChartWindow(anchor, anchor, emptyList())
        }
    }
    val values = window.buckets.map { it.value }
    val labels = window.buckets.map { bucketLabel(range, it) }
    val style = when (metric.chartKind) {
        MetricChartKind.Bars -> GhChartStyle.Bars
        MetricChartKind.Dotted -> GhChartStyle.DottedLine
        MetricChartKind.Area -> GhChartStyle.AreaLine
    }
    val unit = stringResource(metric.unitRes)
    val latest = values.lastOrNull()

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { GhTopBar(stringResource(metric.labelRes), onBack) }
        item {
            GhSegmentedControl(
                options = listOf(
                    ChartRange.DAY to R.string.range_day,
                    ChartRange.WEEK to R.string.range_week,
                    ChartRange.MONTH to R.string.range_month,
                    ChartRange.THREE_MONTHS to R.string.range_three_months,
                    ChartRange.YEAR to R.string.range_year,
                ).map { GhSegment(it.first, stringResource(it.second)) },
                selected = range,
                onSelect = {
                    range = it
                    anchor = LocalDate.now()
                },
            )
        }
        item {
            GhRangeHeader(
                label = rangeLabel(window, range),
                onPrevious = { anchor = ChartSeries.shift(range, anchor, -1) },
                onNext = { anchor = ChartSeries.shift(range, anchor, 1) },
                onToday = { anchor = LocalDate.now() },
                nextEnabled = canMoveNext(window.end),
            )
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    latest?.let(::formatChartValue) ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    " $unit",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        item {
            when {
                values.size >= 2 -> GhChart(
                    values = values,
                    labels = labels,
                    style = style,
                    selectedIndex = values.lastIndex,
                    valueFormatter = ::formatChartValue,
                )
                values.isEmpty() -> GhEmptyState(stringResource(R.string.detail_no_range_data))
            }
        }
        if (values.isNotEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val pillContainer = MaterialTheme.colorScheme.surfaceContainer
                    GhStatusPill(
                        stringResource(R.string.stat_min) + " " + formatChartValue(values.min()),
                        container = pillContainer,
                    )
                    GhStatusPill(
                        stringResource(R.string.stat_avg) + " " +
                            formatChartValue(values.average().toFloat()),
                        container = pillContainer,
                    )
                    GhStatusPill(
                        stringResource(R.string.stat_max) + " " + formatChartValue(values.max()),
                        container = pillContainer,
                    )
                }
            }
            item { GhSectionHeader(stringResource(R.string.section_records)) }
            items(window.buckets.reversed()) { bucket ->
                GhListRow(
                    label = bucketLabel(range, bucket),
                    value = formatChartValue(bucket.value) + " " + unit,
                )
            }
        }
    }
}

@Composable
private fun rangeLabel(window: ChartWindow, range: ChartRange): String {
    val locale = Locale.getDefault()
    val sameYear = window.start.year == window.end.year
    val separator = stringResource(R.string.range_separator)
    return when (range) {
        ChartRange.YEAR -> window.start.year.toString()
        ChartRange.MONTH -> DateTimeFormatter.ofPattern("yyyy/M", locale).format(window.start)
        ChartRange.THREE_MONTHS -> {
            val start = DateTimeFormatter.ofPattern("yyyy/M", locale).format(window.start)
            val endPattern = if (sameYear) "M" else "yyyy/M"
            start + separator + DateTimeFormatter.ofPattern(endPattern, locale).format(window.end)
        }
        ChartRange.WEEK -> {
            val pattern = if (sameYear) "M/d" else "yyyy/M/d"
            val fmt = DateTimeFormatter.ofPattern(pattern, locale)
            fmt.format(window.start) + separator + fmt.format(window.end)
        }
        ChartRange.DAY -> DateTimeFormatter.ofPattern("yyyy/M/d", locale).format(window.start)
    }
}

@Composable
private fun bucketLabel(range: ChartRange, bucket: ChartBucket): String {
    val locale = Locale.getDefault()
    return when {
        bucket.hour != null -> "%02d:00".format(bucket.hour)
        range == ChartRange.DAY -> stringResource(R.string.action_today)
        range == ChartRange.WEEK -> bucket.start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        range == ChartRange.MONTH || range == ChartRange.THREE_MONTHS ->
            DateTimeFormatter.ofPattern("M/d", locale).format(bucket.start)
        else -> DateTimeFormatter.ofPattern("MMM", locale).format(bucket.start)
    }
}

private fun formatChartValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)




