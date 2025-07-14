package com.wgandroid.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wgandroid.client.ui.screen.*
import com.wgandroid.client.ui.theme.WgAndroidTheme
import com.wgandroid.client.utils.WGNotificationManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализируем канал уведомлений
        WGNotificationManager.createNotificationChannel(this)
        
        try {
            setContent {
                WgAndroidTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        } catch (e: Exception) {
            // Log error if needed
            e.printStackTrace()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToClients = { navController.navigate("clients") },
                onNavigateToSmart = { navController.navigate("smart") },
                onNavigateToStep = { navController.navigate("step") },
                onNavigateToDiagnostics = { navController.navigate("diagnostics") }
            )
        }
        composable("clients") {
            SmartClientsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToStats = { navController.navigate("stats") }
            )
        }
        composable("stats") {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SmartAppSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("smart") {
            SmartSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("step") {
            StepByStepSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("diagnostics") {
            NetworkDiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToClients: () -> Unit,
    onNavigateToSmart: () -> Unit,
    onNavigateToStep: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔧 Syrex WG",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "WireGuard Easy Android Client",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main App Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🚀 Основное приложение",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Управление клиентами WireGuard с умной авторизацией",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onNavigateToClients,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ОТКРЫТЬ ПРИЛОЖЕНИЕ")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Diagnostic Tools
        Text(
            text = "🔧 Инструменты диагностики:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Smart API Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🧠 Smart API",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Автоматический поиск формата авторизации",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onNavigateToSmart) {
                    Text("Открыть")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Network Diagnostics Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔍 Диагностика сети",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Подробный анализ подключения",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onNavigateToDiagnostics) {
                    Text("Открыть")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Step by Step Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔧 Пошаговая диагностика",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ручное тестирование API",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onNavigateToStep) {
                    Text("Открыть")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info
        Text(
            text = "Версия: Smart Auth v2.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 