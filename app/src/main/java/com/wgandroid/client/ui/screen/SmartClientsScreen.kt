package com.wgandroid.client.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.ui.component.CreateClientDialog
import com.wgandroid.client.ui.component.DeleteClientDialog
import com.wgandroid.client.ui.component.QRCodeDialog
import com.wgandroid.client.ui.theme.StatusConnected
import com.wgandroid.client.ui.theme.StatusDisconnected
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.SmartClientsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartClientsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    viewModel: SmartClientsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showQRCodeDialog by viewModel.showQRCodeDialog.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Show error snackbar
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Auto-clear error after showing
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = GlassColors.backgroundGradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = GlassModifiers.glassTopBar(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = androidx.compose.ui.graphics.Color.White,
                        navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                        actionIconContentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    title = { 
                        Column {
                            Text(
                                "WireGuard Клиенты",
                                fontWeight = FontWeight.Medium,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            uiState.serverStatus?.let { status ->
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Назад",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    },
                    actions = {
                        // Auto refresh toggle
                        IconButton(
                            onClick = { viewModel.toggleAutoRefresh() }
                        ) {
                            Icon(
                                imageVector = if (uiState.autoRefreshEnabled) {
                                    Icons.Default.Timer
                                } else {
                                    Icons.Default.TimerOff
                                },
                                contentDescription = if (uiState.autoRefreshEnabled) {
                                    "Отключить автообновление"
                                } else {
                                    "Включить автообновление"
                                },
                                tint = if (uiState.autoRefreshEnabled) {
                                    androidx.compose.ui.graphics.Color.White
                                } else {
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                }
                            )
                        }
                        
                        // Manual refresh
                        IconButton(
                            onClick = { viewModel.refreshClients() },
                            enabled = !uiState.isRefreshing
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = "Обновить",
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                        
                        IconButton(onClick = onNavigateToStats) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Статистика",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings, 
                                contentDescription = "Настройки",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (uiState.isServerConfigured) {
                    FloatingActionButton(
                        onClick = { viewModel.showCreateDialog() },
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = GlassModifiers.glassFAB()
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Добавить клиента",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    !uiState.isServerConfigured -> {
                        // Server not configured state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Сервер не настроен",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Для начала работы необходимо настроить подключение к серверу WireGuard Easy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        GlassModifiers.glassCardColored(
                                            gradient = GlassColors.glassGradientBlue,
                                            cornerRadius = 16
                                        )
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            ) {
                                Icon(
                                    Icons.Default.Settings, 
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "ПЕРЕЙТИ В НАСТРОЙКИ",
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                    
                    uiState.isLoading && uiState.clients.isEmpty() -> {
                        // Initial loading state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Загрузка клиентов...",
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                    
                    uiState.clients.isEmpty() && !uiState.isLoading -> {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Клиенты не найдены",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Создайте первого клиента для начала работы",
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { viewModel.showCreateDialog() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        GlassModifiers.glassCardColored(
                                            gradient = GlassColors.glassGradientBlue,
                                            cornerRadius = 16
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
                                    "СОЗДАТЬ КЛИЕНТА",
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                    
                    else -> {
                        // Clients list with performance optimizations
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.clients,
                                key = { client -> client.id } // Оптимизация: добавляем ключ для избежания ненужных recomposition
                            ) { client ->
                                SmartClientCard(
                                    client = client,
                                    onToggleEnabled = { viewModel.toggleClientEnabled(client, context) },
                                    onDelete = { viewModel.showDeleteDialog(client) },
                                    onDownloadConfig = {
                                        viewModel.downloadClientConfig(context, client)
                                    },
                                    onCopyConfig = {
                                        viewModel.getClientConfig(client) { config ->
                                            clipboardManager.setText(AnnotatedString(config))
                                        }
                                    },
                                    onShowQRCode = { viewModel.showQRCodeDialog(client) }
                                )
                            }
                        }
                    }
                }
                
                // Loading overlay
                if (uiState.isLoading && uiState.clients.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                
                // Error message
                uiState.errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                            .then(GlassModifiers.glassCard(cornerRadius = 12, alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
        
        // QR Code Dialog
        showQRCodeDialog?.let { qrDialogState ->
            QRCodeDialog(
                client = qrDialogState.client,
                onDismiss = { viewModel.hideQRCodeDialog() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartClientCard(
    client: WireguardClient,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onDownloadConfig: () -> Unit,
    onCopyConfig: () -> Unit,
    onShowQRCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Мемоизируем действия для предотвращения ненужных recomposition
    val memoizedToggle = remember(client.id, client.enabled) { onToggleEnabled }
    val memoizedDelete = remember(client.id) { onDelete }
    val memoizedDownload = remember(client.id) { onDownloadConfig }
    val memoizedCopy = remember(client.id) { onCopyConfig }
    val memoizedQR = remember(client.id) { onShowQRCode }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = androidx.compose.ui.graphics.Color.White
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status indicator
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(
                            color = if (client.enabled) StatusConnected else StatusDisconnected
                        )
                    }
                    
                    Text(
                        text = if (client.enabled) "Включен" else "Отключен",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Client info
            Text(
                text = "IP: ${client.address}",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
            )
            
            // Creation date
            client.createdAt?.let { createdAt ->
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(createdAt)
                } catch (e: Exception) {
                    null
                }
                
                if (date != null) {
                    Text(
                        text = "Последнее подключение: ${dateFormat.format(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Traffic info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "↓ ${formatBytes(client.transferRx)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "↑ ${formatBytes(client.transferTx)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row: Toggle and QR code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle button
                    Button(
                        onClick = memoizedToggle,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                GlassModifiers.glassCardColored(
                                    gradient = if (client.enabled) {
                                        GlassColors.glassGradientPurple
                                    } else {
                                        GlassColors.glassGradientBlue
                                    },
                                    cornerRadius = 12
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
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    
                    // QR Code button
                    Button(
                        onClick = memoizedQR,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                GlassModifiers.glassCardColored(
                                    gradient = GlassColors.glassGradient,
                                    cornerRadius = 12
                                )
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "QR код",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                
                // Second row: Download and Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download file button
                    OutlinedButton(
                        onClick = memoizedDownload,
                        modifier = Modifier
                            .weight(1f)
                            .then(GlassModifiers.glassButton(cornerRadius = 12, alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        border = null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Скачать",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    
                    // Copy to clipboard button  
                    OutlinedButton(
                        onClick = memoizedCopy,
                        modifier = Modifier
                            .weight(1f)
                            .then(GlassModifiers.glassButton(cornerRadius = 12, alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        border = null
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Копировать",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                
                // Third row: Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Delete button
                    OutlinedButton(
                        onClick = memoizedDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                GlassModifiers.glassCardColored(
                                    gradient = Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0x50F44336),
                                            androidx.compose.ui.graphics.Color(0x30D32F2F),
                                            androidx.compose.ui.graphics.Color(0x10B71C1C)
                                        )
                                    ),
                                    cornerRadius = 12
                                )
                            ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        border = null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(16.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Удалить клиента",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
} 