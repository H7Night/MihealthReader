package com.jhon.mihealthreader.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class StackSegment(val value: Float, val color: Color)

@Composable
fun LineChart(values: List<Float>, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - ((v - minV) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun BarChart(values: List<Float>, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = values.max().takeIf { it > 0f } ?: 1f
        val barWidth = size.width / values.size
        values.forEachIndexed { i, v ->
            val barHeight = (v / maxV) * size.height
            drawRect(
                color = color,
                topLeft = Offset(i * barWidth + barWidth * 0.15f, size.height - barHeight),
                size = Size(barWidth * 0.7f, barHeight),
            )
        }
    }
}

@Composable
fun StackedBarChart(rows: List<List<StackSegment>>, modifier: Modifier) {
    Canvas(modifier) {
        if (rows.isEmpty()) return@Canvas
        val maxTotal = rows.maxOf { row -> row.sumOf { it.value.toDouble() }.toFloat() }
            .takeIf { it > 0f } ?: 1f
        val barWidth = size.width / rows.size
        rows.forEachIndexed { i, row ->
            var bottom = size.height
            row.forEach { segment ->
                val barHeight = (segment.value / maxTotal) * size.height
                if (barHeight > 0f) {
                    drawRect(
                        color = segment.color,
                        topLeft = Offset(i * barWidth + barWidth * 0.15f, bottom - barHeight),
                        size = Size(barWidth * 0.7f, barHeight),
                    )
                    bottom -= barHeight
                }
            }
        }
    }
}

/** Rounded "capsule" bars anchored to the bottom (Google Health style). */
@Composable
fun CapsuleBars(values: List<Float>, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = values.max().takeIf { it > 0f } ?: 1f
        val gap = size.width * 0.035f
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        val radius = barWidth / 2f
        values.forEachIndexed { i, v ->
            val barHeight = ((v / maxV) * size.height).coerceAtLeast(barWidth)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

/** Dots connected by lines (Google Health style). */
@Composable
fun DottedLine(values: List<Float>, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(stepX * i, size.height - ((v - minV) / range) * size.height)
        }
        for (i in 0 until points.size - 1) {
            drawLine(color, points[i], points[i + 1], strokeWidth = 2.dp.toPx())
        }
        points.forEach { drawCircle(color, radius = 3.dp.toPx(), center = it) }
    }
}
