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
 * 提供对话记录的数据库访问方法，管理"短期记忆"层的数据。
 *
 * 业务背景 (PRD-00003):
 *   - 对话记录是用户与AI交互的原始数据
 *   - is_summarized标记用于区分已处理和待处理的记录
 *   - 定期归档到每日总结后标记为已总结
 *
 * 设计决策:
 *   - 使用PagingSource支持大数据量分页加载
 *   - 支持Flow响应式查询，聊天页面实时更新
 *   - 级联删除：删除联系人时自动删除其对话记录
 *
 * 【数据生命周期】
 * 1. 用户发送消息 → 创建记录，is_summarized=false
 * 2. AI响应生成 → 更新aiResponse字段
 * 3. 每日总结生成 → 标记is_summarized=true
 * 4. 清理过期记录 → 删除已总结的旧记录
 *
 * @see ConversationLogEntity 对话记录实体
 * @see FD-00003 联系人画像记忆系统设计
 */
@Dao
interface ConversationLogDao {

    /**
     * 插入对话记录
     *
     * 【挂起函数】使用suspend确保在协程中执行
     * - 数据库操作是IO密集型，不应阻塞主线程
     * - Room自动处理协程上下文切换
     *
     * @param log 对话记录实体
     * @return 插入的记录ID
     */
    @Insert
    suspend fun insert(log: ConversationLogEntity): Long

    /**
     * 更新AI回复
     *
     * 【流式响应场景】AI回复是逐步生成的：
     * - 首次调用：保存初始回复内容
     * - 后续调用：追加新内容
     * - 适用于流式API的增量更新
     *
     * @param logId 对话记录ID
     * @param aiResponse AI回复内容
     */
    @Query("UPDATE conversation_logs SET ai_response = :aiResponse WHERE id = :logId")
    suspend fun updateAiResponse(logId: Long, aiResponse: String)

    /**
     * 获取未总结的对话记录
     *
     * 【总结触发条件】用于每日总结的素材收集：
     * - 筛选 is_summarized = 0 的记录
     * - 只获取指定时间之后的记录（sinceTimestamp）
     * - 按时间正序排列，便于逐条处理
     *
     * @param sinceTimestamp 时间戳阈值
     * @return 未总结的对话记录列表
     */
    @Query("""
        SELECT * FROM conversation_logs 
        WHERE is_summarized = 0 AND timestamp >= :sinceTimestamp
        ORDER BY timestamp ASC
    """)
    suspend fun getUnsummarizedLogs(sinceTimestamp: Long): List<ConversationLogEntity>

    /**
     * 获取指定联系人在指定日期的对话记录
     *
     * 【SQL日期函数】使用SQLite的date()函数：
     *   date(timestamp/1000, 'unixepoch', 'localtime')
     * 将Unix时间戳转换为本地日期字符串
     *
     * @param contactId 联系人ID
     * @param date 日期字符串（格式：YYYY-MM-DD）
     * @return 指定日期的对话记录列表
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
     *
     * 【批量更新】使用IN子句批量标记：
     * - 一次数据库操作更新多条记录
     * - 比逐条更新效率高得多
     *
     * @param logIds 待标记的记录ID列表
     */
    @Query("UPDATE conversation_logs SET is_summarized = 1 WHERE id IN (:logIds)")
    suspend fun markAsSummarized(logIds: List<Long>)


    /**
     * 删除指定联系人的所有对话记录
     *
     * 【级联删除】配合外键约束使用：
     * - 联系人删除时自动触发
     * - 避免 orphaned 数据（无主的对话）
     *
     * @param contactId 联系人ID
     * @return 删除的记录数
     */
    @Query("DELETE FROM conversation_logs WHERE contact_id = :contactId")
    suspend fun deleteByContactId(contactId: String): Int

    /**
     * 清理过期的已总结对话
     *
     * 【自动清理策略】释放存储空间：
     * - 只清理已总结的记录（is_summarized = 1）
     * - 保留未处理的记录用于总结生成
     * - 保留时间由应用设置决定
     *
     * @param beforeTimestamp 时间阈值
     * @return 删除的记录数
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
    @Query("""
        SELECT * FROM (
            SELECT * FROM conversation_logs 
            WHERE contact_id = :contactId 
            ORDER BY timestamp DESC 
            LIMIT :limit
        ) 
        ORDER BY timestamp ASC
    """)
    suspend fun getRecentConversations(contactId: String, limit: Int): List<ConversationLogEntity>

    // ============================================================================
    // 编辑追踪扩展方法（v10）
    // ============================================================================

    /**
     * 根据ID获取对话记录
     *
     * @param logId 对话记录ID
     * @return 对话记录实体，不存在则返回null
     */
    @Query("SELECT * FROM conversation_logs WHERE id = :logId")
    suspend fun getById(logId: Long): ConversationLogEntity?

    /**
     * 更新对话内容（编辑，带追踪）
     *
     * 使用CASE WHEN保留首次原始值：
     * - 如果original_user_input为NULL，则保存当前传入的originalInput
     * - 如果original_user_input已有值，则保留原有值
     *
     * @param logId 对话记录ID
     * @param newUserInput 新的用户输入内容
     * @param modifiedTime 修改时间
     * @param originalInput 原始用户输入（仅首次编辑时保存）
     * @return 受影响的行数
     */
    @Query("""
        UPDATE conversation_logs SET 
            user_input = :newUserInput,
            is_user_modified = 1,
            last_modified_time = :modifiedTime,
            original_user_input = CASE WHEN original_user_input IS NULL THEN :originalInput ELSE original_user_input END
        WHERE id = :logId
    """)
    suspend fun updateUserInputWithTracking(
        logId: Long,
        newUserInput: String,
        modifiedTime: Long,
        originalInput: String
    ): Int
}
