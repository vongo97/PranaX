package com.example.breathingapp.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.breathingapp.data.SettingsRepository
import com.example.breathingapp.data.ReminderReceiver

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateBackgroundUri(uri.toString())
            }
        }
    )

    // Lanzador para solicitar permisos de notificación en Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Verificar alarmas exactas (Android 12+)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    alarmManager?.canScheduleExactAlarms() == true
                } else {
                    true
                }

                if (!canScheduleExact && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        android.widget.Toast.makeText(
                            context,
                            "Activa 'Alarmas y recordatorios' para sonar a la hora exacta",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        // Fallback seguro
                    }
                }

                viewModel.updateReminderEnabled(true)
                ReminderReceiver.scheduleReminder(context, settings.reminderHour, settings.reminderMinute)
            }
        }
    )

    // Configuración del diálogo de selección de hora nativo (formato de 12 horas AM/PM)
    val timePickerDialog = remember(settings.reminderHour, settings.reminderMinute) {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minuteOfHour ->
                viewModel.updateReminderTime(hourOfDay, minuteOfHour)
                if (settings.isReminderEnabled) {
                    ReminderReceiver.scheduleReminder(context, hourOfDay, minuteOfHour)
                }
            },
            settings.reminderHour,
            settings.reminderMinute,
            false // formato de 12 horas (AM/PM)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
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
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToProfile,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Sincronizar en la Nube ☁️")
            }
        }

        SettingsSection(title = "Personalización") {
            SettingsSwitch(
                title = "Tema Oscuro",
                subtitle = "Cambiar entre modo claro y oscuro",
                checked = settings.isDarkMode,
                onCheckedChange = { viewModel.updateDarkMode(it) }
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
                Button(onClick = { viewModel.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_forest") }) { Text("Bosque") }
                Button(onClick = { viewModel.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_ocean") }) { Text("Océano") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { viewModel.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_night") }) { Text("Noche") }
                Button(onClick = { viewModel.updateBackgroundUri("android.resource://com.example.breathingapp/drawable/bg_mountains") }) { Text("Montañas") }
            }
            Button(
                onClick = { viewModel.updateBackgroundUri(null) },
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
                        val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (!hasNotificationPermission) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            // Verificar alarmas exactas (Android 12+)
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                            val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                alarmManager?.canScheduleExactAlarms() == true
                            } else {
                                true
                            }

                            if (!canScheduleExact && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Activa 'Alarmas y recordatorios' para sonar a la hora exacta",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    // Fallback seguro
                                }
                            }

                            viewModel.updateReminderEnabled(true)
                            ReminderReceiver.scheduleReminder(context, settings.reminderHour, settings.reminderMinute)
                        }
                    } else {
                        viewModel.updateReminderEnabled(false)
                        ReminderReceiver.cancelReminder(context)
                    }
                }
            )
            
            if (settings.isReminderEnabled) {
                val formattedHour = remember(settings.reminderHour, settings.reminderMinute) {
                    val hour = settings.reminderHour
                    val minute = settings.reminderMinute
                    val suffix = if (hour >= 12) "PM" else "AM"
                    val displayHour = when {
                        hour == 0 -> 12
                        hour > 12 -> hour - 12
                        else -> hour
                    }
                    val displayMinute = minute.toString().padStart(2, '0')
                    "$displayHour:$displayMinute $suffix"
                }

                SettingsItemClickable(
                    title = "Hora de Alarma",
                    subtitle = "Programado a las $formattedHour"
                ) {
                    timePickerDialog.show()
                }
            }
        }

        SettingsSection(title = "Experiencia") {
            SettingsSwitch(
                title = "Meditación Guiada",
                subtitle = "Instrucciones de voz relajantes durante tu sesión",
                checked = settings.isGuidedMeditationEnabled,
                onCheckedChange = { viewModel.updateGuidedMeditation(it) }
            )
            SettingsSwitch(
                title = "Campana Guía",
                subtitle = "Efectos de sonido al respirar",
                checked = settings.isBellEnabled,
                onCheckedChange = { viewModel.updateBell(it) }
            )
            SettingsSwitch(
                title = "Sonido de Fondo",
                subtitle = "Reproducir un sonido ambiental relajante durante el ejercicio",
                checked = settings.isBackgroundEnabled,
                onCheckedChange = { viewModel.updateBackground(it) }
            )

            if (settings.isBackgroundEnabled) {
                Text(
                    text = "Tipo de Sonido:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = settings.backgroundAudioType == "forest",
                        onClick = { viewModel.updateBackgroundAudioType("forest") },
                        label = { Text("🌲 Bosque") }
                    )
                    FilterChip(
                        selected = settings.backgroundAudioType == "rain",
                        onClick = { viewModel.updateBackgroundAudioType("rain") },
                        label = { Text("🌧️ Lluvia") }
                    )
                    FilterChip(
                        selected = settings.backgroundAudioType == "ocean",
                        onClick = { viewModel.updateBackgroundAudioType("ocean") },
                        label = { Text("🌊 Océano") }
                    )
                }
            }
            SettingsSwitch(
                title = "Vibración Háptica",
                subtitle = "Vibraciones al cambiar de fase",
                checked = settings.isVibrationEnabled,
                onCheckedChange = { viewModel.updateVibration(it) }
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

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(SettingsRepository(context)) as T
    }
}
