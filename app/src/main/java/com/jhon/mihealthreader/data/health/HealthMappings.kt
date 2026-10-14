package com.jhon.mihealthreader.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.jhon.mihealthreader.parse.StepSample
import java.time.Instant
import java.time.ZoneId

/**
 * Sleep stage codes as stored by [com.jhon.mihealthreader.parse.SleepParser.stateName]:
 * 1 & 5 = awake, 2 = deep, 3 = light, 4 = rem.
 */
fun sleepStageType(state: Int): Int = when (state) {
    1, 5 -> SleepSessionRecord.STAGE_TYPE_AWAKE
    2 -> SleepSessionRecord.STAGE_TYPE_DEEP
    3 -> SleepSessionRecord.STAGE_TYPE_LIGHT
    4 -> SleepSessionRecord.STAGE_TYPE_REM
    else -> SleepSessionRecord.STAGE_TYPE_UNKNOWN
}

/** Maps a Mi Fitness sport key (see `WorkoutTypes`) to a Health Connect exercise type. */
fun workoutExerciseType(typeKey: String): Int = when (typeKey) {
    "outdoor_walking", "walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
    "outdoor_running", "indoor_running", "running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
    "outdoor_riding", "indoor_riding" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
    "climbing" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
    "strength_training", "dumbbell_training", "free_training" ->
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    "swimming" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
    "elliptical" -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
    "basketball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
    "football" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
    "badminton" -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
    "pingpong" -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
    else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
}

/** Steps aggregated for one local calendar day. */
data class DaySteps(val day: String, val start: Long, val end: Long, val count: Int)

/** Per-day step aggregates (ISO yyyy-MM-dd), ordered by each day's first sample. */
fun stepsPerLocalDay(
    samples: List<StepSample>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<DaySteps> =
    samples
        .groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate().toString() }
        .map { (day, daySamples) ->
            DaySteps(
                day = day,
                start = daySamples.minOf { it.time },
                end = daySamples.maxOf { it.time },
                count = daySamples.sumOf { it.steps },
            )
        }
        .sortedBy { it.start }
