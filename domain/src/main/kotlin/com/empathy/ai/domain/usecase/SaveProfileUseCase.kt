package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 保存联系人画像用例
 *
 * 触发场景: 用户在设置页创建或编辑联系人画像时
 *
 * 功能: 验证并保存联系人画像到本地数据库
 */
class SaveProfileUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * 执行保存联系人画像操作
     *
     * @param profile 要保存的联系人画像
     * @return 操作结果，成功返回 Result.success()，失败返回 Result.failure()
     */
    suspend operator fun invoke(profile: ContactProfile): Result<Unit> {
        return try {
            // 1. 前置检查：验证必填字段
            val validationResult = validateProfile(profile)
            if (validationResult.isFailure) {
                return validationResult
            }

            // 2. 调用仓库保存画像
            contactRepository.saveProfile(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 验证联系人画像的必填字段
     *
     * @param profile 要验证的联系人画像
     * @return 验证结果
     */
    private fun validateProfile(profile: ContactProfile): Result<Unit> {
        return when {
            profile.id.isBlank() -> {
                Result.failure(IllegalArgumentException("联系人ID不能为空"))
            }
            profile.name.isBlank() -> {
                Result.failure(IllegalArgumentException("联系人名称不能为空"))
            }
            else -> {
                Result.success(Unit)
            }
        }
    }
}
