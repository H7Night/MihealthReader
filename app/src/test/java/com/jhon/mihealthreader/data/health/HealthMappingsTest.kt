package com.jhon.mihealthreader.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.jhon.mihealthreader.parse.StepSample
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthMappingsTest {

    @Test
    fun `sleep stages map like the parser`() {
        assertEquals(SleepSessionRecord.STAGE_TYPE_AWAKE, sleepStageType(1))
        assertEquals(SleepSessionRecord.STAGE_TYPE_AWAKE, sleepStageType(5))
        assertEquals(SleepSessionRecord.STAGE_TYPE_DEEP, sleepStageType(2))
        assertEquals(SleepSessionRecord.STAGE_TYPE_LIGHT, sleepStageType(3))
        assertEquals(SleepSessionRecord.STAGE_TYPE_REM, sleepStageType(4))
        assertEquals(SleepSessionRecord.STAGE_TYPE_UNKNOWN, sleepStageType(99))
    }

    @Test
    fun `workout types map to health connect`() {
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, workoutExerciseType("outdoor_walking"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, workoutExerciseType("running"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, workoutExerciseType("indoor_riding"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, workoutExerciseType("strength_training"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, workoutExerciseType("swimming"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, workoutExerciseType("motion_sensing_game"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, workoutExerciseType("unknown_key"))
    }

    @Test
    fun `steps aggregate per local day`() {
        val samples = listOf(
            StepSample("s", 0L, 0, 100, 0, 0),
            StepSample("s", 60_000L, 0, 250, 0, 0),
            StepSample("s", 86_400_000L, 0, 40, 0, 0),
        )
        val byDay = stepsByLocalDay(samples)
        assertEquals(350, byDay.values.first())
        assertEquals(40, byDay.values.last())
    }
}
