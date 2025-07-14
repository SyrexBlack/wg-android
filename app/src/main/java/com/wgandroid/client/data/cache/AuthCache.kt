package com.wgandroid.client.data.cache

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import java.util.*

class AuthCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "wg_android_auth_cache"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PASSWORD = "server_password"
        private const val KEY_LOGIN_FORMAT = "login_format"
        private const val KEY_NO_AUTH_REQUIRED = "no_auth_required"
        private const val KEY_COOKIES_PREFIX = "cookie_"
        private const val KEY_COOKIE_COUNT = "cookie_count"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        private const val KEY_SESSION_VALID = "session_valid"
        
        // Время жизни сессии - 24 часа
        private const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L
    }
    
    data class AuthData(
        val serverUrl: String,
        val serverPassword: String,
        val loginFormat: String?,
        val noAuthRequired: Boolean,
        val cookies: List<Cookie>,
        val isSessionValid: Boolean
    )
    
    suspend fun saveAuthData(
        serverUrl: String,
        serverPassword: String?,
        loginFormat: String?,
        noAuthRequired: Boolean,
        cookies: List<Cookie>
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_SERVER_PASSWORD, serverPassword ?: "")
            putString(KEY_LOGIN_FORMAT, loginFormat)
            putBoolean(KEY_NO_AUTH_REQUIRED, noAuthRequired)
            putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            putBoolean(KEY_SESSION_VALID, true)
            
            // Сохраняем cookies
            putInt(KEY_COOKIE_COUNT, cookies.size)
            cookies.forEachIndexed { index, cookie ->
                putString("${KEY_COOKIES_PREFIX}${index}_name", cookie.name)
                putString("${KEY_COOKIES_PREFIX}${index}_value", cookie.value)
                putString("${KEY_COOKIES_PREFIX}${index}_domain", cookie.domain)
                putString("${KEY_COOKIES_PREFIX}${index}_path", cookie.path)
                putLong("${KEY_COOKIES_PREFIX}${index}_expires", cookie.expiresAt)
                putBoolean("${KEY_COOKIES_PREFIX}${index}_secure", cookie.secure)
                putBoolean("${KEY_COOKIES_PREFIX}${index}_httponly", cookie.httpOnly)
            }
        }.apply()
    }
    
    suspend fun getAuthData(): AuthData? = withContext(Dispatchers.IO) {
        val serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: ""
        if (serverUrl.isEmpty()) return@withContext null
        
        val lastLoginTime = prefs.getLong(KEY_LAST_LOGIN_TIME, 0)
        val isSessionExpired = System.currentTimeMillis() - lastLoginTime > SESSION_TIMEOUT_MS
        
        if (isSessionExpired) {
            // Сессия истекла, очищаем данные
            invalidateSession()
            return@withContext null
        }
        
        val serverPassword = prefs.getString(KEY_SERVER_PASSWORD, "") ?: ""
        val loginFormat = prefs.getString(KEY_LOGIN_FORMAT, null)
        val noAuthRequired = prefs.getBoolean(KEY_NO_AUTH_REQUIRED, false)
        val isSessionValid = prefs.getBoolean(KEY_SESSION_VALID, false)
        
        // Загружаем cookies
        val cookieCount = prefs.getInt(KEY_COOKIE_COUNT, 0)
        val cookies = mutableListOf<Cookie>()
        
        for (i in 0 until cookieCount) {
            try {
                val name = prefs.getString("${KEY_COOKIES_PREFIX}${i}_name", null) ?: continue
                val value = prefs.getString("${KEY_COOKIES_PREFIX}${i}_value", null) ?: continue
                val domain = prefs.getString("${KEY_COOKIES_PREFIX}${i}_domain", null) ?: continue
                val path = prefs.getString("${KEY_COOKIES_PREFIX}${i}_path", null) ?: continue
                val expires = prefs.getLong("${KEY_COOKIES_PREFIX}${i}_expires", 0)
                val secure = prefs.getBoolean("${KEY_COOKIES_PREFIX}${i}_secure", false)
                val httpOnly = prefs.getBoolean("${KEY_COOKIES_PREFIX}${i}_httponly", false)
                
                val cookie = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(domain)
                    .path(path)
                    .expiresAt(expires)
                    .apply { 
                        if (secure) secure()
                        if (httpOnly) httpOnly()
                    }
                    .build()
                
                cookies.add(cookie)
            } catch (e: Exception) {
                // Игнорируем поврежденные cookies
            }
        }
        
        return@withContext AuthData(
            serverUrl = serverUrl,
            serverPassword = serverPassword,
            loginFormat = loginFormat,
            noAuthRequired = noAuthRequired,
            cookies = cookies,
            isSessionValid = isSessionValid && !isSessionExpired
        )
    }
    
    suspend fun invalidateSession() = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putBoolean(KEY_SESSION_VALID, false)
            putLong(KEY_LAST_LOGIN_TIME, 0)
            
            // Очищаем cookies
            val cookieCount = prefs.getInt(KEY_COOKIE_COUNT, 0)
            for (i in 0 until cookieCount) {
                remove("${KEY_COOKIES_PREFIX}${i}_name")
                remove("${KEY_COOKIES_PREFIX}${i}_value")
                remove("${KEY_COOKIES_PREFIX}${i}_domain")
                remove("${KEY_COOKIES_PREFIX}${i}_path")
                remove("${KEY_COOKIES_PREFIX}${i}_expires")
                remove("${KEY_COOKIES_PREFIX}${i}_secure")
                remove("${KEY_COOKIES_PREFIX}${i}_httponly")
            }
            putInt(KEY_COOKIE_COUNT, 0)
        }.apply()
    }
    
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
    
    fun hasValidSession(): Boolean {
        val lastLoginTime = prefs.getLong(KEY_LAST_LOGIN_TIME, 0)
        val isSessionValid = prefs.getBoolean(KEY_SESSION_VALID, false)
        val isSessionExpired = System.currentTimeMillis() - lastLoginTime > SESSION_TIMEOUT_MS
        
        return isSessionValid && !isSessionExpired
    }
    
    fun getServerUrl(): String? {
        return prefs.getString(KEY_SERVER_URL, null)
    }
    
    fun getServerPassword(): String? {
        return prefs.getString(KEY_SERVER_PASSWORD, null)
    }
} 