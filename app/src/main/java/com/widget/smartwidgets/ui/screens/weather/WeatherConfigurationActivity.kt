package com.widget.smartwidgets.ui.screens.weather

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.data.local.WeatherWidgetConfigEntity
import com.widget.smartwidgets.data.repository.WeatherRepository
import com.widget.smartwidgets.ui.theme.SmartWidgetsTheme
import com.widget.smartwidgets.widgets.weather.WeatherWidget
import com.widget.smartwidgets.widgets.weather.WeatherWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: WeatherRepository
    private lateinit var db: AppDatabase

    private var locationPermissionCallback: ((Boolean) -> Unit)? = null

    @android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        locationPermissionCallback?.invoke(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED upfront. If the user presses back or cancels,
        // the AppWidget host will cancel the widget placement.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If this activity was started without a valid widget ID, finish immediately.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "WeatherConfigurationActivity started with INVALID_APPWIDGET_ID")
            finish()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        repository = WeatherRepository.getInstance(this)
        db = AppDatabase.getInstance(this)

        setContent {
            SmartWidgetsTheme {
                WeatherConfigScreenContent(
                    appWidgetId = appWidgetId,
                    db = db,
                    onSave = { mode, city, lat, lon, unit ->
                        saveConfiguration(mode, city, lat, lon, unit)
                    },
                    onRequestLocation = { onResult ->
                        requestLocation(onResult)
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun requestLocation(onResult: (Double?, Double?) -> Unit) {
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            getLocation(onResult)
        } else {
            locationPermissionCallback = { isGranted ->
                if (isGranted) {
                    getLocation(onResult)
                } else {
                    Toast.makeText(this, "Location permission is required for current location weather.", Toast.LENGTH_LONG).show()
                    onResult(null, null)
                }
            }
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun getLocation(onResult: (Double?, Double?) -> Unit) {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onResult(location.latitude, location.longitude)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                onResult(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                Toast.makeText(this, "Could not determine location. Ensure GPS is enabled.", Toast.LENGTH_LONG).show()
                                onResult(null, null)
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Could not determine location.", Toast.LENGTH_SHORT).show()
                            onResult(null, null)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Location request failed.", Toast.LENGTH_SHORT).show()
                    onResult(null, null)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while accessing location", e)
            onResult(null, null)
        }
    }

    private fun saveConfiguration(mode: String, city: String, lat: Double?, lon: Double?, unit: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val cleanCity = city.trim()
            val config = WeatherWidgetConfigEntity(
                appWidgetId = appWidgetId,
                locationMode = mode,
                cityName = cleanCity,
                latitude = lat,
                longitude = lon,
                temperatureUnit = unit
            )
            db.weatherWidgetConfigDao().insertConfig(config)
            Log.d(TAG, "Saved configuration for appWidgetId: $appWidgetId")

            // Pre-fetch live weather to cache it
            try {
                if (mode == "CURRENT_LOCATION" && lat != null && lon != null) {
                    repository.getWeatherForCoordinates(lat, lon, unit, forceRefresh = true)
                } else if (cleanCity.isNotBlank()) {
                    repository.getWeatherForCity(cleanCity, unit, forceRefresh = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initial weather fetch failed during config save for widget $appWidgetId", e)
            }

            withContext(Dispatchers.Main) {
                // Return RESULT_OK to the launcher
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(Activity.RESULT_OK, resultValue)

                // Trigger Glance to do the actual UI generation natively via broadcast
                val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = ComponentName(this@WeatherConfigurationActivity, WeatherWidgetReceiver::class.java)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
                sendBroadcast(updateIntent)
                
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "WeatherConfigActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherConfigScreenContent(
    appWidgetId: Int,
    db: AppDatabase,
    onSave: (mode: String, city: String, lat: Double?, lon: Double?, unit: String) -> Unit,
    onRequestLocation: (onResult: (Double?, Double?) -> Unit) -> Unit,
    onCancel: () -> Unit
) {
    var mode by remember { mutableStateOf("MANUAL_CITY") }
    var city by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("metric") }
    var isSaving by remember { mutableStateOf(false) }

    // Pre-populate existing config if reconfiguring
    LaunchedEffect(appWidgetId) {
        val existingConfig = withContext(Dispatchers.IO) {
            db.weatherWidgetConfigDao().getConfig(appWidgetId)
        }
        if (existingConfig != null) {
            mode = existingConfig.locationMode
            city = existingConfig.cityName
            unit = existingConfig.temperatureUnit
        }
    }

    val handleSave = {
        if (!isSaving) {
            isSaving = true
            if (mode == "CURRENT_LOCATION") {
                onRequestLocation { lat, lon ->
                    if (lat != null && lon != null) {
                        onSave("CURRENT_LOCATION", "", lat, lon, unit)
                    } else {
                        isSaving = false
                    }
                }
            } else {
                if (city.isNotBlank()) {
                    onSave("MANUAL_CITY", city.trim(), null, null, unit)
                } else {
                    isSaving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Weather") },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    val canSave = if (mode == "CURRENT_LOCATION") true else city.isNotBlank()
                    IconButton(onClick = handleSave, enabled = !isSaving && canSave) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("How should weather be determined?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Current Location Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving) { mode = "CURRENT_LOCATION" },
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == "CURRENT_LOCATION") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Use my current location", style = MaterialTheme.typography.titleSmall)
                        Text("Automatically use your location for weather.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual City Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving) { mode = "MANUAL_CITY" },
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == "MANUAL_CITY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Choose a city", style = MaterialTheme.typography.titleSmall)
                            Text("Search or enter a city name manually.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (mode == "MANUAL_CITY") {
                        Spacer(modifier = Modifier.height(16.dp))
                        var expanded by remember { mutableStateOf(false) }
                        val commonCities = listOf("London", "New York", "Tokyo", "Paris", "Berlin", "Delhi", "Mumbai", "Sydney", "Moscow", "Beijing", "Dubai", "Singapore", "San Francisco")

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = {
                                    city = it
                                    expanded = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = !isSaving),
                                placeholder = { Text("e.g. London, Tokyo, New York") },
                                singleLine = true,
                                enabled = !isSaving,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            val filteredCities = commonCities.filter { it.contains(city, ignoreCase = true) && !it.equals(city, ignoreCase = true) }
                            if (filteredCities.isNotEmpty() && expanded) {
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    filteredCities.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption) },
                                            onClick = {
                                                city = selectionOption
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("Temperature Unit", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = unit == "metric",
                    onClick = { unit = "metric" },
                    enabled = !isSaving
                )
                Text("Celsius (°C)", modifier = Modifier.clickable(enabled = !isSaving) { unit = "metric" })
                Spacer(modifier = Modifier.width(24.dp))
                RadioButton(
                    selected = unit == "imperial",
                    onClick = { unit = "imperial" },
                    enabled = !isSaving
                )
                Text("Fahrenheit (°F)", modifier = Modifier.clickable(enabled = !isSaving) { unit = "imperial" })
            }

            if (isSaving) {
                Spacer(modifier = Modifier.height(28.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Saving & fetching weather...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
