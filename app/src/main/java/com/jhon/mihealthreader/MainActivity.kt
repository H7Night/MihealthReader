package com.jhon.mihealthreader

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.discover.PackageScanner
import com.jhon.mihealthreader.discover.TargetLocator
import com.jhon.mihealthreader.dump.DbDumper
import com.jhon.mihealthreader.dump.ManifestWriter
import com.jhon.mihealthreader.extract.DbExtractor
import com.jhon.mihealthreader.extract.SourceReader
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.local.MiHealthDatabase
import com.jhon.mihealthreader.root.BackendManager
import com.jhon.mihealthreader.ui.AppNav
import com.jhon.mihealthreader.ui.ExtractionViewModel
import com.jhon.mihealthreader.ui.theme.MiHealthTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shell = BackendManager(this).current()
        val exportsDir = File(filesDir, "exports")
        val repository = ExportRepository(exportsDir)
        val localStore = LocalStore(MiHealthDatabase.build(applicationContext))
        val parsedRepository = ParsedRepository(localStore)
        val scanner = PackageScanner {
            packageManager.getInstalledPackages(0).map { it.packageName }
        }
        val extractionViewModel = ExtractionViewModel(
            rootShell = shell,
            targetFinder = TargetLocator(shell, scanner),
            extractor = DbExtractor(SourceReader { path -> shell.openFile(path) }),
            dumper = DbDumper(),
            manifestWriter = ManifestWriter(),
            repository = repository,
            androidVersion = Build.VERSION.SDK_INT,
            appVersion = "0.1.0",
            exportsDir = exportsDir,
            localStore = localStore,
        )

        setContent {
            MiHealthTheme {
                AppNav(extractionViewModel, repository, parsedRepository, exportsDir)
            }
        }
    }
}
