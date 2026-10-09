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
class ActivityParserTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `dedups same minute keeping larger steps`() {
        val out = ActivityParser.aggregate(
            listOf(
                StepSample("dev", 0, 0, 10, 5, 1),
                StepSample("dev", 0, 0, 30, 20, 2),
                StepSample("dev", 60, 0, 5, 3, 1),
            )
        )
        assertEquals(1, out.size)
        assertEquals(35, out.single().steps)
        assertEquals(23, out.single().distanceMeters)
        assertEquals(3, out.single().calories)
    }

    @Test
    fun `separates local days by zone offset`() {
        val time = 23L * 3600 + 30 * 60
        assertEquals("1970-01-02", ActivityParser.localDate(time, 28800))
        assertEquals("1970-01-01", ActivityParser.localDate(time, 0))
    }

    @Test
    fun `parseSamples reads source id`() {
        val file = File(tmp.root, "fitness_data")
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL("CREATE TABLE step_record(value TEXT, zoneOffsetInSec INTEGER, isDeleted INTEGER, sid TEXT)")
        db.execSQL("INSERT INTO step_record VALUES ('{\"time\":0,\"steps\":10,\"distance\":5,\"calories\":1}',0,0,'devA')")
        db.execSQL("INSERT INTO step_record VALUES ('{\"time\":60,\"steps\":20,\"distance\":9,\"calories\":2}',0,1,'devA')")
        db.close()

        val reopened = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val samples = ActivityParser.parseSamples(reopened)
        reopened.close()

        assertEquals(1, samples.size)
        assertEquals("devA", samples.single().sourceId)
        assertEquals(10, samples.single().steps)
    }
}
