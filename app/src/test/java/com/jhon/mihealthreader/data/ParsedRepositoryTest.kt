package com.jhon.mihealthreader.data

import androidx.room.Room
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.local.MiHealthDatabase
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import com.jhon.mihealthreader.parse.StepSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ParsedRepositoryTest {

    private lateinit var db: MiHealthDatabase
    private lateinit var store: LocalStore
    private lateinit var repo: ParsedRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MiHealthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = LocalStore(db)
        repo = ParsedRepository(store)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `aggregates step samples into newest-first daily activity`() = runTest {
        store.save(
            sleep = emptyList(),
            samples = listOf(
                StepSample("dev", 0, 0, 10, 5, 1),
                StepSample("dev", 86400, 0, 20, 12, 2),
            ),
            sourcePackage = "com.mi.health",
            at = 1,
        )
        val activity = repo.dailyActivity().first()
        assertEquals(2, activity.size)
        assertEquals("1970-01-02", activity.first().date)
        assertEquals(20, activity.first().steps)
    }

    @Test
    fun `returns saved sleep sessions`() = runTest {
        store.save(
            sleep = listOf(SleepSession(100, 200, 2, 1, 1, 60, 50, 70, listOf(SleepStage(100, 160, 3, "light")))),
            samples = emptyList(),
            sourcePackage = "com.mi.health",
            at = 1,
        )
        val sleep = repo.sleepSessions().first().single()
        assertEquals(1, sleep.stages.size)
        assertEquals("light", sleep.stages.single().stateName)
    }

    @Test
    fun `empty store yields empty lists`() = runTest {
        assertEquals(0, repo.dailyActivity().first().size)
        assertEquals(0, repo.sleepSessions().first().size)
    }
}
