package com.ganathan.skyesabove.ui.screens.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganathan.skyesabove.ui.screens.weather.components.CurrentWeatherCard
import com.ganathan.skyesabove.ui.screens.weather.components.DailyForecast
import com.ganathan.skyesabove.ui.screens.weather.components.HourlyForecast
import com.ganathan.skyesabove.ui.screens.weather.components.HourlyForecastCard
import com.ganathan.skyesabove.ui.screens.weather.components.WeatherStats
import com.ganathan.skyesabove.ui.screens.weather.components.WeatherStatsGrid
import com.ganathan.skyesabove.ui.theme.CardBackgroundTranslucent
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme
import com.ganathan.skyesabove.ui.theme.skyesAboveGradient
import com.ganathan.skyesabove.ui.util.WeatherIconMapper
import com.ganathan.skyesabove.ui.util.WindDirectionUtil
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Weather tab options for navigation
 */
enum class WeatherTab(val title: String) {
    NOW("Now"),
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    SEVEN_DAY("7 Day")
}

/**
 * Main weather screen composable with tabbed navigation.
 * Tabs: Now, Today, Tomorrow, 7-Day
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, actionLabel = "Retry")
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = skyesAboveGradient())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            LocationHeader(
                locationName = uiState.locationName,
                lastUpdated = uiState.lastUpdated,
                onSettingsClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            WeatherTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            when {
                uiState.isLoading && !uiState.isRefreshing -> LoadingState(Modifier.fillMaxSize())
                uiState.error != null && uiState.hourlyForecast.isEmpty() -> ErrorState(
                    message = uiState.error ?: "An error occurred",
                    onRetry = { viewModel.fetchWeatherData() },
                    modifier = Modifier.fillMaxSize()
                )
                else -> when (WeatherTab.entries[selectedTab]) {
                    WeatherTab.NOW -> NowTabContent(uiState)
                    WeatherTab.TODAY -> TodayTabContent(uiState)
                    WeatherTab.TOMORROW -> TomorrowTabContent(uiState)
                    WeatherTab.SEVEN_DAY -> SevenDayTabContent(uiState)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                actionColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WeatherTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    TabRow(
        selectedTabIndex = selectedTab,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = Color.White
            )
        }
    ) {
        WeatherTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = tab.title,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                }
            )
        }
    }
}

@Composable
private fun NowTabContent(uiState: WeatherUiState) {
    val scrollState = rememberScrollState()
    val now = LocalDateTime.now()
    val next60MinForecast = uiState.hourlyForecast.filter {
        it.time.isAfter(now.minusMinutes(30)) && it.time.isBefore(now.plusMinutes(90))
    }.take(3)
    val sunriseHour = uiState.sunTimes.sunrise?.hour ?: 7
    val sunsetHour = uiState.sunTimes.sunset?.hour ?: 20

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        CurrentWeatherCard(
            temperature = uiState.currentWeather.temperature,
            feelsLikeTemperature = uiState.currentWeather.feelsLikeTemperature,
            weatherSymbol = uiState.currentWeather.weatherSymbol,
            weatherDescription = uiState.currentWeather.weatherDescription,
            windSpeed = uiState.currentWeather.windSpeed,
            windDirection = uiState.currentWeather.windDirection,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        WeatherStatsGrid(
            stats = WeatherStats(
                humidity = uiState.currentWeather.humidity,
                cloudCover = uiState.currentWeather.cloudCover,
                sunsetTime = uiState.sunTimes.sunset,
                sunriseTime = uiState.sunTimes.sunrise,
                usableLightStart = uiState.sunTimes.civilTwilightStart,
                usableLightEnd = uiState.sunTimes.civilTwilightEnd,
                visibility = uiState.currentWeather.visibility,
                uvIndex = uiState.currentWeather.uvIndex,
                pressure = uiState.currentWeather.pressure
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (next60MinForecast.isNotEmpty()) {
            Text(
                text = "Next 60 Minutes",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(next60MinForecast) { HourlyForecastCard(forecast = it, sunriseHour = sunriseHour, sunsetHour = sunsetHour) }
            }
        }
    }
}

@Composable
private fun TodayTabContent(uiState: WeatherUiState) {
    val scrollState = rememberScrollState()
    val now = LocalDateTime.now()
    val endOfDay = now.toLocalDate().atTime(23, 59)
    val todayForecast = uiState.hourlyForecast.filter {
        it.time.isAfter(now.minusMinutes(30)) && it.time.isBefore(endOfDay)
    }
    val sunriseHour = uiState.sunTimes.sunrise?.hour ?: 7
    val sunsetHour = uiState.sunTimes.sunset?.hour ?: 20

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        TodaySummaryCard(uiState, todayForecast)
        Spacer(modifier = Modifier.height(24.dp))

        if (todayForecast.isNotEmpty()) {
            Text(
                text = "Hour by Hour",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            todayForecast.forEach { forecast ->
                HourlyDetailCard(forecast, sunriseHour, sunsetHour)
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(
                text = "No more forecast data for today",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TomorrowTabContent(uiState: WeatherUiState) {
    val scrollState = rememberScrollState()
    val tomorrow = LocalDate.now().plusDays(1)
    val tomorrowForecast = uiState.hourlyForecast.filter { it.time.toLocalDate() == tomorrow }
    val tomorrowSummary = uiState.dailyForecast.find { it.date == tomorrow }
    val sunriseHour = uiState.sunTimes.sunrise?.hour ?: 7
    val sunsetHour = uiState.sunTimes.sunset?.hour ?: 20

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        tomorrowSummary?.let {
            DailySummaryCard(title = "Tomorrow", dailyForecast = it)
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (tomorrowForecast.isNotEmpty()) {
            Text(
                text = "Hour by Hour",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            tomorrowForecast.forEach { forecast ->
                HourlyDetailCard(forecast, sunriseHour, sunsetHour)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SevenDayTabContent(uiState: WeatherUiState) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.dailyForecast.isNotEmpty()) {
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            uiState.dailyForecast.forEach { dailyForecast ->
                DailyDetailCard(dailyForecast)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(uiState: WeatherUiState, todayForecast: List<HourlyForecast>) {
    val temps = todayForecast.map { it.temperature }
    val highTemp = temps.maxOrNull() ?: uiState.currentWeather.temperature
    val lowTemp = temps.minOrNull() ?: uiState.currentWeather.temperature

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Today", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "High", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "${highTemp.toInt()}°", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Low", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "${lowTemp.toInt()}°", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Now", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "${uiState.currentWeather.temperature.toInt()}°", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(title: String, dailyForecast: DailyForecast) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(dailyForecast.symbolCode)
    val compassDirection = WindDirectionUtil.degreesToCompassDirection(dailyForecast.windDirection)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Icon(imageVector = weatherIcon.icon, contentDescription = weatherIcon.contentDescription, modifier = Modifier.size(64.dp), tint = weatherIcon.color)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "High", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "${dailyForecast.highTemp.toInt()}°", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Low", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "${dailyForecast.lowTemp.toInt()}°", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            dailyForecast.windSpeed?.let { speed ->
                Text(
                    text = "Wind: ${speed.toInt()} km/h $compassDirection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            dailyForecast.precipitationProbability?.let { prob ->
                if (prob > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${(prob * 100).toInt()}% chance of rain", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun HourlyDetailCard(forecast: HourlyForecast, sunriseHour: Int = 7, sunsetHour: Int = 20) {
    val hour = forecast.time.hour
    val isNight = hour < sunriseHour || hour >= sunsetHour
    val weatherIcon = WeatherIconMapper.getWeatherIcon(forecast.symbolCode, isNight)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val compassDirection = WindDirectionUtil.degreesToCompassDirection(forecast.windDirection)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = forecast.time.format(timeFormatter), style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.width(60.dp))
            Icon(imageVector = weatherIcon.icon, contentDescription = weatherIcon.contentDescription, modifier = Modifier.size(32.dp), tint = weatherIcon.color)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${forecast.temperature.toInt()}°", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                forecast.windSpeed?.let { speed ->
                    val arrowRotation = WindDirectionUtil.getArrowRotation(forecast.windDirection)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Wind direction $compassDirection",
                            modifier = Modifier.size(14.dp).rotate(arrowRotation),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${speed.toInt()} km/h $compassDirection", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
            forecast.precipitationProbability?.let { prob ->
                if (prob > 0) Text(text = "${(prob * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun DailyDetailCard(dailyForecast: DailyForecast) {
    val weatherIcon = WeatherIconMapper.getWeatherIcon(dailyForecast.symbolCode)
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val isToday = dailyForecast.date == LocalDate.now()
    val isTomorrow = dailyForecast.date == LocalDate.now().plusDays(1)
    val dayName = when { isToday -> "Today"; isTomorrow -> "Tomorrow"; else -> dailyForecast.date.format(dayFormatter) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dayName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = dailyForecast.date.format(dateFormatter), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            Icon(imageVector = weatherIcon.icon, contentDescription = weatherIcon.contentDescription, modifier = Modifier.size(40.dp), tint = weatherIcon.color)
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${dailyForecast.highTemp.toInt()}° / ${dailyForecast.lowTemp.toInt()}°", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                dailyForecast.precipitationProbability?.let { prob ->
                    if (prob > 0) Text(text = "${(prob * 100).toInt()}% rain", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun LocationHeader(locationName: String, lastUpdated: LocalDateTime?, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location", tint = Color.White, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = locationName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                lastUpdated?.let { Text(text = "Updated ${it.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f)) }
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Loading weather data...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Unable to load weather", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onRetry) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(text = "Try Again", color = Color.White, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7B4FD9)
@Composable
private fun WeatherScreenPreview() {
    SkyesAboveTheme {
        val previewUiState = WeatherUiState(
            isLoading = false,
            currentWeather = CurrentWeatherState(temperature = 18.5, feelsLikeTemperature = 16.2, weatherSymbol = "partlycloudy_day", weatherDescription = "Partly Cloudy", windSpeed = 5.2, windDirection = 225.0, humidity = 75.0, cloudCover = 40.0, pressure = 1015.0, visibility = 10.0, uvIndex = 3.0),
            sunTimes = SunTimesState(sunrise = LocalTime.of(7, 30), sunset = LocalTime.of(17, 45), civilTwilightStart = LocalTime.of(7, 0), civilTwilightEnd = LocalTime.of(18, 15)),
            hourlyForecast = (0..48).map { HourlyForecast(time = LocalDateTime.now().plusHours(it.toLong()), temperature = 15.0 + (it % 8), symbolCode = listOf("clearsky_day", "partlycloudy_day", "cloudy", "lightrain", "rain")[it % 5], precipitationProbability = if (it % 3 == 0) 0.3 else null) },
            dailyForecast = (0..6).map { DailyForecast(date = LocalDate.now().plusDays(it.toLong()), highTemp = 18.0 + it, lowTemp = 10.0 + it, symbolCode = listOf("clearsky_day", "partlycloudy_day", "rain", "cloudy")[it % 4], precipitationProbability = if (it % 2 == 0) 0.3 else null) },
            lastUpdated = LocalDateTime.now(),
            locationName = "Dublin, Ireland"
        )
        Box(modifier = Modifier.fillMaxSize().background(brush = skyesAboveGradient())) {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                LocationHeader(locationName = previewUiState.locationName, lastUpdated = previewUiState.lastUpdated, onSettingsClick = {}, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                NowTabContent(previewUiState)
            }
        }
    }
}
