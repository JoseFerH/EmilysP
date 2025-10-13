package com.example.limitphone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limitphone.viewmodel.AppViewModel

@Composable
fun EmergencyUnlockScreen(
    viewModel: AppViewModel,
    onUnlockSuccess: () -> Unit
) {
    var enteredCode by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    val maxAttempts = 3
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icono de emergencia
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Red
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Título
            Text(
                text = "Desbloqueo de Emergencia",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Descripción
            Text(
                text = "Ingresa tu código de emergencia de 4 dígitos",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Campo de entrada del código
            OutlinedTextField(
                value = enteredCode,
                onValueChange = { 
                    if (it.length <= 4) {
                        enteredCode = it
                        showError = false
                    }
                },
                modifier = Modifier.width(200.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botón de verificación
            Button(
                onClick = {
                    if (enteredCode.length == 4) {
                        if (viewModel.emergencyUnlock(enteredCode)) {
                            onUnlockSuccess()
                        } else {
                            showError = true
                            attempts++
                            enteredCode = ""
                        }
                    }
                },
                enabled = enteredCode.length == 4 && attempts < maxAttempts,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                ),
                modifier = Modifier.width(200.dp)
            ) {
                Text(
                    text = "Verificar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje de error
            if (showError) {
                Text(
                    text = "Código incorrecto. Intentos restantes: ${maxAttempts - attempts}",
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            // Bloqueo por intentos excesivos
            if (attempts >= maxAttempts) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Demasiados intentos fallidos",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "El dispositivo permanecerá bloqueado por seguridad.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Información de seguridad
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Solo usa esta función en emergencias reales",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


