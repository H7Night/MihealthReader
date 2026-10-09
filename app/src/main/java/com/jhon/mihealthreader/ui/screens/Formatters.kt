package com.jhon.mihealthreader.ui.screens

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm")
private val DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd")

internal fun formatDateTime(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(DATE_TIME)

internal fun formatDate(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(DATE_ONLY)

internal fun formatDurationSeconds(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}

internal fun formatMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"
