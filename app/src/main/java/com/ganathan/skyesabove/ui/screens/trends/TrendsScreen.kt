package com.ganathan.skyesabove.ui.screens.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganathan.skyesabove.data.model.TrendMetric
import com.ganathan.skyesabove.data.model.TrendRange
import com.ganathan.skyesabove.data.model.TrendStats
import com.ganathan.skyesabove.ui.screens.trends.components.LineChart
import com.ganathan.skyesabove.ui.screens.trends.components.PressureGauge
import com.ganathan.skyesabove.ui.theme.skyesAboveGradient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Accent colour per metric — keeps each series visually distinct and on-brand. */
private fun accentFor(metric: TrendMetric): Color = when (metric) {
    TrendMetric.TEMPERATURE -> Color(0xFFFFB74D) // amber
    TrendMetric.FEELS -> Color(0xFFFF8A65)       // deep orange
    TrendMetric.HUMIDITY -> Color(0xFF4FC3F7)    // light blue
    TrendMetric.PRESSURE -> Color(0xFF81C784)    // green
    TrendMetric.DEW_POINT -> Color(0xFF4DD0E1)   // cyan
}

/**
 * Garden-sensor historical trends (the HOME station, IKILLI35). Pick a metric and a time
 * range; data comes from the Pi's GitHub-published CSV history.
 */
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val accent = accentFor(uiState.metric)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = skyesAboveGradient())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Garden Trends",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Home station · IKILLI35" + (lastUpdatedSuffix(uiState.series?.lastUpdatedEpochSec)),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }

            // Metric chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendMetric.entries.forEach { m ->
                    SelectChip(
                        label = "${m.emoji} ${m.label}",
                        selected = m == uiState.metric,
                        accent = accentFor(m),
                        onClick = { viewModel.selectMetric(m) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Range chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendRange.entries.forEach { r ->
                    SelectChip(
                        label = r.label,
                        selected = r == uiState.range,
                        accent = accent,
                        onClick = { viewModel.selectRange(r) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Current + stats
            StatsHeader(
                metric = uiState.metric,
                unitLabel = uiState.series?.unitLabel ?: "",
                stats = uiState.series?.stats,
                accent = accent
            )

            // Pressure: low→high scale + 3-hour tendency (turns the bare number into meaning).
            val pStats = uiState.series?.stats
            if (uiState.metric == TrendMetric.PRESSURE && pStats != null) {
                Spacer(Modifier.height(14.dp))
                PressureGauge(
                    pressureMbar = pStats.current,
                    tendency = uiState.series?.tendency
                )
            }

            Spacer(Modifier.height(12.dp))

            // Chart card
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    when {
                        uiState.isLoading && uiState.series == null ->
                            CircularProgressIndicator(color = accent)
                        uiState.error != null ->
                            Text(
                                uiState.error ?: "Error",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        else -> {
                            val series = uiState.series
                            LineChart(
                                points = series?.points ?: emptyList(),
                                range = uiState.range,
                                unitLabel = series?.unitLabel ?: "",
                                fractionDigits = uiState.metric.fractionDigits,
                                lineColor = accent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun StatsHeader(
    metric: TrendMetric,
    unitLabel: String,
    stats: TrendStats?,
    accent: Color
) {
    val digits = metric.fractionDigits
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                metric.label.uppercase(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (stats != null) "${fmt(stats.current, digits)}$unitLabel" else "—",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (stats != null) {
            Column(horizontalAlignment = Alignment.End) {
                StatLine("High", "${fmt(stats.max, digits)}$unitLabel", accent)
                StatLine("Avg", "${fmt(stats.average, digits)}$unitLabel", Color.White.copy(alpha = 0.85f))
                StatLine("Low", "${fmt(stats.min, digits)}$unitLabel", Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label ",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp
        )
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            labelColor = Color.White.copy(alpha = 0.85f),
            selectedContainerColor = accent.copy(alpha = 0.30f),
            selectedLabelColor = Color.White
        )
    )
}

private fun fmt(v: Double, digits: Int): String =
    if (digits <= 0) v.roundToInt().toString() else "%.${digits}f".format(v)

private fun lastUpdatedSuffix(epochSec: Long?): String {
    if (epochSec == null) return ""
    val t = Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault())
    return " · as of ${DateTimeFormatter.ofPattern("HH:mm").format(t)}"
}
