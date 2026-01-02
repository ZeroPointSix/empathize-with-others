package com.empathy.ai.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.model.ProxyType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 代理配置存储
 *
 * 使用 EncryptedSharedPreferences 安全存储代理配置信息，
 * 特别是用户名和密码等敏感信息。
 *
 * TD-00025: AI配置功能完善 - 网络代理设置
 */
@Singleton
class ProxyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "proxy_preferences"
        private const val KEY_ENABLED = "proxy_enabled"
        private const val KEY_TYPE = "proxy_type"
        private const val KEY_HOST = "proxy_host"
        private const val KEY_PORT = "proxy_port"
        private const val KEY_USERNAME = "proxy_username"
        private const val KEY_PASSWORD = "proxy_password"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 获取代理配置
     */
    fun getProxyConfig(): ProxyConfig {
        return ProxyConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            type = getProxyType(),
            host = prefs.getString(KEY_HOST, "") ?: "",
            port = prefs.getInt(KEY_PORT, 0),
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = prefs.getString(KEY_PASSWORD, "") ?: ""
        )
    }

    /**
     * 保存代理配置
     */
    fun saveProxyConfig(config: ProxyConfig) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, config.enabled)
            putString(KEY_TYPE, config.type.name)
            putString(KEY_HOST, config.host)
            putInt(KEY_PORT, config.port)
            putString(KEY_USERNAME, config.username)
            putString(KEY_PASSWORD, config.password)
            apply()
        }
    }

    /**
     * 清除代理配置
     */
    fun clearProxyConfig() {
        prefs.edit().clear().apply()
    }

    /**
     * 检查代理是否启用
     */
    fun isProxyEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    /**
     * 设置代理启用状态
     */
    fun setProxyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * 获取代理类型
     */
    private fun getProxyType(): ProxyType {
        val typeName = prefs.getString(KEY_TYPE, ProxyType.HTTP.name) ?: ProxyType.HTTP.name
        return try {
            ProxyType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            ProxyType.HTTP
        }
    }

    /**
     * 检查是否有有效的代理配置
     */
    fun hasValidConfig(): Boolean {
        val config = getProxyConfig()
        return config.enabled && config.isValid()
    }
}
