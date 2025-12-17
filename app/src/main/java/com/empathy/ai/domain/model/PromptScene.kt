package com.empathy.ai.domain.model

/**
 * 提示词场景枚举
 *
 * 定义应用中使用AI的各种场景，每个场景有不同的可用变量
 *
 * @property displayName 场景显示名称
 * @property description 场景描述
 * @property availableVariables 该场景可用的变量列表
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
enum class PromptScene(
    val displayName: String,
    val description: String,
    val availableVariables: List<String>
) {
    /**
     * 聊天分析场景 - 分析聊天上下文，提供沟通建议
     */
    ANALYZE(
        displayName = "聊天分析",
        description = "分析聊天上下文，提供沟通建议",
        availableVariables = listOf(
            "contact_name",
            "relationship_status",
            "risk_tags",
            "strategy_tags",
            "facts_count"
        )
    ),

    /**
     * 安全检查场景 - 检查草稿内容是否触发风险规则
     *
     * @deprecated 使用 POLISH 替代，风险检查已合并到润色功能
     */
    @Deprecated("使用 POLISH 替代，风险检查已合并到润色功能")
    CHECK(
        displayName = "安全检查",
        description = "检查草稿内容是否触发风险规则",
        availableVariables = listOf("contact_name", "risk_tags")
    ),

    /**
     * 信息提取场景 - 从文本中提取关键信息
     */
    EXTRACT(
        displayName = "信息提取",
        description = "从文本中提取关键信息",
        availableVariables = listOf("contact_name")
    ),

    /**
     * 每日总结场景 - 生成每日对话总结
     */
    SUMMARY(
        displayName = "每日总结",
        description = "生成每日对话总结",
        availableVariables = listOf(
            "contact_name",
            "relationship_status",
            "facts_count",
            "today_date"
        )
    ),

    // ==================== 新增场景（TD-00009） ====================

    /**
     * 润色优化场景 - 优化用户草稿，使表达更得体
     *
     * 替代原有的 CHECK 场景，同时包含风险检查功能
     */
    POLISH(
        displayName = "润色优化",
        description = "优化用户草稿，使表达更得体",
        availableVariables = listOf(
            "contact_name",
            "relationship_status",
            "risk_tags"
        )
    ),

    /**
     * 生成回复场景 - 根据对方消息生成合适的回复
     */
    REPLY(
        displayName = "生成回复",
        description = "根据对方消息生成合适的回复",
        availableVariables = listOf(
            "contact_name",
            "relationship_status",
            "risk_tags",
            "strategy_tags"
        )
    );

    companion object {
        /**
         * 从 ActionType 获取对应的 PromptScene
         *
         * @param actionType 操作类型
         * @return 对应的提示词场景
         */
        fun fromActionType(actionType: ActionType): PromptScene = when (actionType) {
            ActionType.ANALYZE -> ANALYZE
            ActionType.POLISH -> POLISH
            ActionType.REPLY -> REPLY
            @Suppress("DEPRECATION")
            ActionType.CHECK -> CHECK
        }
    }
}
