package com.jhon.mihealthreader.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GhLoadingStateTest {
    @Test
    fun `not loaded shows loading`() = assertTrue(shouldShowLoading(false))

    @Test
    fun `loaded hides loading`() = assertFalse(shouldShowLoading(true))
}
