package com.widget.smartwidgets.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.widget.smartwidgets.data.local.WidgetDataDao
import com.widget.smartwidgets.data.model.WidgetDataEntity
import com.widget.smartwidgets.data.local.WeatherWidgetConfigDao
import com.widget.smartwidgets.data.local.WeatherWidgetConfigEntity

import com.widget.smartwidgets.data.local.NoteDao
import com.widget.smartwidgets.data.local.NoteEntity
import com.widget.smartwidgets.data.local.WeatherDao
import com.widget.smartwidgets.data.local.WeatherEntity
import com.widget.smartwidgets.data.local.TodoDao
import com.widget.smartwidgets.data.local.TodoEntity

@Database(
    entities = [WidgetDataEntity::class, NoteEntity::class, WeatherEntity::class, TodoEntity::class, WeatherWidgetConfigEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun widgetDataDao(): WidgetDataDao
    abstract fun noteDao(): NoteDao
    abstract fun weatherDao(): WeatherDao
    abstract fun todoDao(): TodoDao
    abstract fun weatherWidgetConfigDao(): WeatherWidgetConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_widgets_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
