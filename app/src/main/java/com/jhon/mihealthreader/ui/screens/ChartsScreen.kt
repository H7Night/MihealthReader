package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ChartData
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.SleepSummary
import com.jhon.mihealthreader.ui.Metric
import com.jhon.mihealthreader.ui.MetricChartKind
import com.jhon.mihealthreader.ui.charts.CapsuleBars
import com.jhon.mihealthreader.ui.charts.DottedLine
import com.jhon.mihealthreader.ui.component.GhCard
import com.jhon.mihealthreader.ui.component.GhIconPill
import com.jhon.mihealthreader.ui.component.GhLoadingState
import com.jhon.mihealthreader.ui.component.GhMetricTile
import com.jhon.mihealthreader.ui.component.GhPageTitle
import com.jhon.mihealthreader.ui.component.GhSectionHeader
import com.jhon.mihealthreader.ui.component.GhTone
import com.jhon.mihealthreader.ui.component.ghToneContent
import com.jhon.mihealthreader.ui.component.shouldShowLoading
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ChartsScreen(
    parsedRepository: ParsedRepository,
    dataLoaded: Boolean,
    onOpenDetail: (Metric) -> Unit,
    onOpenSleepDetail: () -> Unit,
    onOpenWorkoutList: () -> Unit,
) {
    if (shouldShowLoading(dataLoaded)) {
        GhLoadingState(background = MaterialTheme.colorScheme.surface)
        return
    }
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val scheme = MaterialTheme.colorScheme
    val steps = remember(activity) { ChartData.lastN(ChartData.stepsSeries(activity), 7).map { it.value } }
    val sleepBars = remember(sleep) { ChartData.lastN(ChartData.sleepSeries(sleep), 7).map { it.total } }
    val hr = remember(heart) { ChartData.lastN(ChartData.metricSeries(heart), 7).map { it.value } }
    val spo = remember(spo2) { ChartData.lastN(ChartData.metricSeries(spo2), 7).map { it.value } }
    val workoutBars = remember(workouts) { ChartData.lastN(ChartData.workoutSeries(workouts), 7).map { it.value } }
    val weights = remember(body) { ChartData.lastN(ChartData.bodySeries(body), 7).map { it.value } }
    val mainSleep = remember(sleep) { SleepSummary.mainSleep(sleep) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GhPageTitle(stringResource(R.string.title_charts))
        GhSectionHeader(stringResource(R.string.section_main_metrics))

        Metric.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { metric ->
                    val data = when (metric) {
                        Metric.STEPS -> MetricTileData(
                            value = activity.firstOrNull()?.steps?.toString() ?: "—",
                            unit = stringResource(R.string.unit_steps),
                            status = activity.firstOrNull()?.let {
                                stringResource(
                                    R.string.today_steps_status_distance,
                                    "%.1f".format(it.distanceMeters / 1000f),
                                )
                            },
                            series = steps,
                            tone = GhTone.Primary,
                        )
                        Metric.SLEEP -> MetricTileData(
                            value = mainSleep?.let { formatMinutesCompact(it.durationMinutes) } ?: "—",
                            unit = null,
                            status = mainSleep?.let {
                                stringResource(R.string.today_sleep_deep, it.deepMinutes)
                            },
                            series = sleepBars,
                            tone = GhTone.Tertiary,
                        )
                        Metric.HR -> MetricTileData(
                            value = heart.firstOrNull()?.value?.toInt()?.toString() ?: "—",
                            unit = stringResource(R.string.unit_bpm),
                            status = null,
                            series = hr,
                            tone = GhTone.Secondary,
                        )
                        Metric.SPO2 -> MetricTileData(
                            value = spo2.firstOrNull()?.value?.toInt()?.toString() ?: "—",
                            unit = stringResource(R.string.unit_percent),
                            status = null,
                            series = spo,
                            tone = GhTone.Neutral,
                        )
                        Metric.WORKOUT -> MetricTileData(
                            value = workouts.count {
                                it.start >= LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                            }.toString(),
                            unit = null,
                            status = null,
                            series = workoutBars,
                            tone = GhTone.Primary,
                        )
                        Metric.BODY -> MetricTileData(
                            value = body.firstOrNull()?.weightKg?.toString() ?: "—",
                            unit = stringResource(R.string.unit_kg),
                            status = body.firstOrNull()?.bodyFatRate?.let { "%.1f%%".format(it) },
                            series = weights,
                            tone = GhTone.Neutral,
                        )
                    }
                    GhMetricTile(
                        title = stringResource(metric.labelRes),
                        value = data.value,
                        unit = data.unit,
                        status = data.status,
                        tone = data.tone,
                        onClick = { onOpenDetail(metric) },
                        modifier = Modifier.weight(1f).height(240.dp),
                        valueStyle = if (metric == Metric.SLEEP) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.displaySmall
                        },
                    ) {
                        val color = ghToneContent(scheme, data.tone).copy(alpha = 0.75f)
                        if (metric.chartKind == MetricChartKind.Bars) {
                            CapsuleBars(data.series, Modifier.fillMaxSize(), color)
                        } else {
                            DottedLine(data.series, Modifier.fillMaxSize(), color)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        GhSectionHeader(stringResource(R.string.section_categories))
        CategoryCard(
            icon = Icons.Filled.DirectionsRun,
            title = stringResource(R.string.category_activity),
            subtitle = stringResource(R.string.tracked_metrics, 2),
            onClick = { onOpenDetail(Metric.STEPS) },
        )
        CategoryCard(
            icon = Icons.Filled.Bedtime,
            title = stringResource(R.string.category_sleep),
            subtitle = stringResource(R.string.tracked_metrics, 1),
            onClick = onOpenSleepDetail,
        )
        CategoryCard(
            icon = Icons.Filled.MonitorWeight,
            title = stringResource(R.string.category_body),
            subtitle = stringResource(R.string.tracked_metrics, 1),
            onClick = { onOpenDetail(Metric.BODY) },
        )
    }
}

private data class MetricTileData(
    val value: String,
    val unit: String?,
    val status: String?,
    val series: List<Float>,
    val tone: GhTone,
)

@Composable
private fun CategoryCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    GhCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhIconPill(icon, null, scheme.primaryContainer, scheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}





