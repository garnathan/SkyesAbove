package com.ganathan.skyesabove.ui.screens.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganathan.skyesabove.widget.DiagEvent
import com.ganathan.skyesabove.widget.WidgetDiagnostics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Per-half status colours (mirror the widget's own green/amber, plus red for unreachable).
private val OkColor = Color(0xFF66BB6A)
private val StaleColor = Color(0xFFFFB74D)
private val ErrorColor = Color(0xFFE57373)

private fun statusColor(status: String): Color = when (status) {
    "OK" -> OkColor
    "STALE" -> StaleColor
    else -> ErrorColor
}

/**
 * On-device widget refresh log. Shows per-half success rates and a breakdown of the
 * "both halves blank" cases (by network and by which code path ran), then the raw event
 * list — all readable without adb/logcat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFmt = remember { SimpleDateFormat("MMM d  HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget diagnostics", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload log")
                    }
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear log")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.width(0.dp)) }

            uiState.summary24h?.let { s ->
                item { SummaryCard(title = "Last 24 hours", summary = s) }
            }
            uiState.summary7d?.let { s ->
                item { SummaryCard(title = "Last 7 days", summary = s) }
            }

            item {
                RefreshNowRow(refreshing = uiState.refreshing, onClick = viewModel::refreshWidgetNow)
            }

            item {
                Text(
                    text = "Recent refreshes (newest first)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.events.isEmpty()) {
                item {
                    Text(
                        text = "No refreshes recorded yet. Tap \"Refresh widget now\" or wait for " +
                            "the next background refresh, then reload.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.events) { event ->
                    EventRow(event = event, timeFmt = timeFmt)
                }
            }

            item { Spacer(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(title: String, summary: WidgetDiagnostics.Summary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            if (summary.total == 0) {
                Text(
                    "No refreshes in this window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                "${summary.total} refreshes recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RatePill(label = "Forecast live", pct = summary.forecastPct(), n = summary.forecastOk, total = summary.total)
            RatePill(label = "Home live", pct = summary.homePct(), n = summary.homeOk, total = summary.total)

            if (summary.bothBlank > 0) {
                Spacer(Modifier.size(2.dp))
                Text(
                    "Both halves blank: ${summary.bothBlank} time(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorColor,
                    fontWeight = FontWeight.SemiBold
                )
                val byNet = summary.bothBlankByNet.entries.joinToString("  ") { "${it.key}:${it.value}" }
                val byTrig = summary.bothBlankByTrigger.entries.joinToString("  ") { "${it.key}:${it.value}" }
                Text(
                    "by network:  $byNet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "by trigger:  $byTrig",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RatePill(label: String, pct: Int, n: Int, total: Int) {
    val color = when {
        pct >= 90 -> OkColor
        pct >= 70 -> StaleColor
        else -> ErrorColor
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$pct%",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(56.dp)
        )
        Text(
            "$label  ($n/$total)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RefreshNowRow(refreshing: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = !refreshing,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (refreshing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Refreshing…")
        } else {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Refresh widget now")
        }
    }
}

@Composable
private fun EventRow(event: DiagEvent, timeFmt: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeFmt.format(Date(event.timeMs)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${event.trigger} · ${event.net}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(label = "FC", status = event.forecast)
                StatusChip(label = "HOME", status = event.home)
                if (event.homeObsAgeSec >= 0) {
                    Text(
                        "obs ${formatAge(event.homeObsAgeSec)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.durationMs >= 0) {
                    Text(
                        "${event.durationMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!event.error.isNullOrBlank()) {
                Text(
                    event.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, status: String) {
    val color = statusColor(status)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).padding(0.dp)) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawCircle(color)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "$label $status",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatAge(sec: Long): String = when {
    sec < 60 -> "${sec}s"
    sec < 3600 -> "${sec / 60}m"
    else -> "${sec / 3600}h${(sec % 3600) / 60}m"
}
