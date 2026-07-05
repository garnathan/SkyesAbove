package com.ganathan.skyesabove.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ganathan.skyesabove.ui.theme.CloudDarkGray
import com.ganathan.skyesabove.ui.theme.CloudGray
import com.ganathan.skyesabove.ui.theme.CloudLightGray
import com.ganathan.skyesabove.ui.theme.FogGray
import com.ganathan.skyesabove.ui.theme.NightBlue
import com.ganathan.skyesabove.ui.theme.RainBlue
import com.ganathan.skyesabove.ui.theme.RainLightBlue
import com.ganathan.skyesabove.ui.theme.SnowLightBlue
import com.ganathan.skyesabove.ui.theme.SunGold
import com.ganathan.skyesabove.ui.theme.SunYellow
import com.ganathan.skyesabove.ui.theme.ThunderPurple

/**
 * Weather icon data containing the icon and its associated color
 */
data class WeatherIconData(
    val icon: ImageVector,
    val color: Color,
    val contentDescription: String
)

/**
 * Maps Met Eireann weather symbols to appropriate Material icons and colors.
 *
 * Met Eireann symbols include:
 * - Sun, LightCloud, PartlyCloud, Cloud
 * - LightRain, Rain, DrizzleSun, DrizzleThunderSun
 * - Sleet, SleetSun, SleetThunder, SleetThunderSun
 * - Snow, SnowSun, SnowThunder, SnowThunderSun
 * - Fog
 * - LightRainSun, LightRainThunder, LightRainThunderSun
 * - RainThunder, HeavyRain, HeavyRainThunder, HeavyRainThunderSun
 *
 * Each has day variants (no suffix or _day) and night variants (_night or _polartwilight)
 */
object WeatherIconMapper {

