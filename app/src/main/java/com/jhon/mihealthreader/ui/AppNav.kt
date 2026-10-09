package com.jhon.mihealthreader.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.jhon.mihealthreader.data.ExportManager
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.local.BackendPreferences
import com.jhon.mihealthreader.root.ShizukuPermission
import com.jhon.mihealthreader.ui.nav.Routes
import com.jhon.mihealthreader.ui.nav.showsBottomBar
import com.jhon.mihealthreader.ui.screens.ChartsScreen
import com.jhon.mihealthreader.ui.screens.MetricDetailScreen
import com.jhon.mihealthreader.ui.screens.SettingsScreen
import com.jhon.mihealthreader.ui.screens.SleepDetailScreen
import com.jhon.mihealthreader.ui.screens.TodayScreen
import java.io.File

@Composable
fun AppNav(
    extractionViewModel: ExtractionViewModel,
    parsedRepository: ParsedRepository,
    exportsDir: File,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (showsBottomBar(currentRoute)) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.TODAY,
                        onClick = { navController.navigateSingleTop(Routes.TODAY) },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = "今天") },
                        label = { Text("今天") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.CHARTS,
                        onClick = { navController.navigateSingleTop(Routes.CHARTS) },
                        icon = { Icon(Icons.Filled.List, contentDescription = "视图") },
                        label = { Text("视图") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navController.navigateSingleTop(Routes.SETTINGS) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.TODAY) {
                val summary by extractionViewModel.summary.collectAsState()
                TodayScreen(
                    parsedRepository = parsedRepository,
                    summary = summary,
                    onOpenDetail = { metric ->
                        if (metric == Metric.SLEEP) {
                            navController.navigate("sleep_detail")
                        } else {
                            navController.navigate("detail/${metric.name}")
                        }
                    },
                    onGoSettings = { navController.navigateSingleTop(Routes.SETTINGS) },
                )
            }

            composable(Routes.CHARTS) {
                ChartsScreen(
                    parsedRepository = parsedRepository,
                    onOpenSleepDetail = { navController.navigate("sleep_detail") },
                )
            }

            composable("sleep_detail") {
                SleepDetailScreen(
                    parsedRepository = parsedRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("detail/{metric}") { entry ->
                val metric = Metric.valueOf(entry.arguments?.getString("metric") ?: Metric.STEPS.name)
                MetricDetailScreen(
                    metric = metric,
                    parsedRepository = parsedRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                val state by extractionViewModel.state.collectAsState()
                val rootGranted by extractionViewModel.rootGranted.collectAsState()
                val summary by extractionViewModel.summary.collectAsState()
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
