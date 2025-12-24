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
 */
interface CleanupPreferences {
    fun getLastCleanupDate(): String
    fun setLastCleanupDate(date: String)
    fun getCurrentDateString(): String
}

/**
 * 数据清理管理器
 * 
 * 负责定期清理过期的对话记录、总结和失败任务
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
     */
    suspend fun performCleanup(): CleanupResult {
        return try {
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

            // 清理对话记录
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
     */
    data class CleanupResult(
        val conversationsDeleted: Int = 0,
        val summariesDeleted: Int = 0,
        val failedTasksDeleted: Int = 0,
        val success: Boolean = true,
        val errorMessage: String? = null
    ) {
        val totalDeleted: Int
            get() = conversationsDeleted + summariesDeleted + failedTasksDeleted
    }
}
