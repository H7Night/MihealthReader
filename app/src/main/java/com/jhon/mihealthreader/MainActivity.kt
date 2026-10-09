package com.jhon.mihealthreader

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.discover.PackageScanner
import com.jhon.mihealthreader.discover.TargetLocator
import com.jhon.mihealthreader.dump.DbDumper
import com.jhon.mihealthreader.dump.ManifestWriter
import com.jhon.mihealthreader.extract.DbExtractor
import com.jhon.mihealthreader.extract.SuSourceReader
import com.jhon.mihealthreader.root.LibsuRootShell
import com.jhon.mihealthreader.ui.AppNav
import com.jhon.mihealthreader.ui.ExtractionViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shell = LibsuRootShell()
        val exportsDir = File(filesDir, "exports")
        val repository = ExportRepository(exportsDir)
        val scanner = PackageScanner {
            packageManager.getInstalledPackages(0).map { it.packageName }
        }
        val extractionViewModel = ExtractionViewModel(
            rootShell = shell,
            targetFinder = TargetLocator(shell, scanner),
            extractor = DbExtractor(SuSourceReader()),
            dumper = DbDumper(),
            manifestWriter = ManifestWriter(),
            repository = repository,
            androidVersion = Build.VERSION.SDK_INT,
            appVersion = "0.1.0",
            exportsDir = exportsDir,
        )

        setContent {
            MaterialTheme {
                AppNav(extractionViewModel, repository)
            }
        }
    }
}
