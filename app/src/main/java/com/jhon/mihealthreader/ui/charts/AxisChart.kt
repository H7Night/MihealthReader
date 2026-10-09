package com.jhon.mihealthreader.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AXIS_WIDTH = 44.dp
private val CHART_HEIGHT = 180.dp

/** Line/bar chart with y-axis value labels and x-axis date labels. */
@Composable
fun ChartWithAxes(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color,
    bars: Boolean,
) {
    val maxV = values.maxOrNull() ?: 0f
    val minV = values.minOrNull() ?: 0f

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(AXIS_WIDTH).height(CHART_HEIGHT),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatAxis(maxV), style = MaterialTheme.typography.labelSmall)
                Text(formatAxis(minV), style = MaterialTheme.typography.labelSmall)
            }
            if (bars) {
                RoundedBarChart(values, Modifier.fillMaxWidth().height(CHART_HEIGHT), color)
            } else {
                AreaLineChart(values, Modifier.fillMaxWidth().height(CHART_HEIGHT), color)
            }
        }
        AxisLabels(labels)
    }
}

/** Stacked bar chart (sleep stages) with a total-minutes y-axis. */
@Composable
fun StackedChartWithAxes(
    rows: List<List<StackSegment>>,
    totals: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val maxV = totals.maxOrNull() ?: 0f
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(AXIS_WIDTH).height(CHART_HEIGHT),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatAxis(maxV), style = MaterialTheme.typography.labelSmall)
                Text("0", style = MaterialTheme.typography.labelSmall)
            }
            StackedBarChart(rows, Modifier.fillMaxWidth().height(CHART_HEIGHT))
        }
        AxisLabels(labels)
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = AXIS_WIDTH),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        if (labels.size > 2) {
            labels.getOrNull(labels.size / 2)?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        if (labels.size > 1) {
            labels.lastOrNull()?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private fun formatAxis(value: Float): String =
    if (value >= 100f) value.toInt().toString() else "%.1f".format(value)
