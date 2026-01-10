package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.BrainTagEntity
import kotlinx.coroutines.flow.Flow

/**
 * 策略标签数据访问对象 (DAO)
 *
 * 【设计原则】
 * - 查询响应式：返回Flow，实现"数据喂养页面添加标签 → 聊天页面分析卡片立即感知"
 * - 写入挂起：Insert/Delete使用suspend函数，确保协程安全
 *
 * 【标签类型】BrainTag是沟通策略的精华：
 * - RISK_RED（雷区）：必须避免的话题或行为
 * - STRATEGY_GREEN（策略）：推荐的沟通策略
 * - 标签来源：用户手动添加 + AI分析推断
 *
 * @see BrainTagEntity
 * @see com.empathy.ai.domain.model.TagType
 */
@Dao
interface BrainTagDao {

    /**
     * 查询某联系人的所有标签
     *
     * 【Flow的联动效应】当你在"数据喂养"页面添加一个标签时：
     * - 聊天页面的分析卡片应该能立即感知到数据变化并更新
     * - 这种跨页面的实时联动是Flow的核心价值
     * - 无需手动刷新或轮询
     *
     * @param contactId 联系人ID
     * @return 标签实体列表的Flow
     */
    @Query("SELECT * FROM brain_tags WHERE contact_id = :contactId")
    fun getTagsByContactId(contactId: String): Flow<List<BrainTagEntity>>

    /**
     * 获取全库的雷区标签
     *
     * 【全局风控】用于以下场景：
     * - 无特定联系人时的通用检测
     * - 新建联系人时的初始风险评估
     * - 全局敏感话题过滤
     *
     * 这是一次性查询（返回List而非Flow），因为雷区标签变更不频繁。
     *
     * @return 所有雷区标签列表
     */
    @Query("SELECT * FROM brain_tags WHERE tag_type = 'RISK_RED'")
    suspend fun getAllRedFlags(): List<BrainTagEntity>

    /**
     * 保存标签
     *
     * 【Upsert设计】使用REPLACE策略：
     * - 如果ID冲突则覆盖更新（更新内容）
     * - 如果ID不存在则插入新记录
     *
     * 返回插入的标签ID，用于新创建标签后的后续操作。
     *
     * @param entity 标签实体
     * @return 插入的标签ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: BrainTagEntity): Long

    /**
     * 根据ID删除标签
     *
     * 【物理删除】标签一旦删除不可恢复：
     * - 用于用户主动移除错误的标签
     * - 确认来源：AI推断的标签删除时需二次确认
     *
     * @param id 要删除的标签ID
     */
    @Query("DELETE FROM brain_tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    /**
     * 根据联系人ID删除所有标签
     *
     * 【级联删除】删除联系人时使用：
     * - 联系人删除时自动删除其所有标签
     * - 避免 orphaned 数据（无主的标签）
     *
     * @param contactId 联系人ID
     */
    @Query("DELETE FROM brain_tags WHERE contact_id = :contactId")
    suspend fun deleteTagsByContactId(contactId: String)

    /**
     * 根据ID获取单个标签
     *
     * 【编辑前查询】用于编辑标签前验证标签存在：
     * - BUG-00066: 画像标签编辑功能
     * - 返回 null 表示标签不存在
     *
     * @param id 标签ID
     * @return 标签实体，如果不存在返回 null
     */
    @Query("SELECT * FROM brain_tags WHERE id = :id")
    suspend fun getTagById(id: Long): BrainTagEntity?

    /**
     * 更新标签内容和类型
     *
     * 【部分更新】只更新 content 和 tag_type 字段：
     * - BUG-00066: 画像标签编辑功能
     * - 保留 contact_id、source 等字段不变
     * - 用于用户编辑已添加的标签
     *
     * @param id 标签ID
     * @param content 新的标签内容
     * @param type 新的标签类型（RISK_RED 或 STRATEGY_GREEN）
     */
    @Query("UPDATE brain_tags SET content = :content, tag_type = :type WHERE id = :id")
    suspend fun updateTag(id: Long, content: String, type: String)
}
