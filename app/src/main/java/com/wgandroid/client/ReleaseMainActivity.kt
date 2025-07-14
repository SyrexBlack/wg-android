package com.wgandroid.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wgandroid.client.data.api.CachedSmartApiClient
import com.wgandroid.client.ui.screen.*
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.theme.WgAndroidTheme
import com.wgandroid.client.utils.DebugUtils

class ReleaseMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализируем кешированный API клиент
        CachedSmartApiClient.initialize(this)
        
        setContent {
            WgAndroidTheme {
                ReleaseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Определяем вкладки нижней навигации
    val bottomNavItems = listOf(
        BottomNavItem(
            route = "connection",
            icon = Icons.Default.Settings,
            title = "Подключение"
        ),
        BottomNavItem(
            route = "clients", 
            icon = Icons.Default.List,
            title = "Клиенты"
        ),
        BottomNavItem(
            route = "diagnostics",
            icon = Icons.Default.Analytics,
            title = "Диагностика"
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = GlassColors.backgroundGradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                GlassBottomNavigationBar(
                    navController = navController,
                    items = bottomNavItems
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "connection",
                modifier = Modifier.padding(paddingValues)
            ) {
                // Вкладка подключения
                composable("connection") {
                    ConnectionScreen(
                        onConnectionEstablished = {
                            // Переключаемся на вкладку клиентов при успешном подключении
                            navController.navigate("clients") {
                                popUpTo("connection") { inclusive = false }
                            }
                        }
                    )
                }
                
                // Вкладка клиентов
                composable("clients") {
                    ClientsTabScreen()
                }
                
                // Вкладка диагностики
                composable("diagnostics") {
                    DiagnosticsTabScreen()
                }
            }
        }
        
        // Показываем информацию о сборке только в debug режиме
        DebugUtils.debugOnly {
            BuildInfoOverlay()
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
)

@Composable
fun GlassBottomNavigationBar(
    navController: androidx.navigation.NavController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 0, alpha = 0.4f))
    ) {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) {
                                androidx.compose.ui.graphics.Color.White
                            } else {
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            }
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            color = if (isSelected) {
                                androidx.compose.ui.graphics.Color.White
                            } else {
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.ui.graphics.Color.White,
                        unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                        selectedTextColor = androidx.compose.ui.graphics.Color.White,
                        unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                        indicatorColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
fun BuildInfoOverlay() {
    var showBuildInfo by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        // Кнопка для показа информации о сборке
        FloatingActionButton(
            onClick = { showBuildInfo = !showBuildInfo },
            modifier = Modifier
                .size(40.dp)
                .then(GlassModifiers.glassFAB()),
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Информация о сборке",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Панель с информацией о сборке
        if (showBuildInfo) {
            Card(
                modifier = Modifier
                    .padding(top = 50.dp)
                    .then(GlassModifiers.glassCard(cornerRadius = 12, alpha = 0.4f)),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Информация о сборке",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = DebugUtils.getBuildInfo(),
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Информация о кеше
                    val cacheInfo = buildString {
                        append("Кешированная сессия: ${CachedSmartApiClient.hasValidSession()}\n")
                        append("Кешированный URL: ${CachedSmartApiClient.getCachedServerUrl() ?: "Нет"}")
                    }
                    
                    Text(
                        text = cacheInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showBuildInfo = false },
                        modifier = Modifier.then(
                            GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.3f)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Text(
                            "Закрыть",
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
} 