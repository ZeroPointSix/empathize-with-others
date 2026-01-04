package com.empathy.ai.data.repository

import android.util.Log
import com.empathy.ai.data.local.dao.ConversationLogDao
import com.empathy.ai.data.local.entity.ConversationLogEntity
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConversationRepositoryImpl 实现了对话记录的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00003)联系人画像记忆系统的短期记忆层
 *   - 存储原始对话日志，作为AI分析的素材来源
 *   - 支持标记已总结状态，避免重复处理
 *   - 响应式Flow查询，UI自动更新
 *
 * 【设计决策】(TDD-00003)
 *   - 使用Room数据库 + Flow实现响应式查询
 *   - 自动清理已总结的历史日志，释放存储空间
 *   - 支持追踪用户修改：originalUserInput记录原始输入
 *
 * 【关键逻辑】
 *   - cleanupOldSummarizedLogs：惰性删除已总结的旧日志
 *   - getConversationsByContactFlow：联系人对话变更时自动通知UI
 *   - updateUserInputWithTracking：修改追踪，防止无限循环更新
 *
 * 【任务追踪】
 *   - FD-00003/Task-001: 对话记录自动存储
 *   - FD-00011/Task-002: 手动触发AI总结功能
 */
@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationLogDao
) : ConversationRepository {

    override suspend fun saveUserInput(
        contactId: String,
        userInput: String,
        timestamp: Long
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val entity = ConversationLogEntity(
                contactId = contactId,
                userInput = userInput,
                aiResponse = null,
                timestamp = timestamp,
                isSummarized = false
            )
            val id = dao.insert(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAiResponse(
        logId: Long,
        aiResponse: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateAiResponse(logId, aiResponse)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnsummarizedLogs(
        sinceTimestamp: Long
    ): Result<List<ConversationLog>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getUnsummarizedLogs(sinceTimestamp)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLogsByContactAndDate(
        contactId: String,
        date: String
    ): Result<List<ConversationLog>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getLogsByContactAndDate(contactId, date)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsSummarized(
        logIds: List<Long>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.markAsSummarized(logIds)
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

    /**
     * cleanupOldSummarizedLogs 清理历史已总结对话记录
     *
     * 【策略】采用惰性删除策略，不使用定时任务
     * 权衡(TDD-00003)：牺牲少量读取性能，换取数据库写入压力的降低
     *
     * 【业务规则】(PRD-00011/US-002)
     *   - 只清理已总结的对话记录，保留未总结的供后续处理
     *   - 防止数据库无限增长，同时不影响AI总结功能的数据完整性
     */
    override suspend fun cleanupOldSummarizedLogs(
        beforeTimestamp: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.cleanupOldSummarizedLogs(beforeTimestamp)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConversationsByContact(contactId: String): Result<List<ConversationLog>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getConversationsByContact(contactId)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getConversationsByContactFlow(contactId: String): Flow<List<ConversationLog>> {
        return dao.getConversationsByContactFlow(contactId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateUserInput(
        logId: Long,
        userInput: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateUserInput(logId, userInput)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(
        logId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(logId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentConversations(
        contactId: String,
        limit: Int
    ): Result<List<ConversationLog>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getRecentConversations(contactId, limit)
            Log.d("ConversationRepo", "查询历史: contactId=$contactId, limit=$limit, 结果数=${entities.size}")
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e("ConversationRepo", "查询历史失败: contactId=$contactId", e)
            Result.failure(e)
        }
    }

    override suspend fun getById(logId: Long): ConversationLog? = withContext(Dispatchers.IO) {
        dao.getById(logId)?.toDomain()
    }

    override suspend fun updateUserInputWithTracking(
        logId: Long,
        newUserInput: String,
        modifiedTime: Long,
        originalInput: String
    ): Int = withContext(Dispatchers.IO) {
        dao.updateUserInputWithTracking(logId, newUserInput, modifiedTime, originalInput)
    }

    private fun ConversationLogEntity.toDomain(): ConversationLog {
        return ConversationLog(
            id = id,
            contactId = contactId,
            userInput = userInput,
            aiResponse = aiResponse,
            timestamp = timestamp,
            isSummarized = isSummarized,
            isUserModified = isUserModified,
            lastModifiedTime = if (lastModifiedTime > 0) lastModifiedTime else timestamp,
            originalUserInput = originalUserInput
        )
    }
}
