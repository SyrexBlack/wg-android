package com.wgandroid.client.data.api

import com.wgandroid.client.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface WgEasyApiService {
    
    @GET("/api/wireguard/client")
    suspend fun getClients(): Response<List<WireguardClient>>
    
    @POST("/api/wireguard/client")
    suspend fun createClient(@Body request: CreateClientRequest): Response<WireguardClient>
    
    @DELETE("/api/wireguard/client/{clientId}")
    suspend fun deleteClient(@Path("clientId") clientId: String): Response<Unit>
    
    @POST("/api/wireguard/client/{clientId}/enable")
    suspend fun enableClient(@Path("clientId") clientId: String): Response<Unit>
    
    @POST("/api/wireguard/client/{clientId}/disable")
    suspend fun disableClient(@Path("clientId") clientId: String): Response<Unit>
    
    @GET("/api/wireguard/client/{clientId}/configuration")
    suspend fun getClientConfig(@Path("clientId") clientId: String): Response<ResponseBody>
    
    @GET("/api/wireguard/client/{clientId}/qrcode.svg")
    suspend fun getClientQRCode(@Path("clientId") clientId: String): Response<ResponseBody>
    
    @GET("/api/session")
    suspend fun getSession(): Response<ServerInfo>
} 