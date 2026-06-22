package com.example.breathingapp.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.breathingapp.data.AppSettings
import com.example.breathingapp.data.NeonSyncRepository
import com.example.breathingapp.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val settingsRepository = SettingsRepository(context)
    private val syncRepository = NeonSyncRepository(context, settingsRepository)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun signUp(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = syncRepository.signUp(email, password)
            if (result.isSuccess) {
                // Al registrar, subir las estadísticas locales actuales
                val current = settings.value
                syncRepository.pushStats(
                    streak = current.dailyStreak,
                    sessions = current.completedSessionsCount,
                    minutes = current.totalMinutesMeditated
                )
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = syncRepository.signIn(email, password)
            if (result.isSuccess) {
                syncRepository.pullAndSyncStats()
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun signOut(onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = syncRepository.signOut()
            _isLoading.value = false
            onResult(result)
        }
    }

    fun signInWithGoogle(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = syncRepository.signInWithGoogle(email)
            _isLoading.value = false
            onResult(result)
        }
    }

    fun syncStats(streak: Int, sessions: Int, minutes: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val pushResult = syncRepository.pushStats(streak, sessions, minutes)
            val pullResult = syncRepository.pullAndSyncStats()
            _isLoading.value = false
            
            if (pushResult.isSuccess && pullResult.isSuccess) {
                onResult(Result.success(Unit))
            } else {
                val error = pushResult.exceptionOrNull() ?: pullResult.exceptionOrNull() ?: Exception("Error en la sincronización")
                onResult(Result.failure(error))
            }
        }
    }
}
