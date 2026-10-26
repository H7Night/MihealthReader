package com.jhon.mihealthreader.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhBarLayoutTest {
    @Test
    fun `thirty buckets keep a positive bar width`() {
        val layout = ghBarLayout(30, 350f)
        assertTrue(layout.barWidth > 0f)
        assertTrue(layout.gap >= 0f)
        assertTrue(layout.barWidth * 30 + layout.gap * 29 <= 350.01f)
    }

    @Test
    fun `single bucket fills the width`() {
        val layout = ghBarLayout(1, 350f)
        assertEquals(350f, layout.barWidth, 0.01f)
        assertEquals(0f, layout.gap, 0f)
    }

    @Test
    fun `empty layout is zero`() {
        val layout = ghBarLayout(0, 350f)
        assertEquals(0f, layout.barWidth, 0f)
        assertEquals(0f, layout.gap, 0f)
    }
}
