package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.Metric
import java.time.YearMonth

@Composable
fun ChartsScreen(parsedRepository: ParsedRepository) {
    var metric by remember { mutableStateOf(Metric.STEPS) }
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("视图", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Metric.entries.forEach { m ->
                FilterChip(
                    selected = metric == m,
                    onClick = { metric = m },
                    label = { Text(m.label) },
                )
            }
        }

        MonthSelector(yearMonth = yearMonth, onChange = { yearMonth = it })

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                MetricChartView(
                    metric = metric,
                    parsedRepository = parsedRepository,
                    yearMonth = yearMonth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun MonthSelector(yearMonth: YearMonth, onChange: (YearMonth) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange(yearMonth.minusMonths(1)) }) { Text("‹") }
        Text(
            "${yearMonth.year} 年 ${yearMonth.monthValue} 月",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        TextButton(onClick = { onChange(yearMonth.plusMonths(1)) }) { Text("›") }
    }
}
