package com.wgandroid.client.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.ApiClient
import com.wgandroid.client.data.repository.WireguardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectionStatus(
    val isSuccess: Boolean,
    val message: String
)

data class SettingsUiState(
    val serverUrl: String = "",
    val serverPassword: String = "",
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val connectionStatus: ConnectionStatus? = null,
    val settingsSaved: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private var sharedPreferences: SharedPreferences? = null
    private val repository = WireguardRepository()
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "wg_android_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PASSWORD = "server_password"
    }
    
    fun initializeWithContext(context: Context) {
        try {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadSettings()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus(
                    isSuccess = false,
                    message = "Ошибка инициализации настроек: ${e.message}"
                )
            )
        }
    }
    
    private fun loadSettings() {
        sharedPreferences?.let { prefs ->
            val url = prefs.getString(KEY_SERVER_URL, "") ?: ""
            val password = prefs.getString(KEY_SERVER_PASSWORD, "") ?: ""
            
            _uiState.value = _uiState.value.copy(
                serverUrl = url,
                serverPassword = password
            )
            
            // Configure API client if URL is available
            if (url.isNotBlank()) {
                try {
                    ApiClient.setServerConfig(url, password.ifBlank { null })
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        urlError = "Неверный формат URL"
                    )
                }
            }
        }
    }
    
    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            serverUrl = url,
            urlError = null,
            connectionStatus = null
        )
    }
    
    fun updateServerPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            serverPassword = password,
            connectionStatus = null
        )
    }
    
    fun testConnection() {
        val url = _uiState.value.serverUrl.trim()
        
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                urlError = "URL не может быть пустым"
            )
            return
        }
        
        if (!isValidUrl(url)) {
            _uiState.value = _uiState.value.copy(
                urlError = "Неверный формат URL"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                connectionStatus = null,
                urlError = null
            )
            
            try {
                // Configure API client with new settings
                ApiClient.setServerConfig(url, _uiState.value.serverPassword.ifBlank { null })
                
                // Test connection by trying to get clients
                repository.getClients()
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = ConnectionStatus(
                                isSuccess = true,
                                message = "Подключение успешно установлено"
                            )
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = ConnectionStatus(
                                isSuccess = false,
                                message = "Ошибка подключения: ${error.message}"
                            )
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = ConnectionStatus(
                        isSuccess = false,
                        message = "Ошибка конфигурации: ${e.message}"
                    )
                )
            }
        }
    }
    
    fun saveSettings() {
        val url = _uiState.value.serverUrl.trim()
        val password = _uiState.value.serverPassword
        
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                urlError = "URL не может быть пустым"
            )
            return
        }
        
        if (!isValidUrl(url)) {
            _uiState.value = _uiState.value.copy(
                urlError = "Неверный формат URL"
            )
            return
        }
        
        sharedPreferences?.edit()?.apply {
            putString(KEY_SERVER_URL, url)
            putString(KEY_SERVER_PASSWORD, password)
            apply()
        }
        
        // Configure API client
        try {
            ApiClient.setServerConfig(url, password.ifBlank { null })
            _uiState.value = _uiState.value.copy(
                settingsSaved = true,
                urlError = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                urlError = "Ошибка сохранения настроек"
            )
        }
    }
    
    fun clearSavedState() {
        _uiState.value = _uiState.value.copy(settingsSaved = false)
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            val pattern = Regex("^https?://[^\\s/$.?#].[^\\s]*$", RegexOption.IGNORE_CASE)
            pattern.matches(url) || url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
} 