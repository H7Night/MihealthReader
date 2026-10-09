package com.jhon.mihealthreader.dump

import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DbDumperTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun makeDb(name: String, block: (SQLiteDatabase) -> Unit): File {
        val file = File(tmp.root, name)
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        block(db)
        db.close()
        return file
    }

    private fun jsonDir(): File = File(tmp.root, "json").also { it.mkdirs() }

    @Test
    fun `dumps table rows to json`() {
        val db = makeDb("a.db") {
            it.execSQL("CREATE TABLE t_sleep(id INTEGER, stage TEXT)")
            it.execSQL("INSERT INTO t_sleep VALUES (1,'deep')")
        }
        val result = DbDumper().dump(db, jsonDir())
        assertTrue(result.isSqlite)
        val arr = JSONArray(File(jsonDir(), "t_sleep.json").readText())
        assertEquals(1, arr.length())
        assertEquals("deep", arr.getJSONObject(0).getString("stage"))
        assertEquals(1L, arr.getJSONObject(0).getLong("id"))
        assertEquals(1, result.tables.single().rowCount)
    }

    @Test
    fun `non sqlite file is reported not crash`() {
        val bad = File(tmp.root, "bad.db").apply { writeText("not a database") }
        val result = DbDumper().dump(bad, jsonDir())
        assertFalse(result.isSqlite)
        assertNotNull(result.error)
    }

    @Test
    fun `empty table yields empty array`() {
        val db = makeDb("e.db") { it.execSQL("CREATE TABLE t(a INTEGER)") }
        DbDumper().dump(db, jsonDir())
        assertEquals(0, JSONArray(File(jsonDir(), "t.json").readText()).length())
    }

    @Test
    fun `quotes keyword table and column names`() {
        val db = makeDb("k.db") {
            it.execSQL("CREATE TABLE \"select\"(\"order\" INTEGER)")
            it.execSQL("INSERT INTO \"select\" VALUES (7)")
        }
        DbDumper().dump(db, jsonDir())
        assertEquals(7, JSONArray(File(jsonDir(), "select.json").readText()).getJSONObject(0).getInt("order"))
    }

    @Test
    fun `respects row limit`() {
        val db = makeDb("l.db") {
            it.execSQL("CREATE TABLE t(a INTEGER)")
            repeat(10) { i -> it.execSQL("INSERT INTO t VALUES ($i)") }
        }
        DbDumper(rowLimit = 3).dump(db, jsonDir())
        assertEquals(3, JSONArray(File(jsonDir(), "t.json").readText()).length())
    }
}
