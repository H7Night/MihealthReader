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
        assertFalse(showsBottomBar(null))
        assertFalse(showsBottomBar("detail/STEPS"))
    }

    @Test
    fun `detail and workout routes hide the bar`() {
        assertFalse(showsBottomBar("sleep_detail"))
        assertFalse(showsBottomBar("workout_list"))
        assertFalse(showsBottomBar("workout_detail/0"))
    }
}
