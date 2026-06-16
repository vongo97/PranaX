package com.example.breathingapp.ui.breathing

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.media.SoundPool
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.domain.BoxBreathing
import kotlinx.coroutines.delay

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
            vibrator?.cancel() // Detener vibración inmediatamente
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
            
            // Disparar vibración inicial
            val initialDuration = if (currentPhase == BreathingPhase.PREPARE) 3000L else currentPattern.inhaleMs
            vibratePhaseWaveform(vibrator, currentPhase, initialDuration, settings)
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
                // Iniciar vibración háptica para esta fase
                vibratePhaseWaveform(vibrator, currentPhase, phaseDuration, settings)

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
                        vibrator?.cancel() // Detener vibración
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

    private fun vibratePhaseWaveform(
        vibrator: Vibrator?, 
        phase: BreathingPhase, 
        durationMs: Long, 
        settings: com.example.breathingapp.data.AppSettings
    ) {
        if (!settings.isVibrationEnabled || vibrator == null || durationMs <= 0) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Dividimos la duración de la fase en pasos de 200ms
                val stepDuration = 200L
                val steps = (durationMs / stepDuration).toInt().coerceAtLeast(3)
                val timings = LongArray(steps) { stepDuration }
                val amplitudes = IntArray(steps)

                when (phase) {
                    BreathingPhase.INHALE -> {
                        // Amplitud ascendente (vibración creciente de 10 a 160)
                        for (i in 0 until steps) {
                            val progress = i.toFloat() / (steps - 1)
                            amplitudes[i] = (15 + progress * 135).toInt().coerceIn(0, 255)
                        }
                    }
                    BreathingPhase.EXHALE -> {
                        // Amplitud descendente (vibración decreciente de 160 a 10)
                        for (i in 0 until steps) {
                            val progress = i.toFloat() / (steps - 1)
                            amplitudes[i] = (150 - progress * 135).toInt().coerceIn(0, 255)
                        }
                    }
                    BreathingPhase.HOLD_IN -> {
                        // Latidos suaves cada 1 segundo (pulso de 80ms encendido, el resto apagado)
                        for (i in 0 until steps) {
                            // 5 pasos de 200ms = 1000ms. Hacemos un pulso en el paso 0 de cada ciclo
                            amplitudes[i] = if (i % 5 == 0) 50 else 0
                        }
                    }
                    BreathingPhase.PREPARE -> {
                        // Pequeño doble pulso de inicio
                        timings[0] = 100L
                        timings[1] = 100L
                        timings[2] = 100L
                        amplitudes[0] = 80
                        amplitudes[1] = 0
                        amplitudes[2] = 80
                        // Apagamos el resto de pasos
                        for (i in 3 until steps) {
                            amplitudes[i] = 0
                        }
                    }
                    else -> {
                        // HOLD_OUT o IDLE: sin vibración continua
                        vibrator.cancel()
                        return
                    }
                }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                // Fallback para APIs anteriores a Android 8.0 (clicks simples de inicio)
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
            // Ignorar si falla el motor de vibración
        }
    }
}
