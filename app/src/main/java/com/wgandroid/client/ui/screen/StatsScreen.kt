package com.wgandroid.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.StatsViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    clients: List<WireguardClient> = emptyList(),
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(clients) {
        viewModel.updateClients(clients)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = GlassColors.backgroundGradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = GlassModifiers.glassTopBar(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = { 
                        Text(
                            "📊 Статистика",
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshStats() }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Обновить статистику",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.exportStats() }
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Экспорт статистики",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Общая статистика
                item {
                    OverallStatsCard(
                        totalClients = uiState.totalClients,
                        activeClients = uiState.activeClients,
                        totalDownload = uiState.totalDownload,
                        totalUpload = uiState.totalUpload
                    )
                }
                
                // График общего трафика
                item {
                    TrafficChartCard(
                        downloadData = uiState.downloadHistory,
                        uploadData = uiState.uploadHistory,
                        title = "📈 Общий трафик (последние 7 дней)"
                    )
                }
                
                // Топ клиентов по трафику
                item {
                    TopClientsCard(
                        topClients = uiState.topClientsByTraffic,
                        title = "🏆 Топ клиентов по трафику"
                    )
                }
                
                // Активность по времени
                item {
                    ActivityTimeCard(
                        hourlyActivity = uiState.hourlyActivity,
                        title = "🕐 Активность по времени"
                    )
                }
                
                // Детальная статистика по клиентам
                items(uiState.clientStats) { clientStat ->
                    ClientStatsCard(clientStat = clientStat)
                }
            }
        }
    }
}

@Composable
fun OverallStatsCard(
    totalClients: Int,
    activeClients: Int,
    totalDownload: Long,
    totalUpload: Long
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
                text = "📊 Общая статистика",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = "👥",
                    value = "$totalClients",
                    label = "Всего клиентов",
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    icon = "✅",
                    value = "$activeClients",
                    label = "Активных",
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    icon = "📥",
                    value = formatBytes(totalDownload),
                    label = "Скачано",
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    icon = "📤",
                    value = formatBytes(totalUpload),
                    label = "Загружено",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrafficChartCard(
    downloadData: List<FloatEntry>,
    uploadData: List<FloatEntry>,
    title: String
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (downloadData.isNotEmpty() || uploadData.isNotEmpty()) {
                
                // Упрощенный график с Canvas (пока Vico сложен)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Пока временная заглушка для графика
                    SimpleTrafficChart(
                        downloadData = downloadData,
                        uploadData = uploadData
                    )
                }
                
                // Легенда
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = Color(0xFF4FC3F7),
                        label = "📥 Скачивание"
                    )
                    LegendItem(
                        color = Color(0xFF81C784),
                        label = "📤 Загрузка"
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊 Данные отсутствуют",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TopClientsCard(
    topClients: List<ClientTrafficStat>,
    title: String
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (topClients.isNotEmpty()) {
                topClients.forEachIndexed { index, client ->
                    ClientTrafficItem(
                        rank = index + 1,
                        clientName = client.name,
                        traffic = client.totalTraffic,
                        isLast = index == topClients.lastIndex
                    )
                }
            } else {
                Text(
                    text = "🤷‍♂️ Нет данных о трафике",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ClientTrafficItem(
    rank: Int,
    clientName: String,
    traffic: Long,
    isLast: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            when (rank) {
                                1 -> Color(0xFFFFD700) // Gold
                                2 -> Color(0xFFC0C0C0) // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
                                else -> Color.White.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.Black else Color.White
                    )
                }
                
                Text(
                    text = clientName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = formatBytes(traffic),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        if (!isLast) {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ActivityTimeCard(
    hourlyActivity: Map<Int, Int>,
    title: String
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Простая визуализация активности по часам
            val maxActivity = hourlyActivity.values.maxOrNull() ?: 1
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..23).forEach { hour ->
                    val activity = hourlyActivity[hour] ?: 0
                    val height = if (maxActivity > 0) {
                        ((activity.toFloat() / maxActivity) * 40).dp
                    } else 2.dp
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(height.coerceAtLeast(2.dp))
                                .background(
                                    if (activity > 0) {
                                        Color(0xFF4FC3F7)
                                    } else {
                                        Color.White.copy(alpha = 0.2f)
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        
                        if (hour % 6 == 0) {
                            Text(
                                text = "${hour}h",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientStatsCard(
    clientStat: ClientStatistic
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 16, alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clientStat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Text(
                    text = if (clientStat.isActive) "🟢 Активен" else "🔴 Неактивен",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📥 ${formatBytes(clientStat.downloadBytes)}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "📤 ${formatBytes(clientStat.uploadBytes)}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            clientStat.lastSeen?.let { lastSeen ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Последний раз: $lastSeen",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

data class ClientTrafficStat(
    val name: String,
    val totalTraffic: Long
)

data class ClientStatistic(
    val name: String,
    val isActive: Boolean,
    val downloadBytes: Long,
    val uploadBytes: Long,
    val lastSeen: String?
)

@Composable
fun SimpleTrafficChart(
    downloadData: List<FloatEntry>,
    uploadData: List<FloatEntry>
) {
    if (downloadData.isEmpty() && uploadData.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Данные графика будут здесь",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "после настройки Vico Charts",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Простая визуализация с барами
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(7) { day ->
                val downloadValue = downloadData.getOrNull(day)?.y ?: 0f
                val uploadValue = uploadData.getOrNull(day)?.y ?: 0f
                val maxValue = maxOf(downloadValue, uploadValue, 1f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    // Download bar
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(((downloadValue / maxValue) * 80).dp.coerceAtLeast(2.dp))
                            .background(
                                Color(0xFF4FC3F7),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Upload bar
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(((uploadValue / maxValue) * 80).dp.coerceAtLeast(2.dp))
                            .background(
                                Color(0xFF81C784),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Д${day + 1}",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
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