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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ganathan.skyesabove.ui.theme.CardBackgroundTranslucent
import com.ganathan.skyesabove.ui.theme.RainBlue
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme
import com.ganathan.skyesabove.ui.util.WeatherIconMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Data class representing a single day's forecast
 */
data class DailyForecast(
    val date: LocalDate,
    val highTemp: Double,
    val lowTemp: Double,
    val symbolCode: String?,
    val precipitationProbability: Double? = null,
    val precipitationAmount: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null
)

/**
 * Grid/Column of 7 day forecast cards showing:
 * - Day name
 * - Weather icon
 * - High/low temperatures
 */
@Composable
fun WeeklyForecastGrid(
    dailyForecasts: List<DailyForecast>,
    temperatureUnit: String = "C",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "7-Day Forecast",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackgroundTranslucent
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                dailyForecasts.take(7).forEachIndexed { index, forecast ->
                    DailyForecastRow(
                        forecast = forecast,
                        temperatureUnit = temperatureUnit,
                        isToday = forecast.date == LocalDate.now()
                    )
                    if (index < dailyForecasts.size - 1 && index < 6) {
                        // Divider between rows (optional, subtle)
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single row in the weekly forecast showing one day's weather
 */
@Composable
fun DailyForecastRow(
    forecast: DailyForecast,
    temperatureUnit: String = "C",
    isToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(forecast.symbolCode)
    val dayName = if (isToday) {
        "Today"
    } else if (forecast.date == LocalDate.now().plusDays(1)) {
        "Tomorrow"
    } else {
        forecast.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color = Color.White,
            modifier = Modifier.width(80.dp)
        )

        // Weather Icon
        Icon(
            imageVector = weatherIcon.icon,
            contentDescription = weatherIcon.contentDescription,
            modifier = Modifier.size(28.dp),
            tint = weatherIcon.color
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Precipitation probability (if available)
        forecast.precipitationProbability?.let { prob ->
            if (prob > 0) {
                Text(
                    text = "${(prob * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = RainBlue,
                    modifier = Modifier.width(40.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        } ?: Spacer(modifier = Modifier.width(40.dp))

        Spacer(modifier = Modifier.weight(1f))

        // High and Low temperatures
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High temp
            Text(
                text = "${forecast.highTemp.toInt()}°",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                modifier = Modifier.width(40.dp)
            )

            // Low temp
            Text(
                text = "${forecast.lowTemp.toInt()}°",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

/**
 * Alternative card-based layout for weekly forecast
 */
@Composable
fun WeeklyForecastCards(
    dailyForecasts: List<DailyForecast>,
    temperatureUnit: String = "C",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "7-Day Forecast",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        dailyForecasts.take(7).forEach { forecast ->
            DailyForecastCard(
                forecast = forecast,
                temperatureUnit = temperatureUnit,
                isToday = forecast.date == LocalDate.now(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Individual card for a day's forecast
 */
@Composable
fun DailyForecastCard(
    forecast: DailyForecast,
    temperatureUnit: String = "C",
    isToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(forecast.symbolCode)
    val dayName = if (isToday) {
        "Today"
    } else if (forecast.date == LocalDate.now().plusDays(1)) {
        "Tomorrow"
    } else {
        forecast.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    val dateString = forecast.date.format(DateTimeFormatter.ofPattern("MMM d"))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                Color.White.copy(alpha = 0.25f)
            } else {
                CardBackgroundTranslucent
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day and Date
            Column(modifier = Modifier.width(100.dp)) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Weather Icon
            Icon(
                imageVector = weatherIcon.icon,
                contentDescription = weatherIcon.contentDescription,
                modifier = Modifier.size(36.dp),
                tint = weatherIcon.color
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Weather description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = WeatherIconMapper.getWeatherDescription(forecast.symbolCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                forecast.precipitationAmount?.let { amount ->
                    if (amount > 0) {
                        Text(
                            text = "${String.format("%.1f", amount)} mm",
                            style = MaterialTheme.typography.bodySmall,
                            color = RainBlue
                        )
                    }
                }
            }

            // Temperatures
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${forecast.highTemp.toInt()}°$temperatureUnit",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "${forecast.lowTemp.toInt()}°$temperatureUnit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun WeeklyForecastGridPreview() {
    val today = LocalDate.now()
    val forecasts = (0..6).map { day ->
        DailyForecast(
            date = today.plusDays(day.toLong()),
            highTemp = 18.0 + day,
            lowTemp = 10.0 + day,
            symbolCode = when (day % 4) {
                0 -> "clearsky_day"
                1 -> "partlycloudy_day"
                2 -> "rain"
                else -> "cloudy"
            },
            precipitationProbability = if (day % 2 == 0) 0.3 else null
        )
    }

    SkyesAboveTheme {
        WeeklyForecastGrid(
            dailyForecasts = forecasts,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun DailyForecastCardPreview() {
    SkyesAboveTheme {
        DailyForecastCard(
            forecast = DailyForecast(
                date = LocalDate.now(),
                highTemp = 22.0,
                lowTemp = 14.0,
                symbolCode = "partlycloudy_day",
                precipitationAmount = 2.5
            ),
            isToday = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
