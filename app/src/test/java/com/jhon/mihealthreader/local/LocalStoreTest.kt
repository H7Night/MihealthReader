package com.jhon.mihealthreader.local

import androidx.room.Room
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import com.jhon.mihealthreader.parse.StepSample
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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

    @Test
    fun `save inserts and reports new`() {
        val result = store.save(sessions(), samples(), "com.mi.health", at = 1000)
        assertEquals(2, result.newSamples)
        assertEquals(1, result.newStages)
        assertEquals(1, result.totalSessions)
    }

    @Test
    fun `save is idempotent`() {
        store.save(sessions(), samples(), "com.mi.health", 1000)
        val result = store.save(sessions(), samples(), "com.mi.health", 2000)
        assertEquals(0, result.newSamples)
        assertEquals(0, result.newStages)
    }

    @Test
    fun `sleep sessions merge stages`() {
        store.save(sessions(), samples(), "com.mi.health", 1000)
        val session = store.sleepSessions().single()
        assertEquals(1, session.stages.size)
        assertEquals("light", session.stages.single().stateName)
    }

    @Test
    fun `summary reflects counts`() {
        store.save(sessions(), samples(), "com.mi.health", 1000)
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
