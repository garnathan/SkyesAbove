package com.ganathan.skyesabove

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Skyes Above weather app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SkyesAboveApplication : Application()
