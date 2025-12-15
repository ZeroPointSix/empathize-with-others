package com.empathy.ai.domain.model

/**
 * 情绪类型枚举
 *
 * 用于时间线节点的情绪可视化，通过Emoji传递情感信息
 * 注意：颜色映射已移至表现层SemanticColors，遵循Clean Architecture原则
 *
 * @property emoji 情绪对应的Emoji表情
 * @property displayName 显示名称
 */
enum class EmotionType(
    val emoji: String,
    val displayName: String
) {
    /**
     * 甜蜜互动
     * 表示温馨、浪漫、亲密的时刻
     */
    SWEET("❤️", "甜蜜"),
    
    /**
     * 冲突事件
     * 表示争吵、矛盾、不愉快的时刻
     */
    CONFLICT("⛈️", "冲突"),
    
    /**
     * 礼物交换
     * 表示送礼、收礼的时刻
     */
    GIFT("🎁", "礼物"),
    
    /**
     * 约会用餐
     * 表示一起吃饭、约会的时刻
     */
    DATE("🍽️", "约会"),
    
    /**
     * 深度对话
     * 表示深入交流、谈心的时刻
     */
    DEEP_TALK("💬", "深聊"),
    
    /**
     * 日常互动
     * 表示普通、中性的互动
     */
    NEUTRAL("⭕", "日常");
    
    companion object {
        /**
         * 根据文本内容推断情绪类型
         *
         * 使用简单的关键词匹配来检测情绪
         * 注意：这是一个简化的实现，实际应用中可能需要更复杂的NLP分析
         *
         * @param text 要分析的文本
         * @return 推断出的情绪类型
         */
        fun fromText(text: String): EmotionType {
            return when {
                text.contains("爱") || text.contains("喜欢") || text.contains("想你") -> SWEET
                text.contains("生气") || text.contains("吵架") || text.contains("不开心") -> CONFLICT
                text.contains("礼物") || text.contains("送") || text.contains("买") -> GIFT
                text.contains("吃饭") || text.contains("约会") || text.contains("见面") -> DATE
                text.contains("聊天") || text.contains("谈心") || text.contains("说说") -> DEEP_TALK
                else -> NEUTRAL
            }
        }
    }
}
