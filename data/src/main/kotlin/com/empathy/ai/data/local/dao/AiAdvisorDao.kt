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
}
