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
                val series = remember(activity, yearMonth) {
                    ChartData.inMonth(ChartData.stepsSeries(activity), yearMonth) { it.date }
                }
                ChartWithAxes(
                    series.map { it.value },
                    series.map { shortDate(it.date) },
                    Modifier,
                    primary,
                    bars = true,
                )
                Caption(series.lastOrNull()?.let { "${it.value.toInt()} 步 · ${it.date}" })
            }

            Metric.SLEEP -> {
                val series = remember(sleep, yearMonth) {
                    ChartData.inMonth(ChartData.sleepSeries(sleep), yearMonth) { it.date }
                }
                StackedChartWithAxes(
                    series.map {
                        listOf(
                            StackSegment(it.deep, primary),
                            StackSegment(it.light, tertiary),
                            StackSegment(it.awake, outline),
                        )
                    },
                    series.map { it.total },
                    series.map { shortDate(it.date) },
                    Modifier,
                )
                Caption(series.lastOrNull()?.let { "${it.total.toInt()} 分钟 · ${it.date}" })
            }

            Metric.HR -> {
                val series = remember(heart, yearMonth) {
                    ChartData.inMonth(ChartData.metricSeries(heart), yearMonth) { it.date }
                }
                ChartWithAxes(series.map { it.value }, series.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(series.lastOrNull()?.let { "${it.value.toInt()} bpm · ${it.date}" })
            }

            Metric.SPO2 -> {
                val series = remember(spo2, yearMonth) {
                    ChartData.inMonth(ChartData.metricSeries(spo2), yearMonth) { it.date }
                }
                ChartWithAxes(series.map { it.value }, series.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(series.lastOrNull()?.let { "${it.value.toInt()} % · ${it.date}" })
            }

            Metric.WORKOUT -> {
                val series = remember(workouts, yearMonth) {
                    ChartData.inMonth(ChartData.workoutSeries(workouts), yearMonth) { it.date }
                }
                ChartWithAxes(series.map { it.value }, series.map { shortDate(it.date) }, Modifier, primary, bars = true)
                Caption(series.lastOrNull()?.let { "${it.value.toInt()} 分钟 · ${it.date}" })
            }

            Metric.BODY -> {
                val series = remember(body, yearMonth) {
                    ChartData.inMonth(ChartData.bodySeries(body), yearMonth) { it.date }
                }
                ChartWithAxes(series.map { it.value }, series.map { shortDate(it.date) }, Modifier, primary, bars = false)
                Caption(series.lastOrNull()?.let { "${it.value} kg · ${it.date}" })
            }
        }
    }
}

@Composable
private fun Caption(text: String?) {
    Text(
        text ?: "本月无数据",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun shortDate(date: String): String = date.substringAfter('-')
