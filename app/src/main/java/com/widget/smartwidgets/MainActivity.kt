package com.widget.smartwidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.widget.smartwidgets.ui.screens.HomeScreen
import com.widget.smartwidgets.ui.theme.SmartWidgetsTheme

import androidx.navigation.compose.rememberNavController
import com.widget.smartwidgets.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartWidgetsTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController, intent = intent)
            }
        }
    }
}
