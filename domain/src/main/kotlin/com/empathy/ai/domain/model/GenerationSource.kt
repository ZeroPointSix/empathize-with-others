package com.empathy.ai.domain.model

/**
 * 总结生成来源枚举
 *
 * 标识总结是自动生成还是手动触发，用于区分每日定时任务和用户手动触发的总结。
 *
 * 业务背景:
 * - 自动生成：由后台定时任务触发，用于每日回顾
 * - 手动触发：用户主动选择日期范围生成总结
 *
 * 设计决策 (TDD-00026):
 * - 使用枚举区分来源，便于统计和展示
 * - displayName用于UI显示，icon用于视觉标识
 *
 * @property displayName 来源显示名称
 * @property icon 来源图标
 * @see SummaryType 总结类型枚举
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
