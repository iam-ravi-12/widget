package com.widget.smartwidgets.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.widget.smartwidgets.data.model.WidgetDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDataDao {

    @Query("SELECT * FROM widget_data WHERE widgetType = :type ORDER BY lastUpdated DESC LIMIT 1")
    fun getLatestByType(type: String): Flow<WidgetDataEntity?>

    @Query("SELECT * FROM widget_data WHERE id = :id")
    suspend fun getById(id: String): WidgetDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: WidgetDataEntity)

    @Query("DELETE FROM widget_data WHERE widgetType = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM widget_data WHERE expiresAt > 0 AND expiresAt < :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())
}
