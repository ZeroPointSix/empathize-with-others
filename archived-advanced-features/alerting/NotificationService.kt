package com.empathy.ai.data.alerting

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 通知服务
 * 
 * 负责发送各种类型的通知
 * 支持多种通知渠道和优先级
 */
class NotificationService private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NotificationService"
        
        // 通知渠道ID
        private const val CHANNEL_ID_CRITICAL = "ai_parser_critical"
        private const val CHANNEL_ID_WARNING = "ai_parser_warning"
        private const val CHANNEL_ID_INFO = "ai_parser_info"
        
        // 通知ID
        private const val NOTIFICATION_ID_CRITICAL = "critical_alert"
        private const val NOTIFICATION_ID_WARNING = "warning_alert"
        private const val NOTIFICATION_ID_INFO = "info_alert"
        
        // 请求码
        private const val REQUEST_CODE_DISMISS = 1001
        private const val REQUEST_CODE_RESOLVE = 1002
        
        @Volatile
        private var INSTANCE: NotificationService? = null
        
        fun getInstance(context: Context): NotificationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    // 通知历史
    private val notificationHistory = CopyOnWriteArrayList<NotificationRecord>()
    
    // 通知配置
    private val notificationConfig = ConcurrentHashMap<String, NotificationConfig>()
    
    init {
        // 初始化通知渠道
        initializeNotificationChannels()
        
        // 初始化默认通知配置
        initializeDefaultNotificationConfigs()
        
        Log.i(TAG, "NotificationService 初始化完成")
    }
    
    /**
     * 发送告警通知
     */
    fun sendAlertNotification(
        type: AlertType,
        level: AlertLevel,
        title: String,
        message: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val config = getNotificationConfig(type, level)
        if (!config.enabled) {
            Log.d(TAG, "通知已禁用: $type, $level")
            return
        }
        
        try {
            val channelId = getChannelId(level)
            val notificationId = getNotificationId(level)
            
            // 创建通知
            val builder = NotificationCompat.Builder(context, notificationId)
                .setSmallIcon(R.drawable.ic_notification_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(getNotificationPriority(level))
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setAutoCancel(config.autoCancel)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
            
            // 添加操作按钮
            if (config.showActions) {
                addActionButtons(builder, type, level, metadata)
            }
            
            // 添加大文本样式
            if (config.showBigTextStyle) {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigTextContent(message)
                )
            }
            
            // 创建PendingIntent
            val intent = createNotificationIntent(type, level, metadata)
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_DISMISS,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.setContentIntent(pendingIntent)
            
            // 发送通知
            val notification = builder.build()
            notificationManager.notify(notificationId, notification)
            
            // 记录通知历史
            recordNotification(type, level, title, message, notification.id)
            
            Log.i(TAG, "发送告警通知: $type, $level")
            
        } catch (e: Exception) {
            Log.e(TAG, "发送通知失败", e)
        }
    }
    
    /**
     * 发送解析性能通知
     */
    fun sendPerformanceNotification(
        title: String,
        message: String,
        metrics: Map<String, Any>
    ) {
        val config = getNotificationConfig(AlertType.PERFORMANCE_DEGRADED, AlertLevel.WARNING)
        if (!config.enabled) {
            Log.d(TAG, "性能通知已禁用")
            return
        }
        
        try {
            val builder = NotificationCompat.Builder(context, NOTIFICATION_ID_WARNING)
                .setSmallIcon(R.drawable.ic_notification_performance)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(config.autoCancel)
            
            // 添加性能详情
            if (config.showDetailedInfo) {
                val details = formatPerformanceDetails(metrics)
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigTextContent(details)
                )
            }
            
            val notification = builder.build()
            notificationManager.notify(NOTIFICATION_ID_WARNING, notification)
            
            // 记录通知历史
            recordNotification(AlertType.PERFORMANCE_DEGRADED, AlertLevel.WARNING, title, message, notification.id)
            
            Log.i(TAG, "发送性能通知: $title")
            
        } catch (e: Exception) {
            Log.e(TAG, "发送性能通知失败", e)
        }
    }
    
    /**
     * 发送系统状态通知
     */
    fun sendSystemStatusNotification(
        title: String,
        message: String,
        status: SystemStatus
    ) {
        val level = when (status) {
            SystemStatus.HEALTHY -> AlertLevel.INFO
            SystemStatus.DEGRADED -> AlertLevel.WARNING
            SystemStatus.UNHEALTHY -> AlertLevel.WARNING
            SystemStatus.CRITICAL -> AlertLevel.CRITICAL
        }
        
        val config = getNotificationConfig(AlertType.SYSTEM_STATUS, level)
        if (!config.enabled) {
            Log.d(TAG, "系统状态通知已禁用")
            return
        }
        
        try {
            val channelId = getChannelId(level)
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_status)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(getNotificationPriority(level))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(config.autoCancel)
            
            // 添加状态图标
            builder.setSmallIcon(getStatusIcon(status))
            
            val notification = builder.build()
            notificationManager.notify(channelId, notification)
            
            // 记录通知历史
            recordNotification(AlertType.SYSTEM_STATUS, level, title, message, notification.id)
            
            Log.i(TAG, "发送系统状态通知: $title")
            
        } catch (e: Exception) {
            Log.e(TAG, "发送系统状态通知失败", e)
        }
    }
    
    /**
     * 清除通知
     */
    fun clearNotification(notificationId: String) {
        try {
            notificationManager.cancel(notificationId)
            Log.i(TAG, "清除通知: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "清除通知失败", e)
        }
    }
    
    /**
     * 清除所有通知
     */
    fun clearAllNotifications() {
        try {
            notificationManager.cancelAll()
            Log.i(TAG, "清除所有通知")
        } catch (e: Exception) {
            Log.e(TAG, "清除所有通知失败", e)
        }
    }
    
    /**
     * 更新通知配置
     */
    fun updateNotificationConfig(
        type: AlertType,
        level: AlertLevel,
        config: NotificationConfig
    ) {
        val key = getConfigKey(type, level)
        notificationConfig[key] = config
        
        Log.i(TAG, "更新通知配置: $key")
    }
    
    /**
     * 获取通知配置
     */
    fun getNotificationConfig(
        type: AlertType,
        level: AlertLevel
    ): NotificationConfig {
        val key = getConfigKey(type, level)
        return notificationConfig[key] ?: getDefaultNotificationConfig(level)
    }
    
    /**
     * 获取通知历史
     */
    fun getNotificationHistory(limit: Int = 50): List<NotificationRecord> {
        return synchronized(notificationHistory) {
            notificationHistory.takeLast(limit)
        }
    }
    
    /**
     * 获取通知统计
     */
    fun getNotificationStatistics(): NotificationStatistics {
        val allNotifications = synchronized(notificationHistory) {
            notificationHistory.toList()
        }
        
        val totalNotifications = allNotifications.size
        val notificationsByType = allNotifications.groupBy { it.type }
        val notificationsByLevel = allNotifications.groupBy { it.level }
        
        val typeDistribution = notificationsByType.mapValues { it.value.size }
        val levelDistribution = notificationsByLevel.mapValues { it.value.size }
        
        // 计算最近24小时的通知数量
        val last24Hours = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val recentNotifications = allNotifications.filter { it.timestamp > last24Hours }
        val recentCount = recentNotifications.size
        
        return NotificationStatistics(
            totalNotifications = totalNotifications,
            recent24Hours = recentCount,
            typeDistribution = typeDistribution,
            levelDistribution = levelDistribution,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * 初始化通知渠道
     */
    private fun initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                createNotificationChannel(
                    CHANNEL_ID_CRITICAL,
                    "严重告警",
                    "AI解析器的严重告警通知",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                createNotificationChannel(
                    CHANNEL_ID_WARNING,
                    "警告通知",
                    "AI解析器的警告通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                createNotificationChannel(
                    CHANNEL_ID_INFO,
                    "信息通知",
                    "AI解析器的信息通知",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            
            notificationManager.createNotificationChannels(channels)
        }
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel(
        channelId: String,
        name: String,
        description: String,
        importance: Int
    ): NotificationChannel {
        return NotificationChannel(
            channelId,
            name,
            description
        ).apply {
            enableLights = true
            enableVibration = true
            importance = importance
            setShowBadge(true)
        }
    }
    
    /**
     * 获取渠道ID
     */
    private fun getChannelId(level: AlertLevel): String {
        return when (level) {
            AlertLevel.CRITICAL -> CHANNEL_ID_CRITICAL
            AlertLevel.WARNING -> CHANNEL_ID_WARNING
            AlertLevel.INFO -> CHANNEL_ID_INFO
        }
    }
    
    /**
     * 获取通知ID
     */
    private fun getNotificationId(level: AlertLevel): String {
        return when (level) {
            AlertLevel.CRITICAL -> NOTIFICATION_ID_CRITICAL
            AlertLevel.WARNING -> NOTIFICATION_ID_WARNING
            AlertLevel.INFO -> NOTIFICATION_ID_INFO
        }
    }
    
    /**
     * 获取通知优先级
     */
    private fun getNotificationPriority(level: AlertLevel): Int {
        return when (level) {
            AlertLevel.CRITICAL -> NotificationCompat.PRIORITY_HIGH
            AlertLevel.WARNING -> NotificationCompat.PRIORITY_DEFAULT
            AlertLevel.INFO -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    /**
     * 添加操作按钮
     */
    private fun addActionButtons(
        builder: NotificationCompat.Builder,
        type: AlertType,
        level: AlertLevel,
        metadata: Map<String, Any>
    ) {
        // 解决按钮
        val resolveIntent = createNotificationIntent(type, level, metadata, "resolve")
        val resolvePendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_RESOLVE,
            resolveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification_resolve,
                "解决",
                resolvePendingIntent
            ).build()
        )
        
        // 忽略按钮
        val dismissIntent = createNotificationIntent(type, level, metadata, "dismiss")
        val dismissPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_DISMISS,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification_dismiss,
                "忽略",
                dismissPendingIntent
            ).build()
        )
    }
    
    /**
     * 创建通知Intent
     */
    private fun createNotificationIntent(
        type: AlertType,
        level: AlertLevel,
        metadata: Map<String, Any>,
        action: String? = null
    ): Intent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", type.name)
            putExtra("level", level.name)
            putExtra("action", action)
            metadata.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        return intent
    }
    
    /**
     * 获取状态图标
     */
    private fun getStatusIcon(status: SystemStatus): Int {
        return when (status) {
            SystemStatus.HEALTHY -> R.drawable.ic_status_healthy
            SystemStatus.DEGRADED -> R.drawable.ic_status_degraded
            SystemStatus.UNHEALTHY -> R.drawable.ic_status_unhealthy
            SystemStatus.CRITICAL -> R.drawable.ic_status_critical
        }
    }
    
    /**
     * 格式化性能详情
     */
    private fun formatPerformanceDetails(metrics: Map<String, Any>): String {
        val details = StringBuilder()
        
        metrics.forEach { (key, value) ->
            details.append("$key: $value\n")
        }
        
        return details.toString()
    }
    
    /**
     * 获取配置键
     */
    private fun getConfigKey(type: AlertType, level: AlertLevel): String {
        return "${type.name}_${level.name}"
    }
    
    /**
     * 获取默认通知配置
     */
    private fun getDefaultNotificationConfig(level: AlertLevel): NotificationConfig {
        return when (level) {
            AlertLevel.CRITICAL -> NotificationConfig(
                enabled = true,
                showActions = true,
                autoCancel = false,
                showBigTextStyle = true,
                showDetailedInfo = true
            )
            
            AlertLevel.WARNING -> NotificationConfig(
                enabled = true,
                showActions = true,
                autoCancel = true,
                showBigTextStyle = true,
                showDetailedInfo = false
            )
            
            AlertLevel.INFO -> NotificationConfig(
                enabled = true,
                showActions = false,
                autoCancel = true,
                showBigTextStyle = false,
                showDetailedInfo = false
            )
        }
    }
    
    /**
     * 记录通知历史
     */
    private fun recordNotification(
        type: AlertType,
        level: AlertLevel,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val record = NotificationRecord(
            id = notificationId.toString(),
            type = type,
            level = level,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        
        synchronized(notificationHistory) {
            notificationHistory.add(record)
            
            // 保持历史大小
            while (notificationHistory.size > 100) {
                notificationHistory.removeAt(0)
            }
        }
    }
    
    // 数据类定义
    data class NotificationRecord(
        val id: String,
        val type: AlertType,
        val level: AlertLevel,
        val title: String,
        val message: String,
        val timestamp: Long
    )
    
    data class NotificationConfig(
        val enabled: Boolean = true,
        val showActions: Boolean = true,
        val autoCancel: Boolean = true,
        val showBigTextStyle: Boolean = false,
        val showDetailedInfo: Boolean = false
    )
    
    data class NotificationStatistics(
        val totalNotifications: Int,
        val recent24Hours: Int,
        val typeDistribution: Map<AlertType, Int>,
        val levelDistribution: Map<AlertLevel, Int>,
        val lastUpdated: Long
    )
    
    enum class AlertType {
        SUCCESS_RATE_LOW,
        PERFORMANCE_DEGRADED,
        ERROR_RATE_HIGH,
        RESOURCE_USAGE_HIGH,
        MEMORY_LEAK,
        CONNECTION_FAILURE,
        DATA_CORRUPTION,
        SYSTEM_ERROR,
        SYSTEM_STATUS,
        CUSTOM
    }
    
    enum class AlertLevel {
        INFO,      // 信息性通知
        WARNING,   // 警告性通知
        CRITICAL   // 严重性通知
    }
    
    enum class SystemStatus {
        HEALTHY,    // 健康
        DEGRADED,   // 降级
        UNHEALTHY,  // 不健康
        CRITICAL    // 严重
    }
    
    /**
     * 通知接收器
     */
    class NotificationReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val type = intent.getStringExtra("type")
            val level = intent.getStringExtra("level")
            val action = intent.getStringExtra("action")
            
            when (action) {
                "resolve" -> {
                    // 处理解决操作
                    Log.i(TAG, "用户选择解决通知: $type, $level")
                }
                
                "dismiss" -> {
                    // 处理忽略操作
                    Log.i(TAG, "用户选择忽略通知: $type, $level")
                }
            }
        }
    }
}