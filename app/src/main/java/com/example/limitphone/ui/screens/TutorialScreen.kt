package com.example.limitphone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limitphone.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TutorialSlide(
    val title: String,
    val description: String,
    val imageRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    onNavigateBack: () -> Unit
) {
    val tutorialSlides = listOf(
        TutorialSlide(
            title = "¡Bienvenido a LimitPhone!",
            description = "Esta aplicación te ayuda a mantener un equilibrio saludable entre trabajo y descanso.",
            imageRes = R.drawable.ic_launcher_foreground,
            icon = Icons.Default.Home
        ),
        TutorialSlide(
            title = "Configura tu Horario",
            description = "En la sección de configuración, puedes establecer tus horarios de trabajo y tiempo de descanso.",
            imageRes = R.drawable.ic_launcher_foreground,
            icon = Icons.Default.Info
        ),
        TutorialSlide(
            title = "Inicia el Modo Trabajo",
            description = "Cuando sea hora de trabajar, presiona 'Iniciar Trabajo' para bloquear el dispositivo.",
            imageRes = R.drawable.ic_launcher_foreground,
            icon = Icons.Default.Info
        ),
        TutorialSlide(
            title = "Tiempo de Descanso",
            description = "Cuando sea hora de descansar, el dispositivo se desbloqueará automáticamente para que puedas relajarte.",
            imageRes = R.drawable.ic_launcher_foreground,
            icon = Icons.Default.Info
        ),
        TutorialSlide(
            title = "Código de Emergencia",
            description = "En caso de emergencia, puedes usar el código de 4 dígitos configurado para desbloquear el dispositivo.",
            imageRes = R.drawable.ic_launcher_foreground,
            icon = Icons.Default.Lock
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { tutorialSlides.size })
    var autoAdvance by remember { mutableStateOf(true) }
    
    // Auto-avance del carrusel
    LaunchedEffect(autoAdvance) {
        if (autoAdvance) {
            while (true) {
                delay(5000) // Cambiar cada 5 segundos
                val nextPage = (pagerState.currentPage + 1) % tutorialSlides.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Carrusel principal
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            TutorialSlideContent(
                slide = tutorialSlides[page],
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Indicadores de página
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(tutorialSlides.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }
        
        // Controles
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            // Botón de auto-avance
            FloatingActionButton(
                onClick = { autoAdvance = !autoAdvance },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (autoAdvance) Icons.Default.Info else Icons.Default.PlayArrow,
                    contentDescription = if (autoAdvance) "Pausar" else "Reproducir"
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botón de navegación manual
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        autoAdvance = false
                        val prevPage = if (pagerState.currentPage > 0) 
                            pagerState.currentPage - 1 
                        else 
                            tutorialSlides.size - 1
                        // pagerState.animateScrollToPage(prevPage) // Comentado por ser función suspend
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Anterior")
                }
                
                FloatingActionButton(
                    onClick = {
                        autoAdvance = false
                        val nextPage = (pagerState.currentPage + 1) % tutorialSlides.size
                        // pagerState.animateScrollToPage(nextPage) // Comentado por ser función suspend
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Siguiente")
                }
            }
        }
        
        // Botón de cerrar
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cerrar tutorial",
                tint = Color.White
            )
        }
    }
}

@Composable
fun TutorialSlideContent(
    slide: TutorialSlide,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono principal
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Imagen de ejemplo (puedes reemplazar con imágenes reales)
            Image(
                painter = painterResource(id = slide.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título
            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Descripción
            Text(
                text = slide.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 24.sp
            )
        }
    }
}


