package com.wgandroid.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.ui.component.CreateClientDialog
import com.wgandroid.client.ui.component.DeleteClientDialog
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.OptimizedClientsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsTabScreen(
    viewModel: OptimizedClientsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header с статистикой
            ClientsHeaderCard(
                clientCount = uiState.clients.size,
                activeClients = uiState.clients.count { it.enabled },
                onRefresh = { viewModel.refreshClients() },
                onCreateClient = { viewModel.showCreateDialog() },
                isLoading = uiState.isLoading
            )
            
            when {
                uiState.isLoading && uiState.clients.isEmpty() -> {
                    LoadingStateCard()
                }
                
                !uiState.isServerConfigured -> {
                    NotConfiguredStateCard()
                }
                
                uiState.clients.isEmpty() -> {
                    EmptyStateCard(
                        onCreateClient = { viewModel.showCreateDialog() }
                    )
                }
                
                else -> {
                    OptimizedClientsList(
                        clients = uiState.clients,
                        onToggleEnabled = { client -> viewModel.toggleClientEnabled(client, context) },
                        onDelete = { client -> viewModel.showDeleteDialog(client) },
                        onDownloadConfig = { client -> viewModel.downloadClientConfig(context, client) },
                        onCopyConfig = { client -> viewModel.copyClientConfig(context, client) },
                        onShowQRCode = { client -> viewModel.showQRCodeForClient(client) },
                        isRefreshing = uiState.isRefreshing
                    )
                }
            }
        }
        
        // Error snackbar
        uiState.errorMessage?.let { error ->
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
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
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
    
    // Dialogs
    if (showCreateDialog) {
        CreateClientDialog(
            onCreateClient = { name -> viewModel.createClient(name, context) },
            onDismiss = { viewModel.hideCreateDialog() }
        )
    }
    
    showDeleteDialog?.let { client ->
        DeleteClientDialog(
            client = client,
            onConfirmDelete = { viewModel.deleteClient(client) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }
}

@Composable
fun ClientsHeaderCard(
    clientCount: Int,
    activeClients: Int,
    onRefresh: () -> Unit,
    onCreateClient: () -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WireGuard Клиенты",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        text = "Всего: $clientCount • Активных: $activeClients",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {
                        Icon(
                            if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    
                    IconButton(onClick = onCreateClient) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить клиента",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizedClientsList(
    clients: List<WireguardClient>,
    onToggleEnabled: (WireguardClient) -> Unit,
    onDelete: (WireguardClient) -> Unit,
    onDownloadConfig: (WireguardClient) -> Unit,
    onCopyConfig: (WireguardClient) -> Unit,
    onShowQRCode: (WireguardClient) -> Unit,
    isRefreshing: Boolean
) {
    val listState = rememberLazyListState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = clients,
                key = { client -> client.id } // Важно для производительности!
            ) { client ->
                OptimizedClientCard(
                    client = client,
                    onToggleEnabled = remember(client.id) { { onToggleEnabled(client) } },
                    onDelete = remember(client.id) { { onDelete(client) } },
                    onDownloadConfig = remember(client.id) { { onDownloadConfig(client) } },
                    onCopyConfig = remember(client.id) { { onCopyConfig(client) } },
                    onShowQRCode = remember(client.id) { { onShowQRCode(client) } }
                )
            }
            
            // Padding для bottom navigation
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = androidx.compose.ui.graphics.Color.White,
                trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedClientCard(
    client: WireguardClient,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onDownloadConfig: () -> Unit,
    onCopyConfig: () -> Unit,
    onShowQRCode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 16, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = client.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
                
                // Status indicator
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (client.enabled) {
                            androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.3f)
                        } else {
                            androidx.compose.ui.graphics.Color(0xFFFF5722).copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier.then(
                        GlassModifiers.glassCard(cornerRadius = 8, alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (client.enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (client.enabled) {
                                androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            } else {
                                androidx.compose.ui.graphics.Color(0xFFFF5722)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (client.enabled) "Активен" else "Неактивен",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Traffic and connection info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Upload traffic
                TrafficInfo(
                    label = "Отправлено",
                    bytes = client.transferTx,
                    currentSpeed = client.transferTxCurrent,
                    icon = Icons.Default.Upload
                )
                
                // Download traffic  
                TrafficInfo(
                    label = "Получено",
                    bytes = client.transferRx,
                    currentSpeed = client.transferRxCurrent,
                    icon = Icons.Default.Download
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Last connection
            LastConnectionInfo(
                lastHandshake = client.latestHandshakeAt,
                isEnabled = client.enabled
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle button
                Button(
                    onClick = onToggleEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            GlassModifiers.glassCardColored(
                                gradient = if (client.enabled) {
                                    GlassColors.glassGradientPurple
                                } else {
                                    GlassColors.glassGradientBlue
                                },
                                cornerRadius = 8
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (client.enabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (client.enabled) "Отключить" else "Включить",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                // QR Code button
                OutlinedButton(
                    onClick = onShowQRCode,
                    modifier = Modifier.then(
                        GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "QR код",
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                
                // Download button
                OutlinedButton(
                    onClick = onDownloadConfig,
                    modifier = Modifier.then(
                        GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Скачать",
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                
                // Copy button
                OutlinedButton(
                    onClick = onCopyConfig,
                    modifier = Modifier.then(
                        GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Копировать",
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                
                // Delete button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.then(
                        GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color(0xFFFF5722)
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficInfo(
    label: String,
    bytes: Long,
    currentSpeed: Double,
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
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.ui.graphics.Color.White
        )
        
        if (currentSpeed > 0) {
            Text(
                text = "${formatBytes(currentSpeed.toLong())}/s",
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun LastConnectionInfo(
    lastHandshake: String?,
    isEnabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Последнее подключение: ${formatLastHandshake(lastHandshake, isEnabled)}",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
        )
    }
}

// Helper functions
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
        // Парсим ISO 8601 дату
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
        "Неизвестно"
    }
}

@Composable
fun LoadingStateCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Загрузка клиентов...",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun NotConfiguredStateCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Сервер не настроен",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Text(
                    text = "Перейдите на вкладку 'Подключение' для настройки сервера",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    onCreateClient: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Нет клиентов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Text(
                    text = "Создайте первого клиента для начала работы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateClient,
                    modifier = Modifier.then(
                        GlassModifiers.glassCardColored(
                            gradient = GlassColors.glassGradientBlue,
                            cornerRadius = 12
                        )
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Создать клиента",
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
} 