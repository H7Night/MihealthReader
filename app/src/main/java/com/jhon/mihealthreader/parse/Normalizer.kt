package com.jhon.mihealthreader.parse

import com.jhon.mihealthreader.dump.SqliteSupport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

class Normalizer {

    fun normalize(snapshotRoot: File, generatedAt: String = Instant.now().toString()): ParsedData {
        val fitnessData = findFitnessData(snapshotRoot)

        var sleep: List<SleepSession> = emptyList()
        var samples: List<StepSample> = emptyList()
        var activity: List<DailyActivity> = emptyList()
        var metrics: List<MetricSample> = emptyList()
        var workouts: List<Workout> = emptyList()
        var body: List<BodyMeasurement> = emptyList()

        if (fitnessData != null) {
            val db = SqliteSupport.openReadOnly(fitnessData)
            try {
                sleep = runCatching { SleepParser.parse(db) }.getOrDefault(emptyList())
                samples = runCatching { ActivityParser.parseSamples(db) }.getOrDefault(emptyList())
                activity = runCatching { ActivityParser.aggregate(samples) }.getOrDefault(emptyList())
                metrics = runCatching { HeartRateParser.parse(db) + Spo2Parser.parse(db) }
                    .getOrDefault(emptyList())
                workouts = runCatching { WorkoutParser.parse(db) }.getOrDefault(emptyList())
                body = runCatching { BodyCompositionParser.parse(db) }.getOrDefault(emptyList())
            } finally {
                db.close()
                SqliteSupport.removeStaleShm(fitnessData)
            }
        }

        val dest = File(snapshotRoot, "parsed/normalized.json")
        writeNormalizedEnvelope(sleep, activity, metrics, workouts, body, skipped = 0, dest = dest, generatedAt = generatedAt)
        return ParsedData(
            sleep = sleep,
            samples = samples,
            activity = activity,
            skipped = 0,
            outputFile = dest,
            metricSamples = metrics,
            workouts = workouts,
            bodyMeasurements = body,
        )
    }

    private fun findFitnessData(snapshotRoot: File): File? =
        File(snapshotRoot, "raw").walkTopDown()
            .firstOrNull { it.isFile && it.name == "fitness_data" }
}

fun writeNormalizedEnvelope(
    sleep: List<SleepSession>,
    activity: List<DailyActivity>,
    metricSamples: List<MetricSample> = emptyList(),
    workouts: List<Workout> = emptyList(),
    bodyMeasurements: List<BodyMeasurement> = emptyList(),
    skipped: Int,
    dest: File,
    generatedAt: String,
) {
    val records = JSONObject()
    records.put("sleep", JSONArray().apply { sleep.forEach { put(sleepJson(it)) } })
    records.put("daily_activity", JSONArray().apply { activity.forEach { put(activityJson(it)) } })
    records.put("heart_rate", metricArray(metricSamples, MetricTypes.HEART_RATE))
    records.put("resting_heart_rate", metricArray(metricSamples, MetricTypes.RESTING_HEART_RATE))
    records.put("spo2", metricArray(metricSamples, MetricTypes.SPO2))
    records.put("workouts", JSONArray().apply { workouts.forEach { put(workoutJson(it)) } })
    records.put("body_measurements", JSONArray().apply { bodyMeasurements.forEach { put(bodyJson(it)) } })

    val root = JSONObject()
    root.put("schema_version", "1.0")
    root.put("source", "mihealthreader")
    root.put("generated_at", generatedAt)
    root.put("skipped", skipped)
    root.put("records", records)

    dest.parentFile?.mkdirs()
    dest.writeText(root.toString(2))
}

private fun metricArray(samples: List<MetricSample>, type: String): JSONArray =
    JSONArray().apply {
        samples.filter { it.metricType == type }.forEach {
            put(JSONObject().apply {
                put("time", it.time)
                put("value", it.value)
            })
        }
    }

private fun workoutJson(workout: Workout): JSONObject = JSONObject().apply {
    put("start", workout.start)
    put("end", workout.end)
    put("duration_sec", workout.durationSec)
    put("calories", workout.calories)
    put("avg_hr", workout.avgHr ?: JSONObject.NULL)
    put("max_hr", workout.maxHr ?: JSONObject.NULL)
    put("min_hr", workout.minHr ?: JSONObject.NULL)
    put("distance_m", workout.distanceMeters ?: JSONObject.NULL)
    put("type", workout.type)
    put("type_name", workout.typeName)
}

private fun bodyJson(body: BodyMeasurement): JSONObject = JSONObject().apply {
    put("time", body.time)
    put("weight_kg", body.weightKg ?: JSONObject.NULL)
    put("bmi", body.bmi ?: JSONObject.NULL)
    put("body_fat_rate", body.bodyFatRate ?: JSONObject.NULL)
    put("muscle_kg", body.muscleKg ?: JSONObject.NULL)
    put("water_pct", body.waterPct ?: JSONObject.NULL)
    put("bone_kg", body.boneKg ?: JSONObject.NULL)
    put("bmr_kcal", body.bmrKcal ?: JSONObject.NULL)
    put("visceral_fat", body.visceralFat ?: JSONObject.NULL)
    put("protein_rate", body.proteinRate ?: JSONObject.NULL)
}

private fun sleepJson(session: SleepSession): JSONObject = JSONObject().apply {
    put("start", session.start)
    put("end", session.end)
    put("duration_min", session.durationMinutes)
    put("deep_min", session.deepMinutes)
    put("light_min", session.lightMinutes)
    put("avg_hr", session.avgHr ?: JSONObject.NULL)
    put("min_hr", session.minHr ?: JSONObject.NULL)
    put("max_hr", session.maxHr ?: JSONObject.NULL)
    put("stages", JSONArray().apply {
        session.stages.forEach { stage ->
            put(JSONObject().apply {
                put("start", stage.start)
                put("end", stage.end)
                put("state", stage.state)
                put("state_name", stage.stateName)
            })
        }
    })
}

private fun activityJson(activity: DailyActivity): JSONObject = JSONObject().apply {
    put("date", activity.date)
    put("steps", activity.steps)
    put("distance_m", activity.distanceMeters)
    put("active_kcal", activity.calories)
}
