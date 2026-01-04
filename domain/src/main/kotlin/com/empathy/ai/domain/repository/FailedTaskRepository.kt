package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.FailedSummaryTask

/**
 * 失败任务仓储接口
 *
 * 业务背景:
 * - 每日总结失败后记录，用于后续重试
 * - 支持重试机制，最多重试3次后放弃
 * - 超过7天的失败任务自动清理
 *
 * 设计决策:
 * - 返回领域模型：使用FailedSummaryTask而非Data层Entity
 * - 重试策略：3次后放弃，避免无限重试
 * - 过期清理：7天后自动删除，释放存储空间
 *
 * 注意: 返回类型使用领域模型FailedSummaryTask，而非Data层Entity
 */
interface FailedTaskRepository {

    /**
     * 保存失败任务
     *
     * 业务规则:
     * - 记录contactId、summaryDate、failureReason
     * - 初始化retryCount为0
     * - 创建时间戳自动记录
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
     * 筛选条件: retryCount < 3 且创建时间在7天内
     *
     * @return 待重试的任务列表（领域模型）
     */
    suspend fun getPendingTasks(): Result<List<FailedSummaryTask>>

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
     * 业务规则:
     * - 删除创建时间超过7天的任务
     * - 每周自动执行一次清理
     *
     * @param beforeTimestamp 此时间之前的任务将被删除
     * @return 删除的记录数
     */
    suspend fun cleanupOldTasks(beforeTimestamp: Long): Result<Int>

    /**
     * 清理已放弃的任务（重试次数>=3）
     *
     * 用途: 清理达到最大重试次数的任务
     *
     * @return 删除的记录数
     */
    suspend fun cleanupAbandonedTasks(): Result<Int>
}
