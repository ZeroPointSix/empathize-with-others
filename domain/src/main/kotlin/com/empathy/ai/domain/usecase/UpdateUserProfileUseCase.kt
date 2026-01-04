package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * 更新用户画像用例
 *
 * 负责更新用户画像数据。
 *
 * 设计决策:
 *   - 自动更新: 更新时间戳 updatedAt = System.currentTimeMillis()
 *   - 缓存处理: 缓存一致性由 Repository 实现类内部处理
 *   - 返回结果: 成功返回更新后的 UserProfile，失败返回异常
 *
 * @see UserProfileRepository.updateUserProfile 仓库更新方法
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * 更新用户画像
     *
     * @param profile 要更新的用户画像
     * @return 更新结果，成功返回更新后的UserProfile，失败返回异常
     */
    suspend operator fun invoke(profile: UserProfile): Result<UserProfile> {
        // 1. 更新时间戳
        val updatedProfile = profile.copy(
            updatedAt = System.currentTimeMillis()
        )
        
        // 2. 保存到存储
        val result = userProfileRepository.updateUserProfile(updatedProfile)
        
        // 3. 返回结果
        return if (result.isSuccess) {
            Result.success(updatedProfile)
        } else {
            Result.failure(
                result.exceptionOrNull() ?: Exception("更新用户画像失败")
            )
        }
    }
}
