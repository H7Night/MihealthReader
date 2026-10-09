package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.local.LocalSummary
import com.jhon.mihealthreader.ui.ExtractionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExtractionScreen(
    state: ExtractionState,
    rootGranted: Boolean?,
    summary: LocalSummary,
    onStart: () -> Unit,
    onBrowse: () -> Unit,
    onActivity: () -> Unit,
    onSleep: () -> Unit,
    onHeartRate: () -> Unit,
    onSpo2: () -> Unit,
    onWorkout: () -> Unit,
    onBody: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("MihealthReader", style = MaterialTheme.typography.headlineSmall)
        Text("root 状态：" + when (rootGranted) {
            true -> "已授权"
            false -> "未授权"
            null -> "检测中…"
        })
        Text(summaryText(summary), style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()

        when (state) {
            ExtractionState.Idle -> Button(onClick = onStart) { Text("提取数据") }

            is ExtractionState.Running -> {
                Button(onClick = {}, enabled = false) { Text("提取中…") }
                Text(state.step)
            }

            is ExtractionState.Success -> {
                Text("完成：共 ${state.tableCount} 张表")
                Text(if (state.newRecords > 0) "新增 ${state.newRecords} 条记录" else "无新数据")
                Button(onClick = onStart) { Text("再次提取") }
            }

            is ExtractionState.Error -> {
                Text("错误：${state.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onStart) { Text("重试") }
            }
        }

        HorizontalDivider()

        Text("数据导航", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onActivity) { Text("活动视图") }
        Button(onClick = onSleep) { Text("睡眠视图") }
        Button(onClick = onHeartRate) { Text("心率视图") }
        Button(onClick = onSpo2) { Text("血氧视图") }
        Button(onClick = onWorkout) { Text("运动视图") }
        Button(onClick = onBody) { Text("体重视图") }
        Button(onClick = onBrowse) { Text("浏览数据（原始表）") }
    }
}

private val SUMMARY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun summaryText(summary: LocalSummary): String {
    val last = summary.lastExtractedAt
    val whenText = if (last == null) {
        "尚未同步"
    } else {
        "上次同步：" + Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).format(SUMMARY_FORMAT)
    }
    return "$whenText · 步数样本 ${summary.stepSamples} · 睡眠 ${summary.sleepSessions} 段"
}
