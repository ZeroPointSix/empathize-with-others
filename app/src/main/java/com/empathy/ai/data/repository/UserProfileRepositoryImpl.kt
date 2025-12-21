package com.empathy.ai.data.repository

import com.empathy.ai.data.local.UserProfilePreferences
import com.empathy.ai.di.IoDispatcher
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
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfilePreferences: UserProfilePreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserProfileRepository {
    
    override suspend fun getUserProfile(): Result<UserProfile> {
        return withContext(ioDispatcher) {
            try {
                userProfilePreferences.loadUserProfile()
            } catch (e: Exception) {
                Result.failure(Exception("获取用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                userProfilePreferences.saveUserProfile(profile)
            } catch (e: Exception) {
                Result.failure(Exception("更新用户画像失败: ${e.message}", e))
            }
        }
    }
    
    override suspend fun clearUserProfile(): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                userProfilePreferences.clearUserProfile()
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
                userProfilePreferences.importUserProfile(json)
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
