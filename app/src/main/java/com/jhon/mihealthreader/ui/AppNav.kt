package com.jhon.mihealthreader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.TableEntry
import com.jhon.mihealthreader.ui.screens.ActivityScreen
import com.jhon.mihealthreader.ui.screens.BodyScreen
import com.jhon.mihealthreader.ui.screens.ExtractionScreen
import com.jhon.mihealthreader.ui.screens.HeartRateScreen
import com.jhon.mihealthreader.ui.screens.SleepScreen
import com.jhon.mihealthreader.ui.screens.Spo2Screen
import com.jhon.mihealthreader.ui.screens.TableDetailScreen
import com.jhon.mihealthreader.ui.screens.TableListScreen
import com.jhon.mihealthreader.ui.screens.WorkoutScreen

@Composable
fun AppNav(
    extractionViewModel: ExtractionViewModel,
    repository: ExportRepository,
    parsedRepository: ParsedRepository,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "extract") {
        composable("extract") {
            val state by extractionViewModel.state.collectAsState()
            val rootGranted by extractionViewModel.rootGranted.collectAsState()
            val summary by extractionViewModel.summary.collectAsState()
            ExtractionScreen(
                state = state,
                rootGranted = rootGranted,
                summary = summary,
                onStart = { extractionViewModel.start() },
                onBrowse = { navController.navigate("browse") },
                onActivity = { navController.navigate("activity") },
                onSleep = { navController.navigate("sleep") },
                onHeartRate = { navController.navigate("heartrate") },
                onSpo2 = { navController.navigate("spo2") },
                onWorkout = { navController.navigate("workout") },
                onBody = { navController.navigate("body") },
            )
        }

        composable("activity") {
            val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
            ActivityScreen(
                activity = activity,
                onBack = { navController.popBackStack() },
            )
        }

        composable("sleep") {
            val sessions by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
            SleepScreen(
                sessions = sessions,
                onBack = { navController.popBackStack() },
            )
        }

        composable("heartrate") {
            val resting by parsedRepository.restingHeartRate().collectAsState(initial = emptyList())
            val samples by parsedRepository.heartRate().collectAsState(initial = emptyList())
            HeartRateScreen(
                resting = resting,
                samples = samples,
                onBack = { navController.popBackStack() },
            )
        }

        composable("spo2") {
            val samples by parsedRepository.spo2().collectAsState(initial = emptyList())
            Spo2Screen(
                samples = samples,
                onBack = { navController.popBackStack() },
            )
        }

        composable("workout") {
            val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
            WorkoutScreen(
                workouts = workouts,
                onBack = { navController.popBackStack() },
            )
        }

        composable("body") {
            val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())
            BodyScreen(
                measurements = body,
                onBack = { navController.popBackStack() },
            )
        }

        composable("browse") {
            val browser = remember { BrowserViewModel(repository, repository.latestSnapshot()) }
            var selected by remember { mutableStateOf<TableEntry?>(null) }
            val entry = selected
            if (entry == null) {
                TableListScreen(
                    tables = browser.tables(),
                    onTable = { selected = it },
                    onBack = { navController.popBackStack() },
                )
            } else {
                TableDetailScreen(
                    title = entry.name,
                    rows = browser.rows(entry),
                    onBack = { selected = null },
                )
            }
        }
    }
}
