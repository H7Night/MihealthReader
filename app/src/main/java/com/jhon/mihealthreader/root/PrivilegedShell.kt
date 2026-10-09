package com.jhon.mihealthreader.root

import java.io.InputStream

data class ShellOutput(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>,
)

interface PrivilegedShell {
    fun run(vararg commands: String): ShellOutput

    /** Binary-safe read of a file that may be inaccessible to the app process. */
    fun openFile(path: String): InputStream

    /** True when the channel is usable AND actually runs as uid=0. */
    fun isGranted(): Boolean
}

internal val SAFE_ARG = Regex("[A-Za-z0-9_./=:@,+-]+")

internal fun buildShellCommand(commands: Array<out String>): String =
    commands.joinToString(" ") { arg ->
        if (arg.matches(SAFE_ARG)) arg else "'" + arg.replace("'", "'\\''") + "'"
    }

internal fun outputIndicatesRoot(output: ShellOutput): Boolean =
    output.exitCode == 0 && output.stdout.any { it.contains("uid=0") }
