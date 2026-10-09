package com.jhon.mihealthreader.local

import com.jhon.mihealthreader.parse.BodyMeasurement
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.MetricTypes
import com.jhon.mihealthreader.parse.ParsedData
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import com.jhon.mihealthreader.parse.StepSample
import com.jhon.mihealthreader.parse.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class SaveResult(
    val newSamples: Int,
    val newStages: Int,
    val newMetrics: Int,
    val newWorkouts: Int,
    val newBody: Int,
    val totalSessions: Int,
) {
    val newRecords: Int get() = newSamples + newStages + newMetrics + newWorkouts + newBody
}

data class LocalSummary(
    val lastExtractedAt: Long?,
    val sourcePackage: String?,
    val stepSamples: Int,
    val sleepSessions: Int,
    val heartRateSamples: Int = 0,
    val spo2Samples: Int = 0,
    val workouts: Int = 0,
    val bodyMeasurements: Int = 0,
)

class LocalStore(private val db: MiHealthDatabase) {

    fun save(data: ParsedData, sourcePackage: String, at: Long): SaveResult {
        val sessionEntities = data.sleep.map {
            SleepSessionEntity(
                start = it.start,
                end = it.end,
                durationMinutes = it.durationMinutes,
                deepMinutes = it.deepMinutes,
                lightMinutes = it.lightMinutes,
                avgHr = it.avgHr,
                minHr = it.minHr,
                maxHr = it.maxHr,
            )
        }
        db.sleepDao().upsertSessions(sessionEntities)

        val stageEntities = data.sleep.flatMap { session ->
            session.stages.map {
                SleepStageEntity(session.start, it.start, it.end, it.state, it.stateName)
            }
        }
        val newStages = db.sleepDao().insertStages(stageEntities).count { it != -1L }

        val sampleEntities = data.samples.map {
            StepSampleEntity(it.sourceId, it.time, it.zoneOffsetSec, it.steps, it.distanceMeters, it.calories)
        }
        val newSamples = db.stepSampleDao().insertIgnore(sampleEntities).count { it != -1L }

        val metricEntities = data.metricSamples.map {
            MetricSampleEntity(it.metricType, it.sourceId, it.time, it.value)
        }
        val newMetrics = db.metricDao().insertIgnore(metricEntities).count { it != -1L }

        val existingWorkoutKeys = db.workoutDao().allKeys().toSet()
        val workoutEntities = data.workouts.map {
            WorkoutEntity(
                it.sourceId, it.start, it.end, it.durationSec, it.calories,
                it.avgHr, it.maxHr, it.minHr, it.distanceMeters, it.type, it.typeName,
            )
        }
        val newWorkouts = workoutEntities.count { "${it.sourceId}|${it.start}" !in existingWorkoutKeys }
        db.workoutDao().upsert(workoutEntities)

        val existingBodyKeys = db.bodyMeasurementDao().allKeys().toSet()
        val bodyEntities = data.bodyMeasurements.map {
            BodyMeasurementEntity(
                it.sourceId, it.time, it.weightKg, it.bmi, it.bodyFatRate, it.muscleKg,
                it.waterPct, it.boneKg, it.bmrKcal, it.visceralFat, it.proteinRate,
            )
        }
        val newBody = bodyEntities.count { "${it.sourceId}|${it.time}" !in existingBodyKeys }
        db.bodyMeasurementDao().upsert(bodyEntities)

        db.syncStateDao().put(
            SyncStateEntity(1, at, sourcePackage, newStages + newSamples + newMetrics + newWorkouts + newBody)
        )

        return SaveResult(newSamples, newStages, newMetrics, newWorkouts, newBody, sessionEntities.size)
    }

    fun stepSamples(): List<StepSample> = db.stepSampleDao().all().map {
        StepSample(it.sourceId, it.time, it.zoneOffsetSec, it.steps, it.distanceMeters, it.calories)
    }

    fun sleepSessions(): List<SleepSession> {
        val stagesBySession = db.sleepDao().stages().groupBy { it.sessionStart }
        return db.sleepDao().sessions().map { session ->
            SleepSession(
                start = session.start,
                end = session.end,
                durationMinutes = session.durationMinutes,
                deepMinutes = session.deepMinutes,
                lightMinutes = session.lightMinutes,
                avgHr = session.avgHr,
                minHr = session.minHr,
                maxHr = session.maxHr,
                stages = stagesBySession[session.start].orEmpty().map {
                    SleepStage(it.start, it.end, it.state, it.stateName)
                },
            )
        }
    }

    fun summary(): LocalSummary {
        val state = db.syncStateDao().get()
        return LocalSummary(
            lastExtractedAt = state?.lastExtractedAt,
            sourcePackage = state?.sourcePackage,
            stepSamples = db.stepSampleDao().count(),
            sleepSessions = db.sleepDao().sessionCount(),
            heartRateSamples = db.metricDao().countByType(MetricTypes.HEART_RATE),
            spo2Samples = db.metricDao().countByType(MetricTypes.SPO2),
            workouts = db.workoutDao().count(),
            bodyMeasurements = db.bodyMeasurementDao().count(),
        )
    }

    fun stepSamplesFlow(): Flow<List<StepSample>> =
        db.stepSampleDao().allFlow().map { entities ->
            entities.map { StepSample(it.sourceId, it.time, it.zoneOffsetSec, it.steps, it.distanceMeters, it.calories) }
        }

    fun sleepSessionsFlow(): Flow<List<SleepSession>> =
        combine(db.sleepDao().sessionsFlow(), db.sleepDao().stagesFlow()) { sessions, stages ->
            val stagesBySession = stages.groupBy { it.sessionStart }
            sessions.map { session ->
                SleepSession(
                    start = session.start,
                    end = session.end,
                    durationMinutes = session.durationMinutes,
                    deepMinutes = session.deepMinutes,
                    lightMinutes = session.lightMinutes,
                    avgHr = session.avgHr,
                    minHr = session.minHr,
                    maxHr = session.maxHr,
                    stages = stagesBySession[session.start].orEmpty().map {
                        SleepStage(it.start, it.end, it.state, it.stateName)
                    },
                )
            }
        }

    fun metricSamplesFlow(type: String): Flow<List<MetricSample>> =
        db.metricDao().byTypeFlow(type).map { entities ->
            entities.map { MetricSample(it.metricType, it.sourceId, it.time, it.value) }
        }

    fun workoutsFlow(): Flow<List<Workout>> =
        db.workoutDao().allFlow().map { entities ->
            entities.map {
                Workout(
                    it.sourceId, it.start, it.end, it.durationSec, it.calories,
                    it.avgHr, it.maxHr, it.minHr, it.distanceMeters, it.type, it.typeName,
                )
            }
        }

    fun bodyMeasurementsFlow(): Flow<List<BodyMeasurement>> =
        db.bodyMeasurementDao().allFlow().map { entities ->
            entities.map {
                BodyMeasurement(
                    it.sourceId, it.time, it.weightKg, it.bmi, it.bodyFatRate, it.muscleKg,
                    it.waterPct, it.boneKg, it.bmrKcal, it.visceralFat, it.proteinRate,
                )
            }
        }
}
