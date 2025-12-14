package com.empathy.ai.domain.util

import android.util.Log
import com.empathy.ai.data.local.MemoryPreferences
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.repository.FailedTaskRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据清理管理器
 *
 * 负责清理过期的对话记录、总结记录和失败任务
 * 使用CleanupConfig配置清理策略，支持动态调整
 */
@Singleton
class DataCleanupManager @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val failedTaskRepository: FailedTaskRepository,
    private val memoryPreferences: MemoryPreferences
) {
    companion object {
        private const val TAG = "DataCleanupManager"
    }

    /**
     * 清理配置，可通过setConfig方法动态调整
     */
    @Volatile
    var config: CleanupConfig = CleanupConfig.DEFAULT
        private set

    /**
     * 设置清理配置
     */
    fun setConfig(newConfig: CleanupConfig) {
        config = newConfig
        Log.i(TAG, "清理配置已更新: 保留${newConfig.retentionDays}天, 检查间隔${newConfig.checkIntervalDays}天")
    }

    /**
     * 清理结果统计
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

    /**
     * 检查并执行清理
     *
     * 根据配置的检查间隔判断是否需要执行清理
     *
     * @return 清理结果，如果未执行清理则返回null
     */
    suspend fun checkAndCleanup(): CleanupResult? {
        val lastCleanupDate = memoryPreferences.getLastCleanupDate()
        val currentDate = memoryPreferences.getCurrentDateString()

        // 使用配置检查是否需要清理
        if (!config.shouldCleanup(lastCleanupDate, currentDate)) {
            Log.d(TAG, "跳过清理：距离上次清理未超过${config.checkIntervalDays}天")
            return null
        }

        Log.i(TAG, "开始执行数据清理（保留${config.retentionDays}天数据）...")
        return performCleanup().also { result ->
            if (result.success) {
                memoryPreferences.setLastCleanupDate(currentDate)
                Log.i(TAG, "数据清理完成：共删除${result.totalDeleted}条记录")
            } else {
                Log.e(TAG, "数据清理失败：${result.errorMessage}")
            }
        }
    }

    /**
     * 执行清理操作
     *
     * 根据配置删除过期的对话记录、总结记录和失败任务
     */
    suspend fun performCleanup(): CleanupResult {
        val now = System.currentTimeMillis()
        val retentionThreshold = config.getRetentionThreshold(now)
        val failedTaskThreshold = config.getFailedTaskThreshold(now)

        var conversationsDeleted = 0
        var summariesDeleted = 0
        var failedTasksDeleted = 0

        try {
            // 清理过期对话记录
            conversationRepository.cleanupOldSummarizedLogs(retentionThreshold)
                .onSuccess { count ->
                    conversationsDeleted = count
                    Log.d(TAG, "删除${count}条过期对话记录")
                }
                .onFailure { e ->
                    Log.w(TAG, "清理对话记录失败: ${e.message}")
                }

            // 清理过期总结记录
            dailySummaryRepository.cleanupOldSummaries(retentionThreshold)
                .onSuccess { count ->
                    summariesDeleted = count
                    Log.d(TAG, "删除${count}条过期总结记录")
                }
                .onFailure { e ->
                    Log.w(TAG, "清理总结记录失败: ${e.message}")
                }

            // 清理过期失败任务
            failedTaskRepository.cleanupOldTasks(failedTaskThreshold)
                .onSuccess { count ->
                    failedTasksDeleted = count
                    Log.d(TAG, "删除${count}条过期失败任务")
                }
                .onFailure { e ->
                    Log.w(TAG, "清理失败任务失败: ${e.message}")
                }

            return CleanupResult(
                conversationsDeleted = conversationsDeleted,
                summariesDeleted = summariesDeleted,
                failedTasksDeleted = failedTasksDeleted,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "数据清理异常: ${e.message}", e)
            return CleanupResult(
                conversationsDeleted = conversationsDeleted,
                summariesDeleted = summariesDeleted,
                failedTasksDeleted = failedTasksDeleted,
                success = false,
                errorMessage = e.message
            )
        }
    }

}
