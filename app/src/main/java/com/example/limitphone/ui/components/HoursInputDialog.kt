package com.example.limitphone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun HoursInputDialog(
    initialHours: Float,
    onHoursSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var hoursText by remember { mutableStateOf(initialHours.toString()) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Duración del Descanso",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Ingresa la duración en horas (ej: 0.5 para 30 minutos, 1.0 para 1 hora)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { 
                        hoursText = it
                        errorMessage = ""
                    },
                    label = { Text("Horas") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage.isNotEmpty(),
                    supportingText = if (errorMessage.isNotEmpty()) {
                        { Text(errorMessage) }
                    } else {
                        { Text("Ejemplos: 0.5 (30 min), 1.0 (1 hora), 1.5 (1h 30min)") }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de selección rápida
                Text(
                    text = "Selección Rápida:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { hours ->
                        OutlinedButton(
                            onClick = { 
                                hoursText = hours.toString()
                                errorMessage = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when(hours) {
                                    0.25f -> "15min"
                                    0.5f -> "30min"
                                    1.0f -> "1h"
                                    1.5f -> "1.5h"
                                    2.0f -> "2h"
                                    else -> "${hours}h"
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            val hours = hoursText.toFloatOrNull()
                            if (hours != null && hours > 0 && hours <= 24) {
                                onHoursSelected(hours)
                                onDismiss()
                            } else {
                                errorMessage = "Ingresa un valor válido entre 0.1 y 24 horas"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}
