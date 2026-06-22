package com.example.breathingapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlinx.serialization.json.*

data class UserStatsRow(
    val email: String,
    val daily_streak: Int,
    val total_sessions: Int,
    val total_minutes: Int
)

class NeonSyncRepository(private val settingsRepository: SettingsRepository) {

    // Constructor secundario para mantener compatibilidad con llamadas que usan Context
    constructor(context: Context) : this(SettingsRepository(context))

    /**
     * Aplica hashing SHA-256 a la contraseña utilizando el correo electrónico del usuario como sal (salt).
     * Esto previene ataques de diccionario y rainbow tables al asegurar hashes únicos.
     */
    private fun hashPassword(password: String, email: String): String {
        val salt = email.lowercase().trim()
        val bytes = (password + salt).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun getCurrentUserEmail(): String? {
        return settingsRepository.settingsFlow.first().loggedInUserEmail
    }

    suspend fun isUserLoggedIn(): Boolean {
        return settingsRepository.settingsFlow.first().loggedInUserEmail != null
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.lowercase().trim()
            
            // Check if user already exists
            val existing = NeonDatabaseClient.execute(
                "SELECT 1 FROM users WHERE email = $1",
                listOf(cleanEmail)
            )
            if (existing.isNotEmpty()) {
                throw Exception("El correo ya está registrado")
            }
            
            // Insert new user with salted password hash
            NeonDatabaseClient.execute(
                "INSERT INTO users (email, password) VALUES ($1, $2)",
                listOf(cleanEmail, hashPassword(password, cleanEmail))
            )
            
            // Insert initial stats row
            NeonDatabaseClient.execute(
                "INSERT INTO user_stats (email, daily_streak, total_sessions, total_minutes) VALUES ($1, 0, 0, 0)",
                listOf(cleanEmail)
            )
            
            settingsRepository.updateLoggedInUserEmail(cleanEmail)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.lowercase().trim()
            
            // 1. Intentar iniciar sesión usando el nuevo hash con sal
            var result = NeonDatabaseClient.execute(
                "SELECT 1 FROM users WHERE email = $1 AND password = $2",
                listOf(cleanEmail, hashPassword(password, cleanEmail))
            )
            
            // 2. Si no se encuentra, verificar si existe con el formato de hash anterior (sin sal) para migración transparente
            if (result.isEmpty()) {
                val oldHash = MessageDigest.getInstance("SHA-256")
                    .digest(password.toByteArray())
                    .fold("") { str, it -> str + "%02x".format(it) }
                
                val oldResult = NeonDatabaseClient.execute(
                    "SELECT 1 FROM users WHERE email = $1 AND password = $2",
                    listOf(cleanEmail, oldHash)
                )
                
                if (oldResult.isNotEmpty()) {
                    // Actualizar el hash del usuario en la base de datos de Neon de forma segura
                    NeonDatabaseClient.execute(
                        "UPDATE users SET password = $1 WHERE email = $2",
                        listOf(hashPassword(password, cleanEmail), cleanEmail)
                    )
                    result = oldResult
                }
            }
            
            if (result.isNotEmpty()) {
                settingsRepository.updateLoggedInUserEmail(cleanEmail)
            } else {
                throw Exception("Correo o contraseña incorrectos")
            }
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            settingsRepository.updateLoggedInUserEmail(null)
        }
    }

    suspend fun pushStats(streak: Int, sessions: Int, minutes: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val email = getCurrentUserEmail() ?: throw Exception("No hay sesión iniciada")
            val sql = """
                INSERT INTO user_stats (email, daily_streak, total_sessions, total_minutes)
                VALUES ($1, $2, $3, $4)
                ON CONFLICT (email) DO UPDATE SET
                    daily_streak = EXCLUDED.daily_streak,
                    total_sessions = EXCLUDED.total_sessions,
                    total_minutes = EXCLUDED.total_minutes
            """.trimIndent()
            
            NeonDatabaseClient.execute(
                sql,
                listOf(email, streak, sessions, minutes)
            )
            Unit
        }
    }

    suspend fun pullAndSyncStats(): Result<UserStatsRow?> = withContext(Dispatchers.IO) {
        runCatching {
            val email = getCurrentUserEmail() ?: throw Exception("No hay sesión iniciada")
            val result = NeonDatabaseClient.execute(
                "SELECT daily_streak, total_sessions, total_minutes FROM user_stats WHERE email = $1",
                listOf(email)
            )
            val remoteRow = result.firstOrNull()?.let { row ->
                val streak = row["daily_streak"]?.jsonPrimitive?.int ?: 0
                val sessions = row["total_sessions"]?.jsonPrimitive?.int ?: 0
                val minutes = row["total_minutes"]?.jsonPrimitive?.int ?: 0
                UserStatsRow(
                    email = email,
                    daily_streak = streak,
                    total_sessions = sessions,
                    total_minutes = minutes
                )
            }
            
            remoteRow?.let { row ->
                val local = settingsRepository.settingsFlow.first()
                val finalStreak = maxOf(local.dailyStreak, row.daily_streak)
                val finalSessions = maxOf(local.completedSessionsCount, row.total_sessions)
                val finalMinutes = maxOf(local.totalMinutesMeditated, row.total_minutes)
                
                // Si el local tiene datos más avanzados que la nube, los sincronizamos automáticamente en Neon
                if (local.dailyStreak > row.daily_streak || 
                    local.completedSessionsCount > row.total_sessions || 
                    local.totalMinutesMeditated > row.total_minutes) {
                    pushStats(finalStreak, finalSessions, finalMinutes)
                }
                
                settingsRepository.restoreStats(
                    streak = finalStreak,
                    sessions = finalSessions,
                    minutes = finalMinutes
                )
            }
            remoteRow
        }
    }

    suspend fun signInWithGoogle(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.lowercase().trim()
            val existing = NeonDatabaseClient.execute(
                "SELECT 1 FROM users WHERE email = $1",
                listOf(cleanEmail)
            )
            
            if (existing.isEmpty()) {
                NeonDatabaseClient.execute(
                    "INSERT INTO users (email, password) VALUES ($1, $2)",
                    listOf(cleanEmail, "GOOGLE_AUTH_USER")
                )
                NeonDatabaseClient.execute(
                    "INSERT INTO user_stats (email, daily_streak, total_sessions, total_minutes) VALUES ($1, 0, 0, 0)",
                    listOf(cleanEmail)
                )
            }
            settingsRepository.updateLoggedInUserEmail(cleanEmail)
            pullAndSyncStats()
            Unit
        }
    }
}
