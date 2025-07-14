package com.wgandroid.client.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.CachedSmartApiClient
import com.wgandroid.client.utils.DebugUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DiagnosticsUiState(
    val isConnected: Boolean = false,
    val serverUrl: String? = null,
    val responseTime: Long? = null,
    val lastCheck: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    
    // Network diagnostics
    val pingResult: String? = null,
    val connectivityResult: String? = null,
    val dnsResult: String? = null,
    
    // Server info
    val serverInfo: String? = null,
    val clientCount: Int? = null,
    val serverVersion: String? = null,
    
    // App statistics
    val totalRequests: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    
    // System info
    val systemInfo: Map<String, String> = emptyMap()
)

class DiagnosticsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()
    
    fun initialize(context: Context) {
        CachedSmartApiClient.initialize(context)
        loadSystemInfo()
        checkConnectionStatus()
        loadStatistics()
    }
    
    private fun loadSystemInfo() {
        val systemInfo = mutableMapOf<String, String>()
        systemInfo["Android версия"] = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        systemInfo["Устройство"] = "${Build.MANUFACTURER} ${Build.MODEL}"
        systemInfo["Архитектура"] = Build.SUPPORTED_ABIS.firstOrNull() ?: "Неизвестно"
        systemInfo["Приложение"] = "WireGuard Easy Client v1.0.0"
        systemInfo["Время запуска"] = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        _uiState.value = _uiState.value.copy(systemInfo = systemInfo)
    }
    
    private fun checkConnectionStatus() {
        viewModelScope.launch {
            val hasValidSession = CachedSmartApiClient.hasValidSession()
            val cachedUrl = CachedSmartApiClient.getCachedServerUrl()
            
            _uiState.value = _uiState.value.copy(
                isConnected = hasValidSession,
                serverUrl = cachedUrl,
                lastCheck = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            if (hasValidSession && cachedUrl != null) {
                measureResponseTime(cachedUrl)
                loadServerInfo()
            }
        }
    }
    
    private suspend fun measureResponseTime(serverUrl: String) {
        try {
            val startTime = System.currentTimeMillis()
            val apiService = CachedSmartApiClient.getApiService()
            val response = apiService.getClients()
            val endTime = System.currentTimeMillis()
            
            if (response.isSuccessful) {
                _uiState.value = _uiState.value.copy(
                    responseTime = endTime - startTime,
                    clientCount = response.body()?.size
                )
            }
        } catch (e: Exception) {
            DebugUtils.log("DiagnosticsViewModel", "Error measuring response time: ${e.message}")
        }
    }
    
    private suspend fun loadServerInfo() {
        try {
            val apiService = CachedSmartApiClient.getApiService()
            val sessionResponse = apiService.getSession()
            
            if (sessionResponse.isSuccessful) {
                val serverInfo = sessionResponse.body()
                _uiState.value = _uiState.value.copy(
                    serverVersion = serverInfo?.version,
                    serverInfo = "Последняя версия: ${serverInfo?.latestRelease?.version ?: "Неизвестно"}"
                )
            }
        } catch (e: Exception) {
            DebugUtils.log("DiagnosticsViewModel", "Error loading server info: ${e.message}")
        }
    }
    
    private fun loadStatistics() {
        // Здесь можно добавить загрузку статистики из SharedPreferences или базы данных
        // Пока используем моковые данные
        _uiState.value = _uiState.value.copy(
            totalRequests = 47,
            successfulRequests = 43,
            failedRequests = 4,
            cacheHits = 12,
            cacheMisses = 35
        )
    }
    
    fun pingServer() {
        val serverUrl = _uiState.value.serverUrl
        if (serverUrl.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(
                pingResult = "❌ Сервер не настроен"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val url = URL(serverUrl)
                val host = url.host
                val startTime = System.currentTimeMillis()
                
                val address = InetAddress.getByName(host)
                val isReachable = address.isReachable(5000)
                
                val endTime = System.currentTimeMillis()
                val pingTime = endTime - startTime
                
                val result = if (isReachable) {
                    "✅ Ping успешен: ${pingTime}ms"
                } else {
                    "❌ Сервер недоступен"
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pingResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pingResult = "❌ Ошибка ping: ${e.message}"
                )
            }
        }
    }
    
    fun testConnectivity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val apiService = CachedSmartApiClient.getApiService()
                val startTime = System.currentTimeMillis()
                val response = apiService.getClients()
                val endTime = System.currentTimeMillis()
                
                val result = if (response.isSuccessful) {
                    "✅ Соединение активно (${endTime - startTime}ms)"
                } else {
                    "❌ Ошибка HTTP ${response.code()}: ${response.message()}"
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectivityResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectivityResult = "❌ Ошибка соединения: ${e.message}"
                )
            }
        }
    }
    
    fun checkDNS() {
        val serverUrl = _uiState.value.serverUrl
        if (serverUrl.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(
                dnsResult = "❌ Сервер не настроен"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val url = URL(serverUrl)
                val host = url.host
                
                val startTime = System.currentTimeMillis()
                val address = InetAddress.getByName(host)
                val endTime = System.currentTimeMillis()
                
                val result = "✅ DNS разрешение: ${address.hostAddress} (${endTime - startTime}ms)"
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dnsResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dnsResult = "❌ Ошибка DNS: ${e.message}"
                )
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                CachedSmartApiClient.logout()
                delay(500) // Имитация времени очистки
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "✅ Кеш очищен",
                    isConnected = false,
                    serverUrl = null,
                    responseTime = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "❌ Ошибка очистки кеша: ${e.message}"
                )
            }
        }
    }
    
    fun exportLogs(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "wg_logs_$timestamp.txt"
                
                val logContent = buildString {
                    appendLine("WireGuard Easy Client - Логи")
                    appendLine("Время создания: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine("=".repeat(50))
                    appendLine()
                    
                    appendLine("Системная информация:")
                    _uiState.value.systemInfo.forEach { (key, value) ->
                        appendLine("$key: $value")
                    }
                    appendLine()
                    
                    appendLine("Состояние подключения:")
                    appendLine("Подключено: ${_uiState.value.isConnected}")
                    appendLine("URL сервера: ${_uiState.value.serverUrl ?: "Не задан"}")
                    appendLine("Время отклика: ${_uiState.value.responseTime ?: "N/A"}ms")
                    appendLine("Последняя проверка: ${_uiState.value.lastCheck ?: "N/A"}")
                    appendLine()
                    
                    appendLine("Диагностика:")
                    appendLine("Ping: ${_uiState.value.pingResult ?: "Не выполнен"}")
                    appendLine("Связность: ${_uiState.value.connectivityResult ?: "Не проверена"}")
                    appendLine("DNS: ${_uiState.value.dnsResult ?: "Не проверен"}")
                    appendLine()
                    
                    appendLine("Статистика:")
                    appendLine("Всего запросов: ${_uiState.value.totalRequests}")
                    appendLine("Успешных: ${_uiState.value.successfulRequests}")
                    appendLine("Неуспешных: ${_uiState.value.failedRequests}")
                    appendLine("Попадания в кеш: ${_uiState.value.cacheHits}")
                    appendLine("Промахи кеша: ${_uiState.value.cacheMisses}")
                }
                
                // Сохраняем в папку Downloads
                val downloadsDir = context.getExternalFilesDir(null)
                val logFile = File(downloadsDir, fileName)
                logFile.writeText(logContent)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "✅ Логи экспортированы: $fileName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "❌ Ошибка экспорта логов: ${e.message}"
                )
            }
        }
    }
    
    fun resetStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            delay(500) // Имитация времени сброса
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                totalRequests = 0,
                successfulRequests = 0,
                failedRequests = 0,
                cacheHits = 0,
                cacheMisses = 0,
                message = "✅ Статистика сброшена"
            )
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
} 