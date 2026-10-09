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
import com.jhon.mihealthreader.data.TableEntry
import com.jhon.mihealthreader.ui.screens.ExtractionScreen
import com.jhon.mihealthreader.ui.screens.TableDetailScreen
import com.jhon.mihealthreader.ui.screens.TableListScreen

@Composable
fun AppNav(
    extractionViewModel: ExtractionViewModel,
    repository: ExportRepository,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "extract") {
        composable("extract") {
            val state by extractionViewModel.state.collectAsState()
            val rootGranted by extractionViewModel.rootGranted.collectAsState()
            ExtractionScreen(
                state = state,
                rootGranted = rootGranted,
                onStart = { extractionViewModel.start() },
                onBrowse = { navController.navigate("browse") },
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
