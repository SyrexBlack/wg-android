package com.wgandroid.client.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SmartApiClient {
    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""
    private var successfulFormat: LoginFormat? = null
    private var noAuthRequired: Boolean = false
    private val cookieJar = MemoryCookieJar()
    private var logCallback: ((String) -> Unit)? = null
    
    enum class LoginFormat {
        JSON_PASSWORD,
        JSON_PASS,
        FORM_PASSWORD,
        FORM_PASS,
        PLAIN_TEXT
    }
    
    fun setLogCallback(callback: (String) -> Unit) {
        logCallback = callback
    }
    
    private fun addLog(message: String) {
        Log.d("SmartApiClient", message)
        logCallback?.invoke(message)
    }
    
    suspend fun setServerConfig(url: String, pwd: String? = null) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        retrofit = null // Force recreation
        successfulFormat = null // Сбрасываем предыдущие результаты
        noAuthRequired = false
        
        // Если есть пароль, находим рабочий формат и логинимся
        if (pwd != null && pwd.isNotBlank()) {
            findWorkingFormatAndLogin(pwd)
        } else {
            // Пробуем без авторизации
            addLog("🔓 Пытаемся подключиться без авторизации...")
            tryWithoutAuth()
        }
    }
    
    private suspend fun tryWithoutAuth() = withContext(Dispatchers.IO) {
        if (baseUrl.isEmpty()) return@withContext
        
        addLog("🔍 === ПРОВЕРКА БЕЗ АВТОРИЗАЦИИ ===")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        // Пробуем получить список клиентов без авторизации
        val endpoints = listOf(
            "api/wireguard/client",
            "api/clients"
        )
        
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
                    noAuthRequired = true
                    successfulFormat = null
                    response.close()
                    return@withContext
                } else if (response.code == 401) {
                    addLog("🔐 Эндпоинт требует авторизацию")
                } else {
                    addLog("❌ Код ответа: ${response.code}")
                }
                
                response.close()
            } catch (e: Exception) {
                addLog("❌ Ошибка для $endpoint: ${e.message}")
            }
        }
        
        addLog("❌ Все эндпоинты требуют авторизацию или недоступны")
    }
    
    private suspend fun findWorkingFormatAndLogin(pwd: String) = withContext(Dispatchers.IO) {
        if (baseUrl.isEmpty()) return@withContext
        
        addLog("🔍 === НАЧИНАЕМ ПОИСК ФОРМАТА АВТОРИЗАЦИИ ===")
        addLog("🌐 Base URL: $baseUrl")
        addLog("🔑 Password length: ${pwd.length}")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        val formats = listOf(
            LoginFormat.JSON_PASSWORD,
            LoginFormat.JSON_PASS, 
            LoginFormat.FORM_PASSWORD,
            LoginFormat.FORM_PASS,
            LoginFormat.PLAIN_TEXT
        )
        
        // Альтернативные эндпоинты для авторизации
        val endpoints = listOf(
            "api/session",
            "api/auth",
            "api/login",
            "session",
            "auth",
            "login"
        )
        
        // Сначала проверяем доступность сервера
        try {
            addLog("🏓 Проверяем доступность сервера...")
            val pingRequest = Request.Builder()
                .url(baseUrl)
                .get()
                .build()
            val pingResponse = client.newCall(pingRequest).execute()
            addLog("🏓 Ping response: ${pingResponse.code} ${pingResponse.message}")
            pingResponse.close()
        } catch (e: Exception) {
            addLog("❌ Сервер недоступен: ${e.message}")
        }
        
        var totalAttempts = 0
        val maxAttempts = formats.size * endpoints.size
        
                for (endpoint in endpoints) {
            addLog("🎯 Пробуем эндпоинт: $endpoint")
            
            for (format in formats) {
                totalAttempts++
                try {
                    addLog("🔍 [$totalAttempts/$maxAttempts] $endpoint + $format")
                    
                    val request = createLoginRequestForEndpoint(endpoint, format, pwd)
                    addLog("📤 Request URL: ${request.url}")
                    addLog("📤 Request Method: ${request.method}")
                    addLog("📤 Request Headers: ${request.headers}")
                    
                    // Логируем тело запроса (упрощенно)
                    val cleanPwd = pwd.trim()
                    val bodyDescription = when (format) {
                        LoginFormat.JSON_PASSWORD -> """{"password":"$cleanPwd"}"""
                        LoginFormat.JSON_PASS -> """{"pass":"$cleanPwd"}"""
                        LoginFormat.FORM_PASSWORD -> "password=$cleanPwd"
                        LoginFormat.FORM_PASS -> "pass=$cleanPwd"
                        LoginFormat.PLAIN_TEXT -> cleanPwd
                    }
                    addLog("📤 Request Body: $bodyDescription")
                    
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    
                    addLog("📥 [$endpoint/$format] Response Code: ${response.code}")
                    addLog("📥 [$endpoint/$format] Response Message: ${response.message}")
                    addLog("📥 [$endpoint/$format] Response Headers: ${response.headers}")
                    addLog("📥 [$endpoint/$format] Response Body: ${responseBody?.take(500)}...")
                    
                    // Проверяем успешность более детально
                    when {
                        response.isSuccessful -> {
                            successfulFormat = format
                            addLog("✅ SUCCESS! Endpoint: $endpoint, Format: $format")
                            addLog("🍪 Set-Cookie headers: ${response.headers("Set-Cookie")}")
                            response.close()
                            return@withContext
                        }
                        response.code == 401 -> {
                            addLog("🔐 [$endpoint/$format] Unauthorized - неправильный пароль или формат")
                        }
                        response.code == 404 -> {
                            addLog("🚫 [$endpoint/$format] Not Found - эндпоинт не существует")
                        }
                        response.code == 403 -> {
                            addLog("⛔ [$endpoint/$format] Forbidden - доступ запрещен")
                        }
                        response.code == 405 -> {
                            addLog("🚷 [$endpoint/$format] Method Not Allowed - неправильный HTTP метод")
                        }
                        response.code >= 500 -> {
                            addLog("🔥 [$endpoint/$format] Server Error - ошибка сервера")
                        }
                        else -> {
                            addLog("❌ [$endpoint/$format] Неожиданный код ответа: ${response.code}")
                        }
                    }
                    
                    response.close()
                } catch (e: Exception) {
                    addLog("💥 [$endpoint/$format] Ошибка: ${e.message}")
                    addLog("💥 [$endpoint/$format] Тип ошибки: ${e.javaClass.simpleName}")
                    if (e.message?.contains("timeout") == true) {
                        addLog("⏰ [$endpoint/$format] Таймаут - сервер не отвечает вовремя")
                    } else if (e.message?.contains("ConnectException") == true) {
                        addLog("🔌 [$endpoint/$format] Не удается подключиться к серверу")
                    } else if (e.message?.contains("UnknownHostException") == true) {
                        addLog("🌐 [$endpoint/$format] Хост не найден")
                    }
                }
            }
        }
        
        addLog("❌ === ВСЕ ФОРМАТЫ И ЭНДПОИНТЫ ПРОВАЛИЛИСЬ ===")
        addLog("❌ Проверено $totalAttempts комбинаций")
        throw Exception("All login formats failed! Check server and password.")
    }
    
    private fun createLoginRequest(format: LoginFormat, password: String): Request {
        return createLoginRequestForEndpoint("api/session", format, password)
    }
    
    private fun createLoginRequestForEndpoint(endpoint: String, format: LoginFormat, password: String): Request {
        val url = "$baseUrl$endpoint"
        
        val cleanPassword = password.trim() // Убираем пробелы
        
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
    
    fun getApiService(): WgEasyApiService {
        if (baseUrl.isEmpty()) {
            throw IllegalStateException("Server URL not configured. Call setServerConfig() first.")
        }
        
        // successfulFormat может быть null, если авторизация не требуется
        // Проверяем, что мы хотя бы попытались подключиться
        
        // Lazy initialization
        if (retrofit == null) {
            retrofit = createRetrofit()
        }
        
        return retrofit!!.create(WgEasyApiService::class.java)
    }
    
    private fun createRetrofit(): Retrofit {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableMapOf<String, List<Cookie>>()
        
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies[url.host] = cookies
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies[url.host] ?: emptyList()
        }
    }
    
    fun getSuccessfulFormat(): String? {
        return when {
            successfulFormat != null -> successfulFormat!!.name
            noAuthRequired -> "NO_AUTH_REQUIRED"
            else -> null
        }
    }
} 