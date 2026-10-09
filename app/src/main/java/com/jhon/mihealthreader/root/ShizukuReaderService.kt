package com.jhon.mihealthreader.root

import android.os.ParcelFileDescriptor
import com.jhon.mihealthreader.IUserService
import java.io.File

/**
 * Runs inside Shizuku's process (root when Shizuku was started with root).
 * Exposes command execution and file opening to the app via AIDL.
 */
class ShizukuReaderService : IUserService.Stub() {

    override fun run(command: String): List<String> {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val stdout = process.inputStream.bufferedReader().readLines()
        val stderr = process.errorStream.bufferedReader().readLines()
        val exitCode = process.waitFor()
        return ArrayList<String>().apply {
            add("exit=$exitCode")
            addAll(stdout)
            add("--err--")
            addAll(stderr)
        }
    }

    override fun openFile(path: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
}
