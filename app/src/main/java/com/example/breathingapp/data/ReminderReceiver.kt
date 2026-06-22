package com.example.breathingapp.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.breathingapp.MainActivity
import com.example.breathingapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d("ReminderReceiver", "¡Broadcast recibido! Acción: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED || action == ACTION_SHOW_REMINDER) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = SettingsRepository(context)
                    val settings = repository.settingsFlow.first()
                    
                    if (action == ACTION_SHOW_REMINDER) {
                        android.util.Log.d("ReminderReceiver", "Procesando ACTION_SHOW_REMINDER. Mostrando notificación...")
                        showNotification(context)
                    }
                    
                    if (settings.isReminderEnabled) {
                        android.util.Log.d("ReminderReceiver", "Programando alarma diaria de recordatorio...")
                        scheduleReminder(context, settings.reminderHour, settings.reminderMinute)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReminderReceiver", "Error procesando broadcast en segundo plano", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "daily_breathing_reminder_v2"
        android.util.Log.d("ReminderReceiver", "showNotification iniciada con canal: $channelId")
        
        // Intent para abrir MainActivity al presionar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Ícono de Prana plano compatible
            .apply {
                if (largeIcon != null) {
                    setLargeIcon(largeIcon)
                }
            }
            .setContentTitle("Momento de respirar... 🌱")
            .setContentText("Tómate 5 minutos para relajarte, respirar y regar tu planta hoy.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta para heads-up banner
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Sonido, vibración y luces por defecto
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Verificar si el canal existe (para estar seguros)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel(channelId)
                if (channel == null) {
                    android.util.Log.e("ReminderReceiver", "¡Error! El canal de notificación $channelId no existe en el sistema.")
                } else {
                    android.util.Log.d("ReminderReceiver", "Canal $channelId encontrado. Importancia: ${channel.importance}")
                }
            }

            // Verificar permiso de notificación para Android 13+
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            android.util.Log.d("ReminderReceiver", "¿Tiene permiso de notificación? $hasPermission")
            if (hasPermission) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                android.util.Log.d("ReminderReceiver", "Llamada a notify() realizada con éxito.")
            } else {
                android.util.Log.e("ReminderReceiver", "No se puede mostrar la notificación: Permiso denegado.")
            }
        } catch (e: Exception) {
            // Loguear el error para diagnóstico
            android.util.Log.e("ReminderReceiver", "Error al publicar la notificación", e)
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.breathingapp.ACTION_SHOW_REMINDER"
        private const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE = 2002

        fun scheduleReminder(context: Context, hour: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SHOW_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Configurar calendario para la hora especificada
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Si la hora ya pasó hoy, programar para mañana
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                // Intentar programar con alta precisión (exacta)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Fallback seguro a alarma no exacta si no tiene el permiso concedido
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }

        fun cancelReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SHOW_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
