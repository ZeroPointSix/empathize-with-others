package com.empathy.ai.data.repository.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.empathy.ai.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置仓库实现类
 *
 * 使用 EncryptedSharedPreferences 安全存储敏感配置，如 API Key。
 * 相比普通 SharedPreferences，提供硬件级加密保护。
 *
 * 安全特性:
 * - 使用 MasterKey 进行密钥管理
 * - AES-256-GCM 加密算法
 * - 即使设备被 Root，也能提供一定的安全保护
 * 
 * 容错机制 (BUG-00028 修复)：
 * - 完全延迟初始化：构造函数不访问 Keystore，只在首次使用时初始化
 * - 重试机制：Keystore 服务不可用时自动重试（最多3次，递增延迟）
 * - 降级策略：多次重试失败后使用普通 SharedPreferences
 * - 线程安全：使用 synchronized 确保并发安全
 *
 * @property context 应用上下文
 * @property privacyPreferences 隐私设置持久化
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacyPreferences: com.empathy.ai.data.local.PrivacyPreferences,
    private val conversationPreferences: com.empathy.ai.data.local.ConversationPreferences
) : SettingsRepository {

    companion object {
        private const val TAG = "SettingsRepositoryImpl"
        private const val PREFS_NAME = "empathy_ai_encrypted_prefs"
        private const val FALLBACK_PREFS_NAME = "empathy_ai_encrypted_prefs_fallback"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_BASE_URL = "base_url"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 200L

        // 默认值
        private const val DEFAULT_PROVIDER = "OpenAI"
        private const val DEFAULT_BASE_URL_OPENAI = "https://api.openai.com/v1/chat/completions"
        private const val DEFAULT_BASE_URL_DEEPSEEK = "https://api.deepseek.com/chat/completions"
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
     * EncryptedSharedPreferences 实例
     *
     * 使用自定义 getter + synchronized 实现延迟初始化，
     * 避免在 Application 创建时触发 Keystore 访问。
     */
    private val encryptedPrefs: SharedPreferences
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
                val prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                Log.d(TAG, "EncryptedSharedPreferences 创建成功")
                isEncryptionAvailable = true
                return prefs
            } catch (e: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences 创建失败", e)
            }
        }
        
        Log.w(TAG, "降级使用普通 SharedPreferences，设置数据将以明文存储")
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
    
    /**
     * 检查安全存储是否可用
     */
    fun isSecureStorageAvailable(): Boolean {
        encryptedPrefs // 触发初始化
        return isEncryptionAvailable
    }

    /**
     * 获取 API Key
     *
     * 从加密存储中读取 API Key。
     * 这是一个 suspend 函数，虽然在 IO 线程中执行很快，
     * 但遵循协程最佳实践。
     *
     * @return 包含 API Key 或 null 的 Result
     */
    override suspend fun getApiKey(): Result<String?> {
        return try {
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            Result.success(apiKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存 API Key
     *
     * 将 API Key 加密存储。
     *
     * @param key API Key
     * @return 操作结果
     */
    override suspend fun saveApiKey(key: String): Result<Unit> {
        return try {
            encryptedPrefs.edit()
                .putString(KEY_API_KEY, key)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取当前选择的 AI 服务商
     *
     * @return 包含服务商名称的 Result，默认返回 "OpenAI"
     */
    override suspend fun getAiProvider(): Result<String> {
        return try {
            val provider = encryptedPrefs.getString(KEY_AI_PROVIDER, DEFAULT_PROVIDER)
            Result.success(provider ?: DEFAULT_PROVIDER)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存 AI 服务商选择
     *
     * @param provider 服务商名称（OpenAI、DeepSeek 等）
     * @return 操作结果
     */
    override suspend fun saveAiProvider(provider: String): Result<Unit> {
        return try {
            encryptedPrefs.edit()
                .putString(KEY_AI_PROVIDER, provider)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取 BaseUrl
     *
     * 根据当前选择的服务商返回对应的 URL。
     * 如果用户自定义了 URL，则返回自定义 URL。
     *
     * @return 包含 BaseUrl 的 Result
     */
    override suspend fun getBaseUrl(): Result<String> {
        return try {
            // 检查是否有自定义 URL
            val customUrl = encryptedPrefs.getString(KEY_BASE_URL, null)
            if (!customUrl.isNullOrEmpty()) {
                return Result.success(customUrl)
            }

            // 根据服务商返回默认 URL
            val provider = getAiProvider().getOrDefault(DEFAULT_PROVIDER)
            val baseUrl = when (provider) {
                "OpenAI" -> DEFAULT_BASE_URL_OPENAI
                "DeepSeek" -> DEFAULT_BASE_URL_DEEPSEEK
                else -> DEFAULT_BASE_URL_OPENAI
            }

            Result.success(baseUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取特定服务商需要的 Headers
     *
     * 构造包含鉴权信息的请求头。
     *
     * @return 包含 Headers Map 的 Result
     */
    override suspend fun getProviderHeaders(): Result<Map<String, String>> {
        return try {
            val apiKey = getApiKey().getOrNull()
            if (apiKey.isNullOrEmpty()) {
                return Result.failure(Exception("API Key not found. Please configure your API Key in settings."))
            }

            val headers = mutableMapOf<String, String>(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            )

            // 特殊处理：OpenAI 需要额外的 Referer（某些情况）
            val provider = getAiProvider().getOrDefault(DEFAULT_PROVIDER)
            if (provider == "OpenAI") {
                // 可以在这里添加其他 OpenAI 特定的 Headers
                // headers["OpenAI-Organization"] = "org-xxx"
            }

            Result.success(headers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清除所有设置
     *
     * 用于用户登出或重置应用。
     *
     * @return 操作结果
     */
    suspend fun clearAllSettings(): Result<Unit> {
        return try {
            encryptedPrefs.edit().clear().apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查 API Key 是否存在
     *
     * @return 如果 API Key 存在且不为空返回 true
     */
    suspend fun hasApiKey(): Result<Boolean> {
        return try {
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            Result.success(!apiKey.isNullOrEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除 API Key
     *
     * @return 操作结果
     */
    suspend fun deleteApiKey(): Result<Unit> {
        return try {
            encryptedPrefs.edit()
                .remove(KEY_API_KEY)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取数据掩码启用状态
     *
     * @return 包含启用状态的 Result，默认为 true
     */
    override suspend fun getDataMaskingEnabled(): Result<Boolean> {
        return try {
            Result.success(privacyPreferences.isDataMaskingEnabled())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 设置数据掩码启用状态
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    override suspend fun setDataMaskingEnabled(enabled: Boolean): Result<Unit> {
        return try {
            privacyPreferences.setDataMaskingEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取本地优先模式启用状态
     *
     * @return 包含启用状态的 Result，默认为 true
     */
    override suspend fun getLocalFirstModeEnabled(): Result<Boolean> {
        return try {
            Result.success(privacyPreferences.isLocalFirstModeEnabled())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 设置本地优先模式启用状态
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    override suspend fun setLocalFirstModeEnabled(enabled: Boolean): Result<Unit> {
        return try {
            privacyPreferences.setLocalFirstModeEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取历史对话条数配置
     *
     * @return 0/5/10，默认5
     */
    override suspend fun getHistoryConversationCount(): Result<Int> {
        return try {
            Result.success(conversationPreferences.getHistoryConversationCount())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设置历史对话条数
     *
     * @param count 条数，必须是 0/5/10 之一
     * @return 操作结果
     */
    override suspend fun setHistoryConversationCount(count: Int): Result<Unit> {
        return try {
            conversationPreferences.setHistoryConversationCount(count)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
