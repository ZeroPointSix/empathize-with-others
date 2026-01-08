package com.empathy.ai.domain.model

/**
 * 提示词场景枚举
 *
 * 定义应用中使用AI的各种场景，每个场景有不同的可用变量
 *
 * @property displayName 场景显示名称
 * @property description 场景描述
 * @property availableVariables 该场景可用的变量列表
 * @property isDeprecated 是否已废弃（废弃场景保留代码但不在设置界面显示）
 * @property showInSettings 是否在设置界面显示
 *
 * @see PRD-00015 提示词设置优化需求
 * @see TDD-00015 提示词设置优化技术设计
 */
enum class PromptScene(
    val displayName: String,
    val description: String,
    val availableVariables: List<String>,
    val isDeprecated: Boolean = false,
    val showInSettings: Boolean = true
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
        ),
        isDeprecated = false,
        showInSettings = true
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
        availableVariables = listOf("contact_name", "risk_tags"),
        isDeprecated = true,
        showInSettings = false
    ),

    /**
     * 信息提取场景 - 从文本中提取关键信息
     *
     * @deprecated 暂不在设置界面展示
     */
    @Deprecated("暂不在设置界面展示")
    EXTRACT(
        displayName = "信息提取",
        description = "从文本中提取关键信息",
        availableVariables = listOf("contact_name"),
        isDeprecated = true,
        showInSettings = false
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
        ),
        isDeprecated = false,
        showInSettings = true
    ),

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
        ),
        isDeprecated = false,
        showInSettings = true
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
        ),
        isDeprecated = false,
        showInSettings = true
    ),

    /**
     * AI军师对话场景 - 多轮对话咨询，提供社交沟通建议
     *
     * PRD-00026: AI军师对话功能
     * 特点：
     * - 支持多轮对话
     * - 不返回JSON，直接返回自然语言
     * - 支持Markdown格式输出
     */
    AI_ADVISOR(
        displayName = "AI军师",
        description = "多轮对话咨询，提供社交沟通建议",
        availableVariables = listOf(
            "contact_name",
            "relationship_status",
            "risk_tags",
            "strategy_tags",
            "facts_count"
        ),
        isDeprecated = false,
        showInSettings = true
    );

    companion object {
        /**
         * 设置界面场景显示顺序
         * 按照用户使用频率和逻辑顺序排列：分析 → 润色 → 回复 → 总结 → AI军师
         */
        val SETTINGS_SCENE_ORDER = listOf(ANALYZE, POLISH, REPLY, SUMMARY, AI_ADVISOR)

        /**
         * 获取在设置界面显示的场景列表
         * @return 可在设置界面配置的场景列表
         */
        fun getSettingsScenes(): List<PromptScene> {
            return entries.filter { it.showInSettings }
        }

        /**
         * 获取活跃（非废弃）的场景列表
         * @return 活跃场景列表
         */
        fun getActiveScenes(): List<PromptScene> {
            return entries.filter { !it.isDeprecated }
        }

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
            ActionType.KNOWLEDGE -> ANALYZE  // KNOWLEDGE使用ANALYZE场景的提示词配置
            @Suppress("DEPRECATION")
            ActionType.CHECK -> POLISH  // CHECK映射到POLISH（TD-00015迁移）
        }
    }
}
