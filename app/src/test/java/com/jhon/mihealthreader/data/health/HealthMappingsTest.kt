package com.jhon.mihealthreader.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.jhon.mihealthreader.parse.StepSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

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
        val days = stepsPerLocalDay(samples)
        assertEquals(350, days.first().count)
        assertEquals(40, days.last().count)
    }

    @Test
    fun `empty samples produce no days`() {
        assertTrue(stepsPerLocalDay(emptyList()).isEmpty())
    }

    @Test
    fun `steps on different local days stay separate`() {
        val zone = ZoneId.of("UTC")
        val day1 = Instant.parse("2026-01-01T10:00:00Z").toEpochMilli()
        val day2 = Instant.parse("2026-01-02T10:00:00Z").toEpochMilli()
        val byDay = stepsPerLocalDay(
            listOf(
                StepSample("s", day1, 0, 100, 0, 0),
                StepSample("s", day1 + 60_000, 0, 50, 0, 0),
                StepSample("s", day2, 0, 7, 0, 0),
            ),
            zone,
        ).associate { it.day to it.count }
        assertEquals(150, byDay["2026-01-01"])
        assertEquals(7, byDay["2026-01-02"])
    }

    @Test
    fun `stepsPerLocalDay reports the span of each day`() {
        val zone = ZoneId.of("UTC")
        val t0 = Instant.parse("2026-01-01T08:00:00Z").toEpochMilli()
        val t1 = Instant.parse("2026-01-01T20:00:00Z").toEpochMilli()
        val days = stepsPerLocalDay(
            listOf(
                StepSample("s", t1, 0, 20, 0, 0),
                StepSample("s", t0, 0, 80, 0, 0),
            ),
            zone,
        )
        assertEquals(1, days.size)
        assertEquals(t0, days.first().start)
        assertEquals(t1, days.first().end)
        assertEquals(100, days.first().count)
    }
}
