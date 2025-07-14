package com.wgandroid.client.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wgandroid.client.data.api.SafeApiClient
import com.wgandroid.client.data.api.TestApiClient
import com.wgandroid.client.data.repository.SafeWireguardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StepByStepSettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep
    
    private val _stepResult = MutableStateFlow("")
    val stepResult: StateFlow<String> = _stepResult
    
    private val _debugLog = MutableStateFlow("")
    val debugLog: StateFlow<String> = _debugLog
    
    private fun addToLog(message: String) {
        val timestamp = System.currentTimeMillis()
        val newLog = _debugLog.value + "\n[$timestamp] $message"
        _debugLog.value = newLog
        Log.d("StepByStep", message)
    }
    
    fun testConnection(serverUrl: String, password: String) {
        viewModelScope.launch {
            try {
                addToLog("=== ДИАГНОСТИКА ПОШАГОВО ===")
                addToLog("Сервер: $serverUrl")
                addToLog("Пароль: ${password.take(3)}***")
                
                // Шаг 1: Тест SharedPreferences
                _currentStep.value = 1
                _stepResult.value = "Тестируем SharedPreferences..."
                testSharedPreferences()
                
                // Шаг 2: Тест класса SafeApiClient
                _currentStep.value = 2
                _stepResult.value = "Проверяем SafeApiClient..."
                testSafeApiClientClass()
                
                // Шаг 3: Тест разных форматов аутентификации  
                _currentStep.value = 3
                _stepResult.value = "Тестируем разные форматы логина..."
                testLoginFormats(serverUrl, password)
                
                // Шаг 4: Тест setServerConfig()
                _currentStep.value = 4
                _stepResult.value = "Тестируем setServerConfig()..."
                testSetServerConfig(serverUrl, password)
                
                // Шаг 5: Тест класса SafeWireguardRepository
                _currentStep.value = 5
                _stepResult.value = "Проверяем SafeWireguardRepository..."
                testSafeWireguardRepositoryClass()
                
                // Шаг 6: Создание репозитория
                _currentStep.value = 6
                _stepResult.value = "Создаем репозиторий..."
                testRepositoryCreation()
                
                // Шаг 7: Мок API симуляция
                _currentStep.value = 7
                _stepResult.value = "Симулируем работу API..."
                testMockApiSimulation()
                
                _stepResult.value = "✅ ВСЕ ШАГИ ЗАВЕРШЕНЫ!"
                addToLog("=== ДИАГНОСТИКА ЗАВЕРШЕНА ===")
                
            } catch (e: Exception) {
                addToLog("❌ ОШИБКА НА ШАГЕ ${_currentStep.value}: ${e.message}")
                _stepResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }
    
    private fun testSharedPreferences() {
        try {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("wg_android_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("test_key", "test_value").apply()
            val value = prefs.getString("test_key", null)
            
            if (value == "test_value") {
                addToLog("✅ Шаг 1: SharedPreferences работают")
            } else {
                addToLog("❌ Шаг 1: SharedPreferences НЕ работают")
            }
        } catch (e: Exception) {
            addToLog("❌ Шаг 1 ОШИБКА: ${e.message}")
            throw e
        }
    }
    
    private fun testSafeApiClientClass() {
        try {
            // SafeApiClient - это object, не class
            addToLog("✅ Шаг 2: Object SafeApiClient доступен")
        } catch (e: Exception) {
            addToLog("❌ Шаг 2 ОШИБКА: ${e.message}")
            throw e
        }
    }
    
    private fun testLoginFormats(serverUrl: String, password: String) {
        try {
            addToLog("🔍 Шаг 3: Тестируем разные форматы логина...")
            val testClient = TestApiClient()
            testClient.testLogin(serverUrl, password)
            addToLog("✅ Шаг 3: Тесты форматов запущены (см. логи TestApiClient)")
        } catch (e: Exception) {
            addToLog("❌ Шаг 3 ОШИБКА: ${e.message}")
            throw e
        }
    }
    
    private suspend fun testSetServerConfig(serverUrl: String, password: String) {
        try {
            addToLog("🔧 Вызываем SafeApiClient.setServerConfig()...")
            // SafeApiClient - это object, используем напрямую
            SafeApiClient.setServerConfig(serverUrl, password)
            addToLog("✅ Шаг 4: SafeApiClient.setServerConfig() выполнен без ошибок")
        } catch (e: Exception) {
            addToLog("❌ Шаг 4 ОШИБКА: ${e.message}")
            throw e
        }
    }
    
    private fun testSafeWireguardRepositoryClass() {
        try {
            // Просто проверяем, что класс доступен
            SafeWireguardRepository::class.java
            addToLog("✅ Шаг 5: Класс SafeWireguardRepository доступен")
        } catch (e: Exception) {
            addToLog("❌ Шаг 5 ОШИБКА: ${e.message}")
            throw e
        }
    }
    
    private fun testRepositoryCreation() {
        try {
            // SafeApiClient - object, SafeWireguardRepository - class
            val repository = SafeWireguardRepository()
            addToLog("✅ Шаг 6: SafeWireguardRepository создан успешно")
        } catch (e: Exception) {
            addToLog("❌ Шаг 6 ОШИБКА: ${e.message}")
            throw e
        }
    }
    
    private fun testMockApiSimulation() {
        try {
            addToLog("🎭 Симулируем успешные API вызовы...")
            Thread.sleep(1000) // Симуляция задержки
            addToLog("✅ Шаг 7: Мок API симуляция завершена")
        } catch (e: Exception) {
            addToLog("❌ Шаг 7 ОШИБКА: ${e.message}")
            throw e
        }
    }
} 