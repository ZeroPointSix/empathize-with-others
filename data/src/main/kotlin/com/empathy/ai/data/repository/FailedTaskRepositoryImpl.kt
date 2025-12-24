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
 * 失败任务仓库实现类
 *
 * 负责失败任务的数据访问
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
