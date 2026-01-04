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
 * 【BYOK (Bring Your Own Key) 原则的具体实现】
 * 应用不持有用户密钥，密钥由用户提供并直接存储在本地。
 * 加密存储确保：
 * - 即使设备被攻破，密钥也不会轻易泄露
 * - 符合隐私优先原则
 *
 * 【重试机制的设计意图】
 * MasterKey创建可能因系统状态短暂不稳定而失败：
 * - 第一次失败：立即重试
 * - 第二次失败：等待200ms后重试
 * - 第三次失败：放弃，加密不可用
 *
 * 这种"指数退避"策略在重试次数和响应延迟之间取得平衡。
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

    /**
     * 脱敏显示API Key
     *
     * 【安全与可用性的平衡】
     * - 完全隐藏：用户无法确认是否正确
     * - 完全显示：容易被他人窥视
     *
     * 方案：首尾各保留4位，中间用*替代
     * 这样用户可以：
     * 1. 确认密钥已被设置
     * 2. 看到密钥的格式特征
     * 3. 即使被看到也只知道部分字符
     */
    fun mask(apiKey: String): String {
        return when {
            apiKey.length <= 8 -> "****"
            else -> "${apiKey.take(4)}****${apiKey.takeLast(4)}"
        }
    }

    /**
     * 生成密钥标识
     *
     * 【密钥标识标准化】
     * 每个服务商对应一个标准化的密钥key：
     * "api_key_" + providerId
     * 例如：api_key_deepseek、api_key_openai
     * 便于管理和查找
     */
    fun generateKey(providerId: String): String {
        return "api_key_$providerId"
    }
}
