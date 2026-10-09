package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.Metric
import java.time.YearMonth

@Composable
fun MetricDetailScreen(
    metric: Metric,
    parsedRepository: ParsedRepository,
    onBack: () -> Unit,
) {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text(metric.label, style = MaterialTheme.typography.headlineSmall)

        MonthSelector(yearMonth = yearMonth, onChange = { yearMonth = it })

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
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
