package com.example.breathingapp.ui.garden

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.breathingapp.data.SettingsRepository
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GardenScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())

    val streak = settings.dailyStreak

    val (plantName, message) = when {
        streak == 0 -> Pair("Semilla Dormida", "Haz tu primer ejercicio para plantar tu semilla.")
        streak in 1..2 -> Pair("Pequeño Brote", "Tu constancia hace que empiece a crecer.")
        streak in 3..5 -> Pair("Planta Joven", "Racha de 3+ días. ¡Tu planta toma fuerza!")
        streak in 6..10 -> Pair("Árbol Fuerte", "Racha de 6+ días. Tienes un hábito sólido.")
        else -> Pair("Árbol Florecido", "Racha de 11+ días. ¡Tu constancia ha florecido!")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Jardín de la Calma",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.85f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CANVAS ANIMADO
                PlantCanvas(streak = streak, modifier = Modifier.size(200.dp))

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = plantName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Días de Racha: $streak 🔥",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun PlantCanvas(streak: Int, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val sway by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Suelo (Dirt)
        drawArc(
            color = Color(0xFF5D4037),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.2f, h * 0.85f),
            size = Size(w * 0.6f, h * 0.2f),
            style = Stroke(width = 8f)
        )
        drawOval(
            color = Color(0xFF3E2723),
            topLeft = Offset(w * 0.25f, h * 0.9f),
            size = Size(w * 0.5f, h * 0.1f)
        )

        val rootX = w / 2
        val rootY = h * 0.9f

        if (streak == 0) {
            // Semilla
            drawOval(
                color = Color(0xFF8D6E63),
                topLeft = Offset(rootX - 10f, rootY - 10f),
                size = Size(20f, 20f)
            )
            return@Canvas
        }

        val stemColor = Color(0xFF388E3C)
        val leafColor = Color(0xFF4CAF50)
        val flowerColor = Color(0xFFE91E63)

        // Altura del tallo según racha
        val targetHeight = when {
            streak in 1..2 -> h * 0.4f
            streak in 3..5 -> h * 0.6f
            else -> h * 0.75f
        }
        val topY = rootY - targetHeight
        val swayOffset = (sway * (targetHeight / 100f))

        // Dibujar Tallo
        val stemPath = Path().apply {
            moveTo(rootX, rootY)
            quadraticBezierTo(
                rootX + (swayOffset / 2), rootY - (targetHeight / 2),
                rootX + swayOffset, topY
            )
        }
        drawPath(
            path = stemPath,
            color = stemColor,
            style = Stroke(width = if (streak > 5) 16f else 8f, cap = StrokeCap.Round)
        )

        // Función para dibujar hojas
        fun drawLeaf(cx: Float, cy: Float, angleDegrees: Float, scale: Float = 1f) {
            val leafPath = Path()
            leafPath.moveTo(cx, cy)
            val rad = Math.toRadians(angleDegrees.toDouble())
            val tipX = cx + (cos(rad) * 40 * scale).toFloat()
            val tipY = cy + (sin(rad) * 40 * scale).toFloat()
            
            // Aproximación de hoja con curvas
            leafPath.quadraticBezierTo(cx + 10*scale, cy - 20*scale, tipX, tipY)
            leafPath.quadraticBezierTo(cx + 20*scale, cy + 10*scale, cx, cy)

            drawPath(path = leafPath, color = leafColor)
        }

        // Hojas
        if (streak >= 1) {
            val leaf1Y = rootY - (targetHeight * 0.3f)
            drawLeaf(rootX + (swayOffset * 0.3f), leaf1Y, -30f, if (streak > 2) 1.5f else 1f)
            
            val leaf2Y = rootY - (targetHeight * 0.6f)
            drawLeaf(rootX + (swayOffset * 0.6f), leaf2Y, 210f, if (streak > 2) 1.5f else 1f)
        }

        if (streak >= 3) {
            val leaf3Y = rootY - (targetHeight * 0.8f)
            drawLeaf(rootX + (swayOffset * 0.8f), leaf3Y, -20f, 1.2f)
            val leaf4Y = rootY - (targetHeight * 0.45f)
            drawLeaf(rootX + (swayOffset * 0.45f), leaf4Y, 200f, 1.2f)
        }

        if (streak >= 6) {
            // Copa de árbol sencilla (círculos)
            drawCircle(color = leafColor, radius = 50f, center = Offset(rootX + swayOffset, topY))
            drawCircle(color = stemColor.copy(alpha=0.8f), radius = 35f, center = Offset(rootX + swayOffset - 30f, topY + 10f))
            drawCircle(color = leafColor.copy(alpha=0.9f), radius = 40f, center = Offset(rootX + swayOffset + 30f, topY + 5f))
            drawCircle(color = stemColor.copy(alpha=0.7f), radius = 45f, center = Offset(rootX + swayOffset, topY - 30f))
        }

        if (streak >= 11) {
            // Flores
            drawCircle(color = flowerColor, radius = 10f, center = Offset(rootX + swayOffset - 20f, topY - 20f))
            drawCircle(color = flowerColor, radius = 8f, center = Offset(rootX + swayOffset + 25f, topY - 10f))
            drawCircle(color = flowerColor, radius = 12f, center = Offset(rootX + swayOffset + 5f, topY + 15f))
        }
    }
}
