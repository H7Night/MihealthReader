package com.jhon.mihealthreader.root

data class ShellOutput(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>,
)

interface RootShell {
    fun run(vararg commands: String): ShellOutput

    fun isRootGranted(): Boolean {
        val output = run("id")
        return output.exitCode == 0 && output.stdout.any { it.contains("uid=0") }
    }
}

private val SAFE_ARG = Regex("[A-Za-z0-9_./=:@,+-]+")

internal fun buildShellCommand(commands: Array<out String>): String =
    commands.joinToString(" ") { arg ->
        if (arg.matches(SAFE_ARG)) arg else "'" + arg.replace("'", "'\\''") + "'"
    }
