package com.example.breathingapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.breathingapp.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
    val coroutineScope = rememberCoroutineScope()

    var darkModeEnabled = settings.isDarkMode
    var bellEnabled = settings.isBellEnabled
    var backgroundAudioEnabled = settings.isBackgroundEnabled
    var vibrationEnabled = settings.isVibrationEnabled

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // You would typically persist permissions here for the URI, but for simplicity:
                coroutineScope.launch { repository.updateBackgroundUri(uri.toString()) }
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingsSection(title = "Tu Progreso") {
            SettingsInfoItem(title = "Racha Actual", value = "${settings.dailyStreak} días")
            SettingsInfoItem(title = "Sesiones Totales", value = "${settings.completedSessionsCount}")
            SettingsInfoItem(title = "Tiempo Meditado", value = "${settings.totalMinutesMeditated} min")
        }

        SettingsSection(title = "Personalización") {
            SettingsSwitch(
                title = "Tema Oscuro",
                subtitle = "Cambiar entre modo claro y oscuro",
                checked = darkModeEnabled,
                onCheckedChange = { 
                    coroutineScope.launch { repository.updateDarkMode(it) } 
                }
            )
            // Selector de Fondos de Pantalla
            SettingsItemClickable(
                title = "Fondo de Pantalla Personalizado",
                subtitle = "Elegir de tu galería"
            ) {
                photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
            
            Text("O elegir un fondo relajante:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { coroutineScope.launch { repository.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_forest") } }) { Text("Bosque") }
                Button(onClick = { coroutineScope.launch { repository.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_ocean") } }) { Text("Océano") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { coroutineScope.launch { repository.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_night") } }) { Text("Noche") }
                Button(onClick = { coroutineScope.launch { repository.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_mountains") } }) { Text("Montañas") }
            }
            Button(
                onClick = { coroutineScope.launch { repository.updateBackgroundUri(null) } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Quitar Fondo") }
        }

        SettingsSection(title = "Experiencia") {
            SettingsSwitch(
                title = "Campana Guía",
                subtitle = "Efectos de sonido al respirar",
                checked = bellEnabled,
                onCheckedChange = { 
                    coroutineScope.launch { repository.updateBell(it) } 
                }
            )
            SettingsSwitch(
                title = "Fondo de Bosque",
                subtitle = "Sonido relajante constante",
                checked = backgroundAudioEnabled,
                onCheckedChange = { 
                    coroutineScope.launch { repository.updateBackground(it) } 
                }
            )
            SettingsSwitch(
                title = "Vibración Háptica",
                subtitle = "Vibraciones al cambiar de fase",
                checked = vibrationEnabled,
                onCheckedChange = { 
                    coroutineScope.launch { repository.updateVibration(it) } 
                }
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsItemClickable(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onClick, shape = MaterialTheme.shapes.small) {
            Text("Cambiar")
        }
    }
}

@Composable
fun SettingsInfoItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
