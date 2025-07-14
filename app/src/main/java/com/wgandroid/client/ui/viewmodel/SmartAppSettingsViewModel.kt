package com.wgandroid.client.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.repository.SmartWireguardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmartAppConnectionStatus(
    val isSuccess: Boolean,
    val message: String
)

data class SmartAppSettingsUiState(
    val serverUrl: String = "",
    val serverPassword: String = "",
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val connectionStatus: SmartAppConnectionStatus? = null,
    val settingsSaved: Boolean = false,
    val initError: String? = null
)

class SmartAppSettingsViewModel : ViewModel() {
    private var sharedPreferences: SharedPreferences? = null
    private val repository = SmartWireguardRepository()
    
    private val _uiState = MutableStateFlow(SmartAppSettingsUiState())
    val uiState: StateFlow<SmartAppSettingsUiState> = _uiState.asStateFlow()
    
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
                initError = "Ошибка инициализации настроек: ${e.message}"
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
        }
    }
    
    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            serverUrl = url,
            urlError = null,
            connectionStatus = null,
            settingsSaved = false
        )
    }
    
    fun updateServerPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            serverPassword = password,
            connectionStatus = null,
            settingsSaved = false
        )
    }
    
    private fun validateUrl(url: String): String? {
        if (url.isBlank()) {
            return "URL не может быть пустым"
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "URL должен начинаться с http:// или https://"
        }
        
        return null
    }
    
    fun testConnection() {
        val url = _uiState.value.serverUrl.trim()
        val password = _uiState.value.serverPassword.trim().takeIf { it.isNotEmpty() }
        
        val urlError = validateUrl(url)
        if (urlError != null) {
            _uiState.value = _uiState.value.copy(urlError = urlError)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                connectionStatus = null,
                urlError = null
            )
            
            try {
                repository.configureServer(url, password)
                    .onSuccess { format ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = SmartAppConnectionStatus(
                                isSuccess = true,
                                message = "✅ Подключение успешно! Формат: $format"
                            )
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = SmartAppConnectionStatus(
                                isSuccess = false,
                                message = "❌ Ошибка подключения: ${error.message}"
                            )
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = SmartAppConnectionStatus(
                        isSuccess = false,
                        message = "❌ Критическая ошибка: ${e.message}"
                    )
                )
            }
        }
    }
    
    fun saveSettings() {
        val url = _uiState.value.serverUrl.trim()
        val password = _uiState.value.serverPassword.trim()
        
        val urlError = validateUrl(url)
        if (urlError != null) {
            _uiState.value = _uiState.value.copy(urlError = urlError)
            return
        }
        
        viewModelScope.launch {
            try {
                sharedPreferences?.edit()?.apply {
                    putString(KEY_SERVER_URL, url)
                    putString(KEY_SERVER_PASSWORD, password)
                    apply()
                }
                
                // Also configure the repository
                repository.configureServer(url, password.takeIf { it.isNotEmpty() })
                
                _uiState.value = _uiState.value.copy(
                    settingsSaved = true,
                    urlError = null
                )
                
                // Clear the saved flag after a delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(settingsSaved = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = SmartAppConnectionStatus(
                        isSuccess = false,
                        message = "❌ Ошибка сохранения настроек: ${e.message}"
                    )
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            urlError = null,
            connectionStatus = null
        )
    }
    
    fun handleQrCodeResult(qrContent: String) {
        try {
            // Пробуем распарсить QR код как JSON
            val jsonRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
            val passwordRegex = """"password"\s*:\s*"([^"]+)"""".toRegex()
            
            val urlMatch = jsonRegex.find(qrContent)
            val passwordMatch = passwordRegex.find(qrContent)
            
            if (urlMatch != null) {
                // JSON формат найден
                val url = urlMatch.groupValues[1]
                val password = passwordMatch?.groupValues?.get(1) ?: ""
                
                _uiState.value = _uiState.value.copy(
                    serverUrl = url,
                    serverPassword = password,
                    connectionStatus = SmartAppConnectionStatus(
                        isSuccess = true,
                        message = "✅ QR код успешно отсканирован!"
                    ),
                    urlError = null
                )
                return
            }
            
            // Пробуем как простой URL
            if (qrContent.startsWith("http://") || qrContent.startsWith("https://")) {
                _uiState.value = _uiState.value.copy(
                    serverUrl = qrContent,
                    connectionStatus = SmartAppConnectionStatus(
                        isSuccess = true,
                        message = "✅ URL сервера получен из QR кода!"
                    ),
                    urlError = null
                )
                return
            }
            
            // Пробуем как URL:password формат
            if (qrContent.contains(":") && 
                (qrContent.startsWith("http://") || qrContent.startsWith("https://"))) {
                val parts = qrContent.split(":", limit = 4) // http, //, host:port, password
                if (parts.size >= 4) {
                    val url = "${parts[0]}:${parts[1]}:${parts[2]}"
                    val password = parts[3]
                    
                    _uiState.value = _uiState.value.copy(
                        serverUrl = url,
                        serverPassword = password,
                        connectionStatus = SmartAppConnectionStatus(
                            isSuccess = true,
                            message = "✅ Настройки сервера получены из QR кода!"
                        ),
                        urlError = null
                    )
                    return
                }
            }
            
            // Если ничего не подошло
            _uiState.value = _uiState.value.copy(
                connectionStatus = SmartAppConnectionStatus(
                    isSuccess = false,
                    message = "❌ QR код не содержит настройки сервера. Содержимое: ${qrContent.take(50)}..."
                )
            )
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                connectionStatus = SmartAppConnectionStatus(
                    isSuccess = false,
                    message = "❌ Ошибка обработки QR кода: ${e.message}"
                )
            )
        }
    }
} 