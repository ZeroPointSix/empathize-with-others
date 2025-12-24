package com.empathy.ai.domain.model

/**
 * 时间线项目密封类
 *
 * 表示时间线上的不同类型内容，统一管理各种事件和记录
 *
 * 设计理念：
 * - 使用密封类确保类型安全
 * - 所有子类共享基础属性（id、timestamp、emotionType）
 * - 支持多态处理，便于UI渲染
 */
sealed class TimelineItem {
    /**
     * 唯一标识符
     */
    abstract val id: String

    /**
     * 时间戳（毫秒）
     */
    abstract val timestamp: Long

    /**
     * 情绪类型
     */
    abstract val emotionType: EmotionType

    /**
     * 图文时刻
     *
     * 展示照片和简短描述，类似朋友圈动态
     *
     * @property photoUrl 图片URL
     * @property description 描述文本
     * @property tags 相关标签
     */
    data class PhotoMoment(
        override val id: String,
        override val timestamp: Long,
        override val emotionType: EmotionType,
        val photoUrl: String,
        val description: String,
        val tags: List<String> = emptyList()
    ) : TimelineItem()

    /**
     * AI总结
     *
     * 展示AI对一段时间的复盘和洞察
     *
     * @property summary 每日总结对象
     */
    data class AiSummary(
        override val id: String,
        override val timestamp: Long,
        override val emotionType: EmotionType,
        val summary: DailySummary
    ) : TimelineItem()

    /**
     * 里程碑事件
     *
     * 标记重大事件，如"第一次旅行"、"相识100天"
     *
     * @property title 标题
     * @property description 描述
     * @property icon 图标（Emoji或图标名称）
     */
    data class Milestone(
        override val id: String,
        override val timestamp: Long,
        override val emotionType: EmotionType,
        val title: String,
        val description: String,
        val icon: String
    ) : TimelineItem()

    /**
     * 对话记录
     *
     * 展示具体的对话内容
     *
     * @property log 对话记录对象
     */
    data class Conversation(
        override val id: String,
        override val timestamp: Long,
        override val emotionType: EmotionType,
        val log: ConversationLog
    ) : TimelineItem()

    /**
     * 用户添加的事实
     *
     * 展示用户手动添加的事实记录
     *
     * @property fact 事实对象
     */
    data class UserFact(
        override val id: String,
        override val timestamp: Long,
        override val emotionType: EmotionType,
        val fact: Fact
    ) : TimelineItem()
}
