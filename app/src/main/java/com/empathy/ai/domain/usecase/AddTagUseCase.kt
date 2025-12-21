package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileValidationResult
import com.empathy.ai.domain.util.UserProfileValidator
import javax.inject.Inject

/**
 * 添加标签用例
 *
 * 负责向用户画像的指定维度添加标签。
 * 包含完整的验证逻辑：标签内容验证、重复检查、数量限制检查。
 */
class AddTagUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val validator: UserProfileValidator
) {
    /**
     * 添加标签到指定维度
     *
     * @param dimensionKey 维度键名（基础维度使用枚举的name，自定义维度使用维度名称）
     * @param tag 要添加的标签内容
     * @return 添加结果，成功返回更新后的UserProfile，失败返回异常
     */
    suspend operator fun invoke(
        dimensionKey: String,
        tag: String
    ): Result<UserProfile> {
        // 1. 获取当前画像
        val profileResult = getUserProfileUseCase()
        if (profileResult.isFailure) {
            return Result.failure(
                profileResult.exceptionOrNull() ?: Exception("获取用户画像失败")
            )
        }
        val profile = profileResult.getOrThrow()
        
        // 2. 清理输入
        val sanitizedTag = validator.sanitizeInput(tag)
        
        // 3. 综合验证
        val validationResult = validator.validateAddTag(profile, dimensionKey, sanitizedTag)
        if (!validationResult.isValid()) {
            return Result.failure(
                ValidationException(validationResult)
            )
        }
        
        // 4. 添加标签
        val updatedProfile = profile.addTag(dimensionKey, sanitizedTag)
        
        // 5. 保存更新
        return updateUserProfileUseCase(updatedProfile)
    }
    
    /**
     * 验证异常
     *
     * 用于封装验证失败的详细信息
     */
    class ValidationException(
        val validationResult: UserProfileValidationResult
    ) : Exception(validationResult.getErrorMessage())
}
