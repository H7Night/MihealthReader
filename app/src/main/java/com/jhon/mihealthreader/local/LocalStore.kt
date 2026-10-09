package com.jhon.mihealthreader.local

import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import com.jhon.mihealthreader.parse.StepSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class SaveResult(
    val newSamples: Int,
    val newStages: Int,
    val totalSessions: Int,
)

data class LocalSummary(
    val lastExtractedAt: Long?,
    val sourcePackage: String?,
    val stepSamples: Int,
    val sleepSessions: Int,
)

class LocalStore(private val db: MiHealthDatabase) {

    fun save(
        sleep: List<SleepSession>,
        samples: List<StepSample>,
        sourcePackage: String,
        at: Long,
    ): SaveResult {
        val sessionEntities = sleep.map {
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

        val stageEntities = sleep.flatMap { session ->
            session.stages.map {
                SleepStageEntity(session.start, it.start, it.end, it.state, it.stateName)
            }
        }
        val newStages = db.sleepDao().insertStages(stageEntities).count { it != -1L }

        val sampleEntities = samples.map {
            StepSampleEntity(it.sourceId, it.time, it.zoneOffsetSec, it.steps, it.distanceMeters, it.calories)
        }
        val newSamples = db.stepSampleDao().insertIgnore(sampleEntities).count { it != -1L }

        db.syncStateDao().put(
            SyncStateEntity(1, at, sourcePackage, newStages + newSamples)
        )

        return SaveResult(newSamples, newStages, sessionEntities.size)
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
}
