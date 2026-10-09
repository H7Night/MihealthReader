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
class BodyCompositionParserTest {

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
    fun `parses independent columns`() {
        val db = openDb(
            "body.db",
            listOf(
                "CREATE TABLE weight_item_record(time INTEGER, sid TEXT, weight REAL, bmi REAL, bodyFatRate REAL, moistureRate REAL, boneMass REAL, basalMetabolism REAL, muscleMass REAL, proteinRate REAL, visceralFat REAL, isDeleted INTEGER, value TEXT)",
                "INSERT INTO weight_item_record VALUES (100,'dev',60.5,22.1,18.2,55.0,2.8,1400,45.0,17.0,5.0,0,'{}')",
            ),
        )
        val out = BodyCompositionParser.parse(db)
        db.close()

        val b = out.single()
        assertEquals(60.5, b.weightKg!!, 0.0)
        assertEquals(22.1, b.bmi!!, 0.0)
        assertEquals(18.2, b.bodyFatRate!!, 0.0)
        assertEquals(45.0, b.muscleKg!!, 0.0)
        assertEquals(55.0, b.waterPct!!, 0.0)
        assertEquals(2.8, b.boneKg!!, 0.0)
        assertEquals(1400.0, b.bmrKcal!!, 0.0)
        assertEquals(5.0, b.visceralFat!!, 0.0)
        assertEquals(17.0, b.proteinRate!!, 0.0)
    }

    @Test
    fun `falls back to value json when columns absent`() {
        val db = openDb(
            "body2.db",
            listOf(
                "CREATE TABLE weight_item_record(time INTEGER, sid TEXT, value TEXT, isDeleted INTEGER)",
                "INSERT INTO weight_item_record VALUES (100,'dev','{\"weight\":61.0,\"bmi\":22.5,\"body_fat_rate\":19.0}',0)",
            ),
        )
        val out = BodyCompositionParser.parse(db)
        db.close()

        val b = out.single()
        assertEquals(61.0, b.weightKg!!, 0.0)
        assertEquals(19.0, b.bodyFatRate!!, 0.0)
    }

    @Test
    fun `missing table yields empty`() {
        val db = openDb("empty.db", listOf("CREATE TABLE other(x INTEGER)"))
        assertEquals(0, BodyCompositionParser.parse(db).size)
        db.close()
    }
}
