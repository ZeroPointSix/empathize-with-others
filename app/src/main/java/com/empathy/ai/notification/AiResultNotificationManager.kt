package com.empathy.ai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.empathy.ai.R
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.service.FloatingWindowService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI结果通知管理器
 *
 * 负责在悬浮球最小化状态下，AI处理完成后发送系统通知
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 * @see PRD-00010 悬浮球状态指示与拖动功能需求
 */
@Singleton
class AiResultNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AiResultNotification"

        /** 通知渠道ID */
        const val CHANNEL_ID = "empathy_ai_result"

        /** 通知渠道名称 */
        private const val CHANNEL_NAME = "AI处理结果"

        /** 通知渠道描述 */
        private const val CHANNEL_DESCRIPTION = "AI分析、润色、回复的处理结果通知"

        /** 通知ID */
        const val NOTIFICATION_ID = 1001

        /** 展开悬浮窗的Action */
        const val ACTION_EXPAND_FROM_NOTIFICATION = "com.empathy.ai.ACTION_EXPAND_FROM_NOTIFICATION"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道
     *
     * Android 8.0+ 需要创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                // 不震动
                enableVibration(false)
                // 不闪灯
                enableLights(false)
            }

            notificationManager.createNotificationChannel(channel)
            android.util.Log.d(TAG, "通知渠道已创建: $CHANNEL_ID")
        }
    }

    /**
     * 发送成功通知
     *
     * @param taskType 任务类型
     */
    fun notifySuccess(taskType: ActionType) {
        val content = when (taskType) {
            ActionType.ANALYZE -> "分析完成，点击查看结果"
            ActionType.POLISH -> "润色完成，点击查看结果"
            ActionType.REPLY -> "回复已生成，点击查看"
            ActionType.CHECK -> "检查完成，点击查看结果"
            ActionType.KNOWLEDGE -> "知识查询完成，点击查看"
        }

        showNotification(
            title = "共情AI",
            content = content,
            isError = false
        )

        android.util.Log.d(TAG, "发送成功通知: $taskType")
    }

    /**
     * 发送失败通知
     *
     * @param errorMessage 错误消息（可选）
     */
    fun notifyError(errorMessage: String? = null) {
        val content = errorMessage ?: "处理失败，点击重试"

        showNotification(
            title = "共情AI",
            content = content,
            isError = true
        )

        android.util.Log.d(TAG, "发送失败通知: $errorMessage")
    }

    /**
     * 显示通知
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param isError 是否为错误通知
     */
    private fun showNotification(title: String, content: String, isError: Boolean) {
        // 创建点击Intent
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = ACTION_EXPAND_FROM_NOTIFICATION
        }

        val pendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知 - BUG-00041修复：使用单色通知图标
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        android.util.Log.d(TAG, "通知已取消")
    }

    /**
     * 检查通知权限
     *
     * @return true 如果有通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }
}
