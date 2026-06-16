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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.breathingapp.data.SettingsRepository
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
    val coroutineScope = rememberCoroutineScope()

    val streak = settings.dailyStreak
    val currentSeed = settings.seedType

    // Animación para el riego de la planta
    val waterAnim = remember { Animatable(0f) }
    var isWatering by remember { mutableStateOf(false) }

    val (plantName, message) = when {
        streak == 0 -> Pair("Semilla Dormida", "Haz tu primer ejercicio para plantar tu semilla.")
        streak in 1..2 -> Pair("Pequeño Brote", "Tu constancia hace que empiece a crecer.")
        streak in 3..5 -> Pair("Planta Joven", "Racha de 3+ días. ¡Tu planta toma fuerza!")
        streak in 6..10 -> Pair("Estructura Fuerte", "Racha de 6+ días. Tienes un hábito sólido.")
        else -> Pair("Planta Florecida", "Racha de 11+ días. ¡Tu constancia ha florecido!")
    }

    // Nombre personalizado según la semilla
    val seedDisplayName = when (currentSeed) {
        "bonsai" -> "Bonsái Zen"
        "cactus" -> "Cactus del Desierto"
        else -> "Flor de la Calma"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Jardín de la Calma",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Selector de Semilla
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Semilla:", fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = currentSeed == "flower",
                    onClick = { coroutineScope.launch { repository.updateSeedType("flower") } },
                    label = { Text("🌸 Flor") }
                )
                FilterChip(
                    selected = currentSeed == "bonsai",
                    onClick = { coroutineScope.launch { repository.updateSeedType("bonsai") } },
                    label = { Text("🌳 Bonsái") }
                )
                FilterChip(
                    selected = currentSeed == "cactus",
                    onClick = { coroutineScope.launch { repository.updateSeedType("cactus") } },
                    label = { Text("🌵 Cactus") }
                )
            }
        }

        // Card de la Planta
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // CANVAS DE LA PLANTA
                    PlantCanvas(
                        streak = streak,
                        seedType = currentSeed,
                        waterProgress = waterAnim.value,
                        modifier = Modifier.size(240.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$seedDisplayName ($plantName)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Días de Racha: $streak 🔥",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón interactivo de riego
            Button(
                onClick = {
                    if (!isWatering) {
                        isWatering = true
                        coroutineScope.launch {
                            waterAnim.snapTo(0f)
                            waterAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(1500, easing = LinearOutSlowInEasing)
                            )
                            isWatering = false
                            waterAnim.snapTo(0f)
                        }
                    }
                },
                enabled = streak > 0 && !isWatering,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isWatering) "Regando... 💦" else "Regar 💧")
            }
        }
    }
}

