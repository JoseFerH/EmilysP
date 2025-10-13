package com.example.limitphone.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limitphone.AlarmReceiver
import com.example.limitphone.DeviceAdminReceiver
import com.example.limitphone.data.BreakAlarm
import com.example.limitphone.data.EmergencyCode
import com.example.limitphone.data.WorkSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class AppViewModel : ViewModel() {
    
    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()
    
    private val _isBreakTime = MutableStateFlow(false)
    val isBreakTime: StateFlow<Boolean> = _isBreakTime.asStateFlow()
    
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()
    
    private val _workSchedules = MutableStateFlow<List<WorkSchedule>>(emptyList())
    val workSchedules: StateFlow<List<WorkSchedule>> = _workSchedules.asStateFlow()
    
    private val _breakAlarms = MutableStateFlow<List<BreakAlarm>>(emptyList())
    val breakAlarms: StateFlow<List<BreakAlarm>> = _breakAlarms.asStateFlow()
    
    private val _emergencyCode = MutableStateFlow(EmergencyCode("1234"))
    val emergencyCode: StateFlow<EmergencyCode> = _emergencyCode.asStateFlow()
    
    private var lockStartTime: Long = 0
    private var lockDuration: Long = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var context: Context? = null
    private var devicePolicyManager: DevicePolicyManager? = null
    private var deviceAdminComponent: ComponentName? = null
    
    fun setContext(context: Context) {
        this.context = context
        createNotificationChannel()
        initializeDeviceAdmin()
        startScheduleMonitoring()
    }
    
    private fun initializeDeviceAdmin() {
        context?.let { ctx ->
            devicePolicyManager = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            deviceAdminComponent = ComponentName(ctx, DeviceAdminReceiver::class.java)
        }
    }
    
    fun isDeviceAdminEnabled(): Boolean {
        return devicePolicyManager?.isAdminActive(deviceAdminComponent!!) ?: false
    }
    
    fun requestDeviceAdmin(context: Context) {
        if (!isDeviceAdminEnabled()) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Esta aplicación necesita permisos de administrador para bloquear el dispositivo durante el horario de trabajo.")
            context.startActivity(intent)
        }
    }
    
    fun lockScreen() {
        if (isDeviceAdminEnabled()) {
            _isScreenLocked.value = true
            devicePolicyManager?.lockNow()
            
            // Programar desbloqueo automático basado en el horario de trabajo
            scheduleWorkEndAlarm()
        }
    }
    
    fun unlockScreen() {
        if (isDeviceAdminEnabled()) {
            _isScreenLocked.value = false
            _isBreakTime.value = false
            _remainingTime.value = 0
        }
    }
    
    fun startBreak() {
        _isBreakTime.value = true
        _isScreenLocked.value = false
        
        // Programar fin de descanso
        scheduleBreakEndAlarm()
    }
    
    fun endBreak() {
        _isBreakTime.value = false
        _isScreenLocked.value = true
        devicePolicyManager?.lockNow()
    }
    
    private fun startTimeUpdate() {
        viewModelScope.launch {
            while (_isScreenLocked.value) {
                val elapsed = System.currentTimeMillis() - lockStartTime
                val remaining = lockDuration - elapsed
                
                if (remaining <= 0) {
                    unlockScreen()
                    break
                }
                
                _remainingTime.value = remaining
                kotlinx.coroutines.delay(1000) // Actualizar cada segundo
            }
        }
    }
    
    fun getRemainingTime(): String {
        val totalSeconds = _remainingTime.value / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    // Métodos para BreakAlarms (nuevos)
    fun deleteBreakAlarm(alarmId: String) {
        cancelAlarm(alarmId)
        val updatedAlarms = _breakAlarms.value.filter { it.id != alarmId }
        _breakAlarms.value = updatedAlarms
    }
    
    fun toggleBreakAlarm(alarmId: String) {
        val updatedAlarms = _breakAlarms.value.map { alarm ->
            if (alarm.id == alarmId) {
                val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
                if (updatedAlarm.isEnabled) {
                    scheduleAlarm(updatedAlarm)
                } else {
                    cancelAlarm(alarmId)
                }
                updatedAlarm
            } else {
                alarm
            }
        }
        _breakAlarms.value = updatedAlarms
    }
    
    private fun scheduleAlarm(alarm: BreakAlarm) {
        context?.let { ctx ->
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ctx, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("message", alarm.message)
                putExtra("is_break_start", alarm.isBreakStart)
                putExtra("schedule_id", alarm.scheduleId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                ctx, 
                alarm.id.hashCode(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Calcular el tiempo de la alarma
            val calendar = Calendar.getInstance()
            val timeP

            // 1. CAMBIO: Importar AndroidViewModel y Application para evitar fugas de memoria con el contexto.
// 2. CAMBIO: Heredar de AndroidViewModel en lugar de ViewModel.
// 3. CAMBIO: Usar AndroidViewModel(application) y pasar 'application' al constructor.
            class AppViewModel(application: Application) : AndroidViewModel(application) {

                private val _isScreenLocked = MutableStateFlow(false)
                val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

                private val _isBreakTime = MutableStateFlow(false)
                val isBreakTime: StateFlow<Boolean> = _isBreakTime.asStateFlow()

                private val _remainingTime = MutableStateFlow(0L)
                val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

                private val _workSchedules = MutableStateFlow<List<WorkSchedule>>(emptyList())
                val workSchedules: StateFlow<List<WorkSchedule>> = _workSchedules.asStateFlow()

                private val _breakAlarms = MutableStateFlow<List<BreakAlarm>>(emptyList())
                val breakAlarms: StateFlow<List<BreakAlarm>> = _breakAlarms.asStateFlow()

                private val _emergencyCode = MutableStateFlow(EmergencyCode("1234"))
                val emergencyCode: StateFlow<EmergencyCode> = _emergencyCode.asStateFlow()

                private var lockStartTime: Long = 0
                private var lockDuration: Long = 0
                private var wakeLock: PowerManager.WakeLock? = null
                private var devicePolicyManager: DevicePolicyManager? = null
                private var deviceAdminComponent: ComponentName? = null

                // 4. CAMBIO: Inicializar todo en el bloque init. Ya no se necesita setContext().
                init {
                    createNotificationChannel()
                    initializeDeviceAdmin()
                    startScheduleMonitoring()
                }

                private fun initializeDeviceAdmin() {
                    val context = getApplication<Application>().applicationContext
                    devicePolicyManager =
                        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    deviceAdminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
                }

                fun isDeviceAdminEnabled(): Boolean {
                    return devicePolicyManager?.isAdminActive(deviceAdminComponent!!) ?: false
                }

                fun requestDeviceAdmin(context: Context) {
                    if (!isDeviceAdminEnabled()) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Esta aplicación necesita permisos de administrador para bloquear el dispositivo durante el horario de trabajo."
                            )
                            // Es mejor iniciar la actividad desde un contexto que no sea de la aplicación si es posible.
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }

                fun lockScreen() {
                    if (isDeviceAdminEnabled()) {
                        _isScreenLocked.value = true
                        devicePolicyManager?.lockNow()
                        scheduleWorkEndAlarm()
                    }
                }

                fun unlockScreen() {
                    if (isDeviceAdminEnabled()) {
                        _isScreenLocked.value = false
                        _isBreakTime.value = false
                        _remainingTime.value = 0
                    }
                }

                fun startBreak() {
                    _isBreakTime.value = true
                    _isScreenLocked.value = false
                    scheduleBreakEndAlarm()
                }

                fun endBreak() {
                    _isBreakTime.value = false
                    _isScreenLocked.value = true
                    devicePolicyManager?.lockNow()
                }

                private fun startTimeUpdate() {
                    viewModelScope.launch {
                        while (_isScreenLocked.value) {
                            val elapsed = System.currentTimeMillis() - lockStartTime
                            val remaining = lockDuration - elapsed

                            if (remaining <= 0) {
                                unlockScreen()
                                break
                            }

                            _remainingTime.value = remaining
                            delay(1000) // Actualizar cada segundo
                        }
                    }
                }

                fun getRemainingTime(): String {
                    val totalSeconds = _remainingTime.value / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    return String.format("%02d:%02d", minutes, seconds)
                }

                fun deleteBreakAlarm(alarmId: String) {
                    cancelAlarm(alarmId)
                    val updatedAlarms = _breakAlarms.value.filter { it.id != alarmId }
                    _breakAlarms.value = updatedAlarms
                }

                fun toggleBreakAlarm(alarmId: String) {
                    val updatedAlarms = _breakAlarms.value.map { alarm ->
                        if (alarm.id == alarmId) {
                            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
                            if (updatedAlarm.isEnabled) {
                                scheduleAlarm(updatedAlarm)
                            } else {
                                cancelAlarm(alarmId)
                            }
                            updatedAlarm
                        } else {
                            alarm
                        }
                    }
                    _breakAlarms.value = updatedAlarms
                }

                private fun scheduleAlarm(alarm: BreakAlarm) {
                    val context = getApplication<Application>().applicationContext
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("alarm_id", alarm.id)
                        putExtra("message", alarm.message)
                        putExtra("is_break_start", alarm.isBreakStart)
                        putExtra("schedule_id", alarm.scheduleId)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.id.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // 5. CORRECCIÓN: Se limpió el código duplicado y con errores de sintaxis.
                    val calendar = Calendar.getInstance()
                    val timeParts = alarm.startTime.split(":")
                    calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                    calendar.set(Calendar.SECOND, 0)

                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }

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
                }

                private fun cancelAlarm(alarmId: String) {
                    val context = getApplication<Application>().applicationContext
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(context, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarmId.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                }

                private fun createNotificationChannel() {
                    val context = getApplication<Application>().applicationContext
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            "alarm_channel",
                            "Alarmas",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "Canal para notificaciones de alarmas"
                        }

                        val notificationManager = ContextCompat.getSystemService(
                            context,
                            NotificationManager::class.java
                        )
                        notificationManager?.createNotificationChannel(channel)
                    }
                }

                fun addWorkSchedule(schedule: WorkSchedule) {
                    val updatedSchedules = _workSchedules.value + schedule
                    _workSchedules.value = updatedSchedules
                    scheduleBreakAlarms(schedule)
                }

                fun updateWorkSchedule(schedule: WorkSchedule) {
                    val updatedSchedules = _workSchedules.value.map {
                        if (it.id == schedule.id) schedule else it
                    }
                    _workSchedules.value = updatedSchedules
                }

                fun deleteWorkSchedule(scheduleId: String) {
                    val updatedSchedules = _workSchedules.value.filter { it.id != scheduleId }
                    _workSchedules.value = updatedSchedules

                    _breakAlarms.value.filter { it.scheduleId == scheduleId }
                        .forEach { cancelAlarm(it.id) }
                }

                private fun scheduleBreakAlarms(schedule: WorkSchedule) {
                    val breakStart = BreakAlarm(
                        startTime = schedule.breakStartTime,
                        endTime = schedule.breakEndTime,
                        message = "¡Es hora de descansar!",
                        isBreakStart = true,
                        scheduleId = schedule.id
                    )
                    // 6. CORRECCIÓN LÓGICA: La alarma de fin de descanso debe usar la hora de fin.
                    val breakEnd = BreakAlarm(
                        startTime = schedule.breakEndTime, // Usar breakEndTime
                        endTime = schedule.breakEndTime,
                        message = "El descanso ha terminado. Regresa al trabajo.",
                        isBreakStart = false,
                        scheduleId = schedule.id
                    )
                    val updatedAlarms = _breakAlarms.value + breakStart + breakEnd
                    _breakAlarms.value = updatedAlarms
                    scheduleAlarm(breakStart)
                    scheduleAlarm(breakEnd)
                }

                private fun calculateBreakEndTime(startTime: String, durationHours: Float): String {
                    val timeParts = startTime.split(":")
                    val hours = timeParts[0].toInt()
                    val minutes = timeParts[1].toInt()

                    val durationMinutes = (durationHours * 60).toInt()
                    val totalMinutes = hours * 60 + minutes + durationMinutes
                    val endHours = (totalMinutes / 60) % 24
                    val endMinutes = totalMinutes % 60

                    return String.format("%02d:%02d", endHours, endMinutes)
                }

                private fun scheduleWorkEndAlarm() {
                    // Implementar lógica para programar fin de jornada
                }

                private fun scheduleBreakEndAlarm() {
                    // Implementar lógica para programar fin de descanso
                }

                fun updateEmergencyCode(newCode: String) {
                    _emergencyCode.value = EmergencyCode(newCode)
                }

                fun verifyEmergencyCode(inputCode: String): Boolean {
                    return _emergencyCode.value.code == inputCode && _emergencyCode.value.isActive
                }

                fun emergencyUnlock(inputCode: String): Boolean {
                    return if (verifyEmergencyCode(inputCode)) {
                        unlockScreen()
                        _emergencyCode.value = _emergencyCode.value.copy(lastUsed = Date())
                        true
                    } else {
                        false
                    }
                }

                private fun startScheduleMonitoring() {
                    viewModelScope.launch {
                        while (true) {
                            checkCurrentSchedule()
                            delay(60000) // Verificar cada minuto
                        }
                    }
                }

                private fun checkCurrentSchedule() {
                    val currentTime = Calendar.getInstance()
                    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = currentTime.get(Calendar.MINUTE)
                    // 7. CORRECCIÓN: Se completó la línea de código.
                    val currentTimeString = String.format("%02d:%02d", currentHour, currentMinute)
                    // Aquí iría la lógica para comparar con los horarios de trabajo.
                }
            }arts = alarm.
            val calendar = Calendar.getInstance()
            val timeParts = alarm.startTime.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)time.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
            
            // Si la hora ya pasó hoy, programar para mañana
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
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
        }
    }
    
    private fun cancelAlarm(alarmId: String) {
        context?.let { ctx ->
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ctx, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                ctx, 
                alarmId.hashCode(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
    
    private fun createNotificationChannel() {
        context?.let { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "alarm_channel",
                    "Alarmas",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Canal para notificaciones de alarmas"
                }
                
                val notificationManager = ContextCompat.getSystemService(
                    ctx, 
                    NotificationManager::class.java
                )
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
    
    // Métodos para manejar horarios de trabajo
    fun addWorkSchedule(schedule: WorkSchedule) {
        val updatedSchedules = _workSchedules.value + schedule
        _workSchedules.value = updatedSchedules
        scheduleBreakAlarms(schedule)
    }
    
    fun updateWorkSchedule(schedule: WorkSchedule) {
        val updatedSchedules = _workSchedules.value.map { 
            if (it.id == schedule.id) schedule else it 
        }
        _workSchedules.value = updatedSchedules
    }
    
    fun deleteWorkSchedule(scheduleId: String) {
        val updatedSchedules = _workSchedules.value.filter { it.id != scheduleId }
        _workSchedules.value = updatedSchedules
        
        // Cancelar alarmas relacionadas
        _breakAlarms.value.filter { it.scheduleId == scheduleId }
            .forEach { cancelAlarm(it.id) }
    }
    
    private fun scheduleBreakAlarms(schedule: WorkSchedule) {
        val breakStart = BreakAlarm(
            startTime = schedule.breakStartTime,
            endTime = schedule.breakEndTime,
            message = "¡Es hora de descansar!",
            isBreakStart = true,
            scheduleId = schedule.id
        )
        val breakEnd = BreakAlarm(
            startTime = schedule.breakStartTime,
            endTime = schedule.breakEndTime,
            message = "El descanso ha terminado. Regresa al trabajo.",
            isBreakStart = false,
            scheduleId = schedule.id
        )
        val updatedAlarms = _breakAlarms.value + breakStart + breakEnd
        _breakAlarms.value = updatedAlarms
        scheduleAlarm(breakStart)
        scheduleAlarm(breakEnd)
    }
    
    private fun calculateBreakEndTime(startTime: String, durationHours: Float): String {
        val timeParts = startTime.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        
        val durationMinutes = (durationHours * 60).toInt()
        val totalMinutes = hours * 60 + minutes + durationMinutes
        val endHours = (totalMinutes / 60) % 24
        val endMinutes = totalMinutes % 60
        
        return String.format("%02d:%02d", endHours, endMinutes)
    }
    
    private fun scheduleWorkEndAlarm() {
        // Implementar lógica para programar fin de jornada
    }
    
    private fun scheduleBreakEndAlarm() {
        // Implementar lógica para programar fin de descanso
    }
    
    // Métodos para código de emergencia
    fun updateEmergencyCode(newCode: String) {
        _emergencyCode.value = EmergencyCode(newCode)
    }
    
    fun verifyEmergencyCode(inputCode: String): Boolean {
        return _emergencyCode.value.code == inputCode && _emergencyCode.value.isActive
    }
    
    fun emergencyUnlock(inputCode: String): Boolean {
        return if (verifyEmergencyCode(inputCode)) {
            unlockScreen()
            // Registrar uso del código
            _emergencyCode.value = _emergencyCode.value.copy(lastUsed = Date())
            true
        } else {
            false
        }
    }
    
    private fun startScheduleMonitoring() {
        viewModelScope.launch {
            while (true) {
                checkCurrentSchedule()
                delay(60000) // Verificar cada minuto
            }
        }
    }
    
    private fun checkCurrentSchedule() {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTimeString = String.format("%02d:%02d", currentHour, currentMinute)
        val currentDayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)
        
        val activeSchedule = _workSchedules.value.find { schedule ->
            schedule.isEnabled && 
            currentDayOfWeek in schedule.daysOfWeek &&
            currentTimeString >= schedule.startTime && 
            currentTimeString <= schedule.endTime
        }
        
        if (activeSchedule != null && !_isScreenLocked.value && !_isBreakTime.value) {
            // Es hora de trabajar y el dispositivo no está bloqueado
            lockScreen()
        } else if (activeSchedule == null && _isScreenLocked.value && !_isBreakTime.value) {
            // Ya no es hora de trabajar
            unlockScreen()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        wakeLock?.release()
    }
}

