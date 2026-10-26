package com.jhon.mihealthreader.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MetricRangeUiTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun `past windows can move forward`() {
        assertTrue(canMoveNext(LocalDate.of(2026, 10, 20), today))
        assertTrue(canMoveNext(LocalDate.of(2026, 10, 18), today))
    }

    @Test
    fun `current and future windows cannot`() {
        assertFalse(canMoveNext(today, today))
        assertFalse(canMoveNext(today.plusDays(1), today))
    }
}
