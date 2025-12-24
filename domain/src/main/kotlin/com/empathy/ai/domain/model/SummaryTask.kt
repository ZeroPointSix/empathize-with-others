package com.empathy.ai.domain.model

import java.util.UUID

/**
 * 总结任务模型
 *
 * 表示一个手动触发的总结任务，包含任务的完整生命周期状态
 *
 * @property id 任务唯一ID
 * @property contactId 联系人ID
 * @property startDate 开始日期，格式: "yyyy-MM-dd"
 * @property endDate 结束日期，格式: "yyyy-MM-dd"
 * @property status 任务状态
 * @property progress 进度 (0.0 - 1.0)
 * @property currentStep 当前步骤描述
 * @property createdAt 创建时间戳
 * @property startedAt 开始执行时间戳
 * @property completedAt 完成时间戳
 * @property error 错误信息
 * @property conflictResolution 冲突处理方式
 */
data class SummaryTask(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val startDate: String,
    val endDate: String,
    val status: SummaryTaskStatus = SummaryTaskStatus.IDLE,
    val progress: Float = 0f,
    val currentStep: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val error: SummaryError? = null,
    val conflictResolution: ConflictResolution? = null
) {
    init {
        require(contactId.isNotBlank()) { "contactId不能为空" }
        require(startDate.matches(DATE_PATTERN)) {
            "startDate格式必须为yyyy-MM-dd"
        }
        require(endDate.matches(DATE_PATTERN)) {
            "endDate格式必须为yyyy-MM-dd"
        }
        require(progress in 0f..1f) { "progress必须在0到1之间" }
    }

    /**
     * 获取日期范围对象
     */
    fun getDateRange(): DateRange = DateRange(startDate, endDate)

    /**
     * 计算日期范围天数
     */
    fun getDayCount(): Int = getDateRange().getDayCount()

    /**
     * 更新进度
     *
     * @param newProgress 新进度值
     * @param step 当前步骤描述
     */
    fun withProgress(newProgress: Float, step: String): SummaryTask {
        return copy(
            progress = newProgress.coerceIn(0f, 1f),
            currentStep = step
        )
    }

    /**
     * 更新状态
     *
     * @param newStatus 新状态
     */
    fun withStatus(newStatus: SummaryTaskStatus): SummaryTask {
        return copy(status = newStatus)
    }

    /**
     * 标记为开始
     */
    fun markStarted(): SummaryTask {
        return copy(
            status = SummaryTaskStatus.FETCHING_DATA,
            startedAt = System.currentTimeMillis(),
            currentStep = "正在获取对话记录..."
        )
    }

    /**
     * 标记为成功
     */
    fun markSuccess(): SummaryTask {
        return copy(
            status = SummaryTaskStatus.SUCCESS,
            progress = 1f,
            currentStep = "完成",
            completedAt = System.currentTimeMillis()
        )
    }

    /**
     * 标记为失败
     *
     * @param error 错误信息
     */
    fun markFailed(error: SummaryError): SummaryTask {
        return copy(
            status = SummaryTaskStatus.FAILED,
            error = error,
            completedAt = System.currentTimeMillis()
        )
    }

    /**
     * 标记为取消
     */
    fun markCancelled(): SummaryTask {
        return copy(
            status = SummaryTaskStatus.CANCELLED,
            error = SummaryError.Cancelled,
            completedAt = System.currentTimeMillis()
        )
    }

    /**
     * 获取执行时长（毫秒）
     *
     * @return 执行时长，如果未开始则返回null
     */
    fun getDuration(): Long? {
        val start = startedAt ?: return null
        val end = completedAt ?: System.currentTimeMillis()
        return end - start
    }

    /**
     * 是否可以重试
     */
    fun canRetry(): Boolean {
        return status == SummaryTaskStatus.FAILED && error?.isRetryable() == true
    }

    companion object {
        private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")

        /**
         * 创建新任务
         *
         * @param contactId 联系人ID
         * @param dateRange 日期范围
         */
        fun create(contactId: String, dateRange: DateRange): SummaryTask {
            return SummaryTask(
                contactId = contactId,
                startDate = dateRange.startDate,
                endDate = dateRange.endDate
            )
        }
    }
}
