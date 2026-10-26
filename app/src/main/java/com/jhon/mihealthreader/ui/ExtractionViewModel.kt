package com.jhon.mihealthreader.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhon.mihealthreader.BuildConfig
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.data.health.HealthConnectManager
import com.jhon.mihealthreader.di.IoDispatcher
import com.jhon.mihealthreader.discover.TargetFinder
import com.jhon.mihealthreader.local.HealthConnectPreferences
import com.jhon.mihealthreader.dump.Dumper
import com.jhon.mihealthreader.dump.ManifestData
import com.jhon.mihealthreader.dump.ManifestWriter
import com.jhon.mihealthreader.extract.Extractor
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.local.LocalSummary
import com.jhon.mihealthreader.parse.Normalizer
import com.jhon.mihealthreader.root.PrivilegedShell
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

@HiltViewModel
class ExtractionViewModel @Inject constructor(
    private val rootShell: PrivilegedShell,
    private val targetFinder: TargetFinder,
    private val extractor: Extractor,
    private val dumper: Dumper,
    private val manifestWriter: ManifestWriter,
    private val localStore: LocalStore,
    val parsedRepository: ParsedRepository,
    private val healthConnectManager: HealthConnectManager,
    private val normalizer: Normalizer,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val androidVersion = Build.VERSION.SDK_INT
    private val appVersion = BuildConfig.VERSION_NAME
    val exportsDir: File = File(context.filesDir, "exports")

    private val healthConnectPreferences = HealthConnectPreferences(context)

    private val _state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    private val _rootGranted = MutableStateFlow<Boolean?>(null)
    val rootGranted: StateFlow<Boolean?> = _rootGranted.asStateFlow()

    private val _summary = MutableStateFlow(LocalSummary(null, null, 0, 0))
    val summary: StateFlow<LocalSummary> = _summary.asStateFlow()

    private val _dataLoaded = MutableStateFlow(false)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    val healthConnectAvailable: Boolean = healthConnectManager.isAvailable()
    val healthConnectPermissions: Set<String> get() = healthConnectManager.permissions

    private val _healthConnectEnabled = MutableStateFlow(healthConnectPreferences.enabled())
    val healthConnectEnabled: StateFlow<Boolean> = _healthConnectEnabled.asStateFlow()

    private val _healthConnectMessage = MutableStateFlow<String?>(null)
    val healthConnectMessage: StateFlow<String?> = _healthConnectMessage.asStateFlow()

    private var running = false

    init {
        viewModelScope.launch(io) {
            _rootGranted.value = rootShell.isGranted()
            _summary.value = localStore.summary()
            _dataLoaded.value = true
        }
    }

    fun start() {
        if (running) return
        running = true
        viewModelScope.launch(io) { runExtraction() }
    }

    /**
     * Foreground auto-refresh: run extraction on app entry unless one is already in progress,
     * root is known to be denied, or the last sync was less than [MIN_AUTO_INTERVAL_MS] ago.
     */
    fun onForeground() {
        if (running) return
        if (_rootGranted.value == false) return
        viewModelScope.launch(io) {
            if (running) return@launch
            val last = localStore.summary().lastExtractedAt
            if (last != null && System.currentTimeMillis() - last < MIN_AUTO_INTERVAL_MS) return@launch
            running = true
            runExtraction()
        }
    }

    private suspend fun runExtraction() {
        try {
            _state.value = ExtractionState.Running(context.getString(R.string.extract_step_check_root))
            if (!rootShell.isGranted()) {
                _state.value = ExtractionState.Error(context.getString(R.string.extract_error_root_denied))
                return
            }

            _state.value = ExtractionState.Running(context.getString(R.string.extract_step_locate))
            val target = targetFinder.findCandidates().firstOrNull()
            if (target == null) {
                _state.value = ExtractionState.Error(context.getString(R.string.extract_error_target_missing))
                return
            }

            _state.value = ExtractionState.Running(context.getString(R.string.extract_step_copy))
            val snapshot = extractor.extract(target, exportsDir)

            _state.value = ExtractionState.Running(context.getString(R.string.extract_step_export))
            val sidecarSuffixes = listOf("-wal", "-shm", "-journal")
            val dbFiles = snapshot.files.filter { file ->
                file.isFile && sidecarSuffixes.none { file.name.endsWith(it) }
            }
            val results = dbFiles.map { db ->
                val relative = db.relativeToOrNull(snapshot.directory)?.path ?: db.name
                dumper.dump(db, File(snapshot.root, "json/$relative"))
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

            _state.value = ExtractionState.Running(context.getString(R.string.extract_step_parse))
            val parsed = normalizer.normalize(snapshot.root)

            val saved = localStore.save(parsed, snapshot.sourcePackage, System.currentTimeMillis())
            _summary.value = localStore.summary()

            // Best-effort Health Connect write; only when the user opted in, and never
            // fails the extraction. sync() itself also no-ops without write permission.
            if (healthConnectPreferences.enabled()) {
                runCatching { healthConnectManager.sync() }
            }

            _state.value = ExtractionState.Success(
                snapshot = snapshot.root,
                tableCount = results.sumOf { it.tables.size },
                newRecords = saved.newRecords,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            _state.value = ExtractionState.Error(e.message ?: e.toString())
        } finally {
            running = false
        }
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        healthConnectPreferences.setEnabled(enabled)
        _healthConnectEnabled.value = enabled
        if (enabled) syncHealthConnect()
    }

    fun onHealthPermissionsResult(granted: Boolean) {
        setHealthConnectEnabled(granted)
    }

    fun syncHealthConnect() {
        viewModelScope.launch(io) {
            if (!healthConnectPreferences.enabled()) {
                _healthConnectMessage.value = context.getString(R.string.hc_disabled)
                return@launch
            }
            if (!healthConnectManager.hasAllPermissions()) {
                _healthConnectMessage.value = context.getString(R.string.hc_permissions)
                return@launch
            }
            _healthConnectMessage.value = context.getString(R.string.hc_syncing)
            val written = runCatching { healthConnectManager.sync() }.getOrDefault(0)
            _healthConnectMessage.value = context.getString(R.string.hc_written, written)
        }
    }

    private companion object {
        const val MIN_AUTO_INTERVAL_MS = 60_000L
    }
}

