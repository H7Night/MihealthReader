package com.jhon.mihealthreader.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

enum class GhChartStyle { Bars, DottedLine, AreaLine }

data class GhAxis(val min: Float, val max: Float, val ticks: List<Float>)

data class GhBarLayout(val barWidth: Float, val gap: Float)

fun ghBarLayout(count: Int, width: Float): GhBarLayout {
    if (count <= 0 || width <= 0f) return GhBarLayout(0f, 0f)
    if (count == 1) return GhBarLayout(width, 0f)
    val slot = width / count
    val gap = minOf(width * 0.035f, slot * 0.35f)
    val barWidth = ((width - gap * (count - 1)) / count).coerceAtLeast(0.5f)
    return GhBarLayout(barWidth, gap)
}

fun ghAxis(values: List<Float>, fromZero: Boolean): GhAxis {
    if (values.isEmpty()) return GhAxis(0f, 1f, listOf(1f, 0.5f, 0f))
    val max = values.max().coerceAtLeast(1f)
    val rawMin = values.min()
    if (!fromZero && rawMin == max) {
        val pad = if (max == 0f) 1f else max * 0.1f
        return GhAxis(max - pad, max + pad, listOf(max + pad, max, max - pad))
    }
    val min = if (fromZero) 0f else rawMin.coerceAtMost(max)
    val safeMax = if (max == min) min + 1f else max
    return GhAxis(min, safeMax, listOf(safeMax, (safeMax + min) / 2f, min))
}

fun ghLabelIndices(size: Int, maxLabels: Int = 7): List<Int> {
    if (size <= 0) return emptyList()
    if (size <= maxLabels) return (0 until size).toList()
    val step = (size - 1).toFloat() / (maxLabels - 1)
    return (0 until maxLabels).map { (it * step).roundToInt() }.distinct()
}

@Composable
fun GhChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    style: GhChartStyle,
    color: Color = MaterialTheme.colorScheme.secondary,
    selectedIndex: Int? = null,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
) {
    val axis = remember(values, style) { ghAxis(values, fromZero = style == GhChartStyle.Bars) }
    val animated by animateFloatAsState(if (values.isEmpty()) 0f else 1f, label = "chart")
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(180.dp)) {
                when (style) {
                    GhChartStyle.Bars -> BarsCanvas(values, axis, color, selectedIndex, surface, animated)
                    GhChartStyle.DottedLine -> DottedCanvas(values, axis, color, selectedIndex, surface, animated)
                    GhChartStyle.AreaLine -> AreaCanvas(values, axis, color, animated)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                axis.ticks.forEach {
                    Text(
                        valueFormatter(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(end = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ghLabelIndices(labels.size).forEach { i ->
                Text(
                    labels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BarsCanvas(
    values: List<Float>,
    axis: GhAxis,
    color: Color,
    selected: Int?,
    surface: Color,
    progress: Float,
) {
    Canvas(Modifier.fillMaxSize()) {
        if (values.isEmpty()) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            return@Canvas
        }
        val range = (axis.max - axis.min).takeIf { it > 0f } ?: 1f
        val layout = ghBarLayout(values.size, size.width)
        val gap = layout.gap
        val barWidth = layout.barWidth
        values.forEachIndexed { i, v ->
            val h = (((v - axis.min) / range).coerceIn(0f, 1f)) * size.height * progress
            val radius = min(barWidth / 2f, h / 2f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
        selected?.let { i ->
            if (i in values.indices) {
                val x = i * (barWidth + gap) + barWidth / 2f
                val h = (((values[i] - axis.min) / range).coerceIn(0f, 1f)) * size.height * progress
                drawLine(surface, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                drawCircle(surface, 5.dp.toPx(), Offset(x, size.height - h))
                drawCircle(color, 3.dp.toPx(), Offset(x, size.height - h))
            }
        }
    }
}

@Composable
private fun DottedCanvas(
    values: List<Float>,
    axis: GhAxis,
    color: Color,
    selected: Int?,
    surface: Color,
    progress: Float,
) {
    Canvas(Modifier.fillMaxSize()) {
        if (values.isEmpty()) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            return@Canvas
        }
        val range = (axis.max - axis.min).takeIf { it > 0f } ?: 1f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val points = values.mapIndexed { i, v ->
            val y = size.height - ((v - axis.min) / range) * size.height * progress
            Offset(stepX * i, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(color, points[i], points[i + 1], 2.dp.toPx())
        }
        points.forEach { drawCircle(color, 3.5.dp.toPx(), it) }
        selected?.let { i ->
            points.getOrNull(i)?.let {
                drawCircle(surface, 6.dp.toPx(), it)
                drawCircle(color, 4.dp.toPx(), it)
            }
        }
    }
}

@Composable
private fun AreaCanvas(values: List<Float>, axis: GhAxis, color: Color, progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        if (values.size < 2) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            return@Canvas
        }
        val range = (axis.max - axis.min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(stepX * i, size.height - ((v - axis.min) / range) * size.height * progress)
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                val midX = (prev.x + cur.x) / 2f
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f))))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun GhStackedBars(
    rows: List<List<StackSegment>>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val maxTotal = remember(rows) {
        rows.maxOfOrNull { row -> row.sumOf { it.value.toDouble() }.toFloat() } ?: 1f
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(160.dp)) {
                StackedBarChart(rows, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(maxTotal.toInt().toString(), style = MaterialTheme.typography.labelSmall)
                Text("0", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(end = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ghLabelIndices(labels.size).forEach { i ->
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

