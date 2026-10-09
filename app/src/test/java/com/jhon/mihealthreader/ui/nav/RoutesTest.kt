package com.jhon.mihealthreader.ui.nav

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    @Test
    fun `bottom bar only on main tabs`() {
        assertTrue(showsBottomBar(Routes.TODAY))
        assertTrue(showsBottomBar(Routes.CHARTS))
        assertTrue(showsBottomBar(Routes.SETTINGS))
        assertFalse(showsBottomBar(Routes.BROWSE))
        assertFalse(showsBottomBar(null))
    }
}
