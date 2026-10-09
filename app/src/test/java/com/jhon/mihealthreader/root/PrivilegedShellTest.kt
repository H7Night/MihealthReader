package com.jhon.mihealthreader.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivilegedShellTest {

    @Test
    fun `joins args into a single command`() {
        assertEquals(
            "find /data/data/com.mi.health/databases -type f",
            buildShellCommand(arrayOf("find", "/data/data/com.mi.health/databases", "-type", "f")),
        )
    }

    @Test
    fun `quotes args containing spaces`() {
        assertEquals("echo 'a b'", buildShellCommand(arrayOf("echo", "a b")))
    }

    @Test
    fun `detects root from id output`() {
        assertTrue(outputIndicatesRoot(ShellOutput(0, listOf("uid=0(root) gid=0(root)"), emptyList())))
        assertFalse(outputIndicatesRoot(ShellOutput(0, listOf("uid=2000(shell) gid=2000(shell)"), emptyList())))
        assertFalse(outputIndicatesRoot(ShellOutput(1, emptyList(), listOf("permission denied"))))
    }

    @Test
    fun `shizuku granted requires ping, permission and root id`() {
        val rootId = ShellOutput(0, listOf("uid=0(root)"), emptyList())
        val shellId = ShellOutput(0, listOf("uid=2000(shell)"), emptyList())
        assertTrue(shizukuGranted(ping = true, permissionGranted = true, id = rootId))
        assertFalse(shizukuGranted(ping = false, permissionGranted = true, id = rootId))
        assertFalse(shizukuGranted(ping = true, permissionGranted = false, id = rootId))
        assertFalse(shizukuGranted(ping = true, permissionGranted = true, id = shellId))
    }
}
