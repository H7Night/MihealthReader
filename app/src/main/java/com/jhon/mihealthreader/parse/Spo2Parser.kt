package com.jhon.mihealthreader.parse

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

object Spo2Parser {

    fun parse(db: SQLiteDatabase): List<MetricSample> {
        if (!db.tableExists("spo2_record")) return emptyList()
        val samples = mutableListOf<MetricSample>()
        db.rawQuery("SELECT value, key, sid, isDeleted, time FROM spo2_record", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (!cursor.isNull(3) && cursor.getInt(3) == 1) continue
                val key = cursor.getString(1)
                if (key != "spo2" && key != "single_spo2") continue
                val json = cursor.getString(0) ?: continue
                val obj = try {
                    JSONObject(json)
                } catch (e: Exception) {
                    continue
                }
                val spo2 = obj.optDoubleOrNull("spo2") ?: continue
                val time = if (!cursor.isNull(4)) cursor.getLong(4) else (obj.optLongOrNull("time") ?: 0)
                val sourceId = if (cursor.isNull(2)) "" else cursor.getString(2)
                samples.add(MetricSample(MetricTypes.SPO2, sourceId, time, spo2))
            }
        }
        return samples
    }
}
