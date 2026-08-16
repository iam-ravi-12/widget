package com.widget.smartwidgets.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.widget.smartwidgets.ui.screens.HomeScreen
import com.widget.smartwidgets.ui.screens.notes.CreateEditNoteScreen
import com.widget.smartwidgets.ui.screens.notes.NotesScreen
import com.widget.smartwidgets.ui.screens.worldclock.WorldClockConfigScreen
import com.widget.smartwidgets.ui.screens.countdown.CountdownConfigScreen
import com.widget.smartwidgets.ui.screens.photoframe.PhotoFrameConfigScreen
import com.widget.smartwidgets.ui.screens.todo.TodoScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    intent: Intent? = null
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToNotes = { navController.navigate("notes") }
            )
        }
        
        composable("notes") {
            NotesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate("create_note") },
                onNavigateToEdit = { noteId -> navController.navigate("edit_note/$noteId") }
            )
        }
        
        composable("create_note") {
            CreateEditNoteScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "edit_note/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")
            CreateEditNoteScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "world_clock_config/{appWidgetId}",
            arguments = listOf(navArgument("appWidgetId") { type = NavType.IntType })
        ) { backStackEntry ->
            val appWidgetId = backStackEntry.arguments?.getInt("appWidgetId") ?: 0
            WorldClockConfigScreen(
                appWidgetId = appWidgetId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "countdown_config/{appWidgetId}",
            arguments = listOf(navArgument("appWidgetId") { type = NavType.IntType })
        ) { backStackEntry ->
            val appWidgetId = backStackEntry.arguments?.getInt("appWidgetId") ?: 0
            CountdownConfigScreen(
                appWidgetId = appWidgetId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "photo_frame_config/{appWidgetId}",
            arguments = listOf(navArgument("appWidgetId") { type = NavType.IntType })
        ) { backStackEntry ->
            val appWidgetId = backStackEntry.arguments?.getInt("appWidgetId") ?: 0
            PhotoFrameConfigScreen(
                appWidgetId = appWidgetId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("todo") {
            TodoScreen(onNavigateBack = { navController.popBackStack() })
        }
    }

    // Handle deep links from Widget
    LaunchedEffect(intent) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "smartwidgets") {
            if (uri.host == "notes") {
                when (uri.pathSegments.firstOrNull()) {
                    "create" -> navController.navigate("create_note")
                    "edit" -> {
                        val noteId = uri.pathSegments.getOrNull(1)?.toLongOrNull()
                        if (noteId != null) {
                            navController.navigate("edit_note/$noteId")
                        }
                    }
                }
                intent.data = null
            } else if (uri.host == "worldclock" || uri.host == "countdown" || uri.host == "photoframe") {
                if (uri.pathSegments.firstOrNull() == "config") {
                    val appWidgetId = uri.pathSegments.getOrNull(1)?.toIntOrNull()
                    if (appWidgetId != null) {
                        when(uri.host) {
                            "worldclock" -> navController.navigate("world_clock_config/$appWidgetId")
                            "countdown" -> navController.navigate("countdown_config/$appWidgetId")
                            "photoframe" -> navController.navigate("photo_frame_config/$appWidgetId")
                        }
                    }
                }
                intent.data = null
            } else if (uri.host == "todo") {
                navController.navigate("todo")
                intent.data = null
            }
        }
    }
}
