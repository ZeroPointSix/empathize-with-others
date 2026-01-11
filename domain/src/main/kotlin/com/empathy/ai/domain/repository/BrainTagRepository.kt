package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.BrainTag
import kotlinx.coroutines.flow.Flow

/**
 * 脑标签仓储接口
 *
 * 业务背景 (PRD-00003):
 * - BrainTag是联系人画像的重要组成部分，分为"雷区"(RISK_RED)和"策略"(STRATEGY_GREEN)
 * - 雷区标签标记绝对不能触碰的话题
 * - 策略标签标记推荐的沟通切入点和方式
 * - 用于RAG检索，作为AI分析的重要参考依据
 *
 * 设计决策:
 * - 双标签类型：红色雷区（避免）、绿色策略（推荐）
 * - Flow响应式：支持UI实时监听标签变化
 * - 标签来源：AI推断(AI_INFERRED) vs 用户手动(MANUAL)
 *
 * 服务对象: 实时风控检测、策略分析
 */
interface BrainTagRepository {

    /**
     * 获取指定联系人的所有策略标签
     *
     * 业务规则:
     * - RAG核心：获取某人的所有"锦囊"
     * - 返回Flow，支持UI实时监听标签变化
     * - 按标签类型分组，便于前端展示
     *
     * @param contactId 联系人ID
     * @return 该联系人的所有脑标签的Flow
     */
    fun getTagsForContact(contactId: String): Flow<List<BrainTag>>

    /**
     * 获取全库的雷区标签
     *
     * 业务规则:
     * - 用于全局风控，或无特定联系人时的通用检测
     * - 返回所有RISK_RED类型标签
     *
     * @return 包含所有雷区标签列表的Result
     */
    suspend fun getAllRedFlags(): Result<List<BrainTag>>

    /**
     * 保存脑标签
     *
     * 业务规则:
     * - 支持AI推断的标签和用户手动添加的标签
     * - AI推断的标签标记来源为AI_INFERRED
     * - 保存前检查是否已存在相同内容，避免重复
     *
     * @param tag 要保存的脑标签
     * @return 包含新插入ID的Result
     */
    suspend fun saveTag(tag: BrainTag): Result<Long>

    /**
     * 删除脑标签
     *
     * 业务规则:
     * - 用户手动删除不准的标签
     * - AI推断的标签删除后无法自动恢复
     *
     * @param id 标签ID
     * @return 操作结果
     */
    suspend fun deleteTag(id: Long): Result<Unit>

    /**
     * 更新脑标签
     *
     * 业务规则:
     * - 支持修改标签内容（content）和类型（type）
     * - source 字段保持不变（不改变标签来源）
     * - isConfirmed 字段保持不变
     * - 用于 BUG-00066 画像标签编辑功能
     *
     * @param tag 更新后的脑标签（只使用 id、content、type 字段）
     * @return 操作结果
     */
    suspend fun updateTag(tag: BrainTag): Result<Unit>
}
