package com.wgandroid.client.utils

import android.util.Log
import com.wgandroid.client.BuildConfig

object DebugUtils {
    
    /**
     * Проверяет, включено ли логирование в текущей сборке
     */
    fun isLoggingEnabled(): Boolean {
        return BuildConfig.DEBUG_LOGGING
    }
    
    /**
     * Проверяет, должны ли отображаться debug экраны
     */
    fun shouldShowDebugScreens(): Boolean {
        return BuildConfig.SHOW_DEBUG_SCREENS
    }
    
    /**
     * Проверяет, разрешены ли тестовые активности
     */
    fun areTestActivitiesEnabled(): Boolean {
        return BuildConfig.ENABLE_TEST_ACTIVITIES
    }
    
    /**
     * Безопасное логирование - работает только в debug сборках
     */
    fun log(tag: String, message: String, logLevel: LogLevel = LogLevel.DEBUG) {
        if (!isLoggingEnabled()) {
            return
        }
        
        when (logLevel) {
            LogLevel.VERBOSE -> Log.v(tag, message)
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }
    
    /**
     * Безопасное логирование с throwable
     */
    fun log(tag: String, message: String, throwable: Throwable, logLevel: LogLevel = LogLevel.ERROR) {
        if (!isLoggingEnabled()) {
            return
        }
        
        when (logLevel) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
    
    /**
     * Выполняет код только в debug сборках
     */
    inline fun debugOnly(action: () -> Unit) {
        if (BuildConfig.DEBUG) {
            action()
        }
    }
    
    /**
     * Выполняет код только в release сборках
     */
    inline fun releaseOnly(action: () -> Unit) {
        if (!BuildConfig.DEBUG) {
            action()
        }
    }
    
    /**
     * Возвращает значение только в debug сборках, иначе возвращает null
     */
    inline fun <T> debugValue(value: () -> T): T? {
        return if (BuildConfig.DEBUG) {
            value()
        } else {
            null
        }
    }
    
    /**
     * Получить информацию о текущей сборке
     */
    fun getBuildInfo(): String {
        return buildString {
            append("Build Type: ${BuildConfig.BUILD_TYPE}\n")
            append("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Debug: ${BuildConfig.DEBUG}\n")
            append("Logging: ${isLoggingEnabled()}\n")
            append("Debug Screens: ${shouldShowDebugScreens()}\n")
            append("Test Activities: ${areTestActivitiesEnabled()}")
        }
    }
    
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
} 