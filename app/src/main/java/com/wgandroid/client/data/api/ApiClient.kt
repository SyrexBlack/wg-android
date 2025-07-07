package com.wgandroid.client.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""
    private var password: String? = null
    
    fun setServerConfig(url: String, pwd: String? = null) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        password = pwd
        retrofit = null // Force recreation
    }
    
    fun getApiService(): WgEasyApiService {
        if (retrofit == null || baseUrl.isEmpty()) {
            throw IllegalStateException("Server URL not configured. Call setServerConfig() first.")
        }
        return retrofit!!.create(WgEasyApiService::class.java)
    }
    
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
            
            // Add logging interceptor for debugging
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            httpClient.addInterceptor(loggingInterceptor)
            
            // Add authentication interceptor if password is provided
            password?.let { pwd ->
                httpClient.addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer $pwd")
                        .header("Content-Type", "application/json")
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
            }
            
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    
    init {
        getRetrofit()
    }
} 