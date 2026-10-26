package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.ui.Metric

sealed interface MetricDestination {
    data object Sleep : MetricDestination
    data object WorkoutList : MetricDestination
    data class Detail(val metric: Metric) : MetricDestination
}

fun metricDestination(metric: Metric): MetricDestination = when (metric) {
    Metric.SLEEP -> MetricDestination.Sleep
    Metric.WORKOUT -> MetricDestination.WorkoutList
    else -> MetricDestination.Detail(metric)
}
