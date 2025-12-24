package com.empathy.ai.domain.model

/**
 * 润色结果模型
 *
 * 承载润色功能的AI返回结果，包含优化后的文本和可选的风险提示
 *
 * @property polishedText 优化后的文本（可直接复制使用）
 * @property hasRisk 是否检测到风险
 * @property riskWarning 风险提示（如果有风险）
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
data class PolishResult(
    /**
     * 优化后的文本（可直接复制使用）
     */
    val polishedText: String,

    /**
     * 是否检测到风险
     */
    val hasRisk: Boolean = false,

    /**
     * 风险提示（如果有风险）
     */
    val riskWarning: String? = null
) {
    /**
     * 获取用于复制的文本
     *
     * @return 优化后的纯文本
     */
    fun getCopyableText(): String = polishedText

    /**
     * 获取用于显示的完整内容
     *
     * 如果有风险提示，会附加在文本后面
     *
     * @return 完整的显示内容
     */
    fun getDisplayContent(): String = buildString {
        append(polishedText)
        if (hasRisk && !riskWarning.isNullOrBlank()) {
            appendLine()
            appendLine()
            append("⚠️ 风险提示：$riskWarning")
        }
    }
}
