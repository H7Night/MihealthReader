package com.jhon.mihealthreader.ui

import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.data.TableEntry
import org.json.JSONObject
import java.io.File

class BrowserViewModel(
    private val repository: ExportRepository,
    private val snapshot: File?,
) {
    fun tables(): List<TableEntry> = snapshot?.let { repository.tables(it) } ?: emptyList()

    fun rows(entry: TableEntry): List<JSONObject> = repository.rows(entry)
}
