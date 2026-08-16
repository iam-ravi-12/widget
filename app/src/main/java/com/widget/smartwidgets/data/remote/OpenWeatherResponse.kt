package com.widget.smartwidgets.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenWeatherResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherDataItem>
)

@Serializable
data class MainData(
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    val humidity: Int
)

@Serializable
data class WeatherDataItem(
    val main: String,
    val description: String,
    val icon: String
)
