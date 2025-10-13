package com.example.limitphone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.limitphone.ui.screens.HomeScreen
import com.example.limitphone.ui.screens.SettingsScreen
import com.example.limitphone.ui.screens.TutorialScreen
import com.example.limitphone.ui.screens.EmergencyUnlockScreen
import com.example.limitphone.ui.theme.LimitPhoneTheme
import com.example.limitphone.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: AppViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Configurar el contexto en el ViewModel
        viewModel.setContext(this)
        
        setContent {
            LimitPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
        
        // Observar cambios en el estado de bloqueo de pantalla
        lifecycleScope.launchWhenStarted {
            viewModel.isScreenLocked.collect { isLocked ->
                if (isLocked) {
                    startLockScreen()
                }
            }
        }
    }
    
    private fun startLockScreen() {
        val intent = Intent(this, LockScreenActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("tutorial") {
            TutorialScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("emergency") {
            EmergencyUnlockScreen(
                viewModel = viewModel,
                onUnlockSuccess = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}