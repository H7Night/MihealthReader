package com.jhon.mihealthreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.discover.TargetFinder
import com.jhon.mihealthreader.dump.Dumper
import com.jhon.mihealthreader.dump.ManifestData
import com.jhon.mihealthreader.dump.ManifestWriter
import com.jhon.mihealthreader.extract.Extractor
import com.jhon.mihealthreader.root.RootShell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

class ExtractionViewModel(
    private val rootShell: RootShell,
    private val targetFinder: TargetFinder,
    private val extractor: Extractor,
    private val dumper: Dumper,
    private val manifestWriter: ManifestWriter,
    @Suppress("unused") private val repository: ExportRepository,
    private val androidVersion: Int,
    private val appVersion: String,
    private val exportsDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    private var running = false

    fun start() {
        if (running) return
        running = true
        viewModelScope.launch(io) {
            try {
                _state.value = ExtractionState.Running("检测 root 权限")
                if (!rootShell.isRootGranted()) {
                    _state.value = ExtractionState.Error("root 权限被拒绝，请授予后重试")
                    return@launch
                }

                _state.value = ExtractionState.Running("定位小米运动健康数据库")
                val target = targetFinder.findCandidates().firstOrNull()
                if (target == null) {
                    _state.value = ExtractionState.Error("未找到目标应用数据库（可手动指定包名）")
                    return@launch
                }

                _state.value = ExtractionState.Running("复制原始数据库快照")
                val snapshot = extractor.extract(target, exportsDir)

                _state.value = ExtractionState.Running("导出 JSON")
                val dbFiles = snapshot.files.filter { it.isFile && it.name.endsWith(".db") }
                val results = dbFiles.map { db ->
                    dumper.dump(db, File(snapshot.root, "json/${db.name}"))
                }

                manifestWriter.write(
                    ManifestData(
                        exportedAt = Instant.now().toString(),
                        sourcePackage = snapshot.sourcePackage,
                        androidVersion = androidVersion,
                        appVersion = appVersion,
                        databases = results,
                    ),
                    File(snapshot.root, "manifest.json"),
                )

                _state.value = ExtractionState.Success(snapshot.root, results.sumOf { it.tables.size })
            } catch (e: Exception) {
                _state.value = ExtractionState.Error(e.message ?: e.toString())
            } finally {
                running = false
            }
        }
    }
}
