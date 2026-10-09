package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.ui.ExtractionState

@Composable
fun ExtractionScreen(
    state: ExtractionState,
    rootGranted: Boolean?,
    onStart: () -> Unit,
    onBrowse: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MihealthReader", style = MaterialTheme.typography.headlineSmall)
        Text("root 状态：" + when (rootGranted) {
            true -> "已授权"
            false -> "未授权"
            null -> "检测中…"
        })

        when (state) {
            ExtractionState.Idle -> Button(onClick = onStart) { Text("开始提取") }

            is ExtractionState.Running -> {
                Button(onClick = {}, enabled = false) { Text("提取中…") }
                Text(state.step)
            }

            is ExtractionState.Success -> {
                Text("完成：共 ${state.tableCount} 张表")
                Text("睡眠 ${state.sleepSessions} 段 · 活动 ${state.activityDays} 天")
                Text(state.snapshot.absolutePath, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onBrowse) { Text("浏览数据") }
            }

            is ExtractionState.Error -> {
                Text("错误：${state.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onStart) { Text("重试") }
            }
        }
    }
}
