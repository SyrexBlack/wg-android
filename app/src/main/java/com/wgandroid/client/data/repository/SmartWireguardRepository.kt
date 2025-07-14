package com.wgandroid.client.data.repository

import com.wgandroid.client.data.api.SmartApiClient
import com.wgandroid.client.data.model.CreateClientRequest
import com.wgandroid.client.data.model.WireguardClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartWireguardRepository {
    private val apiService get() = SmartApiClient.getApiService()
    
    suspend fun getClients(): Result<List<WireguardClient>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getClients()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createClient(name: String): Result<WireguardClient> = withContext(Dispatchers.IO) {
        try {
            val request = CreateClientRequest(name)
            val response = apiService.createClient(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteClient(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteClient(clientId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun enableClient(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.enableClient(clientId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun disableClient(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.disableClient(clientId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getClientConfig(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getClientConfig(clientId)
            if (response.isSuccessful) {
                val config = response.body()?.string()
                if (config != null) {
                    Result.success(config)
                } else {
                    Result.failure(Exception("Empty config response"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getClientQRCode(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getClientQRCode(clientId)
            if (response.isSuccessful) {
                val qrSvg = response.body()?.string()
                if (qrSvg != null) {
                    Result.success(qrSvg)
                } else {
                    Result.failure(Exception("Empty QR code response"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun configureServer(url: String, password: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            SmartApiClient.setServerConfig(url, password)
            val format = SmartApiClient.getSuccessfulFormat()
            if (format != null) {
                Result.success(format)
            } else {
                Result.failure(Exception("Failed to establish connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 