package com.empathy.ai.domain.model

/**
 * 失败的总结任务领域模型
 *
 * 用于表示总结任务失败的记录，支持重试机制。
 * 此模型替代Data层的FailedSummaryTaskEntity，确保Domain层不依赖Data层。
 *
 * @property id 任务ID
 * @property contactId 联系人ID
 * @property summaryDate 总结日期（格式：yyyy-MM-dd）
 * @property failureReason 失败原因
 * @property retryCount 重试次数
 * @property failedAt 首次失败时间戳（毫秒）
 * @property lastRetryAt 最后重试时间戳（毫秒），null表示尚未重试
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
data class FailedSummaryTask(
    val id: Long,
    val contactId: String,
    val summaryDate: String,
    val failureReason: String,
    val retryCount: Int,
    val failedAt: Long,
    val lastRetryAt: Long?
) {
    companion object {
        /**
         * 最大重试次数
         */
        const val MAX_RETRY_COUNT = 3

        /**
         * 创建新的失败任务
         *
         * @param contactId 联系人ID
         * @param summaryDate 总结日期
         * @param failureReason 失败原因
         * @return 新的失败任务实例
         */
        fun create(
            contactId: String,
            summaryDate: String,
            failureReason: String
        ): FailedSummaryTask = FailedSummaryTask(
            id = 0L, // 由数据库自动生成
            contactId = contactId,
            summaryDate = summaryDate,
            failureReason = failureReason,
            retryCount = 0,
            failedAt = System.currentTimeMillis(),
            lastRetryAt = null
        )
    }

    /**
     * 检查是否可以重试
     *
     * @return 如果重试次数小于最大重试次数，返回true
     */
    fun canRetry(): Boolean = retryCount < MAX_RETRY_COUNT

    /**
     * 检查是否已放弃（超过最大重试次数）
     *
     * @return 如果重试次数达到或超过最大重试次数，返回true
     */
    fun isAbandoned(): Boolean = retryCount >= MAX_RETRY_COUNT
}
