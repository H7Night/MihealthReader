package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.parse.ActivityParser
import com.jhon.mihealthreader.parse.BodyMeasurement
import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.MetricTypes
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParsedRepository(private val localStore: LocalStore) {

    fun dailyActivity(): Flow<List<DailyActivity>> =
        localStore.stepSamplesFlow().map { samples ->
            ActivityParser.aggregate(samples).sortedByDescending { it.date }
        }

    fun sleepSessions(): Flow<List<SleepSession>> = localStore.sleepSessionsFlow()

    fun heartRate(): Flow<List<MetricSample>> = localStore.metricSamplesFlow(MetricTypes.HEART_RATE)

    fun restingHeartRate(): Flow<List<MetricSample>> =
        localStore.metricSamplesFlow(MetricTypes.RESTING_HEART_RATE)

    fun spo2(): Flow<List<MetricSample>> = localStore.metricSamplesFlow(MetricTypes.SPO2)

    fun workouts(): Flow<List<Workout>> = localStore.workoutsFlow()

    fun bodyMeasurements(): Flow<List<BodyMeasurement>> = localStore.bodyMeasurementsFlow()
}
