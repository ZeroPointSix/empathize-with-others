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
 * 提供对话主题的CRUD操作，支持主题状态管理和历史追踪。
 *
 * 业务背景 (PRD-00016):
 *   - 每个联系人可以有多个对话主题（如"项目协作"、"日常闲聊"）
 *   - 活跃主题用于AI分析的上下文输入
 *   - 主题历史记录便于用户回顾和切换
 *
 * 设计决策:
 *   - 使用外键关联contact_profiles表，联系人删除时级联删除主题
 *   - 活跃主题同一时间只有一个，新主题创建时自动设为活跃
 *   - 使用UUID作为主键，支持跨设备同步（如果未来需要）
 *
 * 【状态管理】
 * - is_active字段标识当前活跃主题
 * - deactivateAllTopics用于切换主题时停用旧主题
 * - 更新时自动更新updated_at时间戳
 *
 * @see ConversationTopicEntity 对话主题实体
 * @see FD-00016 对话主题功能设计
 */
@Dao
interface ConversationTopicDao {

    /**
     * 插入主题（冲突时替换）
     *
     * 【Upsert设计】使用REPLACE策略：
     * - 如果ID不存在，执行插入新主题
     * - 如果ID已存在，更新主题内容
     * - 新主题自动设为活跃（is_active=1）
     *
     * @param topic 主题实体
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
     *
     * 【单活跃主题】每个联系人的活跃主题只有一个：
     * - 用于AI分析的上下文输入
     * - 优先显示在聊天界面
     *
     * @param contactId 联系人ID
     * @return 活跃主题实体，不存在时返回null
     */
    @Query("""
        SELECT * FROM conversation_topics 
        WHERE contact_id = :contactId AND is_active = 1 
        LIMIT 1
    """)
    suspend fun getActiveTopic(contactId: String): ConversationTopicEntity?


    /**
     * 观察联系人当前活跃主题（响应式）
     *
     * 【Flow订阅】返回的Flow是"数据监听通道"：
     * - 首次订阅：立即推送当前活跃主题
     * - 主题切换：自动推送新主题
     * - 用于UI实时感知主题变化
     *
     * @param contactId 联系人ID
     * @return 活跃主题实体的Flow
     */
    @Query("""
        SELECT * FROM conversation_topics 
        WHERE contact_id = :contactId AND is_active = 1 
        LIMIT 1
    """)
    fun observeActiveTopic(contactId: String): Flow<ConversationTopicEntity?>

    /**
     * 停用联系人所有活跃主题
     *
     * 【主题切换场景】创建新主题时调用：
     * 1. 停用当前所有活跃主题
     * 2. 创建新主题并设为活跃
     * 3. 旧主题保留在历史记录中
     *
     * @param contactId 联系人ID
     * @param timestamp 更新时间戳
     */
    @Query("""
        UPDATE conversation_topics 
        SET is_active = 0, updated_at = :timestamp 
        WHERE contact_id = :contactId AND is_active = 1
    """)
    suspend fun deactivateAllTopics(contactId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * 获取联系人主题历史（按更新时间倒序）
     *
     * 【历史记录】展示用户曾经使用过的主题：
     * - 按更新时间降序排列，最近的在前
     * - 支持限制返回数量（limit）
     * - 用于主题选择器的历史推荐
     *
     * @param contactId 联系人ID
     * @param limit 返回数量限制
     * @return 主题历史列表
     */
    @Query("""
        SELECT * FROM conversation_topics 
        WHERE contact_id = :contactId 
        ORDER BY updated_at DESC 
        LIMIT :limit
    """)
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
    @Query("""
        UPDATE conversation_topics 
        SET content = :content, updated_at = :timestamp 
        WHERE id = :topicId
    """)
    suspend fun updateContent(
        topicId: String,
        content: String,
        timestamp: Long = System.currentTimeMillis()
    )
}
