package com.jhon.mihealthreader.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Guards the v1 -> v2 migration and, more importantly, the removal of
 * [RoomDatabase.Builder.fallbackToDestructiveMigration]. A future schema bump
 * with no matching migration must fail loudly rather than wipe the user's data.
 */
@RunWith(RobolectricTestRunner::class)
class Migration1To2Test {

    private val context: Context = RuntimeEnvironment.getApplication()
    private var db: MiHealthDatabase? = null

    @After
    fun tearDown() {
        db?.close()
    }

    private fun freshDbFile(): File =
        File(context.cacheDir, "migration-test.db").also { it.delete() }

    private fun createV1Database(path: File) {
        val legacy = SQLiteDatabase.openOrCreateDatabase(path, null)
        legacy.execSQL(V1_STEP_SAMPLE)
        legacy.execSQL(V1_SLEEP_SESSION)
        legacy.execSQL(V1_SLEEP_STAGE)
        legacy.execSQL(V1_SYNC_STATE)
        legacy.execSQL(
            "INSERT INTO step_sample (sourceId, time, zoneOffsetSec, steps, distanceMeters, calories) " +
                "VALUES ('dev', 100, 0, 1234, 500, 42)"
        )
        legacy.execSQL(
            "INSERT INTO sleep_session (start, end, durationMinutes, deepMinutes, lightMinutes, avgHr, minHr, maxHr) " +
                "VALUES (100, 200, 60, 20, 40, 55, 48, 70)"
        )
        legacy.execSQL(
            "INSERT INTO sync_state (id, lastExtractedAt, sourcePackage, lastNewCount) " +
                "VALUES (1, 999, 'com.mi.health', 7)"
        )
        legacy.version = 1
        legacy.close()
    }

    private fun openV2(path: File, withMigrations: Boolean = true): MiHealthDatabase {
        val builder = Room.databaseBuilder(context, MiHealthDatabase::class.java, path.absolutePath)
            .allowMainThreadQueries()
        if (withMigrations) builder.addMigrations(*ALL_MIGRATIONS)
        return builder.build()
    }

    @Test
    fun `v1 data survives migration and new tables are usable`() {
        val path = freshDbFile()
        createV1Database(path)

        db = openV2(path)

        // Existing v1 rows are preserved.
        val step = db!!.stepSampleDao().all().single()
        assertEquals(1234, step.steps)
        assertEquals(1, db!!.sleepDao().sessionCount())
        assertEquals(999L, db!!.syncStateDao().get()!!.lastExtractedAt)

        // New v2 tables exist and accept writes.
        assertEquals(0, db!!.workoutDao().count())
        db!!.workoutDao().upsert(
            listOf(WorkoutEntity("dev", 100, 200, 100, 10, 120, 150, 90, null, "run", "跑步"))
        )
        assertEquals(1, db!!.workoutDao().count())

        assertEquals(0, db!!.metricDao().countByType("heart_rate"))
        db!!.metricDao().insertIgnore(listOf(MetricSampleEntity("heart_rate", "dev", 100, 72.0)))
        assertEquals(1, db!!.metricDao().countByType("heart_rate"))

        db!!.bodyMeasurementDao().upsert(
            listOf(BodyMeasurementEntity("dev", 100, 60.0, null, null, null, null, null, null, null, null))
        )
        assertEquals(1, db!!.bodyMeasurementDao().count())
    }

    @Test
    fun `opening a v1 database without a migration fails loudly instead of wiping`() {
        val path = freshDbFile()
        createV1Database(path)

        val unmigrated = openV2(path, withMigrations = false)
        assertThrows(IllegalStateException::class.java) {
            unmigrated.openHelper.writableDatabase
        }
        unmigrated.close()
    }

    companion object {
        private const val V1_STEP_SAMPLE =
            "CREATE TABLE IF NOT EXISTS `step_sample` (`sourceId` TEXT NOT NULL, `time` INTEGER NOT NULL, `zoneOffsetSec` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `distanceMeters` INTEGER NOT NULL, `calories` INTEGER NOT NULL, PRIMARY KEY(`sourceId`, `time`))"
        private const val V1_SLEEP_SESSION =
            "CREATE TABLE IF NOT EXISTS `sleep_session` (`start` INTEGER NOT NULL, `end` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `deepMinutes` INTEGER NOT NULL, `lightMinutes` INTEGER NOT NULL, `avgHr` INTEGER, `minHr` INTEGER, `maxHr` INTEGER, PRIMARY KEY(`start`))"
        private const val V1_SLEEP_STAGE =
            "CREATE TABLE IF NOT EXISTS `sleep_stage` (`sessionStart` INTEGER NOT NULL, `start` INTEGER NOT NULL, `end` INTEGER NOT NULL, `state` INTEGER NOT NULL, `stateName` TEXT NOT NULL, PRIMARY KEY(`sessionStart`, `start`))"
        private const val V1_SYNC_STATE =
            "CREATE TABLE IF NOT EXISTS `sync_state` (`id` INTEGER NOT NULL, `lastExtractedAt` INTEGER NOT NULL, `sourcePackage` TEXT NOT NULL, `lastNewCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    }
}
