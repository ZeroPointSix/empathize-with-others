package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 隐私设置持久化类
 *
 * 【隐私优先原则的体现】
 * - 数据掩码默认开启：发送给AI前自动脱敏
 * - 本地优先模式默认开启：优先使用本地数据
 *
 * 【配置持久化策略】
 * 使用SharedPreferences存储配置：
 * - 轻量级：无需数据库
 * - 即时性：修改立即生效
 * - 简单性：键值对存储
 *
 * 【默认值true的考量】
 * 隐私保护是核心价值，默认开启是最安全的选择。
 * 用户可以关闭，但需要明确操作。
 */
@Singleton
class PrivacyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * 数据掩码是否启用
     *
     * 【数据掩码】发送给AI前自动脱敏：
     * - 隐藏真实姓名、联系方式等敏感信息
     * - 防止AI服务商收集用户隐私
     * - 符合隐私优先原则
     */
    
    fun setDataMaskingEnabled(enabled: Boolean) { prefs.edit { putBoolean(KEY_DATA_MASKING, enabled) } }
    
    /**
     * 本地优先模式是否启用
     *
     * 【本地优先】优先使用本地数据：
     * - 先使用本地联系人画像
     * - 本地数据不足时再请求AI
     * - 减少不必要的数据传输
     */
    
    fun setLocalFirstModeEnabled(enabled: Boolean) { prefs.edit { putBoolean(KEY_LOCAL_FIRST, enabled) } }
    
    /**
     * 重置为默认值
     *
     * 【一键恢复】将隐私设置恢复为默认状态：
     * - 数据掩码：开启
     * - 本地优先：开启
     */
        prefs.edit {
            putBoolean(KEY_DATA_MASKING, DEFAULT_DATA_MASKING)
            putBoolean(KEY_LOCAL_FIRST, DEFAULT_LOCAL_FIRST)
        }
    }
    
    fun clear() { prefs.edit { clear() } }
    
    companion object {
        private const val PREFS_NAME = "privacy_settings"
        private const val KEY_DATA_MASKING = "privacy_data_masking_enabled"
        private const val KEY_LOCAL_FIRST = "privacy_local_first_mode_enabled"
        private const val DEFAULT_DATA_MASKING = true
        private const val DEFAULT_LOCAL_FIRST = true
    }
}
