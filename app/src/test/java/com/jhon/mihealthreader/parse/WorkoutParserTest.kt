package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WorkoutParserTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun openDb(name: String, sql: List<String>): SQLiteDatabase {
        val file = File(tmp.root, name)
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        sql.forEach { db.execSQL(it) }
        db.close()
        return SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Test
    fun `parses workouts`() {
        val db = openDb(
            "sport.db",
            listOf(
                "CREATE TABLE sport_report(value TEXT, key TEXT, sid TEXT, time INTEGER, isDeleted INTEGER)",
                "INSERT INTO sport_report VALUES ('{\"start_time\":100,\"end_time\":400,\"duration\":300,\"calories\":295,\"avg_hrm\":112,\"max_hrm\":152,\"min_hrm\":71}', 'indoor_running','dev',100,0)",
                "INSERT INTO sport_report VALUES ('{\"start_time\":500,\"end_time\":800,\"duration\":300}', 'strength_training','dev',500,1)",
            ),
        )
        val out = WorkoutParser.parse(db)
        db.close()

        assertEquals(1, out.size)
        val w = out.single()
        assertEquals(100L, w.start)
        assertEquals(400L, w.end)
        assertEquals(300, w.durationSec)
        assertEquals(295, w.calories)
        assertEquals(112, w.avgHr)
        assertEquals("indoor_running", w.type)
        assertEquals("室内跑步", w.typeName)
    }

    @Test
    fun `missing table yields empty`() {
        val db = openDb("empty.db", listOf("CREATE TABLE other(x INTEGER)"))
        assertEquals(0, WorkoutParser.parse(db).size)
        db.close()
    }
}
