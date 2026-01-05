package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.empathy.ai.data.local.entity.AiAdvisorMessageBlockEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI军师消息块数据访问对象
 *
 * 提供消息块的CRUD操作，支持流式更新和响应式查询。
 *
 * 业务背景 (FD-00028):
 * - Block架构支持思考过程展示
 * - 支持智能节流更新，减少数据库写入频率
 * - 响应式查询支持UI实时更新
 *
 * 设计决策 (TDD-00028):
 * - 使用Flow支持响应式查询
 * - 提供updateContentAndStatus方法支持原子更新
 * - 按message_id查询时使用索引优化
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see AiAdvisorMessageBlockEntity 数据库实体
 */
@Dao
interface AiAdvisorMessageBlockDao {

    /**
     * 插入单个Block
     *
     * 使用REPLACE策略，如果ID冲突则替换。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: AiAdvisorMessageBlockEntity)

    /**
     * 批量插入Block
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<AiAdvisorMessageBlockEntity>)

    /**
     * 更新Block
     */
    @Update
    suspend fun update(block: AiAdvisorMessageBlockEntity)

    /**
     * 更新Block内容和状态
     *
     * 用于流式更新场景，原子性更新content和status字段。
     * 这是智能节流更新的核心方法。
     *
     * @param blockId Block ID
     * @param content 新内容
     * @param status 新状态（MessageBlockStatus.name）
     */
    @Query("""
        UPDATE ai_advisor_message_blocks 
        SET content = :content, status = :status 
        WHERE id = :blockId
    """)
    suspend fun updateContentAndStatus(
        blockId: String,
        content: String,
        status: String
    )

    /**
     * 更新Block内容
     *
     * 仅更新content字段，用于流式内容追加。
     *
     * @param blockId Block ID
     * @param content 新内容
     */
    @Query("UPDATE ai_advisor_message_blocks SET content = :content WHERE id = :blockId")
    suspend fun updateContent(blockId: String, content: String)

    /**
     * 更新Block状态
     *
     * 仅更新status字段。
     *
     * @param blockId Block ID
     * @param status 新状态（MessageBlockStatus.name）
     */
    @Query("UPDATE ai_advisor_message_blocks SET status = :status WHERE id = :blockId")
    suspend fun updateStatus(blockId: String, status: String)

    /**
     * 响应式查询消息的所有Block
     *
     * 返回Flow，支持UI实时更新。
     * 按创建时间升序排列。
     *
     * @param messageId 消息ID
     * @return Block列表Flow
     */
    @Query("SELECT * FROM ai_advisor_message_blocks WHERE message_id = :messageId ORDER BY created_at ASC")
    fun observeByMessageId(messageId: String): Flow<List<AiAdvisorMessageBlockEntity>>

    /**
     * 查询消息的所有Block
     *
     * 一次性查询，按创建时间升序排列。
     *
     * @param messageId 消息ID
     * @return Block列表
     */
    @Query("SELECT * FROM ai_advisor_message_blocks WHERE message_id = :messageId ORDER BY created_at ASC")
    suspend fun getByMessageId(messageId: String): List<AiAdvisorMessageBlockEntity>

    /**
     * 根据ID查询Block
     *
     * @param blockId Block ID
     * @return Block实体，如果不存在则返回null
     */
    @Query("SELECT * FROM ai_advisor_message_blocks WHERE id = :blockId")
    suspend fun getById(blockId: String): AiAdvisorMessageBlockEntity?

    /**
     * 删除消息的所有Block
     *
     * 用于清理消息关联的Block数据。
     * 注意：由于设置了级联删除，删除消息时会自动删除关联Block。
     *
     * @param messageId 消息ID
     */
    @Query("DELETE FROM ai_advisor_message_blocks WHERE message_id = :messageId")
    suspend fun deleteByMessageId(messageId: String)

    /**
     * 删除单个Block
     */
    @Delete
    suspend fun delete(block: AiAdvisorMessageBlockEntity)

    /**
     * 根据ID删除Block
     *
     * @param blockId Block ID
     */
    @Query("DELETE FROM ai_advisor_message_blocks WHERE id = :blockId")
    suspend fun deleteById(blockId: String)

    /**
     * 查询消息的Block数量
     *
     * @param messageId 消息ID
     * @return Block数量
     */
    @Query("SELECT COUNT(*) FROM ai_advisor_message_blocks WHERE message_id = :messageId")
    suspend fun getCountByMessageId(messageId: String): Int
}
