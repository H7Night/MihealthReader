package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.parse.SleepSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SleepScreen(
    sessions: List<SleepSession>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("睡眠（${sessions.size} 段）", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (sessions.isEmpty()) {
                item { Text("无数据", modifier = Modifier.padding(top = 16.dp)) }
                return@LazyColumn
            }
            items(sessions) { session ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(formatDate(session.start), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${formatDuration(session.durationMinutes)} · 深睡 ${session.deepMinutes}m · 浅睡 ${session.lightMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "心率 平均 ${session.avgHr ?: "-"} / 最低 ${session.minHr ?: "-"} / 最高 ${session.maxHr ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (session.stages.isNotEmpty()) {
                        Text(
                            session.stages.joinToString("  ") {
                                "${stageLabel(it.stateName)} ${(it.end - it.start) / 60}m"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatDate(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)

private fun formatDuration(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

private fun stageLabel(stateName: String): String = when (stateName) {
    "awake" -> "清醒"
    "light" -> "浅睡"
    "deep" -> "深睡"
    "rem" -> "REM"
    else -> stateName
}
