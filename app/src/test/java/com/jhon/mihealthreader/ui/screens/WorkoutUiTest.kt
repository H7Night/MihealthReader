package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutUiTest {
    private fun workout(type: String) = Workout(
        "dev",
        0,
        600,
        600,
        100,
        null,
        null,
        null,
        null,
        type,
        type,
    )

    @Test
    fun `types are distinct most frequent first`() {
        val options = workoutTypeOptions(
            listOf(workout("Running"), workout("Running"), workout("Walking")),
        )
        assertEquals(listOf("Running", "Walking"), options)
    }

    @Test
    fun `null filter keeps everything`() {
        val all = listOf(workout("Running"), workout("Walking"))
        assertEquals(all, filterWorkouts(all, null))
    }

    @Test
    fun `type filter keeps matching sessions`() {
        val all = listOf(workout("Running"), workout("Walking"))
        assertEquals(listOf(all[1]), filterWorkouts(all, "Walking"))
    }
}
