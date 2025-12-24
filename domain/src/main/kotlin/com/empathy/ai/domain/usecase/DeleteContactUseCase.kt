package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 删除联系人用例
 *
 * 职责：
 * 1. 根据联系人ID删除联系人
 * 2. 验证输入参数
 * 3. 统一错误处理
 */
class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * 执行用例
     *
     * @param contactId 联系人ID
     * @return Result<Unit> 成功时返回Unit，失败时返回异常
     */
    suspend operator fun invoke(contactId: String): Result<Unit> {
        return try {
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("联系人ID不能为空"))
            }

            contactRepository.deleteProfile(contactId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
