package com.example.limitphone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun TimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    
    // Parsear tiempo inicial
    val timeParts = initialTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE)
    
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    
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
                    text = "Seleccionar Hora",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Selector de hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón para disminuir hora
                    IconButton(
                        onClick = {
                            selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                        }
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    // Hora actual
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hora",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Botón para aumentar hora
                    IconButton(
                        onClick = {
                            selectedHour = if (selectedHour < 23) selectedHour + 1 else 0
                        }
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de minutos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón para disminuir minutos
                    IconButton(
                        onClick = {
                            selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59
                        }
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    // Minutos actuales
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Minutos",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Botón para aumentar minutos
                    IconButton(
                        onClick = {
                            selectedMinute = if (selectedMinute < 59) selectedMinute + 1 else 0
                        }
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineMedium
                        )
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
                            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                            onTimeSelected(timeString)
                            onDismiss()
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
