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
 * 职责：
 * - 保存和读取数据掩码开关状态
 * - 保存和读取本地优先模式开关状态
 * - 提供线程安全的读写操作
 * 
 * 使用 SharedPreferences 存储配置信息
 */
@Singleton
class PrivacyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    fun isDataMaskingEnabled(): Boolean = prefs.getBoolean(KEY_DATA_MASKING, DEFAULT_DATA_MASKING)
    
    fun setDataMaskingEnabled(enabled: Boolean) { prefs.edit { putBoolean(KEY_DATA_MASKING, enabled) } }
    
    fun isLocalFirstModeEnabled(): Boolean = prefs.getBoolean(KEY_LOCAL_FIRST, DEFAULT_LOCAL_FIRST)
    
    fun setLocalFirstModeEnabled(enabled: Boolean) { prefs.edit { putBoolean(KEY_LOCAL_FIRST, enabled) } }
    
    fun resetToDefaults() {
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
