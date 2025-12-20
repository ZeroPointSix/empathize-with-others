package com.empathy.ai.domain.model

/**
 * 总结类型枚举
 *
 * 区分不同类型的总结
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
