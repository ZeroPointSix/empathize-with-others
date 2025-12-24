package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import javax.inject.Inject

/**
 * 移除用户画像标签用例
 *
 * 从指定维度移除标签。
 */
class RemoveUserProfileTagUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) {
    /**
     * 从指定维度移除标签
     *
     * @param dimensionKey 维度键名
     * @param tag 要移除的标签
     * @return 移除结果，成功返回更新后的UserProfile，失败返回异常
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
        
        // 2. 移除标签（幂等操作，标签不存在也返回成功）
        val updatedProfile = currentProfile.removeTag(dimensionKey, tag)
        
        // 3. 保存更新
        return updateUserProfileUseCase(updatedProfile)
    }
}
