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
        private const val L1_CACHE_TTL_MS = 60_000L
    }
    
    @Volatile
    private var cachedProfile: UserProfile? = null
    
    @Volatile
    private var cacheTimestamp: Long = 0L
    
    private val lock = Any()
    
    fun get(): UserProfile? {
        synchronized(lock) {
            if (!isValid()) return null
            return cachedProfile
        }
    }
    
    fun set(profile: UserProfile) {
        synchronized(lock) {
            cachedProfile = profile
            cacheTimestamp = System.currentTimeMillis()
        }
    }
    
    fun invalidate() {
        synchronized(lock) {
            cachedProfile = null
            cacheTimestamp = 0L
        }
    }
    
    fun isValid(): Boolean {
        synchronized(lock) {
            if (cachedProfile == null) return false
            val elapsed = System.currentTimeMillis() - cacheTimestamp
            return elapsed < L1_CACHE_TTL_MS
        }
    }
    
    fun cleanupExpiredCache() {
        synchronized(lock) {
            if (!isValid()) {
                cachedProfile = null
                cacheTimestamp = 0L
            }
        }
    }
    
    fun getCacheStatus(): String {
        synchronized(lock) {
            val hasCache = cachedProfile != null
            val isValid = isValid()
            val age = if (cacheTimestamp > 0) System.currentTimeMillis() - cacheTimestamp else -1L
            return "UserProfileCache[hasCache=$hasCache, isValid=$isValid, ageMs=$age]"
        }
    }
}
