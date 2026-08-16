package com.widget.smartwidgets.ui.screens.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.widget.smartwidgets.data.repository.NoteRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditNoteScreen(
    noteId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NoteRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var content by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(noteId == null) }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing note
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != -1L) {
            val note = repository.getNoteById(noteId)
            if (note != null) {
                content = note.content
            }
            isLoaded = true
        }
    }

    val handleSave: () -> Unit = {
        if (content.isNotBlank()) {
            isSaving = true
            scope.launch {
                try {
                    if (noteId == null || noteId == -1L) {
                        repository.insertNote(content)
                    } else {
                        repository.updateNote(noteId, content)
                    }
                    onNavigateBack()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save note: ${e.message}")
                    isSaving = false
                }
            }
        } else if (content.isBlank() && noteId != null && noteId != -1L) {
            // User deleted all text, act as a delete operation
            isSaving = true
            scope.launch {
                try {
                    repository.deleteNote(noteId)
                    onNavigateBack()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to delete note: ${e.message}")
                    isSaving = false
                }
            }
        } else {
            // Blank new note, just go back
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null || noteId == -1L) "Create Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSaving) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (noteId != null && noteId != -1L) {
                        IconButton(onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    repository.deleteNote(noteId)
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed to delete: ${e.message}")
                                    isSaving = false
                                }
                            }
                        }, enabled = !isSaving) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    // Save Button
                    IconButton(onClick = handleSave, enabled = !isSaving) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        if (isLoaded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text("Start typing...") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    enabled = !isSaving
                )
            }
        }
    }
}
