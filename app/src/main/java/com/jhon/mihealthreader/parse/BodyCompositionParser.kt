package com.jhon.mihealthreader.parse

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

object BodyCompositionParser {

    fun parse(db: SQLiteDatabase): List<BodyMeasurement> {
        if (!db.tableExists("weight_item_record")) return emptyList()
        return if (db.hasColumn("weight_item_record", "weight")) {
            parseColumns(db)
        } else {
            parseValueJson(db)
        }
    }

    private fun parseColumns(db: SQLiteDatabase): List<BodyMeasurement> {
        val out = mutableListOf<BodyMeasurement>()
        val sql = "SELECT time, sid, weight, bmi, bodyFatRate, moistureRate, boneMass, " +
            "basalMetabolism, muscleMass, proteinRate, visceralFat, isDeleted FROM weight_item_record"
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                if (!c.isNull(11) && c.getInt(11) == 1) continue
                val time = c.getLong(0)
                if (time <= 0) continue
                out.add(
                    BodyMeasurement(
                        sourceId = if (c.isNull(1)) "" else c.getString(1),
                        time = time,
                        weightKg = dbl(c, 2),
                        bmi = dbl(c, 3),
                        bodyFatRate = dbl(c, 4),
                        muscleKg = dbl(c, 8),
                        waterPct = dbl(c, 5),
                        boneKg = dbl(c, 6),
                        bmrKcal = dbl(c, 7),
                        visceralFat = dbl(c, 10),
                        proteinRate = dbl(c, 9),
                    )
                )
            }
        }
        return out
    }

    private fun parseValueJson(db: SQLiteDatabase): List<BodyMeasurement> {
        val out = mutableListOf<BodyMeasurement>()
        db.rawQuery("SELECT time, sid, value, isDeleted FROM weight_item_record", null).use { c ->
            while (c.moveToNext()) {
                if (!c.isNull(3) && c.getInt(3) == 1) continue
                val json = c.getString(2) ?: continue
                val obj = try {
                    JSONObject(json)
                } catch (e: Exception) {
                    continue
                }
                val time = if (!c.isNull(0)) c.getLong(0) else (obj.optLongOrNull("time") ?: 0)
                if (time <= 0) continue
                out.add(
                    BodyMeasurement(
                        sourceId = if (c.isNull(1)) "" else c.getString(1),
                        time = time,
                        weightKg = obj.optDoubleOrNull("weight"),
                        bmi = obj.optDoubleOrNull("bmi"),
                        bodyFatRate = obj.optDoubleOrNull("body_fat_rate"),
                        muscleKg = obj.optDoubleOrNull("muscle_mass"),
                        waterPct = obj.optDoubleOrNull("moisture_rate"),
                        boneKg = obj.optDoubleOrNull("bone_mass"),
                        bmrKcal = obj.optDoubleOrNull("basal_metabolism"),
                        visceralFat = obj.optDoubleOrNull("visceral_fat"),
                        proteinRate = obj.optDoubleOrNull("protein_rate"),
                    )
                )
            }
        }
        return out
    }

    private fun dbl(cursor: Cursor, index: Int): Double? =
        if (cursor.isNull(index)) null else cursor.getDouble(index)
}
