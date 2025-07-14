package com.wgandroid.client

import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.AndroidViewModel
import com.wgandroid.client.ui.theme.WgAndroidTheme
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.screen.UnifiedAuthScreen
import com.wgandroid.client.ui.screen.UnifiedClientsScreen
import com.wgandroid.client.ui.viewmodel.UnifiedViewModel

/**
 * 🎯 Единое приложение WireGuard - современный дизайн
 * 
 * Поток:
 * 1. Красивый экран авторизации 
 * 2. Автоматический переход к списку клиентов
 * 3. Все в едином стиле glassmorphism
 */
class UnifiedMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Полноэкранный режим
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Скрываем системные панели полностью
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
        
        // Прозрачные цвета для системных панелей
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        setContent {
            WgAndroidTheme {
                UnifiedApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedApp() {
    val context = LocalContext.current
    val viewModel: UnifiedViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // Фоновый градиент по всему экрану
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlassColors.primaryGradientStart,
                        GlassColors.primaryGradientEnd
                    )
                )
            )
    ) {
        when {
            // Показываем экран авторизации если не подключены
            !uiState.isAuthenticated -> {
                UnifiedAuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = { 
                        viewModel.onAuthenticationSuccess()
                    }
                )
            }
            
            // Показываем клиентов после успешной авторизации
            else -> {
                UnifiedClientsScreen(
                    viewModel = viewModel,
                    onLogout = {
                        viewModel.logout()
                    }
                )
            }
        }
    }
} 