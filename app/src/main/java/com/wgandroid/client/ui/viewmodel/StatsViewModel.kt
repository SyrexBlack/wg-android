package com.wgandroid.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.ui.screen.ClientStatistic
import com.wgandroid.client.ui.screen.ClientTrafficStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class StatsUiState(
    val totalClients: Int = 0,
    val activeClients: Int = 0,
    val totalDownload: Long = 0,
    val totalUpload: Long = 0,
    val downloadHistory: List<FloatEntry> = emptyList(),
    val uploadHistory: List<FloatEntry> = emptyList(),
    val topClientsByTraffic: List<ClientTrafficStat> = emptyList(),
    val hourlyActivity: Map<Int, Int> = emptyMap(),
    val clientStats: List<ClientStatistic> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class StatsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    
    private var clients: List<WireguardClient> = emptyList()
    
    fun updateClients(newClients: List<WireguardClient>) {
        clients = newClients
        generateStatistics()
    }
    
    fun refreshStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Симуляция загрузки данных
            kotlinx.coroutines.delay(1000)
            
            generateStatistics()
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun exportStats() {
        viewModelScope.launch {
            try {
                // TODO: Реализовать экспорт статистики в CSV/JSON
                _uiState.value = _uiState.value.copy(
                    errorMessage = "📊 Экспорт статистики будет реализован в следующей версии"
                )
                
                // Auto-clear message
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка экспорта: ${e.message}"
                )
            }
        }
    }
    
    private fun generateStatistics() {
        viewModelScope.launch {
            val totalClients = clients.size
            val activeClients = clients.count { it.enabled }
            val totalDownload = clients.sumOf { it.transferRx }
            val totalUpload = clients.sumOf { it.transferTx }
            
            // Генерируем историю трафика (последние 7 дней)
            val downloadHistory = generateTrafficHistory(totalDownload)
            val uploadHistory = generateTrafficHistory(totalUpload)
            
            // Топ клиентов по трафику
            val topClients = clients
                .map { ClientTrafficStat(it.name, it.transferRx + it.transferTx) }
                .sortedByDescending { it.totalTraffic }
                .take(5)
            
            // Активность по часам (симуляция)
            val hourlyActivity = generateHourlyActivity(activeClients)
            
            // Детальная статистика по клиентам
            val clientStats = clients.map { client ->
                ClientStatistic(
                    name = client.name,
                    isActive = client.enabled,
                    downloadBytes = client.transferRx,
                    uploadBytes = client.transferTx,
                    lastSeen = formatLastSeen(client.createdAt)
                )
            }
            
            _uiState.value = _uiState.value.copy(
                totalClients = totalClients,
                activeClients = activeClients,
                totalDownload = totalDownload,
                totalUpload = totalUpload,
                downloadHistory = downloadHistory,
                uploadHistory = uploadHistory,
                topClientsByTraffic = topClients,
                hourlyActivity = hourlyActivity,
                clientStats = clientStats
            )
        }
    }
    
    private fun generateTrafficHistory(totalTraffic: Long): List<FloatEntry> {
        val entries = mutableListOf<FloatEntry>()
        val baseTraffic = (totalTraffic / 7.0).toFloat()
        
        for (day in 0..6) {
            // Генерируем реалистичные данные с вариацией
            val variation = Random.nextFloat() * 0.4f + 0.8f // 80-120% от базового значения
            val dailyTraffic = baseTraffic * variation
            entries.add(FloatEntry(day.toFloat(), dailyTraffic))
        }
        
        return entries
    }
    
    private fun generateHourlyActivity(activeClients: Int): Map<Int, Int> {
        val activity = mutableMapOf<Int, Int>()
        
        for (hour in 0..23) {
            // Симулируем активность: пик в рабочие часы (9-18) и вечером (19-23)
            val baseActivity = when (hour) {
                in 0..6 -> 0.1 // Ночь - низкая активность
                in 7..8 -> 0.3 // Утро - средняя активность
                in 9..18 -> 0.8 // Рабочий день - высокая активность
                in 19..23 -> 0.6 // Вечер - средне-высокая активность
                else -> 0.2
            }
            
            val clients = (activeClients * baseActivity * (Random.nextFloat() * 0.4f + 0.8f)).toInt()
            activity[hour] = clients.coerceAtLeast(0)
        }
        
        return activity
    }
    
    private fun formatLastSeen(createdAt: String?): String? {
        return createdAt?.let {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(it)
                date?.let { outputFormat.format(it) }
            } catch (e: Exception) {
                "Неизвестно"
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 