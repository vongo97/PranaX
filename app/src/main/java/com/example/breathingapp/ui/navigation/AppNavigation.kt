package com.example.breathingapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import com.example.breathingapp.data.BreathingPatternRepository
import com.example.breathingapp.ui.breathing.BreathingScreen
import com.example.breathingapp.ui.create.CreatePatternScreen
import com.example.breathingapp.ui.main.MainScreen
import com.example.breathingapp.ui.prep.SessionPrepScreen
import com.example.breathingapp.ui.settings.SettingsScreen
import com.example.breathingapp.ui.garden.GardenScreen
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.data.SettingsRepository
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.breathingapp.HomeRoute
import com.example.breathingapp.CreateRoute
import com.example.breathingapp.GardenRoute
import com.example.breathingapp.SettingsRoute
import com.example.breathingapp.PrepRoute
import com.example.breathingapp.BreathingRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { BreathingPatternRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
    
    // Check which route we are on to show/hide bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isHomeSelected = currentDestination?.hasRoute<HomeRoute>() == true
    val isCreateSelected = currentDestination?.hasRoute<CreateRoute>() == true
    val isGardenSelected = currentDestination?.hasRoute<GardenRoute>() == true
    val isSettingsSelected = currentDestination?.hasRoute<SettingsRoute>() == true

    val showBottomBar = isHomeSelected || isCreateSelected || isGardenSelected || isSettingsSelected

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("🏡") },
                        label = { Text("Ejercicios") },
                        selected = isHomeSelected,
                        onClick = {
                            navController.navigate(HomeRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("➕") },
                        label = { Text("Crear") },
                        selected = isCreateSelected,
                        onClick = {
                            navController.navigate(CreateRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("🌱") },
                        label = { Text("Jardín") },
                        selected = isGardenSelected,
                        onClick = {
                            navController.navigate(GardenRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("⚙️") },
                        label = { Text("Ajustes") },
                        selected = isSettingsSelected,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            if (settings.backgroundUri != null) {
                AsyncImage(
                    model = settings.backgroundUri,
                    contentDescription = "Fondo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark overlay to make text readable
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }

            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                composable<HomeRoute> {
                    val patterns by repository.allPatterns.collectAsState(initial = emptyList())
                    MainScreen(
                        patterns = patterns,
                        onPatternClick = { pattern ->
                            val index = patterns.indexOf(pattern)
                            if (index != -1) {
                                navController.navigate(PrepRoute(patternIndex = index))
                            }
                        }
                    )
                }
                composable<PrepRoute> { backStackEntry ->
                    val prepRoute = backStackEntry.toRoute<PrepRoute>()
                    val patterns by repository.allPatterns.collectAsState(initial = emptyList())
                    val pattern = patterns.getOrNull(prepRoute.patternIndex)
                    if (pattern != null) {
                        SessionPrepScreen(
                            pattern = pattern,
                            onStartSession = { modifiedPattern, duration ->
                                navController.navigate(
                                    BreathingRoute(
                                        name = modifiedPattern.name,
                                        inhaleMs = modifiedPattern.inhaleMs,
                                        holdInMs = modifiedPattern.holdInMs,
                                        exhaleMs = modifiedPattern.exhaleMs,
                                        holdOutMs = modifiedPattern.holdOutMs,
                                        durationMinutes = duration
                                    )
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable<CreateRoute> {
                    val coroutineScope = rememberCoroutineScope()
                    CreatePatternScreen(
                        onSavePattern = { newPattern ->
                            coroutineScope.launch {
                                repository.saveCustomPattern(newPattern)
                                navController.navigate(HomeRoute) {
                                    popUpTo(HomeRoute) { inclusive = false }
                                }
                            }
                        }
                    )
                }
                composable<GardenRoute> {
                    GardenScreen()
                }
                composable<SettingsRoute> {
                    SettingsScreen()
                }
                composable<BreathingRoute> { backStackEntry ->
                    val breathingRoute = backStackEntry.toRoute<BreathingRoute>()
                    val pattern = remember(breathingRoute) {
                        BreathingPattern(
                            name = breathingRoute.name,
                            inhaleMs = breathingRoute.inhaleMs,
                            holdInMs = breathingRoute.holdInMs,
                            exhaleMs = breathingRoute.exhaleMs,
                            holdOutMs = breathingRoute.holdOutMs
                        )
                    }
                    BreathingScreen(
                        pattern = pattern,
                        targetDurationMinutes = breathingRoute.durationMinutes,
                        onBack = { 
                            navController.popBackStack(HomeRoute, inclusive = false)
                        }
                    )
                }
            }
        }
    }
}
