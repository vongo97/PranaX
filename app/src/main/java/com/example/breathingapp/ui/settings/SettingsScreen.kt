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
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.breathingapp.data.SettingsRepository
import com.example.breathingapp.data.ReminderReceiver
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
                coroutineScope.launch { repository.updateBackgroundUri(uri.toString()) }
            }
        }
    )

    // Lanzador para solicitar permisos de notificación en Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                coroutineScope.launch {
                    repository.updateReminderEnabled(true)
                    ReminderReceiver.scheduleReminder(context, settings.reminderHour, settings.reminderMinute)
                }
            }
        }
    )

    // Configuración del diálogo de selección de hora nativo
    val timePickerDialog = remember(settings.reminderHour, settings.reminderMinute) {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minuteOfHour ->
                coroutineScope.launch {
                    repository.updateReminderTime(hourOfDay, minuteOfHour)
                    if (settings.isReminderEnabled) {
                        ReminderReceiver.scheduleReminder(context, hourOfDay, minuteOfHour)
                    }
                }
            },
            settings.reminderHour,
            settings.reminderMinute,
            true // formato de 24 horas
        )
    }

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

        SettingsSection(title = "Recordatorios 🌱") {
            SettingsSwitch(
                title = "Recordatorio Diario",
                subtitle = "Notificación para hacer tu ejercicio diario de respiración",
                checked = settings.isReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val permission = android.Manifest.permission.POST_NOTIFICATIONS
                            val isGranted = context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (isGranted) {
                                coroutineScope.launch {
                                    repository.updateReminderEnabled(true)
                                    ReminderReceiver.scheduleReminder(context, settings.reminderHour, settings.reminderMinute)
                                }
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        } else {
                            coroutineScope.launch {
                                repository.updateReminderEnabled(true)
                                ReminderReceiver.scheduleReminder(context, settings.reminderHour, settings.reminderMinute)
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            repository.updateReminderEnabled(false)
                            ReminderReceiver.cancelReminder(context)
                        }
                    }
                }
            )
            
            if (settings.isReminderEnabled) {
                SettingsItemClickable(
                    title = "Hora de Alarma",
                    subtitle = "Programado a las ${settings.reminderHour.toString().padStart(2, '0')}:${settings.reminderMinute.toString().padStart(2, '0')}"
                ) {
                    timePickerDialog.show()
                }
            }
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
                title = "Sonido de Fondo",
                subtitle = "Reproducir un sonido ambiental relajante durante el ejercicio",
                checked = backgroundAudioEnabled,
                onCheckedChange = { 
                    coroutineScope.launch { repository.updateBackground(it) } 
                }
            )

            if (backgroundAudioEnabled) {
                Text(
                    text = "Tipo de Sonido:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = settings.backgroundAudioType == "forest",
                        onClick = { coroutineScope.launch { repository.updateBackgroundAudioType("forest") } },
                        label = { Text("🌲 Bosque") }
                    )
                    FilterChip(
                        selected = settings.backgroundAudioType == "rain",
                        onClick = { coroutineScope.launch { repository.updateBackgroundAudioType("rain") } },
                        label = { Text("🌧️ Lluvia") }
                    )
                    FilterChip(
                        selected = settings.backgroundAudioType == "ocean",
                        onClick = { coroutineScope.launch { repository.updateBackgroundAudioType("ocean") } },
                        label = { Text("🌊 Océano") }
                    )
                }
            }
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
