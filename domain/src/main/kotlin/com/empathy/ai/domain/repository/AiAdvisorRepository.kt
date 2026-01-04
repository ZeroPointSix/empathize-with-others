package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import kotlinx.coroutines.flow.Flow

/**
 * AI军师对话仓库接口
 *
 * 提供AI军师会话和对话记录的数据访问抽象。
 * 遵循Clean Architecture原则，定义在domain层，由data层实现。
 */
interface AiAdvisorRepository {

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     *
     * @param session 会话对象
     * @return 创建结果
     */
    suspend fun createSession(session: AiAdvisorSession): Result<Unit>

    /**
     * 获取联系人的所有会话列表
     *
     * @param contactId 联系人ID
     * @return 会话列表，按更新时间降序排列
     */
    suspend fun getSessions(contactId: String): Result<List<AiAdvisorSession>>

    /**
     * 获取联系人的活跃会话
     *
     * @param contactId 联系人ID
     * @return 活跃会话，如果不存在则返回null
     */
    suspend fun getActiveSession(contactId: String): Result<AiAdvisorSession?>

    /**
     * 获取或创建活跃会话
     *
     * 如果联系人已有活跃会话则返回，否则创建新会话。
     *
     * @param contactId 联系人ID
     * @param defaultTitle 默认会话标题
     * @return 活跃会话
     */
    suspend fun getOrCreateActiveSession(
        contactId: String,
        defaultTitle: String = "新对话"
    ): Result<AiAdvisorSession>

    /**
     * 根据ID获取会话
     *
     * @param sessionId 会话ID
     * @return 会话对象，如果不存在则返回null
     */
    suspend fun getSessionById(sessionId: String): Result<AiAdvisorSession?>

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title 新标题
     * @return 更新结果
     */
    suspend fun updateSessionTitle(sessionId: String, title: String): Result<Unit>

    /**
     * 删除会话
     *
     * 会级联删除该会话下的所有对话记录。
     *
     * @param sessionId 会话ID
     * @return 删除结果
     */
    suspend fun deleteSession(sessionId: String): Result<Unit>

    /**
     * 删除联系人的所有会话
     *
     * @param contactId 联系人ID
     * @return 删除结果
     */
    suspend fun deleteSessionsByContact(contactId: String): Result<Unit>

    // ==================== 对话管理 ====================

    /**
     * 保存对话消息
     *
     * 同时会更新会话的消息计数和更新时间。
     *
     * @param conversation 对话记录
     * @return 保存结果
     */
    suspend fun saveMessage(conversation: AiAdvisorConversation): Result<Unit>

    /**
     * 获取会话的对话记录列表
     *
     * @param sessionId 会话ID
     * @return 对话记录列表，按时间戳升序排列
     */
    suspend fun getConversations(sessionId: String): Result<List<AiAdvisorConversation>>

    /**
     * 获取会话的对话记录流（响应式）
     *
     * @param sessionId 会话ID
     * @return 对话记录流，按时间戳升序排列
     */
    fun getConversationsFlow(sessionId: String): Flow<List<AiAdvisorConversation>>

    /**
     * 获取联系人的最近对话记录
     *
     * 用于构建AI上下文。
     *
     * @param contactId 联系人ID
     * @param limit 最大记录数
     * @return 最近对话记录列表
     */
    suspend fun getRecentConversations(
        contactId: String,
        limit: Int = 20
    ): Result<List<AiAdvisorConversation>>

    /**
     * 删除单条对话记录
     *
     * @param conversationId 对话记录ID
     * @return 删除结果
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    /**
     * 清空会话的所有对话记录
     *
     * @param sessionId 会话ID
     * @return 清空结果
     */
    suspend fun clearSession(sessionId: String): Result<Unit>

    /**
     * 获取会话的对话数量
     *
     * @param sessionId 会话ID
     * @return 对话数量
     */
    suspend fun getConversationCount(sessionId: String): Result<Int>
}
