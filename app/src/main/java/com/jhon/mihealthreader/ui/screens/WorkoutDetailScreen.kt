package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.SportColors
import com.jhon.mihealthreader.ui.SportIcons
import com.jhon.mihealthreader.ui.charts.GhChart
import com.jhon.mihealthreader.ui.charts.GhChartStyle
import com.jhon.mihealthreader.ui.component.GhCard
import com.jhon.mihealthreader.ui.component.GhEmptyState
import com.jhon.mihealthreader.ui.component.GhIconPill
import com.jhon.mihealthreader.ui.component.GhLoadingState
import com.jhon.mihealthreader.ui.component.GhTopBar
import com.jhon.mihealthreader.ui.component.shouldShowLoading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutDetailScreen(
    parsedRepository: ParsedRepository,
    dataLoaded: Boolean,
    index: Int,
    onBack: () -> Unit,
) {
    if (shouldShowLoading(dataLoaded)) {
        GhLoadingState(background = MaterialTheme.colorScheme.surface)
        return
    }
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val workout = workouts.getOrNull(index)
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
            .navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (workout == null) {
            GhTopBar(stringResource(R.string.workout_title), onBack)
            GhEmptyState(stringResource(R.string.label_no_data))
            return
        }
        GhTopBar(workout.typeName, onBack)
        val sport = SportColors.of(workout.type)
        Surface(
            color = sport.copy(alpha = 0.14f),
            contentColor = scheme.onSurface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                GhIconPill(
                    SportIcons.of(workout.type),
                    null,
                    sport.copy(alpha = 0.20f),
                    scheme.onSurface,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(workout.typeName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        formatDateTime(workout.start),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(workout.calories.toString(), style = MaterialTheme.typography.displaySmall)
                        Text(
                            " " + stringResource(R.string.unit_kcal),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                stringResource(R.string.detail_duration),
                formatDurationText(workout.durationSec),
                Modifier.weight(1f),
            )
            StatTile(
                stringResource(R.string.detail_avg_hr),
                "${workout.avgHr ?: "—"} " + stringResource(R.string.unit_bpm),
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                stringResource(R.string.detail_max_hr),
                "${workout.maxHr ?: "—"} " + stringResource(R.string.unit_bpm),
                Modifier.weight(1f),
            )
            StatTile(
                stringResource(R.string.detail_distance),
                workout.distanceMeters?.let { "$it " + stringResource(R.string.unit_meters) } ?: "—",
                Modifier.weight(1f),
            )
        }

        val samples = remember(heart, workout) {
            heart.filter { it.time in workout.start..workout.end }.sortedBy { it.time }
        }
        GhCard {
            Text(stringResource(R.string.workout_heart_rate), style = MaterialTheme.typography.titleLarge)
            if (samples.size >= 2) {
                GhChart(
                    values = samples.map { it.value.toFloat() },
                    labels = samples.map { hhmm(it.time) },
                    style = GhChartStyle.DottedLine,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Text(
                    stringResource(R.string.workout_no_samples),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    GhCard(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

private fun hhmm(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(HHMM)






