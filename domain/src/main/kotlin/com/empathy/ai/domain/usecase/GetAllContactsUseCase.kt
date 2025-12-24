package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有联系人列表用例
 *
 * 职责：
 * 1. 获取所有联系人数据流
 * 2. 提供响应式数据源
 * 3. 支持实时更新
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
