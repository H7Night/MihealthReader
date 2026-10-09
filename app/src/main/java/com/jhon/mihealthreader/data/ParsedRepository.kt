package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.SleepSession
import com.jhon.mihealthreader.parse.SleepStage
import org.json.JSONObject
import java.io.File

class ParsedRepository(private val exportsDir: File) {

    fun latestNormalizedFile(): File? {
        val latest = exportsDir.listFiles { file -> file.isDirectory }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?: return null
        val file = File(latest, "parsed/normalized.json")
        return if (file.isFile) file else null
    }

    fun dailyActivity(): List<DailyActivity> {
        val array = readRecords("daily_activity") ?: return emptyList()
        return (0 until array.length())
            .map { i ->
                val o = array.getJSONObject(i)
                DailyActivity(
                    date = o.getString("date"),
                    steps = o.getInt("steps"),
                    distanceMeters = o.getInt("distance_m"),
                    calories = o.getInt("active_kcal"),
                )
            }
            .sortedByDescending { it.date }
    }

    fun sleepSessions(): List<SleepSession> {
        val array = readRecords("sleep") ?: return emptyList()
        return (0 until array.length())
            .map { i -> parseSleep(array.getJSONObject(i)) }
            .sortedByDescending { it.start }
    }

    private fun parseSleep(o: JSONObject): SleepSession {
        val stagesArr = o.optJSONArray("stages")
        val stages = if (stagesArr == null) emptyList() else (0 until stagesArr.length()).map { j ->
            val s = stagesArr.getJSONObject(j)
            SleepStage(
                start = s.getLong("start"),
                end = s.getLong("end"),
                state = s.getInt("state"),
                stateName = s.getString("state_name"),
            )
        }
        return SleepSession(
            start = o.getLong("start"),
            end = o.getLong("end"),
            durationMinutes = o.getInt("duration_min"),
            deepMinutes = o.getInt("deep_min"),
            lightMinutes = o.getInt("light_min"),
            avgHr = o.optIntOrNull("avg_hr"),
            minHr = o.optIntOrNull("min_hr"),
            maxHr = o.optIntOrNull("max_hr"),
            stages = stages,
        )
    }

    private fun readRecords(name: String): org.json.JSONArray? {
        val file = latestNormalizedFile() ?: return null
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
        return root.optJSONObject("records")?.optJSONArray(name)
    }
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    val value = optInt(key)
    return value
}
