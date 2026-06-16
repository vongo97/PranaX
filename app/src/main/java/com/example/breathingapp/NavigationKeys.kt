package com.example.breathingapp

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object CreateRoute

@Serializable
object GardenRoute

@Serializable
object SettingsRoute

@Serializable
data class PrepRoute(val patternIndex: Int)

@Serializable
data class BreathingRoute(
    val name: String,
    val inhaleMs: Long,
    val holdInMs: Long,
    val exhaleMs: Long,
    val holdOutMs: Long,
    val durationMinutes: Int
)
