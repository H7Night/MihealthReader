package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

object HeartRateParser {

    fun parse(db: SQLiteDatabase): List<MetricSample> {
        if (!db.tableExists("hr_record")) return emptyList()
        val samples = mutableListOf<MetricSample>()
        db.rawQuery("SELECT value, key, sid, isDeleted, time FROM hr_record", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (!cursor.isNull(3) && cursor.getInt(3) == 1) continue
                val type = when (cursor.getString(1)) {
                    "heart_rate" -> MetricTypes.HEART_RATE
                    "resting_heart_rate" -> MetricTypes.RESTING_HEART_RATE
                    else -> continue
                }
                val json = cursor.getString(0) ?: continue
                val obj = try {
                    JSONObject(json)
                } catch (e: Exception) {
                    continue
                }
                val bpm = obj.optDoubleOrNull("bpm") ?: continue
                val time = if (!cursor.isNull(4)) cursor.getLong(4) else (obj.optLongOrNull("time") ?: 0)
                val sourceId = if (cursor.isNull(2)) "" else cursor.getString(2)
                samples.add(MetricSample(type, sourceId, time, bpm))
            }
        }
        return samples
    }
}
