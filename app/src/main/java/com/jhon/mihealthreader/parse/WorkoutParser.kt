package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

object WorkoutTypes {
    private val names = mapOf(
        "outdoor_walking" to "户外步行",
        "outdoor_running" to "户外跑步",
        "indoor_running" to "室内跑步",
        "outdoor_riding" to "户外骑行",
        "indoor_riding" to "室内骑行",
        "climbing" to "登山",
        "pingpong" to "乒乓球",
        "rope_skipping" to "跳绳",
        "motion_sensing_game" to "体感游戏",
        "dumbbell_training" to "哑铃训练",
        "free_training" to "自由训练",
        "strength_training" to "力量训练",
        "swimming" to "游泳",
        "elliptical" to "椭圆机",
        "yoga" to "瑜伽",
        "basketball" to "篮球",
        "football" to "足球",
        "badminton" to "羽毛球",
        "walking" to "步行",
        "running" to "跑步",
    )

    fun name(key: String): String = names[key] ?: key
}

object WorkoutParser {

    fun parse(db: SQLiteDatabase): List<Workout> {
        if (!db.tableExists("sport_report")) return emptyList()
        val workouts = mutableListOf<Workout>()
        db.rawQuery("SELECT value, key, sid, time, isDeleted FROM sport_report", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (!cursor.isNull(4) && cursor.getInt(4) == 1) continue
                val json = cursor.getString(0) ?: continue
                val obj = try {
                    JSONObject(json)
                } catch (e: Exception) {
                    continue
                }
                val start = obj.optLongOrNull("start_time")
                    ?: (if (cursor.isNull(3)) 0L else cursor.getLong(3))
                if (start <= 0) continue
                val end = obj.optLongOrNull("end_time") ?: start
                val type = cursor.getString(1) ?: obj.optString("sport_type", "unknown")
                val duration = obj.optIntOrNull("duration") ?: (end - start).toInt()
                val calories = obj.optIntOrNull("calories") ?: obj.optIntOrNull("total_cal") ?: 0
                val sourceId = if (cursor.isNull(2)) "" else cursor.getString(2)
                workouts.add(
                    Workout(
                        sourceId = sourceId,
                        start = start,
                        end = end,
                        durationSec = duration,
                        calories = calories,
                        avgHr = obj.optIntOrNull("avg_hrm"),
                        maxHr = obj.optIntOrNull("max_hrm"),
                        minHr = obj.optIntOrNull("min_hrm"),
                        distanceMeters = obj.optIntOrNull("distance"),
                        type = type,
                        typeName = WorkoutTypes.name(type),
                    )
                )
            }
        }
        return workouts
    }
}
