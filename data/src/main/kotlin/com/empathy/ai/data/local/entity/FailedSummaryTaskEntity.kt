package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.empathy.ai.domain.model.FailedSummaryTask

/**
 * 失败总结任务数据库实体
 *
 * 存储失败的总结任务，用于后续重试。实现容错机制，确保数据不丢失。
 *
 * 业务背景 (PRD-00003):
 *   - AI总结可能因网络问题、服务商错误等失败
 *   - 失败任务需要记录并支持后续重试
 *   - 避免因临时错误导致数据丢失
 *
 * 设计决策 (TDD-00003):
 *   - 使用自增主键，便于任务队列管理
 *   - 创建contact_id和failed_at索引，优化重试查询
 *   - retry_count记录重试次数，避免无限重试
 *   - last_retry_at记录上次重试时间，实现指数退避
 *
 * 重试策略（容错机制）:
 *   - 最多重试3次（retry_count < 3）
 *   - 重试间隔采用指数退避策略
 *   - 7天后自动清理已失败的任务
 *   - 超过最大重试次数后标记为最终失败
 *
 * 字段说明:
 *   - id: 数据库自增ID
 *   - contact_id: 关联的联系人
 *   - summary_date: 失败的总结日期
 *   - failure_reason: 失败原因（便于问题诊断）
 *   - retry_count: 已重试次数（最多3次）
 *   - failed_at: 首次失败时间
 *   - last_retry_at: 最后重试时间
 *
 * @see FD-00003 联系人画像记忆系统设计
 */
@Entity(
    tableName = "failed_summary_tasks",
    indices = [
        Index(value = ["contact_id"]),
        Index(value = ["failed_at"])
    ]
)
data class FailedSummaryTaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "summary_date")
    val summaryDate: String,

    @ColumnInfo(name = "failure_reason")
    val failureReason: String,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "failed_at")
    val failedAt: Long,

    @ColumnInfo(name = "last_retry_at")
    val lastRetryAt: Long? = null
)

/**
 * 将Entity转换为Domain模型
 */
fun FailedSummaryTaskEntity.toDomain(): FailedSummaryTask = FailedSummaryTask(
    id = id,
    contactId = contactId,
    summaryDate = summaryDate,
    failureReason = failureReason,
    retryCount = retryCount,
    failedAt = failedAt,
    lastRetryAt = lastRetryAt
)

/**
 * 将Domain模型转换为Entity
 */
fun FailedSummaryTask.toEntity(): FailedSummaryTaskEntity = FailedSummaryTaskEntity(
    id = id,
    contactId = contactId,
    summaryDate = summaryDate,
    failureReason = failureReason,
    retryCount = retryCount,
    failedAt = failedAt,
    lastRetryAt = lastRetryAt
)
