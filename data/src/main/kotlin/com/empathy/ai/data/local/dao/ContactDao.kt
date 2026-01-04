package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.ContactProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * 联系人数据访问对象 (DAO)
 *
 * 【设计原则】
 * - 查询响应式：返回Flow，数据变动自动推送（MVVM架构UI刷新的根本动力）
 * - 写入简单化：使用REPLACE策略，简化上层insert/update判断
 *
 * 【三层记忆架构】联系人画像是长期记忆的载体：
 * - 短期记忆：对话记录（ConversationLog）
 * - 中期记忆：每日总结（DailySummary）
 * - 长期记忆：联系人画像（ContactProfile）
 *
 * @see ContactProfileEntity
 * @see com.empathy.ai.domain.repository.ContactRepository
 */
@Dao
interface ContactDao {

    /**
     * 查询所有联系人
     *
     * 【Flow vs List】返回Flow意味着建立一个"数据监听通道"：
     * - 首次订阅：立即推送当前联系人列表
     * - 数据变动：自动推送新数据（新增/修改/删除）
     * - 取消订阅：自动释放资源
     *
     * 这就是为什么联系人列表能实时刷新，无需手动调用refresh。
     *
     * @return 联系人实体列表的Flow
     */
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ContactProfileEntity>>

    /**
     * 根据ID查询单个联系人
     *
     * @param id 联系人唯一标识
     * @return 联系人实体
     */
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ContactProfileEntity?

    /**
     * 插入或更新联系人
     *
     * 【Upsert设计】使用REPLACE策略：
     * - ID不存在 → 执行INSERT
     * - ID已存在 → 执行UPDATE（覆盖旧数据）
     *
     * 上层无需区分insert和update，专注业务逻辑。
     * 注意：这是挂起函数(suspend)，必须在协程中调用。
     *
     * @param entity 联系人实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ContactProfileEntity)

    /**
     * 根据ID删除联系人
     *
     * @param id 要删除的联系人ID
     */
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    // ============================================================================
    // 记忆系统扩展方法
    // ============================================================================

    /**
     * 更新关系分数
     *
     * 【关系分数】0-100的量化指标：
     * - 初始值：50（中立）
     * - AI建议被采纳：加分
     * - 沟通效果正向反馈：加分
     * - 用于AI分析的优先级排序
     *
     * @param id 联系人ID
     * @param score 新的关系分数
     */
    @Query("UPDATE profiles SET relationship_score = :score WHERE id = :id")
    suspend fun updateRelationshipScore(id: String, score: Int)

    /**
     * 更新Facts
     *
     * 【核心事实槽】Facts是联系人画像的精华：
     * - 格式：List<Fact>序列化为JSON
     * - 内容：喜好、习惯、重要日期等
     * - 来源：用户手动添加 + AI分析推断
     *
     * @param id 联系人ID
     * @param factsJson Facts的JSON字符串
     * @see com.empathy.ai.data.local.converter.FactListConverter
     */
    @Query("UPDATE profiles SET facts_json = :factsJson WHERE id = :id")
    suspend fun updateFacts(id: String, factsJson: String)

    /**
     * 更新最后互动日期
     *
     * @param id 联系人ID
     * @param date 日期字符串
     */
    @Query("UPDATE profiles SET last_interaction_date = :date WHERE id = :id")
    suspend fun updateLastInteractionDate(id: String, date: String)

    // ============================================================================
    // 提示词管理系统扩展方法
    // ============================================================================

    /**
     * 获取联系人自定义提示词
     *
     * @param contactId 联系人ID
     * @return 自定义提示词，如果未设置则返回null
     */
    @Query("SELECT custom_prompt FROM profiles WHERE id = :contactId")
    suspend fun getCustomPrompt(contactId: String): String?

    /**
     * 更新联系人自定义提示词
     *
     * @param contactId 联系人ID
     * @param prompt 自定义提示词，传null表示清除
     */
    @Query("UPDATE profiles SET custom_prompt = :prompt WHERE id = :contactId")
    suspend fun updateCustomPrompt(contactId: String, prompt: String?)

    // ============================================================================
    // 编辑追踪扩展方法（v10）
    // ============================================================================

    /**
     * 更新联系人姓名（编辑）
     *
     * 使用CASE WHEN保留首次原始值：
     * - 如果original_name为NULL，则保存当前传入的originalName
     * - 如果original_name已有值，则保留原有值
     *
     * @param contactId 联系人ID
     * @param newName 新的姓名
     * @param modifiedTime 修改时间
     * @param originalName 原始姓名（仅首次编辑时保存）
     * @return 受影响的行数
     */
    @Query("""
        UPDATE profiles SET 
            name = :newName,
            is_name_user_modified = 1,
            name_last_modified_time = :modifiedTime,
            original_name = CASE WHEN original_name IS NULL THEN :originalName ELSE original_name END
        WHERE id = :contactId
    """)
    suspend fun updateName(
        contactId: String,
        newName: String,
        modifiedTime: Long,
        originalName: String
    ): Int

    /**
     * 更新联系人目标（编辑）
     *
     * 使用CASE WHEN保留首次原始值
     *
     * @param contactId 联系人ID
     * @param newGoal 新的目标
     * @param modifiedTime 修改时间
     * @param originalGoal 原始目标（仅首次编辑时保存）
     * @return 受影响的行数
     */
    @Query("""
        UPDATE profiles SET 
            target_goal = :newGoal,
            is_goal_user_modified = 1,
            goal_last_modified_time = :modifiedTime,
            original_goal = CASE WHEN original_goal IS NULL THEN :originalGoal ELSE original_goal END
        WHERE id = :contactId
    """)
    suspend fun updateGoal(
        contactId: String,
        newGoal: String,
        modifiedTime: Long,
        originalGoal: String
    ): Int
}
