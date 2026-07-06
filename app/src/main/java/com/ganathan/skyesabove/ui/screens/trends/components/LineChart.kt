package com.ganathan.skyesabove.ui.screens.trends.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganathan.skyesabove.data.model.TrendPoint
import com.ganathan.skyesabove.data.model.TrendRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A lightweight, self-contained line chart for a single garden-sensor series.
 * Renders a gradient area fill, the line, min/max Y labels, a few time X labels, and a
 * draggable scrubber that surfaces the exact value + time at the touched point. No 3rd-party
 * charting dependency — Canvas + TextMeasurer keep it aligned with the app's own aesthetic.
 */
@Composable
fun LineChart(
    points: List<TrendPoint>,
    range: TrendRange,
    unitLabel: String,
    fractionDigits: Int,
    lineColor: Color,
    modifier: Modifier = Modifier,
    axisColor: Color = Color.White.copy(alpha = 0.55f),
    gridColor: Color = Color.White.copy(alpha = 0.12f)
) {
    val textMeasurer = rememberTextMeasurer()
    val zone = remember { ZoneId.systemDefault() }

    // Screen offsets of each plotted point + the plot rect, published by draw for the scrubber.
    var geometry by remember(points) { mutableStateOf<ChartGeometry?>(null) }
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    fun nearestIndex(x: Float): Int? {
        val g = geometry ?: return null
        if (g.offsets.isEmpty()) return null
        return g.offsets.indices.minByOrNull { abs(g.offsets[it].x - x) }
    }

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures { pos -> selected = nearestIndex(pos.x) }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { pos -> selected = nearestIndex(pos.x) },
                    onDrag = { change, _ ->
                        selected = nearestIndex(change.position.x)
                        change.consume()
                    }
                )
            }
    ) {
        if (points.size < 2) {
            drawCenteredText(
                textMeasurer,
                if (points.isEmpty()) "No data yet" else "Not enough data yet",
                center = Offset(size.width / 2f, size.height / 2f),
                color = axisColor,
                sizeSp = 13f
            )
            return@Canvas
        }

        val leftPad = 46f
        val rightPad = 14f
        val topPad = 14f
        val bottomPad = 26f
        val plot = Rect(leftPad, topPad, size.width - rightPad, size.height - bottomPad)

        val minE = points.first().epochSec
        val maxE = points.last().epochSec
        val eSpan = (maxE - minE).coerceAtLeast(1L).toFloat()

        var minV = points.minOf { it.value }
        var maxV = points.maxOf { it.value }
        if (minV == maxV) { minV -= 1.0; maxV += 1.0 }        // avoid a flat/zero range
        val vPad = (maxV - minV) * 0.08
        minV -= vPad; maxV += vPad
        val vSpan = (maxV - minV).toFloat()

        fun sx(epoch: Long) = plot.left + (epoch - minE) / eSpan * plot.width
        fun sy(v: Double) = plot.top + (1f - ((v - minV) / vSpan).toFloat()) * plot.height

        // Horizontal grid + Y labels (min, mid, max).
        val yTicks = listOf(maxV, (maxV + minV) / 2.0, minV)
        for (t in yTicks) {
            val y = sy(t)
            drawLine(gridColor, Offset(plot.left, y), Offset(plot.right, y), strokeWidth = 1f)
            val label = fmt(t, fractionDigits)
            val tl = textMeasurer.measure(label, TextStyle(color = axisColor, fontSize = 10.sp))
            drawText(tl, topLeft = Offset(plot.left - tl.size.width - 6f, y - tl.size.height / 2f))
        }

        // X time labels (start, middle, end).
        val fmtX = xFormatter(range)
        for (frac in listOf(0f, 0.5f, 1f)) {
            val epoch = (minE + (eSpan * frac).toLong())
            val x = plot.left + plot.width * frac
            val label = fmtX.format(Instant.ofEpochSecond(epoch).atZone(zone))
            val tl = textMeasurer.measure(label, TextStyle(color = axisColor, fontSize = 10.sp))
            val tx = (x - tl.size.width / 2f).coerceIn(0f, size.width - tl.size.width)
            drawText(tl, topLeft = Offset(tx, plot.bottom + 6f))
        }

        val offsets = points.map { Offset(sx(it.epochSec), sy(it.value)) }

        // Gradient area fill under the line.
        val fill = Path().apply {
            moveTo(offsets.first().x, plot.bottom)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, plot.bottom)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.32f), lineColor.copy(alpha = 0.02f)),
                startY = plot.top, endY = plot.bottom
            )
        )

        // The line.
        val line = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            for (i in 1 until offsets.size) lineTo(offsets[i].x, offsets[i].y)
        }
        drawPath(line, color = lineColor, style = Stroke(width = 2.5f))

        geometry = ChartGeometry(offsets, plot)

        // Scrubber.
        selected?.let { idx ->
            if (idx in offsets.indices) {
                val o = offsets[idx]
                drawLine(
                    axisColor, Offset(o.x, plot.top), Offset(o.x, plot.bottom),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                drawCircle(Color.White, radius = 4.5f, center = o)
                drawCircle(lineColor, radius = 2.5f, center = o)

                val valueTxt = "${fmt(points[idx].value, fractionDigits)}$unitLabel"
                val timeTxt = scrubTimeFormatter(range).format(
                    Instant.ofEpochSecond(points[idx].epochSec).atZone(zone)
                )
                drawScrubBubble(textMeasurer, o, plot, valueTxt, timeTxt, lineColor)
            }
        }
    }
}

