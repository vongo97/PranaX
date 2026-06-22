package com.example.breathingapp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.breathingapp.data.AppSettings
import com.example.breathingapp.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application.applicationContext)

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
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

    fun updateSeedType(type: String) {
        viewModelScope.launch {
            repository.updateSeedType(type)
        }
    }

    fun updateBackgroundAudioType(type: String) {
        viewModelScope.launch {
            repository.updateBackgroundAudioType(type)
        }
    }

    fun updateGuidedMeditation(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateGuidedMeditation(enabled)
        }
    }
}
