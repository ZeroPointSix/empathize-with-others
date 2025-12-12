package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 悬浮窗状态持久化类
 * 
 * 职责：
 * - 保存悬浮窗的启用状态和位置信息
 * - 加载悬浮窗的配置
 * - 保存和恢复最小化请求信息
 * - 保存和恢复最小化指示器位置
 * - 提供线程安全的读写操作
 * 
 * 使用 SharedPreferences 存储配置信息
 */
@Singleton
class FloatingWindowPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * 保存悬浮窗状态
     * 
     * @param state 悬浮窗状态
     */
    fun saveState(state: FloatingWindowState) {
        prefs.edit {
            putBoolean(KEY_IS_ENABLED, state.isEnabled)
            putInt(KEY_BUTTON_X, state.buttonX)
            putInt(KEY_BUTTON_Y, state.buttonY)
        }
    }
    
    /**
     * 加载悬浮窗状态
     * 
     * @return 悬浮窗状态，如果没有保存过则返回默认值
     */
    fun loadState(): FloatingWindowState {
        return FloatingWindowState(
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false),
            buttonX = prefs.getInt(KEY_BUTTON_X, 0),
            buttonY = prefs.getInt(KEY_BUTTON_Y, 0)
        )
    }
    
    /**
     * 保存悬浮窗启用状态
     * 
     * @param isEnabled 是否启用
     */
    fun saveEnabled(isEnabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_IS_ENABLED, isEnabled)
        }
    }
    
    /**
     * 获取悬浮窗启用状态
     * 
     * @return 是否启用
     */
    fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_ENABLED, false)
    }
    
    /**
     * 保存悬浮按钮位置
     * 
     * @param x X 坐标
     * @param y Y 坐标
     */
    fun saveButtonPosition(x: Int, y: Int) {
        prefs.edit {
            putInt(KEY_BUTTON_X, x)
            putInt(KEY_BUTTON_Y, y)
        }
    }
    
    /**
     * 获取悬浮按钮 X 坐标
     * 
     * @return X 坐标
     */
    fun getButtonX(): Int {
        return prefs.getInt(KEY_BUTTON_X, 0)
    }
    
    /**
     * 获取悬浮按钮 Y 坐标
     * 
     * @return Y 坐标
     */
    fun getButtonY(): Int {
        return prefs.getInt(KEY_BUTTON_Y, 0)
    }
    
    /**
     * 清除所有保存的状态
     */
    fun clear() {
        prefs.edit {
            clear()
        }
    }
    
    /**
     * 保存最小化请求信息
     * 
     * 使用 Moshi 将请求信息序列化为 JSON 并保存到 SharedPreferences
     * 
     * @param requestInfo 请求信息
     */
    fun saveRequestInfo(requestInfo: MinimizedRequestInfo) {
        try {
            val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
            val json = adapter.toJson(requestInfo)
            prefs.edit {
                putString(KEY_MINIMIZED_REQUEST, json)
            }
        } catch (e: Exception) {
            // 序列化失败，记录日志但不抛出异常
            android.util.Log.e(TAG, "Failed to save request info", e)
        }
    }
    
    /**
     * 获取最小化请求信息
     * 
     * 从 SharedPreferences 读取 JSON 并使用 Moshi 反序列化
     * 
     * @return 请求信息，如果不存在或解析失败则返回 null
     */
    fun getRequestInfo(): MinimizedRequestInfo? {
        return try {
            val json = prefs.getString(KEY_MINIMIZED_REQUEST, null) ?: return null
            val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            // 反序列化失败，记录日志并返回 null
            android.util.Log.e(TAG, "Failed to parse request info", e)
            // 清除损坏的数据
            clearRequestInfo()
            null
        }
    }
    
    /**
     * 清除最小化请求信息
     */
    fun clearRequestInfo() {
        prefs.edit {
            remove(KEY_MINIMIZED_REQUEST)
        }
    }
    
    /**
     * 保存最小化指示器位置
     * 
     * @param x X 坐标
     * @param y Y 坐标
     */
    fun saveIndicatorPosition(x: Int, y: Int) {
        prefs.edit {
            putInt(KEY_INDICATOR_X, x)
            putInt(KEY_INDICATOR_Y, y)
        }
    }
    
    /**
     * 获取最小化指示器位置
     * 
     * 如果没有保存过指示器位置，则返回悬浮按钮的位置作为默认值
     * 
     * @return 指示器位置 (x, y)
     */
    fun getIndicatorPosition(): Pair<Int, Int> {
        val x = prefs.getInt(KEY_INDICATOR_X, -1)
        val y = prefs.getInt(KEY_INDICATOR_Y, -1)
        
        // 如果没有保存过指示器位置，使用悬浮按钮位置
        return if (x == -1 || y == -1) {
            Pair(getButtonX(), getButtonY())
        } else {
            Pair(x, y)
        }
    }
    
    /**
     * 获取悬浮按钮位置
     * 
     * @return 按钮位置 (x, y)
     */
    fun getButtonPosition(): Pair<Int, Int> {
        return Pair(getButtonX(), getButtonY())
    }
    
    companion object {
        /**
         * 日志标签
         */
        private const val TAG = "FloatingWindowPrefs"
        
        /**
         * SharedPreferences 文件名
         */
        private const val PREFS_NAME = "floating_window_prefs"
        
        /**
         * 启用状态键
         */
        private const val KEY_IS_ENABLED = "is_enabled"
        
        /**
         * 按钮 X 坐标键
         */
        private const val KEY_BUTTON_X = "button_x"
        
        /**
         * 按钮 Y 坐标键
         */
        private const val KEY_BUTTON_Y = "button_y"
        
        /**
         * 最小化请求信息键
         */
        private const val KEY_MINIMIZED_REQUEST = "minimized_request"
        
        /**
         * 指示器 X 坐标键
         */
        private const val KEY_INDICATOR_X = "indicator_x"
        
        /**
         * 指示器 Y 坐标键
         */
        private const val KEY_INDICATOR_Y = "indicator_y"
    }
}
