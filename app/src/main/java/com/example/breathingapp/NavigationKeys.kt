package com.example.breathingapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data class BreathingSession(val patternIndex: Int) : NavKey
