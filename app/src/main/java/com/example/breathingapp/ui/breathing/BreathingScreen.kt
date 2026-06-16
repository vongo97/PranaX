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

enum class BreathingPhase(val displayName: String) {
    IDLE("Listo"),
    PREPARE("Prepárate..."),
    INHALE("Inhalar"),
    HOLD_IN("Retener"),
    EXHALE("Exhalar"),
    HOLD_OUT("Retener")
}

class BreathingViewModel : ViewModel() {
    var isRunning by mutableStateOf(false)
        private set
    
    var currentPhase by mutableStateOf(BreathingPhase.IDLE)
        private set

    var timeLeftInPhaseMs by mutableStateOf(0L)
        private set

    var currentPattern by mutableStateOf(BoxBreathing)
    
    private var phaseStartTime = 0L

    var elapsedSessionTimeMs by mutableStateOf(0L)
        private set

    var targetDurationMs by mutableStateOf(0L)
        private set

    fun toggleRunning(
        pattern: BreathingPattern, 
        vibrator: Vibrator?, 
        settings: com.example.breathingapp.data.AppSettings, 
        targetMinutes: Int,
        mediaPlayerBg: android.media.MediaPlayer?,
        soundPool: SoundPool?,
        soundIdHigh: Int,
        soundIdLow: Int
    ) {
        if (isRunning) {
            isRunning = false
            mediaPlayerBg?.pause()
            // Keep current phase state for potential resume
        } else {
            currentPattern = pattern
            targetDurationMs = targetMinutes * 60 * 1000L
            isRunning = true
            if (currentPhase == BreathingPhase.IDLE) {
                currentPhase = BreathingPhase.PREPARE
                elapsedSessionTimeMs = 0L
            }
            if (settings.isBackgroundEnabled) {
                mediaPlayerBg?.start()
            }
            vibrateTick(vibrator, settings)
        }
    }

    suspend fun runBreathingLoop(
        vibrator: Vibrator?, 
        settings: com.example.breathingapp.data.AppSettings, 
        mediaPlayerBg: android.media.MediaPlayer?,
        soundPool: SoundPool?,
        soundIdHigh: Int,
        soundIdLow: Int,
        onSessionComplete: () -> Unit
    ) {
        var lastTick = System.currentTimeMillis()
        while (isRunning) {
            val phaseDuration = when (currentPhase) {
                BreathingPhase.PREPARE -> 3000L
                BreathingPhase.INHALE -> currentPattern.inhaleMs
                BreathingPhase.HOLD_IN -> currentPattern.holdInMs
                BreathingPhase.EXHALE -> currentPattern.exhaleMs
                BreathingPhase.HOLD_OUT -> currentPattern.holdOutMs
                BreathingPhase.IDLE -> 0L
            }

            if (phaseDuration > 0) {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < phaseDuration && isRunning) {
                    val now = System.currentTimeMillis()
                    if (currentPhase != BreathingPhase.PREPARE) {
                        elapsedSessionTimeMs += (now - lastTick)
                    }
                    lastTick = now

                    timeLeftInPhaseMs = phaseDuration - (now - startTime)

                    if (targetDurationMs > 0 && elapsedSessionTimeMs >= targetDurationMs) {
                        isRunning = false
                        mediaPlayerBg?.pause()
                        currentPhase = BreathingPhase.IDLE
                        onSessionComplete()
                        break
                    }

                    delay(16) // ~60fps UI update
                }
            }

            if (isRunning) {
                currentPhase = getNextPhase(currentPhase)
                
                if (settings.isBellEnabled) {
                    if (currentPhase == BreathingPhase.INHALE && soundIdHigh != 0) {
                        soundPool?.play(soundIdHigh, 1f, 1f, 1, 0, 1f)
                    } else if (currentPhase == BreathingPhase.EXHALE && soundIdLow != 0) {
                        soundPool?.play(soundIdLow, 1f, 1f, 1, 0, 1f)
                    }
                }

                if (currentPhase == BreathingPhase.INHALE || currentPhase == BreathingPhase.EXHALE) {
                   vibrateTick(vibrator, settings)
                }
            }
        }
    }

    private fun getNextPhase(phase: BreathingPhase): BreathingPhase {
        return when (phase) {
            BreathingPhase.PREPARE -> BreathingPhase.INHALE
            BreathingPhase.INHALE -> if (currentPattern.holdInMs > 0) BreathingPhase.HOLD_IN else BreathingPhase.EXHALE
            BreathingPhase.HOLD_IN -> BreathingPhase.EXHALE
            BreathingPhase.EXHALE -> if (currentPattern.holdOutMs > 0) BreathingPhase.HOLD_OUT else BreathingPhase.INHALE
            BreathingPhase.HOLD_OUT -> BreathingPhase.INHALE
            BreathingPhase.IDLE -> BreathingPhase.PREPARE
        }
    }

    private fun vibrateTick(vibrator: Vibrator?, settings: com.example.breathingapp.data.AppSettings) {
        // Sonido rítmico eliminado, ahora usamos MediaPlayer
        if (!settings.isVibrationEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }
}

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

    // WAKELOCK: Evita que la pantalla se apague mientras esta vista esté abierta
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

    DisposableEffect(settings.isBackgroundEnabled) {
        if (settings.isBackgroundEnabled) {
            val mp = android.media.MediaPlayer.create(context, R.raw.forest_bg)
            mp?.isLooping = true
            mp?.setVolume(0.5f, 0.5f)
            mediaPlayerBg.value = mp
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
