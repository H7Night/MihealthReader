package com.jhon.mihealthreader.ui

import java.io.File

sealed interface ExtractionState {
    data object Idle : ExtractionState
    data class Running(val step: String) : ExtractionState
    data class Success(
        val snapshot: File,
        val tableCount: Int,
        val newRecords: Int,
    ) : ExtractionState
    data class Error(val message: String) : ExtractionState
}
