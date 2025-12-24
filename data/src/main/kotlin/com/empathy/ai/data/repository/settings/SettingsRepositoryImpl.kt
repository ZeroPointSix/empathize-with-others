package com.empathy.ai.data.repository.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.empathy.ai.data.local.ConversationPreferences
import com.empathy.ai.data.local.PrivacyPreferences
import com.empathy.ai.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置仓库实现类
 *
 * 使用 EncryptedSharedPreferences 安全存储敏感配置
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacyPreferences: PrivacyPreferences,
    private val conversationPreferences: ConversationPreferences
) : SettingsRepository {

    companion object {
        private const val TAG = "SettingsRepositoryImpl"
        private const val PREFS_NAME = "empathy_ai_encrypted_prefs"
        private const val FALLBACK_PREFS_NAME = "empathy_ai_encrypted_prefs_fallback"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_LAST_SUMMARY_DATE = "last_summary_date"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 200L

        private const val DEFAULT_PROVIDER = "OpenAI"
        private const val DEFAULT_BASE_URL_OPENAI = "https://api.openai.com/v1/chat/completions"
        private const val DEFAULT_BASE_URL_DEEPSEEK = "https://api.deepseek.com/chat/completions"
    }

    @Volatile
    private var isEncryptionAvailable = true
    
    private val initLock = Any()
    
    private var _prefs: SharedPreferences? = null

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
        
        Log.w(TAG, "降级使用普通 SharedPreferences")
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
                        break
                    }
                }
            }
        }
        
        Log.e(TAG, "MasterKey 创建失败，已达最大重试次数", lastException)
        return null
    }
    
    fun isSecureStorageAvailable(): Boolean {
        encryptedPrefs
        return isEncryptionAvailable
    }

    override suspend fun getApiKey(): Result<String?> {
        return try {
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            Result.success(apiKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    override suspend fun getAiProvider(): Result<String> {
        return try {
            val provider = encryptedPrefs.getString(KEY_AI_PROVIDER, DEFAULT_PROVIDER)
            Result.success(provider ?: DEFAULT_PROVIDER)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    override suspend fun getBaseUrl(): Result<String> {
        return try {
            val customUrl = encryptedPrefs.getString(KEY_BASE_URL, null)
            if (!customUrl.isNullOrEmpty()) {
                return Result.success(customUrl)
            }

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

    override suspend fun getProviderHeaders(): Result<Map<String, String>> {
        return try {
            val apiKey = getApiKey().getOrNull()
            if (apiKey.isNullOrEmpty()) {
                return Result.failure(Exception("API Key not found"))
            }

            val headers = mutableMapOf<String, String>(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            )

            Result.success(headers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllSettings(): Result<Unit> {
        return try {
            encryptedPrefs.edit().clear().apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasApiKey(): Result<Boolean> {
        return try {
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            Result.success(!apiKey.isNullOrEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
    
    override suspend fun getDataMaskingEnabled(): Result<Boolean> {
        return try {
            Result.success(privacyPreferences.isDataMaskingEnabled())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setDataMaskingEnabled(enabled: Boolean): Result<Unit> {
        return try {
            privacyPreferences.setDataMaskingEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getLocalFirstModeEnabled(): Result<Boolean> {
        return try {
            Result.success(privacyPreferences.isLocalFirstModeEnabled())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setLocalFirstModeEnabled(enabled: Boolean): Result<Unit> {
        return try {
            privacyPreferences.setLocalFirstModeEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistoryConversationCount(): Result<Int> {
        return try {
            Result.success(conversationPreferences.getHistoryConversationCount())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setHistoryConversationCount(count: Int): Result<Unit> {
        return try {
            conversationPreferences.setHistoryConversationCount(count)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastSummaryDate(): Result<String?> {
        return try {
            val date = encryptedPrefs.getString(KEY_LAST_SUMMARY_DATE, null)
            Result.success(date)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setLastSummaryDate(date: String): Result<Unit> {
        return try {
            encryptedPrefs.edit()
                .putString(KEY_LAST_SUMMARY_DATE, date)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
