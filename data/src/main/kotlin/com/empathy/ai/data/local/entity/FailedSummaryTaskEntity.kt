package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.empathy.ai.domain.model.FailedSummaryTask

/**
 * 失败总结任务数据库实体
 *
 * 存储失败的总结任务，用于后续重试
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
