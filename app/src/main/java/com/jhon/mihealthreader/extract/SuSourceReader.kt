package com.jhon.mihealthreader.extract

import com.topjohnwu.superuser.io.SuFileInputStream
import java.io.InputStream

class SuSourceReader : SourceReader {
    override fun open(path: String): InputStream = SuFileInputStream.open(path)
}
