package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

object SleepParser {

    fun stateName(state: Int): String = when (state) {
        1 -> "awake"
        2 -> "light"
        3 -> "deep"
        4 -> "rem"
        else -> "unknown"
    }

    fun parseValue(valueJson: String): SleepSession? {
        val value = try {
            JSONObject(valueJson)
        } catch (e: Exception) {
            return null
        }

        val start = value.optLongOrNull("bedtime")
            ?: value.optLongOrNull("bed_timestamp")
            ?: return null
        val end = value.optLongOrNull("device_wake_up_time")
            ?: value.optLongOrNull("out_bed_timestamp")
            ?: value.optLongOrNull("bed_timestamp")
            ?: return null

        val duration = value.optIntOrNull("duration") ?: ((end - start) / 60).toInt()
        val deep = value.optIntOrNull("sleep_deep_duration") ?: 0
        val light = value.optIntOrNull("sleep_light_duration") ?: 0

        val stages = mutableListOf<SleepStage>()
        value.optJSONArray("items")?.let { items ->
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val stageStart = item.optLong("start_time", -1)
                val stageEnd = item.optLong("end_time", -1)
                val state = item.optInt("state", -1)
                if (stageStart >= 0 && stageEnd >= 0 && state >= 0) {
                    stages.add(SleepStage(stageStart, stageEnd, state, stateName(state)))
                }
            }
        }

        return SleepSession(
            start = start,
            end = end,
            durationMinutes = duration,
            deepMinutes = deep,
            lightMinutes = light,
            avgHr = value.optIntOrNull("avg_hr"),
            minHr = value.optIntOrNull("min_hr"),
            maxHr = value.optIntOrNull("max_hr"),
            stages = stages,
        )
    }

    fun parse(db: SQLiteDatabase): List<SleepSession> {
        val sessions = mutableListOf<SleepSession>()
        db.rawQuery("SELECT value FROM sleep_segment", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.isNull(0)) continue
                val json = cursor.getString(0) ?: continue
                parseValue(json)?.let { sessions.add(it) }
            }
        }
        return sessions
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null
