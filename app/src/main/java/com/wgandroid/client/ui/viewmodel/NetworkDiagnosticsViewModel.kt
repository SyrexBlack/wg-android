package com.wgandroid.client.ui.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileWriter
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class NetworkDiagnosticsUiState(
    val serverUrl: String = "",
    val password: String = "",
    val isRunning: Boolean = false,
    val logEntries: List<String> = emptyList(),
    val exportResult: String = ""
)

class NetworkDiagnosticsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(NetworkDiagnosticsUiState())
    val uiState: StateFlow<NetworkDiagnosticsUiState> = _uiState.asStateFlow()
    
    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"
        
        _uiState.value = _uiState.value.copy(
            logEntries = _uiState.value.logEntries + logEntry
        )
        
        Log.d("NetworkDiagnostics", message)
    }
    
    fun setServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }
    
    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }
    
    fun clearLog() {
        _uiState.value = _uiState.value.copy(
            logEntries = emptyList(),
            exportResult = ""
        )
    }
    
    fun runBasicTests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true)
            
            try {
                addLog("🔧 === БАЗОВАЯ ДИАГНОСТИКА ===")
                
                val url = _uiState.value.serverUrl.trim()
                if (url.isEmpty()) {
                    addLog("❌ URL сервера не указан")
                    return@launch
                }
                
                addLog("🌐 Тестируем сервер: $url")
                
                // Test 1: URL validation
                testUrlValidation(url)
                
                // Test 2: DNS resolution
                testDnsResolution(url)
                
                // Test 3: Basic connectivity
                testBasicConnectivity(url)
                
                // Test 4: HTTP response
                testHttpResponse(url)
                
                addLog("✅ Базовая диагностика завершена")
                
            } catch (e: Exception) {
                addLog("❌ Ошибка диагностики: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        }
    }
    
    fun runAdvancedTests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true)
            
            try {
                addLog("🔬 === РАСШИРЕННАЯ ДИАГНОСТИКА ===")
                
                val url = _uiState.value.serverUrl.trim()
                val password = _uiState.value.password.trim()
                
                // Сначала базовые тесты
                runBasicTestsInline(url)
                
                // Advanced tests
                testHttpHeaders(url)
                testDifferentHttpMethods(url)
                testWireguardEndpoints(url)
                
                if (password.isNotEmpty()) {
                    testAuthenticationFormats(url, password)
                }
                
                addLog("✅ Расширенная диагностика завершена")
                
            } catch (e: Exception) {
                addLog("❌ Ошибка диагностики: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        }
    }
    
    private suspend fun runBasicTestsInline(url: String) {
        testUrlValidation(url)
        testDnsResolution(url)
        testBasicConnectivity(url)
        testHttpResponse(url)
    }
    
    private suspend fun testUrlValidation(url: String) {
        addLog("🔍 Проверка валидности URL...")
        
        try {
            val parsedUrl = URL(url)
            addLog("✅ URL валидный")
            addLog("   Протокол: ${parsedUrl.protocol}")
            addLog("   Хост: ${parsedUrl.host}")
            addLog("   Порт: ${if (parsedUrl.port != -1) parsedUrl.port else "default"}")
            addLog("   Путь: ${parsedUrl.path.ifEmpty { "/" }}")
        } catch (e: Exception) {
            addLog("❌ Неверный формат URL: ${e.message}")
            throw e
        }
    }
    
    private suspend fun testDnsResolution(url: String) = withContext(Dispatchers.IO) {
        addLog("🌐 Разрешение DNS...")
        
        try {
            val parsedUrl = URL(url)
            val address = InetAddress.getByName(parsedUrl.host)
            addLog("✅ DNS разрешен: ${address.hostAddress}")
        } catch (e: Exception) {
            addLog("❌ Ошибка DNS: ${e.message}")
        }
    }
    
    private suspend fun testBasicConnectivity(url: String) = withContext(Dispatchers.IO) {
        addLog("🔌 Проверка базового подключения...")
        
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            val response = client.newCall(request).execute()
            addLog("✅ Подключение установлено")
            addLog("   Код ответа: ${response.code}")
            addLog("   Сообщение: ${response.message}")
            response.close()
        } catch (e: Exception) {
            addLog("❌ Ошибка подключения: ${e.message}")
            
            when {
                e.message?.contains("timeout") == true -> {
                    addLog("⚠️ Возможные причины таймаута:")
                    addLog("   - Сервер не запущен")
                    addLog("   - Неправильный порт")
                    addLog("   - Блокировка файрволом")
                }
                e.message?.contains("ConnectException") == true -> {
                    addLog("⚠️ Подключение отклонено:")
                    addLog("   - Сервер недоступен")
                    addLog("   - Неправильный адрес")
                }
            }
        }
    }
    
    private suspend fun testHttpResponse(url: String) = withContext(Dispatchers.IO) {
        addLog("📡 Тестирование HTTP ответа...")
        
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()?.take(200)
            
            addLog("✅ HTTP ответ получен")
            addLog("   Код: ${response.code}")
            addLog("   Размер: ${response.body?.contentLength() ?: "unknown"} байт")
            addLog("   Content-Type: ${response.header("Content-Type") ?: "не указан"}")
            addLog("   Server: ${response.header("Server") ?: "не указан"}")
            if (!body.isNullOrEmpty()) {
                addLog("   Содержимое (200 симв.): $body...")
            }
            
            response.close()
        } catch (e: Exception) {
            addLog("❌ Ошибка HTTP: ${e.message}")
        }
    }
    
    private suspend fun testHttpHeaders(url: String) = withContext(Dispatchers.IO) {
        addLog("🔍 Анализ HTTP заголовков...")
        
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            val response = client.newCall(request).execute()
            
            addLog("📋 Заголовки ответа:")
            response.headers.forEach { (name, value) ->
                addLog("   $name: $value")
            }
            
            response.close()
        } catch (e: Exception) {
            addLog("❌ Ошибка получения заголовков: ${e.message}")
        }
    }
    
    private suspend fun testDifferentHttpMethods(url: String) = withContext(Dispatchers.IO) {
        addLog("🚀 Тестирование HTTP методов...")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        
        val methods = listOf("GET", "POST", "HEAD", "OPTIONS")
        
        for (method in methods) {
            try {
                val requestBuilder = Request.Builder().url(url)
                
                val request = when (method) {
                    "GET" -> requestBuilder.get().build()
                    "POST" -> requestBuilder.post("".toRequestBody()).build()
                    "HEAD" -> requestBuilder.head().build()
                    "OPTIONS" -> requestBuilder.method("OPTIONS", null).build()
                    else -> requestBuilder.get().build()
                }
                
                val response = client.newCall(request).execute()
                addLog("✅ $method: ${response.code} ${response.message}")
                response.close()
            } catch (e: Exception) {
                addLog("❌ $method: ${e.message}")
            }
        }
    }
    
    private suspend fun testWireguardEndpoints(url: String) = withContext(Dispatchers.IO) {
        addLog("🔧 Тестирование WireGuard эндпоинтов...")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        
        val endpoints = listOf(
            "api/wireguard/client",
            "api/clients",
            "api/session",
            "api/auth",
            "api/login"
        )
        
        val baseUrl = url.removeSuffix("/")
        
        for (endpoint in endpoints) {
            try {
                val testUrl = "$baseUrl/$endpoint"
                val request = Request.Builder()
                    .url(testUrl)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                addLog("✅ /$endpoint: ${response.code} ${response.message}")
                
                if (response.code == 401) {
                    addLog("   🔐 Требует авторизацию")
                }
                
                response.close()
            } catch (e: Exception) {
                addLog("❌ /$endpoint: ${e.message}")
            }
        }
    }
    
    private suspend fun testAuthenticationFormats(url: String, password: String) = withContext(Dispatchers.IO) {
        addLog("🔐 Тестирование форматов авторизации...")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        
        val baseUrl = url.removeSuffix("/")
        val authUrl = "$baseUrl/api/session"
        
        // Test JSON password format
        try {
            val json = """{"password":"$password"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(authUrl)
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            addLog("🔍 JSON password: ${response.code} ${response.message}")
            response.close()
        } catch (e: Exception) {
            addLog("❌ JSON password: ${e.message}")
        }
        
        // Test form data format
        try {
            val body = FormBody.Builder()
                .add("password", password)
                .build()
            val request = Request.Builder()
                .url(authUrl)
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            addLog("🔍 Form password: ${response.code} ${response.message}")
            response.close()
        } catch (e: Exception) {
            addLog("❌ Form password: ${e.message}")
        }
    }
    
    fun exportLog() {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val fileName = "network_diagnostics_$timestamp.txt"
                
                val logContent = buildString {
                    append("=== ДИАГНОСТИКА СЕТИ WG ANDROID ===\n")
                    append("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    append("Сервер: ${_uiState.value.serverUrl}\n")
                    append("Пароль: ${if (_uiState.value.password.isNotEmpty()) "Установлен" else "Не установлен"}\n")
                    append("\n=== РЕЗУЛЬТАТЫ ===\n")
                    _uiState.value.logEntries.forEach { entry ->
                        append("$entry\n")
                    }
                    append("\n=== КОНЕЦ ЛОГА ===\n")
                }
                
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                
                FileWriter(file).use { writer ->
                    writer.write(logContent)
                }
                
                _uiState.value = _uiState.value.copy(
                    exportResult = "✅ Лог сохранен: ${file.absolutePath}"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportResult = "❌ Ошибка экспорта: ${e.message}"
                )
            }
        }
    }
} 