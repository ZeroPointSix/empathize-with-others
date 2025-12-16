package com.empathy.ai.domain.model

/**
 * 提示词场景枚举
 *
 * 定义应用中使用AI的四种主要场景，每个场景有不同的可用变量
 *
 * @property displayName 场景显示名称
 * @property description 场景描述
 * @property availableVariables 该场景可用的变量列表
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
     */
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
    )
}
