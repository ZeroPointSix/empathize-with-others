package com.empathy.ai.domain.util

/**
 * 数据清理配置
 *
 * 集中管理数据清理相关的配置参数，支持动态调整
 *
 * @property retentionDays 数据保留天数，超过此天数的数据将被清理
 * @property checkIntervalDays 清理检查间隔天数
 * @property maxRetryCount 失败任务最大重试次数
 * @property failedTaskRetentionDays 失败任务保留天数
 */
data class CleanupConfig(
    val retentionDays: Int = MemoryConstants.DATA_RETENTION_DAYS,
    val checkIntervalDays: Int = MemoryConstants.CLEANUP_INTERVAL_DAYS,
    val maxRetryCount: Int = MemoryConstants.MAX_TASK_RETRIES,
    val failedTaskRetentionDays: Int = MemoryConstants.MAX_RETRY_DAYS
) {
    /**
     * 计算数据保留阈值时间戳
     */
    fun getRetentionThreshold(now: Long = System.currentTimeMillis()): Long {
        return now - retentionDays * MemoryConstants.ONE_DAY_MILLIS
    }

    /**
     * 计算失败任务保留阈值时间戳
     */
    fun getFailedTaskThreshold(now: Long = System.currentTimeMillis()): Long {
        return now - failedTaskRetentionDays * MemoryConstants.ONE_DAY_MILLIS
    }

    /**
     * 检查是否应该执行清理
     */
    fun shouldCleanup(lastCleanupDate: String, currentDate: String): Boolean {
        if (lastCleanupDate.isBlank()) {
            return true // 从未清理过
        }

        return try {
            val lastTimestamp = DateUtils.parseDate(lastCleanupDate)
            val currentTimestamp = DateUtils.parseDate(currentDate)
            val daysDiff = (currentTimestamp - lastTimestamp) / MemoryConstants.ONE_DAY_MILLIS
            daysDiff >= checkIntervalDays
        } catch (e: Exception) {
            true // 解析失败时执行清理
        }
    }

    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = CleanupConfig()

        /**
         * 激进清理配置（保留30天，每3天检查）
         */
        val AGGRESSIVE = CleanupConfig(
            retentionDays = 30,
            checkIntervalDays = 3,
            failedTaskRetentionDays = 3
        )

        /**
         * 保守清理配置（保留180天，每14天检查）
         */
        val CONSERVATIVE = CleanupConfig(
            retentionDays = 180,
            checkIntervalDays = 14,
            failedTaskRetentionDays = 14
        )
    }
}
