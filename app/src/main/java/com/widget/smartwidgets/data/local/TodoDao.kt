package com.widget.smartwidgets.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, createdAt DESC LIMIT 5")
    suspend fun getTopTasksForWidget(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoEntity)

    @Update
    suspend fun updateTask(task: TodoEntity)

    @Delete
    suspend fun deleteTask(task: TodoEntity)

    @Query("UPDATE todo_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, completed: Boolean)
}
