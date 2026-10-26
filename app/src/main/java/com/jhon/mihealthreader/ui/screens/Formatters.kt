package com.jhon.mihealthreader.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jhon.mihealthreader.R
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

private val DATE_TIME = DateFormat.getDateTimeInstance(
    DateFormat.SHORT,
    DateFormat.SHORT,
    Locale.getDefault(),
)
private val DATE_ONLY = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())

internal fun formatDateTime(epochSeconds: Long): String =
    DATE_TIME.format(Date.from(Instant.ofEpochSecond(epochSeconds)))

internal fun formatDate(epochSeconds: Long): String =
    DATE_ONLY.format(Date.from(Instant.ofEpochSecond(epochSeconds)))

internal fun formatDurationSeconds(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}

internal fun formatMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

@Composable
internal fun formatMinutesText(minutes: Int): String = when {
    minutes >= 60 -> stringResource(R.string.duration_hm, minutes / 60, minutes % 60)
    else -> stringResource(R.string.duration_m, minutes)
}

@Composable
internal fun formatMinutesCompact(minutes: Int): String = when {
    minutes >= 60 -> stringResource(R.string.duration_hm_compact, minutes / 60, minutes % 60)
    else -> stringResource(R.string.duration_m, minutes)
}

@Composable
internal fun formatDurationText(seconds: Int): String = formatMinutesText(seconds / 60)

