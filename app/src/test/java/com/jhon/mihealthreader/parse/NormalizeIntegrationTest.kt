package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class NormalizeIntegrationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun fixtureSnapshot(): File {
        val dir = File(tmp.root, "raw/com.mi.health/databases/123/cn").apply { mkdirs() }
        val dbFile = File(dir, "fitness_data")
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("CREATE TABLE sleep_segment(value TEXT)")
        db.execSQL(
            "INSERT INTO sleep_segment VALUES ('{\"bedtime\":1000,\"device_wake_up_time\":1060,\"duration\":1,\"sleep_deep_duration\":1,\"sleep_light_duration\":0,\"items\":[{\"start_time\":1000,\"end_time\":1060,\"state\":3}]}')"
        )
        db.execSQL("CREATE TABLE step_record(value TEXT, zoneOffsetInSec INTEGER, isDeleted INTEGER)")
        db.execSQL("INSERT INTO step_record VALUES ('{\"time\":0,\"steps\":10,\"distance\":5,\"calories\":1}',0,0)")
        db.execSQL("INSERT INTO step_record VALUES ('{\"time\":0,\"steps\":30,\"distance\":20,\"calories\":2}',0,0)")
        db.execSQL("INSERT INTO step_record VALUES ('{\"time\":999,\"steps\":50,\"distance\":50,\"calories\":5}',0,1)")
        db.close()
        return tmp.root
    }

    @Test
    fun `normalizes fixture snapshot`() {
        val root = fixtureSnapshot()
        val result = Normalizer().normalize(root, generatedAt = "2026-01-01T00:00:00Z")

        assertEquals(1, result.sleep.size)
        assertEquals(1, result.activity.size)
        assertEquals(2, result.samples.size)
        assertTrue(result.outputFile.isFile)

        val records = JSONObject(result.outputFile.readText()).getJSONObject("records")
        assertEquals(1, records.getJSONArray("sleep").length())
        // deleted row ignored; same-minute dedup keeps larger (30)
        assertEquals(30, records.getJSONArray("daily_activity").getJSONObject(0).getInt("steps"))
    }

    @Test
    fun `removes generated shm after normalizing`() {
        val root = fixtureSnapshot()
        val fitnessData = File(root, "raw/com.mi.health/databases/123/cn/fitness_data")
        val shm = File(fitnessData.path + "-shm").apply { writeText("stale") }

        Normalizer().normalize(root, generatedAt = "2026-01-01T00:00:00Z")

        assertTrue(!shm.exists())
    }

    @Test
    fun `writes empty envelope when fitness_data missing`() {
        val root = tmp.root
        val result = Normalizer().normalize(root, generatedAt = "2026-01-01T00:00:00Z")
        assertEquals(0, result.sleep.size)
        assertEquals(0, result.activity.size)
        assertTrue(result.outputFile.isFile)
    }
}
