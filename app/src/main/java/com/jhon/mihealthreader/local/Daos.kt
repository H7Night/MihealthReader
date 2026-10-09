package com.jhon.mihealthreader.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StepSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(items: List<StepSampleEntity>): List<Long>

    @Query("SELECT * FROM step_sample")
    fun all(): List<StepSampleEntity>

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

    @Query("SELECT * FROM sleep_stage ORDER BY start")
    fun stages(): List<SleepStageEntity>

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
