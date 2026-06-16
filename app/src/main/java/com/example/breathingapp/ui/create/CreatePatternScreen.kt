package com.example.breathingapp.ui.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.breathingapp.domain.BreathingPattern
import kotlinx.coroutines.launch

@Composable
fun CreatePatternScreen(
    onSavePattern: (BreathingPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var inhaleSec by remember { mutableFloatStateOf(4f) }
    var holdInSec by remember { mutableFloatStateOf(4f) }
    var exhaleSec by remember { mutableFloatStateOf(4f) }
    var holdOutSec by remember { mutableFloatStateOf(4f) }
    
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Crear Ejercicio",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del patrón") },
            placeholder = { Text("Ej. Relajación nocturna") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        TimeSliderConfig("Inhalar", inhaleSec) { inhaleSec = it }
        TimeSliderConfig("Retener (con aire)", holdInSec) { holdInSec = it }
        TimeSliderConfig("Exhalar", exhaleSec) { exhaleSec = it }
        TimeSliderConfig("Retener (sin aire)", holdOutSec) { holdOutSec = it }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val newPattern = BreathingPattern(
                    name = name.ifBlank { "Mi Patrón" },
                    inhaleMs = (inhaleSec * 1000).toLong(),
                    holdInMs = (holdInSec * 1000).toLong(),
                    exhaleMs = (exhaleSec * 1000).toLong(),
                    holdOutMs = (holdOutSec * 1000).toLong()
                )
                onSavePattern(newPattern)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = inhaleSec > 0f || exhaleSec > 0f
        ) {
            Text("Guardar Patrón", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TimeSliderConfig(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Text(text = "${value.toInt()} s", color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..20f,
            steps = 19 // Steps between 0 and 20 (1-19) to snap to whole numbers
        )
    }
}
