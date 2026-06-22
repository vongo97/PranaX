package com.example.breathingapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val isDarkMode: Boolean = false,
    val isBellEnabled: Boolean = true,
    val isBackgroundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val backgroundUri: String? = null,
    val completedSessionsCount: Int = 0,
    val lastSessionDate: String = "",
    val dailyStreak: Int = 0,
    val bellType: String = "bowl", // bowl, chime
    val totalMinutesMeditated: Int = 0,
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val seedType: String = "flower", // flower, bonsai, cactus
    val backgroundAudioType: String = "forest", // forest, rain, ocean
    val isGuidedMeditationEnabled: Boolean = false,
    val loggedInUserEmail: String? = null
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_preferences")

class SettingsRepository(private val context: Context) {
    
    private val dataStore = context.settingsDataStore
    
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val BELL_KEY = booleanPreferencesKey("bell_enabled")
    private val BACKGROUND_KEY = booleanPreferencesKey("background_enabled")
    private val VIBRATION_KEY = booleanPreferencesKey("vibration_enabled")
    private val BACKGROUND_URI_KEY = stringPreferencesKey("background_uri")
    private val SESSIONS_COUNT_KEY = androidx.datastore.preferences.core.intPreferencesKey("sessions_count")
    private val LAST_DATE_KEY = stringPreferencesKey("last_session_date")
    private val DAILY_STREAK_KEY = androidx.datastore.preferences.core.intPreferencesKey("daily_streak")
    private val BELL_TYPE_KEY = stringPreferencesKey("bell_type")
    private val TOTAL_MINUTES_KEY = androidx.datastore.preferences.core.intPreferencesKey("total_minutes")
    private val REMINDER_ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
    private val REMINDER_HOUR_KEY = androidx.datastore.preferences.core.intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE_KEY = androidx.datastore.preferences.core.intPreferencesKey("reminder_minute")
    private val SEED_TYPE_KEY = stringPreferencesKey("seed_type")
    private val BACKGROUND_AUDIO_TYPE_KEY = stringPreferencesKey("background_audio_type")
    private val GUIDED_MEDITATION_KEY = booleanPreferencesKey("guided_meditation_enabled")
    private val LOGGED_IN_USER_EMAIL_KEY = stringPreferencesKey("logged_in_user_email")

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            isDarkMode = preferences[DARK_MODE_KEY] ?: false,
            isBellEnabled = preferences[BELL_KEY] ?: true,
            isBackgroundEnabled = preferences[BACKGROUND_KEY] ?: true,
            isVibrationEnabled = preferences[VIBRATION_KEY] ?: true,
            backgroundUri = preferences[BACKGROUND_URI_KEY],
            completedSessionsCount = preferences[SESSIONS_COUNT_KEY] ?: 0,
            lastSessionDate = preferences[LAST_DATE_KEY] ?: "",
            dailyStreak = preferences[DAILY_STREAK_KEY] ?: 0,
            bellType = preferences[BELL_TYPE_KEY] ?: "bowl",
            totalMinutesMeditated = preferences[TOTAL_MINUTES_KEY] ?: 0,
            isReminderEnabled = preferences[REMINDER_ENABLED_KEY] ?: false,
            reminderHour = preferences[REMINDER_HOUR_KEY] ?: 20,
            reminderMinute = preferences[REMINDER_MINUTE_KEY] ?: 0,
            seedType = preferences[SEED_TYPE_KEY] ?: "flower",
            backgroundAudioType = preferences[BACKGROUND_AUDIO_TYPE_KEY] ?: "forest",
            isGuidedMeditationEnabled = preferences[GUIDED_MEDITATION_KEY] ?: false,
            loggedInUserEmail = preferences[LOGGED_IN_USER_EMAIL_KEY]
        )
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    suspend fun updateBell(enabled: Boolean) {
        dataStore.edit { it[BELL_KEY] = enabled }
    }

    suspend fun updateBackground(enabled: Boolean) {
        dataStore.edit { it[BACKGROUND_KEY] = enabled }
    }

    suspend fun updateVibration(enabled: Boolean) {
        dataStore.edit { it[VIBRATION_KEY] = enabled }
    }

    suspend fun updateBackgroundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(BACKGROUND_URI_KEY)
            } else {
                preferences[BACKGROUND_URI_KEY] = uri
            }
        }
    }

    suspend fun updateBellType(type: String) {
        dataStore.edit { it[BELL_TYPE_KEY] = type }
    }

    suspend fun updateReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED_KEY] = enabled }
    }

    suspend fun updateReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[REMINDER_HOUR_KEY] = hour
            it[REMINDER_MINUTE_KEY] = minute
        }
    }

    suspend fun updateSeedType(type: String) {
        dataStore.edit { it[SEED_TYPE_KEY] = type }
    }

    suspend fun updateBackgroundAudioType(type: String) {
        dataStore.edit { it[BACKGROUND_AUDIO_TYPE_KEY] = type }
    }

    suspend fun updateGuidedMeditation(enabled: Boolean) {
        dataStore.edit { it[GUIDED_MEDITATION_KEY] = enabled }
    }

    suspend fun restoreStats(streak: Int, sessions: Int, minutes: Int) {
        dataStore.edit { preferences ->
            preferences[DAILY_STREAK_KEY] = streak
            preferences[SESSIONS_COUNT_KEY] = sessions
            preferences[TOTAL_MINUTES_KEY] = minutes
        }
    }

    suspend fun recordSessionCompletion(minutesMeditated: Int) {
        dataStore.edit { preferences ->
            // Increment total count
            val currentCount = preferences[SESSIONS_COUNT_KEY] ?: 0
            preferences[SESSIONS_COUNT_KEY] = currentCount + 1

            // Increment total minutes
            val currentMinutes = preferences[TOTAL_MINUTES_KEY] ?: 0
            preferences[TOTAL_MINUTES_KEY] = currentMinutes + minutesMeditated

            // Handle Daily Streak
            val today = java.time.LocalDate.now().toString()
            val lastDate = preferences[LAST_DATE_KEY] ?: ""
            val currentStreak = preferences[DAILY_STREAK_KEY] ?: 0

            if (lastDate != today) {
                // Check if they missed a day
                val isConsecutive = try {
                    if (lastDate.isNotEmpty()) {
                        val last = java.time.LocalDate.parse(lastDate)
                        val now = java.time.LocalDate.parse(today)
                        java.time.temporal.ChronoUnit.DAYS.between(last, now) == 1L
                    } else false
                } catch (e: Exception) { false }

                if (isConsecutive || lastDate.isEmpty()) {
                    preferences[DAILY_STREAK_KEY] = currentStreak + 1
                } else {
                    preferences[DAILY_STREAK_KEY] = 1 // Reset streak to 1
                }
                preferences[LAST_DATE_KEY] = today
            }
        }
    }

    suspend fun updateLoggedInUserEmail(email: String?) {
        dataStore.edit { preferences ->
            if (email == null) {
                preferences.remove(LOGGED_IN_USER_EMAIL_KEY)
            } else {
                preferences[LOGGED_IN_USER_EMAIL_KEY] = email
            }
        }
    }
}
