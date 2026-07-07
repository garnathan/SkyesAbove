package com.ganathan.skyesabove.ui.screens.trends.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganathan.skyesabove.data.model.PressureScale
import com.ganathan.skyesabove.data.model.PressureTendency
import kotlin.math.roundToInt

/**
 * Low→high barometric scale for a pressure reading: a gradient track (blue=low/unsettled →
 * warm=high/settled) with a marker at the current value, the plain-language band descriptor,
 * and the 3-hour tendency arrow. Turns a bare "1012 mb" into something meaningful at a glance.
 */
@Composable
fun PressureGauge(
    pressureMbar: Double,
    tendency: PressureTendency?,
    modifier: Modifier = Modifier
) {
    val pos = PressureScale.position(pressureMbar)
    val descriptor = PressureScale.descriptor(pressureMbar)

    Column(modifier = modifier.fillMaxWidth()) {
        // Descriptor + tendency line
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                descriptor,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (tendency != null && tendency != PressureTendency.STEADY) {
                val tint = if (tendency.rising == true) Color(0xFF81C784) else Color(0xFFFF8A65)
                Text(
                    "   ${tendency.arrow} ${tendency.label}",
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("  · past 3h", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            } else if (tendency == PressureTendency.STEADY) {
                Text("   → steady", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                Text("  · past 3h", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }

        // Gradient track + marker
        Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
            val trackH = 9.dp.toPx()
            val trackY = size.height - trackH
            val radius = CornerRadius(trackH / 2f, trackH / 2f)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF4FC3F7), Color(0xFF81C784), Color(0xFFFFB74D))
                ),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackH),
                cornerRadius = radius,
                alpha = 0.85f
            )
            // Marker: downward triangle sitting on top of the track.
            val mx = (pos * size.width).coerceIn(6f, size.width - 6f)
            val tri = Path().apply {
                moveTo(mx - 6f, trackY - 9f)
                lineTo(mx + 6f, trackY - 9f)
                lineTo(mx, trackY + 1f)
                close()
            }
            drawPath(tri, color = Color.White)
        }

        // Scale end labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("Low · ${PressureScale.MIN.roundToInt()}", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
            Text("avg 1013", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
            Text("${PressureScale.MAX.roundToInt()} · High", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
        }
    }
}
