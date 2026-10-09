package com.jhon.mihealthreader.dump

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SqliteSupportTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `removes stale shm`() {
        val db = File(tmp.root, "fitness_data").apply { writeText("x") }
        val shm = File(tmp.root, "fitness_data-shm").apply { writeText("y") }

        assertTrue(SqliteSupport.removeStaleShm(db))
        assertFalse(shm.exists())
    }

    @Test
    fun `returns false when no shm`() {
        val db = File(tmp.root, "fitness_data").apply { writeText("x") }
        assertFalse(SqliteSupport.removeStaleShm(db))
    }
}
