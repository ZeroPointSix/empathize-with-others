package com.empathy.ai.domain.model

/**
 * 总结生成来源枚举
 *
 * 标识总结是自动生成还是手动触发
 *
 * @property displayName 来源显示名称
 * @property icon 来源图标
 */
enum class GenerationSource(
    val displayName: String,
    val icon: String
) {
    /**
     * 自动生成（每日定时任务）
     */
    AUTO("自动", "🤖"),

    /**
     * 手动触发
     */
    MANUAL("手动", "👤")
}
