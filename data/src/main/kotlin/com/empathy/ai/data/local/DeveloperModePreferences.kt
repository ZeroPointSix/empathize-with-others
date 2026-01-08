package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 开发者模式状态持久化类
 *
 * 【BUG-00050 修复】
 * 解决开发者模式在导航离开设置页面后自动退出的问题。
 * 
 * 【设计思路】
 * 1. 使用 SharedPreferences 持久化开发者模式解锁状态
 * 2. 使用 Session ID 机制确保应用重启后状态重置
 * 3. 导航离开页面时状态保持，应用重启时状态重置
 *
 * 【Session 机制】
 * - 每次应用启动时生成新的 Session ID
 * - 解锁开发者模式时保存当前 Session ID
 * - 检查状态时验证 Session ID 是否匹配
 * - Session ID 不匹配则认为是新的应用会话，重置状态
 *
 * @see BUG-00050 开发者模式状态持久化问题
 */
@Singleton
class DeveloperModePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * 当前应用会话的 Session ID
     * 
     * 每次创建 DeveloperModePreferences 实例时生成新的 Session ID。
     * 由于使用 @Singleton 注解，整个应用生命周期内只会生成一次。
     * 应用重启后会生成新的 Session ID。
     */
    private val currentSessionId: String = UUID.randomUUID().toString()
    
    init {
        // 应用启动时检查 Session，如果不匹配则重置状态
        if (!isCurrentSession()) {
            reset()
        }
    }
    
    /**
     * 获取开发者模式是否已解锁
     *
     * @return true 如果开发者模式已解锁且在当前会话中
     */
    fun isDeveloperModeUnlocked(): Boolean {
        return prefs.getBoolean(KEY_DEVELOPER_MODE_UNLOCKED, false)
    }
    
    /**
     * 设置开发者模式解锁状态
     *
     * @param unlocked true 表示解锁，false 表示锁定
     */
    fun setDeveloperModeUnlocked(unlocked: Boolean) {
        prefs.edit { 
            putBoolean(KEY_DEVELOPER_MODE_UNLOCKED, unlocked)
        }
    }
    
    /**
     * 检查是否是当前会话
     *
     * 通过比较保存的 Session ID 和当前 Session ID 来判断。
     * 如果不匹配，说明应用已重启，应该重置开发者模式状态。
     *
     * @return true 如果是当前会话
     */
    fun isCurrentSession(): Boolean {
        val savedSessionId = prefs.getString(KEY_SESSION_ID, null)
        return savedSessionId == currentSessionId
    }
    
    /**
     * 更新 Session ID
     *
     * 在解锁开发者模式时调用，保存当前 Session ID。
     */
    fun updateSessionId() {
        prefs.edit {
            putString(KEY_SESSION_ID, currentSessionId)
        }
    }
    
    /**
     * 获取保存的 Session ID（用于调试）
     */
    fun getSavedSessionId(): String? {
        return prefs.getString(KEY_SESSION_ID, null)
    }
    
    /**
     * 获取当前 Session ID（用于调试）
     */
    fun getCurrentSessionId(): String {
        return currentSessionId
    }
    
    /**
     * 重置开发者模式状态
     *
     * 清除解锁状态和 Session ID。
     * 在应用重启或手动退出开发者模式时调用。
     */
    fun reset() {
        prefs.edit {
            putBoolean(KEY_DEVELOPER_MODE_UNLOCKED, false)
            remove(KEY_SESSION_ID)
        }
    }
    
    /**
     * 清除所有数据
     */
    fun clear() {
        prefs.edit { clear() }
    }
    
    companion object {
        private const val PREFS_NAME = "developer_mode_prefs"
        private const val KEY_DEVELOPER_MODE_UNLOCKED = "developer_mode_unlocked"
        private const val KEY_SESSION_ID = "session_id"
    }
}
