package com.widget.smartwidgets.widgets.weather

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.data.local.WeatherEntity
import com.widget.smartwidgets.data.local.WeatherWidgetConfigEntity
import com.widget.smartwidgets.data.repository.WeatherRepository
import com.widget.smartwidgets.ui.screens.weather.WeatherConfigurationActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = try {
            manager.getAppWidgetId(id)
        } catch (e: Exception) {
            AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val db = AppDatabase.getInstance(context)
        val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            db.weatherWidgetConfigDao().getConfig(appWidgetId)
        } else {
            null
        }

        var weatherData: WeatherEntity? = null
        var locationIdBeingQueried: String? = null
        if (config != null) {
            val repository = WeatherRepository.getInstance(context)
            locationIdBeingQueried = if (config.locationMode == "CURRENT_LOCATION" && config.latitude != null && config.longitude != null) {
                WeatherRepository.formatCoordinates(config.latitude, config.longitude, config.temperatureUnit)
            } else {
                WeatherRepository.normalizeCity(config.cityName, config.temperatureUnit)
            }

            // Read from cache. Live fetches are handled by WeatherConfigurationActivity and RefreshWeatherAction.
            weatherData = repository.getCachedWeather(locationIdBeingQueried)
        }
        
        Log.d("WeatherWidget", "WEATHER DEBUG:\nwidget rendering\nappWidgetId = $appWidgetId")
        Log.d("WeatherWidget", "WEATHER DEBUG:\nconfig found = ${config != null}\nweather found = ${weatherData != null}")
        
        if (config != null && weatherData == null) {
            Log.d("WeatherWidget", "WEATHER DEBUG:\nweather is null. locationId being queried = $locationIdBeingQueried")
        }

        provideContent {
            GlanceTheme {
                WeatherWidgetContent(
                    appWidgetId = appWidgetId,
                    config = config,
                    weatherData = weatherData
                )
            }
        }
    }
}

@Composable
fun WeatherWidgetContent(
    appWidgetId: Int,
    config: WeatherWidgetConfigEntity?,
    weatherData: WeatherEntity?
) {
    val context = LocalContext.current

    val configIntent = Intent(context, WeatherConfigurationActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            // Case 1: Unconfigured widget
            config == null -> {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(configIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weather",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = "Tap to configure",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Case 2: Configured and weather data available
            weatherData != null -> {
                val emoji = getWeatherEmoji(weatherData.iconCode)
                val unitSymbol = if (config.temperatureUnit.equals("imperial", ignoreCase = true)) "°F" else "°C"
                val displayName = if (config.locationMode == "CURRENT_LOCATION") "📍 ${weatherData.displayName}" else weatherData.displayName

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(configIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = emoji,
                            style = TextStyle(
                                fontSize = 28.sp
                            )
                        )
                        Text(
                            text = " ${weatherData.temperature.toInt()}$unitSymbol",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = displayName,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )

                    Text(
                        text = "${weatherData.condition} • Feels ${weatherData.feelsLike.toInt()}$unitSymbol",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )

                    val timeString = formatRelativeTime(weatherData.fetchedAt)
                    Text(
                        text = "Updated $timeString",
                        style = TextStyle(
                            color = GlanceTheme.colors.outline,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Case 3: Configured but weather unavailable / fetch failed
            else -> {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<RefreshWeatherAction>()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Weather unavailable",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to retry ↻",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

private fun getWeatherEmoji(iconCode: String): String {
    return when (iconCode.take(2)) {
        "01" -> "☀️"
        "02" -> "⛅"
        "03", "04" -> "☁️"
        "09", "10" -> "🌧️"
        "11" -> "⛈️"
        "13" -> "❄️"
        "50" -> "🌫️"
        else -> "🌡️"
    }
}

private fun formatRelativeTime(fetchedAt: Long): String {
    val diffMillis = System.currentTimeMillis() - fetchedAt
    val diffMins = diffMillis / (60 * 1000)
    return when {
        diffMins < 1 -> "just now"
        diffMins < 60 -> "$diffMins min ago"
        diffMins < 1440 -> "${diffMins / 60}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(fetchedAt))
    }
}

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = try {
            manager.getAppWidgetId(glanceId)
        } catch (e: Exception) {
            AppWidgetManager.INVALID_APPWIDGET_ID
        }

        Log.d("RefreshWeatherAction", "Refresh triggered for appWidgetId: $appWidgetId")

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val db = AppDatabase.getInstance(context)
            val config = db.weatherWidgetConfigDao().getConfig(appWidgetId)
            if (config != null) {
                val repository = WeatherRepository.getInstance(context)
                try {
                    if (config.locationMode == "CURRENT_LOCATION" && config.latitude != null && config.longitude != null) {
                        repository.getWeatherForCoordinates(config.latitude, config.longitude, config.temperatureUnit, forceRefresh = true)
                    } else if (config.cityName.isNotBlank()) {
                        repository.getWeatherForCity(config.cityName, config.temperatureUnit, forceRefresh = true)
                    }
                } catch (e: Exception) {
                    Log.e("RefreshWeatherAction", "Failed refreshing weather for widget $appWidgetId", e)
                }
            } else {
                Log.w("RefreshWeatherAction", "No config found in Room for widget $appWidgetId")
            }
        }

        try {
            WeatherWidget().update(context, glanceId)
        } catch (e: Exception) {
            Log.e("RefreshWeatherAction", "Failed updating widget after refresh", e)
        }
    }
}
