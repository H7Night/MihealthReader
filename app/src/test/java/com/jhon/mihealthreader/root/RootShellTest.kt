package com.jhon.mihealthreader.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootShellTest {

    private fun fake(output: ShellOutput) = object : RootShell {
        override fun run(vararg commands: String): ShellOutput = output
    }

    @Test
    fun `root granted when id shows uid 0`() {
        val shell = fake(ShellOutput(0, listOf("uid=0(root) gid=0(root)"), emptyList()))
        assertTrue(shell.isRootGranted())
    }

    @Test
    fun `not granted when id shows normal uid`() {
        val shell = fake(ShellOutput(0, listOf("uid=2000(shell) gid=2000(shell)"), emptyList()))
        assertFalse(shell.isRootGranted())
    }

    @Test
    fun `not granted when command fails`() {
        val shell = fake(ShellOutput(1, emptyList(), listOf("permission denied")))
        assertFalse(shell.isRootGranted())
    }
}
