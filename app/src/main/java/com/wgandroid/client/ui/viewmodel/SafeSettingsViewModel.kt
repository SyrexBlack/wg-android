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
import kotlinx.coroutines.CancellationException

data class SafeConnectionStatus(
    val isSuccess: Boolean,
    val message: String
)

data class SafeSettingsUiState(
    val serverUrl: String = "",
    val serverPassword: String = "",
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val connectionStatus: SafeConnectionStatus? = null,
    val settingsSaved: Boolean = false,
    val initError: String? = null
)

class SafeSettingsViewModel : ViewModel() {
    private var sharedPreferences: SharedPreferences? = null
    private var repository: WireguardRepository? = null
    
    private val _uiState = MutableStateFlow(SafeSettingsUiState())
    val uiState: StateFlow<SafeSettingsUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "wg_android_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PASSWORD = "server_password"
    }
    
    fun initializeWithContext(context: Context) {
        try {
            // Safe SharedPreferences initialization
            sharedPreferences = try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    initError = "Ошибка инициализации настроек: ${e.message}"
                )
                return
            }
            
            // Safe Repository initialization
            repository = try {
                WireguardRepository()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    initError = "Ошибка инициализации репозитория: ${e.message}"
                )
                return
            }
            
            // Safe settings loading
            loadSettingsSafely()
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                initError = "Критическая ошибка инициализации: ${e.message}"
            )
        }
    }
    
    private fun loadSettingsSafely() {
        try {
            sharedPreferences?.let { prefs ->
                val url = try {
                    prefs.getString(KEY_SERVER_URL, "") ?: ""
                } catch (e: Exception) {
                    ""
                }
                
                val password = try {
                    prefs.getString(KEY_SERVER_PASSWORD, "") ?: ""
                } catch (e: Exception) {
                    ""
                }
                
                _uiState.value = _uiState.value.copy(
                    serverUrl = url,
                    serverPassword = password,
                    initError = null
                )
                
                // Safe API client configuration
                if (url.isNotBlank()) {
                    configureApiClientSafely(url, password)
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                initError = "Ошибка загрузки настроек: ${e.message}"
            )
        }
    }
    
    private fun configureApiClientSafely(url: String, password: String) {
        try {
            ApiClient.setServerConfig(url, password.ifBlank { null })
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                urlError = "Ошибка конфигурации API: ${e.message}"
            )
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
        
        // Safe async operation with comprehensive error handling
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                connectionStatus = null,
                urlError = null
            )
            
            try {
                // Safe API client configuration
                configureApiClientSafely(url, _uiState.value.serverPassword)
                
                // Check if repository is available
                val repo = repository
                if (repo == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        connectionStatus = SafeConnectionStatus(
                            isSuccess = false,
                            message = "Ошибка: репозиторий не инициализирован"
                        )
                    )
                    return@launch
                }
                
                // Safe API call with timeout and error handling
                try {
                    repo.getClients()
                        .onSuccess { clients ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                connectionStatus = SafeConnectionStatus(
                                    isSuccess = true,
                                    message = "Подключение успешно установлено (найдено клиентов: ${clients.size})"
                                )
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                connectionStatus = SafeConnectionStatus(
                                    isSuccess = false,
                                    message = "Ошибка подключения: ${error.message ?: "Неизвестная ошибка"}"
                                )
                            )
                        }
                } catch (cancellation: CancellationException) {
                    // Don't handle cancellation as error
                    throw cancellation
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        connectionStatus = SafeConnectionStatus(
                            isSuccess = false,
                            message = "Ошибка API: ${e.message ?: "Неизвестная ошибка"}"
                        )
                    )
                }
                
            } catch (cancellation: CancellationException) {
                // Don't handle cancellation as error
                throw cancellation
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = SafeConnectionStatus(
                        isSuccess = false,
                        message = "Критическая ошибка: ${e.message ?: "Неизвестная ошибка"}"
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
        
        // Safe SharedPreferences save
        try {
            sharedPreferences?.edit()?.apply {
                putString(KEY_SERVER_URL, url)
                putString(KEY_SERVER_PASSWORD, password)
                apply()
            }
            
            // Safe API client configuration
            configureApiClientSafely(url, password)
            
            _uiState.value = _uiState.value.copy(
                settingsSaved = true,
                urlError = null,
                connectionStatus = SafeConnectionStatus(
                    isSuccess = true,
                    message = "Настройки успешно сохранены"
                )
            )
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                urlError = "Ошибка сохранения настроек: ${e.message}",
                connectionStatus = SafeConnectionStatus(
                    isSuccess = false,
                    message = "Ошибка сохранения: ${e.message}"
                )
            )
        }
    }
    
    fun clearSavedState() {
        _uiState.value = _uiState.value.copy(
            settingsSaved = false,
            connectionStatus = null
        )
    }
    
    fun clearInitError() {
        _uiState.value = _uiState.value.copy(initError = null)
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