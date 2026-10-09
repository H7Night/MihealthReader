package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ChartData
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.charts.BarChart
import com.jhon.mihealthreader.ui.charts.LineChart
import com.jhon.mihealthreader.ui.charts.StackSegment
import com.jhon.mihealthreader.ui.charts.StackedBarChart

private enum class Metric(val label: String) {
    STEPS("步数"),
    SLEEP("睡眠"),
    HR("心率"),
    SPO2("血氧"),
    WORKOUT("运动"),
    BODY("体重"),
}

@Composable
fun ChartsScreen(parsedRepository: ParsedRepository) {
    var metric by remember { mutableStateOf(Metric.STEPS) }

    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("视图", style = MaterialTheme.typography.headlineSmall)

        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Metric.entries.forEach { m ->
                FilterChip(
                    selected = metric == m,
                    onClick = { metric = m },
                    label = { Text(m.label) },
                )
            }
        }

        when (metric) {
            Metric.STEPS -> {
                val data = remember(activity) { ChartData.dailySteps(activity) }
                ChartBlock(data.any { it.value > 0f }, "近 30 天步数") {
                    BarChart(data.map { it.value }, Modifier.fillMaxWidth().height(220.dp), primary)
                }
            }

            Metric.SLEEP -> {
                val rows = remember(sleep) { ChartData.sleepDurations(sleep) }
                val hasData = rows.any { it.deep + it.light + it.awake > 0f }
                ChartBlock(hasData, "近 30 天睡眠（深/浅/清醒）") {
                    StackedBarChart(
                        rows.map {
                            listOf(
                                StackSegment(it.deep, primary),
                                StackSegment(it.light, secondary),
                                StackSegment(it.awake, outline),
                            )
                        },
                        Modifier.fillMaxWidth().height(220.dp),
                    )
                }
            }

            Metric.HR -> {
                val data = remember(heart) { ChartData.dailyMetricAvg(heart) }
                ChartBlock(data.any { it.value > 0f }, "近 30 天平均心率") {
                    LineChart(data.map { it.value }, Modifier.fillMaxWidth().height(220.dp), primary)
                }
            }

            Metric.SPO2 -> {
                val data = remember(spo2) { ChartData.dailyMetricAvg(spo2) }
                ChartBlock(data.any { it.value > 0f }, "近 30 天平均血氧") {
                    LineChart(data.map { it.value }, Modifier.fillMaxWidth().height(220.dp), primary)
                }
            }

            Metric.WORKOUT -> {
                val data = remember(workouts) { ChartData.workoutMinutesPerDay(workouts) }
                ChartBlock(data.any { it.value > 0f }, "近 30 天运动时长（分钟）") {
                    BarChart(data.map { it.value }, Modifier.fillMaxWidth().height(220.dp), primary)
                }
            }

            Metric.BODY -> {
                val values = remember(body) { body.mapNotNull { it.weightKg }.map { it.toFloat() } }
                ChartBlock(values.size >= 2, "体重趋势（kg）") {
                    LineChart(values, Modifier.fillMaxWidth().height(220.dp), primary)
                }
            }
        }
    }
}

@Composable
private fun ChartBlock(hasData: Boolean, title: String, chart: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (hasData) {
                    chart()
                } else {
                    Text("暂无数据")
                }
            }
        }
    }
}
