package com.jhon.mihealthreader.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GhFallbackColorsTest {
    @Test
    fun `light fallback uses google health surfaces`() {
        assertEquals(Color(0xFFF0F4F9), GhLightFallback.background)
        assertEquals(Color(0xFFFFFFFF), GhLightFallback.surface)
        assertEquals(Color(0xFF1157CE), GhLightFallback.primary)
        assertEquals(Color(0xFFF0F4F9), GhLightFallback.surfaceContainer)
    }

    @Test
    fun `dark fallback uses deep blue surfaces`() {
        assertEquals(Color(0xFF0F141A), GhDarkFallback.background)
        assertEquals(Color(0xFF171C22), GhDarkFallback.surface)
        assertEquals(Color(0xFF1B2129), GhDarkFallback.surfaceContainer)
        assertEquals(Color(0xFFA8C7FA), GhDarkFallback.primary)
    }
}
