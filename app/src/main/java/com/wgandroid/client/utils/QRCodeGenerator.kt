package com.wgandroid.client.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.wgandroid.client.data.api.CachedSmartApiClient
import com.wgandroid.client.data.model.WireguardClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QRCodeGenerator {
    
    private const val QR_CODE_SIZE = 512
    
    /**
     * Генерирует QR код для клиента WireGuard
     */
    suspend fun generateQRCode(context: Context, client: WireguardClient): Bitmap {
        return withContext(Dispatchers.IO) {
            try {
                // Получаем конфигурацию клиента
                val config = getClientConfig(client)
                
                // Генерируем QR код
                generateQRCodeFromString(config)
            } catch (e: Exception) {
                throw Exception("Ошибка генерации QR кода: ${e.message}")
            }
        }
    }
    
    /**
     * Генерирует QR код из строки конфигурации
     */
    fun generateQRCodeFromString(content: String): Bitmap {
        try {
            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            
            val bitMatrix: BitMatrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
            )
            
            return createBitmapFromMatrix(bitMatrix)
        } catch (e: WriterException) {
            throw Exception("Ошибка создания QR кода: ${e.message}")
        }
    }
    
    /**
     * Получает конфигурацию клиента через API
     */
    private suspend fun getClientConfig(client: WireguardClient): String {
        val apiClient = CachedSmartApiClient
        
        return try {
            val response = apiClient.getClientConfig(client.id)
            if (response.isSuccessful) {
                response.body()?.string() ?: throw Exception("Пустая конфигурация")
            } else {
                throw Exception("Ошибка получения конфигурации: ${response.code()}")
            }
        } catch (e: Exception) {
            // Если не удалось получить через API, создаем базовую конфигурацию
            generateFallbackConfig(client)
        }
    }
    
    /**
     * Создает базовую конфигурацию если API недоступен
     */
    private fun generateFallbackConfig(client: WireguardClient): String {
        return """
[Interface]
PrivateKey = ${client.privateKey ?: "YOUR_PRIVATE_KEY"}
Address = ${client.address}
DNS = 1.1.1.1, 8.8.8.8

[Peer]
PublicKey = ${client.publicKey}
Endpoint = YOUR_SERVER_ENDPOINT:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
        """.trimIndent()
    }
    
    /**
     * Создает Bitmap из BitMatrix
     */
    private fun createBitmapFromMatrix(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (matrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }
        
        return bitmap
    }
    
    /**
     * Проверяет, содержит ли строка валидную конфигурацию WireGuard
     */
    fun isValidWireGuardConfig(config: String): Boolean {
        return config.contains("[Interface]") && 
               config.contains("[Peer]") &&
               (config.contains("PrivateKey") || config.contains("PublicKey"))
    }
} 