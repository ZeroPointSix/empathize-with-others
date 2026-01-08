package com.empathy.ai.domain.model

/**
 * 系统提示词场景枚举
 *
 * 定义可编辑的5个核心AI场景
 * 
 * > 根据PRD-00033需求，系统支持5个核心场景
 * > CHECK和EXTRACT场景已废弃（TD-00015）
 *
 * @property displayName 显示名称
 * @property description 场景描述
 * @property icon 场景图标（Emoji）
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
enum class SystemPromptScene(
    val displayName: String,
    val description: String,
    val icon: String
) {
    /**
     * 聊天分析场景
     */
    ANALYZE(
        displayName = "聊天分析",
        description = "分析对方说的话，提供沟通建议",
        icon = "🔍"
    ),

    /**
     * 润色优化场景（含风险检查）
     */
    POLISH(
        displayName = "润色优化",
        description = "优化用户草稿，使表达更得体（含风险检查）",
        icon = "✍️"
    ),

    /**
     * 生成回复场景
     */
    REPLY(
        displayName = "生成回复",
        description = "根据对方消息生成合适的回复",
        icon = "💬"
    ),

    /**
     * 每日总结场景
     */
    SUMMARY(
        displayName = "每日总结",
        description = "生成每日对话总结",
        icon = "📊"
    ),

    /**
     * AI军师场景
     */
    AI_ADVISOR(
        displayName = "AI军师",
        description = "AI军师对话模式，提供深度策略分析",
        icon = "🎯"
    );

    companion object {
        /**
         * 获取所有场景列表
         */
        fun getAllScenes(): List<SystemPromptScene> = entries.toList()
    }
}
