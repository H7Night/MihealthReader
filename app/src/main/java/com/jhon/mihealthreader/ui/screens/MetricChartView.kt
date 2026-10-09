package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ChartData
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.Metric
import com.jhon.mihealthreader.ui.charts.ChartWithAxes
import com.jhon.mihealthreader.ui.charts.StackSegment
import com.jhon.mihealthreader.ui.charts.StackedChartWithAxes
import java.time.YearMonth

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

    Column(modifier = modifier) {
        when (metric) {
            Metric.STEPS -> {
                val w = remember(activity, yearMonth) {
                    window(ChartData.stepsSeries(activity), yearMonth) { it.date }
                }
                ChartWithAxes(w.items.map { it.value }, w.items.map { shortDate(it.date) }, Modifier, primary, bars = true)
                Caption(w, w.items.lastOrNull()?.let { "${it.value.toInt()} 步 · ${it.date}" })
            }

            Metric.SLEEP -> {
                val w = remember(sleep, yearMonth) {
                    window(ChartData.sleepSeries(sleep), yearMonth) { it.date }
                }
                StackedChartWithAxes(
                    w.items.map {
                        listOf(
                            StackSegment(it.deep, primary),
                            StackSegment(it.light, tertiary),
                            StackSegment(it.awake, outline),
                        )
                    },
                    w.items.map { it.total },
                    w.items.map { shortDate(it.date) },
                    Modifier,
                )
                Caption(w, w.items.lastOrNull()?.let { "${it.total.toInt()} 分钟 · ${it.date}" })
            }

            Metric.HR -> {
                val w = remember(heart, yearMonth) {
                    window(ChartData.metricSeries(heart), yearMonth) { it.date }
                }
                ChartWithAxes(w.items.map { it.value }, w.items.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(w, w.items.lastOrNull()?.let { "${it.value.toInt()} bpm · ${it.date}" })
            }

            Metric.SPO2 -> {
                val w = remember(spo2, yearMonth) {
                    window(ChartData.metricSeries(spo2), yearMonth) { it.date }
                }
                ChartWithAxes(w.items.map { it.value }, w.items.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(w, w.items.lastOrNull()?.let { "${it.value.toInt()} % · ${it.date}" })
            }

            Metric.WORKOUT -> {
                val w = remember(workouts, yearMonth) {
                    window(ChartData.workoutSeries(workouts), yearMonth) { it.date }
                }
                ChartWithAxes(w.items.map { it.value }, w.items.map { shortDate(it.date) }, Modifier, primary, bars = true)
                Caption(w, w.items.lastOrNull()?.let { "${it.value.toInt()} 分钟 · ${it.date}" })
            }

            Metric.BODY -> {
                val w = remember(body, yearMonth) {
                    window(ChartData.bodySeries(body), yearMonth) { it.date }
                }
                ChartWithAxes(w.items.map { it.value }, w.items.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(w, w.items.lastOrNull()?.let { "${it.value} kg · ${it.date}" })
            }
        }
    }
}

@Composable
private fun Caption(window: Windowed<*>, text: String?) {
    val prefix = if (window.fallback && window.items.isNotEmpty()) "本月无数据，显示最近10次 · " else ""
    Text(
        prefix + (text ?: "暂无数据"),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun shortDate(date: String): String = date.substringAfter('-')
