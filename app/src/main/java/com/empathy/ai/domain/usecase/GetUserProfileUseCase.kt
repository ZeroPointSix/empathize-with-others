package com.empathy.ai.domain.usecase

import com.empathy.ai.data.local.UserProfileCache
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * 获取用户画像用例
 *
 * 实现缓存优先策略：
 * 1. 检查缓存是否有效
 * 2. 缓存有效则直接返回缓存数据
 * 3. 缓存无效则从存储加载并更新缓存
 */
class GetUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val userProfileCache: UserProfileCache
) {
    /**
     * 获取用户画像
     *
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @return 获取结果，成功返回UserProfile，失败返回异常
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<UserProfile> {
        // 1. 检查缓存（非强制刷新时）
        if (!forceRefresh && userProfileCache.isValid()) {
            userProfileCache.get()?.let { cachedProfile ->
                return Result.success(cachedProfile)
            }
        }
        
        // 2. 从存储加载
        val result = userProfileRepository.getUserProfile()
        
        // 3. 更新缓存（加载成功时）
        if (result.isSuccess) {
            result.getOrNull()?.let { profile ->
                userProfileCache.set(profile)
            }
        }
        
        return result
    }
}
