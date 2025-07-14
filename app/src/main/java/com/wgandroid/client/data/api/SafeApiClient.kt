package com.wgandroid.client.data.api

import com.wgandroid.client.data.model.LoginRequest
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SafeApiClient {
    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""
    private var password: String? = null
    private val cookieJar = MemoryCookieJar()
    
    suspend fun setServerConfig(url: String, pwd: String? = null) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        password = pwd
        retrofit = null // Force recreation
        
        // Если есть пароль, выполняем логин сразу
        pwd?.let { 
            performLogin(it)
        }
    }
    
    private suspend fun performLogin(pwd: String) {
        if (baseUrl.isEmpty()) return
        
        try {
            // Создаем временный retrofit для логина
            val tempRetrofit = createRetrofit()
            val apiService = tempRetrofit.create(WgEasyApiService::class.java)
            
            // Выполняем логин
            val loginRequest = LoginRequest(pwd)
            val response = apiService.login(loginRequest)
            
            if (!response.isSuccessful) {
                throw Exception("Login failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            throw Exception("Login error: ${e.message}", e)
        }
    }
    
    fun getApiService(): WgEasyApiService {
        if (baseUrl.isEmpty()) {
            throw IllegalStateException("Server URL not configured. Call setServerConfig() first.")
        }
        
        // Lazy initialization - create retrofit only when needed
        if (retrofit == null) {
            retrofit = createRetrofit()
        }
        
        return retrofit!!.create(WgEasyApiService::class.java)
    }
    
    private fun createRetrofit(): Retrofit {
        if (baseUrl.isEmpty()) {
            throw IllegalStateException("Base URL is empty. Call setServerConfig() first.")
        }
        
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar) // Используем cookie-based аутентификацию
        
        // Add logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        httpClient.addInterceptor(loggingInterceptor)
        
        // НЕ ДОБАВЛЯЕМ Authorization заголовки - используем cookies!
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Простая реализация CookieJar для хранения cookies в памяти
    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableMapOf<String, List<Cookie>>()
        
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies[url.host] = cookies
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies[url.host] ?: emptyList()
        }
    }
    
    // NO INIT BLOCK! - это и была проблема в оригинальном ApiClient
} 