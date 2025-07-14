package com.wgandroid.client.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.CachedSmartApiClient
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.data.repository.CachedWireguardRepository
import com.wgandroid.client.utils.FileDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OptimizedClientsUiState(
    val clients: List<WireguardClient> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isServerConfigured: Boolean = false,
    val errorMessage: String? = null,
    val serverStatus: String? = null
)

class OptimizedClientsViewModel : ViewModel() {
    private val repository = CachedWireguardRepository()
    
    private val _uiState = MutableStateFlow(OptimizedClientsUiState())
    val uiState: StateFlow<OptimizedClientsUiState> = _uiState.asStateFlow()
    
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()
    
    private val _showDeleteDialog = MutableStateFlow<WireguardClient?>(null)
    val showDeleteDialog: StateFlow<WireguardClient?> = _showDeleteDialog.asStateFlow()
    
    fun initialize(context: Context) {
        CachedSmartApiClient.initialize(context)
        checkServerConfiguration()
        if (_uiState.value.isServerConfigured) {
            loadClients()
        }
    }
    
    private fun checkServerConfiguration() {
        val hasValidSession = CachedSmartApiClient.hasValidSession()
        val cachedUrl = CachedSmartApiClient.getCachedServerUrl()
        
        _uiState.value = _uiState.value.copy(
            isServerConfigured = hasValidSession && !cachedUrl.isNullOrEmpty()
        )
    }
    
    private fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.getClients()
                .onSuccess { clients ->
                    _uiState.value = _uiState.value.copy(
                        clients = clients,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        serverStatus = "✅ Загружено клиентов: ${clients.size}"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Ошибка загрузки клиентов"
                    )
                }
        }
    }
    
    fun refreshClients() {
        if (!_uiState.value.isServerConfigured) {
            checkServerConfiguration()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadClients()
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
            val repository = CachedWireguardRepository()
            
            if (client.enabled) {
                // Отключаем клиента
                repository.disableClient(client.id)
                    .onSuccess {
                        val updatedClients = _uiState.value.clients.map { 
                            if (it.id == client.id) it.copy(enabled = false) else it 
                        }
                        _uiState.value = _uiState.value.copy(
                            clients = updatedClients,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message ?: "Ошибка отключения клиента"
                        )
                    }
            } else {
                // Включаем клиента
                repository.enableClient(client.id)
                    .onSuccess {
                        val updatedClients = _uiState.value.clients.map { 
                            if (it.id == client.id) it.copy(enabled = true) else it 
                        }
                        _uiState.value = _uiState.value.copy(
                            clients = updatedClients,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message ?: "Ошибка включения клиента"
                        )
                    }
            }
        }
    }
    
    fun downloadClientConfig(context: Context, client: WireguardClient) {
        viewModelScope.launch {
            repository.getClientConfig(client.id)
                .onSuccess { config ->
                    try {
                        val success = FileDownloader.downloadConfigFile(
                            context = context,
                            fileName = "${client.name}.conf",
                            content = config
                        )
                        
                        if (success) {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = null,
                                serverStatus = "✅ Конфигурация ${client.name} сохранена"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Не удалось сохранить файл конфигурации"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Ошибка сохранения файла: ${e.message}"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка получения конфигурации"
                    )
                }
        }
    }
    
    fun copyClientConfig(context: Context, client: WireguardClient) {
        viewModelScope.launch {
            repository.getClientConfig(client.id)
                .onSuccess { config ->
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("WireGuard Config", config)
                    clipboardManager.setPrimaryClip(clip)
                    
                    _uiState.value = _uiState.value.copy(
                        errorMessage = null,
                        serverStatus = "✅ Конфигурация ${client.name} скопирована в буфер обмена"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка получения конфигурации"
                    )
                }
        }
    }
    
    fun showQRCodeForClient(client: WireguardClient) {
        // TODO: Implement QR Code display
        _uiState.value = _uiState.value.copy(
            serverStatus = "QR код для ${client.name} (в разработке)"
        )
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
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 