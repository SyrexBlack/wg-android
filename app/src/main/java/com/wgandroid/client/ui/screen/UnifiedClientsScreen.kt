package com.wgandroid.client.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.ui.component.CreateClientDialog
import com.wgandroid.client.ui.component.DeleteClientDialog
import com.wgandroid.client.ui.component.QRCodeDialog
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.UnifiedViewModel
import com.wgandroid.client.utils.FileDownloader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedClientsScreen(
    viewModel: UnifiedViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        
        // Красивый заголовок с статистикой
        ClientsHeader(
            totalClients = uiState.totalClients,
            activeClients = uiState.activeClients,
            onlineClients = uiState.onlineClients,
            totalTraffic = uiState.totalTraffic,
            currentDownloadSpeed = uiState.currentTotalDownloadSpeed,
            currentUploadSpeed = uiState.currentTotalUploadSpeed,
            serverStatus = uiState.serverStatus,
            isRefreshing = uiState.isRefreshing,
            autoRefreshEnabled = uiState.autoRefreshEnabled,
            lastUpdateTime = uiState.lastUpdateTime,
            onRefresh = viewModel::refreshClients,
            onAddClient = viewModel::showCreateDialog,
            onToggleAutoRefresh = viewModel::toggleAutoRefresh,
            onLogout = onLogout
        )
        
        // Список клиентов
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.clients.isEmpty() && !uiState.isRefreshing) {
                EmptyState(onAddClient = viewModel::showCreateDialog)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.clients,
                        key = { client -> client.id }
                    ) { client ->
                        ModernClientCard(
                            client = client,
                            isOnline = viewModel.isClientOnline(client),
                            timeSinceLastConnection = viewModel.getTimeSinceLastConnection(client),
                            onToggleEnabled = remember(client.id) { 
                                { viewModel.toggleClientEnabled(client) } 
                            },
                            onDelete = remember(client.id) { 
                                { viewModel.showDeleteDialog(client) } 
                            },
                            onDownloadConfig = remember(client.id) { {
                                // TODO: Implement config download
                            }},
                            onShowQRCode = remember(client.id) { 
                                { viewModel.showQRCode(client) } 
                            }
                        )
                    }
                    
                    // Padding для красоты внизу
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
            
            // Индикатор загрузки
            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = GlassColors.accentBlue,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
        
        // Отображение ошибок
        uiState.clientsError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Диалоги
    if (uiState.showCreateDialog) {
        CreateClientDialog(
            onDismiss = viewModel::hideCreateDialog,
            onCreateClient = { name ->
                viewModel.createClient(name)
            }
        )
    }
    
    uiState.clientToDelete?.let { client ->
        DeleteClientDialog(
            client = client,
            onDismiss = viewModel::hideDeleteDialog,
            onConfirmDelete = {
                viewModel.deleteClient(client)
            }
        )
    }
    
    uiState.selectedQRClient?.let { client ->
        QRCodeDialog(
            client = client,
            onDismiss = viewModel::hideQRCode
        )
    }
}

