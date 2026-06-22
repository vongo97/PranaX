package com.example.breathingapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.breathingapp.domain.BreathingPattern
import com.example.breathingapp.domain.predefinedPatterns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Extension property to get DataStore instance from Context (privada al archivo)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "breathing_patterns")

class BreathingPatternRepository(private val context: Context) {
    
    private val CUSTOM_PATTERNS_KEY = stringPreferencesKey("custom_patterns")

    // Flow that emits the combined list of predefined + custom patterns
    val allPatterns: Flow<List<BreathingPattern>> = context.dataStore.data.map { preferences ->
        val customPatternsJson = preferences[CUSTOM_PATTERNS_KEY] ?: "[]"
        val customPatterns = try {
            Json.decodeFromString<List<BreathingPattern>>(customPatternsJson)
        } catch (e: Exception) {
            emptyList()
        }
        
        // Return static predefined patterns followed by any custom patterns the user made
        predefinedPatterns + customPatterns
    }

    suspend fun saveCustomPattern(pattern: BreathingPattern) {
        context.dataStore.edit { preferences ->
            val customPatternsJson = preferences[CUSTOM_PATTERNS_KEY] ?: "[]"
            val currentPatterns = try {
                Json.decodeFromString<List<BreathingPattern>>(customPatternsJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            currentPatterns.add(pattern)
            preferences[CUSTOM_PATTERNS_KEY] = Json.encodeToString(currentPatterns)
        }
    }
}
