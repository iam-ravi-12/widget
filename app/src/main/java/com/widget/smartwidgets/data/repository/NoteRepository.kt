package com.widget.smartwidgets.data.repository

import android.content.Context
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.data.local.NoteEntity
import com.widget.smartwidgets.data.model.Note
import com.widget.smartwidgets.widgets.notes.QuickNotesWidgetReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val context: Context) {
    private val noteDao = AppDatabase.getInstance(context).noteDao()

    fun getAllNotesFlow(): Flow<List<Note>> {
        return noteDao.getAllNotesFlow().map { entities ->
            entities.map { it.toNote() }
        }
    }

    suspend fun getRecentNotes(limit: Int): List<Note> {
        return noteDao.getRecentNotes(limit).map { it.toNote() }
    }
    
    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)?.toNote()
    }

    suspend fun insertNote(content: String) {
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            content = content,
            createdAt = now,
            updatedAt = now
        )
        noteDao.insertNote(entity)
        notifyWidgetUpdated()
    }

    suspend fun updateNote(id: Long, content: String) {
        val existing = noteDao.getNoteById(id) ?: return
        val updatedEntity = existing.copy(
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedEntity)
        notifyWidgetUpdated()
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNoteById(id)
        notifyWidgetUpdated()
    }

    private fun notifyWidgetUpdated() {
        QuickNotesWidgetReceiver.updateAllInstances(context)
    }

    private fun NoteEntity.toNote() = Note(
        id = id,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
