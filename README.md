# Skyes Above

**A clean, three-source weather app for Android — with a home-screen widget that only ever shows you live data.**

Skyes Above pulls the forecast from three independent weather services at once and
**averages them together** for a more reliable reading than any single provider gives you.
It's built with Jetpack Compose and Material 3, and it comes with a transparent home-screen
widget that shows your local forecast next to your own back-garden weather station.

<!-- SCREENSHOTS -->

## Why three sources?

Any one weather API can be wrong — a stale model run, a missing field, a bad station reading.
Skyes Above fetches all three in parallel and merges them, dropping empty/"missing" values so
one provider's gap doesn't drag the average down:

| Source | Coverage | When it's used |
| --- | --- | --- |
| **Met Éireann** | Ireland only | Forecast + live Dublin observations, when you're in Ireland |
| **Open-Meteo** | Global | Always on — the baseline everywhere |
| **OpenWeatherMap** | Global | When you've added a free API key in Settings |

Sun and twilight times come from the Sunrise–Sunset API, shown in your local time.

## Features

- **Now, hourly, and 7-day forecast** — current conditions up top, an hourly strip, and a
  week-ahead grid with daily highs and lows.
- **A full stats panel** — feels-like, humidity, wind speed and direction, pressure, UV index,
  cloud cover, visibility, and sunrise / sunset / civil-twilight times.
- **Pressure tendency** — a three-hour rising/falling/steady arrow and a low→high gauge, so you
  can read where the weather is heading.
- **Garden trends** — a dedicated Trends tab charting your own back-garden sensor history
  (temperature, humidity, pressure) over **Hour / Day / Week / Month / Year**, fed live from the
  companion [`wunderground-killi`](https://github.com/garnathan/wunderground-killi) Raspberry-Pi
  station.
- **On-device diagnostics** — see exactly what each data source returned (and *why* a fetch
  failed) without ever needing a cable or `logcat`.
- **Configurable** — auto-locate by GPS or pin a location, choose your home garden station, add
  your OpenWeatherMap key, and refresh on demand.

## The home-screen widget

A transparent **4×2** widget that splits in two:

- **Left half** — the merged three-source forecast for wherever your phone is.
- **Right half** — your home garden station's latest reading.

The widget refreshes on a **self-healing ~15-minute heartbeat** (WorkManager, Doze-aware and
network-gated) and kicks an immediate refresh whenever you add or tap it, so it recovers the
moment connectivity returns.

> **Live data only.** If the widget can't get a genuine real-time reading, it shows **`—` (NO
> DATA)** rather than a stale value or an old timestamp. What you see on the widget is always
> current, or it's honestly blank.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Hilt** for dependency injection
- **Retrofit** / **OkHttp** for the weather APIs
- **Kotlin Coroutines** for parallel fetches
- **WorkManager** for the widget's background refresh
- **DataStore** for settings
- Min SDK 26 · Target SDK 34

## Build & run

Requires JDK 17.

```bash
# Debug build
./gradlew assembleDebug

# Install onto a connected device / emulator
./gradlew installDebug
```

OpenWeatherMap is optional — the app works out of the box on Open-Meteo (and Met Éireann in
Ireland). To enable the third source, add a free [OpenWeatherMap](https://openweathermap.org/api)
API key under **Settings**.

## Project layout

```
app/src/main/java/com/ganathan/skyesabove/
├── data/
│   ├── api/           # Retrofit services: Met Éireann, Open-Meteo, OpenWeatherMap,
│   │                  #   Sunrise-Sunset, and the garden-history feed
│   ├── model/         # Weather, sun, and sensor-trend data models
│   ├── repository/    # WeatherRepository (merges the 3 sources) + GardenHistoryRepository
│   └── preferences/   # DataStore-backed settings
├── ui/
│   ├── screens/       # weather · trends · settings · diagnostics
│   └── theme/         # Material 3 theme
└── widget/            # Home-screen widget + WorkManager refresh worker
```

## Related project

- **[wunderground-killi](https://github.com/garnathan/wunderground-killi)** — the Raspberry-Pi
  garden weather station that publishes the sensor history this app charts and shows on the
  widget.
