package com.empathy.ai.domain.model

/**
 * AI 模型
 *
 * 表示 AI 服务商提供的具体模型
 *
 * @property id 模型 ID（例如: "gpt-4", "deepseek-chat"）
 * @property displayName 显示名称（可选，用于 UI 展示）
 */
data class AiModel(
    val id: String,
    val displayName: String? = null
) {
    /**
     * 获取用于显示的名称
     * 如果没有设置 displayName，则使用 id
     */
    fun getDisplayText(): String = displayName ?: id
}
