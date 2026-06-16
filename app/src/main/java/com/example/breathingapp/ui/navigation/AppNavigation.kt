package com.example.breathingapp.ui.navigation

import androidx.compose.foundation.layout.padding

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { BreathingPatternRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
    
    // Check which route we are on to show/hide bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var activePattern by remember { mutableStateOf<BreathingPattern?>(null) }
    var sessionDurationMinutes by remember { mutableStateOf(5) }

    val showBottomBar = currentRoute in listOf("home", "create", "garden", "settings")

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("🏡") },
                        label = { Text("Ejercicios") },
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("➕") },
                        label = { Text("Crear") },
                        selected = currentRoute == "create",
                        onClick = {
                            navController.navigate("create") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("🌱") },
                        label = { Text("Jardín") },
                        selected = currentRoute == "garden",
                        onClick = {
                            navController.navigate("garden") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("⚙️") },
                        label = { Text("Ajustes") },
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo("home")
                                launchSingleTop = true
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
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)))
            }

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
            composable("home") {
                val patterns by repository.allPatterns.collectAsState(initial = emptyList())
                MainScreen(
                    patterns = patterns,
                    onPatternClick = { pattern ->
                        activePattern = pattern
                        navController.navigate("prep")
                    }
                )
            }
            composable("prep") {
                if (activePattern != null) {
                    SessionPrepScreen(
                        pattern = activePattern!!,
                        onStartSession = { modifiedPattern, duration ->
                            activePattern = modifiedPattern
                            sessionDurationMinutes = duration
                            navController.navigate("breathing")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("create") {
                val coroutineScope = rememberCoroutineScope()
                CreatePatternScreen(
                    onSavePattern = { newPattern ->
                        coroutineScope.launch {
                            repository.saveCustomPattern(newPattern)
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    }
                )
            }
            composable("garden") {
                GardenScreen()
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("breathing") {
                if (activePattern != null) {
                    BreathingScreen(
                        pattern = activePattern!!,
                        targetDurationMinutes = sessionDurationMinutes,
                        onBack = { 
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                } else {
                    Text("Cargando o Patrón no encontrado...", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
}
