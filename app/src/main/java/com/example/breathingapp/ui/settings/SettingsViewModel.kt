package com.example.breathingapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.breathingapp.data.AppSettings
import com.example.breathingapp.data.SettingsRepository
import com.example.breathingapp.data.NeonSyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    // Exponer el flujo de configuración como un StateFlow caliente en el scope del ViewModel
    val settingsState: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(enabled)
        }
    }

    fun updateBell(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBell(enabled)
        }
    }

    fun updateBackground(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBackground(enabled)
        }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVibration(enabled)
        }
    }

    fun updateBackgroundUri(uri: String?) {
        viewModelScope.launch {
            repository.updateBackgroundUri(uri)
        }
    }

    fun updateBellType(type: String) {
        viewModelScope.launch {
            repository.updateBellType(type)
        }
    }

    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateReminderEnabled(enabled)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateReminderTime(hour, minute)
        }
    }

    fun updateGuidedMeditation(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateGuidedMeditation(enabled)
        }
    }

    fun updateBackgroundAudioType(type: String) {
        viewModelScope.launch {
            repository.updateBackgroundAudioType(type)
        }
    }

    fun updateSeedType(type: String) {
        viewModelScope.launch {
            repository.updateSeedType(type)
        }
    }

    fun recordSessionCompletion(minutes: Int) {
        viewModelScope.launch {
            repository.recordSessionCompletion(minutes)
            val latestSettings = repository.settingsFlow.first()
            if (latestSettings.loggedInUserEmail != null) {
                val syncRepository = NeonSyncRepository(repository)
                syncRepository.pushStats(
                    streak = latestSettings.dailyStreak,
                    sessions = latestSettings.completedSessionsCount,
                    minutes = latestSettings.totalMinutesMeditated
                )
            }
        }
    }
}

