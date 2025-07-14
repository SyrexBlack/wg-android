package com.wgandroid.client.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.CachedSmartApiClient
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.data.repository.WireguardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class UnifiedUiState(
    // Авторизация
    val isAuthenticated: Boolean = false,
    val serverUrl: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val authError: String? = null,
    
    // Клиенты
    val clients: List<WireguardClient> = emptyList(),
    val isRefreshing: Boolean = false,
    val clientsError: String? = null,
    val serverStatus: String = "",
    
    // UI состояния
    val showCreateDialog: Boolean = false,
    val clientToDelete: WireguardClient? = null,
    val selectedQRClient: WireguardClient? = null,
    
    // Статистика
    val totalClients: Int = 0,
    val activeClients: Int = 0,
    val onlineClients: Int = 0,
    val totalTraffic: Long = 0,
    val currentTotalDownloadSpeed: Double = 0.0,
    val currentTotalUploadSpeed: Double = 0.0,
    
    // Real-time мониторинг
    val autoRefreshEnabled: Boolean = true,
    val lastUpdateTime: Long = 0L
)

class UnifiedViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UnifiedUiState())
    val uiState: StateFlow<UnifiedUiState> = _uiState.asStateFlow()
    
    private val cachedApiClient = CachedSmartApiClient
    private val repository: WireguardRepository
    
    init {
        // Инициализируем CachedSmartApiClient с контекстом
        cachedApiClient.initialize(application.applicationContext)
        
        // Создаем repository с правильным API клиентом
        repository = WireguardRepository()
        
        // Проверяем сохраненную авторизацию при запуске
        checkSavedAuthentication()
    }
    
    private fun checkSavedAuthentication() {
        viewModelScope.launch {
            val savedSession = cachedApiClient.getCachedSession()
            if (savedSession.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    serverUrl = savedSession
                )
                loadClients()
                startAutoRefresh()
            }
        }
    }
    
    // === АВТОРИЗАЦИЯ ===
    
    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            serverUrl = url,
            authError = null
        )
    }
    
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            authError = null
        )
    }
    
    fun authenticate() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.serverUrl.isEmpty()) {
                _uiState.value = currentState.copy(authError = "Введите URL сервера")
                return@launch
            }
            
            _uiState.value = currentState.copy(isLoading = true, authError = null)
            
            try {
                // Подключаемся к серверу
                val result = cachedApiClient.connectToServer(
                    serverUrl = currentState.serverUrl,
                    password = currentState.password
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        authError = null,
                        serverStatus = "✅ Подключено успешно"
                    )
                    loadClients()
                    startAutoRefresh()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authError = result.exceptionOrNull()?.message ?: "Ошибка подключения"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    authError = e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }
    
    fun onAuthenticationSuccess() {
        // Дополнительная логика после успешной авторизации
        loadClients()
    }
    
    fun logout() {
        viewModelScope.launch {
            cachedApiClient.clearCache()
            _uiState.value = UnifiedUiState() // Сброс к начальному состоянию
        }
    }
    
    // === УПРАВЛЕНИЕ КЛИЕНТАМИ ===
    
    private fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            try {
                // Используем CachedSmartApiClient для получения клиентов
                val apiService = cachedApiClient.getApiService()
                val response = apiService.getClients()
                
                if (response.isSuccessful) {
                    val clients = response.body() ?: emptyList()
                    updateClientsState(clients)
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        clientsError = null,
                        serverStatus = "✅ Обновлено (${clients.size} клиентов)"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        clientsError = "Ошибка загрузки: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    clientsError = e.message ?: "Ошибка загрузки клиентов"
                )
            }
        }
    }
    
    private fun updateClientsState(clients: List<WireguardClient>) {
        val activeCount = clients.count { it.enabled }
        val onlineCount = clients.count { isClientOnline(it) }
        val totalTraffic = clients.sumOf { it.transferRx + it.transferTx }
        val totalDownloadSpeed = clients.sumOf { it.transferRxCurrent }
        val totalUploadSpeed = clients.sumOf { it.transferTxCurrent }
        
        _uiState.value = _uiState.value.copy(
            clients = clients,
            totalClients = clients.size,
            activeClients = activeCount,
            onlineClients = onlineCount,
            totalTraffic = totalTraffic,
            currentTotalDownloadSpeed = totalDownloadSpeed,
            currentTotalUploadSpeed = totalUploadSpeed,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Определяет онлайн ли клиент на основе последнего handshake
     * Клиент считается онлайн если:
     * 1. Он включен (enabled = true)
     * 2. Последний handshake был менее 2 минут назад
     */
    fun isClientOnline(client: WireguardClient): Boolean {
        if (!client.enabled || client.latestHandshakeAt == null) {
            return false
        }
        
        return try {
            val handshakeTime = Instant.parse(client.latestHandshakeAt)
            val now = Instant.now()
            val diffMinutes = (now.epochSecond - handshakeTime.epochSecond) / 60
            diffMinutes < 2 // Онлайн если последний handshake был менее 2 минут назад
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Возвращает человеко-читаемое время с момента последнего подключения
     */
    fun getTimeSinceLastConnection(client: WireguardClient): String {
        if (!client.enabled || client.latestHandshakeAt == null) {
            return "Никогда"
        }
        
        return try {
            val handshakeTime = Instant.parse(client.latestHandshakeAt)
            val now = Instant.now()
            val diffMinutes = (now.epochSecond - handshakeTime.epochSecond) / 60
            
            when {
                diffMinutes < 1 -> "Только что"
                diffMinutes < 60 -> "${diffMinutes} мин назад"
                diffMinutes < 1440 -> "${diffMinutes / 60} ч назад"
                else -> "${diffMinutes / 1440} д назад"
            }
        } catch (e: Exception) {
            "Неизвестно"
        }
    }
    
    fun refreshClients() {
        loadClients()
    }
    
    fun createClient(name: String) {
        viewModelScope.launch {
            try {
                val apiService = cachedApiClient.getApiService()
                val request = com.wgandroid.client.data.model.CreateClientRequest(name)
                val response = apiService.createClient(request)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(showCreateDialog = false)
                    loadClients() // Обновляем список
                } else {
                    _uiState.value = _uiState.value.copy(
                        clientsError = "Ошибка создания клиента: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    clientsError = e.message ?: "Ошибка создания клиента"
                )
            }
        }
    }
    
    fun toggleClientEnabled(client: WireguardClient) {
        viewModelScope.launch {
            try {
                val apiService = cachedApiClient.getApiService()
                val response = if (client.enabled) {
                    apiService.disableClient(client.id)
                } else {
                    apiService.enableClient(client.id)
                }
                
                if (response.isSuccessful) {
                    // Обновляем список после изменения
                    delay(500) // Небольшая задержка для сервера
                    loadClients()
                } else {
                    _uiState.value = _uiState.value.copy(
                        clientsError = "Ошибка изменения статуса: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    clientsError = e.message ?: "Ошибка изменения статуса"
                )
            }
        }
    }
    
    fun deleteClient(client: WireguardClient) {
        viewModelScope.launch {
            try {
                val apiService = cachedApiClient.getApiService()
                val response = apiService.deleteClient(client.id)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(clientToDelete = null)
                    loadClients()
                } else {
                    _uiState.value = _uiState.value.copy(
                        clientsError = "Ошибка удаления клиента: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    clientsError = e.message ?: "Ошибка удаления клиента"
                )
            }
        }
    }
    
    // === UI УПРАВЛЕНИЕ ===
    
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }
    
    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }
    
    fun showDeleteDialog(client: WireguardClient) {
        _uiState.value = _uiState.value.copy(clientToDelete = client)
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(clientToDelete = null)
    }
    
    fun showQRCode(client: WireguardClient) {
        _uiState.value = _uiState.value.copy(selectedQRClient = client)
    }
    
    fun hideQRCode() {
        _uiState.value = _uiState.value.copy(selectedQRClient = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            authError = null,
            clientsError = null
        )
    }
    
    // === AUTO-REFRESH ===
    
    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (_uiState.value.isAuthenticated && _uiState.value.autoRefreshEnabled) {
                delay(3_000) // Real-time обновления каждые 3 секунды
                if (_uiState.value.isAuthenticated && !_uiState.value.isRefreshing) {
                    silentRefreshClients()
                }
            }
        }
    }
    
    private fun silentRefreshClients() {
        viewModelScope.launch {
            try {
                val apiService = cachedApiClient.getApiService()
                val response = apiService.getClients()
                
                if (response.isSuccessful) {
                    val clients = response.body() ?: emptyList()
                    updateClientsState(clients)
                } else {
                    // Тихая обработка ошибок автообновления
                    if (response.code() == 401) {
                        // Сессия истекла - требуется повторная авторизация
                        logout()
                    }
                }
            } catch (e: Exception) {
                // Тихо игнорируем ошибки автообновления
            }
        }
    }
    
    fun toggleAutoRefresh() {
        _uiState.value = _uiState.value.copy(
            autoRefreshEnabled = !_uiState.value.autoRefreshEnabled
        )
        
        if (_uiState.value.autoRefreshEnabled) {
            startAutoRefresh()
        }
    }
} 