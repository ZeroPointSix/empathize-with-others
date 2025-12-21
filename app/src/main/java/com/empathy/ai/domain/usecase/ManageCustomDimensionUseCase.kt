package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileValidationResult
import com.empathy.ai.domain.util.UserProfileValidator
import javax.inject.Inject

/**
 * 管理自定义维度用例
 *
 * 负责添加和删除用户画像的自定义维度。
 * 删除维度时会级联删除该维度下的所有标签。
 */
class ManageCustomDimensionUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val validator: UserProfileValidator
) {
    /**
     * 添加自定义维度
     *
     * @param dimensionName 维度名称
     * @return 添加结果，成功返回更新后的UserProfile，失败返回异常
     */
    suspend fun addDimension(dimensionName: String): Result<UserProfile> {
        // 1. 获取当前画像
        val profileResult = getUserProfileUseCase()
        if (profileResult.isFailure) {
            return Result.failure(
                profileResult.exceptionOrNull() ?: Exception("获取用户画像失败")
            )
        }
        val profile = profileResult.getOrThrow()
        
        // 2. 清理输入
        val sanitizedName = validator.sanitizeInput(dimensionName)
        
        // 3. 综合验证
        val validationResult = validator.validateAddDimension(profile, sanitizedName)
        if (!validationResult.isValid()) {
            return Result.failure(
                ValidationException(validationResult)
            )
        }
        
        // 4. 添加维度
        val updatedProfile = profile.addCustomDimension(sanitizedName)
        
        // 5. 保存更新
        return updateUserProfileUseCase(updatedProfile)
    }
    
    /**
     * 删除自定义维度
     *
     * 删除维度时会级联删除该维度下的所有标签。
     * 实现幂等性：维度不存在时也返回成功。
     *
     * @param dimensionName 维度名称
     * @return 删除结果，成功返回更新后的UserProfile，失败返回异常
     */
    suspend fun removeDimension(dimensionName: String): Result<UserProfile> {
        // 1. 获取当前画像
        val profileResult = getUserProfileUseCase()
        if (profileResult.isFailure) {
            return Result.failure(
                profileResult.exceptionOrNull() ?: Exception("获取用户画像失败")
            )
        }
        val profile = profileResult.getOrThrow()
        
        // 2. 检查维度是否存在（幂等性：不存在也返回成功）
        if (!profile.customDimensions.containsKey(dimensionName)) {
            return Result.success(profile)
        }
        
        // 3. 删除维度（包含其所有标签）
        val updatedProfile = profile.removeCustomDimension(dimensionName)
        
        // 4. 保存更新
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
