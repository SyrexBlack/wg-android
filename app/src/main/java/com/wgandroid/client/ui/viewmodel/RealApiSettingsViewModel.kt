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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

data class RealApiConnectionStatus(
    val isSuccess: Boolean,
    val message: String
)

data class RealApiSettingsUiState(
    val serverUrl: String = "",
    val serverPassword: String = "",
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val connectionStatus: RealApiConnectionStatus? = null,
    val settingsSaved: Boolean = false,
    val initError: String? = null,
    val debugLog: List<String> = emptyList()
)

class RealApiSettingsViewModel : ViewModel() {
    private var sharedPreferences: SharedPreferences? = null
    private var repository: WireguardRepository? = null
    
    private val _uiState = MutableStateFlow(RealApiSettingsUiState())
    val uiState: StateFlow<RealApiSettingsUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "wg_android_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PASSWORD = "server_password"
    }
    
    private fun addDebugLog(message: String) {
        val currentLogs = _uiState.value.debugLog.takeLast(4) // Keep only last 5 entries
        _uiState.value = _uiState.value.copy(
            debugLog = currentLogs + message
        )
    }
    
    fun initializeWithContext(context: Context) {
        addDebugLog("🔄 Начинаем инициализацию с РЕАЛЬНЫМ API...")
        
        try {
            // Ultra-safe SharedPreferences initialization
            sharedPreferences = try {
                addDebugLog("📁 Создаем SharedPreferences...")
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            } catch (e: Exception) {
                addDebugLog("❌ Ошибка SharedPreferences: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    initError = "Ошибка доступа к настройкам: ${e.message}"
                )
                return
            }
            
            addDebugLog("✅ SharedPreferences создан")
            
            // Ultra-safe Repository initialization
            repository = try {
                addDebugLog("🏗️ Создаем WireguardRepository...")
                WireguardRepository()
            } catch (e: Exception) {
                addDebugLog("❌ Ошибка Repository: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    initError = "Ошибка инициализации репозитория: ${e.message}"
                )
                return
            }
            
            addDebugLog("✅ WireguardRepository создан")
            
            // Ultra-safe settings loading
            loadSettingsUltraSafely()
            
        } catch (e: Exception) {
            addDebugLog("💥 Критическая ошибка: ${e.message}")
            _uiState.value = _uiState.value.copy(
                initError = "Критическая ошибка инициализации: ${e.message}"
            )
        }
    }
    
    private fun loadSettingsUltraSafely() {
        try {
            addDebugLog("📖 Загружаем настройки...")
            
            sharedPreferences?.let { prefs ->
                val url = try {
                    prefs.getString(KEY_SERVER_URL, "") ?: ""
                } catch (e: Exception) {
                    addDebugLog("⚠️ Ошибка чтения URL: ${e.message}")
                    ""
                }
                
                val password = try {
                    prefs.getString(KEY_SERVER_PASSWORD, "") ?: ""
                } catch (e: Exception) {
                    addDebugLog("⚠️ Ошибка чтения пароля: ${e.message}")
                    ""
                }
                
                _uiState.value = _uiState.value.copy(
                    serverUrl = url,
                    serverPassword = password,
                    initError = null
                )
                
                addDebugLog("✅ Настройки загружены: URL=${url.take(30)}...")
                
                // Safe API client configuration if URL exists
                if (url.isNotBlank()) {
                    configureApiClientSafely(url, password)
                }
            }
        } catch (e: Exception) {
            addDebugLog("❌ Ошибка загрузки: ${e.message}")
            _uiState.value = _uiState.value.copy(
                initError = "Ошибка загрузки настроек: ${e.message}"
            )
        }
    }
    
    private fun configureApiClientSafely(url: String, password: String) {
        try {
            addDebugLog("⚙️ Конфигурируем ApiClient...")
            ApiClient.setServerConfig(url, password.ifBlank { null })
            addDebugLog("✅ ApiClient сконфигурирован")
        } catch (e: Exception) {
            addDebugLog("❌ Ошибка конфигурации API: ${e.message}")
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
        addDebugLog("📝 URL обновлен: ${url.take(30)}...")
    }
    
    fun updateServerPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            serverPassword = password,
            connectionStatus = null
        )
        addDebugLog("🔑 Пароль обновлен")
    }
    
    fun testConnection() {
        val url = _uiState.value.serverUrl.trim()
        
        addDebugLog("🚀 Начинаем РЕАЛЬНЫЙ тест подключения...")
        
        if (url.isBlank()) {
            addDebugLog("❌ URL пустой")
            _uiState.value = _uiState.value.copy(
                urlError = "URL не может быть пустым"
            )
            return
        }
        
        if (!isValidUrl(url)) {
            addDebugLog("❌ URL неверный формат")
            _uiState.value = _uiState.value.copy(
                urlError = "Неверный формат URL"
            )
            return
        }
        
        addDebugLog("✅ URL валидный: $url")
        
        // ULTRA-SAFE coroutine launch with REAL API calls
        viewModelScope.launch {
            try {
                addDebugLog("⏳ Начинаем async операцию...")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    connectionStatus = null,
                    urlError = null
                )
                
                // Phase 1: Configure API Client safely
                addDebugLog("🔧 Фаза 1: Конфигурация API...")
                
                try {
                    configureApiClientSafely(url, _uiState.value.serverPassword)
                    addDebugLog("✅ API клиент сконфигурирован")
                } catch (e: Exception) {
                    addDebugLog("💥 Ошибка конфигурации: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        connectionStatus = RealApiConnectionStatus(
                            isSuccess = false,
                            message = "❌ Ошибка конфигурации: ${e.message}"
                        )
                    )
                    return@launch
                }
                
                // Phase 2: Check repository availability
                val repo = repository
                if (repo == null) {
                    addDebugLog("❌ Repository не инициализирован")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        connectionStatus = RealApiConnectionStatus(
                            isSuccess = false,
                            message = "❌ Репозиторий не инициализирован"
                        )
                    )
                    return@launch
                }
                
                addDebugLog("✅ Repository готов к использованию")
                
                // Phase 3: REAL API call
                addDebugLog("🌐 Фаза 3: Выполняем РЕАЛЬНЫЙ API запрос...")
                
                withContext(Dispatchers.IO) {
                    try {
                        addDebugLog("📡 Отправляем GET /api/wireguard/client...")
                        
                        repo.getClients()
                            .onSuccess { clients ->
                                addDebugLog("✅ API ответ получен: ${clients.size} клиентов")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    connectionStatus = RealApiConnectionStatus(
                                        isSuccess = true,
                                        message = "🎯 Подключение успешно! Найдено ${clients.size} клиентов"
                                    )
                                )
                            }
                            .onFailure { error ->
                                addDebugLog("❌ API ошибка: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    connectionStatus = RealApiConnectionStatus(
                                        isSuccess = false,
                                        message = "❌ Ошибка подключения: ${error.message ?: "Неизвестная ошибка"}"
                                    )
                                )
                            }
                            
                    } catch (cancellation: CancellationException) {
                        addDebugLog("🚫 Операция отменена")
                        throw cancellation
                    } catch (e: Exception) {
                        addDebugLog("💥 Ошибка в IO: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = RealApiConnectionStatus(
                                isSuccess = false,
                                message = "❌ Ошибка API: ${e.message ?: "Неизвестная ошибка"}"
                            )
                        )
                    }
                }
                
            } catch (cancellation: CancellationException) {
                addDebugLog("🚫 Общая отмена операции")
                throw cancellation
            } catch (e: Exception) {
                addDebugLog("💥 Общая ошибка: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = RealApiConnectionStatus(
                        isSuccess = false,
                        message = "❌ Критическая ошибка: ${e.message ?: "Неизвестная ошибка"}"
                    )
                )
            }
        }
    }
    
    fun saveSettings() {
        val url = _uiState.value.serverUrl.trim()
        val password = _uiState.value.serverPassword
        
        addDebugLog("💾 Сохраняем настройки...")
        
        if (url.isBlank()) {
            addDebugLog("❌ URL пустой при сохранении")
            _uiState.value = _uiState.value.copy(
                urlError = "URL не может быть пустым"
            )
            return
        }
        
        if (!isValidUrl(url)) {
            addDebugLog("❌ URL неверный при сохранении")
            _uiState.value = _uiState.value.copy(
                urlError = "Неверный формат URL"
            )
            return
        }
        
        // Ultra-safe SharedPreferences save
        try {
            sharedPreferences?.edit()?.apply {
                putString(KEY_SERVER_URL, url)
                putString(KEY_SERVER_PASSWORD, password)
                apply()
            }
            
            addDebugLog("💾 Настройки сохранены в SharedPreferences")
            
            // Safe API client configuration
            configureApiClientSafely(url, password)
            
            _uiState.value = _uiState.value.copy(
                settingsSaved = true,
                urlError = null,
                connectionStatus = RealApiConnectionStatus(
                    isSuccess = true,
                    message = "💾 Настройки успешно сохранены"
                )
            )
            
            addDebugLog("✅ Сохранение завершено успешно")
            
        } catch (e: Exception) {
            addDebugLog("❌ Ошибка сохранения: ${e.message}")
            
            _uiState.value = _uiState.value.copy(
                urlError = "Ошибка сохранения настроек: ${e.message}",
                connectionStatus = RealApiConnectionStatus(
                    isSuccess = false,
                    message = "❌ Ошибка сохранения: ${e.message}"
                )
            )
        }
    }
    
    fun clearSavedState() {
        _uiState.value = _uiState.value.copy(
            settingsSaved = false,
            connectionStatus = null
        )
        addDebugLog("🧹 Состояние очищено")
    }
    
    fun clearInitError() {
        _uiState.value = _uiState.value.copy(initError = null)
        addDebugLog("🧹 Ошибка инициализации очищена")
    }
    
    fun clearDebugLog() {
        _uiState.value = _uiState.value.copy(debugLog = emptyList())
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            when {
                url.startsWith("http://") || url.startsWith("https://") -> {
                    // Basic validation
                    val hasValidChars = url.contains("://") && url.length > 10
                    hasValidChars
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
} 