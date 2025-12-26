package com.empathy.ai.presentation.ui.component.timeline

import com.empathy.ai.presentation.theme.EmotionType

/**
 * 时光轴数据项
 * 
 * 用于事实流页面的时光轴视图数据模型
 * 
 * @param id 唯一标识
 * @param emotionType 情绪类型
 * @param timestamp 时间戳显示文字
 * @param content 内容文字
 * @param aiSuggestion AI建议（可选）
 * @param isAiSummary 是否为AI总结卡片
 * @param scoreChange 关系分数变化（可选）
 * @param tags 标签列表
 * 
 * @see TDD-00020 4.3 EmotionTimelineView时光轴视图
 */
data class TimelineItem(
    val id: String,
    val emotionType: EmotionType,
    val timestamp: String,
    val content: String,
    val aiSuggestion: String? = null,
    val isAiSummary: Boolean = false,
    val scoreChange: Int? = null,
    val tags: List<String> = emptyList()
) {
    companion object {
        /**
         * 创建示例数据（用于预览和测试）
         */
        fun createSample(
            id: String = "1",
            emotionType: EmotionType = EmotionType.SWEET,
            timestamp: String = "今天 14:30",
            content: String = "今天一起去看了电影，她很开心",
            aiSuggestion: String? = null,
            isAiSummary: Boolean = false,
            scoreChange: Int? = null,
            tags: List<String> = emptyList()
        ) = TimelineItem(
            id = id,
            emotionType = emotionType,
            timestamp = timestamp,
            content = content,
            aiSuggestion = aiSuggestion,
            isAiSummary = isAiSummary,
            scoreChange = scoreChange,
            tags = tags
        )
        
        /**
         * 创建示例列表（用于预览和测试）
         */
        fun createSampleList(): List<TimelineItem> = listOf(
            TimelineItem(
                id = "1",
                emotionType = EmotionType.SWEET,
                timestamp = "今天 14:30",
                content = "今天一起去看了电影，她很开心，说下次还想一起看。",
                scoreChange = 5,
                tags = listOf("约会", "电影")
            ),
            TimelineItem(
                id = "2",
                emotionType = EmotionType.CONFLICT,
                timestamp = "昨天 20:15",
                content = "因为工作的事情有点小争执，但最后还是和好了。",
                aiSuggestion = "建议下次遇到类似情况时，先倾听对方的想法，再表达自己的观点。",
                scoreChange = -3,
                tags = listOf("工作", "沟通")
            ),
            TimelineItem(
                id = "3",
                emotionType = EmotionType.DEEP_TALK,
                timestamp = "3天前",
                content = "聊了很多关于未来的规划，感觉彼此更了解了。",
                isAiSummary = true,
                aiSuggestion = "这是一次很好的深度交流，建议定期进行这样的对话。",
                scoreChange = 8,
                tags = listOf("深谈", "未来规划")
            ),
            TimelineItem(
                id = "4",
                emotionType = EmotionType.GIFT,
                timestamp = "上周五",
                content = "送了她一束花，她很惊喜。",
                scoreChange = 10,
                tags = listOf("礼物", "惊喜")
            ),
            TimelineItem(
                id = "5",
                emotionType = EmotionType.DATE,
                timestamp = "上周三",
                content = "一起去了新开的餐厅，环境很好，菜也不错。",
                tags = listOf("约会", "美食")
            )
        )
    }
}
