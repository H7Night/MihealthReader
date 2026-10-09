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
import com.jhon.mihealthreader.local.LocalSummary

@Composable
fun TodayScreen(
    summary: LocalSummary,
    onGoSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("今天", style = MaterialTheme.typography.headlineSmall)
        if (summary.lastExtractedAt == null) {
            Text("暂无数据，请先在设置中提取")
            Button(onClick = onGoSettings) { Text("去设置") }
        } else {
            Text("步数样本 ${summary.stepSamples} · 睡眠 ${summary.sleepSessions} 段")
            Text("图表开发中…", style = MaterialTheme.typography.bodySmall)
        }
    }
}
