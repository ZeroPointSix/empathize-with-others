package com.empathy.ai.domain.model

/**
 * 总结类型枚举
 *
 * 区分不同类型的总结，用于单日自动总结和自定义范围手动总结。
 *
 * 业务背景:
 * - DAILY: 每日定时任务自动生成，记录当天的互动分析
 * - CUSTOM_RANGE: 用户手动选择日期范围生成总结
 *
 * 设计决策:
 * - 枚举值对应不同的总结触发方式
 * - displayName用于UI显示
 *
 * @property displayName 类型显示名称
 */
enum class SummaryType(val displayName: String) {
    /**
     * 单日总结（自动生成）
     */
    DAILY("单日总结"),

    /**
     * 自定义范围总结（手动触发）
     */
    CUSTOM_RANGE("范围总结")
}
