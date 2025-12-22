package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.empathy.ai.data.local.entity.ConversationTopicEntity
import kotlinx.coroutines.flow.Flow

/**
 * 对话主题数据访问对象
 *
 * 提供对话主题的CRUD操作，支持：
 * - 按联系人获取活跃主题
 * - 响应式观察主题变化
 * - 主题历史记录查询
 * - 批量停用主题
 */
@Dao
interface ConversationTopicDao {

    /**
     * 插入主题（冲突时替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(topic: ConversationTopicEntity)

    /**
     * 更新主题
     */
    @Update
    suspend fun update(topic: ConversationTopicEntity)

    /**
     * 根据ID删除主题
     */
    @Query("DELETE FROM conversation_topics WHERE id = :topicId")
    suspend fun deleteById(topicId: String)

    /**
     * 获取联系人当前活跃主题
     */
    @Query(
        """
        SELECT * FROM conversation_topics 
        WHERE contact_id = :contactId AND is_active = 1 
        LIMIT 1
        """
    )
    suspend fun getActiveTopic(contactId: String): ConversationTopicEntity?

    /**
     * 观察联系人当前活跃主题（响应式）
     */
    @Query(
        """
        SELECT * FROM conversation_topics 
        WHERE contact_id = :contactId AND is_active = 1 
        LIMIT 1
        """
    )
    fun observeActiveTopic(contactId: String): Flow<ConversationTopicEntity?>

    /**
     * 停用联系人所有活跃主题
     *
     * @param contactId 联系人ID
     * @param timestamp 更新时间戳
     */
    @Query(
        """
        UPDATE conversation_topics 
        SET is_active = 0, updated_at = :timestamp 
        WHERE contact_id = :contactId AND is_active = 1
        """
    )
    suspend fun deactivateAllTopics(contactId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * 获取联系人主题历史（按更新时间倒序）
     *
     * @param contactId 联系人ID
     * @param limit 返回数量限制
     */
    @Query(
        """
        SELECT * FROM conversation_topics 
        WHERE contact_id = :contactId 
        ORDER BY updated_at DESC 
        LIMIT :limit
        """
    )
    suspend fun getTopicHistory(contactId: String, limit: Int): List<ConversationTopicEntity>

    /**
     * 根据ID获取主题
     */
    @Query("SELECT * FROM conversation_topics WHERE id = :topicId")
    suspend fun getById(topicId: String): ConversationTopicEntity?

    /**
     * 更新主题内容
     *
     * @param topicId 主题ID
     * @param content 新内容
     * @param timestamp 更新时间戳
     */
    @Query(
        """
        UPDATE conversation_topics 
        SET content = :content, updated_at = :timestamp 
        WHERE id = :topicId
        """
    )
    suspend fun updateContent(
        topicId: String,
        content: String,
        timestamp: Long = System.currentTimeMillis()
    )
}
