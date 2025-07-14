package com.wgandroid.client.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wgandroid.client.MainActivity
import com.wgandroid.client.R

object WGNotificationManager {
    private const val CHANNEL_ID = "wg_clients_channel"
    private const val CHANNEL_NAME = "WireGuard Клиенты"
    private const val CHANNEL_DESCRIPTION = "Уведомления о состоянии WireGuard клиентов"
    
    // Notification IDs
    private const val CLIENT_STATUS_ID = 1001
    private const val TRAFFIC_ALERT_ID = 1002
    private const val CONNECTION_ERROR_ID = 1003
    private const val WEEKLY_REPORT_ID = 1004
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showClientStatusNotification(
        context: Context,
        clientName: String,
        isEnabled: Boolean,
        showLongMessage: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return
        
        val icon = if (isEnabled) "🟢" else "🔴"
        val status = if (isEnabled) "подключен" else "отключен"
        val title = "$icon Клиент $clientName $status"
        val message = if (showLongMessage) {
            if (isEnabled) {
                "Клиент успешно подключен к VPN серверу. Трафик защищен."
            } else {
                "Клиент отключен от VPN сервера. Проверьте подключение."
            }
        } else {
            "Статус изменен в ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
        }
        
        showNotification(
            context = context,
            id = CLIENT_STATUS_ID,
            title = title,
            message = message,
            isHighPriority = !isEnabled // Высокий приоритет для отключений
        )
    }
    
    fun showTrafficAlertNotification(
        context: Context,
        clientName: String,
        trafficAmount: Long,
        isDownload: Boolean = true
    ) {
        if (!hasNotificationPermission(context)) return
        
        val direction = if (isDownload) "скачал" else "загрузил"
        val icon = if (isDownload) "📥" else "📤"
        val formattedTraffic = formatBytes(trafficAmount)
        
        val title = "$icon Высокий трафик"
        val message = "Клиент $clientName $direction $formattedTraffic данных"
        
        showNotification(
            context = context,
            id = TRAFFIC_ALERT_ID,
            title = title,
            message = message,
            isHighPriority = true
        )
    }
    
    fun showConnectionErrorNotification(
        context: Context,
        errorMessage: String
    ) {
        if (!hasNotificationPermission(context)) return
        
        val title = "❌ Ошибка подключения"
        val message = "Не удалось подключиться к серверу: $errorMessage"
        
        showNotification(
            context = context,
            id = CONNECTION_ERROR_ID,
            title = title,
            message = message,
            isHighPriority = true
        )
    }
    
    fun showWeeklyReportNotification(
        context: Context,
        totalClients: Int,
        activeClients: Int,
        totalTraffic: Long
    ) {
        if (!hasNotificationPermission(context)) return
        
        val title = "📊 Еженедельный отчет"
        val message = "Клиентов: $totalClients (активных: $activeClients)\n" +
                "Общий трафик: ${formatBytes(totalTraffic)}"
        
        showNotification(
            context = context,
            id = WEEKLY_REPORT_ID,
            title = title,
            message = message,
            isHighPriority = false,
            isBigText = true
        )
    }
    
    fun showMultiClientUpdateNotification(
        context: Context,
        updatedClients: List<String>,
        allConnected: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        
        val icon = if (allConnected) "✅" else "⚠️"
        val status = if (allConnected) "подключены" else "изменили статус"
        val title = "$icon Обновление клиентов"
        
        val message = if (updatedClients.size <= 3) {
            "Клиенты ${updatedClients.joinToString(", ")} $status"
        } else {
            "${updatedClients.size} клиентов $status"
        }
        
        showNotification(
            context = context,
            id = CLIENT_STATUS_ID,
            title = title,
            message = message,
            isHighPriority = !allConnected
        )
    }
    
    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        isHighPriority: Boolean = false,
        isBigText: Boolean = false
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val priority = if (isHighPriority) {
                NotificationCompat.PRIORITY_HIGH
            } else {
                NotificationCompat.PRIORITY_DEFAULT
            }
            
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
            if (isBigText) {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
            }
            
            // Добавляем действия для уведомлений клиентов
            if (id == CLIENT_STATUS_ID) {
                val refreshIntent = Intent(context, MainActivity::class.java).apply {
                    action = "REFRESH_CLIENTS"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val refreshPendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                builder.addAction(
                    android.R.drawable.ic_menu_rotate,
                    "Обновить",
                    refreshPendingIntent
                )
            }
            
            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    fun clearAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun clearNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
} 