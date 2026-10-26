package com.jhon.mihealthreader.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhAxisTest {
    @Test
    fun `empty values produce a safe axis`() {
        val axis = ghAxis(emptyList(), fromZero = true)
        assertEquals(0f, axis.min, 0f)
        assertEquals(1f, axis.max, 0f)
        assertEquals(3, axis.ticks.size)
    }

    @Test
    fun `zero based axis starts at zero`() {
        val axis = ghAxis(listOf(0f, 120f, 60f), fromZero = true)
        assertEquals(0f, axis.min, 0f)
        assertEquals(120f, axis.max, 0f)
        assertEquals(listOf(120f, 60f, 0f), axis.ticks)
    }

    @Test
    fun `non zero axis keeps the minimum`() {
        val axis = ghAxis(listOf(61f, 72f, 80f), fromZero = false)
        assertEquals(61f, axis.min, 0f)
        assertEquals(80f, axis.max, 0f)
    }

    @Test
    fun `constant values center the line`() {
        val axis = ghAxis(listOf(70f, 70f, 70f), fromZero = false)
        assertEquals(70f, (axis.min + axis.max) / 2f, 0.01f)
        assertTrue(axis.min < 70f)
        assertTrue(axis.max > 70f)
    }

    @Test
    fun `label indices include first and last`() {
        val indices = ghLabelIndices(30, maxLabels = 7)
        assertEquals(7, indices.size)
        assertEquals(0, indices.first())
        assertEquals(29, indices.last())
        assertTrue(indices.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `short series shows every label`() {
        assertEquals(listOf(0, 1, 2), ghLabelIndices(3))
    }
}

