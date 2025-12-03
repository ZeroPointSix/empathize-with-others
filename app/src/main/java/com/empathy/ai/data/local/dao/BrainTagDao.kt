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
 * 设计原则:
 * - 查询要响应式: 返回Flow,实现"数据喂养页面添加标签 → 聊天页面分析卡片立即感知"
 * - 写入要挂起: Insert/Delete使用suspend函数
 *
 * 核心操作:
 * 1. 查询某人的所有标签(getTagsByContactId) - 返回Flow<List>,实时监听
 * 2. 保存标签(insertTag) - 冲突时替换
 * 3. 删除标签(deleteTag) - 物理删除
 *
 * @see BrainTagEntity
 */
@Dao
interface BrainTagDao {

    /**
     * 查询某联系人的所有标签
     *
     * 同样使用Flow实现响应式查询。当你在"数据喂养"页面添加一个标签时,
     * 聊天页面的分析卡片应该能立即感知到数据变化并更新。
     *
     * @param contactId 联系人ID
     * @return 标签实体列表的Flow
     */
    @Query("SELECT * FROM brain_tags WHERE contact_id = :contactId")
    fun getTagsByContactId(contactId: String): Flow<List<BrainTagEntity>>

    /**
     * 获取全库的雷区标签
     *
     * 用于全局风控检测或无特定联系人时的通用检测。
     * 这是一个一次性查询,返回List而非Flow。
     *
     * @return 所有雷区标签列表
     */
    @Query("SELECT * FROM brain_tags WHERE tag_type = 'RISK_RED'")
    suspend fun getAllRedFlags(): List<BrainTagEntity>

    /**
     * 保存标签
     *
     * 使用REPLACE策略,如果ID冲突则覆盖更新。
     * 返回插入的标签ID(对新插入的标签有用)。
     *
     * @param entity 标签实体
     * @return 插入的标签ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: BrainTagEntity): Long

    /**
     * 根据ID删除标签
     *
     * 物理删除,操作简单直接。
     *
     * @param id 要删除的标签ID
     */
    @Query("DELETE FROM brain_tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    /**
     * 根据联系人ID删除所有标签
     *
     * 删除联系人时使用,级联删除其所有标签。
     *
     * @param contactId 联系人ID
     */
    @Query("DELETE FROM brain_tags WHERE contact_id = :contactId")
    suspend fun deleteTagsByContactId(contactId: String)
}
