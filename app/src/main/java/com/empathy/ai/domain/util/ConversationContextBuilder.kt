package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.model.TimeFlowMarker
import com.empathy.ai.domain.model.TimestampedMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话上下文构建器
 *
 * 负责构建带时间流逝标记的对话历史，模拟人类翻看聊天记录的心理活动。
 *
 * 时间流逝标记策略：
 * - < 10分钟：Hot Session，不插入标记
 * - 10分钟 ~ 3小时：Warm Session，插入轻标记
 * - > 3小时：Cold Session，插入强标记并提示情绪变化
 * - 跨天：插入日期标记
 *
 * 【PRD-00008】身份前缀支持：
 * - 历史记录中保留原始身份前缀（【对方说】：或【我正在回复】：）
 * - 兼容无前缀的旧数据（显示为"历史"）
 *
 * 【线程安全说明】
 * 本类是 @Singleton 单例，使用 java.time.format.DateTimeFormatter 进行日期格式化。
 * DateTimeFormatter 是线程安全的，可以安全地在多线程环境中共享使用。
 */

@Singleton
class ConversationContextBuilder @Inject constructor() {

    /**
     * 日期格式化器（线程安全）
     *
     * 【重要】使用 java.time.format.DateTimeFormatter 替代 SimpleDateFormat
     * 原因：SimpleDateFormat 不是线程安全的，在 @Singleton 单例中使用会导致并发问题
     * 要求：Android API 26+ (Android 8.0+)，本项目 minSdk=24 需要启用 coreLibraryDesugaring
     */
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    /**
     * 构建带时间流逝标记的对话历史
     *
     * 【PRD-00008】身份前缀处理：
     * - 历史记录中保留原始身份前缀（【对方说】：或【我正在回复】：）
     * - 格式：[历史记录 - HH:mm]: 【对方说】：xxx
     * - 兼容无前缀的旧数据
     *
     * @param messages 按时间正序排列的消息列表
     * @param config 上下文配置
     * @return 格式化后的对话历史字符串
     */
    fun buildHistoryContext(
        messages: List<TimestampedMessage>,
        config: ConversationContextConfig = ConversationContextConfig()
    ): String {
        if (messages.isEmpty()) return ""

        return buildString {
            appendLine("【历史对话】(最近${messages.size}条)")

            var previousTimestamp: Long? = null
            var previousDate: String? = null

            messages.forEach { message ->
                // 计算时间标记
                val marker = calculateTimeMarker(
                    previousTimestamp = previousTimestamp,
                    currentTimestamp = message.timestamp,
                    previousDate = previousDate,
                    config = config
                )

                // 插入时间标记（如果有）
                val markerStr = marker.toDisplayString()
                if (markerStr.isNotEmpty()) {
                    appendLine(markerStr)
                }

                // 【PRD-00008】直接使用原始内容（已包含身份前缀）
                // 格式：[历史记录 - HH:mm]: 【对方说】：xxx 或 [历史记录 - HH:mm]: xxx（旧数据）
                val timeStr = message.getFormattedTime()
                appendLine("[历史记录 - $timeStr]: ${message.content}")

                // 更新前一条消息的信息
                previousTimestamp = message.timestamp
                previousDate = message.getFormattedDate()
            }
        }.trimEnd()
    }

    /**
     * 计算时间流逝标记
     *
     * @param previousTimestamp 上一条消息的时间戳
     * @param currentTimestamp 当前消息的时间戳
     * @param previousDate 上一条消息的日期
     * @param config 配置参数
     * @return 时间流逝标记
     */
    fun calculateTimeMarker(
        previousTimestamp: Long?,
        currentTimestamp: Long,
        previousDate: String?,
        config: ConversationContextConfig
    ): TimeFlowMarker {
        // 第一条消息不需要标记
        if (previousTimestamp == null) return TimeFlowMarker.None

        val currentDate = formatDate(currentTimestamp)

        // 优先检查是否跨天
        if (previousDate != null && previousDate != currentDate) {
            return TimeFlowMarker.DateChange(currentDate)
        }

        // 计算时间间隔
        val gap = currentTimestamp - previousTimestamp

        return when {
            // 热对话：< 10分钟，不插入标记
            gap < config.hotSessionThreshold -> TimeFlowMarker.None

            // 温对话：10分钟 ~ 3小时，插入轻标记
            gap < config.warmSessionThreshold -> {
                val minutes = (gap / (60 * 1000)).toInt()
                TimeFlowMarker.ShortGap(minutes)
            }

            // 冷对话：> 3小时，插入强标记
            else -> {
                val hours = (gap / (60 * 60 * 1000)).toInt()
                TimeFlowMarker.LongGap(hours)
            }
        }
    }

    /**
     * 格式化时间戳为日期字符串
     *
     * 使用线程安全的 DateTimeFormatter
     */
    private fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Instant.ofEpochMilli(timestamp))
    }
}
