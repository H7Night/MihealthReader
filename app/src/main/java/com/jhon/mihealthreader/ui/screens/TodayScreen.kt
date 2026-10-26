package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.SleepSummary
import com.jhon.mihealthreader.data.TodaySummary
import com.jhon.mihealthreader.local.LocalSummary
import com.jhon.mihealthreader.ui.ExtractionState
import com.jhon.mihealthreader.ui.Metric
import com.jhon.mihealthreader.ui.component.GhIconActionButton
import com.jhon.mihealthreader.ui.component.GhIconPill
import com.jhon.mihealthreader.ui.component.GhListRow
import com.jhon.mihealthreader.ui.component.GhLoadingState
import com.jhon.mihealthreader.ui.component.GhPillButton
import com.jhon.mihealthreader.ui.component.GhRing
import com.jhon.mihealthreader.ui.component.GhStatusPill
import com.jhon.mihealthreader.ui.component.GhTone
import com.jhon.mihealthreader.ui.component.GhTintedCard
import com.jhon.mihealthreader.ui.component.ghRingProgress
import com.jhon.mihealthreader.ui.component.ghToneContent
import com.jhon.mihealthreader.ui.component.shouldShowLoading
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    parsedRepository: ParsedRepository,
    summary: LocalSummary,
    dataLoaded: Boolean,
    extractionState: ExtractionState,
    onOpenDetail: (Metric) -> Unit,
    onGoSettings: () -> Unit,
    onStartExtraction: () -> Unit = {},
    onSyncHealthConnect: () -> Unit = {},
) {
    if (shouldShowLoading(dataLoaded)) {
        GhLoadingState()
        return
    }
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val resting by parsedRepository.restingHeartRate().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val today = LocalDate.now()
    val todayActivity = activity.firstOrNull { it.date == today.toString() }
    val mainSleep = remember(sleep) { SleepSummary.mainSleep(sleep) }
    val naps = remember(sleep, mainSleep) {
        SleepSummary.sameDayNaps(sleep).filter { it !== mainSleep }
    }
    val napMinutes = naps.sumOf { it.durationMinutes }
    val weekly = remember(workouts, today) { TodaySummary.weeklyWorkouts(workouts, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.title_today),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            GhStatusPill(
                if (!dataLoaded) stringResource(R.string.label_loading)
                else updateStatusLine(extractionState, summary.lastExtractedAt),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(0.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                GhRing(
                    progress = ghRingProgress(weekly.days, 7),
                    centerValue = stringResource(R.string.today_ring_progress, weekly.days, 7),
                    centerLabel = stringResource(R.string.today_ring_label),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Text(
                    stringResource(R.string.today_workout_minutes, weekly.minutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroCard(
                    tone = GhTone.Primary,
                    icon = Icons.Filled.DirectionsWalk,
                    title = stringResource(R.string.metric_steps),
                    value = todayActivity?.steps?.toString() ?: stringResource(R.string.label_unknown),
                    unit = stringResource(R.string.unit_steps),
                    status = todayActivity?.let {
                        stringResource(
                            R.string.today_steps_status_distance,
                            "%.1f".format(it.distanceMeters / 1000f),
                        )
                    },
                    onClick = { onOpenDetail(Metric.STEPS) },
                )
                HeroCard(
                    tone = GhTone.Tertiary,
                    icon = Icons.Filled.Nightlight,
                    title = stringResource(R.string.metric_sleep),
                    value = mainSleep?.let { formatMinutesCompact(it.durationMinutes + napMinutes) }
                        ?: stringResource(R.string.label_unknown),
                    unit = null,
                    status = when {
                        mainSleep == null -> null
                        napMinutes > 0 -> stringResource(R.string.today_sleep_nap, napMinutes)
                        else -> stringResource(R.string.today_sleep_deep, mainSleep.deepMinutes)
                    },
                    onClick = { onOpenDetail(Metric.SLEEP) },
                )
                HeroCard(
                    tone = GhTone.Secondary,
                    icon = Icons.Filled.Favorite,
                    title = stringResource(R.string.metric_hr),
                    value = heart.firstOrNull()?.value?.toInt()?.toString()
                        ?: stringResource(R.string.label_unknown),
                    unit = stringResource(R.string.unit_bpm),
                    status = stringResource(
                        R.string.today_hr_resting,
                        resting.firstOrNull()?.value?.toInt()?.toString()
                            ?: stringResource(R.string.label_unknown),
                    ),
                    onClick = { onOpenDetail(Metric.HR) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .width(if (index == 0) 18.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhPillButton(
                text = stringResource(R.string.action_extract),
                onClick = onStartExtraction,
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Refresh,
            )
            GhPillButton(
                text = stringResource(R.string.action_sync),
                onClick = onSyncHealthConnect,
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Sync,
            )
            GhIconActionButton(
                Icons.Filled.Settings,
                stringResource(R.string.nav_settings),
                onGoSettings,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.today_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                )

                if (dataLoaded && summary.lastExtractedAt == null) {
                    GhTintedCard(GhTone.Primary) {
                        Text(
                            stringResource(R.string.today_no_data_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.today_no_data_message),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        GhPillButton(stringResource(R.string.action_go_settings), onGoSettings)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Metric.entries.forEach { metric ->
                        Surface(
                            onClick = { onOpenDetail(metric) },
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                stringResource(metric.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.section_recent),
                    style = MaterialTheme.typography.titleLarge,
                )
                workouts.firstOrNull()?.let {
                    GhListRow(
                        label = it.typeName,
                        value = stringResource(
                            R.string.today_recent_workout,
                            formatDateTime(it.start),
                            formatDurationText(it.durationSec),
                        ),
                        onClick = { onOpenDetail(Metric.WORKOUT) },
                    )
                }
                sleep.firstOrNull()?.let {
                    GhListRow(
                        label = stringResource(R.string.today_recent_sleep),
                        value = formatMinutesText(it.durationMinutes),
                        onClick = { onOpenDetail(Metric.SLEEP) },
                    )
                }
                body.firstOrNull()?.weightKg?.let {
                    GhListRow(
                        label = stringResource(R.string.metric_body),
                        value = stringResource(R.string.today_recent_weight, it.toString()),
                        onClick = { onOpenDetail(Metric.BODY) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    tone: GhTone,
    icon: ImageVector,
    title: String,
    value: String,
    unit: String?,
    status: String?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GhTintedCard(tone = tone, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GhIconPill(
                icon = icon,
                contentDescription = null,
                container = ghToneContent(scheme, tone).copy(alpha = 0.10f),
                contentColor = ghToneContent(scheme, tone),
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                    unit?.let {
                        Text(
                            " $it",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun updateStatusLine(state: ExtractionState, lastExtractedAt: Long?): String = when {
    state is ExtractionState.Running -> stringResource(R.string.today_status_running)
    lastExtractedAt != null -> stringResource(
        R.string.status_updated,
        Instant.ofEpochMilli(lastExtractedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm")),
    )
    else -> stringResource(R.string.label_no_data)
}








