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
    
    /**
     * 获取数据掩码启用状态
     * 
     * @return 是否启用数据掩码，默认为 true（开启）
     */
    fun isDataMaskingEnabled(): Boolean {
        return prefs.getBoolean(KEY_DATA_MASKING, DEFAULT_DATA_MASKING)
    }
    
    /**
     * 设置数据掩码启用状态
     * 
     * @param enabled 是否启用
     */
    fun setDataMaskingEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_DATA_MASKING, enabled)
        }
    }
    
    /**
     * 获取本地优先模式启用状态
     * 
     * @return 是否启用本地优先模式，默认为 true（开启）
     */
    fun isLocalFirstModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_FIRST, DEFAULT_LOCAL_FIRST)
    }
    
    /**
     * 设置本地优先模式启用状态
     * 
     * @param enabled 是否启用
     */
    fun setLocalFirstModeEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_LOCAL_FIRST, enabled)
        }
    }
    
    /**
     * 重置所有隐私设置为默认值
     */
    fun resetToDefaults() {
        prefs.edit {
            putBoolean(KEY_DATA_MASKING, DEFAULT_DATA_MASKING)
            putBoolean(KEY_LOCAL_FIRST, DEFAULT_LOCAL_FIRST)
        }
    }
    
    /**
     * 清除所有隐私设置
     */
    fun clear() {
        prefs.edit {
            clear()
        }
    }
    
    companion object {
        /**
         * SharedPreferences 文件名
         */
        private const val PREFS_NAME = "privacy_settings"
        
        /**
         * 数据掩码键名
         */
        private const val KEY_DATA_MASKING = "privacy_data_masking_enabled"
        
        /**
         * 本地优先模式键名
         */
        private const val KEY_LOCAL_FIRST = "privacy_local_first_mode_enabled"
        
        /**
         * 数据掩码默认值（默认开启）
         */
        private const val DEFAULT_DATA_MASKING = true
        
        /**
         * 本地优先模式默认值（默认开启）
         */
        private const val DEFAULT_LOCAL_FIRST = true
    }
}
