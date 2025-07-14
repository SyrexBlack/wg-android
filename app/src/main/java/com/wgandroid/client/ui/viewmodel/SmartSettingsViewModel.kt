package com.wgandroid.client.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.SmartApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SmartSettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result
    
    private val _debugLog = MutableStateFlow("")
    val debugLog: StateFlow<String> = _debugLog
    
    private val _successfulFormat = MutableStateFlow<String?>(null)
    val successfulFormat: StateFlow<String?> = _successfulFormat
    
    private val _exportResult = MutableStateFlow("")
    val exportResult: StateFlow<String> = _exportResult
    
    private fun addToLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = _debugLog.value + "\n[$timestamp] $message"
        _debugLog.value = newLog
        Log.d("SmartSettings", message)
    }
    
    fun findFormatAndConnect(serverUrl: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _result.value = "Ищем рабочий формат аутентификации..."
                
                addToLog("🧠 === SMART API ПОИСК ===")
                addToLog("Сервер: $serverUrl")
                addToLog("Пароль: ${password.take(3)}***")
                
                // Устанавливаем callback для получения логов из SmartApiClient
                SmartApiClient.setLogCallback { message ->
                    addToLog(message)
                }
                
                // SmartApiClient автоматически найдет рабочий формат
                SmartApiClient.setServerConfig(serverUrl, password)
                
                // Получаем найденный формат
                val format = SmartApiClient.getSuccessfulFormat()
                _successfulFormat.value = format
                
                if (format != null) {
                    _result.value = "✅ Подключение установлено!"
                    addToLog("✅ SUCCESS! Рабочий формат: $format")
                    addToLog("🍪 Cookies сохранены для последующих запросов")
                } else {
                    _result.value = "❌ Не удалось найти рабочий формат"
                    addToLog("❌ FAILED: Ни один формат не сработал")
                }
                
            } catch (e: Exception) {
                _result.value = "❌ Ошибка: ${e.message}"
                addToLog("❌ ERROR: ${e.message}")
                addToLog("❌ STACK TRACE: ${e.stackTrace.joinToString("\n")}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun testApiCalls() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _result.value = "Тестируем API вызовы..."
                
                addToLog("🚀 === ТЕСТИРОВАНИЕ API ===")
                
                // Устанавливаем callback для получения логов из SmartApiClient
                SmartApiClient.setLogCallback { message ->
                    addToLog(message)
                }
                
                // Получаем API service
                val apiService = SmartApiClient.getApiService()
                addToLog("✅ API Service получен")
                
                // Тест 1: Получить список клиентов
                addToLog("📋 Запрашиваем список клиентов...")
                val clientsResponse = apiService.getClients()
                
                if (clientsResponse.isSuccessful) {
                    val clients = clientsResponse.body()
                    addToLog("✅ Клиенты получены: ${clients?.size ?: 0} шт.")
                    _result.value = "✅ API работает! Клиентов: ${clients?.size ?: 0}"
                } else {
                    addToLog("❌ Ошибка получения клиентов: HTTP ${clientsResponse.code()}")
                    _result.value = "❌ API ошибка: HTTP ${clientsResponse.code()}"
                }
                
            } catch (e: Exception) {
                _result.value = "❌ Ошибка API: ${e.message}"
                addToLog("❌ API ERROR: ${e.message}")
                addToLog("❌ API STACK TRACE: ${e.stackTrace.joinToString("\n")}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun exportLogToFile() {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val fileName = "wg_android_log_$timestamp.txt"
                
                val logContent = buildString {
                    append("=== WG ANDROID SMART API LOG ===\n")
                    append("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    append("Успешный формат: ${_successfulFormat.value ?: "Не найден"}\n")
                    append("Статус: ${_result.value}\n")
                    append("\n=== ПОДРОБНЫЙ ЛОГ ===\n")
                    append(_debugLog.value)
                    append("\n\n=== КОНЕЦ ЛОГА ===\n")
                }
                
                // Сохраняем в Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                
                FileWriter(file).use { writer ->
                    writer.write(logContent)
                }
                
                _exportResult.value = "✅ Лог сохранен: ${file.absolutePath}"
                addToLog("📁 Лог экспортирован в: ${file.absolutePath}")
                
            } catch (e: Exception) {
                _exportResult.value = "❌ Ошибка экспорта: ${e.message}"
                addToLog("❌ EXPORT ERROR: ${e.message}")
            }
        }
    }
    
    fun copyLogToClipboard() {
        try {
            val logContent = buildString {
                append("=== WG ANDROID SMART API LOG ===\n")
                append("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                append("Успешный формат: ${_successfulFormat.value ?: "Не найден"}\n")
                append("Статус: ${_result.value}\n")
                append("\n=== ПОДРОБНЫЙ ЛОГ ===\n")
                append(_debugLog.value)
                append("\n\n=== КОНЕЦ ЛОГА ===\n")
            }
            
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("WG Android Log", logContent)
            clipboard.setPrimaryClip(clip)
            
            _exportResult.value = "✅ Лог скопирован в буфер обмена"
            addToLog("📋 Лог скопирован в буфер обмена")
            
        } catch (e: Exception) {
            _exportResult.value = "❌ Ошибка копирования: ${e.message}"
            addToLog("❌ CLIPBOARD ERROR: ${e.message}")
        }
    }
    
    fun clearLog() {
        _debugLog.value = ""
        _exportResult.value = ""
        _result.value = ""
        _successfulFormat.value = null
    }
} 