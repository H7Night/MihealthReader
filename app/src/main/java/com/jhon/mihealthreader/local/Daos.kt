package com.jhon.mihealthreader.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StepSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(items: List<StepSampleEntity>): List<Long>

    @Query("SELECT * FROM step_sample")
    fun all(): List<StepSampleEntity>

    @Query("SELECT * FROM step_sample")
    fun allFlow(): Flow<List<StepSampleEntity>>

    @Query("SELECT COUNT(*) FROM step_sample")
    fun count(): Int
}

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSessions(items: List<SleepSessionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertStages(items: List<SleepStageEntity>): List<Long>

    @Query("SELECT * FROM sleep_session ORDER BY start DESC")
    fun sessions(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_session ORDER BY start DESC")
    fun sessionsFlow(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_stage ORDER BY start")
    fun stages(): List<SleepStageEntity>

    @Query("SELECT * FROM sleep_stage ORDER BY start")
    fun stagesFlow(): Flow<List<SleepStageEntity>>

    @Query("SELECT COUNT(*) FROM sleep_session")
    fun sessionCount(): Int
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(state: SyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE id = 1")
    fun get(): SyncStateEntity?
}

@Dao
interface MetricDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(items: List<MetricSampleEntity>): List<Long>

    @Query("SELECT * FROM metric_sample WHERE metricType = :type ORDER BY time DESC")
    fun byType(type: String): List<MetricSampleEntity>

    @Query("SELECT * FROM metric_sample WHERE metricType = :type ORDER BY time DESC")
    fun byTypeFlow(type: String): Flow<List<MetricSampleEntity>>

    @Query("SELECT COUNT(*) FROM metric_sample WHERE metricType = :type")
    fun countByType(type: String): Int
}

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(items: List<WorkoutEntity>): List<Long>

    @Query("SELECT * FROM workout ORDER BY start DESC")
    fun all(): List<WorkoutEntity>

    @Query("SELECT * FROM workout ORDER BY start DESC")
    fun allFlow(): Flow<List<WorkoutEntity>>

    @Query("SELECT sourceId || '|' || start FROM workout")
    fun allKeys(): List<String>

    @Query("SELECT COUNT(*) FROM workout")
    fun count(): Int
}

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(items: List<BodyMeasurementEntity>): List<Long>

    @Query("SELECT * FROM body_measurement ORDER BY time DESC")
    fun all(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurement ORDER BY time DESC")
    fun allFlow(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT sourceId || '|' || time FROM body_measurement")
    fun allKeys(): List<String>

    @Query("SELECT COUNT(*) FROM body_measurement")
    fun count(): Int
}
