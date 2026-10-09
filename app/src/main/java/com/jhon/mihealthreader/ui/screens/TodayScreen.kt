package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.jhon.mihealthreader.local.LocalSummary
import com.jhon.mihealthreader.ui.charts.CapsuleBars
import com.jhon.mihealthreader.ui.charts.DottedLine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TodayScreen(
    parsedRepository: ParsedRepository,
    summary: LocalSummary,
    onGoSettings: () -> Unit,
) {
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val resting by parsedRepository.restingHeartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val today = LocalDate.now().toString()
    val todayActivity = activity.firstOrNull()
    val todayWorkouts = workouts.filter { localDate(it.start) == today }
    val latestSleep = sleep.firstOrNull()
    val latestHeart = heart.firstOrNull()
    val latestSpo2 = spo2.firstOrNull()
    val latestBody = body.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "今天",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
        )

        if (summary.lastExtractedAt == null) {
            Text("暂无数据，请先在设置中提取", modifier = Modifier.padding(20.dp))
            Button(onClick = onGoSettings, modifier = Modifier.padding(horizontal = 20.dp)) { Text("去设置") }
            return
        }

        val steps7 = remember(activity) { ChartData.dailySteps(activity, days = 7).map { it.value } }
        val sleep7 = remember(sleep) {
            ChartData.sleepDurations(sleep, days = 7).map { it.deep + it.light + it.awake }
        }
        val hr7 = remember(heart) { ChartData.dailyMetricAvg(heart, days = 7).map { it.value } }
        val workout7 = remember(workouts) { ChartData.workoutMinutesPerDay(workouts, days = 7).map { it.value } }
        val spo27 = remember(spo2) { ChartData.dailyMetricAvg(spo2, days = 7).map { it.value } }
        val weights = remember(body) { body.mapNotNull { it.weightKg }.map { it.toFloat() } }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                MetricGridCard(
                    title = "步数",
                    value = todayActivity?.steps?.toString() ?: "—",
                    unit = "步",
                    status = todayActivity?.let { "${it.distanceMeters} 米" },
                    mini = { CapsuleBars(steps7, Modifier.fillMaxWidth().height(44.dp), primary) },
                )
            }
            item {
                MetricGridCard(
                    title = "睡眠",
                    value = latestSleep?.let { formatMinutes(it.durationMinutes) } ?: "—",
                    unit = null,
                    status = latestSleep?.let { "深睡 ${it.deepMinutes}m" },
                    mini = { CapsuleBars(sleep7, Modifier.fillMaxWidth().height(44.dp), tertiary) },
                )
            }
            item {
                MetricGridCard(
                    title = "心率",
                    value = latestHeart?.let { it.value.toInt().toString() } ?: "—",
                    unit = "bpm",
                    status = "静息 " + (resting.firstOrNull()?.value?.toInt()?.toString() ?: "—"),
                    mini = { DottedLine(hr7, Modifier.fillMaxWidth().height(44.dp), primary) },
                )
            }
            item {
                MetricGridCard(
                    title = "运动",
                    value = todayWorkouts.size.toString(),
                    unit = "次",
                    status = if (todayWorkouts.isEmpty()) null else "共 ${formatDurationSeconds(todayWorkouts.sumOf { it.durationSec })}",
                    mini = { CapsuleBars(workout7, Modifier.fillMaxWidth().height(44.dp), primary) },
                )
            }
            item {
                MetricGridCard(
                    title = "血氧",
                    value = latestSpo2?.let { it.value.toInt().toString() } ?: "—",
                    unit = "%",
                    status = null,
                    mini = { DottedLine(spo27, Modifier.fillMaxWidth().height(44.dp), primary) },
                )
            }
            item {
                MetricGridCard(
                    title = "体重",
                    value = latestBody?.weightKg?.toString() ?: "—",
                    unit = "kg",
                    status = latestBody?.bodyFatRate?.let { "体脂 $it%" },
                    mini = { DottedLine(weights, Modifier.fillMaxWidth().height(44.dp), primary) },
                )
            }
        }
    }
}

@Composable
private fun MetricGridCard(
    title: String,
    value: String,
    unit: String?,
    status: String?,
    mini: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium)
                if (unit != null) {
                    Text(
                        " $unit",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(44.dp)) { mini() }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    status ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun localDate(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).toLocalDate().toString()
