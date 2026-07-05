package com.ganathan.skyesabove.ui.screens.weather.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ganathan.skyesabove.ui.theme.CardBackgroundTranslucent
import com.ganathan.skyesabove.ui.theme.CloudGray
import com.ganathan.skyesabove.ui.theme.HumidityBlue
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme
import com.ganathan.skyesabove.ui.theme.SunOrange
import com.ganathan.skyesabove.ui.theme.TwilightPurple
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Data class for weather statistics
 */
data class WeatherStats(
    val humidity: Double, // Percentage 0-100
    val cloudCover: Double, // Percentage 0-100
    val sunsetTime: LocalTime?,
    val sunriseTime: LocalTime?,
    val usableLightStart: LocalTime?, // Civil twilight start
    val usableLightEnd: LocalTime?, // Civil twilight end
    val visibility: Double? = null, // km
    val uvIndex: Double? = null,
    val pressure: Double? = null // hPa
)

/**
 * 2x2 grid showing weather statistics:
 * - Humidity with water droplet icon
 * - Cloud cover with cloud icon
 * - Sunset time with sun icon
 * - Usable light/twilight with lightbulb icon
 */
@Composable
fun WeatherStatsGrid(
    stats: WeatherStats,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(modifier = modifier) {
        Text(
            text = "Weather Details",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First row: Humidity and Cloud Cover
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherStatCard(
                    icon = Icons.Filled.WaterDrop,
                    iconTint = HumidityBlue,
                    label = "Humidity",
                    value = "${stats.humidity.toInt()}%",
                    modifier = Modifier.weight(1f)
                )

                WeatherStatCard(
                    icon = Icons.Filled.Cloud,
                    iconTint = CloudGray,
                    label = "Cloud Cover",
                    value = "${stats.cloudCover.toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row: Sunset and Usable Light
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherStatCard(
                    icon = Icons.Filled.WbTwilight,
                    iconTint = SunOrange,
                    label = "Sunset",
                    value = stats.sunsetTime?.format(timeFormatter) ?: "--:--",
                    modifier = Modifier.weight(1f)
                )

                WeatherStatCard(
                    icon = Icons.Filled.LightMode,
                    iconTint = TwilightPurple,
                    label = "Usable Light",
                    value = stats.usableLightEnd?.format(timeFormatter) ?: "--:--",
                    subtitle = "Civil Twilight",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Extended stats grid with additional weather information
 */
@Composable
fun WeatherStatsGridExtended(
    stats: WeatherStats,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(modifier = modifier) {
        Text(
            text = "Weather Details",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherStatCard(
                    icon = Icons.Filled.WaterDrop,
                    iconTint = HumidityBlue,
                    label = "Humidity",
                    value = "${stats.humidity.toInt()}%",
                    modifier = Modifier.weight(1f)
                )

                WeatherStatCard(
                    icon = Icons.Filled.Cloud,
                    iconTint = CloudGray,
                    label = "Cloud Cover",
                    value = "${stats.cloudCover.toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherStatCard(
                    icon = Icons.Filled.WbSunny,
                    iconTint = SunOrange,
                    label = "Sunrise",
                    value = stats.sunriseTime?.format(timeFormatter) ?: "--:--",
                    modifier = Modifier.weight(1f)
                )

                WeatherStatCard(
                    icon = Icons.Filled.WbTwilight,
                    iconTint = SunOrange,
                    label = "Sunset",
                    value = stats.sunsetTime?.format(timeFormatter) ?: "--:--",
                    modifier = Modifier.weight(1f)
                )
            }

            // Third row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherStatCard(
                    icon = Icons.Filled.LightMode,
                    iconTint = TwilightPurple,
                    label = "First Light",
                    value = stats.usableLightStart?.format(timeFormatter) ?: "--:--",
                    subtitle = "Civil Dawn",
                    modifier = Modifier.weight(1f)
                )

                WeatherStatCard(
                    icon = Icons.Filled.LightMode,
                    iconTint = TwilightPurple,
                    label = "Last Light",
                    value = stats.usableLightEnd?.format(timeFormatter) ?: "--:--",
                    subtitle = "Civil Dusk",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual stat card component
 */
@Composable
fun WeatherStatCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundTranslucent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Vertical stat card variant for different layouts
 */
@Composable
fun WeatherStatCardVertical(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundTranslucent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun WeatherStatsGridPreview() {
    SkyesAboveTheme {
        WeatherStatsGrid(
            stats = WeatherStats(
                humidity = 75.0,
                cloudCover = 40.0,
                sunsetTime = LocalTime.of(17, 45),
                sunriseTime = LocalTime.of(8, 15),
                usableLightStart = LocalTime.of(7, 45),
                usableLightEnd = LocalTime.of(18, 15)
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun WeatherStatCardPreview() {
    SkyesAboveTheme {
        WeatherStatCard(
            icon = Icons.Filled.WaterDrop,
            iconTint = HumidityBlue,
            label = "Humidity",
            value = "75%",
            modifier = Modifier
                .width(180.dp)
                .padding(16.dp)
        )
    }
}
