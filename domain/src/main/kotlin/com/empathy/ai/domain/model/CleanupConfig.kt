package com.empathy.ai.domain.model

/**
 * 数据清理配置
 *
 * 定义数据清理的策略参数
 */
data class CleanupConfig(
    /** 数据保留天数（默认90天） */
    val retentionDays: Int = 90,
    /** 清理检查间隔天数（默认7天） */
    val checkIntervalDays: Int = 7,
    /** 失败任务保留天数（默认30天） */
    val failedTaskRetentionDays: Int = 30
) {
    companion object {
        /** 默认配置 */
        val DEFAULT = CleanupConfig()
    }
}
