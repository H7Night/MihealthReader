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
import com.jhon.mihealthreader.parse.Workout

@Composable
fun WorkoutScreen(
    workouts: List<Workout>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("运动（${workouts.size} 次）", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (workouts.isEmpty()) {
                item { Text("无数据", modifier = Modifier.padding(top = 16.dp)) }
                return@LazyColumn
            }
            items(workouts) { workout ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text("${workout.typeName} · ${formatDateTime(workout.start)}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${formatDurationSeconds(workout.durationSec)} · ${workout.calories} kcal · " +
                            "平均 ${workout.avgHr ?: "-"} bpm" +
                            (workout.distanceMeters?.let { " · ${it} m" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
