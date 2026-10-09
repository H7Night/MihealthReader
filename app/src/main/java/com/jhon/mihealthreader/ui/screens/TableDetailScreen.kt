package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import org.json.JSONObject

@Composable
fun TableDetailScreen(
    title: String,
    rows: List<JSONObject>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("$title（${rows.size} 行）", style = MaterialTheme.typography.titleLarge)
        if (rows.isEmpty()) {
            Text("无数据", modifier = Modifier.padding(top = 16.dp))
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rows) { row ->
                Text(
                    row.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                HorizontalDivider()
            }
        }
    }
}
