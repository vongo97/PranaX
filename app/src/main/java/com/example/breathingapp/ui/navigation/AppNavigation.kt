package com.example.breathingapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.ui.settings.SettingsViewModel
import com.example.breathingapp.ui.profile.ProfileViewModel
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
import com.example.breathingapp.ProfileRoute
import com.example.breathingapp.PrepRoute
import com.example.breathingapp.BreathingRoute
import com.example.breathingapp.ui.profile.ProfileScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings

@Composable
fun AppNavigation(
    settingsViewModel: SettingsViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { BreathingPatternRepository(context) }
    val settings by settingsViewModel.settings.collectAsState()

    // Sincronización automática de base de datos en el arranque si está logueado
    LaunchedEffect(settings.loggedInUserEmail) {
        val email = settings.loggedInUserEmail
        if (email != null) {
            profileViewModel.syncStats(
                streak = settings.dailyStreak,
                sessions = settings.completedSessionsCount,
                minutes = settings.totalMinutesMeditated
            ) { /* No-op, el estado se actualiza en settingsFlow en segundo plano */ }
        }
    }
    
    // Check which route we are on to show/hide bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isHomeSelected = currentDestination?.hasRoute<HomeRoute>() == true
    val isCreateSelected = currentDestination?.hasRoute<CreateRoute>() == true
    val isGardenSelected = currentDestination?.hasRoute<GardenRoute>() == true
    val isSettingsSelected = currentDestination?.hasRoute<SettingsRoute>() == true

    val showBottomBar = isHomeSelected || isCreateSelected || isGardenSelected || isSettingsSelected

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFF0D1411),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Ejercicios") },
                        label = { Text("Ejercicios") },
                        selected = isHomeSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(HomeRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = "Crear") },
                        label = { Text("Crear") },
                        selected = isCreateSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(CreateRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Jardín") },
                        label = { Text("Jardín") },
                        selected = isGardenSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(GardenRoute) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes") },
                        selected = isSettingsSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        ),
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
                modifier = Modifier
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .fillMaxSize()
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
                    SettingsScreen(onNavigateToProfile = { navController.navigate(ProfileRoute) })
                }
                composable<ProfileRoute> {
                    ProfileScreen(onBack = { navController.popBackStack() })
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
