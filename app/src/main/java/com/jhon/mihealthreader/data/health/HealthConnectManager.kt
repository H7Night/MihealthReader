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
open class HealthConnectManager @Inject constructor(
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

    /**
     * Best-effort write of every stored metric. Returns the number of records written.
     * No-op when Health Connect is absent or the write permissions are not all granted.
     */
    open suspend fun sync(): Int {
        val client = client() ?: return 0
        if (!hasAllPermissions()) return 0
        var written = 0

        // Build + insert each metric type in isolation: a bad record in one group must not
        // drop the others, and every failure is logged so it can be diagnosed.
        suspend fun insertGroup(name: String, build: () -> List<Record>) {
            val records = runCatching { build() }
                .onFailure { Log.w(TAG, "build $name failed", it) }
                .getOrDefault(emptyList())
            records.chunked(BATCH).forEach { batch ->
                val ok = runCatching { client.insertRecords(batch) }
                    .onFailure { Log.w(TAG, "insert $name failed", it) }
                    .isSuccess
                if (ok) written += batch.size
            }
        }

        insertGroup("steps") { stepsRecords() }
        insertGroup("heartRate") { heartRateRecords() }
        insertGroup("restingHeartRate") { restingHeartRateRecords() }
        insertGroup("spo2") { spo2Records() }
        insertGroup("sleep") { sleepRecords() }
        insertGroup("workout") { workoutRecords() }
        return written
    }

    // LocalStore stores every timestamp as epoch SECONDS (see ParsedRepository/ChartData). All
    // Health Connect records must therefore be built with Instant.ofEpochSecond.
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private fun offset(seconds: Long) = zone.rules.getOffset(Instant.ofEpochSecond(seconds))

    private fun stepsRecords(): List<Record> =
        stepsPerLocalDay(localStore.stepSamples()).map { day ->
            StepsRecord(
                startTime = Instant.ofEpochSecond(day.start),
                startZoneOffset = offset(day.start),
                endTime = Instant.ofEpochSecond(day.end + 1),
                endZoneOffset = offset(day.end),
                count = day.count.toLong(),
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:steps:${day.day}"),
            )
        }

    private fun heartRateRecords(): List<Record> {
        val samples = localStore.metricSamples(MetricTypes.HEART_RATE)
        if (samples.isEmpty()) return emptyList()
        return samples.groupBy { dayKey(it.time) }.map { (day, daySamples) ->
            val start = daySamples.minOf { it.time }
            val end = daySamples.maxOf { it.time }
            HeartRateRecord(
                startTime = Instant.ofEpochSecond(start),
                startZoneOffset = offset(start),
                endTime = Instant.ofEpochSecond(end + 1),
                endZoneOffset = offset(end),
                samples = daySamples.map {
                    HeartRateRecord.Sample(
                        time = Instant.ofEpochSecond(it.time),
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
                    time = Instant.ofEpochSecond(sample.time),
                    zoneOffset = offset(sample.time),
                    beatsPerMinute = sample.value.toLong(),
                    metadata = Metadata.manualEntry(clientRecordId = "mihealth:resting:$day"),
                )
            }

    private fun spo2Records(): List<Record> =
        localStore.metricSamples(MetricTypes.SPO2).map {
            OxygenSaturationRecord(
                time = Instant.ofEpochSecond(it.time),
                zoneOffset = offset(it.time),
                percentage = Percentage(it.value),
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:spo2:${it.time}"),
            )
        }

    private fun sleepRecords(): List<Record> =
        localStore.sleepSessions().filter { it.end > it.start }.map { session ->
            val stages = validSleepStages(session.stages).map { span ->
                SleepSessionRecord.Stage(
                    startTime = Instant.ofEpochSecond(span.start),
                    endTime = Instant.ofEpochSecond(span.end),
                    stage = sleepStageType(span.state),
                )
            }
            SleepSessionRecord(
                startTime = Instant.ofEpochSecond(session.start),
                startZoneOffset = offset(session.start),
                endTime = Instant.ofEpochSecond(session.end),
                endZoneOffset = offset(session.end),
                title = null,
                notes = null,
                stages = stages,
                metadata = Metadata.manualEntry(clientRecordId = "mihealth:sleep:${session.start}"),
            )
        }

    private fun workoutRecords(): List<Record> =
        localStore.workouts().filter { it.end > it.start }.map { workout ->
            val start = Instant.ofEpochSecond(workout.start)
            val end = Instant.ofEpochSecond(workout.end)
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

    private fun dayKey(seconds: Long): String =
        Instant.ofEpochSecond(seconds).atZone(zone).toLocalDate().toString()

    private companion object {
        const val TAG = "HealthConnectManager"
        const val BATCH = 200
    }
}
