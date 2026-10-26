package com.jhon.mihealthreader.ui

import androidx.annotation.StringRes
import com.jhon.mihealthreader.R

enum class MetricChartKind { Bars, Dotted, Area }

enum class Metric(
    @StringRes val labelRes: Int,
    @StringRes val unitRes: Int,
    val chartKind: MetricChartKind,
    val sumBuckets: Boolean,
) {
    STEPS(R.string.metric_steps, R.string.unit_steps, MetricChartKind.Bars, true),
    SLEEP(R.string.metric_sleep, R.string.unit_minutes, MetricChartKind.Bars, true),
    HR(R.string.metric_hr, R.string.unit_bpm, MetricChartKind.Dotted, false),
    SPO2(R.string.metric_spo2, R.string.unit_percent, MetricChartKind.Dotted, false),
    WORKOUT(R.string.metric_workout, R.string.unit_minutes, MetricChartKind.Bars, true),
    BODY(R.string.metric_body, R.string.unit_kg, MetricChartKind.Area, false),
}
