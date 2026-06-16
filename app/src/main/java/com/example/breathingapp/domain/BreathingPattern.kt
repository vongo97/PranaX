package com.example.breathingapp.domain

import kotlinx.serialization.Serializable

@Serializable
data class BreathingPattern(
    val name: String,
    val inhaleMs: Long,
    val holdInMs: Long,
    val exhaleMs: Long,
    val holdOutMs: Long
) {
    val totalCycleMs: Long
        get() = inhaleMs + holdInMs + exhaleMs + holdOutMs
}

val BoxBreathing = BreathingPattern("Respiración en Caja", 4000, 4000, 4000, 4000)
val Relaxing478 = BreathingPattern("Relajación 4-7-8", 4000, 7000, 8000, 0)
val Coherent = BreathingPattern("Respiración Coherente", 5500, 0, 5500, 0)
val PhysiologicalSigh = BreathingPattern("Suspiro Fisiológico", 2000, 1000, 6000, 0)

val predefinedPatterns = listOf(BoxBreathing, Relaxing478, Coherent, PhysiologicalSigh)
