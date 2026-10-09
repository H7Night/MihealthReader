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
import com.jhon.mihealthreader.parse.BodyMeasurement

@Composable
fun BodyScreen(
    measurements: List<BodyMeasurement>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("体重 / 体成分（${measurements.size} 条）", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (measurements.isEmpty()) {
                item { Text("无数据", modifier = Modifier.padding(top = 16.dp)) }
                return@LazyColumn
            }
            items(measurements) { body ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(formatDateTime(body.time), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "体重 ${body.weightKg ?: "-"} kg · BMI ${body.bmi ?: "-"} · 体脂 ${body.bodyFatRate ?: "-"}%",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "肌肉 ${body.muscleKg ?: "-"} kg · 水分 ${body.waterPct ?: "-"}% · 骨量 ${body.boneKg ?: "-"} kg",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "基础代谢 ${body.bmrKcal ?: "-"} · 内脏脂肪 ${body.visceralFat ?: "-"} · 蛋白 ${body.proteinRate ?: "-"}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
