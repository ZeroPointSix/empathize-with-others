package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import kotlinx.coroutines.flow.Flow

/**
 * 联系人画像仓库接口
 *
 * 服务对象: 联系人列表、画像详情、RAG 上下文构建
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
     * [业务用] 创建或完全覆盖一个画像
     *
     * @param profile 要保存的联系人画像
     * @return 操作结果
     */
    suspend fun saveProfile(profile: ContactProfile): Result<Unit>

    /**
     * 更新联系人的事实字段（旧版兼容方法）
     *
     * [数据喂养用] 仅追加或更新事实字段 (增量更新)
     * 场景: AI 从聊天记录中分析出了新爱好，只需存入 {"爱好": "滑雪"}
     *
     * @param contactId 联系人 ID
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
}
