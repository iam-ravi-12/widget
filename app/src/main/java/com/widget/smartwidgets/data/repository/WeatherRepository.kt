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
        Log.d(TAG, "WEATHER DEBUG:\ncalling OpenWeather\ncity = $city\nunits = $cleanUnit")
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
        Log.d(TAG, "WEATHER DEBUG:\ncalling OpenWeather\nlat = $lat\nlon = $lon\nunits = $cleanUnit")
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
                Log.e(TAG, "WEATHER DEBUG:\napiKeyConfigured = false\napiKeyLength = ${apiKey.length}")
                Log.e(TAG, "OPEN_WEATHER_API_KEY is missing from local.properties.")
                return cached
            }
            Log.d(TAG, "WEATHER DEBUG:\napiKeyConfigured = true\napiKeyLength = ${apiKey.length}")

            try {
                val response = apiCall(apiKey)
                
                Log.d(TAG, "WEATHER DEBUG:\nOpenWeather response received")
                Log.d(TAG, "WEATHER DEBUG:\nresponse.name = ${response.name}\nresponse.main.temp = ${response.main.temp}\nresponse.main.feels_like = ${response.main.feelsLike}\nresponse.main.humidity = ${response.main.humidity}\nresponse.weather.firstOrNull()?.main = ${response.weather.firstOrNull()?.main}\nresponse.weather.firstOrNull()?.description = ${response.weather.firstOrNull()?.description}\nresponse.weather.firstOrNull()?.icon = ${response.weather.firstOrNull()?.icon}")
                
                val newEntity = WeatherEntity(
                    locationId = locationId,
                    displayName = response.name.ifBlank { locationId.substringBefore(":") },
                    temperature = response.main.temp,
                    feelsLike = response.main.feelsLike,
                    condition = response.weather.firstOrNull()?.main ?: "Unknown",
                    iconCode = response.weather.firstOrNull()?.icon ?: "01d",
                    fetchedAt = System.currentTimeMillis()
                )
                
                Log.d(TAG, "WEATHER DEBUG:\nresponse conversion SUCCESS")
                Log.d(TAG, "WEATHER DEBUG:\nWeatherEntity created")
                
                weatherDao.insertWeather(newEntity)
                Log.d(TAG, "WEATHER DEBUG:\nRoom weather save SUCCESS\nlocationId = $locationId")
                
                val readBack = weatherDao.getWeatherByLocation(locationId)
                if (readBack != null) {
                    Log.d(TAG, "WEATHER DEBUG:\nRoom read-back SUCCESS\ncity = ${readBack.displayName}\ntemperature = ${readBack.temperature}")
                } else {
                    Log.e(TAG, "WEATHER DEBUG:\nRoom read-back FAILED for locationId = $locationId")
                }
                
                return newEntity
            } catch (e: HttpException) {
                Log.e(TAG, "WEATHER DEBUG:\nHTTP FAILURE\nstatus = ${e.code()}")
                val errorBody = e.response()?.errorBody()?.string()?.take(200) ?: "unknown"
                Log.e(TAG, "WEATHER DEBUG:\nerror body = $errorBody")
                return cached
            } catch (e: IOException) {
                Log.e(TAG, "WEATHER DEBUG:\nNETWORK FAILURE\nmessage = ${e.message}")
                return cached
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.e(TAG, "WEATHER DEBUG:\nPARSING FAILURE\nmessage = ${e.message}")
                return cached
            } catch (e: Exception) {
                Log.e(TAG, "WEATHER DEBUG:\nUNEXPECTED FAILURE\nclass = ${e.javaClass.simpleName}\nmessage = ${e.message}")
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
