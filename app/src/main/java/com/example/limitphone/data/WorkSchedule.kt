package com.example.limitphone.data

import java.util.*

data class WorkSchedule(
    val id: String = UUID.randomUUID().toString(),
    val startTime: String, // Formato HH:MM
    val endTime: String,   // Formato HH:MM
    val breakStartTime: String, // Formato HH:MM - inicio del descanso
    val breakEndTime: String,   // Formato HH:MM - fin del descanso
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = listOf(1,2,3,4,5), // 1=Lunes, 7=Domingo
    val createdAt: Date = Date()
)

data class BreakAlarm(
    val id: String = UUID.randomUUID().toString(),
    val startTime: String, // Formato HH:MM - inicio del descanso
    val endTime: String,   // Formato HH:MM - fin del descanso
    val message: String = "Es hora de descansar",
    val isBreakStart: Boolean = true, // true = inicio, false = fin
    val isEnabled: Boolean = true,
    val scheduleId: String, // Relación con el horario principal
    val createdAt: Date = Date()
)

data class EmergencyCode(
    val code: String,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val lastUsed: Date? = null
)