@Composable
fun PlantCanvas(
    streak: Int,
    seedType: String,
    waterProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "swayTransition")
    val sway by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayAngle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Dibujar Suelo / Tierra
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

        // 2. Si la racha es 0, solo se dibuja la semilla
        if (streak == 0) {
            drawOval(
                color = Color(0xFF8D6E63),
                topLeft = Offset(rootX - 12f, rootY - 16f),
                size = Size(24f, 24f)
            )
            // Brote pequeño saliendo de la semilla
            return@Canvas
        }

        // Altura y curvatura del crecimiento según la racha
        val targetHeight = when {
            streak in 1..2 -> h * 0.35f
            streak in 3..5 -> h * 0.55f
            else -> h * 0.72f
        }
        val topY = rootY - targetHeight
        val swayOffset = (sway * (targetHeight / 120f))

        // 3. Dibujar Planta según el Tipo de Semilla
        when (seedType) {
            "bonsai" -> {
                // Tallo o tronco de madera (marrón)
                val stemPath = Path().apply {
                    moveTo(rootX, rootY)
                    // Tronco retorcido de bonsái
                    quadraticTo(
                        rootX - 30f + (swayOffset * 0.3f), rootY - (targetHeight * 0.4f),
                        rootX + swayOffset, topY
                    )
                }
                drawPath(
                    path = stemPath,
                    color = Color(0xFF6D4C41),
                    style = Stroke(width = if (streak > 5) 24f else 12f, cap = StrokeCap.Round)
                )

                // Ramificaciones laterales si racha >= 3
                if (streak >= 3) {
                    val branch1Y = rootY - (targetHeight * 0.5f)
                    val branchPath1 = Path().apply {
                        moveTo(rootX - 10f + (swayOffset * 0.5f), branch1Y)
                        quadraticTo(
                            rootX - 50f, branch1Y - 20f,
                            rootX - 60f + (swayOffset * 0.7f), branch1Y - 40f
                        )
                    }
                    drawPath(path = branchPath1, color = Color(0xFF6D4C41), style = Stroke(width = 8f, cap = StrokeCap.Round))
                    
                    // Nube de hojas en rama izquierda
                    drawCircle(color = Color(0xFF1B5E20), radius = 25f, center = Offset(rootX - 60f + (swayOffset * 0.7f), branch1Y - 45f))
                    drawCircle(color = Color(0xFF2E7D32), radius = 20f, center = Offset(rootX - 75f + (swayOffset * 0.7f), branch1Y - 35f))
                }

                if (streak >= 6) {
                    val branch2Y = rootY - (targetHeight * 0.7f)
                    val branchPath2 = Path().apply {
                        moveTo(rootX + 10f + (swayOffset * 0.7f), branch2Y)
                        quadraticTo(
                            rootX + 50f, branch2Y - 10f,
                            rootX + 60f + (swayOffset * 0.9f), branch2Y - 30f
                        )
                    }
                    drawPath(path = branchPath2, color = Color(0xFF6D4C41), style = Stroke(width = 6f, cap = StrokeCap.Round))

                    // Nube de hojas en rama derecha
                    drawCircle(color = Color(0xFF2E7D32), radius = 22f, center = Offset(rootX + 60f + (swayOffset * 0.9f), branch2Y - 35f))
                }

                // Follaje principal en la copa
                if (streak >= 1) {
                    val r = if (streak > 5) 45f else 30f
                    drawCircle(color = Color(0xFF2E7D32), radius = r, center = Offset(rootX + swayOffset, topY))
                    drawCircle(color = Color(0xFF388E3C), radius = r * 0.8f, center = Offset(rootX + swayOffset - r * 0.5f, topY - r * 0.2f))
                    drawCircle(color = Color(0xFF1B5E20), radius = r * 0.8f, center = Offset(rootX + swayOffset + r * 0.5f, topY - r * 0.1f))
                }

                // Frutos del Bonsái (círculos rojos si streak >= 11)
                if (streak >= 11) {
                    drawCircle(color = Color(0xFFD32F2F), radius = 6f, center = Offset(rootX + swayOffset - 20f, topY - 10f))
                    drawCircle(color = Color(0xFFD32F2F), radius = 5f, center = Offset(rootX + swayOffset + 25f, topY - 5f))
                    drawCircle(color = Color(0xFFD32F2F), radius = 6f, center = Offset(rootX - 50f + (swayOffset * 0.7f), rootY - (targetHeight * 0.5f) - 50f))
                }
            }
            "cactus" -> {
                // Tallo o cuerpo de cactus (Verde Oliva)
                val cWidth = if (streak > 5) 40f else 28f
                val stemColor = Color(0xFF2E7D32)
                
                // Dibujar cuerpo central
                val cactusPath = Path().apply {
                    moveTo(rootX - cWidth/2, rootY)
                    quadraticTo(
                        rootX - cWidth/2 + (swayOffset * 0.5f), rootY - targetHeight/2,
                        rootX - cWidth/3 + swayOffset, topY
                    )
                    // Copa redondeada
                    lineTo(rootX + cWidth/3 + swayOffset, topY)
                    quadraticTo(
                        rootX + cWidth/2 + (swayOffset * 0.5f), rootY - targetHeight/2,
                        rootX + cWidth/2, rootY
                    )
                }
                drawPath(path = cactusPath, color = stemColor)

                // Brazos laterales del cactus si racha >= 3
                if (streak >= 3) {
                    val armY = rootY - (targetHeight * 0.45f)
                    val armX = rootX - cWidth/2 + (swayOffset * 0.45f)
                    // Brazo izquierdo (curva arriba)
                    val leftArm = Path().apply {
                        moveTo(armX, armY)
                        quadraticTo(armX - 35f, armY - 5f, armX - 35f, armY - 45f)
                        lineTo(armX - 20f, armY - 45f)
                        quadraticTo(armX - 20f, armY - 15f, armX, armY - 10f)
                    }
                    drawPath(path = leftArm, color = stemColor)
                }

                if (streak >= 6) {
                    val armY2 = rootY - (targetHeight * 0.65f)
                    val armX2 = rootX + cWidth/2 + (swayOffset * 0.65f)
                    // Brazo derecho (curva arriba)
                    val rightArm = Path().apply {
                        moveTo(armX2, armY2)
                        quadraticTo(armX2 + 35f, armY2 - 5f, armX2 + 35f, armY2 - 45f)
                        lineTo(armX2 + 20f, armY2 - 45f)
                        quadraticTo(armX2 + 20f, armY2 - 15f, armX2, armY2 - 10f)
                    }
                    drawPath(path = rightArm, color = stemColor)
                }

                // Dibujar espinas (pequeñas rayitas blancas)
                if (streak >= 1) {
                    val spineColor = Color.White.copy(alpha = 0.85f)
                    // Espinas del cuerpo principal
                    val spineY1 = rootY - (targetHeight * 0.25f)
                    drawLine(spineColor, Offset(rootX - cWidth/2 - 5f, spineY1), Offset(rootX - cWidth/2 + 5f, spineY1 + 5f), strokeWidth = 3f)
                    drawLine(spineColor, Offset(rootX + cWidth/2 - 5f, spineY1), Offset(rootX + cWidth/2 + 5f, spineY1 - 5f), strokeWidth = 3f)

                    val spineY2 = rootY - (targetHeight * 0.5f)
                    drawLine(spineColor, Offset(rootX - 5f + (swayOffset * 0.5f), spineY2), Offset(rootX + 5f + (swayOffset * 0.5f), spineY2 - 5f), strokeWidth = 3f)

                    val spineY3 = rootY - (targetHeight * 0.75f)
                    drawLine(spineColor, Offset(rootX - cWidth/2 - 3f + (swayOffset * 0.75f), spineY3), Offset(rootX - cWidth/2 + 7f + (swayOffset * 0.75f), spineY3 + 3f), strokeWidth = 3f)
                    drawLine(spineColor, Offset(rootX + cWidth/2 - 7f + (swayOffset * 0.75f), spineY3), Offset(rootX + cWidth/2 + 3f + (swayOffset * 0.75f), spineY3 - 3f), strokeWidth = 3f)
                }

                // Gran flor roja en la copa si streak >= 11
                if (streak >= 11) {
                    val flowerX = rootX + swayOffset
                    drawCircle(color = Color(0xFFE53935), radius = 18f, center = Offset(flowerX, topY - 10f))
                    drawCircle(color = Color(0xFFFFB300), radius = 8f, center = Offset(flowerX, topY - 10f))
                }
            }
            else -> {
                // Flor tradicional (versión original mejorada)
                val stemPath = Path().apply {
                    moveTo(rootX, rootY)
                    quadraticTo(
                        rootX + (swayOffset / 2), rootY - (targetHeight / 2),
                        rootX + swayOffset, topY
                    )
                }
                drawPath(
                    path = stemPath,
                    color = Color(0xFF388E3C),
                    style = Stroke(width = if (streak > 5) 16f else 8f, cap = StrokeCap.Round)
                )

                // Hojas laterales
                fun drawLeaf(cx: Float, cy: Float, angleDegrees: Float, scale: Float = 1f) {
                    val leafPath = Path()
                    leafPath.moveTo(cx, cy)
                    val rad = Math.toRadians(angleDegrees.toDouble())
                    val tipX = cx + (cos(rad) * 45 * scale).toFloat()
                    val tipY = cy + (sin(rad) * 45 * scale).toFloat()
                    
                    leafPath.quadraticTo(cx + 10 * scale, cy - 20 * scale, tipX, tipY)
                    leafPath.quadraticTo(cx + 20 * scale, cy + 10 * scale, cx, cy)

                    drawPath(path = leafPath, color = Color(0xFF4CAF50))
                }

                if (streak >= 1) {
                    val leaf1Y = rootY - (targetHeight * 0.3f)
                    drawLeaf(rootX + (swayOffset * 0.3f), leaf1Y, -30f, if (streak > 2) 1.4f else 1f)
                    
                    val leaf2Y = rootY - (targetHeight * 0.6f)
                    drawLeaf(rootX + (swayOffset * 0.6f), leaf2Y, 210f, if (streak > 2) 1.4f else 1f)
                }

                if (streak >= 3) {
                    val leaf3Y = rootY - (targetHeight * 0.8f)
                    drawLeaf(rootX + (swayOffset * 0.8f), leaf3Y, -20f, 1.2f)
                    val leaf4Y = rootY - (targetHeight * 0.45f)
                    drawLeaf(rootX + (swayOffset * 0.45f), leaf4Y, 200f, 1.2f)
                }

                if (streak >= 6) {
                    // Flores o capullo en la copa
                    drawCircle(color = Color(0xFF81C784), radius = 35f, center = Offset(rootX + swayOffset, topY))
                    drawCircle(color = Color(0xFF4CAF50), radius = 25f, center = Offset(rootX + swayOffset - 20f, topY + 5f))
                    drawCircle(color = Color(0xFF4CAF50), radius = 28f, center = Offset(rootX + swayOffset + 20f, topY + 5f))
                }

                if (streak >= 11) {
                    // Flor florecida rosa/magenta brillante
                    val fX = rootX + swayOffset
                    drawCircle(color = Color(0xFFE91E63), radius = 25f, center = Offset(fX, topY))
                    drawCircle(color = Color(0xFFF48FB1), radius = 15f, center = Offset(fX - 15f, topY - 15f))
                    drawCircle(color = Color(0xFFF48FB1), radius = 15f, center = Offset(fX + 15f, topY - 15f))
                    drawCircle(color = Color(0xFFF48FB1), radius = 15f, center = Offset(fX - 15f, topY + 15f))
                    drawCircle(color = Color(0xFFF48FB1), radius = 15f, center = Offset(fX + 15f, topY + 15f))
                    drawCircle(color = Color(0xFFFFEB3B), radius = 12f, center = Offset(fX, topY)) // Centro amarillo
                }
            }
        }

        // 4. Dibujar animación de gotas de agua al regar
        if (waterProgress > 0f && waterProgress < 1f) {
            val dripColor = Color(0xFF29B6F6).copy(alpha = 0.8f)
            val dripLength = 25f
            
            // Dibujamos regadera arriba a la derecha que se inclina
            val canX = w * 0.8f - (waterProgress * 30f)
            val canY = h * 0.15f + (waterProgress * 10f)
            
            // Regadera (Cuerpo)
            drawRoundRect(
                color = Color(0xFF78909C),
                topLeft = Offset(canX - 25f, canY - 15f),
                size = Size(50f, 30f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            // Tubo de riego inclinado hacia la planta
            drawLine(
                color = Color(0xFF546E7A),
                start = Offset(canX - 25f, canY + 5f),
                end = Offset(canX - 55f, canY + 25f),
                strokeWidth = 6f
            )

            // Varias columnas de gotas cayendo en base al progreso
            val dropY1 = h * 0.2f + (waterProgress * h * 0.6f)
            val dropY2 = h * 0.2f + (((waterProgress + 0.25f) % 1f) * h * 0.6f)
            val dropY3 = h * 0.2f + (((waterProgress + 0.5f) % 1f) * h * 0.6f)

            if (dropY1 < rootY) {
                drawLine(dripColor, Offset(w * 0.45f, dropY1), Offset(w * 0.45f, dropY1 + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(dripColor, Offset(w * 0.55f, dropY1 - 40f), Offset(w * 0.55f, dropY1 - 40f + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            if (dropY2 < rootY) {
                drawLine(dripColor, Offset(w * 0.38f, dropY2), Offset(w * 0.38f, dropY2 + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(dripColor, Offset(w * 0.62f, dropY2 - 30f), Offset(w * 0.62f, dropY2 - 30f + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            if (dropY3 < rootY) {
                drawLine(dripColor, Offset(w * 0.5f, dropY3), Offset(w * 0.5f, dropY3 + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(dripColor, Offset(w * 0.42f, dropY3 - 50f), Offset(w * 0.42f, dropY3 - 50f + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(dripColor, Offset(w * 0.58f, dropY3 - 10f), Offset(w * 0.58f, dropY3 - 10f + dripLength), strokeWidth = 4f, cap = StrokeCap.Round)
            }
        }
    }
}
