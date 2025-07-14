package com.wgandroid.client.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wgandroid.client.R
import com.wgandroid.client.ui.viewmodel.NetworkDiagnosticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NetworkDiagnosticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "🔧 Диагностика сети",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearLog() }
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Очистить"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.exportLog() }
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Экспорт"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Настройки сервера",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = uiState.serverUrl,
                            onValueChange = viewModel::setServerUrl,
                            label = { Text("URL сервера") },
                            placeholder = { Text("http://example.com:51821") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isRunning
                        )
                        
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::setPassword,
                            label = { Text("Пароль (опционально)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isRunning
                        )
                    }
                }
            }
            
            // Test Controls
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Тесты диагностики",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runBasicTests() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isRunning && uiState.serverUrl.isNotBlank()
                            ) {
                                if (uiState.isRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Базовые")
                            }
                            
                            Button(
                                onClick = { viewModel.runAdvancedTests() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isRunning && uiState.serverUrl.isNotBlank()
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Расширенные")
                            }
                        }
                    }
                }
            }
            
            // Results
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null)
                            Text(
                                text = "Результаты диагностики",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (uiState.logEntries.isEmpty() && !uiState.isRunning) {
                            Text(
                                text = "Запустите диагностику для получения результатов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Log entries
            items(uiState.logEntries) { entry ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            entry.startsWith("✅") -> MaterialTheme.colorScheme.primaryContainer
                            entry.startsWith("❌") -> MaterialTheme.colorScheme.errorContainer
                            entry.startsWith("⚠️") -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                ) {
                    Text(
                        text = entry,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            entry.startsWith("✅") -> MaterialTheme.colorScheme.onPrimaryContainer
                            entry.startsWith("❌") -> MaterialTheme.colorScheme.onErrorContainer
                            entry.startsWith("⚠️") -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            // Status message
            if (uiState.exportResult.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.exportResult.startsWith("✅")) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Text(
                            text = uiState.exportResult,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.exportResult.startsWith("✅")) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }
        }
    }
} 