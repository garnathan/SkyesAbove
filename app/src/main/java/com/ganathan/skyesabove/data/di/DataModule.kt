package com.ganathan.skyesabove.data.di

import android.content.Context
import com.ganathan.skyesabove.data.api.MetEireannApi
import com.ganathan.skyesabove.data.api.MetEireannObservationsApi
import com.ganathan.skyesabove.data.api.OpenMeteoApi
import com.ganathan.skyesabove.data.api.OpenWeatherMapApi
import com.ganathan.skyesabove.data.api.SunriseSunsetApi
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing data layer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val TIMEOUT_SECONDS = 30L

    /**
     * Provides OkHttpClient with logging and timeout configuration.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides Retrofit instance for Met Eireann API.
     */
    @Provides
    @Singleton
    @Named("MetEireann")
    fun provideMetEireannRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MetEireannApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides Retrofit instance for Sunrise-Sunset API.
     */
    @Provides
    @Singleton
    @Named("SunriseSunset")
    fun provideSunriseSunsetRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SunriseSunsetApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides Retrofit instance for Met Éireann Observations API.
     */
    @Provides
    @Singleton
    @Named("MetEireannObservations")
    fun provideMetEireannObservationsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MetEireannObservationsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides Retrofit instance for Open-Meteo API.
     */
    @Provides
    @Singleton
    @Named("OpenMeteo")
    fun provideOpenMeteoRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides Retrofit instance for OpenWeatherMap API.
     */
    @Provides
    @Singleton
    @Named("OpenWeatherMap")
    fun provideOpenWeatherMapRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OpenWeatherMapApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides Met Eireann API interface.
     */
    @Provides
    @Singleton
    fun provideMetEireannApi(@Named("MetEireann") retrofit: Retrofit): MetEireannApi {
        return retrofit.create(MetEireannApi::class.java)
    }

    /**
     * Provides Met Éireann Observations API interface.
     */
    @Provides
    @Singleton
    fun provideMetEireannObservationsApi(@Named("MetEireannObservations") retrofit: Retrofit): MetEireannObservationsApi {
        return retrofit.create(MetEireannObservationsApi::class.java)
    }

    /**
     * Provides Sunrise-Sunset API interface.
     */
    @Provides
    @Singleton
    fun provideSunriseSunsetApi(@Named("SunriseSunset") retrofit: Retrofit): SunriseSunsetApi {
        return retrofit.create(SunriseSunsetApi::class.java)
    }

    /**
     * Provides Open-Meteo API interface.
     */
    @Provides
    @Singleton
    fun provideOpenMeteoApi(@Named("OpenMeteo") retrofit: Retrofit): OpenMeteoApi {
        return retrofit.create(OpenMeteoApi::class.java)
    }

    /**
     * Provides OpenWeatherMap API interface.
     */
    @Provides
    @Singleton
    fun provideOpenWeatherMapApi(@Named("OpenWeatherMap") retrofit: Retrofit): OpenWeatherMapApi {
        return retrofit.create(OpenWeatherMapApi::class.java)
    }

    /**
     * Provides WeatherRepository.
     */
    @Provides
    @Singleton
    fun provideWeatherRepository(
        metEireannApi: MetEireannApi,
        metEireannObservationsApi: MetEireannObservationsApi,
        openMeteoApi: OpenMeteoApi,
        openWeatherMapApi: OpenWeatherMapApi,
        sunriseSunsetApi: SunriseSunsetApi,
        settingsDataStore: SettingsDataStore
    ): WeatherRepository {
        return WeatherRepository(
            metEireannApi,
            metEireannObservationsApi,
            openMeteoApi,
            openWeatherMapApi,
            sunriseSunsetApi,
            settingsDataStore
        )
    }

    /**
     * Provides SettingsDataStore.
     */
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}
