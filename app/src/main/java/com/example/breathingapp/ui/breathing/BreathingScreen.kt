package com.example.breathingapp.ui.breathing

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.domain.BoxBreathing
import com.example.breathingapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun BreathingScreen(
    pattern: BreathingPattern,
    targetDurationMinutes: Int = 5,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BreathingViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsRepository = remember { com.example.breathingapp.data.SettingsRepository(context) }
    val settings by settingsRepository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
    val coroutineScope = rememberCoroutineScope()

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // WAKELOCK: Evita que la pantalla se apague mientras esta vista esté abierta
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            vibrator.cancel() // Detener vibración al salir de la pantalla
        }
    }

    val mediaPlayerBg = remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    val soundPool = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    
    val soundIdHigh = remember { mutableStateOf(0) }
    val soundIdLow = remember { mutableStateOf(0) }

    DisposableEffect(settings.isBackgroundEnabled, settings.backgroundAudioType) {
        if (settings.isBackgroundEnabled) {
            val audioRes = when (settings.backgroundAudioType) {
                "rain" -> R.raw.rain_bg
                "ocean" -> R.raw.ocean_bg
                else -> R.raw.forest_bg
            }
            val mp = android.media.MediaPlayer.create(context, audioRes)
            mp?.isLooping = true
            mp?.setVolume(0.5f, 0.5f)
            mediaPlayerBg.value = mp
            if (viewModel.isRunning) {
                mp?.start()
            }
        }
        onDispose {
            mediaPlayerBg.value?.release()
            mediaPlayerBg.value = null
        }
    }

    DisposableEffect(settings.bellType) {
        val resHigh = if (settings.bellType == "chime") R.raw.chime else R.raw.bowl_high
        val resLow = if (settings.bellType == "chime") R.raw.chime else R.raw.bowl_low
        
        val idH = soundPool.load(context, resHigh, 1)
        val idL = soundPool.load(context, resLow, 1)
        
        soundIdHigh.value = idH
        soundIdLow.value = idL
        
        onDispose {
            soundPool.unload(idH)
            soundPool.unload(idL)
        }
    }

    LaunchedEffect(viewModel.isRunning) {
        if (viewModel.isRunning) {
            viewModel.runBreathingLoop(vibrator, settings, mediaPlayerBg.value, soundPool, soundIdHigh.value, soundIdLow.value) {
                // Auto-complete logic
                coroutineScope.launch {
                    val minutes = Math.ceil(viewModel.elapsedSessionTimeMs / 60000.0).toInt()
                    settingsRepository.recordSessionCompletion(minutes)
                    onBack()
                }
            }
        }
    }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by animateFloatAsState(
        targetValue = when (viewModel.currentPhase) {
            BreathingPhase.PREPARE -> 1.0f
            BreathingPhase.INHALE -> 1.5f
            BreathingPhase.HOLD_IN -> 1.5f
            BreathingPhase.EXHALE -> 1.0f
            BreathingPhase.HOLD_OUT -> 1.0f
            BreathingPhase.IDLE -> 1.0f
        },
        animationSpec = tween(
            durationMillis = when (viewModel.currentPhase) {
                BreathingPhase.PREPARE -> 3000
                BreathingPhase.INHALE -> pattern.inhaleMs.toInt()
                BreathingPhase.EXHALE -> pattern.exhaleMs.toInt()
                else -> 0
            },
            easing = LinearEasing
        ), label = "circleScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBack) {
                Text("Atrás", color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = pattern.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Text
        if (viewModel.targetDurationMs > 0) {
            val elapsedSec = viewModel.elapsedSessionTimeMs / 1000
            val targetSec = viewModel.targetDurationMs / 1000
            Text(
                text = "${elapsedSec / 60}:${(elapsedSec % 60).toString().padStart(2, '0')} / ${targetSec / 60}:00",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Animation Container
        Box(
            modifier = Modifier
                .size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated Circle
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            )

            // Text inside
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.currentPhase.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                if (viewModel.isRunning) {
                    val secondsLeft = Math.ceil(viewModel.timeLeftInPhaseMs / 1000.0).toInt()
                    Text(
                        text = "$secondsLeft",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.toggleRunning(pattern, vibrator, settings, targetDurationMinutes, mediaPlayerBg.value, soundPool, soundIdHigh.value, soundIdLow.value) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp)
        ) {
            Text(if (viewModel.isRunning) "Pausar" else "Comenzar", style = MaterialTheme.typography.titleMedium)
        }

        if (!viewModel.isRunning && viewModel.currentPhase != BreathingPhase.IDLE) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    mediaPlayerBg.value?.pause()
                    coroutineScope.launch {
                        val minutes = Math.ceil(viewModel.elapsedSessionTimeMs / 60000.0).toInt()
                        settingsRepository.recordSessionCompletion(minutes)
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text("Finalizar y Regar Jardín", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
