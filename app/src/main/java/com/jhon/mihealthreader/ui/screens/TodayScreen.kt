package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.local.LocalSummary
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

    val today = LocalDate.now().toString()
    val todayActivity = activity.firstOrNull()
    val todayWorkouts = workouts.filter { localDate(it.start) == today }
    val workoutSeconds = todayWorkouts.sumOf { it.durationSec }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("今天", style = MaterialTheme.typography.headlineSmall)

        if (summary.lastExtractedAt == null) {
            Text("暂无数据，请先在设置中提取", modifier = Modifier.padding(top = 12.dp))
            Button(onClick = onGoSettings, modifier = Modifier.padding(top = 12.dp)) { Text("去设置") }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MetricCard(
                    "步数",
                    if (todayActivity == null) "暂无" else "${todayActivity.steps} 步",
                    todayActivity?.let { "${it.distanceMeters} 米 · ${it.calories} kcal" },
                )
            }
            item {
                val latest = sleep.firstOrNull()
                MetricCard(
                    "睡眠",
                    if (latest == null) "暂无" else formatMinutes(latest.durationMinutes),
                    latest?.let { "深睡 ${it.deepMinutes}m · 浅睡 ${it.lightMinutes}m" },
                )
            }
            item {
                val hr = heart.firstOrNull()
                MetricCard(
                    "心率",
                    if (hr == null) "暂无" else "${hr.value.toInt()} bpm",
                    "静息 " + (resting.firstOrNull()?.value?.toInt()?.toString() ?: "-") + " bpm",
                )
            }
            item {
                MetricCard(
                    "运动",
                    "${todayWorkouts.size} 次",
                    if (todayWorkouts.isEmpty()) null else "共 ${formatDurationSeconds(workoutSeconds)}",
                )
            }
            item {
                val s = spo2.firstOrNull()
                MetricCard("血氧", if (s == null) "暂无" else "${s.value.toInt()} %", null)
            }
            item {
                val b = body.firstOrNull()
                MetricCard(
                    "体重",
                    b?.weightKg?.let { "$it kg" } ?: "暂无",
                    b?.bodyFatRate?.let { "体脂 $it%" },
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun localDate(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).toLocalDate().toString()
