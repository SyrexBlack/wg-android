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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.ConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    onConnectionEstablished: () -> Unit,
    viewModel: ConnectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Инициализируем ViewModel с контекстом
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Автоматически переходим к клиентам при успешном подключении
    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) {
            onConnectionEstablished()
        }
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
            contentPadding = PaddingValues(vertical = 32.dp)
        ) {
            item {
                // Заголовок приложения
                AppHeaderCard()
            }
            
            item {
                // Статус подключения
                ConnectionStatusCard(
                    isConnected = uiState.isConnected,
                    serverUrl = uiState.serverUrl,
                    connectionFormat = uiState.connectionFormat,
                    clientCount = uiState.clientCount
                )
            }
            
            if (!uiState.isConnected) {
                item {
                    // Настройки подключения
                    ServerConfigurationCard(
                        serverUrl = uiState.serverUrl,
                        serverPassword = uiState.serverPassword,
                        onServerUrlChange = viewModel::updateServerUrl,
                        onServerPasswordChange = viewModel::updateServerPassword,
                        onConnect = viewModel::connectToServer,
                        isLoading = uiState.isLoading,
                        connectionStatus = uiState.connectionStatus,
                        urlError = uiState.urlError
                    )
                }
            } else {
                item {
                    // Быстрые действия для подключенного состояния
                    QuickActionsCard(
                        onReconnect = viewModel::reconnect,
                        onDisconnect = viewModel::disconnect,
                        onRefreshClients = viewModel::refreshClients,
                        isLoading = uiState.isLoading
                    )
                }
            }
            
            item {
                // Информация о версии
                VersionInfoCard()
            }
        }
    }
}

@Composable
fun AppHeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Router,
                contentDescription = "WireGuard",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "WireGuard Easy Client",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Управление клиентами WireGuard",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    serverUrl: String,
    connectionFormat: String?,
    clientCount: Int?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка статуса
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = "Статус подключения",
                tint = if (isConnected) {
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                } else {
                    androidx.compose.ui.graphics.Color(0xFFFF9800)
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected) "Подключено" else "Не подключено",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                
                if (isConnected && serverUrl.isNotEmpty()) {
                    Text(
                        text = serverUrl.take(40) + if (serverUrl.length > 40) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                    )
                    
                    connectionFormat?.let { format ->
                        Text(
                            text = "Формат: $format",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    clientCount?.let { count ->
                        Text(
                            text = "Клиентов: $count",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Text(
                        text = "Настройте подключение к серверу",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ServerConfigurationCard(
    serverUrl: String,
    serverPassword: String,
    onServerUrlChange: (String) -> Unit,
    onServerPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    isLoading: Boolean,
    connectionStatus: String?,
    urlError: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Настройки сервера",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            // URL поле
            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = { 
                    Text(
                        "URL сервера",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                    ) 
                },
                placeholder = { 
                    Text(
                        "http://example.com:51821",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                    ) 
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = urlError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                    cursorColor = androidx.compose.ui.graphics.Color.White
                )
            )
            
            if (urlError != null) {
                Text(
                    text = urlError,
                    color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Пароль поле
            OutlinedTextField(
                value = serverPassword,
                onValueChange = onServerPasswordChange,
                label = { 
                    Text(
                        "Пароль (опционально)",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                    ) 
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                    cursorColor = androidx.compose.ui.graphics.Color.White
                )
            )
            
            // Кнопка подключения
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        GlassModifiers.glassCardColored(
                            gradient = GlassColors.glassGradientBlue,
                            cornerRadius = 12
                        )
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                enabled = !isLoading && serverUrl.isNotBlank() && urlError == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = if (isLoading) "Подключение..." else "Подключиться",
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
            
            // Статус подключения
            connectionStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.contains("✅")) {
                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFFFF6B6B)
                    }
                )
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshClients: () -> Unit,
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
                text = "Быстрые действия",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Переподключиться
                OutlinedButton(
                    onClick = onReconnect,
                    modifier = Modifier
                        .weight(1f)
                        .then(GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null,
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Обновить", color = androidx.compose.ui.graphics.Color.White)
                }
                
                // Отключиться
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .weight(1f)
                        .then(GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null,
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.PowerOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Отключить", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}

@Composable
fun VersionInfoCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 20, alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WireGuard Easy Client v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Создано для упрощения управления WireGuard",
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
} 