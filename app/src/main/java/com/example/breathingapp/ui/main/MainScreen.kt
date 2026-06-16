package com.example.breathingapp.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.breathingapp.domain.BreathingPattern

@Composable
fun MainScreen(
    patterns: List<BreathingPattern>,
    onPatternClick: (BreathingPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ejercicios de Respiración",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(patterns) { pattern ->
                PatternCard(pattern = pattern, onClick = { onPatternClick(pattern) })
            }
        }
    }
}

@Composable
fun PatternCard(pattern: BreathingPattern, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val inhaleSec = pattern.inhaleMs / 1000
            val holdInSec = pattern.holdInMs / 1000
            val exhaleSec = pattern.exhaleMs / 1000
            val holdOutSec = pattern.holdOutMs / 1000
            
            Text(
                text = "Patrón: Inhalar ${inhaleSec}s - Retener ${holdInSec}s - Exhalar ${exhaleSec}s - Retener ${holdOutSec}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
