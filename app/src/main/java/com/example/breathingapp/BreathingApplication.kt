package com.example.breathingapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class BreathingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "daily_breathing_reminder_v2"
            val name = "Recordatorios de Respiración"
            val descriptionText = "Notificaciones para recordar tu práctica diaria de respiración."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
