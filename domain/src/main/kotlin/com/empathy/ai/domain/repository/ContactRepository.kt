package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import kotlinx.coroutines.flow.Flow

/**
 * 联系人画像仓储接口
 *
 * 业务背景 (PRD-00003):
 * - 联系人画像是AI分析的核心数据源，包含Facts、关系分数、标签等
 * - 采用三层记忆架构：短期(对话记录)->中期(每日总结)->长期(画像)
 * - 支持RAG检索，为AI分析提供个性化的上下文
 *
 * 设计决策:
 * - Facts带时间戳和来源：追踪信息来源，支持老化策略
 * - 关系分数量化：0-100分，追踪关系变化趋势
 * - 批量更新支持：每日总结时批量更新facts和标签
 *
 * 服务对象: 联系人列表、画像详情、RAG上下文构建
 */
interface ContactRepository {
    /**
     * 获取所有联系人画像
     *
     * [UI用] 首页列表渲染。Flow 保证数据库变动即刷新 UI
     *
     * @return 所有联系人画像的 Flow
     */
    fun getAllProfiles(): Flow<List<ContactProfile>>

    /**
     * 根据 ID 获取单个联系人画像
     *
     * [业务用] 拼 Prompt 时读取单个画像
     *
     * @param id 联系人 ID
     * @return 包含联系人画像或 null 的 Result
     */
    suspend fun getProfile(id: String): Result<ContactProfile?>

    /**
     * 保存联系人画像
     *
     * 业务规则:
     * - 创建或完全覆盖一个画像
     * - 保存时验证必填字段：id、name、targetGoal
     *
     * @param profile 要保存的联系人画像
     * @return 操作结果
     */
    suspend fun saveProfile(profile: ContactProfile): Result<Unit>

    /**
     * 更新联系人的事实字段（旧版兼容方法）
     *
     * 业务规则:
     * - 仅追加或更新事实字段（增量更新）
     * - 场景：AI从聊天记录中分析出新爱好，只需存入{"爱好":"滑雪"}
     * - 新版使用updateFacts方法，支持完整Fact结构
     *
     * @param contactId 联系人ID
     * @param newFacts 新的事实键值对
     * @return 操作结果
     */
    suspend fun updateContactFacts(contactId: String, newFacts: Map<String, String>): Result<Unit>

    /**
     * 删除联系人画像
     *
     * [管理用] 删除联系人
     *
     * @param id 联系人 ID
     * @return 操作结果
     */
    suspend fun deleteProfile(id: String): Result<Unit>

    // ============================================================================
    // 记忆系统扩展方法
    // ============================================================================

    /**
     * 更新联系人的关系分数
     *
     * 业务规则 (PRD-00003):
     * - 分数范围0-100：0-30陌生/冷淡，31-60普通，61-80熟悉，81-100亲密
     * - 每日总结时由AI推断变化（-10到+10）
     * - 手动调用时直接覆盖当前分数
     *
     * @param contactId 联系人ID
     * @param newScore 新的关系分数（0-100）
     * @return 操作结果
     */
    suspend fun updateRelationshipScore(contactId: String, newScore: Int): Result<Unit>

    /**
     * 更新联系人的Facts（新版方法）
     *
     * @param contactId 联系人ID
     * @param facts 新的Facts列表
     * @return 操作结果
     */
    suspend fun updateFacts(contactId: String, facts: List<Fact>): Result<Unit>

    /**
     * 添加单个Fact
     *
     * @param contactId 联系人ID
     * @param fact 要添加的Fact
     * @return 操作结果
     */
    suspend fun addFact(contactId: String, fact: Fact): Result<Unit>

    /**
     * 更新最后互动日期
     *
     * @param contactId 联系人ID
     * @param date 日期字符串，格式"yyyy-MM-dd"
     * @return 操作结果
     */
    suspend fun updateLastInteractionDate(contactId: String, date: String): Result<Unit>

    /**
     * 批量更新联系人数据（事务）
     *
     * 设计权衡: 使用事务保证数据一致性
     * 避免多次数据库调用，减少锁竞争
     *
     * 业务规则:
     * - facts、relationshipScore、lastInteractionDate均为可选参数
     * - 只更新传入的参数，未传入的参数保持不变
     * - 每日总结后调用，更新facts、分数、互动日期
     *
     * @param contactId 联系人ID
     * @param facts 新的Facts列表（可选）
     * @param relationshipScore 新的关系分数（可选）
     * @param lastInteractionDate 最后互动日期（可选）
     * @return 操作结果
     */
    suspend fun updateContactData(
        contactId: String,
        facts: List<Fact>? = null,
        relationshipScore: Int? = null,
        lastInteractionDate: String? = null
    ): Result<Unit>

    /**
     * 更新联系人画像（完整更新）
     *
     * @param profile 更新后的联系人画像
     * @return 操作结果
     */
    suspend fun updateProfile(profile: ContactProfile): Result<Unit>

    // ============================================================================
    // 编辑追踪扩展方法（v10）
    // ============================================================================

    /**
     * 更新联系人姓名（编辑追踪）
     *
     * 业务规则:
     * - 仅首次编辑时保存原始姓名
     * - 记录修改时间，用于编辑历史追踪
     *
     * @param contactId 联系人ID
     * @param newName 新的姓名
     * @param modifiedTime 修改时间
     * @param originalName 原始姓名（仅首次编辑时保存）
     * @return 受影响的行数
     */
    suspend fun updateName(
        contactId: String,
        newName: String,
        modifiedTime: Long,
        originalName: String
    ): Int

    /**
     * 更新联系人目标（编辑追踪）
     *
     * @param contactId 联系人ID
     * @param newGoal 新的目标
     * @param modifiedTime 修改时间
     * @param originalGoal 原始目标（仅首次编辑时保存）
     * @return 受影响的行数
     */
    suspend fun updateGoal(
        contactId: String,
        newGoal: String,
        modifiedTime: Long,
        originalGoal: String
    ): Int
}
