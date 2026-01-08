package com.empathy.ai.data.repository

import com.empathy.ai.data.local.DeveloperModePreferences
import com.empathy.ai.domain.repository.DeveloperModeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 开发者模式仓库实现
 *
 * 【BUG-00050 修复】
 * 实现开发者模式状态持久化，委托给 DeveloperModePreferences。
 *
 * @see DeveloperModeRepository
 * @see DeveloperModePreferences
 * @see BUG-00050 开发者模式状态持久化问题
 */
@Singleton
class DeveloperModeRepositoryImpl @Inject constructor(
    private val preferences: DeveloperModePreferences
) : DeveloperModeRepository {
    
    override fun isDeveloperModeUnlocked(): Boolean {
        return preferences.isDeveloperModeUnlocked()
    }
    
    override fun setDeveloperModeUnlocked(unlocked: Boolean) {
        preferences.setDeveloperModeUnlocked(unlocked)
    }
    
    override fun isCurrentSession(): Boolean {
        return preferences.isCurrentSession()
    }
    
    override fun updateSessionId() {
        preferences.updateSessionId()
    }
    
    override fun reset() {
        preferences.reset()
    }
}
