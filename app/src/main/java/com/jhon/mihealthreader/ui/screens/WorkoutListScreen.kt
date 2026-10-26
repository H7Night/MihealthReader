package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.ui.SportColors
import com.jhon.mihealthreader.ui.SportIcons
import com.jhon.mihealthreader.ui.component.GhCard
import com.jhon.mihealthreader.ui.component.GhEmptyState
import com.jhon.mihealthreader.ui.component.GhIconPill
import com.jhon.mihealthreader.ui.component.GhLoadingState
import com.jhon.mihealthreader.ui.component.GhStatusPill
import com.jhon.mihealthreader.ui.component.GhTopBar
import com.jhon.mihealthreader.ui.component.shouldShowLoading

@Composable
fun WorkoutListScreen(
    parsedRepository: ParsedRepository,
    dataLoaded: Boolean,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
) {
    if (shouldShowLoading(dataLoaded)) {
        GhLoadingState(background = MaterialTheme.colorScheme.surface)
        return
    }
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    val options = remember(workouts) { workoutTypeOptions(workouts) }
    val filtered = remember(workouts, type) { filterWorkouts(workouts, type) }
    val indexes = remember(workouts, filtered) { filtered.map { workouts.indexOf(it) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
        GhTopBar(
            stringResource(R.string.workout_title),
            onBack,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            GhStatusPill(
                stringResource(R.string.workout_count, workouts.size),
                modifier = Modifier.padding(end = 8.dp),
                container = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhFilterPill(stringResource(R.string.workout_all), type == null) { type = null }
            options.forEach { option ->
                GhFilterPill(option, type == option) { type = option }
            }
        }
        if (workouts.isEmpty()) {
            GhEmptyState(stringResource(R.string.workout_empty), icon = Icons.Filled.DirectionsRun)
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(filtered) { i, workout ->
                GhCard(onClick = { onOpen(indexes[i]) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GhIconPill(
                            icon = SportIcons.of(workout.type),
                            contentDescription = null,
                            container = SportColors.of(workout.type).copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(workout.typeName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatDateTime(workout.start) + " · " +
                                    formatDurationText(workout.durationSec) + " · " +
                                    workout.calories + " kcal · " +
                                    "${workout.avgHr ?: "—"} bpm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GhFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}





