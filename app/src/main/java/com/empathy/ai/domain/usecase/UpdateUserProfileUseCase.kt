package com.empathy.ai.domain.usecase

import com.empathy.ai.data.local.UserProfileCache
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * 更新用户画像用例
 *
 * 负责更新用户画像数据，同时维护缓存一致性。
 * 自动更新updatedAt时间戳。
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val userProfileCache: UserProfileCache
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
        
        // 3. 更新缓存（保存成功时）
        if (result.isSuccess) {
            userProfileCache.set(updatedProfile)
            return Result.success(updatedProfile)
        }
        
        return Result.failure(
            result.exceptionOrNull() ?: Exception("更新用户画像失败")
        )
    }
}
