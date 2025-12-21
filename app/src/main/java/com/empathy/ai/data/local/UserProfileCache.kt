package com.empathy.ai.data.local

import com.empathy.ai.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户画像多级缓存
 *
 * 实现内存缓存机制，减少磁盘IO操作，提升性能。
 * L1缓存：内存缓存，有效期1分钟
 */
@Singleton
class UserProfileCache @Inject constructor() {
    
    companion object {
        /** L1缓存有效期：1分钟 */
        private const val L1_CACHE_TTL_MS = 60_000L
    }
    
    @Volatile
    private var cachedProfile: UserProfile? = null
    
    @Volatile
    private var cacheTimestamp: Long = 0L
    
    private val lock = Any()
    
    /**
     * 获取缓存的用户画像
     *
     * @return 缓存的用户画像，如果缓存无效返回null
     */
    fun get(): UserProfile? {
        synchronized(lock) {
            if (!isValid()) {
                return null
            }
            return cachedProfile
        }
    }
    
    /**
     * 设置缓存的用户画像
     *
     * @param profile 要缓存的用户画像
     */
    fun set(profile: UserProfile) {
        synchronized(lock) {
            cachedProfile = profile
            cacheTimestamp = System.currentTimeMillis()
        }
    }
    
    /**
     * 使缓存失效
     */
    fun invalidate() {
        synchronized(lock) {
            cachedProfile = null
            cacheTimestamp = 0L
        }
    }
    
    /**
     * 检查缓存是否有效
     *
     * @return 缓存是否有效
     */
    fun isValid(): Boolean {
        synchronized(lock) {
            if (cachedProfile == null) {
                return false
            }
            val elapsed = System.currentTimeMillis() - cacheTimestamp
            return elapsed < L1_CACHE_TTL_MS
        }
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupExpiredCache() {
        synchronized(lock) {
            if (!isValid()) {
                cachedProfile = null
                cacheTimestamp = 0L
            }
        }
    }
    
    /**
     * 获取缓存状态信息（用于调试）
     *
     * @return 缓存状态描述
     */
    fun getCacheStatus(): String {
        synchronized(lock) {
            val hasCache = cachedProfile != null
            val isValid = isValid()
            val age = if (cacheTimestamp > 0) {
                System.currentTimeMillis() - cacheTimestamp
            } else {
                -1L
            }
            return "UserProfileCache[hasCache=$hasCache, isValid=$isValid, ageMs=$age]"
        }
    }
}
