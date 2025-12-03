package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.BrainTag
import kotlinx.coroutines.flow.Flow

/**
 * 策略标签仓库接口
 *
 * 服务对象: 实时风控检测、策略分析
 */
interface BrainTagRepository {
    /**
     * 获取指定联系人的所有策略标签
     *
     * [业务用] RAG 核心: 获取某人的所有"锦囊" (Flow 实时监听)
     *
     * @param contactId 联系人 ID
     * @return 该联系人的所有策略标签的 Flow
     */
    fun getTagsForContact(contactId: String): Flow<List<BrainTag>>

    /**
     * 获取全库的雷区标签
     *
     * [风控用] 用于全局风控，或者无特定联系人时的通用检测
     *
     * @return 包含所有雷区标签列表的 Result
     */
    suspend fun getAllRedFlags(): Result<List<BrainTag>>

    /**
     * 保存策略标签
     *
     * [数据喂养] AI 分析出新策略/用户手动添加
     *
     * @param tag 要保存的策略标签
     * @return 包含新插入的 ID 的 Result
     */
    suspend fun saveTag(tag: BrainTag): Result<Long>

    /**
     * 删除策略标签
     *
     * [UI用] 用户手动删掉不准的标签
     *
     * @param id 标签 ID
     * @return 操作结果
     */
    suspend fun deleteTag(id: Long): Result<Unit>
}
