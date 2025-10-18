package com.example.limitphone

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.limitphone/lock"
    private val TAG = "LimitPhoneLock"
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var isLockServiceActive = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            try {
                when (call.method) {
                    "startLockService" -> {
                        Log.d(TAG, "Iniciando servicio de bloqueo")
                        startLockMonitoring()
                        result.success(true)
                    }
                    "stopLockService" -> {
                        Log.d(TAG, "Deteniendo servicio de bloqueo")
                        stopLockMonitoring()
                        result.success(true)
                    }
                    "bringToFront" -> {
                        Log.d(TAG, "Trayendo app al frente")
                        bringAppToFront()
                        result.success(true)
                    }
                    "startLockTask" -> {
                        Log.d(TAG, "Iniciando Lock Task Mode (Screen Pinning)")
                        activateLockTaskMode()
                        result.success(true)
                    }
                    "stopLockTask" -> {
                        Log.d(TAG, "Deteniendo Lock Task Mode (Screen Pinning)")
                        deactivateLockTaskMode()
                        result.success(true)
                    }
                    "isInLockTaskMode" -> {
                        val isLocked = checkLockTaskStatus()
                        Log.d(TAG, "¿Está en Lock Task Mode? $isLocked")
                        result.success(isLocked)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en método ${call.method}: ${e.message}")
                result.error("ERROR", e.message, null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Mantener la pantalla encendida cuando la app está activa
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d(TAG, "MainActivity creada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
        }
    }

    private fun startLockMonitoring() {
        try {
            if (isLockServiceActive) {
                Log.d(TAG, "El servicio de bloqueo ya está activo")
                return
            }
            
            handler = Handler(Looper.getMainLooper())
            runnable = object : Runnable {
                override fun run() {
                    try {
                        if (!isAppInForeground()) {
                            Log.d(TAG, "App en segundo plano, trayendo al frente")
                            bringAppToFront()
                        }
                        handler?.postDelayed(this, 3000) // Verificar cada 3 segundos
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en runnable de monitoreo: ${e.message}")
                    }
                }
            }
            handler?.post(runnable!!)
            isLockServiceActive = true
            Log.d(TAG, "Servicio de monitoreo iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar monitoreo: ${e.message}")
        }
    }

    private fun stopLockMonitoring() {
        try {
            runnable?.let { handler?.removeCallbacks(it) }
            handler = null
            runnable = null
            isLockServiceActive = false
            Log.d(TAG, "Servicio de monitoreo detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener monitoreo: ${e.message}")
        }
    }

    private fun isAppInForeground(): Boolean {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses
            
            if (appProcesses == null) {
                Log.d(TAG, "No se pudieron obtener procesos en ejecución")
                return false
            }
            
            val packageName = applicationContext.packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND 
                    && appProcess.processName == packageName) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando si app está en primer plano: ${e.message}")
            false
        }
    }

    private fun bringAppToFront() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            Log.d(TAG, "Intent para traer app al frente enviado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al traer app al frente: ${e.message}")
        }
    }

    /**
     * Activa el modo Lock Task (Screen Pinning)
     * Requiere Android 5.0+ (API 21)
     */
    private fun activateLockTaskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startLockTask()
                Log.d(TAG, "Lock Task Mode activado exitosamente")
            } else {
                Log.w(TAG, "Lock Task Mode no disponible en esta versión de Android")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar Lock Task Mode: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Desactiva el modo Lock Task (Screen Pinning)
     * Requiere Android 5.0+ (API 21)
     */
    private fun deactivateLockTaskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask()
                Log.d(TAG, "Lock Task Mode desactivado exitosamente")
            } else {
                Log.w(TAG, "Lock Task Mode no disponible en esta versión de Android")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar Lock Task Mode: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Verifica si la app está actualmente en modo Lock Task
     * @return true si está en Lock Task Mode, false en caso contrario
     */
    private fun checkLockTaskStatus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ (API 23) - Método recomendado
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val lockTaskMode = activityManager.lockTaskModeState
                val isLocked = lockTaskMode != ActivityManager.LOCK_TASK_MODE_NONE
                
                Log.d(TAG, "Lock Task Mode State: $lockTaskMode")
                Log.d(TAG, "¿Está bloqueado?: $isLocked")
                
                isLocked
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5.0-5.1 (API 21-22)
                // Usar reflexión para evitar conflicto de nombres
                try {
                    val method = this.javaClass.getMethod("isInLockTaskMode")
                    val isLocked = method.invoke(this) as Boolean
                    Log.d(TAG, "¿Está bloqueado? (reflexión): $isLocked")
                    isLocked
                } catch (e: Exception) {
                    Log.e(TAG, "Error al verificar con reflexión: ${e.message}")
                    false
                }
            } else {
                Log.w(TAG, "Lock Task Mode no disponible en esta versión de Android")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar Lock Task Mode: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "App resumida")
        
        // Verificar estado de Lock Task al resumir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val isLocked = checkLockTaskStatus()
            Log.d(TAG, "Estado de Screen Pinning en onResume: $isLocked")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "App pausada")
    }

    override fun onDestroy() {
        Log.d(TAG, "MainActivity destruida")
        stopLockMonitoring()
        
        // Asegurarse de detener Lock Task si está activo
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && checkLockTaskStatus()) {
                stopLockTask()
                Log.d(TAG, "Lock Task Mode desactivado en onDestroy")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar Lock Task en onDestroy: ${e.message}")
        }
        
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Verificar si está en Lock Task Mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && checkLockTaskStatus()) {
            Log.d(TAG, "Botón atrás bloqueado - App en Lock Task Mode")
            // No llamar a super.onBackPressed() para bloquear el botón
            return
        }
        
        // Comportamiento normal si no está en Lock Task
        super.onBackPressed()
    }
}