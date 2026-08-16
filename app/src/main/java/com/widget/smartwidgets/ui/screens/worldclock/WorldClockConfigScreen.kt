package com.widget.smartwidgets.ui.screens.worldclock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import com.widget.smartwidgets.widgets.worldclock.WorldClockWidget
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockConfigScreen(
    appWidgetId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { WidgetPreferences(context) }
    val zonesList = remember { mutableStateListOf<String>() }
    
    var isSaving by remember { mutableStateOf(false) }
    var currentInput by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<String>()) }
    
    val allZones = remember { ZoneId.getAvailableZoneIds().toList() }

    LaunchedEffect(Unit) {
        val zonesKey = PreferencesKeys.worldClockZones(appWidgetId)
        val configuredZonesStr = prefs.getPreference(zonesKey, "").firstOrNull()
        if (!configuredZonesStr.isNullOrBlank()) {
            zonesList.addAll(configuredZonesStr.split(","))
        }
    }

    val handleSave: () -> Unit = {
        isSaving = true
        scope.launch {
            val zonesKey = PreferencesKeys.worldClockZones(appWidgetId)
            prefs.setPreference(zonesKey, zonesList.joinToString(","))
            
            // Force redraw of this specific widget
            val manager = GlanceAppWidgetManager(context)
            val glanceId = manager.getGlanceIdBy(appWidgetId)
            WorldClockWidget().update(context, glanceId)
            
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure World Clock") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSaving) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = handleSave, enabled = !isSaving) {
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
            Text("Selected Timezones", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            zonesList.forEach { zone ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(zone, modifier = Modifier.weight(1f))
                    IconButton(onClick = { zonesList.remove(zone) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = currentInput,
                onValueChange = { 
                    currentInput = it 
                    searchResults = allZones.filter { zone -> zone.contains(it, ignoreCase = true) }.take(5)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for timezone (e.g. London)") },
                singleLine = true
            )
            
            LazyColumn {
                items(searchResults) { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result, modifier = Modifier.weight(1f))
                        IconButton(onClick = { 
                            if (!zonesList.contains(result)) {
                                zonesList.add(result)
                            }
                            currentInput = ""
                            searchResults = emptyList()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        }
    }
}
