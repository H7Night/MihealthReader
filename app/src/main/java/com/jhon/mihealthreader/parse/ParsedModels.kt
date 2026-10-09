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
    val metricSamples: List<MetricSample> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val bodyMeasurements: List<BodyMeasurement> = emptyList(),
)

object MetricTypes {
    const val HEART_RATE = "heart_rate"
    const val RESTING_HEART_RATE = "resting_heart_rate"
    const val SPO2 = "spo2"
}

data class MetricSample(
    val metricType: String,
    val sourceId: String,
    val time: Long,
    val value: Double,
)

data class Workout(
    val sourceId: String,
    val start: Long,
    val end: Long,
    val durationSec: Int,
    val calories: Int,
    val avgHr: Int?,
    val maxHr: Int?,
    val minHr: Int?,
    val distanceMeters: Int?,
    val type: String,
    val typeName: String,
)

data class BodyMeasurement(
    val sourceId: String,
    val time: Long,
    val weightKg: Double?,
    val bmi: Double?,
    val bodyFatRate: Double?,
    val muscleKg: Double?,
    val waterPct: Double?,
    val boneKg: Double?,
    val bmrKcal: Double?,
    val visceralFat: Double?,
    val proteinRate: Double?,
)
