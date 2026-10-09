package com.jhon.mihealthreader.extract

import java.io.InputStream

fun interface SourceReader {
    fun open(path: String): InputStream
}
