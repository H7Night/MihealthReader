package com.jhon.mihealthreader.ui.component

import com.jhon.mihealthreader.ui.theme.GhDarkFallback
import com.jhon.mihealthreader.ui.theme.GhLightFallback
import org.junit.Assert.assertEquals
import org.junit.Test

class GhToneTest {
    @Test
    fun `light tones use matching containers`() {
        assertEquals(GhLightFallback.primaryContainer, ghToneContainer(GhLightFallback, GhTone.Primary))
        assertEquals(GhLightFallback.secondaryContainer, ghToneContainer(GhLightFallback, GhTone.Secondary))
        assertEquals(GhLightFallback.tertiaryContainer, ghToneContainer(GhLightFallback, GhTone.Tertiary))
        assertEquals(GhLightFallback.surfaceContainerHigh, ghToneContainer(GhLightFallback, GhTone.Neutral))
    }

    @Test
    fun `dark tones use matching on-colors`() {
        assertEquals(GhDarkFallback.onPrimaryContainer, ghToneContent(GhDarkFallback, GhTone.Primary))
        assertEquals(GhDarkFallback.onSecondaryContainer, ghToneContent(GhDarkFallback, GhTone.Secondary))
        assertEquals(GhDarkFallback.onTertiaryContainer, ghToneContent(GhDarkFallback, GhTone.Tertiary))
        assertEquals(GhDarkFallback.onSurface, ghToneContent(GhDarkFallback, GhTone.Neutral))
    }
}
