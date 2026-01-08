package com.empathy.ai.domain.repository

/**
 * 开发者模式仓库接口
 *
 * 【BUG-00050 修复】
 * 定义开发者模式状态持久化的接口，遵循 Clean Architecture 原则。
 * 接口定义在 domain 层，实现在 data 层。
 *
 * 【设计思路】
 * 1. 使用 Session ID 机制确保应用重启后状态重置
 * 2. 导航离开页面时状态保持，应用重启时状态重置
 *
 * @see BUG-00050 开发者模式状态持久化问题
 */
interface DeveloperModeRepository {
    
    /**
     * 获取开发者模式是否已解锁
     *
     * @return true 如果开发者模式已解锁
     */
    fun isDeveloperModeUnlocked(): Boolean
    
    /**
     * 设置开发者模式解锁状态
     *
     * @param unlocked true 表示解锁，false 表示锁定
     */
    fun setDeveloperModeUnlocked(unlocked: Boolean)
    
    /**
     * 检查是否是当前会话
     *
     * 通过比较保存的 Session ID 和当前 Session ID 来判断。
     * 如果不匹配，说明应用已重启，应该重置开发者模式状态。
     *
     * @return true 如果是当前会话
     */
    fun isCurrentSession(): Boolean
    
    /**
     * 更新 Session ID
     *
     * 在解锁开发者模式时调用，保存当前 Session ID。
     */
    fun updateSessionId()
    
    /**
     * 重置开发者模式状态
     *
     * 清除解锁状态和 Session ID。
     */
    fun reset()
}
