package com.example.limitphone.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.limitphone.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Inicializar ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Cargar video local desde assets (placeholder)
            // val mediaItem = MediaItem.fromUri("android.resource://com.example.limitphone/raw/sample_video")
            // setMediaItem(mediaItem)
            // prepare()
        }
    }
    
    // Limpiar cuando se destruya el composable
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra superior
        TopAppBar(
            title = { 
                Text(
                    "Reproductor de Video",
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }
        )
        
        // Reproductor de video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .padding(16.dp)
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Información del video
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Video de Ejemplo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Este es un video de ejemplo almacenado localmente en la aplicación. " +
                            "Puedes reemplazar este archivo con tu propio video.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Controles adicionales
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { exoPlayer.play() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reproducir")
            }
            
            OutlinedButton(
                onClick = { exoPlayer.pause() },
                modifier = Modifier.weight(1f)
            ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pausar")
            }
            
            OutlinedButton(
                onClick = { exoPlayer.seekTo(0) },
                modifier = Modifier.weight(1f)
            ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reiniciar")
            }
        }
    }
}