@Composable
fun ClientsHeader(
    totalClients: Int,
    activeClients: Int,
    onlineClients: Int,
    totalTraffic: Long,
    currentDownloadSpeed: Double,
    currentUploadSpeed: Double,
    serverStatus: String,
    isRefreshing: Boolean,
    autoRefreshEnabled: Boolean,
    lastUpdateTime: Long,
    onRefresh: () -> Unit,
    onAddClient: () -> Unit,
    onToggleAutoRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Верхняя строка с заголовком и кнопками
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WireGuard Клиенты",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = serverStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Row {
                    // Auto-refresh toggle
                    IconButton(onClick = onToggleAutoRefresh) {
                        Icon(
                            if (autoRefreshEnabled) Icons.Default.AutoMode else Icons.Default.Schedule,
                            contentDescription = "Автообновление",
                            tint = if (autoRefreshEnabled) GlassColors.accentGreen else Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            if (isRefreshing) Icons.Default.HourglassEmpty else Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = Color.White
                        )
                    }
                    
                    // Add client button
                    IconButton(onClick = onAddClient) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить клиента",
                            tint = GlassColors.accentBlue
                        )
                    }
                    
                    // Logout button
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Выйти",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Статистика - верхний ряд
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem(
                    value = totalClients.toString(),
                    label = "Всего",
                    icon = Icons.Default.People,
                    color = GlassColors.accentBlue
                )
                
                StatisticItem(
                    value = activeClients.toString(),
                    label = "Активных",
                    icon = Icons.Default.CheckCircle,
                    color = GlassColors.accentGreen
                )
                
                StatisticItem(
                    value = onlineClients.toString(),
                    label = "Онлайн",
                    icon = Icons.Default.Wifi,
                    color = if (onlineClients > 0) Color.Green else Color.Gray
                )
                
                StatisticItem(
                    value = formatBytes(totalTraffic),
                    label = "Трафик",
                    icon = Icons.Default.DataUsage,
                    color = GlassColors.accentPurple
                )
            }
            
            // Real-time скорости
            if (currentDownloadSpeed > 0 || currentUploadSpeed > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RealTimeSpeedItem(
                        speed = formatSpeed(currentDownloadSpeed),
                        label = "↓ Скачивание",
                        color = GlassColors.accentBlue
                    )
                    
                    RealTimeSpeedItem(
                        speed = formatSpeed(currentUploadSpeed),
                        label = "↑ Отправка",
                        color = GlassColors.accentPurple
                    )
                    
                    // Последнее обновление
                    if (lastUpdateTime > 0) {
                        RealTimeSpeedItem(
                            speed = formatLastUpdate(lastUpdateTime),
                            label = "Обновлено",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModernClientCard(
    client: WireguardClient,
    isOnline: Boolean,
    timeSinceLastConnection: String,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onDownloadConfig: () -> Unit,
    onShowQRCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 16, alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Заголовок клиента
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "IP: ${client.address}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Статус (онлайн/офлайн + включен/выключен)
                StatusIndicator(
                    isEnabled = client.enabled,
                    isOnline = isOnline
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Трафик - исправленное отображение
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrafficIndicator(
                    label = "Получено",
                    bytes = client.transferRx,
                    currentSpeed = client.transferRxCurrent,
                    icon = Icons.Default.Download,
                    color = GlassColors.accentBlue
                )
                
                TrafficIndicator(
                    label = "Отправлено",
                    bytes = client.transferTx,
                    currentSpeed = client.transferTxCurrent,
                    icon = Icons.Default.Upload,
                    color = GlassColors.accentPurple
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Последнее подключение - real-time отображение
            LastConnectionIndicator(
                timeSinceConnection = timeSinceLastConnection,
                isOnline = isOnline
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle enabled
                Button(
                    onClick = onToggleEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (client.enabled) Color.Red.copy(alpha = 0.7f) 
                                       else GlassColors.accentGreen.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (client.enabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (client.enabled) "Отключить" else "Включить",
                        fontSize = 14.sp
                    )
                }
                
                // QR Code
                IconButton(
                    onClick = onShowQRCode,
                    modifier = Modifier
                        .background(
                            GlassColors.accentBlue.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "QR код",
                        tint = Color.White
                    )
                }
                
                // Download config
                IconButton(
                    onClick = onDownloadConfig,
                    modifier = Modifier
                        .background(
                            GlassColors.accentPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Скачать",
                        tint = Color.White
                    )
                }
                
                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(
                            Color.Red.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    isEnabled: Boolean,
    isOnline: Boolean
) {
    Column(
        horizontalAlignment = Alignment.End
    ) {
        // Онлайн/офлайн статус
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) {
                    Color.Green.copy(alpha = 0.3f)
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isOnline) Color.Green else Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isOnline) "Онлайн" else "Офлайн",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Включен/выключен статус
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) {
                    GlassColors.accentGreen.copy(alpha = 0.3f)
                } else {
                    Color.Red.copy(alpha = 0.3f)
                }
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isEnabled) GlassColors.accentGreen else Color.Red
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEnabled) "Активен" else "Неактивен",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TrafficIndicator(
    label: String,
    bytes: Long,
    currentSpeed: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
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
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        
        // Показываем текущую скорость если есть
        if (currentSpeed > 0) {
            Text(
                text = "${formatBytes(currentSpeed.toLong())}/s",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LastConnectionIndicator(
    timeSinceConnection: String,
    isOnline: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOnline) Icons.Default.AccessTime else Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isOnline) Color.Green else Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isOnline) "Онлайн сейчас" else "Последнее подключение: $timeSinceConnection",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOnline) Color.Green else Color.White.copy(alpha = 0.8f),
            fontWeight = if (isOnline) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun EmptyState(onAddClient: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DevicesOther,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Клиентов пока нет",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Добавьте первого клиента для начала работы",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddClient,
            colors = ButtonDefaults.buttonColors(containerColor = GlassColors.accentBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить клиента")
        }
    }
}

// Utility functions - ИСПРАВЛЕННЫЕ для правильного отображения

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}

private fun formatLastHandshake(lastHandshake: String?, isEnabled: Boolean): String {
    if (lastHandshake.isNullOrEmpty()) {
        return if (isEnabled) "Никогда" else "Отключен"
    }
    
    return try {
        // Парсим дату в формате ISO 8601
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(lastHandshake)
        
        if (date != null) {
            val now = Date()
            val diffMs = now.time - date.time
            
            when {
                diffMs < 60_000 -> "Только что"
                diffMs < 3600_000 -> "${(diffMs / 60_000).toInt()} мин назад"
                diffMs < 86400_000 -> "${(diffMs / 3600_000).toInt()} ч назад"
                diffMs < 2592000_000L -> "${(diffMs / 86400_000).toInt()} дн назад"
                else -> {
                    val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } else {
            "Неизвестно"
        }
    } catch (e: Exception) {
        // Если не удалось распарсить, попробуем другой формат
        try {
            val alternativeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = alternativeFormat.parse(lastHandshake)
            if (date != null) {
                val now = Date()
                val diffMs = now.time - date.time
                when {
                    diffMs < 60_000 -> "Только что"
                    diffMs < 3600_000 -> "${(diffMs / 60_000).toInt()} мин назад" 
                    diffMs < 86400_000 -> "${(diffMs / 3600_000).toInt()} ч назад"
                    else -> "${(diffMs / 86400_000).toInt()} дн назад"
                }
            } else "Неизвестно"
        } catch (e2: Exception) {
            "Неизвестно"
        }
    }
}

// НОВЫЕ КОМПОНЕНТЫ И ФУНКЦИИ ДЛЯ REAL-TIME МОНИТОРИНГА

@Composable
fun RealTimeSpeedItem(
    speed: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = speed,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

private fun formatSpeed(speedBytes: Double): String {
    if (speedBytes <= 0) return "0 B/s"
    
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    val digitGroups = (Math.log10(speedBytes) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
    
    return String.format(
        "%.1f %s",
        speedBytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}

private fun formatLastUpdate(lastUpdateTime: Long): String {
    if (lastUpdateTime <= 0) return ""
    
    val now = System.currentTimeMillis()
    val diffSeconds = (now - lastUpdateTime) / 1000
    
    return when {
        diffSeconds < 10 -> "сейчас"
        diffSeconds < 60 -> "${diffSeconds}с"
        else -> "${diffSeconds / 60}м"
    }
} 