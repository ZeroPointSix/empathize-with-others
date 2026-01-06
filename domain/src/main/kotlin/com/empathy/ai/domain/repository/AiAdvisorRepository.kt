package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorMessageBlock
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.SendStatus
import kotlinx.coroutines.flow.Flow

/**
 * AI军师对话仓储接口
 *
 * 业务背景 (PRD-00026):
 * - AI军师是提供独立深度分析对话的智能体模块，与主应用共享联系人数据
 * - 支持多会话管理，每个联系人可有多个对话主题
 * - 实现对话历史持久化，支持上下文连续性
 *
 * 设计决策 (TDD-00026):
 * - 采用会话-对话双层结构：Session(会话)管理主题，Conversation(消息)存储内容
 * - 会话默认标题机制：首次创建时使用"新对话"，支持后续自定义
 * - 活跃会话优先策略：getOrCreateActiveSession 确保复用已有会话
 * - 级联删除：删除会话时自动清理关联的对话记录
 *
 * 任务追踪 (FD-00026):
 * - TD-00026: AI军师对话功能 - 会话与对话管理
 */
interface AiAdvisorRepository {

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     *
     * 业务规则 (PRD-00026/AC-001):
     * - 新会话默认为非活跃状态，需要通过 getOrCreateActiveSession 激活
     * - 会话创建时自动记录创建时间，用于排序和展示
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
     * 设计权衡 (TDD-00026):
     * - 优先复用已有活跃会话，避免频繁创建新会话导致上下文碎片化
     * - 如果联系人不存在活跃会话，则创建新会话并设为活跃
     *
     * 业务规则 (PRD-00026):
     * - 每个联系人同一时间只能有一个活跃会话
     * - 活跃会话用于继续上次的对话主题
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
     * 设计权衡: 采用级联删除策略，删除会话时自动清理所有关联对话记录
     * 避免数据碎片化和孤立数据，简化上层调用逻辑
     *
     * 业务规则 (PRD-00026):
     * - 会话删除后不可恢复
     * - 级联删除该会话下的所有对话记录
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
     * 用途: 用于构建AI上下文，支持多轮对话的连续性分析
     *
     * 业务规则:
     * - 返回按时间戳升序排列的历史记录（ oldest -> newest）
     * - 限制最大记录数，避免上下文过长导致token超限
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

    // ==================== FD-00028: Block管理 ====================

    /**
     * 保存消息块
     *
     * 用于创建新的Block（如MAIN_TEXT、THINKING等）。
     *
     * @param block 消息块对象
     * @return 保存结果
     */
    suspend fun saveBlock(block: AiAdvisorMessageBlock): Result<Unit>

    /**
     * 更新Block内容和状态
     *
     * 用于流式更新场景，原子性更新content和status字段。
     * 这是智能节流更新的核心方法。
     *
     * @param blockId Block ID
     * @param content 新内容
     * @param status 新状态
     * @return 更新结果
     */
    suspend fun updateBlockContent(
        blockId: String,
        content: String,
        status: MessageBlockStatus
    ): Result<Unit>

    /**
     * 获取消息的所有Block
     *
     * @param messageId 消息ID
     * @return Block列表，按创建时间升序排列
     */
    suspend fun getBlocksByMessageId(messageId: String): Result<List<AiAdvisorMessageBlock>>

    /**
     * 响应式观察消息的Block
     *
     * 返回Flow，支持UI实时更新。
     *
     * @param messageId 消息ID
     * @return Block列表Flow
     */
    fun observeBlocksByMessageId(messageId: String): Flow<List<AiAdvisorMessageBlock>>

    /**
     * 更新消息状态
     *
     * 用于更新消息的发送状态（如PENDING→SUCCESS、PENDING→FAILED等）。
     *
     * @param messageId 消息ID
     * @param status 新状态
     * @return 更新结果
     */
    suspend fun updateMessageStatus(messageId: String, status: SendStatus): Result<Unit>

    /**
     * 更新消息内容和状态
     *
     * BUG-044-P1-002: 用于停止生成时保存当前内容并更新状态。
     *
     * @param messageId 消息ID
     * @param content 新内容
     * @param status 新状态
     * @return 更新结果
     */
    suspend fun updateMessageContentAndStatus(
        messageId: String,
        content: String,
        status: SendStatus
    ): Result<Unit>

    /**
     * 更新AI消息内容和状态（带类型验证）
     *
     * BUG-048修复: 只更新AI类型的消息，防止误更新用户消息。
     * 用于停止生成时保存当前内容并更新状态。
     *
     * @param messageId 消息ID
     * @param content 新内容
     * @param status 新状态
     * @return 更新结果，包含是否成功更新（如果消息不是AI类型则返回false）
     */
    suspend fun updateAiMessageContentAndStatus(
        messageId: String,
        content: String,
        status: SendStatus
    ): Result<Boolean>

    /**
     * 删除消息
     *
     * BUG-044-P1-002: 用于删除没有内容的消息。
     *
     * @param messageId 消息ID
     * @return 删除结果
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>
}
