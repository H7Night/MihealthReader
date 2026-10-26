package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.SleepSummary
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.ui.charts.GhChart
import com.jhon.mihealthreader.ui.charts.GhChartStyle
import com.jhon.mihealthreader.ui.charts.HypnoSegment
import com.jhon.mihealthreader.ui.charts.Hypnogram
import com.jhon.mihealthreader.ui.component.GhCard
import com.jhon.mihealthreader.ui.component.GhEmptyState
import com.jhon.mihealthreader.ui.component.GhLoadingState
import com.jhon.mihealthreader.ui.component.GhRangeHeader
import com.jhon.mihealthreader.ui.component.GhStatusPill
import com.jhon.mihealthreader.ui.component.GhTintedCard
import com.jhon.mihealthreader.ui.component.GhTone
import com.jhon.mihealthreader.ui.component.GhTopBar
import com.jhon.mihealthreader.ui.component.shouldShowLoading
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepDetailScreen(
    parsedRepository: ParsedRepository,
    dataLoaded: Boolean,
    onBack: () -> Unit,
) {
    if (shouldShowLoading(dataLoaded)) {
        GhLoadingState(background = MaterialTheme.colorScheme.surface)
        return
    }
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val main = remember(sleep) { SleepSummary.mainSleep(sleep) }
    val mainIndex = remember(sleep, main) { sleep.indexOfFirst { it === main }.coerceAtLeast(0) }
    var index by remember(sleep) { mutableStateOf(mainIndex) }
    val session = sleep.getOrNull(index)
    val scheme = MaterialTheme.colorScheme
    val deepColor = scheme.primary
    val lightColor = scheme.tertiary
    val awakeColor = scheme.error
    val napColor = scheme.secondary

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
            .navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GhTopBar(stringResource(R.string.sleep_title), onBack)
        if (session == null) {
            GhEmptyState(stringResource(R.string.sleep_no_data))
            return
        }

        GhRangeHeader(
            label = formatNight(session.start),
            onPrevious = { if (index < sleep.size - 1) index += 1 },
            onNext = { if (index > 0) index -= 1 },
            onToday = { index = sleep.indexOfFirst { it === main }.coerceAtLeast(0) },
            nextEnabled = index > 0,
        )

        val pagePill = MaterialTheme.colorScheme.surfaceContainer
        Text(formatMinutesText(session.durationMinutes), style = MaterialTheme.typography.displaySmall)
        val dayNaps = remember(sleep, session) {
            SleepSummary.napsOnDay(sleep, SleepSummary.endDate(session)).filter { it !== session }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhStatusPill(
                stringResource(R.string.sleep_deep_pct, SleepSummary.deepRatioPct(session)),
                container = pagePill,
            )
            if (dayNaps.isNotEmpty()) {
                GhStatusPill(
                    stringResource(
                        R.string.sleep_naps_summary,
                        dayNaps.sumOf { it.durationMinutes },
                        dayNaps.size,
                    ),
                    container = pagePill,
                )
            }
        }

        GhCard {
            Text(stringResource(R.string.sleep_analysis), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatItem(stringResource(R.string.sleep_total), formatMinutesText(session.durationMinutes), Modifier.weight(1f))
                StatItem(stringResource(R.string.sleep_bedtime), SleepSummary.hhmm(session.start), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatItem(stringResource(R.string.sleep_deep_ratio), "${SleepSummary.deepRatioPct(session)}%", Modifier.weight(1f))
                StatItem(stringResource(R.string.sleep_awake_count), "${SleepSummary.awakeCount(session)}", Modifier.weight(1f))
            }
        }

        val day = SleepSummary.endDate(session)
        val daySessions = remember(sleep, day) {
            SleepSummary.daySessions(sleep, day).sortedBy { it.start }
        }
        val segments = remember(daySessions) {
            daySessions.flatMap { s ->
                if (s.stages.isNotEmpty()) {
                    s.stages.map { HypnoSegment(it.start, it.end, it.stateName) }
                } else {
                    listOf(HypnoSegment(s.start, s.end, "nap"))
                }
            }
        }

        GhCard {
            Text(stringResource(R.string.sleep_timeline), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(deepColor, stringResource(R.string.sleep_deep))
                LegendDot(lightColor, stringResource(R.string.sleep_light))
                LegendDot(awakeColor, stringResource(R.string.sleep_awake))
                LegendDot(napColor, stringResource(R.string.sleep_nap))
            }
            Spacer(Modifier.height(12.dp))
            if (segments.isEmpty()) {
                Text(stringResource(R.string.sleep_no_stage))
            } else {
                Hypnogram(
                    segments = segments,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    deepColor = deepColor,
                    lightColor = lightColor,
                    awakeColor = awakeColor,
                    napColor = napColor,
                    gridColor = scheme.outlineVariant,
                )
            }
        }

        val b = sleepStageBreakdown(session)
        GhCard {
            Text(stringResource(R.string.sleep_total), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StageBar(b.deep, deepColor, Modifier.weight(b.deep.coerceAtLeast(1).toFloat()))
                StageBar(b.light, lightColor, Modifier.weight(b.light.coerceAtLeast(1).toFloat()))
                StageBar(b.awake, awakeColor, Modifier.weight(b.awake.coerceAtLeast(1).toFloat()))
            }
            Spacer(Modifier.height(8.dp))
            Text("${b.deep}% / ${b.light}% / ${b.awake}%", style = MaterialTheme.typography.labelMedium)
        }

        GhTintedCard(GhTone.Tertiary) {
            Text(stringResource(R.string.sleep_interpretation), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(sleepQualityRes(session)),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                stringResource(
                    R.string.sleep_details,
                    formatMinutesText(session.durationMinutes),
                    SleepSummary.hhmm(session.start),
                    SleepSummary.hhmm(session.end),
                    SleepSummary.deepRatioPct(session),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val recent = remember(sleep) { sleep.filter { SleepSummary.isNightSleep(it) }.take(14) }
        val regularity = remember(recent) { SleepSummary.bedtimeRegularity(recent) }
        if (regularity != null) {
            GhCard {
                Text(stringResource(R.string.sleep_regularity), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatItem(
                        stringResource(R.string.sleep_avg_bedtime),
                        SleepSummary.formatBedtime(regularity.averageMinutes),
                    )
                    StatItem(
                        stringResource(R.string.sleep_variation),
                        "±${regularity.stdMinutes} " + stringResource(R.string.unit_minutes),
                    )
                    StatItem(
                        stringResource(R.string.sleep_rating),
                        stringResource(regularityLabelRes(regularity.stdMinutes)),
                    )
                }
                val values = remember(recent) {
                    recent.reversed().map { SleepSummary.bedtimeMinutes(it).toFloat() }
                }
                val labels = remember(recent) {
                    recent.reversed().map {
                        Instant.ofEpochSecond(it.start).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("M/d"))
                    }
                }
                if (values.size >= 2) {
                    GhChart(
                        values = values,
                        labels = labels,
                        modifier = Modifier.padding(top = 12.dp),
                        style = GhChartStyle.DottedLine,
                        valueFormatter = { SleepSummary.formatBedtime(it.toInt()) },
                    )
                }
                Text(
                    stringResource(R.string.sleep_recent_nights, regularity.nights),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun StageBar(percent: Int, color: Color, modifier: Modifier) {
    Box(modifier.height(10.dp).clip(CircleShape).background(color))
}

private val NIGHT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun formatNight(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(NIGHT_FORMAT)







