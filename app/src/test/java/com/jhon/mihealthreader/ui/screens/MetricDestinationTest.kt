package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.ui.Metric
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricDestinationTest {
    @Test
    fun `sleep and workout have dedicated screens`() {
        assertEquals(MetricDestination.Sleep, metricDestination(Metric.SLEEP))
        assertEquals(MetricDestination.WorkoutList, metricDestination(Metric.WORKOUT))
    }

    @Test
    fun `other metrics open the detail screen`() {
        assertEquals(MetricDestination.Detail(Metric.STEPS), metricDestination(Metric.STEPS))
        assertEquals(MetricDestination.Detail(Metric.HR), metricDestination(Metric.HR))
        assertEquals(MetricDestination.Detail(Metric.SPO2), metricDestination(Metric.SPO2))
        assertEquals(MetricDestination.Detail(Metric.BODY), metricDestination(Metric.BODY))
    }
}
