package com.wgandroid.client.data.model

import com.google.gson.annotations.SerializedName

data class WireguardClient(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("publicKey") 
    val publicKey: String,
    
    @SerializedName("privateKey")
    val privateKey: String?,
    
    @SerializedName("enabled")
    val enabled: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String,
    
    @SerializedName("latestHandshakeAt")
    val latestHandshakeAt: String?,
    
    @SerializedName("transferRx")
    val transferRx: Long,
    
    @SerializedName("transferTx")
    val transferTx: Long,
    
    @SerializedName("transferRxCurrent")
    val transferRxCurrent: Double,
    
    @SerializedName("transferTxCurrent")
    val transferTxCurrent: Double
)

data class CreateClientRequest(
    @SerializedName("name")
    val name: String
)

data class CreateClientResponse(
    @SerializedName("client")
    val client: WireguardClient
)

data class ClientsResponse(
    @SerializedName("clients")
    val clients: List<WireguardClient>
)

data class ServerInfo(
    @SerializedName("version")
    val version: String?,
    
    @SerializedName("latestRelease")
    val latestRelease: Release?
)

data class Release(
    @SerializedName("version")
    val version: String,
    
    @SerializedName("changelog")
    val changelog: String
) 