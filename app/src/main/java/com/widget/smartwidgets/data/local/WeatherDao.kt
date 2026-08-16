package com.widget.smartwidgets.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE locationId = :locationId LIMIT 1")
    suspend fun getWeatherByLocation(locationId: String): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM weather ORDER BY fetchedAt DESC LIMIT 1")
    suspend fun getMostRecentWeather(): WeatherEntity?
}
