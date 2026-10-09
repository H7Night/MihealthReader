package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.jhon.mihealthreader.data.TableEntry

@Composable
fun TableListScreen(
    tables: List<TableEntry>,
    onTable: (TableEntry) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("数据表（${tables.size}）", style = MaterialTheme.typography.titleLarge)
        if (tables.isEmpty()) {
            Text("没有可显示的表", modifier = Modifier.padding(top = 16.dp))
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tables) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTable(entry) }
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${entry.dbName} · ${entry.rowCount} 行",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
