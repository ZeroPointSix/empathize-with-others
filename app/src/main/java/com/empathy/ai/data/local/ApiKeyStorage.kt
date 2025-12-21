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
 * 使用场景:
 * - 保存 AI 服务商的 API Key
 * - 读取 API Key 用于 API 调用
 * - 删除服务商时清理 API Key
 * - 在 UI 中显示脱敏后的 API Key
 *
 * 存储格式:
 * - Key: "api_key_{providerId}" (例如: "api_key_uuid-123")
 * - Value: 加密后的 API Key 字符串
 *
 * 容错机制 (BUG-00028 修复):
 * - 完全延迟初始化：构造函数不访问 Keystore，只在首次使用时初始化
 * - 重试机制：Keystore 服务不可用时自动重试（最多3次，递增延迟）
 * - 降级策略：多次重试失败后使用普通 SharedPreferences
 * - 线程安全：使用 synchronized 确保并发安全
 *
 * @property context 应用上下文
 *
 * @see androidx.security.crypto.EncryptedSharedPreferences
 * @see androidx.security.crypto.MasterKey
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

    /**
     * 标记加密存储是否可用
     * 
     * - true: 使用 EncryptedSharedPreferences（安全）
     * - false: 降级使用普通 SharedPreferences（不安全，仅作为最后手段）
     */
    @Volatile
    private var isEncryptionAvailable = true

    /**
     * 标记是否已完成初始化
     */
    @Volatile
    private var isInitialized = false

    /**
     * 初始化锁，确保线程安全
     */
    private val initLock = Any()

    /**
     * SharedPreferences 实例（延迟初始化）
     * 
     * 注意：不使用 Kotlin 的 `by lazy`，因为 Hilt 的依赖图验证可能触发 lazy 属性访问。
     * 使用自定义 getter + synchronized 确保：
     * 1. 构造函数完全不访问 Keystore
     * 2. 只在首次调用 save/get/delete 时初始化
     * 3. 线程安全
     */
    private var _prefs: SharedPreferences? = null

    /**
     * 获取 SharedPreferences 实例
     * 
     * 首次访问时会触发初始化，包括：
     * 1. 尝试创建 MasterKey（带重试）
     * 2. 尝试创建 EncryptedSharedPreferences
     * 3. 失败时降级到普通 SharedPreferences
     */
    private val prefs: SharedPreferences
        get() {
            // 快速路径：已初始化
            if (_prefs != null) {
                return _prefs!!
            }
            
            // 慢路径：需要初始化
            synchronized(initLock) {
                // 双重检查
                if (_prefs == null) {
                    _prefs = initializePrefs()
                    isInitialized = true
                    Log.d(TAG, "SharedPreferences 初始化完成，加密可用: $isEncryptionAvailable")
                }
                return _prefs!!
            }
        }

    /**
     * 初始化 SharedPreferences
     * 
     * 这个方法只在首次访问 prefs 时调用，而不是在构造函数中。
     * 这是 BUG-00028 修复的核心：确保 Keystore 访问延迟到应用完全启动后。
     * 
     * @return SharedPreferences 实例（加密或普通）
     */
    private fun initializePrefs(): SharedPreferences {
        Log.d(TAG, "开始初始化 SharedPreferences...")
        
        // 尝试创建加密存储
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

        // 降级到普通 SharedPreferences
        Log.w(TAG, "降级使用普通 SharedPreferences，API Key 将以明文存储")
        isEncryptionAvailable = false
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 创建 MasterKey，带重试机制
     * 
     * Keystore 服务在设备启动后可能需要一些时间才能完全就绪。
     * 通过重试机制（递增延迟）给服务更多恢复时间。
     *
     * @return MasterKey 实例，如果创建失败返回 null
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
                        // 递增延迟：200ms, 400ms, 600ms...
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

    /**
     * 检查安全存储是否可用
     *
     * @return true 如果使用加密存储，false 如果降级到普通存储
     */
    fun isSecureStorageAvailable(): Boolean {
        // 触发延迟初始化（如果尚未初始化）
        prefs
        return isEncryptionAvailable
    }

    /**
     * 检查是否已初始化
     * 
     * 用于测试和调试，不触发初始化。
     * 
     * @return true 如果已初始化
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * 保存 API Key
     *
     * 将 API Key 加密后存储到 EncryptedSharedPreferences。
     * 如果加密存储不可用，会降级到普通存储。
     *
     * @param key 存储的 key (通常是 "api_key_{providerId}")
     * @param apiKey 要保存的 API Key (明文)
     */
    fun save(key: String, apiKey: String) {
        try {
            prefs.edit().putString(key, apiKey).apply()
            Log.d(TAG, "API Key 保存成功: $key")
        } catch (e: Exception) {
            Log.e(TAG, "保存 API Key 失败: ${e.message}")
        }
    }

    /**
     * 读取 API Key
     *
     * 从 EncryptedSharedPreferences 中读取并解密 API Key。
     *
     * @param key 存储的 key
     * @return API Key (明文), 如果不存在或读取失败则返回 null
     */
    fun get(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            Log.e(TAG, "读取 API Key 失败: ${e.message}")
            null
        }
    }

    /**
     * 删除 API Key
     *
     * 从 EncryptedSharedPreferences 中删除指定的 API Key。
     *
     * @param key 存储的 key
     */
    fun delete(key: String) {
        try {
            prefs.edit().remove(key).apply()
            Log.d(TAG, "API Key 删除成功: $key")
        } catch (e: Exception) {
            Log.e(TAG, "删除 API Key 失败: ${e.message}")
        }
    }

    /**
     * 脱敏显示 API Key
     *
     * 将 API Key 脱敏处理, 只显示前 4 位和后 4 位字符, 中间用 **** 替代。
     *
     * 脱敏规则:
     * - 长度 <= 8: 全部显示为 "****"
     * - 长度 > 8: 显示前 4 位 + "****" + 后 4 位
     *
     * 示例:
     * - "sk-1234567890abcdef" -> "sk-1****cdef"
     * - "short" -> "****"
     *
     * @param apiKey 要脱敏的 API Key
     * @return 脱敏后的字符串
     */
    fun mask(apiKey: String): String {
        return when {
            apiKey.length <= 8 -> "****"
            else -> "${apiKey.take(4)}****${apiKey.takeLast(4)}"
        }
    }

    /**
     * 生成 API Key 的存储 key
     *
     * 根据服务商 ID 生成存储 key。
     *
     * @param providerId 服务商 ID
     * @return 存储 key (格式: "api_key_{providerId}")
     */
    fun generateKey(providerId: String): String {
        return "api_key_$providerId"
    }
}
