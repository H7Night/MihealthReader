package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.SportColors

@Composable
fun WorkoutListScreen(
    parsedRepository: ParsedRepository,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("运动（${workouts.size} 次）", style = MaterialTheme.typography.headlineSmall)

        if (workouts.isEmpty()) {
            Text("暂无数据", modifier = Modifier.padding(top = 16.dp))
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            itemsIndexed(workouts) { index, workout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(index) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(12.dp).background(SportColors.of(workout.type), CircleShape))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(workout.typeName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${formatDateTime(workout.start)} · ${formatDurationSeconds(workout.durationSec)} · " +
                                "${workout.calories} kcal · 平均 ${workout.avgHr ?: "-"} bpm",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
