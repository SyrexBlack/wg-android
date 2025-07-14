package com.wgandroid.client.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.data.repository.SmartWireguardRepository
import com.wgandroid.client.utils.FileDownloader
import com.wgandroid.client.utils.WGNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class SmartClientsUiState(
    val clients: List<WireguardClient> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isServerConfigured: Boolean = false,
    val serverStatus: String? = null,
    val autoRefreshEnabled: Boolean = true
)

data class QRCodeDialogState(
    val client: WireguardClient,
    val config: String
)

class SmartClientsViewModel : ViewModel() {
    private val repository = SmartWireguardRepository()
    
    private val _uiState = MutableStateFlow(SmartClientsUiState())
    val uiState: StateFlow<SmartClientsUiState> = _uiState.asStateFlow()
    
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()
    
    private val _showDeleteDialog = MutableStateFlow<WireguardClient?>(null)
    val showDeleteDialog: StateFlow<WireguardClient?> = _showDeleteDialog.asStateFlow()
    
    private val _showQRCodeDialog = MutableStateFlow<QRCodeDialogState?>(null)
    val showQRCodeDialog: StateFlow<QRCodeDialogState?> = _showQRCodeDialog.asStateFlow()
    
    init {
        checkServerStatus()
        startAutoRefresh()
    }
    
