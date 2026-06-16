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
