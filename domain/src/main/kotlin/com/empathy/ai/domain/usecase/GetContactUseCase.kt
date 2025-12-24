package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 获取单个联系人用例
 *
 * 职责：
 * 1. 根据联系人ID获取联系人详情
 * 2. 统一错误处理
 * 3. 返回Result类型便于UI层处理
 */
class GetContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * 执行用例
     *
     * @param contactId 联系人ID
     * @return Result<ContactProfile?> 成功时返回联系人详情，失败时返回异常
     */
    suspend operator fun invoke(contactId: String): Result<ContactProfile?> {
        return try {
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("联系人ID不能为空"))
            }

            contactRepository.getProfile(contactId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