private data class ChartGeometry(val offsets: List<Offset>, val plot: Rect)

private fun fmt(v: Double, digits: Int): String =
    if (digits <= 0) v.roundToInt().toString() else "%.${digits}f".format(v)

private fun xFormatter(range: TrendRange): DateTimeFormatter = when (range) {
    TrendRange.HOUR, TrendRange.DAY -> DateTimeFormatter.ofPattern("HH:mm")
    TrendRange.WEEK -> DateTimeFormatter.ofPattern("EEE")
    TrendRange.MONTH -> DateTimeFormatter.ofPattern("d MMM")
    TrendRange.YEAR -> DateTimeFormatter.ofPattern("MMM")
}

private fun scrubTimeFormatter(range: TrendRange): DateTimeFormatter = when (range) {
    TrendRange.HOUR, TrendRange.DAY -> DateTimeFormatter.ofPattern("HH:mm")
    TrendRange.WEEK -> DateTimeFormatter.ofPattern("EEE HH:mm")
    else -> DateTimeFormatter.ofPattern("d MMM")
}

private fun DrawScope.drawCenteredText(
    tm: TextMeasurer, text: String, center: Offset, color: Color, sizeSp: Float
) {
    val tl = tm.measure(text, TextStyle(color = color, fontSize = sizeSp.sp))
    drawText(tl, topLeft = Offset(center.x - tl.size.width / 2f, center.y - tl.size.height / 2f))
}

private fun DrawScope.drawScrubBubble(
    tm: TextMeasurer, at: Offset, plot: Rect, value: String, time: String, accent: Color
) {
    val valLayout = tm.measure(value, TextStyle(color = Color.White, fontSize = 13.sp))
    val timeLayout = tm.measure(time, TextStyle(color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp))
    val padH = 10f; val padV = 6f
    val w = maxOf(valLayout.size.width, timeLayout.size.width) + padH * 2
    val h = valLayout.size.height + timeLayout.size.height + padV * 2 + 2f
    var left = at.x - w / 2f
    left = left.coerceIn(plot.left, plot.right - w)
    val top = plot.top + 2f
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawText(valLayout, topLeft = Offset(left + padH, top + padV))
    drawText(timeLayout, topLeft = Offset(left + padH, top + padV + valLayout.size.height + 2f))
}
