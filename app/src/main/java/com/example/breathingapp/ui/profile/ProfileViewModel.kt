package com.example.breathingapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.breathingapp.data.AppSettings
import com.example.breathingapp.data.SettingsRepository
import com.example.breathingapp.data.NeonSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: NeonSyncRepository
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signIn(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = syncRepository.signIn(email, password)
            if (result.isSuccess) {
                syncRepository.pullAndSyncStats()
                _uiState.update { it.copy(isLoading = false, successMessage = "¡Sesión iniciada con éxito! ☁️") }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar sesión") }
            }
        }
    }

    fun signUp(email: String, password: String, currentStreak: Int, currentSessions: Int, currentMinutes: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = syncRepository.signUp(email, password)
            if (result.isSuccess) {
                syncRepository.pushStats(currentStreak, currentSessions, currentMinutes)
                _uiState.update { it.copy(isLoading = false, successMessage = "¡Usuario registrado correctamente! 🛡️") }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Error al registrar usuario") }
            }
        }
    }

    fun signInWithGoogle(email: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = syncRepository.signInWithGoogle(email)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, successMessage = "¡Sesión iniciada con Google! ☁️") }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Error al sincronizar con Google") }
            }
        }
    }

    fun signOut() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            syncRepository.signOut()
            _uiState.update { it.copy(isLoading = false, successMessage = "Sesión cerrada correctamente") }
        }
    }

    fun syncStats(streak: Int, sessions: Int, minutes: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val push = syncRepository.pushStats(streak, sessions, minutes)
            val pull = syncRepository.pullAndSyncStats()
            if (push.isSuccess && pull.isSuccess) {
                _uiState.update { it.copy(isLoading = false, successMessage = "¡Sincronización exitosa con Neon! ⚡") }
            } else {
                val errorMsg = push.exceptionOrNull()?.message ?: pull.exceptionOrNull()?.message ?: "Error en la sincronización"
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        }
    }
}
