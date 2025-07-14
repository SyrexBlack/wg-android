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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.AppSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClients: () -> Unit,
    viewModel: AppSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Инициализируем ViewModel с контекстом
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
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
                        Text(
                            "Настройки",
                            fontWeight = FontWeight.Medium,
                            color = androidx.compose.ui.graphics.Color.White
                        ) 
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
                        if (uiState.hasValidSession) {
                            IconButton(onClick = onNavigateToClients) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = "Клиенты",
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ServerConfigCard(
                        serverUrl = uiState.serverUrl,
                        serverPassword = uiState.serverPassword,
                        onServerUrlChange = viewModel::updateServerUrl,
                        onServerPasswordChange = viewModel::updateServerPassword,
                        onTestConnection = viewModel::testConnection,
                        isLoading = uiState.isLoading,
                        connectionStatus = uiState.connectionStatus,
                        urlError = uiState.urlError
                    )
                }
                
                if (uiState.hasValidSession) {
                    item {
                        SessionInfoCard(
                            serverUrl = uiState.serverUrl,
                            lastLoginTime = "Активна",
                            onLogout = viewModel::logout
                        )
                    }
                }
                
                item {
                    QuickActionsCard(
                        onNavigateToClients = {
                            if (uiState.hasValidSession) {
                                onNavigateToClients()
                            } else {
                                viewModel.testConnection()
                            }
                        },
                        hasValidSession = uiState.hasValidSession,
                        isLoading = uiState.isLoading
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ServerConfigCard(
    serverUrl: String,
    serverPassword: String,
    onServerUrlChange: (String) -> Unit,
    onServerPasswordChange: (String) -> Unit,
    onTestConnection: () -> Unit,
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
                        "Пароль",
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
            
            // Кнопка тестирования
            Button(
                onClick = onTestConnection,
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
                enabled = !isLoading && serverUrl.isNotBlank()
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
                    text = if (isLoading) "Подключение..." else "Проверить подключение",
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
fun SessionInfoCard(
    serverUrl: String,
    lastLoginTime: String,
    onLogout: () -> Unit
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
                text = "Информация о сессии",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сервер: ${serverUrl.take(30)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Статус: $lastLoginTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    )
                }
                
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.then(
                        GlassModifiers.glassButton(cornerRadius = 8, alpha = 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = null
                ) {
                    Text("Выход", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onNavigateToClients: () -> Unit,
    hasValidSession: Boolean,
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
            
            Button(
                onClick = onNavigateToClients,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        GlassModifiers.glassCardColored(
                            gradient = if (hasValidSession) {
                                GlassColors.glassGradientPurple
                            } else {
                                GlassColors.glassGradientBlue
                            },
                            cornerRadius = 12
                        )
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = if (hasValidSession) Icons.Default.List else Icons.Default.Settings,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasValidSession) {
                        "Перейти к клиентам"
                    } else {
                        "Настроить подключение"
                    },
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
} 