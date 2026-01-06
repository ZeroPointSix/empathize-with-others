package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有联系人列表用例
 *
 * 负责提供联系人列表的响应式数据流，支持实时更新。
 *
 * 业务背景 (PRD-00003):
 * - 联系人画像是AI分析的基础数据源
 * - 使用Flow支持响应式数据更新，UI自动感知变化
 *
 * 设计决策 (TDD-00003):
 * - 直接委托给Repository，不做额外处理
 * - 返回Flow而非List，支持实时更新
 *
 * @return Flow<List<ContactProfile>> 联系人列表数据流，支持实时更新
 * @see ContactRepository 联系人仓库接口
 */
class GetAllContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * 执行用例
     *
     * @return Flow<List<ContactProfile>> 联系人列表数据流，支持实时更新
     */
    operator fun invoke(): Flow<List<ContactProfile>> {
        return contactRepository.getAllProfiles()
    }
}
