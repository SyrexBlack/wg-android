package com.wgandroid.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.DiagnosticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsTabScreen(
    viewModel: DiagnosticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Инициализируем ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = GlassColors.backgroundGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Заголовок диагностики
                DiagnosticsHeaderCard()
            }
            
            item {
                // Статус подключения
                ConnectionStatusCard(
                    isConnected = uiState.isConnected,
                    serverUrl = uiState.serverUrl,
                    responseTime = uiState.responseTime,
                    lastCheck = uiState.lastCheck
                )
            }
            
            item {
                // Сетевая диагностика
                NetworkDiagnosticsCard(
                    onPingServer = { viewModel.pingServer() },
                    onTestConnectivity = { viewModel.testConnectivity() },
                    onCheckDNS = { viewModel.checkDNS() },
                    isLoading = uiState.isLoading,
                    pingResult = uiState.pingResult,
                    connectivityResult = uiState.connectivityResult,
                    dnsResult = uiState.dnsResult
                )
            }
            
            item {
                // Информация о сервере
                ServerInfoCard(
                    serverInfo = uiState.serverInfo,
                    clientCount = uiState.clientCount,
                    serverVersion = uiState.serverVersion
                )
            }
            
            item {
                // Статистика приложения
                AppStatisticsCard(
                    totalRequests = uiState.totalRequests,
                    successfulRequests = uiState.successfulRequests,
                    failedRequests = uiState.failedRequests,
                    cacheHits = uiState.cacheHits,
                    cacheMisses = uiState.cacheMisses
                )
            }
            
            item {
                // Инструменты отладки
                DebugToolsCard(
                    onClearCache = { viewModel.clearCache() },
                    onExportLogs = { viewModel.exportLogs(context) },
                    onResetStatistics = { viewModel.resetStatistics() },
                    isLoading = uiState.isLoading
                )
            }
            
            item {
                // Системная информация
                SystemInfoCard(
                    systemInfo = uiState.systemInfo
                )
            }
            
            item {
                // Padding для bottom navigation
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // Error/status message
        uiState.message?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(GlassModifiers.glassCard(cornerRadius = 12, alpha = 0.4f)),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (message.contains("✅")) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (message.contains("✅")) {
                                androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            } else {
                                androidx.compose.ui.graphics.Color.White
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessage() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsHeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "Диагностика",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Диагностика и Инструменты",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Анализ подключения и системные инструменты",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    serverUrl: String?,
    responseTime: Long?,
    lastCheck: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Статус подключения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isConnected) {
                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFFFF5722)
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = if (isConnected) "Подключено" else "Не подключено",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    
                    serverUrl?.let { url ->
                        Text(
                            text = url.take(50) + if (url.length > 50) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            if (isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    responseTime?.let { time ->
                        DiagnosticItem(
                            label = "Время отклика",
                            value = "${time}ms",
                            icon = Icons.Default.Speed
                        )
                    }
                    
                    lastCheck?.let { check ->
                        DiagnosticItem(
                            label = "Последняя проверка",
                            value = check,
                            icon = Icons.Default.Schedule
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkDiagnosticsCard(
    onPingServer: () -> Unit,
    onTestConnectivity: () -> Unit,
    onCheckDNS: () -> Unit,
    isLoading: Boolean,
    pingResult: String?,
    connectivityResult: String?,
    dnsResult: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Сетевая диагностика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопки диагностики
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticButton(
                    text = "Ping",
                    icon = Icons.Default.NetworkPing,
                    onClick = onPingServer,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
                
                DiagnosticButton(
                    text = "Связь",
                    icon = Icons.Default.Wifi,
                    onClick = onTestConnectivity,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
                
                DiagnosticButton(
                    text = "DNS",
                    icon = Icons.Default.Dns,
                    onClick = onCheckDNS,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Результаты диагностики
            if (pingResult != null || connectivityResult != null || dnsResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                pingResult?.let { result ->
                    DiagnosticResult(
                        label = "Ping результат:",
                        result = result
                    )
                }
                
                connectivityResult?.let { result ->
                    DiagnosticResult(
                        label = "Тест связи:",
                        result = result
                    )
                }
                
                dnsResult?.let { result ->
                    DiagnosticResult(
                        label = "DNS проверка:",
                        result = result
                    )
                }
            }
        }
    }
}

@Composable
fun ServerInfoCard(
    serverInfo: String?,
    clientCount: Int?,
    serverVersion: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Информация о сервере",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            serverVersion?.let { version ->
                InfoRow(
                    label = "Версия сервера",
                    value = version,
                    icon = Icons.Default.Info
                )
            }
            
            clientCount?.let { count ->
                InfoRow(
                    label = "Количество клиентов",
                    value = count.toString(),
                    icon = Icons.Default.Group
                )
            }
            
            serverInfo?.let { info ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AppStatisticsCard(
    totalRequests: Int,
    successfulRequests: Int,
    failedRequests: Int,
    cacheHits: Int,
    cacheMisses: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Статистика приложения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Всего запросов",
                    value = totalRequests.toString(),
                    icon = Icons.Default.Api
                )
                
                StatisticItem(
                    label = "Успешных",
                    value = successfulRequests.toString(),
                    icon = Icons.Default.CheckCircle
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Ошибок",
                    value = failedRequests.toString(),
                    icon = Icons.Default.Error
                )
                
                StatisticItem(
                    label = "Кеш попаданий",
                    value = "$cacheHits/$cacheMisses",
                    icon = Icons.Default.Memory
                )
            }
        }
    }
}

@Composable
fun DebugToolsCard(
    onClearCache: () -> Unit,
    onExportLogs: () -> Unit,
    onResetStatistics: () -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Инструменты отладки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugButton(
                    text = "Очистить кеш",
                    icon = Icons.Default.CleaningServices,
                    onClick = onClearCache,
                    enabled = !isLoading
                )
                
                DebugButton(
                    text = "Экспорт логов",
                    icon = Icons.Default.FileDownload,
                    onClick = onExportLogs,
                    enabled = !isLoading
                )
                
                DebugButton(
                    text = "Сбросить статистику",
                    icon = Icons.Default.RestartAlt,
                    onClick = onResetStatistics,
                    enabled = !isLoading
                )
            }
        }
    }
}

@Composable
fun SystemInfoCard(
    systemInfo: Map<String, String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Системная информация",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            systemInfo.forEach { (key, value) ->
                InfoRow(
                    label = key,
                    value = value,
                    icon = Icons.Default.PhoneAndroid
                )
            }
        }
    }
}

// Helper composables
@Composable
fun DiagnosticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
fun DiagnosticButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.then(
            GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        border = null,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = androidx.compose.ui.graphics.Color.White
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun DiagnosticResult(
    label: String,
    result: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.contains("✅")) {
                androidx.compose.ui.graphics.Color(0xFF4CAF50)
            } else if (result.contains("❌")) {
                androidx.compose.ui.graphics.Color(0xFFFF5722)
            } else {
                androidx.compose.ui.graphics.Color.White
            }
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun DebugButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        border = null,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = androidx.compose.ui.graphics.Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
} 