    /**
     * Maps a Met Eireann symbol ID to an icon and color.
     *
     * @param symbolCode The Met Eireann symbol code (e.g., "clearsky_day", "rain", "partlycloudy_night")
     * @param isNight Whether it's currently night time (optional override)
     * @return WeatherIconData containing the icon, color, and content description
     */
    fun getWeatherIcon(symbolCode: String?, isNight: Boolean? = null): WeatherIconData {
        if (symbolCode == null) {
            return WeatherIconData(
                icon = Icons.Filled.WbCloudy,
                color = CloudGray,
                contentDescription = "Unknown weather"
            )
        }

        val code = symbolCode.lowercase()
        val nightTime = isNight ?: code.contains("night") || code.contains("polartwilight")

        return when {
            // Clear sky / Sun
            code.contains("clearsky") || code == "sun" -> {
                if (nightTime) {
                    WeatherIconData(Icons.Filled.NightsStay, NightBlue, "Clear night")
                } else {
                    WeatherIconData(Icons.Filled.WbSunny, SunGold, "Clear sky")
                }
            }

            // Fair / Light cloud
            code.contains("fair") || code.contains("lightcloud") -> {
                if (nightTime) {
                    WeatherIconData(Icons.Outlined.WbCloudy, CloudLightGray, "Fair night")
                } else {
                    WeatherIconData(Icons.Outlined.WbSunny, SunYellow, "Fair weather")
                }
            }

            // Partly cloudy
            code.contains("partlycloudy") || code.contains("partlycloud") -> {
                if (nightTime) {
                    WeatherIconData(Icons.Filled.WbCloudy, CloudGray, "Partly cloudy night")
                } else {
                    WeatherIconData(Icons.Filled.FilterDrama, CloudLightGray, "Partly cloudy")
                }
            }

            // Heavy rain with thunder
            code.contains("heavyrainthunder") -> {
                WeatherIconData(Icons.Filled.Thunderstorm, ThunderPurple, "Heavy rain with thunder")
            }

            // Heavy rain
            code.contains("heavyrain") -> {
                WeatherIconData(Icons.Filled.WaterDrop, RainBlue, "Heavy rain")
            }

            // Rain with thunder (but not heavy)
            code.contains("rainthunder") || code.contains("lightrainthunder") -> {
                WeatherIconData(Icons.Filled.Thunderstorm, ThunderPurple, "Rain with thunder")
            }

            // Light rain with sun
            code.contains("lightrainsun") || code.contains("drizzlesun") -> {
                WeatherIconData(Icons.Filled.WaterDrop, RainLightBlue, "Light rain with sun")
            }

            // Light rain
            code.contains("lightrain") || code.contains("drizzle") -> {
                WeatherIconData(Icons.Filled.WaterDrop, RainLightBlue, "Light rain")
            }

            // Rain (general)
            code.contains("rain") -> {
                WeatherIconData(Icons.Filled.WaterDrop, RainBlue, "Rain")
            }

            // Snow with thunder
            code.contains("snowthunder") -> {
                WeatherIconData(Icons.Filled.Thunderstorm, ThunderPurple, "Snow with thunder")
            }

            // Snow with sun
            code.contains("snowsun") || code.contains("lightsnowsun") -> {
                WeatherIconData(Icons.Outlined.AcUnit, SnowLightBlue, "Light snow")
            }

            // Snow
            code.contains("snow") || code.contains("heavysnow") -> {
                WeatherIconData(Icons.Outlined.AcUnit, SnowLightBlue, "Snow")
            }

            // Sleet with thunder
            code.contains("sleetthunder") -> {
                WeatherIconData(Icons.Filled.Thunderstorm, ThunderPurple, "Sleet with thunder")
            }

            // Sleet with sun
            code.contains("sleetsun") -> {
                WeatherIconData(Icons.Filled.Grain, RainLightBlue, "Sleet with sun")
            }

            // Sleet
            code.contains("sleet") -> {
                WeatherIconData(Icons.Filled.Grain, RainBlue, "Sleet")
            }

            // Thunder (standalone)
            code.contains("thunder") -> {
                WeatherIconData(Icons.Filled.Thunderstorm, ThunderPurple, "Thunderstorm")
            }

            // Fog
            code.contains("fog") -> {
                WeatherIconData(Icons.Filled.Air, FogGray, "Fog")
            }

            // Cloudy / Overcast
            code.contains("cloudy") || code.contains("cloud") || code.contains("overcast") -> {
                WeatherIconData(Icons.Filled.Cloud, CloudDarkGray, "Cloudy")
            }

            // Default fallback
            else -> {
                WeatherIconData(Icons.Outlined.Cloud, CloudGray, "Weather: $symbolCode")
            }
        }
    }

    /**
     * Gets a simple weather description from symbol code
     */
    fun getWeatherDescription(symbolCode: String?): String {
        if (symbolCode == null) return "Unknown"

        val code = symbolCode.lowercase()
        return when {
            code.contains("clearsky") -> "Clear Sky"
            code.contains("fair") -> "Fair"
            code.contains("partlycloudy") -> "Partly Cloudy"
            code.contains("cloudy") || code.contains("cloud") -> "Cloudy"
            code.contains("heavyrainthunder") -> "Heavy Rain & Thunder"
            code.contains("heavyrain") -> "Heavy Rain"
            code.contains("lightrainthunder") || code.contains("rainthunder") -> "Rain & Thunder"
            code.contains("lightrain") || code.contains("drizzle") -> "Light Rain"
            code.contains("rain") -> "Rain"
            code.contains("snowthunder") -> "Snow & Thunder"
            code.contains("heavysnow") -> "Heavy Snow"
            code.contains("lightsnow") || code.contains("snowsun") -> "Light Snow"
            code.contains("snow") -> "Snow"
            code.contains("sleetthunder") -> "Sleet & Thunder"
            code.contains("sleet") -> "Sleet"
            code.contains("thunder") -> "Thunderstorm"
            code.contains("fog") -> "Foggy"
            else -> symbolCode.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
}
