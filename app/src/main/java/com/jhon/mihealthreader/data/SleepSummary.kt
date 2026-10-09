package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.SleepSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SleepSummary {

    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

    fun deepRatioPct(session: SleepSession): Int =
        if (session.durationMinutes > 0) session.deepMinutes * 100 / session.durationMinutes else 0

    fun awakeCount(session: SleepSession): Int = session.stages.count { it.stateName == "awake" }

    fun hhmm(epochSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochSecond(epochSeconds).atZone(zone).format(HHMM)

    fun describe(session: SleepSession, zone: ZoneId = ZoneId.systemDefault()): String {
        val deepPct = deepRatioPct(session)
        val quality = when {
            session.durationMinutes >= 420 && deepPct >= 25 -> "睡眠质量良好"
            deepPct < 15 -> "深睡偏少，建议关注睡眠环境"
            session.durationMinutes < 360 -> "睡眠时长偏短"
            else -> "睡眠质量一般"
        }
        return "$quality。睡眠 ${formatMinutes(session.durationMinutes)}，" +
            "${hhmm(session.start, zone)} 入睡，${hhmm(session.end, zone)} 起床，深睡占比 $deepPct%。"
    }

    private fun formatMinutes(minutes: Int): String = "${minutes / 60} 小时 ${minutes % 60} 分钟"
}
