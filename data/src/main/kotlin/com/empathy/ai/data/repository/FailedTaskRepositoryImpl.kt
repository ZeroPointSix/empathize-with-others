package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.FailedSummaryTaskDao
import com.empathy.ai.data.local.entity.FailedSummaryTaskEntity
import com.empathy.ai.domain.model.FailedSummaryTask
import com.empathy.ai.domain.repository.FailedTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FailedTaskRepositoryImpl 实现了失败任务的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00011)手动触发AI总结功能的容错机制
 *   - 记录失败的总结任务，支持后续重试
 *   - 跟踪重试次数，避免无限重试
 *   - 保存失败原因，便于问题排查
 *
 * 【设计决策】(TDD-00011)
 *   - 重试次数限制：防止同一任务无限重试
 *   - 失败原因记录：保存完整错误信息
 *   - 批量处理：getPendingTasks获取所有待重试任务
 *
 * 【关键逻辑】
 *   - saveFailedTask：保存失败任务，初始化重试次数为0
 *   - getPendingTasks：获取所有待重试任务（未达到最大重试次数）
 *   - incrementRetryCount：增加重试次数，记录最后失败时间
 *
 * 【任务追踪】
 *   - FD-00011/Task-003: 失败任务记录和重试机制
 */
@Singleton
class FailedTaskRepositoryImpl @Inject constructor(
    private val dao: FailedSummaryTaskDao
) : FailedTaskRepository {

    override suspend fun saveFailedTask(
        contactId: String,
        summaryDate: String,
        failureReason: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val entity = FailedSummaryTaskEntity(
                contactId = contactId,
                summaryDate = summaryDate,
                failureReason = failureReason,
                retryCount = 0,
                failedAt = System.currentTimeMillis()
            )
            val id = dao.insert(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingTasks(): Result<List<FailedSummaryTask>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = dao.getPendingTasks()
                val tasks = entities.map { entityToDomain(it) }
                Result.success(tasks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun incrementRetryCount(
        taskId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tasks = dao.getPendingTasks()
            val task = tasks.find { it.id == taskId }
            if (task != null) {
                val updated = task.copy(
                    retryCount = task.retryCount + 1,
                    lastRetryAt = System.currentTimeMillis()
                )
                dao.update(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(
        taskId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByContactId(
        contactId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.deleteByContactId(contactId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupOldTasks(
        beforeTimestamp: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.cleanupOldTasks(beforeTimestamp)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupAbandonedTasks(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.cleanupAbandonedTasks()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun entityToDomain(entity: FailedSummaryTaskEntity): FailedSummaryTask {
        return FailedSummaryTask(
            id = entity.id,
            contactId = entity.contactId,
            summaryDate = entity.summaryDate,
            failureReason = entity.failureReason,
            retryCount = entity.retryCount,
            failedAt = entity.failedAt,
            lastRetryAt = entity.lastRetryAt
        )
    }
}
