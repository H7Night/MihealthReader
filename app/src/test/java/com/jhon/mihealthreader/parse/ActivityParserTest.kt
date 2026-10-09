package com.jhon.mihealthreader.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityParserTest {

    @Test
    fun `dedups same minute keeping larger steps`() {
        val out = ActivityParser.aggregate(
            listOf(
                StepSample(0, 0, 10, 5, 1),
                StepSample(0, 0, 30, 20, 2),
                StepSample(60, 0, 5, 3, 1),
            )
        )
        assertEquals(1, out.size)
        assertEquals(35, out.single().steps)
        assertEquals(23, out.single().distanceMeters)
        assertEquals(3, out.single().calories)
    }

    @Test
    fun `separates local days by zone offset`() {
        val time = 23L * 3600 + 30 * 60
        assertEquals("1970-01-02", ActivityParser.localDate(time, 28800))
        assertEquals("1970-01-01", ActivityParser.localDate(time, 0))
    }
}
