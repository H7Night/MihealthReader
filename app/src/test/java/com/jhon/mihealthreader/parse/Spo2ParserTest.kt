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
class Spo2ParserTest {

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
    fun `parses spo2 samples`() {
        val db = openDb(
            "spo2.db",
            listOf(
                "CREATE TABLE spo2_record(value TEXT, key TEXT, sid TEXT, isDeleted INTEGER, time INTEGER)",
                "INSERT INTO spo2_record VALUES ('{\"spo2\":98}', 'spo2','dev',0,100)",
                "INSERT INTO spo2_record VALUES ('{\"spo2\":95}', 'single_spo2','dev',0,200)",
                "INSERT INTO spo2_record VALUES ('{\"spo2\":90}', 'spo2','dev',1,300)",
            ),
        )
        val out = Spo2Parser.parse(db)
        db.close()

        assertEquals(2, out.size)
        assertEquals(MetricTypes.SPO2, out[0].metricType)
        assertEquals(98.0, out[0].value, 0.0)
    }

    @Test
    fun `missing table yields empty`() {
        val db = openDb("empty.db", listOf("CREATE TABLE other(x INTEGER)"))
        assertEquals(0, Spo2Parser.parse(db).size)
        db.close()
    }
}
