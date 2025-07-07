package com.wgandroid.client.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.R
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.ui.component.CreateClientDialog
import com.wgandroid.client.ui.component.DeleteClientDialog
import com.wgandroid.client.ui.theme.StatusConnected
import com.wgandroid.client.ui.theme.StatusDisconnected
import com.wgandroid.client.ui.viewmodel.ClientsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ClientsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Handle error display
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            // Show snackbar or toast
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.clients_title),
                        fontWeight = FontWeight.Medium
                    ) 
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshClients() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_client)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.clients.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.loading),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                uiState.clients.isEmpty() && !uiState.isLoading -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.no_clients),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.create_first_client),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                else -> {
                    // Client list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.clients) { client ->
                            ClientCard(
                                client = client,
                                onToggleEnabled = { viewModel.toggleClientEnabled(client) },
                                onDelete = { viewModel.showDeleteDialog(client) },
                                onDownloadConfig = { 
                                    viewModel.getClientConfig(client) { config ->
                                        clipboardManager.setText(AnnotatedString(config))
                                        // Show toast that config was copied
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Loading overlay for actions
            if (uiState.isLoading && uiState.clients.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showCreateDialog) {
        CreateClientDialog(
            onCreateClient = { name -> viewModel.createClient(name) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCard(
    client: WireguardClient,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onDownloadConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = client.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier.size(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = if (client.enabled) StatusConnected else StatusDisconnected
                            )
                        }
                    }
                    
                    Text(
                        text = if (client.enabled) stringResource(R.string.client_enabled) 
                               else stringResource(R.string.client_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (client.enabled) StatusConnected else StatusDisconnected
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.data_sent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(client.transferTx),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column {
                    Text(
                        text = stringResource(R.string.data_received),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(client.transferRx),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column {
                    Text(
                        text = stringResource(R.string.last_handshake),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatLastSeen(client.latestHandshakeAt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (client.enabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (client.enabled) "Отключить" else "Включить"
                    )
                }
                
                OutlinedButton(
                    onClick = onDownloadConfig,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = stringResource(R.string.download),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.download))
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "%.1f %s".format(size, units[unitIndex])
}

private fun formatLastSeen(lastHandshake: String?): String {
    if (lastHandshake.isNullOrEmpty()) return "Никогда"
    
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(lastHandshake)
        val now = System.currentTimeMillis()
        val diff = now - (date?.time ?: now)
        
        when {
            diff < 60000 -> "Только что"
            diff < 3600000 -> "${diff / 60000} мин. назад"
            diff < 86400000 -> "${diff / 3600000} ч. назад"
            else -> "${diff / 86400000} дн. назад"
        }
    } catch (e: Exception) {
        "Неизвестно"
    }
} 