package com.empathy.ai.domain.repository

import com.empathy.ai.data.local.entity.FailedSummaryTaskEntity

/**
 * 失败任务仓库接口
 *
 * 定义失败任务的数据访问接口
 */
interface FailedTaskRepository {

    /**
     * 保存失败任务
     *
     * @param contactId 联系人ID
     * @param summaryDate 总结日期
     * @param failureReason 失败原因
     * @return 保存的任务ID
     */
    suspend fun saveFailedTask(
        contactId: String,
        summaryDate: String,
        failureReason: String
    ): Result<Long>

    /**
     * 获取所有待重试的任务
     *
     * @return 待重试的任务列表
     */
    suspend fun getPendingTasks(): Result<List<FailedSummaryTaskEntity>>

    /**
     * 更新重试次数
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    suspend fun incrementRetryCount(taskId: Long): Result<Unit>

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    suspend fun deleteTask(taskId: Long): Result<Unit>

    /**
     * 删除指定联系人的所有失败任务
     *
     * @param contactId 联系人ID
     * @return 删除的记录数
     */
    suspend fun deleteByContactId(contactId: String): Result<Int>

    /**
     * 清理过期的失败任务
     *
     * @param beforeTimestamp 此时间之前的任务将被删除
     * @return 删除的记录数
     */
    suspend fun cleanupOldTasks(beforeTimestamp: Long): Result<Int>

    /**
     * 清理已放弃的任务（重试次数>=3）
     *
     * @return 删除的记录数
     */
    suspend fun cleanupAbandonedTasks(): Result<Int>
}
