package com.wgandroid.client.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object FileDownloader {
    
    fun downloadConfigFile(
        context: Context,
        fileName: String,
        content: String
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - используем MediaStore API
                saveFileToDownloadsApi29Plus(context, fileName, content)
            } else {
                // Android 9 и ниже - используем внешнее хранилище
                saveFileToDownloadsLegacy(fileName, content)
            }
        } catch (e: Exception) {
            DebugUtils.log("FileDownloader", "Error downloading file: ${e.message}")
            false
        }
    }
    
    private fun saveFileToDownloadsApi29Plus(
        context: Context,
        fileName: String,
        content: String
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let { 
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()
                }
                true
            } ?: false
        } catch (e: Exception) {
            DebugUtils.log("FileDownloader", "Error saving file with MediaStore: ${e.message}")
            false
        }
    }
    
    private fun saveFileToDownloadsLegacy(
        fileName: String,
        content: String
    ): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray())
                outputStream.flush()
            }
            
            true
        } catch (e: Exception) {
            DebugUtils.log("FileDownloader", "Error saving file legacy: ${e.message}")
            false
        }
    }
    
    fun saveFileToInternalStorage(
        context: Context,
        fileName: String,
        content: String
    ): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            DebugUtils.log("FileDownloader", "Error saving to internal storage: ${e.message}")
            false
        }
    }
} 