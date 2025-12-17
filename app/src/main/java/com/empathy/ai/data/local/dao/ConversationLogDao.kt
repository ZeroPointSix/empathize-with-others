package com.empathy.ai.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.empathy.ai.data.local.entity.ConversationLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * 对话记录DAO
 *
 * 提供对话记录的数据库访问方法
 */
@Dao
interface ConversationLogDao {

    /**
     * 插入对话记录
     */
    @Insert
    suspend fun insert(log: ConversationLogEntity): Long

    /**
     * 更新AI回复
     */
    @Query("UPDATE conversation_logs SET ai_response = :aiResponse WHERE id = :logId")
    suspend fun updateAiResponse(logId: Long, aiResponse: String)

    /**
     * 获取未总结的对话记录
     */
    @Query("""
        SELECT * FROM conversation_logs 
        WHERE is_summarized = 0 AND timestamp >= :sinceTimestamp
        ORDER BY timestamp ASC
    """)
    suspend fun getUnsummarizedLogs(sinceTimestamp: Long): List<ConversationLogEntity>

    /**
     * 获取指定联系人在指定日期的对话记录
     */
    @Query("""
        SELECT * FROM conversation_logs 
        WHERE contact_id = :contactId 
        AND date(timestamp/1000, 'unixepoch', 'localtime') = :date
        ORDER BY timestamp ASC
    """)
    suspend fun getLogsByContactAndDate(contactId: String, date: String): List<ConversationLogEntity>

    /**
     * 标记对话为已总结
     */
    @Query("UPDATE conversation_logs SET is_summarized = 1 WHERE id IN (:logIds)")
    suspend fun markAsSummarized(logIds: List<Long>)

    /**
     * 删除指定联系人的所有对话记录
     */
    @Query("DELETE FROM conversation_logs WHERE contact_id = :contactId")
    suspend fun deleteByContactId(contactId: String): Int

    /**
     * 清理过期的已总结对话
     */
    @Query("DELETE FROM conversation_logs WHERE is_summarized = 1 AND timestamp < :beforeTimestamp")
    suspend fun cleanupOldSummarizedLogs(beforeTimestamp: Long): Int

    /**
     * 获取分页数据源（用于Paging 3）
     */
    @Query("""
        SELECT * FROM conversation_logs
        WHERE contact_id = :contactId
        ORDER BY timestamp DESC
    """)
    fun getPagingSource(contactId: String): PagingSource<Int, ConversationLogEntity>

    /**
     * 获取指定联系人的所有对话记录
     */
    @Query("SELECT * FROM conversation_logs WHERE contact_id = :contactId ORDER BY timestamp DESC")
    suspend fun getConversationsByContact(contactId: String): List<ConversationLogEntity>

    /**
     * 获取指定联系人的对话记录流
     */
    @Query("SELECT * FROM conversation_logs WHERE contact_id = :contactId ORDER BY timestamp DESC")
    fun getConversationsByContactFlow(contactId: String): Flow<List<ConversationLogEntity>>

    /**
     * 更新用户输入内容
     */
    @Query("UPDATE conversation_logs SET user_input = :userInput WHERE id = :logId")
    suspend fun updateUserInput(logId: Long, userInput: String)

    /**
     * 删除单条对话记录
     */
    @Query("DELETE FROM conversation_logs WHERE id = :logId")
    suspend fun deleteById(logId: Long)

    /**
     * 获取指定联系人的最近N条对话记录
     * 按时间戳正序排列（最早的在前）
     *
     * 使用子查询先获取最近N条（倒序），再正序排列
     *
     * 【SQL性能分析】
     * - 利用 contact_id 索引快速定位联系人记录
     * - 利用 timestamp 索引进行排序
     * - 子查询 + LIMIT 避免全表扫描
     * - 预估性能：单人App场景（<10万条记录），查询时间 < 10ms
     */
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM conversation_logs 
            WHERE contact_id = :contactId 
            ORDER BY timestamp DESC 
            LIMIT :limit
        ) 
        ORDER BY timestamp ASC
        """
    )
    suspend fun getRecentConversations(contactId: String, limit: Int): List<ConversationLogEntity>
}
