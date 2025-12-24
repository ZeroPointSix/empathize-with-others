package com.empathy.ai.domain.model

/**
 * 回复结果模型
 *
 * 承载回复功能的AI返回结果，包含建议的回复内容和策略说明
 *
 * @property suggestedReply 建议的回复内容（可直接复制使用）
 * @property strategyNote 策略说明（为什么这样回复）
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
data class ReplyResult(
    /**
     * 建议的回复内容（可直接复制使用）
     */
    val suggestedReply: String,

    /**
     * 策略说明（为什么这样回复）
     */
    val strategyNote: String? = null
) {
    /**
     * 获取用于复制的文本
     *
     * @return 建议的回复纯文本
     */
    fun getCopyableText(): String = suggestedReply

    /**
     * 获取用于显示的完整内容
     *
     * 如果有策略说明，会附加在回复后面
     *
     * @return 完整的显示内容
     */
    fun getDisplayContent(): String = buildString {
        append(suggestedReply)
        if (!strategyNote.isNullOrBlank()) {
            appendLine()
            appendLine()
            append("💡 策略说明：$strategyNote")
        }
    }
}
