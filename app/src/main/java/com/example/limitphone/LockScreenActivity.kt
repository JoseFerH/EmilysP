package com.example.limitphone

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.limitphone.viewmodel.AppViewModel

class LockScreenActivity : ComponentActivity() {
    
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var keyguardLock: KeyguardManager.KeyguardLock
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar para mostrar sobre la pantalla de bloqueo
        setupLockScreen()
        
        setContent {
            LockScreenContent()
        }
    }
    
    private fun setupLockScreen() {
        // Hacer que la actividad se muestre sobre la pantalla de bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Desactivar el keyguard
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = keyguardManager.newKeyguardLock("LimitPhone")
        keyguardLock.disableKeyguard()
        
        // Mantener la pantalla encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hacer la actividad de pantalla completa
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        keyguardLock.reenableKeyguard()
    }
    
    @Composable
    fun LockScreenContent() {
        val viewModel: AppViewModel = viewModel()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icono de candado
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Mensaje principal
                Text(
                    text = "Pantalla Bloqueada",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tiempo restante
                val remainingTime by viewModel.remainingTime.collectAsState()
                val totalSeconds = remainingTime / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                
                Text(
                    text = "Tiempo restante: $timeString",
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Instrucciones
                Text(
                    text = "Tu pantalla estará bloqueada hasta que termine el tiempo.\n" +
                            "No puedes usar tu dispositivo durante este período.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Botón de emergencia (solo visible después de cierto tiempo)
                if (totalSeconds < 60) { // Solo en el último minuto
                    Button(
                        onClick = {
                            viewModel.unlockScreen()
                            finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "Desbloquear (Emergencia)",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

