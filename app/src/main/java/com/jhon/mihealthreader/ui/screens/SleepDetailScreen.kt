package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.SleepSummary
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.ui.charts.DottedLine
import com.jhon.mihealthreader.ui.charts.HypnoSegment
import com.jhon.mihealthreader.ui.charts.Hypnogram
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SleepDetailScreen(
    parsedRepository: ParsedRepository,
    onBack: () -> Unit,
) {
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    var index by remember { mutableStateOf(0) }
    val session = sleep.getOrNull(index)

    val deepColor = MaterialTheme.colorScheme.primary
    val lightColor = MaterialTheme.colorScheme.tertiary
    val awakeColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("睡眠", style = MaterialTheme.typography.headlineSmall)

        if (session == null) {
            Text("暂无数据")
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { index += 1 }, enabled = index < sleep.size - 1) {
                Text("‹", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                formatNight(session.start),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = { index -= 1 }, enabled = index > 0) {
                Text("›", style = MaterialTheme.typography.titleLarge)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("睡眠分析", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("睡眠总时长", formatMinutesCn(session.durationMinutes))
                    StatItem("入睡时间", SleepSummary.hhmm(session.start))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("深度睡眠比率", "${SleepSummary.deepRatioPct(session)}%")
                    StatItem("清醒次数", "${SleepSummary.awakeCount(session)} 次")
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                SleepSummary.describe(session),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }

        val recent = remember(sleep) { sleep.filter { SleepSummary.isNightSleep(it) }.take(14) }
        val regularity = remember(recent) { SleepSummary.bedtimeRegularity(recent) }
        if (regularity != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("入睡时间规律性", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem("平均入睡", SleepSummary.formatBedtime(regularity.averageMinutes))
                        StatItem("波动", "±${regularity.stdMinutes} 分钟")
                        StatItem("评价", SleepSummary.regularityLabel(regularity.stdMinutes))
                    }
                    val values = remember(recent) {
                        recent.reversed().map { SleepSummary.bedtimeMinutes(it).toFloat() }
                    }
                    if (values.size >= 2) {
                        DottedLine(values, Modifier.fillMaxWidth().height(48.dp), MaterialTheme.colorScheme.primary)
                    }
                    Text("近 ${regularity.nights} 晚", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("全天睡眠", style = MaterialTheme.typography.titleMedium)
                Text(formatMinutesCn(session.durationMinutes), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(deepColor, "深睡")
                    LegendDot(lightColor, "浅睡")
                    LegendDot(awakeColor, "清醒")
                }
                if (session.stages.isNotEmpty()) {
                    Hypnogram(
                        segments = session.stages.map { HypnoSegment(it.start, it.end, it.stateName) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        deepColor = deepColor,
                        lightColor = lightColor,
                        awakeColor = awakeColor,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(SleepSummary.hhmm(session.start), style = MaterialTheme.typography.labelSmall)
                        Text(SleepSummary.hhmm(session.end), style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text("无阶段数据")
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
    }
}

private val NIGHT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun formatNight(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(NIGHT_FORMAT)

private fun formatMinutesCn(minutes: Int): String = "${minutes / 60} 小时 ${minutes % 60} 分钟"
