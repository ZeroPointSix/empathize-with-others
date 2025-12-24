package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import javax.inject.Inject

/**
 * 移除标签用例
 *
 * 负责从用户画像的指定维度移除标签。
 * 实现幂等性：标签不存在时也返回成功。
 */
class RemoveTagUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) {
    /**
     * 从指定维度移除标签
     *
     * @param dimensionKey 维度键名（基础维度使用枚举的name，自定义维度使用维度名称）
     * @param tag 要移除的标签内容
     * @return 移除结果，成功返回更新后的UserProfile，失败返回异常
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
        
        // 2. 检查标签是否存在（幂等性：不存在也返回成功）
        val existingTags = profile.getTagsForDimension(dimensionKey)
        if (!existingTags.contains(tag)) {
            // 标签不存在，直接返回当前画像（幂等）
            return Result.success(profile)
        }
        
        // 3. 移除标签
        val updatedProfile = profile.removeTag(dimensionKey, tag)
        
        // 4. 保存更新
        return updateUserProfileUseCase(updatedProfile)
    }
}
