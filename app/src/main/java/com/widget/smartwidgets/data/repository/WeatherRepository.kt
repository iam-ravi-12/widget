package com.widget.smartwidgets.data.repository

import android.content.Context
import android.util.Log
import com.widget.smartwidgets.BuildConfig
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.data.local.WeatherEntity
import com.widget.smartwidgets.data.remote.OpenWeatherResponse
import com.widget.smartwidgets.data.remote.WeatherApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeatherRepository private constructor(context: Context) {
    private val weatherDao = AppDatabase.getInstance(context).weatherDao()

    /**
     * Tries to fetch fresh weather if the cache is stale. 
     * If the network fails, or if the cache is still fresh, returns the cached version.
     */
    suspend fun getWeatherForCity(
        city: String,
        units: String = "metric",
        forceRefresh: Boolean = false
    ): WeatherEntity? {
        if (city.isBlank()) {
            Log.w(TAG, "Empty city name provided to getWeatherForCity")
            return null
        }
        val cleanUnit = sanitizeUnits(units)
        val locationId = normalizeCity(city, cleanUnit)
        return fetchWeather(locationId = locationId, units = cleanUnit, forceRefresh = forceRefresh) { apiKey ->
            api.getWeatherByCity(city = city.trim(), apiKey = apiKey, units = cleanUnit)
        }
    }

    suspend fun getWeatherForCoordinates(
        lat: Double,
        lon: Double,
        units: String = "metric",
        forceRefresh: Boolean = false
    ): WeatherEntity? {
        val cleanUnit = sanitizeUnits(units)
        val locationId = formatCoordinates(lat, lon, cleanUnit)
        return fetchWeather(locationId = locationId, units = cleanUnit, forceRefresh = forceRefresh) { apiKey ->
            api.getWeatherByCoordinates(lat = lat, lon = lon, apiKey = apiKey, units = cleanUnit)
        }
    }

    private suspend fun fetchWeather(
        locationId: String,
        units: String,
        forceRefresh: Boolean,
        apiCall: suspend (String) -> OpenWeatherResponse
    ): WeatherEntity? {
        val cached = weatherDao.getWeatherByLocation(locationId)
        
        val isStale = cached == null || (System.currentTimeMillis() - cached.fetchedAt) > STALE_CACHE_THRESHOLD_MS

        if (isStale || forceRefresh) {
            val apiKey = BuildConfig.WEATHER_API_KEY
            if (apiKey == "NO_API_KEY" || apiKey.isBlank()) {
                Log.e(TAG, "WeatherAPI: API key configured = false")
                Log.e(TAG, "OPEN_WEATHER_API_KEY is missing from local.properties.")
                return cached
            }
            Log.d(TAG, "WeatherAPI: API key configured = true")

            try {
                val response = apiCall(apiKey)
                
                val newEntity = WeatherEntity(
                    locationId = locationId,
                    displayName = response.name.ifBlank { locationId.substringBefore(":") },
                    temperature = response.main.temp,
                    feelsLike = response.main.feelsLike,
                    condition = response.weather.firstOrNull()?.main ?: "Unknown",
                    iconCode = response.weather.firstOrNull()?.icon ?: "01d",
                    fetchedAt = System.currentTimeMillis()
                )
                
                weatherDao.insertWeather(newEntity)
                Log.d(TAG, "WeatherAPI: request successful")
                Log.d(TAG, "WeatherAPI: temperature=${newEntity.temperature}")
                Log.d(TAG, "WeatherAPI: city=${newEntity.displayName}")
                return newEntity
            } catch (e: HttpException) {
                Log.e(TAG, "WeatherAPI: HTTP status = ${e.code()}")
                val errorBody = e.response()?.errorBody()?.string()?.take(200) ?: "unknown"
                Log.e(TAG, "WeatherAPI: error body = $errorBody")
                return cached
            } catch (e: IOException) {
                Log.e(TAG, "WeatherAPI: network error = ${e.message}")
                return cached
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.e(TAG, "WeatherAPI: JSON parsing error = ${e.message}")
                return cached
            } catch (e: Exception) {
                Log.e(TAG, "WeatherAPI: unexpected error = ${e.javaClass.simpleName}: ${e.message}")
                return cached
            }
        }
        
        return cached
    }

    suspend fun getCachedWeather(locationId: String): WeatherEntity? {
        return weatherDao.getWeatherByLocation(locationId)
    }

    companion object {
        private const val TAG = "WeatherRepository"

        // Cache weather for 1 hour
        private const val STALE_CACHE_THRESHOLD_MS = 60 * 60 * 1000L

        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }

        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(MediaType.parse("application/json")!!))
                .build()
        }

        private val api: WeatherApiService by lazy {
            retrofit.create(WeatherApiService::class.java)
        }

        fun sanitizeUnits(units: String): String {
            return if (units.equals("imperial", ignoreCase = true)) "imperial" else "metric"
        }

        fun normalizeCity(city: String, units: String = "metric"): String {
            val cleanCity = city.trim().lowercase(Locale.US)
            val cleanUnit = sanitizeUnits(units)
            return "$cleanCity:$cleanUnit"
        }

        fun formatCoordinates(lat: Double, lon: Double, units: String = "metric"): String {
            val cleanUnit = sanitizeUnits(units)
            return String.format(Locale.US, "%.4f,%.4f:%s", lat, lon, cleanUnit)
        }
    }
}
