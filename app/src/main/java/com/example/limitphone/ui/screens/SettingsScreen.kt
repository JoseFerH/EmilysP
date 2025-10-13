package com.example.limitphone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.limitphone.data.WorkSchedule
import com.example.limitphone.viewmodel.AppViewModel
import com.example.limitphone.ui.components.TimePickerDialog
import com.example.limitphone.ui.components.HoursInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showEmergencyCodeDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    val schedules by viewModel.workSchedules.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra superior
        TopAppBar(
            title = { 
                Text(
                    "Configuración",
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de horarios de trabajo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Horarios de Trabajo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showAddScheduleDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar horario")
                            }
                        }
                        
                        Text(
                            text = "Configura los horarios de trabajo y descanso",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Lista de horarios
            items(schedules) { schedule ->
                ScheduleItem(
                    schedule = schedule,
                    onDelete = { viewModel.deleteWorkSchedule(schedule.id) },
                    onEdit = { /* Implementar edición */ }
                )
            }
            
            // Sección de configuración de emergencia
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Código de Emergencia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Código para desbloquear el dispositivo en emergencias",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showEmergencyCodeDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurar Código")
                        }
                    }
                }
            }
            
            // Sección de tutorial
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Tutorial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ver tutorial de uso de la aplicación",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showTutorialDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Tutorial")
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo para agregar horario
    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onAdd = { schedule ->
                viewModel.addWorkSchedule(schedule)
                showAddScheduleDialog = false
            }
        )
    }
    
    // Diálogo para código de emergencia
    if (showEmergencyCodeDialog) {
        EmergencyCodeDialog(
            viewModel = viewModel,
            onDismiss = { showEmergencyCodeDialog = false }
        )
    }
    
    // Diálogo de tutorial
    if (showTutorialDialog) {
        TutorialDialog(
            onDismiss = { showTutorialDialog = false }
        )
    }
}

@Composable
fun ScheduleItem(
    schedule: WorkSchedule,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isEnabled) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Descanso: ${schedule.breakStartTime} - ${schedule.breakEndTime}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onAdd: (WorkSchedule) -> Unit
) {
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var breakStartTime by remember { mutableStateOf("12:00") }
    var breakEndTime by remember { mutableStateOf("12:30") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showBreakStartTimePicker by remember { mutableStateOf(false) }
    var showBreakEndTimePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Agregar Horario de Trabajo", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

            SettingsTimeField("Hora de Inicio de Trabajo", startTime) {
                showStartTimePicker = true
            }
            SettingsTimeField("Hora de Fin de Trabajo", endTime) {
                showEndTimePicker = true
            }
            SettingsTimeField("Hora de Inicio de Descanso", breakStartTime) {
                showBreakStartTimePicker = true
            }
            SettingsTimeField("Hora de Fin de Descanso", breakEndTime) {
                showBreakEndTimePicker = true
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = {
                    val schedule = WorkSchedule(
                        startTime = startTime, endTime = endTime,
                        breakStartTime = breakStartTime, breakEndTime = breakEndTime
                    )
                    onAdd(schedule)
                }, modifier = Modifier.weight(1f)) { Text("Agregar") }
            }
        }
    }
    if (showStartTimePicker) {
        TimePickerDialog(initialTime = startTime, onTimeSelected = { startTime = it }) { showStartTimePicker = false }
    }
    if (showEndTimePicker) {
        TimePickerDialog(initialTime = endTime, onTimeSelected = { endTime = it }) { showEndTimePicker = false }
    }
    if (showBreakStartTimePicker) {
        TimePickerDialog(initialTime = breakStartTime, onTimeSelected = { breakStartTime = it }) { showBreakStartTimePicker = false }
    }
    if (showBreakEndTimePicker) {
        TimePickerDialog(initialTime = breakEndTime, onTimeSelected = { breakEndTime = it }) { showBreakEndTimePicker = false }
    }
}

@Composable
fun SettingsTimeField(label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EmergencyCodeDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var currentCode by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Código de Emergencia") },
        text = {
            Column {
                Text(
                    text = "Configura un código de 4 dígitos para desbloquear el dispositivo en emergencias.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newCode,
                    onValueChange = { newCode = it.take(4) },
                    label = { Text("Nuevo código") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmCode,
                    onValueChange = { confirmCode = it.take(4) },
                    label = { Text("Confirmar código") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newCode.length == 4 && newCode == confirmCode) {
                        viewModel.updateEmergencyCode(newCode)
                        onDismiss()
                    }
                },
                enabled = newCode.length == 4 && newCode == confirmCode
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tutorial") },
        text = {
            Text(
                text = "Esta funcionalidad mostrará un carrusel de imágenes con instrucciones de uso. " +
                        "Por ahora, puedes navegar por la aplicación para familiarizarte con sus funciones.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}


