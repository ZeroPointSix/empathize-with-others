package com.empathy.ai.data.local

import android.content.Context
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
 * @property context 应用上下文
 *
 * @see androidx.security.crypto.EncryptedSharedPreferences
 * @see androidx.security.crypto.MasterKey
 */
@Singleton
class ApiKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 主密钥
     *
     * 使用 AES256_GCM 加密方案, 密钥存储在 Android Keystore 中。
     */
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * 加密的 SharedPreferences
     *
     * 使用 AES256_SIV 加密 key, AES256_GCM 加密 value。
     */
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 保存 API Key
     *
     * 将 API Key 加密后存储到 EncryptedSharedPreferences。
     *
     * @param key 存储的 key (通常是 "api_key_{providerId}")
     * @param apiKey 要保存的 API Key (明文)
     */
    fun save(key: String, apiKey: String) {
        encryptedPrefs.edit().putString(key, apiKey).apply()
    }

    /**
     * 读取 API Key
     *
     * 从 EncryptedSharedPreferences 中读取并解密 API Key。
     *
     * @param key 存储的 key
     * @return API Key (明文), 如果不存在则返回 null
     */
    fun get(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }

    /**
     * 删除 API Key
     *
     * 从 EncryptedSharedPreferences 中删除指定的 API Key。
     *
     * @param key 存储的 key
     */
    fun delete(key: String) {
        encryptedPrefs.edit().remove(key).apply()
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