    private fun checkServerStatus() {
        viewModelScope.launch {
            try {
                // Пробуем загрузить клиентов, чтобы проверить подключение
                loadClients()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isServerConfigured = false,
                    serverStatus = "Сервер не настроен. Перейдите в настройки."
                )
            }
        }
    }
    
    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.getClients()
                .onSuccess { clients ->
                    _uiState.value = _uiState.value.copy(
                        clients = clients,
                        isLoading = false,
                        errorMessage = null,
                        isServerConfigured = true,
                        serverStatus = "✅ Подключено (${clients.size} клиентов)"
                    )
                }
                .onFailure { error ->
                    val isAuthError = error.message?.contains("401") == true || 
                                    error.message?.contains("Unauthorized") == true
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isServerConfigured = !isAuthError,
                        errorMessage = error.message ?: "Неизвестная ошибка",
                        serverStatus = if (isAuthError) {
                            "❌ Требуется настройка авторизации"
                        } else {
                            "❌ Ошибка подключения"
                        }
                    )
                }
        }
    }
    
    fun refreshClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            repository.getClients()
                .onSuccess { clients ->
                    _uiState.value = _uiState.value.copy(
                        clients = clients,
                        isRefreshing = false,
                        errorMessage = null,
                        serverStatus = "✅ Обновлено (${clients.size} клиентов)"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Ошибка обновления"
                    )
                }
        }
    }
    
    fun createClient(name: String, context: Context? = null) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Имя клиента не может быть пустым")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.createClient(name.trim())
                .onSuccess { newClient ->
                    val updatedClients = _uiState.value.clients + newClient
                    _uiState.value = _uiState.value.copy(
                        clients = updatedClients,
                        isLoading = false,
                        errorMessage = null,
                        serverStatus = "✅ Клиент создан (${updatedClients.size} клиентов)"
                    )
                    hideCreateDialog()
                    
                    // Показываем уведомление о создании
                    context?.let {
                        WGNotificationManager.showClientStatusNotification(
                            context = it,
                            clientName = name.trim(),
                            isEnabled = true,
                            showLongMessage = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Ошибка создания клиента"
                    )
                }
        }
    }
    
    fun deleteClient(client: WireguardClient) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.deleteClient(client.id)
                .onSuccess {
                    val updatedClients = _uiState.value.clients.filter { it.id != client.id }
                    _uiState.value = _uiState.value.copy(
                        clients = updatedClients,
                        isLoading = false,
                        errorMessage = null,
                        serverStatus = "✅ Клиент удален (${updatedClients.size} клиентов)"
                    )
                    hideDeleteDialog()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Ошибка удаления клиента"
                    )
                }
        }
    }
    
    fun toggleClientEnabled(client: WireguardClient, context: Context? = null) {
        viewModelScope.launch {
            val operation = if (client.enabled) {
                repository.disableClient(client.id)
            } else {
                repository.enableClient(client.id)
            }
            
            operation
                .onSuccess {
                    // Обновляем локальное состояние
                    val updatedClients = _uiState.value.clients.map { 
                        if (it.id == client.id) {
                            it.copy(enabled = !it.enabled)
                        } else {
                            it
                        }
                    }
                    val newStatus = !client.enabled
                    val status = if (client.enabled) "отключен" else "включен"
                    
                    _uiState.value = _uiState.value.copy(
                        clients = updatedClients,
                        serverStatus = "✅ Клиент $status"
                    )
                    
                    // TODO: Add notification when WGNotificationManager is implemented
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка изменения статуса"
                    )
                    
                    // TODO: Add error notification when WGNotificationManager is implemented
                }
        }
    }
    
    fun showCreateDialog() {
        _showCreateDialog.value = true
    }
    
    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }
    
    fun showDeleteDialog(client: WireguardClient) {
        _showDeleteDialog.value = client
    }
    
    fun hideDeleteDialog() {
        _showDeleteDialog.value = null
    }
    
    fun getClientConfig(client: WireguardClient, onConfigReceived: (String) -> Unit) {
        viewModelScope.launch {
            repository.getClientConfig(client.id)
                .onSuccess { config ->
                    onConfigReceived(config)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка получения конфигурации"
                    )
                }
        }
    }
    
    fun downloadClientConfig(context: Context, client: WireguardClient) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getClientConfig(client.id)
                .onSuccess { config ->
                    val fileName = "${client.name}.conf"
                    val success = FileDownloader.downloadConfigFile(context, fileName, config)
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            serverStatus = "📁 Файл $fileName сохранен"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Не удалось сохранить файл"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Ошибка получения конфигурации"
                    )
                }
        }
    }
    
    fun showQRCodeDialog(client: WireguardClient) {
        viewModelScope.launch {
            repository.getClientConfig(client.id)
                .onSuccess { config ->
                    _showQRCodeDialog.value = QRCodeDialogState(client, config)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка получения конфигурации для QR кода"
                    )
                }
        }
    }
    
    fun hideQRCodeDialog() {
        _showQRCodeDialog.value = null
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30_000) // 30 секунд
                if (_uiState.value.autoRefreshEnabled && 
                    _uiState.value.isServerConfigured && 
                    !_uiState.value.isLoading) {
                    silentRefreshClients()
                }
            }
        }
    }
    
    private fun silentRefreshClients() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return
        
        viewModelScope.launch {
            repository.getClients()
                .onSuccess { clients ->
                    // Проверяем изменения трафика и показываем уведомления
                    checkTrafficAlerts(clients)
                    
                    _uiState.value = _uiState.value.copy(
                        clients = clients,
                        serverStatus = "✅ Автообновлено (${clients.size} клиентов)"
                    )
                }
                .onFailure { error ->
                    // Тихо обрабатываем ошибки автообновления
                    if (error.message?.contains("401") == true) {
                        _uiState.value = _uiState.value.copy(
                            isServerConfigured = false,
                            serverStatus = "❌ Требуется повторная авторизация"
                        )
                    }
                }
        }
    }
    
    private fun checkTrafficAlerts(newClients: List<WireguardClient>) {
        val oldClients = _uiState.value.clients
        val highTrafficThreshold = 1024 * 1024 * 100 // 100 MB
        
        newClients.forEach { newClient ->
            val oldClient = oldClients.find { it.id == newClient.id }
            if (oldClient != null) {
                val downloadDiff = newClient.transferRx - oldClient.transferRx
                val uploadDiff = newClient.transferTx - oldClient.transferTx
                
                // TODO: Будем показывать уведомления когда добавим context
                // Пока сохраняем логику для будущего использования
                if (downloadDiff > highTrafficThreshold) {
                    // WGNotificationManager.showTrafficAlertNotification(context, newClient.name, downloadDiff, true)
                }
                if (uploadDiff > highTrafficThreshold) {
                    // WGNotificationManager.showTrafficAlertNotification(context, newClient.name, uploadDiff, false)
                }
            }
        }
    }
    
    fun toggleAutoRefresh() {
        _uiState.value = _uiState.value.copy(
            autoRefreshEnabled = !_uiState.value.autoRefreshEnabled
        )
    }
} 