package com.example.breathingapp.ui.breathing

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.core.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.R
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.breathingapp.ui.settings.SettingsViewModel
import com.example.breathingapp.ui.settings.SettingsViewModelFactory

@Composable
fun BreathingScreen(
    pattern: BreathingPattern,
    targetDurationMinutes: Int = 5,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BreathingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(settings.isGuidedMeditationEnabled) {
        if (settings.isGuidedMeditationEnabled) {
            var ttsInstance: TextToSpeech? = null
            ttsInstance = TextToSpeech(context, { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = ttsInstance?.setLanguage(Locale.forLanguageTag("es-ES"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsInstance?.setLanguage(Locale.forLanguageTag("es"))
                    }
                    ttsInstance?.setPitch(1.0f)
                    ttsInstance?.setSpeechRate(0.95f)
                    tts = ttsInstance
                }
            }, "com.google.android.tts")
            onDispose {
                ttsInstance?.shutdown()
                tts = null
            }
        } else {
            onDispose {}
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

    // WAKELOCK y parada de vibración
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            vibrator.cancel() // Detener vibración al salir de la pantalla
            viewModel.stopSession() // Detener la sesión para liberar hilos
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
            if (uiState.isRunning) {
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

    // Controlar reproducción de música de fondo basado en estado de reproducción
    LaunchedEffect(uiState.isRunning, settings.isBackgroundEnabled) {
        val mp = mediaPlayerBg.value
        if (uiState.isRunning && settings.isBackgroundEnabled) {
            mp?.start()
        } else {
            mp?.pause()
        }
    }

    // Colectar eventos de efectos secundarios emitidos por el ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BreathingViewModel.BreathingEvent.PlaySound -> {
                    if (settings.isBellEnabled) {
                        val soundId = if (event.isHighFrequency) soundIdHigh.value else soundIdLow.value
                        if (soundId != 0) {
                            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                        }
                    }
                }
                is BreathingViewModel.BreathingEvent.TriggerVibration -> {
                    vibratePhaseWaveform(vibrator, event.phase, event.durationMs, settings.isVibrationEnabled)
                }
                is BreathingViewModel.BreathingEvent.SessionCompleted -> {
                    val minutes = Math.ceil(uiState.elapsedSessionTimeMs / 60000.0).toInt()
                    settingsViewModel.recordSessionCompletion(minutes)
                    onBack()
                }
            }
        }
    }

    // Narrador de voz TTS (Text to Speech)
    LaunchedEffect(uiState.currentPhase, settings.isGuidedMeditationEnabled, uiState.isRunning, tts) {
        if (settings.isGuidedMeditationEnabled && uiState.isRunning) {
            val textToSpeak = when (uiState.currentPhase) {
                BreathingPhase.INHALE -> "Inhala"
                BreathingPhase.HOLD_IN -> "Retén"
                BreathingPhase.EXHALE -> "Exhala"
                BreathingPhase.HOLD_OUT -> "Espera"
                BreathingPhase.PREPARE -> "Prepárate"
                else -> ""
            }
            if (textToSpeak.isNotEmpty()) {
                if (uiState.currentPhase == BreathingPhase.PREPARE) {
                    delay(500)
                }
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC.toString())
                }
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "BreathingPhase")
            }
        }
    }

    // Animation states
    val scale by animateFloatAsState(
        targetValue = when (uiState.currentPhase) {
            BreathingPhase.PREPARE -> 1.0f
            BreathingPhase.INHALE -> 1.5f
            BreathingPhase.HOLD_IN -> 1.5f
            BreathingPhase.EXHALE -> 1.0f
            BreathingPhase.HOLD_OUT -> 1.0f
            BreathingPhase.IDLE -> 1.0f
        },
        animationSpec = tween(
            durationMillis = when (uiState.currentPhase) {
                BreathingPhase.PREPARE -> 5000
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
            .statusBarsPadding()
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
        if (uiState.targetDurationMs > 0) {
            val elapsedSec = uiState.elapsedSessionTimeMs / 1000
            val targetSec = uiState.targetDurationMs / 1000
            Text(
                text = "${elapsedSec / 60}:${(elapsedSec % 60).toString().padStart(2, '0')} / ${targetSec / 60}:00",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Animation Container
        Box(
            modifier = Modifier.size(250.dp),
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
                    text = uiState.currentPhase.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.isRunning) {
                    val secondsLeft = Math.ceil(uiState.timeLeftInPhaseMs / 1000.0).toInt()
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
            onClick = { viewModel.toggleRunning(pattern, targetDurationMinutes) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp)
        ) {
            Text(if (uiState.isRunning) "Pausar" else "Comenzar", style = MaterialTheme.typography.titleMedium)
        }

        if (!uiState.isRunning && uiState.currentPhase != BreathingPhase.IDLE) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.stopSession()
                    mediaPlayerBg.value?.pause()
                    val minutes = Math.ceil(uiState.elapsedSessionTimeMs / 60000.0).toInt()
                    settingsViewModel.recordSessionCompletion(minutes)
                    onBack()
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

/**
 * Función helper en la capa de UI para disparar la vibración haptica de forma segura y controlada.
 */
private fun vibratePhaseWaveform(
    vibrator: Vibrator?, 
    phase: BreathingPhase, 
    durationMs: Long, 
    isVibrationEnabled: Boolean
) {
    if (!isVibrationEnabled || vibrator == null || durationMs <= 0) return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val stepDuration = 200L
            val steps = (durationMs / stepDuration).toInt().coerceAtLeast(3)
            val timings = LongArray(steps) { stepDuration }
            val amplitudes = IntArray(steps)

            when (phase) {
                BreathingPhase.INHALE -> {
                    // Amplitud ascendente (creciente de 15 a 150)
                    for (i in 0 until steps) {
                        val progress = i.toFloat() / (steps - 1)
                        amplitudes[i] = (15 + progress * 135).toInt().coerceIn(0, 255)
                    }
                }
                BreathingPhase.EXHALE -> {
                    // Amplitud descendente (decreciente de 150 a 15)
                    for (i in 0 until steps) {
                        val progress = i.toFloat() / (steps - 1)
                        amplitudes[i] = (150 - progress * 135).toInt().coerceIn(0, 255)
                    }
                }
                BreathingPhase.HOLD_IN -> {
                    // Latido suave
                    for (i in 0 until steps) {
                        amplitudes[i] = if (i % 5 == 0) 50 else 0
                    }
                }
                BreathingPhase.PREPARE -> {
                    timings[0] = 100L
                    timings[1] = 100L
                    timings[2] = 100L
                    amplitudes[0] = 80
                    amplitudes[1] = 0
                    amplitudes[2] = 80
                    for (i in 3 until steps) {
                        amplitudes[i] = 0
                    }
                }
                else -> {
                    vibrator.cancel()
                    return
                }
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            val pattern = when (phase) {
                BreathingPhase.INHALE -> longArrayOf(0, 80, 200, 80)
                BreathingPhase.EXHALE -> longArrayOf(0, 100, 150, 50)
                BreathingPhase.HOLD_IN -> longArrayOf(0, 40)
                BreathingPhase.PREPARE -> longArrayOf(0, 60, 100, 60)
                else -> longArrayOf(0)
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    } catch (e: Exception) {
        // Ignorar
    }
}
