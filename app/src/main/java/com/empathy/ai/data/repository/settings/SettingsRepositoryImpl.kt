package com.empathy.ai.data.repository.settings

import android.content.Context
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
        private const val PREFS_NAME = "empathy_ai_encrypted_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_BASE_URL = "base_url"

        // 默认值
        private const val DEFAULT_PROVIDER = "OpenAI"
        private const val DEFAULT_BASE_URL_OPENAI = "https://api.openai.com/v1/chat/completions"
        private const val DEFAULT_BASE_URL_DEEPSEEK = "https://api.deepseek.com/chat/completions"
    }

    /**
     * EncryptedSharedPreferences 实例
     *
     * 懒加载初始化，避免应用启动时阻塞
     */
    private val encryptedPrefs by lazy {
        try {
            // 创建或获取 MasterKey
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // 创建 EncryptedSharedPreferences 实例
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 如果创建失败（例如设备不支持），回退到普通 SharedPreferences
            // 注意：这只是一个降级方案，会发出警告
            android.util.Log.w("SettingsRepository", "Failed to create EncryptedSharedPreferences, falling back to regular SharedPreferences", e)
            context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
        }
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
