package com.jhon.mihealthreader.parse

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun writeNormalizedEnvelope(
    sleep: List<SleepSession>,
    activity: List<DailyActivity>,
    skipped: Int,
    dest: File,
    generatedAt: String,
) {
    val records = JSONObject()
    records.put("sleep", JSONArray().apply { sleep.forEach { put(sleepJson(it)) } })
    records.put("daily_activity", JSONArray().apply { activity.forEach { put(activityJson(it)) } })

    val root = JSONObject()
    root.put("schema_version", "1.0")
    root.put("source", "mihealthreader")
    root.put("generated_at", generatedAt)
    root.put("skipped", skipped)
    root.put("records", records)

    dest.parentFile?.mkdirs()
    dest.writeText(root.toString(2))
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
