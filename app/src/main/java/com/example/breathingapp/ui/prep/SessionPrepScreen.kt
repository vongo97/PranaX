package com.example.breathingapp.ui.prep

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.example.breathingapp.data.SettingsRepository
import com.example.breathingapp.domain.BreathingPattern
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPrepScreen(
    pattern: BreathingPattern,
    onStartSession: (BreathingPattern, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
    val coroutineScope = rememberCoroutineScope()

    var inhaleMs by remember { mutableStateOf(pattern.inhaleMs) }
    var holdInMs by remember { mutableStateOf(pattern.holdInMs) }
    var exhaleMs by remember { mutableStateOf(pattern.exhaleMs) }
    var holdOutMs by remember { mutableStateOf(pattern.holdOutMs) }

    var durationMinutes by remember { mutableStateOf(5) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(pattern.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("⬅️")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Phase Adjustments
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    PhaseAdjuster("Inhala", inhaleMs) { inhaleMs = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    PhaseAdjuster("Retén", holdInMs) { holdInMs = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    PhaseAdjuster("Exhala", exhaleMs) { exhaleMs = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    PhaseAdjuster("Espera", holdOutMs) { holdOutMs = it }
                }
            }

            // Duration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duración", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { if (durationMinutes > 1) durationMinutes -= 1 }) { Text("➖") }
                        Text("$durationMinutes min", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                        FilledTonalIconButton(onClick = { if (durationMinutes < 60) durationMinutes += 1 }) { Text("➕") }
                    }
                }
            }

            // Quick Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibrar ⚙️", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = settings.isVibrationEnabled,
                            onCheckedChange = { coroutineScope.launch { repository.updateVibration(it) } }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Campana Guía 🔔", style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = settings.isBellEnabled,
                                onCheckedChange = { coroutineScope.launch { repository.updateBell(it) } }
                            )
                        }
                        
                        if (settings.isBellEnabled) {
                            BellSelector(
                                currentType = settings.bellType,
                                onTypeSelected = { coroutineScope.launch { repository.updateBellType(it) } }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fondo de Bosque 🌲", style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = settings.isBackgroundEnabled,
                                onCheckedChange = { coroutineScope.launch { repository.updateBackground(it) } }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val customPattern = BreathingPattern(
                        name = pattern.name,
                        inhaleMs = inhaleMs,
                        holdInMs = holdInMs,
                        exhaleMs = exhaleMs,
                        holdOutMs = holdOutMs
                    )
                    onStartSession(customPattern, durationMinutes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = CircleShape
            ) {
                Text("Comenzar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PhaseAdjuster(label: String, valueMs: Long, onValueChange: (Long) -> Unit) {
    val seconds = (valueMs / 1000).toInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { if (seconds > 0) onValueChange((seconds - 1) * 1000L) }) { Text("➖") }
            Text("$seconds", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            FilledTonalIconButton(onClick = { if (seconds < 30) onValueChange((seconds + 1) * 1000L) }) { Text("➕") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BellSelector(currentType: String, onTypeSelected: (String) -> Unit) {
    val modes = listOf("bowl" to "🥣 Cuenco Tibetano", "chime" to "🔔 Campana de Meditación")
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val soundPool = remember {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        android.media.SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    
    val soundIds = remember { mutableMapOf<String, Int>() }
    
    DisposableEffect(Unit) {
        soundIds["chime"] = soundPool.load(context, com.example.breathingapp.R.raw.chime, 1)
        soundIds["bowl"] = soundPool.load(context, com.example.breathingapp.R.raw.bowl_high, 1)
        onDispose {
            soundPool.release()
        }
    }
    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = modes.find { it.first == currentType }?.second ?: "🥣 Cuenco Tibetano",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                modes.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption.second) },
                        onClick = {
                            onTypeSelected(selectionOption.first)
                            expanded = false
                            
                            // PREVIEW SONIDO AUTOMÁTICO AL SELECCIONAR
                            val id = soundIds[selectionOption.first]
                            if (id != null && id != 0) {
                                soundPool.play(id, 1f, 1f, 1, 0, 1f)
                            }
                        }
                    )
                }
            }
        }
    }
}
