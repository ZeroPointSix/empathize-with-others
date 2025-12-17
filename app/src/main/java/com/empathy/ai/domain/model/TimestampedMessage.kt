package com.empathy.ai.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 带时间戳的对话消息
 *
 * 用于构建带时间流逝标记的上下文
 *
 * @property content 消息内容
 * @property timestamp 消息时间戳（毫秒）
 * @property sender 消息发送者
 */
data class TimestampedMessage(
    val content: String,
    val timestamp: Long,
    val sender: MessageSender
) {
    init {
        require(content.isNotBlank()) { "消息内容不能为空" }
        require(timestamp > 0) { "时间戳必须大于0" }
    }

    companion object {
        /**
         * 时间格式化器（线程安全）
         * 格式: HH:mm
         */
        private val timeFormatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        /**
         * 日期格式化器（线程安全）
         * 格式: yyyy-MM-dd
         */
        private val dateFormatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        /**
         * 获取发送者的显示前缀
         *
         * @param sender 消息发送者
         * @return 显示前缀
         */
        fun getSenderPrefix(sender: MessageSender): String {
            return when (sender) {
                MessageSender.ME -> "[我]"
                MessageSender.THEM -> "[对方]"
            }
        }
    }

    /**
     * 获取格式化的时间字符串 (HH:mm)
     */
    fun getFormattedTime(): String {
        return timeFormatter.format(Instant.ofEpochMilli(timestamp))
    }

    /**
     * 获取格式化的日期字符串 (yyyy-MM-dd)
     */
    fun getFormattedDate(): String {
        return dateFormatter.format(Instant.ofEpochMilli(timestamp))
    }

    /**
     * 转换为显示格式
     *
     * @param showTime 是否显示时间
     * @return 格式化后的字符串，示例: [我]: 你好 (10:30)
     */
    fun toDisplayString(showTime: Boolean = true): String {
        val prefix = getSenderPrefix(sender)
        return if (showTime) {
            "$prefix: $content (${getFormattedTime()})"
        } else {
            "$prefix: $content"
        }
    }
}
