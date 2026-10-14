package com.jhon.mihealthreader.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Percentage
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.parse.MetricTypes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the already-parsed metrics in [LocalStore] to Health Connect. Write-only and
 * best-effort: never throws into callers, and is a no-op when Health Connect is absent.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localStore: LocalStore,
) {

    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(RestingHeartRateRecord::class),
        HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        runCatching {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }.getOrDefault(false)

    private fun client(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean {
        val client = client() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Best-effort write of every stored metric. Returns the number of records written. */
    suspend fun sync(): Int {
        val client = client() ?: return 0
        val records = buildList {
            addAll(stepsRecords())
            addAll(heartRateRecords())
            addAll(restingHeartRateRecords())
            addAll(spo2Records())
            addAll(sleepRecords())
            addAll(workoutRecords())
        }
        var written = 0
        records.chunked(BATCH).forEach { batch ->
            val ok = runCatching { client.insertRecords(batch) }
                .onFailure { Log.w(TAG, "Health Connect insert failed", it) }
                .isSuccess
            if (ok) written += batch.size
        }
        return written
    }

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private fun offset(millis: Long) = zone.rules.getOffset(Instant.ofEpochMilli(millis))

    private fun stepsRecords(): List<Record> {
        val samples = localStore.stepSamples()
        if (samples.isEmpty()) return emptyList()
        return samples.groupBy { dayKey(it.time) }.map { (day, daySamples) ->
            val start = daySamples.minOf { it.time }
            val end = daySamples.maxOf { it.time }
            StepsRecord(
                startTime = Instant.ofEpochMilli(start),
                startZoneOffset = offset(start),
                endTime = Instant.ofEpochMilli(end + 1),
                endZoneOffset = offset(end),
                count = daySamples.sumOf { it.steps }.toLong(),
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:steps:$day"),
            )
        }
    }

    private fun heartRateRecords(): List<Record> {
        val samples = localStore.metricSamples(MetricTypes.HEART_RATE)
        if (samples.isEmpty()) return emptyList()
        return samples.groupBy { dayKey(it.time) }.map { (day, daySamples) ->
            val start = daySamples.minOf { it.time }
            val end = daySamples.maxOf { it.time }
            HeartRateRecord(
                startTime = Instant.ofEpochMilli(start),
                startZoneOffset = offset(start),
                endTime = Instant.ofEpochMilli(end + 1),
                endZoneOffset = offset(end),
                samples = daySamples.map {
                    HeartRateRecord.Sample(
                        time = Instant.ofEpochMilli(it.time),
                        beatsPerMinute = it.value.toLong(),
                    )
                },
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:hr:$day"),
            )
        }
    }

    private fun restingHeartRateRecords(): List<Record> =
        localStore.metricSamples(MetricTypes.RESTING_HEART_RATE).groupBy { dayKey(it.time) }
            .map { (day, daySamples) ->
                val sample = daySamples.maxBy { it.time }
                RestingHeartRateRecord(
                    time = Instant.ofEpochMilli(sample.time),
                    zoneOffset = offset(sample.time),
                    beatsPerMinute = sample.value.toLong(),
                    metadata = Metadata.manualEntry(clientRecordId = "mihealth:resting:$day"),
                )
            }

    private fun spo2Records(): List<Record> =
        localStore.metricSamples(MetricTypes.SPO2).map {
            OxygenSaturationRecord(
                time = Instant.ofEpochMilli(it.time),
                zoneOffset = offset(it.time),
                percentage = Percentage(it.value),
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:spo2:${it.time}"),
            )
        }

    private fun sleepRecords(): List<Record> =
        localStore.sleepSessions().map { session ->
            val stages = session.stages.map { stage ->
                SleepSessionRecord.Stage(
                    startTime = Instant.ofEpochMilli(stage.start),
                    endTime = Instant.ofEpochMilli(stage.end),
                    stage = sleepStageType(stage.state),
                )
            }
            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(session.start),
                startZoneOffset = offset(session.start),
                endTime = Instant.ofEpochMilli(session.end),
                endZoneOffset = offset(session.end),
                title = null,
                notes = null,
                stages = stages,
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:sleep:${session.start}"),
            )
        }

    private fun workoutRecords(): List<Record> =
        localStore.workouts().map { workout ->
            val start = Instant.ofEpochMilli(workout.start)
            val end = Instant.ofEpochMilli(workout.end)
            ExerciseSessionRecord(
                start,
                offset(workout.start),
                end,
                offset(workout.end),
                Metadata.manualEntry(clientRecordId = "mihealth:workout:${workout.sourceId}:${workout.start}"),
                workoutExerciseType(workout.type),
                workout.typeName.takeIf { it.isNotBlank() },
            )
        }

    private fun dayKey(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()

    private companion object {
        const val TAG = "HealthConnectManager"
        const val BATCH = 200
    }
}
