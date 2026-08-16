package com.widget.smartwidgets.ui.screens.countdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import com.widget.smartwidgets.widgets.countdown.CountdownWidget
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownConfigScreen(
    appWidgetId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { WidgetPreferences(context) }
    
    var isSaving by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var daysFromNow by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val titleKey = PreferencesKeys.countdownTitle(appWidgetId)
        val configuredTitle = prefs.getPreference(titleKey, "").firstOrNull()
        if (!configuredTitle.isNullOrBlank()) {
            title = configuredTitle
        }
    }

    val handleSave: () -> Unit = {
        val days = daysFromNow.toLongOrNull()
        if (title.isNotBlank() && days != null && days > 0) {
            isSaving = true
            scope.launch {
                val targetKey = PreferencesKeys.countdownTarget(appWidgetId)
                val titleKey = PreferencesKeys.countdownTitle(appWidgetId)
                
                val targetTime = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                prefs.setPreference(titleKey, title)
                prefs.setPreference(targetKey, targetTime.toString())
                
                val manager = GlanceAppWidgetManager(context)
                val glanceId = manager.getGlanceIdBy(appWidgetId)
                CountdownWidget().update(context, glanceId)
                
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Countdown") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSaving) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = handleSave, enabled = !isSaving && title.isNotBlank() && daysFromNow.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Event Title", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Project Deadline") },
                singleLine = true,
                enabled = !isSaving
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Days until event", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = daysFromNow,
                onValueChange = { if (it.all { char -> char.isDigit() }) daysFromNow = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 14") },
                singleLine = true,
                enabled = !isSaving
            )
        }
    }
}
