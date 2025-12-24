package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key 加密存储
 *
 * 使用 Android Jetpack Security 的 EncryptedSharedPreferences 实现 API Key 的安全存储。
 *
 * 安全特性:
 * - 使用 AES256_GCM 加密算法
 * - 密钥存储在 Android Keystore (硬件级加密)
 * - 自动处理密钥轮换和备份
 * - 完全延迟初始化 + 重试机制，避免 Keystore 服务未就绪导致崩溃
 *
 * @property context 应用上下文
 */
@Singleton
class ApiKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ApiKeyStorage"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 200L
        private const val ENCRYPTED_PREFS_NAME = "api_keys"
        private const val FALLBACK_PREFS_NAME = "api_keys_fallback"
    }

    @Volatile
    private var isEncryptionAvailable = true

    @Volatile
    private var isInitialized = false

    private val initLock = Any()

    private var _prefs: SharedPreferences? = null

    private val prefs: SharedPreferences
        get() {
            if (_prefs != null) return _prefs!!
            synchronized(initLock) {
                if (_prefs == null) {
                    _prefs = initializePrefs()
                    isInitialized = true
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
                    context,
                    ENCRYPTED_PREFS_NAME,
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
        Log.w(TAG, "降级使用普通 SharedPreferences，API Key 将以明文存储")
        isEncryptionAvailable = false
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

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
                        Log.w(TAG, "重试被中断")
                        break
                    }
                }
            }
        }
        Log.e(TAG, "MasterKey 创建失败，已达最大重试次数", lastException)
        return null
    }

    fun isSecureStorageAvailable(): Boolean {
        prefs
        return isEncryptionAvailable
    }

    fun isInitialized(): Boolean = isInitialized

    fun save(key: String, apiKey: String) {
        try {
            prefs.edit().putString(key, apiKey).apply()
            Log.d(TAG, "API Key 保存成功: $key")
        } catch (e: Exception) {
            Log.e(TAG, "保存 API Key 失败: ${e.message}")
        }
    }

    fun get(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            Log.e(TAG, "读取 API Key 失败: ${e.message}")
            null
        }
    }

    fun delete(key: String) {
        try {
            prefs.edit().remove(key).apply()
            Log.d(TAG, "API Key 删除成功: $key")
        } catch (e: Exception) {
            Log.e(TAG, "删除 API Key 失败: ${e.message}")
        }
    }

    fun mask(apiKey: String): String {
        return when {
            apiKey.length <= 8 -> "****"
            else -> "${apiKey.take(4)}****${apiKey.takeLast(4)}"
        }
    }

    fun generateKey(providerId: String): String {
        return "api_key_$providerId"
    }
}
