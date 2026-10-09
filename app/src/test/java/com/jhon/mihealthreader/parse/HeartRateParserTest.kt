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
class HeartRateParserTest {

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
    fun `parses heart rate and resting, skipping deleted and bad rows`() {
        val db = openDb(
            "hr.db",
            listOf(
                "CREATE TABLE hr_record(value TEXT, key TEXT, sid TEXT, isDeleted INTEGER, time INTEGER)",
                "INSERT INTO hr_record VALUES ('{\"bpm\":72}', 'heart_rate','dev',0,100)",
                "INSERT INTO hr_record VALUES ('{\"bpm\":56}', 'resting_heart_rate','dev',0,200)",
                "INSERT INTO hr_record VALUES ('{\"bpm\":80}', 'heart_rate','dev',1,300)",
                "INSERT INTO hr_record VALUES ('bad', 'heart_rate','dev',0,400)",
                "INSERT INTO hr_record VALUES ('{\"bpm\":90}', 'min_heart_rate','dev',0,500)",
            ),
        )
        val out = HeartRateParser.parse(db)
        db.close()

        assertEquals(2, out.size)
        assertEquals(MetricTypes.HEART_RATE, out[0].metricType)
        assertEquals(72.0, out[0].value, 0.0)
        assertEquals(MetricTypes.RESTING_HEART_RATE, out[1].metricType)
        assertEquals(56.0, out[1].value, 0.0)
    }

    @Test
    fun `missing table yields empty`() {
        val db = openDb("empty.db", listOf("CREATE TABLE other(x INTEGER)"))
        assertEquals(0, HeartRateParser.parse(db).size)
        db.close()
    }
}
