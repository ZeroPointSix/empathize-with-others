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
 * AiAdvisorRepositoryImpl 实现了AI军师对话的数据访问层
 *
 * 【架构位置】Clean Architecture Data层 (TD-00026新增)
 * 【业务背景】(PRD-00026)AI军师对话功能的独立数据管理
 *   - 每个联系人可有多个会话，每个会话包含多条对话记录
 *   - 独立的深度分析对话系统，区别于悬浮窗的即时反应式功能
 *   - 支持会话历史持久化，退出应用后恢复
 *
 * 【设计决策】(TDD-00026)
 *   - 使用@IoDispatcher指定IO调度器，避免阻塞主线程
 *   - 会话消息数自动更新：incrementMessageCount在保存消息时触发
 *   - getOrCreateActiveSession实现懒加载：先查后创
 *
 * 【关键逻辑】
 *   - 会话管理：createSession/getSessions/deleteSession
 *   - 对话管理：saveMessage/getConversations/getConversationsFlow
 *   - 级联删除：删除会话时自动删除关联的对话记录
 *
 * 【任务追踪】
 *   - FD-00026/Task-001: AI军师仓库接口定义
 *   - FD-00026/Task-002: 数据库实体和DAO实现
 *   - FD-00026/Task-003: 仓库接口实现
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

    /**
     * getOrCreateActiveSession 获取或创建活跃会话
     *
     * 【策略】采用懒加载模式，延迟创建直到真正需要
     * 设计权衡：避免创建过多空会话，提升资源利用率
     *
     * 【业务规则】(PRD-00026/US-003)
     *   - 每个联系人只有一个活跃会话（is_active=1）
     *   - 新会话创建时自动设为活跃，旧会话自动设为非活跃
     *   - 首次对话时自动创建会话，用户无感知
     */
    override suspend fun getOrCreateActiveSession(
        contactId: String,
        defaultTitle: String
    ): Result<AiAdvisorSession> = withContext(ioDispatcher) {
        runCatching {
            // Step 1: 尝试获取现有活跃会话
            val existingSession = aiAdvisorDao.getActiveSession(contactId)
            if (existingSession != null) {
                return@runCatching existingSession.toDomain()
            }

            // Step 2: 不存在则创建新会话
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
