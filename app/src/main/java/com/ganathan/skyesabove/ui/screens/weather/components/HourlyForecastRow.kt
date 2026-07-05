package com.ganathan.skyesabove.ui.screens.weather.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme
import com.ganathan.skyesabove.ui.util.WeatherIconMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Data class representing a single hour's forecast
 */
data class HourlyForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val symbolCode: String?,
    val precipitationProbability: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null
)

/**
 * Horizontal scrollable row displaying hourly forecast.
 * Shows 24+ hours with time, temperature, and weather icon for each hour.
 */
@Composable
fun HourlyForecastRow(
    hourlyForecasts: List<HourlyForecast>,
    temperatureUnit: String = "C",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Hourly Forecast",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(hourlyForecasts) { forecast ->
                HourlyForecastCard(
                    forecast = forecast,
                    temperatureUnit = temperatureUnit
                )
            }
        }
    }
}

/**
 * Individual card for a single hour's forecast
 */
@Composable
fun HourlyForecastCard(
    forecast: HourlyForecast,
    temperatureUnit: String = "C",
    sunriseHour: Int = 7,
    sunsetHour: Int = 20,
    modifier: Modifier = Modifier
) {
    val hour = forecast.time.hour
    val isNight = hour < sunriseHour || hour >= sunsetHour
    val weatherIcon = WeatherIconMapper.getWeatherIcon(forecast.symbolCode, isNight)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val isNow = forecast.time.hour == LocalDateTime.now().hour &&
                forecast.time.dayOfYear == LocalDateTime.now().dayOfYear

    Card(
        modifier = modifier.width(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNow) {
                Color.White.copy(alpha = 0.3f)
            } else {
                CardBackgroundTranslucent
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time
            Text(
                text = if (isNow) "Now" else forecast.time.format(timeFormatter),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather Icon
            Icon(
                imageVector = weatherIcon.icon,
                contentDescription = weatherIcon.contentDescription,
                modifier = Modifier.size(28.dp),
                tint = weatherIcon.color
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperature
            Text(
                text = "${forecast.temperature.toInt()}°",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )

            // Precipitation probability (if available)
            forecast.precipitationProbability?.let { probability ->
                if (probability > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(probability * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Extended hourly forecast card with more details
 */
@Composable
fun HourlyForecastCardExtended(
    forecast: HourlyForecast,
    windSpeed: Double? = null,
    humidity: Double? = null,
    temperatureUnit: String = "C",
    modifier: Modifier = Modifier
) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(forecast.symbolCode)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val isToday = forecast.time.dayOfYear == LocalDateTime.now().dayOfYear

    Card(
        modifier = modifier.width(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundTranslucent
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day (if not today)
            if (!isToday) {
                Text(
                    text = forecast.time.format(dayFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Time
            Text(
                text = forecast.time.format(timeFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather Icon
            Icon(
                imageVector = weatherIcon.icon,
                contentDescription = weatherIcon.contentDescription,
                modifier = Modifier.size(32.dp),
                tint = weatherIcon.color
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperature
            Text(
                text = "${forecast.temperature.toInt()}°$temperatureUnit",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )

            // Wind Speed
            windSpeed?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${it.toInt()} km/h",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Humidity
            humidity?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${it.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun HourlyForecastRowPreview() {
    val now = LocalDateTime.now()
    val forecasts = (0..24).map { hour ->
        HourlyForecast(
            time = now.plusHours(hour.toLong()),
            temperature = 15.0 + (hour % 8),
            symbolCode = when (hour % 5) {
                0 -> "clearsky_day"
                1 -> "partlycloudy_day"
                2 -> "cloudy"
                3 -> "lightrain"
                else -> "rain"
            },
            precipitationProbability = if (hour % 3 == 0) 0.3 else null
        )
    }

    SkyesAboveTheme {
        HourlyForecastRow(
            hourlyForecasts = forecasts,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun HourlyForecastCardPreview() {
    SkyesAboveTheme {
        HourlyForecastCard(
            forecast = HourlyForecast(
                time = LocalDateTime.now().plusHours(2),
                temperature = 18.5,
                symbolCode = "partlycloudy_day",
                precipitationProbability = 0.25
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
