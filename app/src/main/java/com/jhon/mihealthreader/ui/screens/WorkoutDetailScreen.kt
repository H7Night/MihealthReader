package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.SportColors
import com.jhon.mihealthreader.ui.charts.ChartWithAxes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutDetailScreen(
    parsedRepository: ParsedRepository,
    index: Int,
    onBack: () -> Unit,
) {
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val workout = workouts.getOrNull(index)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        if (workout == null) {
            Text("暂无数据", modifier = Modifier.padding(20.dp))
            return
        }

        val color = SportColors.of(workout.type)
        Surface(
            color = color,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) { Text("← 返回") }
                Text(workout.typeName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    formatDateTime(workout.start),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("运动汇总", style = MaterialTheme.typography.titleMedium)
                Row {
                    Text("${workout.calories}", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        " kcal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("运动时长", formatDurationSeconds(workout.durationSec))
                    StatItem("平均心率", "${workout.avgHr ?: "-"} bpm")
                    StatItem("最大心率", "${workout.maxHr ?: "-"} bpm")
                }
                workout.distanceMeters?.let {
                    Text("距离 $it 米", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        val samples = remember(heart, workout) {
            heart.filter { it.time in workout.start..workout.end }.sortedBy { it.time }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("心率 (BPM)", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("平均心率", "${workout.avgHr ?: "-"}")
                    StatItem("最大心率", "${workout.maxHr ?: "-"}")
                }
                if (samples.size >= 2) {
                    ChartWithAxes(
                        values = samples.map { it.value.toFloat() },
                        labels = samples.map { hhmm(it.time) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        bars = false,
                    )
                } else {
                    Text("无逐点心率数据", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

private fun hhmm(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(HHMM)
