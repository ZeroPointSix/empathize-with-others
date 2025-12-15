package com.empathy.ai.data.repository

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
 * 对话记录仓库实现类
 *
 * 负责对话记录的数据访问
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

    // ============================================================================
    // 私有映射函数
    // ============================================================================

    private fun ConversationLogEntity.toDomain(): ConversationLog {
        return ConversationLog(
            id = id,
            contactId = contactId,
            userInput = userInput,
            aiResponse = aiResponse,
            timestamp = timestamp,
            isSummarized = isSummarized
        )
    }
}
