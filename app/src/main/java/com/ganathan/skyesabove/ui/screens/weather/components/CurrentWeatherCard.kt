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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganathan.skyesabove.ui.theme.CardBackgroundTranslucent
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme
import com.ganathan.skyesabove.ui.theme.WindBlue
import com.ganathan.skyesabove.ui.util.FeelsLikeCalculator
import com.ganathan.skyesabove.ui.util.WeatherIconMapper
import com.ganathan.skyesabove.ui.util.WindDirectionUtil

/**
 * Large card showing current weather conditions including:
 * - Temperature with weather icon
 * - Feels-like temperature
 * - Wind speed with direction arrow (rotated based on degrees)
 * - Weather description
 */
@Composable
fun CurrentWeatherCard(
    temperature: Double,
    feelsLikeTemperature: Double,
    weatherSymbol: String?,
    weatherDescription: String?,
    windSpeed: Double,
    windDirection: Double?,
    temperatureUnit: String = "C",
    modifier: Modifier = Modifier
) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(weatherSymbol)
    val compassDirection = WindDirectionUtil.degreesToCompassDirection(windDirection)
    val arrowRotation = WindDirectionUtil.getArrowRotation(windDirection)
    val description = weatherDescription ?: WeatherIconMapper.getWeatherDescription(weatherSymbol)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundTranslucent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Weather Icon
            Icon(
                imageVector = weatherIcon.icon,
                contentDescription = weatherIcon.contentDescription,
                modifier = Modifier.size(80.dp),
                tint = weatherIcon.color
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather Description
            Text(
                text = description,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large Temperature
            Text(
                text = "${temperature.toInt()}°$temperatureUnit",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Light
                ),
                color = Color.White
            )

            // Feels Like
            Text(
                text = "Feels like ${feelsLikeTemperature.toInt()}°$temperatureUnit",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Wind Information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Air,
                    contentDescription = "Wind",
                    modifier = Modifier.size(24.dp),
                    tint = WindBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.1f km/h", windSpeed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Wind Direction Arrow
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = "Wind direction $compassDirection",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation),
                    tint = WindBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = compassDirection,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Compact version of the current weather card for smaller displays
 */
@Composable
fun CurrentWeatherCardCompact(
    temperature: Double,
    feelsLikeTemperature: Double,
    weatherSymbol: String?,
    windSpeed: Double,
    windDirection: Double?,
    temperatureUnit: String = "C",
    modifier: Modifier = Modifier
) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(weatherSymbol)
    val compassDirection = WindDirectionUtil.degreesToCompassDirection(windDirection)
    val arrowRotation = WindDirectionUtil.getArrowRotation(windDirection)

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Weather Icon
            Icon(
                imageVector = weatherIcon.icon,
                contentDescription = weatherIcon.contentDescription,
                modifier = Modifier.size(48.dp),
                tint = weatherIcon.color
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Temperature Section
            Column {
                Text(
                    text = "${temperature.toInt()}°$temperatureUnit",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = "Feels ${feelsLikeTemperature.toInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Wind Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = "Wind direction",
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(arrowRotation),
                    tint = WindBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${windSpeed.toInt()} km/h $compassDirection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun CurrentWeatherCardPreview() {
    SkyesAboveTheme {
        CurrentWeatherCard(
            temperature = 18.5,
            feelsLikeTemperature = 16.2,
            weatherSymbol = "partlycloudy_day",
            weatherDescription = "Partly Cloudy",
            windSpeed = 5.2,
            windDirection = 225.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun CurrentWeatherCardCompactPreview() {
    SkyesAboveTheme {
        CurrentWeatherCardCompact(
            temperature = 18.5,
            feelsLikeTemperature = 16.2,
            weatherSymbol = "rain",
            windSpeed = 5.2,
            windDirection = 180.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
