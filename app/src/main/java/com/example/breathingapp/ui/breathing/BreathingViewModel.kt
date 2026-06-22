package com.example.breathingapp.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.domain.BoxBreathing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BreathingPhase(val displayName: String) {
    IDLE("Listo"),
    PREPARE("Prepárate..."),
    INHALE("Inhalar"),
    HOLD_IN("Retener"),
    EXHALE("Exhalar"),
    HOLD_OUT("Retener")
}

data class BreathingUiState(
    val isRunning: Boolean = false,
    val currentPhase: BreathingPhase = BreathingPhase.IDLE,
    val timeLeftInPhaseMs: Long = 0L,
    val currentPattern: BreathingPattern = BoxBreathing,
    val elapsedSessionTimeMs: Long = 0L,
    val targetDurationMs: Long = 0L
)

class BreathingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    // Canal para notificar a la UI eventos que requieren efectos secundarios del sistema (sonidos, vibración)
    private val _eventFlow = MutableSharedFlow<BreathingEvent>()
    val eventFlow: SharedFlow<BreathingEvent> = _eventFlow.asSharedFlow()

    sealed class BreathingEvent {
        data class PlaySound(val isHighFrequency: Boolean) : BreathingEvent()
        data class TriggerVibration(val phase: BreathingPhase, val durationMs: Long) : BreathingEvent()
        object SessionCompleted : BreathingEvent()
    }

    private var timerJob: Job? = null

    fun toggleRunning(pattern: BreathingPattern, targetMinutes: Int) {
        val wasRunning = _uiState.value.isRunning
        _uiState.update { state ->
            val nextRunning = !state.isRunning
            val nextPhase = if (nextRunning && state.currentPhase == BreathingPhase.IDLE) {
                BreathingPhase.PREPARE
            } else {
                state.currentPhase
            }
            val elapsed = if (nextRunning && state.currentPhase == BreathingPhase.IDLE) 0L else state.elapsedSessionTimeMs
            
            state.copy(
                isRunning = nextRunning,
                currentPhase = nextPhase,
                currentPattern = pattern,
                targetDurationMs = targetMinutes * 60 * 1000L,
                elapsedSessionTimeMs = elapsed
            )
        }

        if (_uiState.value.isRunning) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            
            // Disparar vibración inicial para la fase actual
            val state = _uiState.value
            val initialDuration = when (state.currentPhase) {
                BreathingPhase.PREPARE -> 5000L
                BreathingPhase.INHALE -> state.currentPattern.inhaleMs
                BreathingPhase.HOLD_IN -> state.currentPattern.holdInMs
                BreathingPhase.EXHALE -> state.currentPattern.exhaleMs
                BreathingPhase.HOLD_OUT -> state.currentPattern.holdOutMs
                BreathingPhase.IDLE -> 0L
            }
            if (initialDuration > 0) {
                _eventFlow.emit(BreathingEvent.TriggerVibration(state.currentPhase, initialDuration))
            }

            while (_uiState.value.isRunning) {
                val current = _uiState.value
                val phaseDuration = when (current.currentPhase) {
                    BreathingPhase.PREPARE -> 5000L
                    BreathingPhase.INHALE -> current.currentPattern.inhaleMs
                    BreathingPhase.HOLD_IN -> current.currentPattern.holdInMs
                    BreathingPhase.EXHALE -> current.currentPattern.exhaleMs
                    BreathingPhase.HOLD_OUT -> current.currentPattern.holdOutMs
                    BreathingPhase.IDLE -> 0L
                }

                if (phaseDuration > 0) {
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < phaseDuration && _uiState.value.isRunning) {
                        val now = System.currentTimeMillis()
                        val diff = now - lastTick
                        lastTick = now
                        
                        _uiState.update { s ->
                            val newElapsed = if (s.currentPhase != BreathingPhase.PREPARE) {
                                s.elapsedSessionTimeMs + diff
                            } else {
                                s.elapsedSessionTimeMs
                            }
                            
                            val isComplete = s.targetDurationMs > 0 && newElapsed >= s.targetDurationMs
                            if (isComplete) {
                                s.copy(
                                    isRunning = false,
                                    currentPhase = BreathingPhase.IDLE,
                                    timeLeftInPhaseMs = 0L,
                                    elapsedSessionTimeMs = s.targetDurationMs
                                )
                            } else {
                                s.copy(
                                    timeLeftInPhaseMs = phaseDuration - (now - startTime),
                                    elapsedSessionTimeMs = newElapsed
                                )
                            }
                        }

                        if (!_uiState.value.isRunning) {
                            _eventFlow.emit(BreathingEvent.SessionCompleted)
                            break
                        }
                        delay(16) // ~60fps UI update
                    }
                }

                if (_uiState.value.isRunning) {
                    val nextPhase = getNextPhase(_uiState.value.currentPhase, _uiState.value.currentPattern)
                    _uiState.update { s ->
                        s.copy(currentPhase = nextPhase)
                    }

                    // Emitir campanada según la fase entrante
                    if (nextPhase == BreathingPhase.INHALE) {
                        _eventFlow.emit(BreathingEvent.PlaySound(isHighFrequency = true))
                    } else if (nextPhase == BreathingPhase.EXHALE) {
                        _eventFlow.emit(BreathingEvent.PlaySound(isHighFrequency = false))
                    }

                    // Emitir vibración para la nueva fase
                    val nextDuration = when (nextPhase) {
                        BreathingPhase.INHALE -> _uiState.value.currentPattern.inhaleMs
                        BreathingPhase.HOLD_IN -> _uiState.value.currentPattern.holdInMs
                        BreathingPhase.EXHALE -> _uiState.value.currentPattern.exhaleMs
                        BreathingPhase.HOLD_OUT -> _uiState.value.currentPattern.holdOutMs
                        else -> 0L
                    }
                    if (nextDuration > 0) {
                        _eventFlow.emit(BreathingEvent.TriggerVibration(nextPhase, nextDuration))
                    }
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun getNextPhase(phase: BreathingPhase, pattern: BreathingPattern): BreathingPhase {
        return when (phase) {
            BreathingPhase.PREPARE -> BreathingPhase.INHALE
            BreathingPhase.INHALE -> if (pattern.holdInMs > 0) BreathingPhase.HOLD_IN else BreathingPhase.EXHALE
            BreathingPhase.HOLD_IN -> BreathingPhase.EXHALE
            BreathingPhase.EXHALE -> if (pattern.holdOutMs > 0) BreathingPhase.HOLD_OUT else BreathingPhase.INHALE
            BreathingPhase.HOLD_OUT -> BreathingPhase.INHALE
            BreathingPhase.IDLE -> BreathingPhase.PREPARE
        }
    }

    fun stopSession() {
        stopTimer()
        _uiState.update { s ->
            s.copy(
                isRunning = false,
                currentPhase = BreathingPhase.IDLE,
                timeLeftInPhaseMs = 0L
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
