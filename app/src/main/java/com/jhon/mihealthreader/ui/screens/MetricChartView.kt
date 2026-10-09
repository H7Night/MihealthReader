package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ChartData
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.Metric
import com.jhon.mihealthreader.ui.charts.ChartWithAxes
import com.jhon.mihealthreader.ui.charts.StackSegment
import com.jhon.mihealthreader.ui.charts.StackedChartWithAxes
import java.time.YearMonth

private class ChartSpec(
    val values: List<Float>,
    val labels: List<String>,
    val unit: String,
    val bars: Boolean,
    val stacked: List<List<StackSegment>>?,
    val fallback: Boolean,
)

private class Windowed<T>(val items: List<T>, val fallback: Boolean)

private fun <T> window(series: List<T>, yearMonth: YearMonth, dateOf: (T) -> String): Windowed<T> {
    val month = ChartData.inMonth(series, yearMonth, dateOf)
    return if (month.isNotEmpty()) {
        Windowed(month, fallback = false)
    } else {
        Windowed(ChartData.lastN(series, 10), fallback = true)
    }
}

@Composable
fun MetricChartView(
    metric: Metric,
    parsedRepository: ParsedRepository,
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline

    val spec: ChartSpec = when (metric) {
        Metric.STEPS -> {
            val w = remember(activity, yearMonth) {
                window(ChartData.stepsSeries(activity), yearMonth) { it.date }
            }
            ChartSpec(w.items.map { it.value }, w.items.map { shortDate(it.date) }, "步", true, null, w.fallback)
        }

        Metric.SLEEP -> {
            val w = remember(sleep, yearMonth) {
                window(ChartData.sleepSeries(sleep), yearMonth) { it.date }
            }
            ChartSpec(
                w.items.map { it.total },
                w.items.map { shortDate(it.date) },
                "分钟",
                false,
                w.items.map {
                    listOf(
                        StackSegment(it.deep, primary),
                        StackSegment(it.light, tertiary),
                        StackSegment(it.awake, outline),
                    )
                },
                w.fallback,
            )
        }

        Metric.HR -> {
            val w = remember(heart, yearMonth) {
                window(ChartData.metricSeries(heart), yearMonth) { it.date }
            }
            ChartSpec(w.items.map { it.value }, w.items.map { shortDate(it.date) }, "bpm", false, null, w.fallback)
        }

        Metric.SPO2 -> {
            val w = remember(spo2, yearMonth) {
                window(ChartData.metricSeries(spo2), yearMonth) { it.date }
            }
            ChartSpec(w.items.map { it.value }, w.items.map { shortDate(it.date) }, "%", false, null, w.fallback)
        }

        Metric.WORKOUT -> {
            val w = remember(workouts, yearMonth) {
                window(ChartData.workoutSeries(workouts), yearMonth) { it.date }
            }
            ChartSpec(w.items.map { it.value }, w.items.map { shortDate(it.date) }, "分钟", true, null, w.fallback)
        }

        Metric.BODY -> {
            val w = remember(body, yearMonth) {
                window(ChartData.bodySeries(body), yearMonth) { it.date }
            }
            ChartSpec(w.items.map { it.value }, w.items.map { shortDate(it.date) }, "kg", false, null, w.fallback)
        }
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                spec.values.lastOrNull()?.let { formatValue(it) } ?: "—",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                " ${spec.unit}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        if (spec.stacked != null) {
            StackedChartWithAxes(spec.stacked, spec.values, spec.labels, Modifier.padding(top = 8.dp))
        } else {
            ChartWithAxes(spec.values, spec.labels, Modifier.padding(top = 8.dp), primary, bars = spec.bars)
        }

        SummaryRow(spec.values, spec.unit)

        if (spec.fallback) {
            Text(
                "本月无数据，显示最近 10 次",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(values: List<Float>, unit: String) {
    if (values.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryItem("最小", formatValue(values.min()), unit)
        SummaryItem("平均", formatValue(values.average().toFloat()), unit)
        SummaryItem("最大", formatValue(values.max()), unit)
    }
}

@Composable
private fun SummaryItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text("$value $unit", style = MaterialTheme.typography.titleSmall)
    }
}

private fun formatValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)

private fun shortDate(date: String): String = date.substringAfter('-')
