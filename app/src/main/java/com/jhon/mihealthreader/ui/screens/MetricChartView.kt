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

enum class TimeMode { LAST_N, MONTH }

@Composable
fun MetricChartView(
    metric: Metric,
    parsedRepository: ParsedRepository,
    timeMode: TimeMode,
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
                val series = remember(activity) { ChartData.stepsSeries(activity) }
                val windowed = window(series, timeMode, yearMonth) { it.date }
                ChartWithAxes(
                    windowed.map { it.value },
                    windowed.map { shortDate(it.date) },
                    Modifier,
                    primary,
                    bars = true,
                )
                Caption(windowed.lastOrNull()?.let { "${it.value.toInt()} 步 · ${it.date}" })
            }

            Metric.SLEEP -> {
                val series = remember(sleep) { ChartData.sleepSeries(sleep) }
                val windowed = window(series, timeMode, yearMonth) { it.date }
                StackedChartWithAxes(
                    windowed.map {
                        listOf(
                            StackSegment(it.deep, primary),
                            StackSegment(it.light, tertiary),
                            StackSegment(it.awake, outline),
                        )
                    },
                    windowed.map { it.total },
                    windowed.map { shortDate(it.date) },
                    Modifier,
                )
                Caption(windowed.lastOrNull()?.let { "${it.total.toInt()} 分钟 · ${it.date}" })
            }

            Metric.HR -> {
                val series = remember(heart) { ChartData.metricSeries(heart) }
                val windowed = window(series, timeMode, yearMonth) { it.date }
                ChartWithAxes(windowed.map { it.value }, windowed.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(windowed.lastOrNull()?.let { "${it.value.toInt()} bpm · ${it.date}" })
            }

            Metric.SPO2 -> {
                val series = remember(spo2) { ChartData.metricSeries(spo2) }
                val windowed = window(series, timeMode, yearMonth) { it.date }
                ChartWithAxes(windowed.map { it.value }, windowed.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(windowed.lastOrNull()?.let { "${it.value.toInt()} % · ${it.date}" })
            }

            Metric.WORKOUT -> {
                val series = remember(workouts) { ChartData.workoutSeries(workouts) }
                val windowed = window(series, timeMode, yearMonth) { it.date }
                ChartWithAxes(windowed.map { it.value }, windowed.map { shortDate(it.date) }, Modifier, primary, bars = true)
                Caption(windowed.lastOrNull()?.let { "${it.value.toInt()} 分钟 · ${it.date}" })
            }

            Metric.BODY -> {
                val series = remember(body) { ChartData.bodySeries(body) }
                val windowed = window(series, timeMode, yearMonth) { it.date }
                ChartWithAxes(windowed.map { it.value }, windowed.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(windowed.lastOrNull()?.let { "${it.value} kg · ${it.date}" })
            }
        }
    }
}

@Composable
private fun Caption(text: String?) {
    Text(
        text ?: "该区间无数据",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun <T> window(list: List<T>, mode: TimeMode, yearMonth: YearMonth, dateOf: (T) -> String): List<T> =
    when (mode) {
        TimeMode.LAST_N -> ChartData.lastN(list, 10)
        TimeMode.MONTH -> ChartData.inMonth(list, yearMonth, dateOf)
    }

private fun shortDate(date: String): String = date.substringAfter('-')
