package com.jhon.mihealthreader.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class GhRingTest {
    @Test
    fun `zero total is empty`() = assertEquals(0f, ghRingProgress(3, 0), 0f)

    @Test
    fun `partial progress is clamped`() {
        assertEquals(3f / 7f, ghRingProgress(3, 7), 0.0001f)
        assertEquals(1f, ghRingProgress(9, 7), 0f)
        assertEquals(0f, ghRingProgress(-1, 7), 0f)
    }
}
