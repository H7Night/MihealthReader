package com.jhon.mihealthreader.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp

data class HypnoSegment(val start: Long, val end: Long, val stateName: String)

/**
 * Horizontal sleep-stage timeline (hypnogram): contiguous filled columns whose height
 * encodes the stage (deep short, light medium, awake tall), Xiaomi-style.
 */
@Composable
fun Hypnogram(
    segments: List<HypnoSegment>,
    modifier: Modifier,
    deepColor: Color,
    lightColor: Color,
    awakeColor: Color,
    napColor: Color,
    gridColor: Color,
) {
    Canvas(modifier) {
        if (segments.isEmpty()) return@Canvas
        val start = segments.minOf { it.start }
        val end = segments.maxOf { it.end }
        val span = (end - start).coerceAtLeast(1L)

        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
        for (i in 1..2) {
            val y = size.height * i / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash,
            )
        }

        segments.forEach { segment ->
            val x0 = ((segment.start - start).toFloat() / span) * size.width
            val x1 = ((segment.end - start).toFloat() / span) * size.width
            val level = when (segment.stateName) {
                "deep" -> 0.35f
                "light" -> 0.6f
                "awake" -> 1f
                "nap" -> 0.6f
                else -> 0.45f
            }
            val color = when (segment.stateName) {
                "deep" -> deepColor
                "light" -> lightColor
                "awake" -> awakeColor
                "nap" -> napColor
                else -> lightColor
            }
            val barHeight = level * size.height
            drawRect(
                color = color,
                topLeft = Offset(x0, size.height - barHeight),
                size = Size((x1 - x0).coerceAtLeast(0.5f), barHeight),
            )
        }
    }
}
