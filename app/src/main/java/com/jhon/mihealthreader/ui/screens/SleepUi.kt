package com.jhon.mihealthreader.ui.screens

import androidx.annotation.StringRes
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.parse.SleepSession

data class SleepBreakdown(val deep: Int, val light: Int, val awake: Int)

enum class SleepQuality { Good, LowDeep, Short, Fair }

fun sleepQuality(session: SleepSession): SleepQuality {
    val deepPct = if (session.durationMinutes > 0) {
        session.deepMinutes * 100 / session.durationMinutes
    } else {
        0
    }
    return when {
        session.durationMinutes >= 420 && deepPct >= 25 -> SleepQuality.Good
        deepPct < 15 -> SleepQuality.LowDeep
        session.durationMinutes < 360 -> SleepQuality.Short
        else -> SleepQuality.Fair
    }
}

@StringRes
fun sleepQualityRes(session: SleepSession): Int = when (sleepQuality(session)) {
    SleepQuality.Good -> R.string.sleep_quality_good
    SleepQuality.LowDeep -> R.string.sleep_quality_low_deep
    SleepQuality.Short -> R.string.sleep_quality_short
    SleepQuality.Fair -> R.string.sleep_quality_fair
}

fun sleepStageBreakdown(session: SleepSession): SleepBreakdown {
    val total = session.durationMinutes.coerceAtLeast(1)
    val deep = (session.deepMinutes * 100 / total).coerceIn(0, 100)
    val light = (session.lightMinutes * 100 / total).coerceIn(0, 100 - deep)
    return SleepBreakdown(deep, light, 100 - deep - light)
}

@StringRes
fun regularityLabelRes(stdMinutes: Int): Int = when {
    stdMinutes <= 30 -> R.string.sleep_regular
    stdMinutes <= 60 -> R.string.sleep_somewhat_regular
    else -> R.string.sleep_irregular
}
