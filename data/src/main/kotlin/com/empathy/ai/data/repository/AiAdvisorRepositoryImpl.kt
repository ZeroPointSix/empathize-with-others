package com.empathy.ai.data.repository

import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.data.local.dao.AiAdvisorDao
import com.empathy.ai.data.local.entity.AiAdvisorConversationEntity
import com.empathy.ai.data.local.entity.AiAdvisorSessionEntity
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI军师对话仓库实现
 *
 * 实现AiAdvisorRepository接口，提供AI军师会话和对话记录的数据访问。
 * 使用Room数据库进行本地存储。
 */
@Singleton
class AiAdvisorRepositoryImpl @Inject constructor(
    private val aiAdvisorDao: AiAdvisorDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AiAdvisorRepository {

    // ==================== 会话管理 ====================

    override suspend fun createSession(session: AiAdvisorSession): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = AiAdvisorSessionEntity.fromDomain(session)
                aiAdvisorDao.insertSession(entity)
            }
        }

    override suspend fun getSessions(contactId: String): Result<List<AiAdvisorSession>> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.getSessionsByContact(contactId).map { it.toDomain() }
            }
        }

    override suspend fun getActiveSession(contactId: String): Result<AiAdvisorSession?> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.getActiveSession(contactId)?.toDomain()
            }
        }

    override suspend fun getOrCreateActiveSession(
        contactId: String,
        defaultTitle: String
    ): Result<AiAdvisorSession> = withContext(ioDispatcher) {
        runCatching {
            // Try to get existing active session
            val existingSession = aiAdvisorDao.getActiveSession(contactId)
            if (existingSession != null) {
                return@runCatching existingSession.toDomain()
            }

            // Create new session
            val newSession = AiAdvisorSession.create(
                contactId = contactId,
                title = defaultTitle
            )
            val entity = AiAdvisorSessionEntity.fromDomain(newSession)
            aiAdvisorDao.insertSession(entity)
            newSession
        }
    }

    override suspend fun getSessionById(sessionId: String): Result<AiAdvisorSession?> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.getSessionById(sessionId)?.toDomain()
            }
        }

    override suspend fun updateSessionTitle(sessionId: String, title: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.updateSessionTitle(sessionId, title, System.currentTimeMillis())
            }
        }

    override suspend fun deleteSession(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.deleteSession(sessionId)
            }
        }

    override suspend fun deleteSessionsByContact(contactId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.deleteSessionsByContact(contactId)
            }
        }

    // ==================== 对话管理 ====================

    override suspend fun saveMessage(conversation: AiAdvisorConversation): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = AiAdvisorConversationEntity.fromDomain(conversation)
                aiAdvisorDao.insertConversation(entity)
                // Update session message count and timestamp
                aiAdvisorDao.incrementMessageCount(conversation.sessionId, System.currentTimeMillis())
            }
        }

    override suspend fun getConversations(sessionId: String): Result<List<AiAdvisorConversation>> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.getConversationsBySession(sessionId).map { it.toDomain() }
            }
        }

    override fun getConversationsFlow(sessionId: String): Flow<List<AiAdvisorConversation>> =
        aiAdvisorDao.getConversationsBySessionFlow(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRecentConversations(
        contactId: String,
        limit: Int
    ): Result<List<AiAdvisorConversation>> = withContext(ioDispatcher) {
        runCatching {
            aiAdvisorDao.getRecentConversations(contactId, limit).map { it.toDomain() }
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.deleteConversation(conversationId)
            }
        }

    override suspend fun clearSession(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.clearSessionConversations(sessionId)
            }
        }

    override suspend fun getConversationCount(sessionId: String): Result<Int> =
        withContext(ioDispatcher) {
            runCatching {
                aiAdvisorDao.getConversationCount(sessionId)
            }
        }
}
