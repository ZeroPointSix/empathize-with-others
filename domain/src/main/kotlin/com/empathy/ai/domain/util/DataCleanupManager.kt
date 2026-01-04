package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.CleanupConfig
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.repository.FailedTaskRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据清理偏好设置接口
 *
 * 用于解耦DataCleanupManager对具体实现的依赖
 * 实现类在data层提供（使用SharedPreferences）
 */
interface CleanupPreferences {
    fun getLastCleanupDate(): String
    fun setLastCleanupDate(date: String)
    fun getCurrentDateString(): String
}

/**
 * 数据清理管理器
 *
 * 负责定期清理过期的对话记录、总结和失败任务。
 *
 * 业务背景 (PRD-00003):
 * - 每日总结功能会产生大量历史数据
 * - 需要定期清理过期数据以控制存储空间
 *
 * 设计决策:
 * - 采用惰性清理策略（应用启动时检查）
 * - 避免使用定时任务，减少系统资源占用
 *
 * 清理策略:
 * - 对话记录：已总结且超过保留期（默认90天）
 * - 每日总结：超过保留期
 * - 失败任务：超过重试期（默认7天）
 */
@Singleton
class DataCleanupManager @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val failedTaskRepository: FailedTaskRepository,
    private val cleanupPreferences: CleanupPreferences
) {
    /**
     * 清理配置
     */
    var config: CleanupConfig = CleanupConfig.DEFAULT
        private set

    /**
     * 设置清理配置
     */
    fun setConfig(newConfig: CleanupConfig) {
        config = newConfig
    }

    /**
     * 检查并执行清理（如果需要）
     *
     * 执行策略:
     * 1. 检查距离上次清理是否超过间隔天数（默认7天）
     * 2. 如果需要清理，执行完整清理流程
     * 3. 清理成功则更新最后清理日期
     *
     * @return 清理结果，如果未执行清理则返回null
     */
    suspend fun checkAndCleanup(): CleanupResult? {
        val lastCleanupDate = cleanupPreferences.getLastCleanupDate()
        val currentDate = cleanupPreferences.getCurrentDateString()

        // 检查是否需要清理
        if (!shouldCleanup(lastCleanupDate, currentDate)) {
            return null
        }

        // 执行清理
        val result = performCleanup()

        // 更新最后清理日期
        if (result.success) {
            cleanupPreferences.setLastCleanupDate(currentDate)
        }

        return result
    }

    /**
     * 执行清理操作
     *
     * 清理项目及优先级:
     * 1. 清理已总结的旧对话记录（最早清理）
     * 2. 清理旧的每日总结
     * 3. 清理超过重试期的失败任务
     *
     * @return 清理结果统计
     */
    suspend fun performCleanup(): CleanupResult {
        return try {
            // 计算保留期限（当前时间 - 保留天数）
            val retentionThreshold = LocalDate.now().minusDays(config.retentionDays.toLong())
            val failedTaskThreshold = LocalDate.now().minusDays(config.failedTaskRetentionDays.toLong())

            // 将LocalDate转换为时间戳（毫秒）
            val retentionTimestamp = retentionThreshold.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val failedTaskTimestamp = failedTaskThreshold.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // 清理对话记录（只清理已总结的）
            val conversationsDeleted = conversationRepository
                .cleanupOldSummarizedLogs(retentionTimestamp)
                .getOrDefault(0)

            // 清理总结
            val summariesDeleted = dailySummaryRepository
                .cleanupOldSummaries(retentionTimestamp)
                .getOrDefault(0)

            // 清理失败任务
            val failedTasksDeleted = failedTaskRepository
                .cleanupOldTasks(failedTaskTimestamp)
                .getOrDefault(0)

            CleanupResult(
                conversationsDeleted = conversationsDeleted,
                summariesDeleted = summariesDeleted,
                failedTasksDeleted = failedTasksDeleted,
                success = true
            )
        } catch (e: Exception) {
            CleanupResult(
                success = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * 判断是否需要执行清理
     *
     * 条件判断:
     * - 从未执行过清理（返回true）
     * - 距离上次清理超过间隔天数（返回true）
     * - 日期解析失败（保守返回true）
     *
     * @param lastCleanupDate 上次清理日期
     * @param currentDate 当前日期
     * @return 是否需要执行清理
     */
    private fun shouldCleanup(lastCleanupDate: String, currentDate: String): Boolean {
        if (lastCleanupDate.isBlank()) {
            return true
        }

        return try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val last = LocalDate.parse(lastCleanupDate, formatter)
            val current = LocalDate.parse(currentDate, formatter)
            val daysSinceLastCleanup = ChronoUnit.DAYS.between(last, current)
            daysSinceLastCleanup >= config.checkIntervalDays
        } catch (e: Exception) {
            true
        }
    }

    /**
     * 清理结果
     *
     * @property conversationsDeleted 删除的对话记录数
     * @property summariesDeleted 删除的总结数
     * @property failedTasksDeleted 删除的失败任务数
     * @property success 是否成功
     * @property errorMessage 错误信息（如果失败）
     */
    data class CleanupResult(
        val conversationsDeleted: Int = 0,
        val summariesDeleted: Int = 0,
        val failedTasksDeleted: Int = 0,
        val success: Boolean = true,
        val errorMessage: String? = null
    ) {
        /**
         * 总删除数量
         */
        val totalDeleted: Int
            get() = conversationsDeleted + summariesDeleted + failedTasksDeleted
    }
}
