package com.example.breathingapp.ui.breathing

import androidx.lifecycle.ViewModel
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.domain.BoxBreathing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BreathingPhase(val displayName: String) {
    IDLE("Listo"),
    PREPARE("Prepárate..."),
    INHALE("Inhalar"),
    HOLD_IN("Retener"),
    EXHALE("Exhalar"),
    HOLD_OUT("Retener")
}

class BreathingViewModel : ViewModel() {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentPhase = MutableStateFlow(BreathingPhase.IDLE)
    val currentPhase: StateFlow<BreathingPhase> = _currentPhase.asStateFlow()

    private val _timeLeftInPhaseMs = MutableStateFlow(0L)
    val timeLeftInPhaseMs: StateFlow<Long> = _timeLeftInPhaseMs.asStateFlow()

    private val _currentPattern = MutableStateFlow(BoxBreathing)
    val currentPattern: StateFlow<BreathingPattern> = _currentPattern.asStateFlow()

    private val _elapsedSessionTimeMs = MutableStateFlow(0L)
    val elapsedSessionTimeMs: StateFlow<Long> = _elapsedSessionTimeMs.asStateFlow()

    private val _targetDurationMs = MutableStateFlow(0L)
    val targetDurationMs: StateFlow<Long> = _targetDurationMs.asStateFlow()

    fun toggleRunning(pattern: BreathingPattern, targetMinutes: Int) {
        if (_isRunning.value) {
            _isRunning.value = false
        } else {
            _currentPattern.value = pattern
            _targetDurationMs.value = targetMinutes * 60 * 1000L
            _isRunning.value = true
            if (_currentPhase.value == BreathingPhase.IDLE) {
                _currentPhase.value = BreathingPhase.PREPARE
                _elapsedSessionTimeMs.value = 0L
            }
        }
    }

    suspend fun runBreathingLoop(onSessionComplete: () -> Unit) {
        var lastTick = System.currentTimeMillis()
        while (_isRunning.value) {
            val pattern = _currentPattern.value
            val phaseDuration = when (_currentPhase.value) {
                BreathingPhase.PREPARE -> 5000L
                BreathingPhase.INHALE -> pattern.inhaleMs
                BreathingPhase.HOLD_IN -> pattern.holdInMs
                BreathingPhase.EXHALE -> pattern.exhaleMs
                BreathingPhase.HOLD_OUT -> pattern.holdOutMs
                BreathingPhase.IDLE -> 0L
            }

            if (phaseDuration > 0) {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < phaseDuration && _isRunning.value) {
                    val now = System.currentTimeMillis()
                    if (_currentPhase.value != BreathingPhase.PREPARE) {
                        _elapsedSessionTimeMs.value += (now - lastTick)
                    }
                    lastTick = now

                    _timeLeftInPhaseMs.value = phaseDuration - (now - startTime)

                    if (_targetDurationMs.value > 0 && _elapsedSessionTimeMs.value >= _targetDurationMs.value) {
                        _isRunning.value = false
                        _currentPhase.value = BreathingPhase.IDLE
                        onSessionComplete()
                        break
                    }

                    delay(16) // ~60fps UI update
                }
            }

            if (_isRunning.value) {
                _currentPhase.value = getNextPhase(_currentPhase.value)
            }
        }
    }

    fun stopSession() {
        _isRunning.value = false
        _currentPhase.value = BreathingPhase.IDLE
        _timeLeftInPhaseMs.value = 0L
    }

    private fun getNextPhase(phase: BreathingPhase): BreathingPhase {
        val pattern = _currentPattern.value
        return when (phase) {
            BreathingPhase.PREPARE -> BreathingPhase.INHALE
            BreathingPhase.INHALE -> if (pattern.holdInMs > 0) BreathingPhase.HOLD_IN else BreathingPhase.EXHALE
            BreathingPhase.HOLD_IN -> BreathingPhase.EXHALE
            BreathingPhase.EXHALE -> if (pattern.holdOutMs > 0) BreathingPhase.HOLD_OUT else BreathingPhase.INHALE
            BreathingPhase.HOLD_OUT -> BreathingPhase.INHALE
            BreathingPhase.IDLE -> BreathingPhase.PREPARE
        }
    }
}
