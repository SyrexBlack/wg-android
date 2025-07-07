package com.wgandroid.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.model.WireguardClient
import com.wgandroid.client.data.repository.WireguardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientsUiState(
    val clients: List<WireguardClient> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

class ClientsViewModel : ViewModel() {
    private val repository = WireguardRepository()
    
    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()
    
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()
    
    private val _showDeleteDialog = MutableStateFlow<WireguardClient?>(null)
    val showDeleteDialog: StateFlow<WireguardClient?> = _showDeleteDialog.asStateFlow()
    
    init {
        loadClients()
    }
    
    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.getClients()
                .onSuccess { clients ->
                    _uiState.value = _uiState.value.copy(
                        clients = clients,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Неизвестная ошибка"
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
                        errorMessage = null
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
    
    fun createClient(name: String) {
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
                        errorMessage = null
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
                        errorMessage = null
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
    
    fun toggleClientEnabled(client: WireguardClient) {
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
                    _uiState.value = _uiState.value.copy(clients = updatedClients)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка изменения статуса"
                    )
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
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 