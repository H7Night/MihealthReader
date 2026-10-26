package com.jhon.mihealthreader.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ExportManager
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.local.BackendPreferences
import com.jhon.mihealthreader.root.ShizukuPermission
import com.jhon.mihealthreader.ui.component.GhBottomBar
import com.jhon.mihealthreader.ui.component.GhTab
import com.jhon.mihealthreader.ui.nav.Routes
import com.jhon.mihealthreader.ui.nav.showsBottomBar
import com.jhon.mihealthreader.ui.screens.ChartsScreen
import com.jhon.mihealthreader.ui.screens.MetricDestination
import com.jhon.mihealthreader.ui.screens.MetricDetailScreen
import com.jhon.mihealthreader.ui.screens.metricDestination
import com.jhon.mihealthreader.ui.screens.SettingsScreen
import com.jhon.mihealthreader.ui.screens.SleepDetailScreen
import com.jhon.mihealthreader.ui.screens.TodayScreen
import com.jhon.mihealthreader.ui.screens.WorkoutDetailScreen
import com.jhon.mihealthreader.ui.screens.WorkoutListScreen
import java.io.File

private val mainTabs = listOf(
    GhTab(Routes.TODAY, R.string.nav_today, Icons.Filled.WbSunny),
    GhTab(Routes.CHARTS, R.string.nav_charts, Icons.Filled.Insights),
    GhTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun AppNav(
    extractionViewModel: ExtractionViewModel,
) {
    val parsedRepository = extractionViewModel.parsedRepository
    val exportsDir = extractionViewModel.exportsDir
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val dataLoaded by extractionViewModel.dataLoaded.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showsBottomBar(currentRoute)) {
                GhBottomBar(
                    tabs = mainTabs,
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.navigateSingleTop(route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 10 }
            },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = {
                fadeOut(tween(120)) + slideOutHorizontally(tween(220)) { it / 10 }
            },
        ) {
            composable(Routes.TODAY) {
                val summary by extractionViewModel.summary.collectAsState()
                val extractionState by extractionViewModel.state.collectAsState()
                TodayScreen(
                    parsedRepository = parsedRepository,
                    summary = summary,
                    dataLoaded = dataLoaded,
                    extractionState = extractionState,
                    onOpenDetail = { metric ->
                        when (metric) {
                            Metric.SLEEP -> navController.navigate("sleep_detail")
                            Metric.WORKOUT -> navController.navigate("workout_list")
                            else -> navController.navigate("detail/${metric.name}")
                        }
                    },
                    onGoSettings = { navController.navigateSingleTop(Routes.SETTINGS) },
                    onStartExtraction = { extractionViewModel.start() },
                    onSyncHealthConnect = { extractionViewModel.syncHealthConnect() },
                )
            }

            composable(Routes.CHARTS) {
                ChartsScreen(
                    parsedRepository = parsedRepository,
                    dataLoaded = dataLoaded,
                    onOpenDetail = { metric ->
                        when (val destination = metricDestination(metric)) {
                            MetricDestination.Sleep -> navController.navigate("sleep_detail")
                            MetricDestination.WorkoutList -> navController.navigate("workout_list")
                            is MetricDestination.Detail -> navController.navigate("detail/${destination.metric.name}")
                        }
                    },
                    onOpenSleepDetail = { navController.navigate("sleep_detail") },
                    onOpenWorkoutList = { navController.navigate("workout_list") },
                )
            }

            composable("sleep_detail") {
                SleepDetailScreen(
                    parsedRepository = parsedRepository,
                    dataLoaded = dataLoaded,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("workout_list") {
                WorkoutListScreen(
                    parsedRepository = parsedRepository,
                    dataLoaded = dataLoaded,
                    onOpen = { navController.navigate("workout_detail/$it") },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("workout_detail/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                WorkoutDetailScreen(
                    parsedRepository = parsedRepository,
                    dataLoaded = dataLoaded,
                    index = index,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("detail/{metric}") { entry ->
                val metric = Metric.valueOf(entry.arguments?.getString("metric") ?: Metric.STEPS.name)
                MetricDetailScreen(
                    metric = metric,
                    parsedRepository = parsedRepository,
                    dataLoaded = dataLoaded,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                val state by extractionViewModel.state.collectAsState()
                val rootGranted by extractionViewModel.rootGranted.collectAsState()
                val summary by extractionViewModel.summary.collectAsState()
                val hcEnabled by extractionViewModel.healthConnectEnabled.collectAsState()
                val hcMessage by extractionViewModel.healthConnectMessage.collectAsState()
                val context = LocalContext.current
                val backendPrefs = remember { BackendPreferences(context) }
                val exportManager = remember { ExportManager(exportsDir) }
                var backend by remember { mutableStateOf(backendPrefs.get()) }
                var shizukuGranted by remember { mutableStateOf(ShizukuPermission.isGranted()) }
                SettingsScreen(
                    state = state,
                    rootGranted = rootGranted,
                    summary = summary,
                    backend = backend,
                    shizukuGranted = shizukuGranted,
                    exportManager = exportManager,
                    onSelectBackend = {
                        backendPrefs.set(it)
                        backend = it
                    },
                    onRequestShizuku = {
                        ShizukuPermission.request(1001) { granted -> shizukuGranted = granted }
                    },
                    onStart = { extractionViewModel.start() },
                    healthConnectAvailable = extractionViewModel.healthConnectAvailable,
                    healthConnectEnabled = hcEnabled,
                    healthConnectPermissions = extractionViewModel.healthConnectPermissions,
                    healthConnectMessage = hcMessage,
                    onToggleHealthConnect = { extractionViewModel.setHealthConnectEnabled(it) },
                    onHealthPermissionsResult = { extractionViewModel.onHealthPermissionsResult(it) },
                    onSyncHealthConnect = { extractionViewModel.syncHealthConnect() },
                )
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}


