package com.empathy.ai.domain.model

/**
 * 筛选类型枚举
 *
 * 用于事实流的快速筛选功能。
 * 此版本移除了Compose图标依赖，保持Domain层纯净。
 * 图标映射在Presentation层通过扩展函数提供。
 *
 * @property displayName 显示名称
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
enum class FilterType(val displayName: String) {
    /**
     * 显示全部
     */
    ALL("全部"),

    /**
     * 只显示AI总结
     */
    AI_SUMMARY("只看AI"),

    /**
     * 只显示冲突事件
     */
    CONFLICT("只看冲突"),

    /**
     * 只显示约会事件
     */
    DATE("只看约会"),

    /**
     * 只显示甜蜜时刻
     */
    SWEET("只看甜蜜");

    /**
     * 应用筛选条件
     *
     * @param items 要筛选的时间线项目列表
     * @return 筛选后的列表
     */
    fun apply(items: List<TimelineItem>): List<TimelineItem> {
        return when (this) {
            ALL -> items
            AI_SUMMARY -> items.filterIsInstance<TimelineItem.AiSummary>()
            CONFLICT -> items.filter { it.emotionType == EmotionType.CONFLICT }
            DATE -> items.filter { it.emotionType == EmotionType.DATE }
            SWEET -> items.filter { it.emotionType == EmotionType.SWEET }
        }
    }
}
