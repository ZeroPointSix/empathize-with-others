package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * 获取用户画像用例
 *
 * 从Repository获取用户画像数据。
 * 缓存逻辑由Repository实现类内部处理。
 */
class GetUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * 获取用户画像
     *
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @return 获取结果，成功返回UserProfile，失败返回异常
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<UserProfile> {
        return userProfileRepository.getUserProfile(forceRefresh)
    }
}
