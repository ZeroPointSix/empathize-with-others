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
    
    @Volatile
    private var isEncryptionAvailable = true
    
    private val initLock = Any()
    
    private var _prefs: SharedPreferences? = null
    
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

    
    private fun initializePrefs(): SharedPreferences {
        Log.d(TAG, "开始初始化 SharedPreferences...")
        val masterKey = createMasterKeyWithRetry()
        if (masterKey != null) {
            try {
                val encryptedPrefs = EncryptedSharedPreferences.create(
                    context, PREFS_NAME, masterKey,
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
    
    private fun createMasterKeyWithRetry(): MasterKey? {
        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRY_COUNT) {
            try {
                val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
                Log.d(TAG, "MasterKey 创建成功 (尝试 ${attempt + 1})")
                return key
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "MasterKey 创建失败 (尝试 ${attempt + 1}/$MAX_RETRY_COUNT): ${e.message}")
                if (attempt < MAX_RETRY_COUNT - 1) {
                    try { Thread.sleep(RETRY_DELAY_MS * (attempt + 1)) }
                    catch (ie: InterruptedException) { Thread.currentThread().interrupt(); break }
                }
            }
        }
        Log.e(TAG, "MasterKey 创建失败，已达最大重试次数", lastException)
        return null
    }
    
    private val adapter by lazy { moshi.adapter(UserProfile::class.java) }
    
    fun isSecureStorageAvailable(): Boolean { prefs; return isEncryptionAvailable }


    fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
            prefs.edit().putString(KEY_USER_PROFILE, adapter.toJson(updatedProfile)).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "保存用户画像失败", e)
            Result.failure(Exception("保存用户画像失败: ${e.message}", e))
        }
    }
    
    fun loadUserProfile(): Result<UserProfile> {
        return try {
            val json = prefs.getString(KEY_USER_PROFILE, null)
            val profile = if (json != null) adapter.fromJson(json) ?: UserProfile() else UserProfile()
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "加载用户画像失败", e)
            Result.failure(Exception("加载用户画像失败: ${e.message}", e))
        }
    }
    
    fun clearUserProfile(): Result<Unit> {
        return try {
            prefs.edit().remove(KEY_USER_PROFILE).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "清除用户画像失败", e)
            Result.failure(Exception("清除用户画像失败: ${e.message}", e))
        }
    }
    
    fun exportUserProfile(): Result<String> {
        return try {
            val profileResult = loadUserProfile()
            if (profileResult.isFailure) {
                return Result.failure(profileResult.exceptionOrNull() ?: Exception("加载用户画像失败"))
            }
            Result.success(adapter.toJson(profileResult.getOrThrow()))
        } catch (e: Exception) {
            Log.e(TAG, "导出用户画像失败", e)
            Result.failure(Exception("导出用户画像失败: ${e.message}", e))
        }
    }
    
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
    
    fun hasUserProfile(): Boolean {
        return try { prefs.contains(KEY_USER_PROFILE) }
        catch (e: Exception) { Log.e(TAG, "检查用户画像失败", e); false }
    }
}
