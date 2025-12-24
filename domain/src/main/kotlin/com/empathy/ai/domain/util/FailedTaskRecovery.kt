package com.empathy.ai.domain.util

import com.empathy.ai.domain.repository.FailedTaskRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 失败任务恢复处理器
 *
 * 负责处理失败任务的恢复逻辑：
 * - 查询待重试的任务
 * - 执行重试
 * - 更新重试计数
 * - 清理已放弃的任务
 */
@Singleton
class FailedTaskRecovery @Inject constructor(
    private val failedTaskRepository: FailedTaskRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "FailedTaskRecovery"
    }

    /**
     * 恢复结果
     */
    data class RecoveryResult(
        val totalTasks: Int,
        val recoveredCount: Int,
        val abandonedCount: Int,
        val failedCount: Int
    )

    /**
     * 执行失败任务恢复
     *
     * @param retryAction 重试操作，接收contactId和date，返回是否成功
     * @return 恢复结果统计
     */
    suspend fun recover(
        retryAction: suspend (contactId: String, date: String) -> Boolean
    ): RecoveryResult {
        var recoveredCount = 0
        var abandonedCount = 0
        var failedCount = 0

        try {
            val pendingTasks = failedTaskRepository.getPendingTasks().getOrNull() 
                ?: return RecoveryResult(0, 0, 0, 0)

            for (task in pendingTasks) {
                if (task.retryCount >= MemoryConstants.MAX_TASK_RETRIES) {
                    logger.w(TAG, "任务 ${task.id} 已超过最大重试次数，放弃")
                    failedTaskRepository.deleteTask(task.id)
                    abandonedCount++
                    continue
                }

                val success = try {
                    retryAction(task.contactId, task.summaryDate)
                } catch (e: Exception) {
                    logger.e(TAG, "任务 ${task.id} 重试异常", e)
                    false
                }

                if (success) {
                    failedTaskRepository.deleteTask(task.id)
                    recoveredCount++
                    logger.d(TAG, "成功恢复任务 ${task.id}")
                } else {
                    failedTaskRepository.incrementRetryCount(task.id)
                    failedCount++
                    logger.w(TAG, "任务 ${task.id} 重试失败，已更新重试次数")
                }
            }

            return RecoveryResult(
                totalTasks = pendingTasks.size,
                recoveredCount = recoveredCount,
                abandonedCount = abandonedCount,
                failedCount = failedCount
            )
        } catch (e: Exception) {
            logger.e(TAG, "恢复失败任务时出错", e)
            return RecoveryResult(0, recoveredCount, abandonedCount, failedCount)
        }
    }

    /**
     * 保存失败任务
     *
     * @param contactId 联系人ID
     * @param summaryDate 总结日期
     * @param failureReason 失败原因
     */
    suspend fun saveFailedTask(
        contactId: String,
        summaryDate: String,
        failureReason: String
    ) {
        try {
            failedTaskRepository.saveFailedTask(contactId, summaryDate, failureReason)
            logger.d(TAG, "保存失败任务: contactId=$contactId, date=$summaryDate")
        } catch (e: Exception) {
            logger.e(TAG, "保存失败任务异常", e)
        }
    }
}
