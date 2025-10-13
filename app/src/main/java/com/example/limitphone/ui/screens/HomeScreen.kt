package com.example.limitphone.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limitphone.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()
    val isBreakTime by viewModel.isBreakTime.collectAsState()
    val isDeviceAdminEnabled = viewModel.isDeviceAdminEnabled()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título principal
        Text(
            text = "LimitPhone",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        // Estado del dispositivo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isBreakTime -> MaterialTheme.colorScheme.secondaryContainer
                    isScreenLocked -> MaterialTheme.colorScheme.errorContainer 
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when {
                        isBreakTime -> Icons.Default.Info
                        isScreenLocked -> Icons.Default.Lock 
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        isBreakTime -> "Tiempo de Descanso"
                        isScreenLocked -> "Horario de Trabajo" 
                        else -> "Tiempo Libre"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isBreakTime -> "El dispositivo se desbloqueará automáticamente al terminar el descanso"
                        isScreenLocked -> "Bloqueo automático activo durante horario de trabajo" 
                        else -> "Sin restricciones - configurable en Configuración"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // Estado de Device Admin
        if (!isDeviceAdminEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permisos de Administrador Requeridos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Necesitas habilitar los permisos de administrador para que la aplicación funcione correctamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.requestDeviceAdmin(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Habilitar Administrador")
                    }
                }
            }
        }
        
        // Información de horarios
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Estado del Sistema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = if (isDeviceAdminEnabled) {
                        "✅ Bloqueo automático habilitado - El dispositivo se bloqueará automáticamente durante los horarios de trabajo configurados."
                    } else {
                        "❌ Bloqueo automático deshabilitado - Habilita los permisos de administrador para usar el bloqueo automático."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDeviceAdminEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 Configura tus horarios de trabajo en la sección Configuración para que el dispositivo se bloquee automáticamente durante esas horas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Controles manuales (solo para pruebas)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Controles Manuales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Estos controles son para pruebas. En uso normal, el dispositivo se bloqueará automáticamente según los horarios configurados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.lockScreen() },
                        modifier = Modifier.weight(1f),
                        enabled = isDeviceAdminEnabled && !isScreenLocked
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bloquear")
                    }
                    
                    Button(
                        onClick = { viewModel.startBreak() },
                        modifier = Modifier.weight(1f),
                        enabled = isDeviceAdminEnabled && isScreenLocked && !isBreakTime
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descanso")
                    }
                }
            }
        }
        
        // Botón de configuración
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configuración")
        }
    }
}




