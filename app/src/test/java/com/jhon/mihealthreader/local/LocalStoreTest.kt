package com.jhon.mihealthreader.local

import androidx.room.Room
import com.jhon.mihealthreader.parse.BodyMeasurement
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.ParsedData
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import com.jhon.mihealthreader.parse.StepSample
import com.jhon.mihealthreader.parse.Workout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LocalStoreTest {

    private lateinit var db: MiHealthDatabase
    private lateinit var store: LocalStore

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MiHealthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = LocalStore(db)
    }

    @After
    fun tearDown() = db.close()

    private fun sessions() = listOf(
        SleepSession(100, 200, 2, 1, 1, 60, 50, 70, listOf(SleepStage(100, 160, 3, "light")))
    )

    private fun samples() = listOf(
        StepSample("dev", 0, 0, 10, 5, 1),
        StepSample("dev", 60, 0, 5, 3, 1),
    )

    private fun parsed(
        sleep: List<SleepSession> = emptyList(),
        samples: List<StepSample> = emptyList(),
        metrics: List<MetricSample> = emptyList(),
        workouts: List<Workout> = emptyList(),
        body: List<BodyMeasurement> = emptyList(),
    ) = ParsedData(sleep, samples, emptyList(), 0, File("out.json"), metrics, workouts, body)

    @Test
    fun `save inserts and reports new`() {
        val result = store.save(parsed(sessions(), samples()), "com.mi.health", 1000)
        assertEquals(2, result.newSamples)
        assertEquals(1, result.newStages)
        assertEquals(3, result.newRecords)
        assertEquals(1, result.totalSessions)
    }

    @Test
    fun `save is idempotent`() {
        store.save(parsed(sessions(), samples()), "com.mi.health", 1000)
        val result = store.save(parsed(sessions(), samples()), "com.mi.health", 2000)
        assertEquals(0, result.newSamples)
        assertEquals(0, result.newStages)
        assertEquals(0, result.newRecords)
    }

    @Test
    fun `save inserts metrics workouts body and is idempotent`() {
        val data = parsed(
            metrics = listOf(MetricSample("heart_rate", "dev", 100, 72.0)),
            workouts = listOf(Workout("dev", 100, 400, 300, 295, 112, 152, 71, null, "indoor_running", "室内跑步")),
            body = listOf(BodyMeasurement("dev", 100, 60.0, null, null, null, null, null, null, null, null)),
        )
        val first = store.save(data, "com.mi.health", 1000)
        assertEquals(1, first.newMetrics)
        assertEquals(1, first.newWorkouts)
        assertEquals(1, first.newBody)

        val second = store.save(data, "com.mi.health", 2000)
        assertEquals(0, second.newMetrics)
        assertEquals(0, second.newWorkouts)
        assertEquals(0, second.newBody)

        val summary = store.summary()
        assertEquals(1, summary.heartRateSamples)
        assertEquals(1, summary.workouts)
        assertEquals(1, summary.bodyMeasurements)
    }

    @Test
    fun `sleep sessions merge stages`() {
        store.save(parsed(sessions(), samples()), "com.mi.health", 1000)
        val session = store.sleepSessions().single()
        assertEquals(1, session.stages.size)
        assertEquals("light", session.stages.single().stateName)
    }

    @Test
    fun `summary reflects counts`() {
        store.save(parsed(sessions(), samples()), "com.mi.health", 1000)
        val summary = store.summary()
        assertEquals(2, summary.stepSamples)
        assertEquals(1, summary.sleepSessions)
        assertEquals(1000L, summary.lastExtractedAt)
    }

    @Test
    fun `summary empty when no state`() {
        val summary = store.summary()
        assertNull(summary.lastExtractedAt)
        assertEquals(0, summary.stepSamples)
        assertEquals(0, summary.sleepSessions)
    }
}
