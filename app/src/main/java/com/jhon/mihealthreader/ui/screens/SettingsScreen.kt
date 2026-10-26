package com.jhon.mihealthreader.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.jhon.mihealthreader.BuildConfig
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.data.ExportManager
import com.jhon.mihealthreader.local.BackendType
import com.jhon.mihealthreader.local.LocalSummary
import com.jhon.mihealthreader.ui.ExtractionState
import com.jhon.mihealthreader.ui.component.GhCard
import com.jhon.mihealthreader.ui.component.GhPageTitle
import com.jhon.mihealthreader.ui.component.GhPillButton
import com.jhon.mihealthreader.ui.component.GhSectionHeader
import com.jhon.mihealthreader.ui.component.GhSegment
import com.jhon.mihealthreader.ui.component.GhSegmentedControl
import com.jhon.mihealthreader.ui.component.GhStatusPill
import com.jhon.mihealthreader.ui.component.GhSwitchRow
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    state: ExtractionState,
    rootGranted: Boolean?,
    summary: LocalSummary,
    backend: BackendType,
    shizukuGranted: Boolean,
    exportManager: ExportManager,
    onSelectBackend: (BackendType) -> Unit,
    onRequestShizuku: () -> Unit,
    onStart: () -> Unit,
    healthConnectAvailable: Boolean = false,
    healthConnectEnabled: Boolean = false,
    healthConnectPermissions: Set<String> = emptySet(),
    healthConnectMessage: String? = null,
    onToggleHealthConnect: (Boolean) -> Unit = {},
    onHealthPermissionsResult: (Boolean) -> Unit = {},
    onSyncHealthConnect: () -> Unit = {},
) {
    val context = LocalContext.current
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        onHealthPermissionsResult(granted.containsAll(healthConnectPermissions))
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val file = exportManager.latestNormalized()
            if (file == null) {
                exportMessage = context.getString(R.string.settings_export_empty)
            } else {
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                }.isSuccess
                exportMessage = context.getString(
                    if (ok) R.string.settings_export_done else R.string.settings_export_failed,
                )
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GhPageTitle(
            stringResource(R.string.title_settings),
            subtitle = stringResource(R.string.settings_subtitle),
        )

        GhSectionHeader(stringResource(R.string.settings_source))
        GhCard {
            GhSegmentedControl(
                options = listOf(
                    GhSegment(BackendType.ROOT, stringResource(R.string.settings_source_root)),
                    GhSegment(BackendType.SHIZUKU, stringResource(R.string.settings_source_shizuku)),
                ),
                selected = backend,
                onSelect = onSelectBackend,
                unselectedContainer = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            Spacer(Modifier.height(12.dp))
            GhStatusPill(
                stringResource(
                    R.string.settings_source_current,
                    stringResource(
                        if (backend == BackendType.ROOT) R.string.settings_source_root
                        else R.string.settings_source_shizuku,
                    ),
                    backendStatus(rootGranted),
                ),
            )
            if (backend == BackendType.SHIZUKU && !shizukuGranted) {
                Spacer(Modifier.height(12.dp))
                GhPillButton(stringResource(R.string.action_request_shizuku), onRequestShizuku)
                Text(
                    stringResource(R.string.settings_shizuku_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_source_restart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        GhSectionHeader(stringResource(R.string.settings_extraction))
        GhCard {
            Text(summaryText(summary), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            when (state) {
                ExtractionState.Idle -> GhPillButton(stringResource(R.string.action_extract), onStart)
                is ExtractionState.Running -> {
                    GhPillButton(stringResource(R.string.action_extracting), {}, enabled = false)
                    Text(state.step, style = MaterialTheme.typography.bodySmall)
                }
                is ExtractionState.Success -> {
                    Text(
                        if (state.newRecords > 0) {
                            stringResource(R.string.settings_extract_new, state.newRecords)
                        } else {
                            stringResource(R.string.settings_extract_none)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    GhPillButton(stringResource(R.string.action_extract_again), onStart)
                }
                is ExtractionState.Error -> {
                    Text(
                        stringResource(R.string.settings_error, state.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    GhPillButton(stringResource(R.string.action_retry), onStart)
                }
            }
        }

        GhSectionHeader(stringResource(R.string.settings_export))
        GhCard {
            Text(
                stringResource(R.string.settings_export_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            GhPillButton(
                text = stringResource(R.string.action_export),
                onClick = {
                    if (exportManager.latestNormalized() == null) {
                        exportMessage = context.getString(R.string.settings_export_empty)
                    } else {
                        exportLauncher.launch(exportManager.defaultExportName())
                    }
                },
            )
            exportMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (healthConnectAvailable) {
            GhSectionHeader(stringResource(R.string.settings_health_connect))
            GhSwitchRow(
                title = stringResource(R.string.settings_sync_hc),
                subtitle = null,
                checked = healthConnectEnabled,
                onCheckedChange = { checked ->
                    if (checked) healthPermissionLauncher.launch(healthConnectPermissions)
                    else onToggleHealthConnect(false)
                },
            )
            if (healthConnectEnabled) {
                GhPillButton(stringResource(R.string.action_sync), onSyncHealthConnect)
            }
            healthConnectMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            GhStatusPill(
                stringResource(R.string.settings_hc_unavailable),
                container = MaterialTheme.colorScheme.surfaceContainer,
            )
        }

        GhSectionHeader(stringResource(R.string.settings_about))
        GhCard {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun backendStatus(granted: Boolean?): String = when (granted) {
    true -> stringResource(R.string.settings_source_granted)
    false -> stringResource(R.string.settings_source_not_granted)
    null -> stringResource(R.string.settings_source_checking)
}

private val SUMMARY_FORMAT = DateFormat.getDateTimeInstance(
    DateFormat.MEDIUM,
    DateFormat.SHORT,
    Locale.getDefault(),
)

@Composable
internal fun summaryText(summary: LocalSummary): String {
    val last = summary.lastExtractedAt
    val whenText = if (last == null) {
        stringResource(R.string.settings_never_synced)
    } else {
        stringResource(
            R.string.settings_last_sync,
            SUMMARY_FORMAT.format(Date.from(Instant.ofEpochMilli(last))),
        )
    }
    return stringResource(R.string.settings_summary, whenText, summary.stepSamples, summary.sleepSessions)
}

