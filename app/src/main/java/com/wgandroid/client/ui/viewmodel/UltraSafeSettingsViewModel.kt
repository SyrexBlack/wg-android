package com.wgandroid.client.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UltraSafeConnectionStatus(
    val isSuccess: Boolean,
    val message: String
)

data class UltraSafeSettingsUiState(
    val serverUrl: String = "",
    val serverPassword: String = "",
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val connectionStatus: UltraSafeConnectionStatus? = null,
    val settingsSaved: Boolean = false,
    val initError: String? = null,
    val debugLog: List<String> = emptyList()
)

class UltraSafeSettingsViewModel : ViewModel() {
    private var sharedPreferences: SharedPreferences? = null
    
    private val _uiState = MutableStateFlow(UltraSafeSettingsUiState())
    val uiState: StateFlow<UltraSafeSettingsUiState> = _uiState.asStateFlow()
    
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
        addDebugLog("🔄 Начинаем инициализацию...")
        
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
                
                addDebugLog("✅ Настройки загружены: URL=${url.take(20)}...")
            }
        } catch (e: Exception) {
            addDebugLog("❌ Ошибка загрузки: ${e.message}")
            _uiState.value = _uiState.value.copy(
                initError = "Ошибка загрузки настроек: ${e.message}"
            )
        }
    }
    
    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            serverUrl = url,
            urlError = null,
            connectionStatus = null
        )
        addDebugLog("📝 URL обновлен: ${url.take(20)}...")
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
        
        addDebugLog("🚀 Начинаем тест подключения...")
        
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
        
        // ULTRA-SAFE coroutine launch
        viewModelScope.launch {
            try {
                addDebugLog("⏳ Начинаем async операцию...")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    connectionStatus = null,
                    urlError = null
                )
                
                // Phase 1: Test basic HTTP connectivity (mock first)
                addDebugLog("🔗 Фаза 1: Тест базового подключения...")
                
                withContext(Dispatchers.IO) {
                    try {
                        // Simulate network delay
                        delay(1000)
                        addDebugLog("📡 Симуляция сетевого запроса...")
                        
                        // Phase 2: Simulate API response
                        delay(1000) 
                        addDebugLog("📊 Симуляция API ответа...")
                        
                        // Mock success for now - no real API calls yet
                        val mockClientsCount = (0..10).random()
                        addDebugLog("✅ Моковый ответ: $mockClientsCount клиентов")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = UltraSafeConnectionStatus(
                                isSuccess = true,
                                message = "🎯 Тест успешен (мок: $mockClientsCount клиентов)"
                            )
                        )
                        
                    } catch (e: Exception) {
                        addDebugLog("💥 Ошибка в IO: ${e.message}")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            connectionStatus = UltraSafeConnectionStatus(
                                isSuccess = false,
                                message = "❌ Ошибка IO: ${e.message ?: "Неизвестная"}"
                            )
                        )
                    }
                }
                
            } catch (e: Exception) {
                addDebugLog("💥 Общая ошибка: ${e.message}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = UltraSafeConnectionStatus(
                        isSuccess = false,
                        message = "❌ Критическая ошибка: ${e.message ?: "Неизвестная"}"
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
            
            addDebugLog("✅ Настройки сохранены")
            
            _uiState.value = _uiState.value.copy(
                settingsSaved = true,
                urlError = null,
                connectionStatus = UltraSafeConnectionStatus(
                    isSuccess = true,
                    message = "💾 Настройки успешно сохранены"
                )
            )
            
        } catch (e: Exception) {
            addDebugLog("❌ Ошибка сохранения: ${e.message}")
            
            _uiState.value = _uiState.value.copy(
                urlError = "Ошибка сохранения настроек: ${e.message}",
                connectionStatus = UltraSafeConnectionStatus(
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