package com.jhon.mihealthreader.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_sample", primaryKeys = ["sourceId", "time"])
data class StepSampleEntity(
    val sourceId: String,
    val time: Long,
    val zoneOffsetSec: Int,
    val steps: Int,
    val distanceMeters: Int,
    val calories: Int,
)

@Entity(tableName = "sleep_session", primaryKeys = ["start"])
data class SleepSessionEntity(
    val start: Long,
    val end: Long,
    val durationMinutes: Int,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val avgHr: Int?,
    val minHr: Int?,
    val maxHr: Int?,
)

@Entity(tableName = "sleep_stage", primaryKeys = ["sessionStart", "start"])
data class SleepStageEntity(
    val sessionStart: Long,
    val start: Long,
    val end: Long,
    val state: Int,
    val stateName: String,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastExtractedAt: Long,
    val sourcePackage: String,
    val lastNewCount: Int,
)

@Entity(tableName = "metric_sample", primaryKeys = ["metricType", "sourceId", "time"])
data class MetricSampleEntity(
    val metricType: String,
    val sourceId: String,
    val time: Long,
    val value: Double,
)

@Entity(tableName = "workout", primaryKeys = ["sourceId", "start"])
data class WorkoutEntity(
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

@Entity(tableName = "body_measurement", primaryKeys = ["sourceId", "time"])
data class BodyMeasurementEntity(
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
