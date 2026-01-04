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
 * UserProfileRepositoryImpl 实现了用户画像的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00003)用户画像管理
 *   - 用户基础信息：头像、昵称、社交账号等
 *   - 用户偏好设置：AI交互风格、通知设置等
 *   - 缓存策略：减少频繁的存储访问，提升性能
 *
 * 【设计决策】(TDD-00003)
 *   - 缓存优先策略：优先使用缓存，缓存失效时从存储加载
 *   - UserProfileCache：内存缓存，有效期内直接返回
 *   - UserProfilePreferences：持久化存储用户配置
 *
 * 【关键逻辑】
 *   - getUserProfile：缓存有效则返回缓存，否则从存储加载
 *   - saveUserProfile：更新存储并刷新缓存
 *   - clearCache：手动清除缓存，强制下次从存储加载
 *
 * 【任务追踪】
 *   - FD-00003/Task-001: 用户画像基础CRUD
 *   - FD-00003/Task-002: 缓存策略实现
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
