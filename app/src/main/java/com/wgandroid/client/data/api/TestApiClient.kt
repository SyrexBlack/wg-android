package com.wgandroid.client.data.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import java.io.IOException

class TestApiClient {
    
    fun testLogin(baseUrl: String, password: String) {
        val client = OkHttpClient()
        
        // Тест 1: JSON с password
        testJsonLogin(client, baseUrl, password, "password")
        
        // Тест 2: JSON с pass
        testJsonLogin(client, baseUrl, password, "pass")
        
        // Тест 3: Form data с password 
        testFormLogin(client, baseUrl, password, "password")
        
        // Тест 4: Form data с pass
        testFormLogin(client, baseUrl, password, "pass")
        
        // Тест 5: Простой text/plain
        testPlainLogin(client, baseUrl, password)
    }
    
    private fun testJsonLogin(client: OkHttpClient, baseUrl: String, password: String, fieldName: String) {
        try {
            val json = """{"$fieldName":"$password"}"""
            val requestBody = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/api/session")
                .post(requestBody)
                .build()
                
            Log.d("TestApiClient", "Testing JSON with field '$fieldName'...")
            
            client.newCall(request).execute().use { response ->
                Log.d("TestApiClient", "JSON $fieldName - Code: ${response.code}, Body: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e("TestApiClient", "JSON $fieldName error: ${e.message}")
        }
    }
    
    private fun testFormLogin(client: OkHttpClient, baseUrl: String, password: String, fieldName: String) {
        try {
            val formBody = FormBody.Builder()
                .add(fieldName, password)
                .build()
                
            val request = Request.Builder()
                .url("$baseUrl/api/session")
                .post(formBody)
                .build()
                
            Log.d("TestApiClient", "Testing Form with field '$fieldName'...")
            
            client.newCall(request).execute().use { response ->
                Log.d("TestApiClient", "Form $fieldName - Code: ${response.code}, Body: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e("TestApiClient", "Form $fieldName error: ${e.message}")
        }
    }
    
    private fun testPlainLogin(client: OkHttpClient, baseUrl: String, password: String) {
        try {
            val requestBody = password.toRequestBody("text/plain".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/api/session")
                .post(requestBody)
                .build()
                
            Log.d("TestApiClient", "Testing plain text...")
            
            client.newCall(request).execute().use { response ->
                Log.d("TestApiClient", "Plain text - Code: ${response.code}, Body: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e("TestApiClient", "Plain text error: ${e.message}")
        }
    }
} 