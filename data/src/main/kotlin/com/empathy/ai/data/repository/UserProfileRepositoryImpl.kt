package com.empathy.ai.data.repository

import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.data.local.UserProfileCache
import com.empathy.ai.data.local.UserProfilePreferences
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户画像仓库实现
 *
 * 实现UserProfileRepository接口，提供用户画像数据的访问和操作。
 * 所有IO操作在后台线程执行。
 * 
 * 实现缓存优先策略：
 * 1. 检查缓存是否有效
 * 2. 缓存有效则直接返回缓存数据
 * 3. 缓存无效则从存储加载并更新缓存
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfilePreferences: UserProfilePreferences,
    private val userProfileCache: UserProfileCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserProfileRepository {
    
    override suspend fun getUserProfile(forceRefresh: Boolean): Result<UserProfile> {
        return withContext(ioDispatcher) {
            try {
                // 1. 检查缓存（非强制刷新时）
                if (!forceRefresh && userProfileCache.isValid()) {
                    userProfileCache.get()?.let { cachedProfile ->
                        return@withContext Result.success(cachedProfile)
                    }
                }
                
                // 2. 从存储加载
                val result = userProfilePreferences.loadUserProfile()
                
                // 3. 更新缓存（加载成功时）
                if (result.isSuccess) {
                    result.getOrNull()?.let { profile ->
                        userProfileCache.set(profile)
                    }
                }
                
                result
            } catch (e: Exception) {
                Result.failure(Exception("获取用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val result = userProfilePreferences.saveUserProfile(profile)
                // 更新缓存
                if (result.isSuccess) {
                    userProfileCache.set(profile)
                }
                result
            } catch (e: Exception) {
                Result.failure(Exception("更新用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun clearUserProfile(): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val result = userProfilePreferences.clearUserProfile()
                // 清除缓存
                if (result.isSuccess) {
                    userProfileCache.invalidate()
                }
                result
            } catch (e: Exception) {
                Result.failure(Exception("清除用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun exportUserProfile(): Result<String> {
        return withContext(ioDispatcher) {
            try {
                userProfilePreferences.exportUserProfile()
            } catch (e: Exception) {
                Result.failure(Exception("导出用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun importUserProfile(json: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val result = userProfilePreferences.importUserProfile(json)
                // 导入后清除缓存，下次获取时重新加载
                if (result.isSuccess) {
                    userProfileCache.invalidate()
                }
                result
            } catch (e: Exception) {
                Result.failure(Exception("导入用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun hasUserProfile(): Boolean {
        return withContext(ioDispatcher) {
            try {
                userProfilePreferences.hasUserProfile()
            } catch (e: Exception) {
                false
            }
        }
    }
}
