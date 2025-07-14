package com.wgandroid.client.data.api

import android.content.Context
import android.util.Log
import com.wgandroid.client.data.cache.AuthCache
import com.wgandroid.client.utils.DebugUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object CachedSmartApiClient {
    private var retrofit: Retrofit? = null
    private var authCache: AuthCache? = null
    private var currentBaseUrl: String = ""
    private val cookieJar = MemoryCookieJar()
    private var logCallback: ((String) -> Unit)? = null
    
    enum class LoginFormat {
        JSON_PASSWORD,
        JSON_PASS,
        FORM_PASSWORD,
        FORM_PASS,
        PLAIN_TEXT
    }
    
    fun initialize(context: Context) {
        authCache = AuthCache(context)
        addLog("🔧 CachedSmartApiClient инициализирован")
    }
    
    fun setLogCallback(callback: (String) -> Unit) {
        logCallback = callback
    }
    
    private fun addLog(message: String) {
        DebugUtils.log("CachedSmartApiClient", message)
        logCallback?.invoke(message)
    }
    
    suspend fun setServerConfig(url: String, pwd: String? = null, forceReauth: Boolean = false) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        currentBaseUrl = cleanUrl
        retrofit = null // Force recreation
        
        if (authCache == null) {
            throw IllegalStateException("CachedSmartApiClient не инициализирован. Вызовите initialize() сначала.")
        }
        
        addLog("🌐 Настройка сервера: $cleanUrl")
        
        // Проверяем кешированную авторизацию, если не принудительная переавторизация
        if (!forceReauth && pwd != null) {
            val cachedAuth = authCache!!.getAuthData()
            if (cachedAuth != null && cachedAuth.serverUrl == cleanUrl && cachedAuth.isSessionValid) {
                addLog("🎯 Найдена валидная кешированная сессия!")
                
                // Восстанавливаем cookies
                cookieJar.restoreCookies(cachedAuth.cookies)
                addLog("🍪 Cookies восстановлены: ${cachedAuth.cookies.size} шт.")
                
                // Проверяем, что сессия действительно работает
                if (testCachedSession()) {
                    addLog("✅ Кешированная сессия активна, пропускаем авторизацию")
                    return
                } else {
                    addLog("❌ Кешированная сессия не работает, начинаем повторную авторизацию")
                    authCache!!.invalidateSession()
                }
            }
        }
        
        // Выполняем авторизацию
        if (pwd != null && pwd.isNotBlank()) {
            findWorkingFormatAndLogin(cleanUrl, pwd)
        } else {
            // Пробуем без авторизации
            addLog("🔓 Пытаемся подключиться без авторизации...")
            tryWithoutAuth(cleanUrl)
        }
    }
    
    private suspend fun testCachedSession(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = createHttpClient()
            val testEndpoints = listOf("api/wireguard/client", "api/clients")
            
            for (endpoint in testEndpoints) {
                try {
                    val testUrl = "$currentBaseUrl$endpoint"
                    val request = Request.Builder()
                        .url(testUrl)
                        .get()
                        .build()
                    
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.close()
                        return@withContext true
                    }
                    response.close()
                } catch (e: Exception) {
                    // Продолжаем проверку других эндпоинтов
                }
            }
            false
        } catch (e: Exception) {
            addLog("❌ Ошибка проверки кешированной сессии: ${e.message}")
            false
        }
    }
    
    private suspend fun tryWithoutAuth(baseUrl: String) = withContext(Dispatchers.IO) {
        addLog("🔍 === ПРОВЕРКА БЕЗ АВТОРИЗАЦИИ ===")
        
        val client = createHttpClient()
        val endpoints = listOf("api/wireguard/client", "api/clients")
        
        for (endpoint in endpoints) {
            try {
                val testUrl = "$baseUrl$endpoint"
                addLog("🌐 Пробуем GET $testUrl")
                
                val request = Request.Builder()
                    .url(testUrl)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                addLog("📥 Response: ${response.code} ${response.message}")
                
                if (response.isSuccessful) {
                    addLog("✅ Подключение БЕЗ авторизации работает!")
                    
                    // Сохраняем в кеш
                    authCache?.saveAuthData(
                        serverUrl = baseUrl,
                        serverPassword = null,
                        loginFormat = null,
                        noAuthRequired = true,
                        cookies = emptyList()
                    )
                    
                    response.close()
                    return@withContext
                }
                
                response.close()
            } catch (e: Exception) {
                addLog("❌ Ошибка для $endpoint: ${e.message}")
            }
        }
        
        addLog("❌ Все эндпоинты требуют авторизацию или недоступны")
    }
    
    private suspend fun findWorkingFormatAndLogin(baseUrl: String, pwd: String) = withContext(Dispatchers.IO) {
        addLog("🔍 === НАЧИНАЕМ ПОИСК ФОРМАТА АВТОРИЗАЦИИ ===")
        addLog("🌐 Base URL: $baseUrl")
        addLog("🔑 Password length: ${pwd.length}")
        
        val client = createHttpClient()
        val formats = LoginFormat.values().toList()
        val endpoints = listOf(
            "api/session",
            "api/auth", 
            "api/login",
            "session",
            "auth",
            "login"
        )
        
        var totalAttempts = 0
        val maxAttempts = formats.size * endpoints.size
        
        for (endpoint in endpoints) {
            addLog("🎯 Пробуем эндпоинт: $endpoint")
            
            for (format in formats) {
                totalAttempts++
                try {
                    addLog("🔍 [$totalAttempts/$maxAttempts] $endpoint + $format")
                    
                    val request = createLoginRequestForEndpoint(baseUrl, endpoint, format, pwd)
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    
                    addLog("📥 [$endpoint/$format] Response Code: ${response.code}")
                    
                    if (response.isSuccessful) {
                        addLog("✅ SUCCESS! Endpoint: $endpoint, Format: $format")
                        
                        // Сохраняем успешную авторизацию в кеш
                        val cookiesList = cookieJar.getAllCookies()
                        authCache?.saveAuthData(
                            serverUrl = baseUrl,
                            serverPassword = pwd,
                            loginFormat = format.name,
                            noAuthRequired = false,
                            cookies = cookiesList
                        )
                        
                        addLog("💾 Авторизация сохранена в кеш")
                        addLog("🍪 Cookies сохранены: ${cookiesList.size} шт.")
                        
                        response.close()
                        return@withContext
                    }
                    
                    response.close()
                } catch (e: Exception) {
                    addLog("💥 [$endpoint/$format] Ошибка: ${e.message}")
                }
            }
        }
        
        addLog("❌ === ВСЕ ФОРМАТЫ И ЭНДПОИНТЫ ПРОВАЛИЛИСЬ ===")
        throw Exception("All login formats failed! Check server and password.")
    }
    
    private fun createLoginRequestForEndpoint(baseUrl: String, endpoint: String, format: LoginFormat, password: String): Request {
        val url = "$baseUrl$endpoint"
        val cleanPassword = password.trim()
        
        return when (format) {
            LoginFormat.JSON_PASSWORD -> {
                val json = """{"password":"$cleanPassword"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                Request.Builder().url(url).post(body).build()
            }
            
            LoginFormat.JSON_PASS -> {
                val json = """{"pass":"$cleanPassword"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                Request.Builder().url(url).post(body).build()
            }
            
            LoginFormat.FORM_PASSWORD -> {
                val body = FormBody.Builder()
                    .add("password", cleanPassword)
                    .build()
                Request.Builder().url(url).post(body).build()
            }
            
            LoginFormat.FORM_PASS -> {
                val body = FormBody.Builder()
                    .add("pass", cleanPassword)
                    .build()
                Request.Builder().url(url).post(body).build()
            }
            
            LoginFormat.PLAIN_TEXT -> {
                val body = cleanPassword.toRequestBody("text/plain".toMediaType())
                Request.Builder().url(url).post(body).build()
            }
        }
    }
    
    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            
        // Добавляем логирование только в debug сборках
        DebugUtils.debugOnly {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        
        return builder.build()
    }
    
    fun getApiService(): WgEasyApiService {
        if (currentBaseUrl.isEmpty()) {
            throw IllegalStateException("Server URL not configured. Call setServerConfig() first.")
        }
        
        if (retrofit == null) {
            retrofit = createRetrofit()
        }
        
        return retrofit!!.create(WgEasyApiService::class.java)
    }
    
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(createHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    suspend fun getSuccessfulFormat(): String? {
        return authCache?.getAuthData()?.let { auth ->
            when {
                auth.noAuthRequired -> "NO_AUTH_REQUIRED"
                auth.loginFormat != null -> auth.loginFormat
                else -> null
            }
        }
    }
    
    suspend fun logout() {
        authCache?.clearAll()
        cookieJar.clear()
        retrofit = null
        addLog("🚪 Выход выполнен, кеш очищен")
    }
    
    fun hasValidSession(): Boolean {
        return authCache?.hasValidSession() ?: false
    }
    
    fun getCachedServerUrl(): String? {
        return authCache?.getServerUrl()
    }
    
    fun getCachedServerPassword(): String? {
        return authCache?.getServerPassword()
    }
    
    suspend fun getClientConfig(clientId: String): retrofit2.Response<ResponseBody> {
        return getApiService().getClientConfig(clientId)
    }
    
    suspend fun connectToServer(serverUrl: String, password: String): Result<Unit> {
        return try {
            setServerConfig(serverUrl, password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCachedSession(): String {
        return getCachedServerUrl() ?: ""
    }
    
    suspend fun clearCache() {
        logout()
    }
    
    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableMapOf<String, List<Cookie>>()
        
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies[url.host] = cookies
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies[url.host] ?: emptyList()
        }
        
        fun restoreCookies(cookieList: List<Cookie>) {
            cookieList.forEach { cookie ->
                val host = cookie.domain
                val existingCookies = cookies[host] ?: emptyList()
                cookies[host] = existingCookies + cookie
            }
        }
        
        fun getAllCookies(): List<Cookie> {
            return cookies.values.flatten()
        }
        
        fun clear() {
            cookies.clear()
        }
    }
} 