package com.jhon.mihealthreader.dump

import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Opening helpers for copied Mi Fitness databases.
 *
 * A copied `-shm` (WAL index) is frequently stale relative to the copied `db`/`-wal`
 * and makes SQLite trust an outdated wal-index, locating wrong pages (can SIGBUS).
 * We therefore drop the `-shm` sibling before opening; SQLite rebuilds the index.
 */
object SqliteSupport {

    fun removeStaleShm(dbFile: File): Boolean {
        val shm = File(dbFile.path + "-shm")
        return shm.exists() && shm.delete()
    }

    fun openReadOnly(dbFile: File): SQLiteDatabase {
        removeStaleShm(dbFile)
        return SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
}
