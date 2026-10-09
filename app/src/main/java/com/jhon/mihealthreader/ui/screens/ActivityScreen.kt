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
import com.jhon.mihealthreader.parse.DailyActivity

@Composable
fun ActivityScreen(
    activity: List<DailyActivity>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("活动（${activity.size} 天）", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (activity.isEmpty()) {
                item { Text("无数据", modifier = Modifier.padding(top = 16.dp)) }
                return@LazyColumn
            }
            val latest = activity.first()
            item {
                Text(
                    "最近：${latest.date} · ${latest.steps} 步 · ${latest.distanceMeters} 米 · ${latest.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                HorizontalDivider()
            }
            items(activity) { day ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(day.date, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${day.steps} 步 · ${day.distanceMeters} 米 · ${day.calories} kcal",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
