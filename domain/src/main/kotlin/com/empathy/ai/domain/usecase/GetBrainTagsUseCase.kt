package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.repository.BrainTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 获取联系人标签用例
 *
 * 职责：
 * 1. 根据联系人ID获取标签列表
 * 2. 提供响应式数据流
 * 3. 支持实时更新
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
