package com.wgandroid.client.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.CachedSmartApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppSettingsUiState(
    val serverUrl: String = "",
    val serverPassword: String = "",
    val isLoading: Boolean = false,
    val connectionStatus: String? = null,
    val urlError: String? = null,
    val hasValidSession: Boolean = false
)

class AppSettingsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()
    
    fun initialize(context: Context) {
        CachedSmartApiClient.initialize(context)
        
        // Загружаем сохраненные настройки
        viewModelScope.launch {
            val savedUrl = CachedSmartApiClient.getCachedServerUrl()
            val hasSession = CachedSmartApiClient.hasValidSession()
            
            _uiState.value = _uiState.value.copy(
                serverUrl = savedUrl ?: "",
                hasValidSession = hasSession,
                connectionStatus = if (hasSession) "✅ Подключение активно" else null
            )
        }
    }
    
    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            serverUrl = url,
            urlError = if (url.isBlank()) null else validateUrl(url)
        )
    }
    
    fun updateServerPassword(password: String) {
        _uiState.value = _uiState.value.copy(serverPassword = password)
    }
    
    private fun validateUrl(url: String): String? {
        return when {
            !url.startsWith("http://") && !url.startsWith("https://") -> 
                "URL должен начинаться с http:// или https://"
            url.contains(" ") -> 
                "URL не должен содержать пробелы"
            else -> null
        }
    }
    
    fun testConnection() {
        val currentState = _uiState.value
        
        if (currentState.urlError != null) {
            return
        }
        
        if (currentState.serverUrl.isBlank()) {
            _uiState.value = currentState.copy(urlError = "Введите URL сервера")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                connectionStatus = null
            )
            
            try {
                CachedSmartApiClient.setServerConfig(
                    url = currentState.serverUrl,
                    pwd = currentState.serverPassword.takeIf { it.isNotBlank() }
                )
                
                // Пробуем получить список клиентов для проверки подключения
                val apiService = CachedSmartApiClient.getApiService()
                val response = apiService.getClients()
                
                if (response.isSuccessful) {
                    val successFormat = CachedSmartApiClient.getSuccessfulFormat()
                    val clientCount = response.body()?.size ?: 0
                    
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        hasValidSession = true,
                        connectionStatus = "✅ Подключение успешно! Найдено клиентов: $clientCount" +
                                (if (successFormat != null) " (Формат: $successFormat)" else "")
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        hasValidSession = false,
                        connectionStatus = "❌ Ошибка подключения: HTTP ${response.code()}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    hasValidSession = false,
                    connectionStatus = "❌ Ошибка: ${e.message}"
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            CachedSmartApiClient.logout()
            
            _uiState.value = _uiState.value.copy(
                hasValidSession = false,
                connectionStatus = "Сессия завершена",
                serverPassword = "" // Очищаем пароль для безопасности
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(urlError = null)
    }
} 