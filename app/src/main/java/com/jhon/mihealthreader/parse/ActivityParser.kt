package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset

object ActivityParser {

    fun localDate(time: Long, zoneOffsetSec: Int): String =
        Instant.ofEpochSecond(time)
            .atOffset(ZoneOffset.ofTotalSeconds(zoneOffsetSec))
            .toLocalDate()
            .toString()

    fun aggregate(samples: List<StepSample>): List<DailyActivity> {
        val byMinute = LinkedHashMap<Long, StepSample>()
        for (sample in samples) {
            val minuteKey = Math.floorDiv(sample.time + sample.zoneOffsetSec, 60L)
            val existing = byMinute[minuteKey]
            if (existing == null || sample.steps > existing.steps) {
                byMinute[minuteKey] = sample
            }
        }

        val byDate = LinkedHashMap<String, DailyActivity>()
        for (sample in byMinute.values) {
            val date = localDate(sample.time, sample.zoneOffsetSec)
            val acc = byDate[date]
            byDate[date] = if (acc == null) {
                DailyActivity(date, sample.steps, sample.distanceMeters, sample.calories)
            } else {
                DailyActivity(
                    date,
                    acc.steps + sample.steps,
                    acc.distanceMeters + sample.distanceMeters,
                    acc.calories + sample.calories,
                )
            }
        }
        return byDate.values.sortedBy { it.date }
    }

    fun parseSamples(db: SQLiteDatabase): List<StepSample> {
        val samples = mutableListOf<StepSample>()
        val sql = if (db.hasColumn("step_record", "sid")) {
            "SELECT value, zoneOffsetInSec, isDeleted, sid FROM step_record"
        } else {
            "SELECT value, zoneOffsetInSec, isDeleted, '' FROM step_record"
        }
        db.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                if (!cursor.isNull(2) && cursor.getInt(2) == 1) continue
                val json = cursor.getString(0) ?: continue
                val obj = try {
                    JSONObject(json)
                } catch (e: Exception) {
                    continue
                }
                val time = obj.optLongOrNull("time") ?: continue
                val zone = if (cursor.isNull(1)) 0 else cursor.getInt(1)
                val sourceId = if (cursor.isNull(3)) "" else cursor.getString(3)
                samples.add(
                    StepSample(
                        sourceId = sourceId,
                        time = time,
                        zoneOffsetSec = zone,
                        steps = obj.optInt("steps", 0),
                        distanceMeters = obj.optInt("distance", 0),
                        calories = obj.optInt("calories", 0),
                    )
                )
            }
        }
        return samples
    }
}
