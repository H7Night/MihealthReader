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
        db.execSQL("CREATE TABLE hr_record(value TEXT, key TEXT, sid TEXT, isDeleted INTEGER, time INTEGER)")
        db.execSQL("INSERT INTO hr_record VALUES ('{\"bpm\":72}','heart_rate','dev',0,100)")
        db.execSQL("CREATE TABLE sport_report(value TEXT, key TEXT, sid TEXT, time INTEGER, isDeleted INTEGER)")
        db.execSQL("INSERT INTO sport_report VALUES ('{\"start_time\":100,\"end_time\":400,\"duration\":300,\"calories\":295,\"avg_hrm\":112}','indoor_running','dev',100,0)")
        db.execSQL("CREATE TABLE weight_item_record(time INTEGER, sid TEXT, value TEXT, isDeleted INTEGER)")
        db.execSQL("INSERT INTO weight_item_record VALUES (100,'dev','{\"weight\":60.0}',0)")
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
        assertEquals(1, result.metricSamples.size)
        assertEquals(1, result.workouts.size)
        assertEquals(1, result.bodyMeasurements.size)
        assertTrue(result.outputFile.isFile)

        val records = JSONObject(result.outputFile.readText()).getJSONObject("records")
        assertEquals(1, records.getJSONArray("sleep").length())
        assertEquals(1, records.getJSONArray("heart_rate").length())
        assertEquals(1, records.getJSONArray("workouts").length())
        assertEquals(1, records.getJSONArray("body_measurements").length())
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
