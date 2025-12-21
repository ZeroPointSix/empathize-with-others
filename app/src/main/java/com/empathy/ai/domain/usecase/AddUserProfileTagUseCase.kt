package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileValidationResult
import com.empathy.ai.domain.util.UserProfileValidator
import javax.inject.Inject

/**
 * 添加用户画像标签用例
 *
 * 验证并添加标签到指定维度。
 */
class AddUserProfileTagUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val validator: UserProfileValidator
) {
    /**
     * 添加标签到指定维度
     *
     * @param dimensionKey 维度键名
     * @param tag 要添加的标签
     * @return 添加结果，成功返回更新后的UserProfile，失败返回异常
     */
    suspend operator fun invoke(dimensionKey: String, tag: String): Result<UserProfile> {
        // 1. 获取当前画像
        val currentResult = getUserProfileUseCase()
        if (currentResult.isFailure) {
            return Result.failure(
                currentResult.exceptionOrNull() ?: Exception("获取用户画像失败")
            )
        }
        val currentProfile = currentResult.getOrThrow()
        
        // 2. 清理输入
        val sanitizedTag = validator.sanitizeInput(tag)
        
        // 3. 综合验证
        val validation = validator.validateAddTag(currentProfile, dimensionKey, sanitizedTag)
        if (!validation.isValid()) {
            return Result.failure(Exception(validation.getErrorMessage()))
        }
        
        // 4. 添加标签
        val updatedProfile = currentProfile.addTag(dimensionKey, sanitizedTag)
        
        // 5. 保存更新
        return updateUserProfileUseCase(updatedProfile)
    }
}
