package com.jhon.mihealthreader.parse

import java.io.File

data class SleepStage(
    val start: Long,
    val end: Long,
    val state: Int,
    val stateName: String,
)

data class SleepSession(
    val start: Long,
    val end: Long,
    val durationMinutes: Int,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val avgHr: Int?,
    val minHr: Int?,
    val maxHr: Int?,
    val stages: List<SleepStage>,
)

data class StepSample(
    val sourceId: String,
    val time: Long,
    val zoneOffsetSec: Int,
    val steps: Int,
    val distanceMeters: Int,
    val calories: Int,
)

data class DailyActivity(
    val date: String,
    val steps: Int,
    val distanceMeters: Int,
    val calories: Int,
)

data class ParsedData(
    val sleep: List<SleepSession>,
    val samples: List<StepSample>,
    val activity: List<DailyActivity>,
    val skipped: Int,
    val outputFile: File,
)
