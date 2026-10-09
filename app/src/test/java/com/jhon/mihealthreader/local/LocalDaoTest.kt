package com.jhon.mihealthreader.local

import androidx.room.Room
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalDaoTest {

    private lateinit var db: MiHealthDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MiHealthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `step sample ignore is idempotent`() {
        val batch = listOf(StepSampleEntity("dev", 100, 0, 10, 5, 1))
        assertEquals(1, db.stepSampleDao().insertIgnore(batch).count { it != -1L })
        assertEquals(0, db.stepSampleDao().insertIgnore(batch).count { it != -1L })
        assertEquals(1, db.stepSampleDao().count())
    }

    @Test
    fun `sleep session replace updates same start`() {
        db.sleepDao().upsertSessions(listOf(SleepSessionEntity(100, 200, 1, 1, 0, null, null, null)))
        db.sleepDao().upsertSessions(listOf(SleepSessionEntity(100, 300, 3, 2, 1, 50, 40, 60)))
        val session = db.sleepDao().sessions().single()
        assertEquals(300L, session.end)
        assertEquals(2, session.deepMinutes)
    }

    @Test
    fun `sleep stage ignore dedups`() {
        val stage = SleepStageEntity(100, 100, 160, 3, "light")
        assertEquals(1, db.sleepDao().insertStages(listOf(stage)).count { it != -1L })
        assertEquals(0, db.sleepDao().insertStages(listOf(stage)).count { it != -1L })
    }

    @Test
    fun `sync state overwrites`() {
        db.syncStateDao().put(SyncStateEntity(1, 111, "p", 5))
        db.syncStateDao().put(SyncStateEntity(1, 222, "p", 0))
        assertEquals(222L, db.syncStateDao().get()!!.lastExtractedAt)
        assertEquals(0, db.syncStateDao().get()!!.lastNewCount)
    }

    @Test
    fun `metric sample ignore is idempotent and filters by type`() {
        val batch = listOf(MetricSampleEntity("heart_rate", "dev", 100, 72.0))
        assertEquals(1, db.metricDao().insertIgnore(batch).count { it != -1L })
        assertEquals(0, db.metricDao().insertIgnore(batch).count { it != -1L })
        db.metricDao().insertIgnore(listOf(MetricSampleEntity("spo2", "dev", 200, 98.0)))
        assertEquals(1, db.metricDao().countByType("heart_rate"))
        assertEquals(1, db.metricDao().byType("spo2").size)
    }

    @Test
    fun `workout replace updates same start`() {
        db.workoutDao().upsert(listOf(WorkoutEntity("dev", 100, 200, 100, 10, 120, 150, 90, null, "indoor_running", "室内跑步")))
        db.workoutDao().upsert(listOf(WorkoutEntity("dev", 100, 300, 200, 20, 130, 160, 95, 5000, "indoor_running", "室内跑步")))
        val workout = db.workoutDao().all().single()
        assertEquals(300L, workout.end)
        assertEquals(5000, workout.distanceMeters)
    }

    @Test
    fun `body measurement replace updates same time`() {
        db.bodyMeasurementDao().upsert(listOf(BodyMeasurementEntity("dev", 100, 60.0, null, null, null, null, null, null, null, null)))
        db.bodyMeasurementDao().upsert(listOf(BodyMeasurementEntity("dev", 100, 61.0, 22.0, null, null, null, null, null, null, null)))
        val body = db.bodyMeasurementDao().all().single()
        assertEquals(61.0, body.weightKg!!, 0.0)
        assertEquals(22.0, body.bmi!!, 0.0)
    }
}
