package com.widget.smartwidgets.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherWidgetConfigDao {
    @Query("SELECT * FROM weather_config WHERE appWidgetId = :appWidgetId")
    suspend fun getConfig(appWidgetId: Int): WeatherWidgetConfigEntity?

    @Query("SELECT * FROM weather_config")
    suspend fun getAllConfigs(): List<WeatherWidgetConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: WeatherWidgetConfigEntity)

    @Query("DELETE FROM weather_config WHERE appWidgetId = :appWidgetId")
    suspend fun deleteConfig(appWidgetId: Int)
}
