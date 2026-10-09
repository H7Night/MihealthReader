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
