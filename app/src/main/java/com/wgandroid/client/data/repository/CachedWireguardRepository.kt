package com.wgandroid.client.data.repository

import com.wgandroid.client.data.api.CachedSmartApiClient
import com.wgandroid.client.data.model.CreateClientRequest
import com.wgandroid.client.data.model.WireguardClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachedWireguardRepository {
    
    suspend fun getClients(): Result<List<WireguardClient>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
            val response = apiService.getClients()
            
            if (response.isSuccessful) {
                val clients = response.body() ?: emptyList()
                Result.success(clients)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createClient(name: String): Result<WireguardClient> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
            val request = CreateClientRequest(name)
            val response = apiService.createClient(request)
            
            if (response.isSuccessful) {
                val client = response.body()
                if (client != null) {
                    Result.success(client)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteClient(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
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
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
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
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
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
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
            val response = apiService.getClientConfig(clientId)
            
            if (response.isSuccessful) {
                val config = response.body()?.string() ?: ""
                Result.success(config)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getQRCode(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiService = CachedSmartApiClient.getApiService()
            val response = apiService.getClientQRCode(clientId)
            
            if (response.isSuccessful) {
                val qrCode = response.body()?.string() ?: ""
                Result.success(qrCode)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 