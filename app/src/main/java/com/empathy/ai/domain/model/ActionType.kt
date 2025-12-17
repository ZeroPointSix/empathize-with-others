package com.empathy.ai.domain.model

/**
 * 悬浮窗操作类型
 *
 * 定义用户可以通过悬浮窗触发的操作
 *
 * @property displayName 显示名称
 * @property icon 图标（Emoji）
 * @property identityPrefix 身份前缀
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
enum class ActionType(
    val displayName: String,
    val icon: String,
    val identityPrefix: String
) {
    /**
     * 帮我分析 - 分析对方说的话
     * 调用 AnalyzeChatUseCase 进行聊天分析
     */
    ANALYZE(
        displayName = "帮我分析",
        icon = "🔍",
        identityPrefix = "【对方说】"
    ),

    /**
     * 帮我润色 - 优化我要说的话
     * 调用 PolishDraftUseCase 进行草稿润色
     */
    POLISH(
        displayName = "帮我润色",
        icon = "✍️",
        identityPrefix = "【我正在回复】"
    ),

    /**
     * 帮我回复 - 根据对方的话生成回复
     * 调用 GenerateReplyUseCase 生成回复建议
     */
    REPLY(
        displayName = "帮我回复",
        icon = "💬",
        identityPrefix = "【对方说】"
    ),

    /**
     * 帮我检查
     * 调用 CheckDraftUseCase 进行安全检查
     *
     * @deprecated 使用 POLISH 替代，风险检查已合并到润色功能
     */
    @Deprecated("使用 POLISH 替代，风险检查已合并到润色功能")
    CHECK(
        displayName = "帮我检查",
        icon = "⚠️",
        identityPrefix = "【我正在回复】"
    );

    companion object {
        /**
         * 获取默认操作类型
         */
        fun default(): ActionType = ANALYZE

        /**
         * 获取有效的操作类型（排除废弃的 CHECK）
         */
        fun validTypes(): List<ActionType> = listOf(ANALYZE, POLISH, REPLY)
    }
}
