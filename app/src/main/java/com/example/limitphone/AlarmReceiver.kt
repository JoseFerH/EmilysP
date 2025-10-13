package com.example.limitphone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id")
        val message = intent.getStringExtra("message") ?: "¡Es hora!"
        val isBreakStart = intent.getBooleanExtra("is_break_start", true)
        val scheduleId = intent.getStringExtra("schedule_id")
        
        showNotification(context, alarmId ?: "unknown", message, isBreakStart)
        
        // Manejar acción de la alarma
        handleAlarmAction(context, isBreakStart, scheduleId)
        
        // Reproducir sonido de alarma
        playAlarmSound(context)
    }
    
    private fun showNotification(context: Context, alarmId: String, message: String, isBreakStart: Boolean) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        val title = if (isBreakStart) "¡Hora de Descansar!" else "¡Fin del Descanso!"
        val icon = if (isBreakStart) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground
        
        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        notificationManager.notify(alarmId.hashCode(), notification)
    }
    
    private fun handleAlarmAction(context: Context, isBreakStart: Boolean, scheduleId: String?) {
        // Aquí podrías implementar lógica adicional basada en el tipo de alarma
        // Por ejemplo, cambiar el estado de la aplicación, iniciar actividades, etc.
        
        if (isBreakStart) {
            // Lógica para inicio de descanso
            // Por ejemplo, desbloquear pantalla temporalmente
        } else {
            // Lógica para fin de descanso
            // Por ejemplo, volver a bloquear la pantalla
        }
    }
    
    private fun playAlarmSound(context: Context) {
        // Aquí podrías agregar lógica para reproducir un sonido personalizado
        // Por ahora, el sistema usará el sonido de notificación predeterminado
    }
}

