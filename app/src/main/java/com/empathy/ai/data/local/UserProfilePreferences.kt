package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.empathy.ai.domain.model.UserProfile
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户画像本地加密存储
 *
 * 使用EncryptedSharedPreferences加密存储用户画像数据，
 * 符合项目宪法隐私优先原则。
 * 
 * 容错机制 (BUG-00028 修复)：
 * - 完全延迟初始化：构造函数不访问 Keystore，只在首次使用时初始化
 * - 重试机制：Keystore 服务不可用时自动重试（最多3次，递增延迟）
 * - 降级策略：多次重试失败后使用普通 SharedPreferences
 * - 线程安全：使用 synchronized 确保并发安全
 */
@Singleton
class UserProfilePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    companion object {
        private const val TAG = "UserProfilePreferences"
        private const val PREFS_NAME = "user_profile_prefs"
        private const val FALLBACK_PREFS_NAME = "user_profile_prefs_fallback"
        private const val KEY_USER_PROFILE = "user_profile"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 200L
    }
    
    /**
     * 标记加密存储是否可用
     */
    @Volatile
    private var isEncryptionAvailable = true
    
    /**
     * 初始化锁，确保线程安全
     */
    private val initLock = Any()
    
    /**
     * SharedPreferences 实例（延迟初始化）
     */
    private var _prefs: SharedPreferences? = null
    
    /**
     * 获取 SharedPreferences 实例
     * 
     * 首次访问时会触发初始化，包括重试和降级逻辑
     */
    private val prefs: SharedPreferences
        get() {
            if (_prefs != null) return _prefs!!
            synchronized(initLock) {
                if (_prefs == null) {
                    _prefs = initializePrefs()
                    Log.d(TAG, "SharedPreferences 初始化完成，加密可用: $isEncryptionAvailable")
                }
                return _prefs!!
            }
        }
    
    /**
     * 初始化 SharedPreferences
     */
    private fun initializePrefs(): SharedPreferences {
        Log.d(TAG, "开始初始化 SharedPreferences...")
        
        val masterKey = createMasterKeyWithRetry()
        if (masterKey != null) {
            try {
                val encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                Log.d(TAG, "EncryptedSharedPreferences 创建成功")
                isEncryptionAvailable = true
                return encryptedPrefs
            } catch (e: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences 创建失败", e)
            }
        }
        
        Log.w(TAG, "降级使用普通 SharedPreferences，用户画像将以明文存储")
        isEncryptionAvailable = false
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 创建 MasterKey，带重试机制
     */
    private fun createMasterKeyWithRetry(): MasterKey? {
        var lastException: Exception? = null
        
        for (attempt in 0 until MAX_RETRY_COUNT) {
            try {
                val key = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                Log.d(TAG, "MasterKey 创建成功 (尝试 ${attempt + 1})")
                return key
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "MasterKey 创建失败 (尝试 ${attempt + 1}/$MAX_RETRY_COUNT): ${e.message}")
                
                if (attempt < MAX_RETRY_COUNT - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }
        
        Log.e(TAG, "MasterKey 创建失败，已达最大重试次数", lastException)
        return null
    }
    
    private val adapter by lazy {
        moshi.adapter(UserProfile::class.java)
    }
    
    /**
     * 检查安全存储是否可用
     */
    fun isSecureStorageAvailable(): Boolean {
        prefs // 触发初始化
        return isEncryptionAvailable
    }
    
    /**
     * 保存用户画像
     *
     * @param profile 要保存的用户画像
     * @return 保存结果，成功返回Unit，失败返回异常
     */
    fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
            val json = adapter.toJson(updatedProfile)
            prefs.edit().putString(KEY_USER_PROFILE, json).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "保存用户画像失败", e)
            Result.failure(Exception("保存用户画像失败: ${e.message}", e))
        }
    }
    
    /**
     * 加载用户画像
     *
     * @return 加载结果，成功返回UserProfile，失败返回异常
     */
    fun loadUserProfile(): Result<UserProfile> {
        return try {
            val json = prefs.getString(KEY_USER_PROFILE, null)
            val profile = if (json != null) {
                adapter.fromJson(json) ?: UserProfile()
            } else {
                UserProfile()
            }
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "加载用户画像失败", e)
            Result.failure(Exception("加载用户画像失败: ${e.message}", e))
        }
    }
    
    /**
     * 清除用户画像
     *
     * @return 清除结果，成功返回Unit，失败返回异常
     */
    fun clearUserProfile(): Result<Unit> {
        return try {
            prefs.edit().remove(KEY_USER_PROFILE).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "清除用户画像失败", e)
            Result.failure(Exception("清除用户画像失败: ${e.message}", e))
        }
    }
    
    /**
     * 导出用户画像数据
     *
     * @return 导出结果，成功返回JSON字符串，失败返回异常
     */
    fun exportUserProfile(): Result<String> {
        return try {
            val profileResult = loadUserProfile()
            if (profileResult.isFailure) {
                return Result.failure(
                    profileResult.exceptionOrNull() ?: Exception("加载用户画像失败")
                )
            }
            
            val profile = profileResult.getOrThrow()
            val json = adapter.toJson(profile)
            Result.success(json)
        } catch (e: Exception) {
            Log.e(TAG, "导出用户画像失败", e)
            Result.failure(Exception("导出用户画像失败: ${e.message}", e))
        }
    }
    
    /**
     * 导入用户画像数据
     *
     * @param json JSON格式的用户画像数据
     * @return 导入结果，成功返回Unit，失败返回异常
     */
    fun importUserProfile(json: String): Result<Unit> {
        return try {
            val profile = adapter.fromJson(json)
                ?: return Result.failure(Exception("JSON解析失败：数据格式无效"))
            saveUserProfile(profile)
        } catch (e: Exception) {
            Log.e(TAG, "导入用户画像失败", e)
            Result.failure(Exception("导入用户画像失败: ${e.message}", e))
        }
    }
    
    /**
     * 检查是否存在用户画像数据
     *
     * @return 是否存在数据
     */
    fun hasUserProfile(): Boolean {
        return try {
            prefs.contains(KEY_USER_PROFILE)
        } catch (e: Exception) {
            Log.e(TAG, "检查用户画像失败", e)
            false
        }
    }
}
