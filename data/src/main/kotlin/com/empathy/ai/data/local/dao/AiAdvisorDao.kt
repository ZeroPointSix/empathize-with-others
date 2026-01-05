package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.AiAdvisorConversationEntity
import com.empathy.ai.data.local.entity.AiAdvisorSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI军师数据访问对象
 *
 * 提供AI军师会话和对话记录的数据库操作接口。
 *
 * 业务背景 (PRD-00026/TD-00026):
 *   - AI军师是独立对话模块，支持深度分析和智能建议
 *   - 每个联系人可有多个会话，每个会话包含多条对话记录
 *   - 支持响应式查询（Flow），UI自动感知数据变化
 *
 * 设计决策 (TDD-00026):
 *   - 会话与联系人通过外键关联，支持级联删除
 *   - 活跃会话（is_active=1）同一时间只有一个
 *   - 消息计数在保存消息时自动更新（incrementMessageCount）
 *
 * @see AiAdvisorSessionEntity 会话实体
 * @see AiAdvisorConversationEntity 对话实体
 * @see FD-00026 AI军师对话功能设计
 */
@Dao
interface AiAdvisorDao {

    // ============================================================================
    // 会话操作
    // ============================================================================

    /**
     * 插入会话
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AiAdvisorSessionEntity)

    /**
     * 获取联系人的所有会话（按更新时间倒序）
     */
    @Query("""
        SELECT * FROM ai_advisor_sessions 
        WHERE contact_id = :contactId 
        ORDER BY updated_at DESC
    """)
    suspend fun getSessionsByContact(contactId: String): List<AiAdvisorSessionEntity>

    /**
     * 获取联系人的活跃会话
     *
     * 业务规则 (PRD-00026/US-003):
     *   - 每个联系人同一时间只有一个活跃会话
     *   - 新会话创建时自动设为活跃，旧会话设为非活跃
     *   - 用于判断是否需要创建新会话
     *
     * @param contactId 联系人ID
     * @return 活跃会话，不存在时返回null
     */
    @Query("""
        SELECT * FROM ai_advisor_sessions 
        WHERE contact_id = :contactId AND is_active = 1 
        LIMIT 1
    """)
    suspend fun getActiveSession(contactId: String): AiAdvisorSessionEntity?

    /**
     * 根据ID获取会话
     */
    @Query("SELECT * FROM ai_advisor_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): AiAdvisorSessionEntity?

    /**
     * 更新会话标题
     */
    @Query("""
        UPDATE ai_advisor_sessions 
        SET title = :title, updated_at = :updatedAt 
        WHERE id = :sessionId
    """)
    suspend fun updateSessionTitle(sessionId: String, title: String, updatedAt: Long)

    /**
     * 增加消息计数并更新时间
     */
    @Query("""
        UPDATE ai_advisor_sessions 
        SET message_count = message_count + 1, updated_at = :updatedAt 
        WHERE id = :sessionId
    """)
    suspend fun incrementMessageCount(sessionId: String, updatedAt: Long)

    /**
     * 删除会话
     */
    @Query("DELETE FROM ai_advisor_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    /**
     * 删除联系人的所有会话
     */
    @Query("DELETE FROM ai_advisor_sessions WHERE contact_id = :contactId")
    suspend fun deleteSessionsByContact(contactId: String)

    // ============================================================================
    // 对话操作
    // ============================================================================

    /**
     * 插入对话记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiAdvisorConversationEntity)

    /**
     * 获取会话的所有对话记录（按时间正序）
     */
    @Query("""
        SELECT * FROM ai_advisor_conversations 
        WHERE session_id = :sessionId 
        ORDER BY timestamp ASC
    """)
    suspend fun getConversationsBySession(sessionId: String): List<AiAdvisorConversationEntity>

    /**
     * 获取会话的对话记录流（响应式）
     *
     * [Flow订阅] 返回的Flow是"数据监听通道"：
     *   - 首次订阅：立即推送当前对话列表
     *   - 数据变动：自动推送新数据
     *   - 取消订阅：自动释放资源
     *
     * 这是聊天界面能实时更新的根本动力。
     *
     * @param sessionId 会话ID
     * @return 对话记录列表Flow
     */
    @Query("""
        SELECT * FROM ai_advisor_conversations 
        WHERE session_id = :sessionId 
        ORDER BY timestamp ASC
    """)
    fun getConversationsBySessionFlow(sessionId: String): Flow<List<AiAdvisorConversationEntity>>

    /**
     * 获取联系人的最近N条对话（跨所有会话）
     */
    @Query("""
        SELECT * FROM ai_advisor_conversations 
        WHERE contact_id = :contactId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentConversations(contactId: String, limit: Int): List<AiAdvisorConversationEntity>

    /**
     * 删除单条对话记录
     */
    @Query("DELETE FROM ai_advisor_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    /**
     * 清空会话的所有对话
     */
    @Query("DELETE FROM ai_advisor_conversations WHERE session_id = :sessionId")
    suspend fun clearSessionConversations(sessionId: String)

    /**
     * 获取会话的对话数量
     */
    @Query("SELECT COUNT(*) FROM ai_advisor_conversations WHERE session_id = :sessionId")
    suspend fun getConversationCount(sessionId: String): Int

    /**
     * 更新消息发送状态
     */
    @Query("""
        UPDATE ai_advisor_conversations 
        SET send_status = :status 
        WHERE id = :conversationId
    """)
    suspend fun updateSendStatus(conversationId: String, status: String)

    /**
     * 更新消息内容和状态
     *
     * BUG-044-P1-002: 用于停止生成时保存当前内容并更新状态。
     * 设计权衡：保留部分内容比完全删除更友好。
     *
     * @param conversationId 对话ID
     * @param content 当前内容（可能不完整）
     * @param status 新状态（如STOPPED）
     */
    @Query("""
        UPDATE ai_advisor_conversations 
        SET content = :content, send_status = :status 
        WHERE id = :conversationId
    """)
    suspend fun updateContentAndStatus(conversationId: String, content: String, status: String)
}
