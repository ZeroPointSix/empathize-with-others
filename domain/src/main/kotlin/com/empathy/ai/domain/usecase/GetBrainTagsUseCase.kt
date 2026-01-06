package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.repository.BrainTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 获取联系人标签用例
 *
 * 负责获取指定联系人的大脑标签列表。
 *
 * 业务背景:
 * - BrainTag用于标记沟通雷区和策略建议
 * - 分为RISK_RED（雷区）和STRATEGY_GREEN（策略）两种类型
 *
 * 设计决策:
 * - 空contactId返回空列表而非异常，提供更稳定的行为
 * - 使用Flow支持实时更新
 *
 * @param contactId 联系人ID
 * @return Flow<List<BrainTag>> 标签列表数据流，支持实时更新
 * @see BrainTagRepository 标签仓库接口
 */
class GetBrainTagsUseCase @Inject constructor(
    private val brainTagRepository: BrainTagRepository
) {
    /**
     * 执行用例
     *
     * @param contactId 联系人ID
     * @return Flow<List<BrainTag>> 标签列表数据流，支持实时更新
     */
    operator fun invoke(contactId: String): Flow<List<BrainTag>> {
        if (contactId.isBlank()) {
            // 返回空数据流而不是抛出异常，提供更稳定的行为
            return flowOf(emptyList())
        }

        return brainTagRepository.getTagsForContact(contactId)
    }
}
