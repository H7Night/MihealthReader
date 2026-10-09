package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.SleepSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

object SleepSummary {

    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
    private const val ORIGIN_MINUTES = 18 * 60

    fun deepRatioPct(session: SleepSession): Int =
        if (session.durationMinutes > 0) session.deepMinutes * 100 / session.durationMinutes else 0

    fun awakeCount(session: SleepSession): Int = session.stages.count { it.stateName == "awake" }

    fun hhmm(epochSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochSecond(epochSeconds).atZone(zone).format(HHMM)

    /** Minutes since 18:00, so bedtimes around midnight do not wrap. */
    fun bedtimeMinutes(session: SleepSession, zone: ZoneId = ZoneId.systemDefault()): Int {
        val time = Instant.ofEpochSecond(session.start).atZone(zone)
        val minutes = time.hour * 60 + time.minute
        return ((minutes - ORIGIN_MINUTES) % 1440 + 1440) % 1440
    }

    fun formatBedtime(minutesSince18: Int): String {
        val total = ((minutesSince18 + ORIGIN_MINUTES) % 1440 + 1440) % 1440
        return "%02d:%02d".format(total / 60, total % 60)
    }

    data class Regularity(val averageMinutes: Int, val stdMinutes: Int, val nights: Int)

    fun bedtimeRegularity(
        sessions: List<SleepSession>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Regularity? {
        if (sessions.isEmpty()) return null
        val values = sessions.map { bedtimeMinutes(it, zone) }
        val avg = values.average()
        val std = sqrt(values.map { (it - avg) * (it - avg) }.average())
        return Regularity(avg.toInt(), std.toInt(), values.size)
    }

    fun regularityLabel(stdMinutes: Int): String = when {
        stdMinutes <= 30 -> "规律"
        stdMinutes <= 60 -> "较规律"
        else -> "不规律"
    }

    /** Bedtime falls in the evening/night window 18:00-06:00 (excludes daytime naps). */
    fun isNightSleep(session: SleepSession, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        bedtimeMinutes(session, zone) < 720

    fun interpret(session: SleepSession): String {
        val deepPct = deepRatioPct(session)
        return when {
            session.durationMinutes >= 420 && deepPct >= 25 -> "睡眠质量良好"
            deepPct < 15 -> "深睡偏少，建议关注睡眠环境"
            session.durationMinutes < 360 -> "睡眠时长偏短"
            else -> "睡眠质量一般"
        }
    }

    fun details(session: SleepSession, zone: ZoneId = ZoneId.systemDefault()): String =
        "睡眠 ${formatMinutes(session.durationMinutes)}，${hhmm(session.start, zone)} 入睡，" +
            "${hhmm(session.end, zone)} 起床，深睡占比 ${deepRatioPct(session)}%。"

    fun describe(session: SleepSession, zone: ZoneId = ZoneId.systemDefault()): String =
        "${interpret(session)}。${details(session, zone)}"

    private fun formatMinutes(minutes: Int): String = "${minutes / 60} 小时 ${minutes % 60} 分钟"
}
