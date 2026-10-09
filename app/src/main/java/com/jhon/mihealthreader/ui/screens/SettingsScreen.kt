package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.local.BackendType
import com.jhon.mihealthreader.local.LocalSummary
import com.jhon.mihealthreader.ui.ExtractionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    state: ExtractionState,
    rootGranted: Boolean?,
    summary: LocalSummary,
    backend: BackendType,
    shizukuGranted: Boolean,
    onSelectBackend: (BackendType) -> Unit,
    onRequestShizuku: () -> Unit,
    onStart: () -> Unit,
    onBrowse: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        Text("数据源", style = MaterialTheme.typography.titleMedium)
        Text(
            "当前：" + when (backend) {
                BackendType.ROOT -> "Root"
                BackendType.SHIZUKU -> "Shizuku"
            } + "（" + when (rootGranted) {
                true -> "已授权"
                false -> "未授权"
                null -> "检测中…"
            } + "）",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onSelectBackend(BackendType.ROOT) }) { Text("Root") }
            Button(onClick = { onSelectBackend(BackendType.SHIZUKU) }) { Text("Shizuku") }
        }
        if (backend == BackendType.SHIZUKU && !shizukuGranted) {
            Button(onClick = onRequestShizuku) { Text("请求 Shizuku 权限") }
            Text("Shizuku 需以 root 启动才能读取数据", style = MaterialTheme.typography.bodySmall)
        }
        Text("切换数据源后请重启 App 生效", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()

        Text("数据提取", style = MaterialTheme.typography.titleMedium)
        Text(summaryText(summary), style = MaterialTheme.typography.bodyMedium)
        when (state) {
            ExtractionState.Idle -> Button(onClick = onStart) { Text("提取数据") }
            is ExtractionState.Running -> {
                Button(onClick = {}, enabled = false) { Text("提取中…") }
                Text(state.step)
            }
            is ExtractionState.Success -> {
                Text(if (state.newRecords > 0) "新增 ${state.newRecords} 条记录" else "无新数据")
                Button(onClick = onStart) { Text("再次提取") }
            }
            is ExtractionState.Error -> {
                Text("错误：${state.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onStart) { Text("重试") }
            }
        }

        HorizontalDivider()

        Text("高级", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onBrowse) { Text("浏览原始表（调试）") }

        HorizontalDivider()

        Text("关于", style = MaterialTheme.typography.titleMedium)
        Text("MihealthReader · 读取「小米运动健康」本地数据库并做本地记录与分析")
    }
}

private val SUMMARY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun summaryText(summary: LocalSummary): String {
    val last = summary.lastExtractedAt
    val whenText = if (last == null) {
        "尚未同步"
    } else {
        "上次同步：" + Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).format(SUMMARY_FORMAT)
    }
    return "$whenText · 步数样本 ${summary.stepSamples} · 睡眠 ${summary.sleepSessions} 段"
}
