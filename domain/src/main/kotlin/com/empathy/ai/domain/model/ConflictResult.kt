package com.empathy.ai.domain.model

/**
 * 冲突检测结果密封类
 *
 * 表示日期范围内是否存在已有总结的冲突
 */
sealed class ConflictResult {
    /**
     * 无冲突
     */
    object NoConflict : ConflictResult()

    /**
     * 存在冲突
     *
     * @property existingSummaries 已存在的总结列表
     * @property conflictDates 冲突的日期列表
     */
    data class HasConflict(
        val existingSummaries: List<DailySummary>,
        val conflictDates: List<String>
    ) : ConflictResult() {
        /**
         * 冲突数量
         */
        val conflictCount: Int get() = conflictDates.size
    }
}

/**
 * 冲突处理方式枚举
 *
 * @property displayName 处理方式显示名称
 */
enum class ConflictResolution(val displayName: String) {
    /**
     * 覆盖现有总结
     */
    OVERWRITE("覆盖现有总结"),

    /**
     * 仅补充缺失日期
     */
    FILL_GAPS("仅补充缺失日期"),

    /**
     * 取消操作
     */
    CANCEL("取消")
}
