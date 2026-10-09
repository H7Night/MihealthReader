package com.jhon.mihealthreader.ui

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
import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.TableEntry
import com.jhon.mihealthreader.local.BackendPreferences
import com.jhon.mihealthreader.root.ShizukuPermission
import com.jhon.mihealthreader.ui.nav.Routes
import com.jhon.mihealthreader.ui.nav.showsBottomBar
import com.jhon.mihealthreader.ui.screens.ChartsScreen
import com.jhon.mihealthreader.ui.screens.SettingsScreen
import com.jhon.mihealthreader.ui.screens.TableDetailScreen
import com.jhon.mihealthreader.ui.screens.TableListScreen
import com.jhon.mihealthreader.ui.screens.TodayScreen

@Composable
fun AppNav(
    extractionViewModel: ExtractionViewModel,
    repository: ExportRepository,
    parsedRepository: ParsedRepository,
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
        ) {
            composable(Routes.TODAY) {
                val summary by extractionViewModel.summary.collectAsState()
                TodayScreen(
                    summary = summary,
                    onGoSettings = { navController.navigateSingleTop(Routes.SETTINGS) },
                )
            }

            composable(Routes.CHARTS) {
                ChartsScreen()
            }

            composable(Routes.SETTINGS) {
                val state by extractionViewModel.state.collectAsState()
                val rootGranted by extractionViewModel.rootGranted.collectAsState()
                val summary by extractionViewModel.summary.collectAsState()
                val context = LocalContext.current
                val backendPrefs = remember { BackendPreferences(context) }
                var backend by remember { mutableStateOf(backendPrefs.get()) }
                var shizukuGranted by remember { mutableStateOf(ShizukuPermission.isGranted()) }
                SettingsScreen(
                    state = state,
                    rootGranted = rootGranted,
                    summary = summary,
                    backend = backend,
                    shizukuGranted = shizukuGranted,
                    onSelectBackend = {
                        backendPrefs.set(it)
                        backend = it
                    },
                    onRequestShizuku = {
                        ShizukuPermission.request(1001) { granted -> shizukuGranted = granted }
                    },
                    onStart = { extractionViewModel.start() },
                    onBrowse = { navController.navigate(Routes.BROWSE) },
                )
            }

            composable(Routes.BROWSE) {
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
}

private fun androidx.navigation.NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
