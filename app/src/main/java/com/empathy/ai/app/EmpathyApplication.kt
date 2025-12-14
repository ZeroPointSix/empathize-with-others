package com.empathy.ai.app

import android.app.Application
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.util.FloatingWindowManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application 入口
 *
 * 使用 @HiltAndroidApp 注解启用 Hilt 依赖注入
 * 
 * 职责：
 * 1. 初始化 Hilt 依赖注入
 * 2. 恢复悬浮窗服务（如果之前已启用）
 */
@HiltAndroidApp
class EmpathyApplication : Application() {
    
    @Inject
    lateinit var floatingWindowPreferences: FloatingWindowPreferences
    
    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // 恢复悬浮窗服务
        restoreFloatingWindowService()
    }
    
    /**
     * 恢复悬浮窗服务
     * 
     * 在应用启动时检查悬浮窗状态，如果之前已启用且有权限，则自动启动服务
     * 这确保了悬浮窗在应用重启后能够自动恢复，而不需要用户手动重新开启
     */
    private fun restoreFloatingWindowService() {
        applicationScope.launch {
            try {
                // 加载保存的悬浮窗状态
                val state = floatingWindowPreferences.loadState()
                
                if (state.isEnabled) {
                    // 检查悬浮窗权限
                    val permissionResult = FloatingWindowManager.hasPermission(this@EmpathyApplication)
                    
                    if (permissionResult is FloatingWindowManager.PermissionResult.Granted) {
                        // 有权限，启动服务
                        android.util.Log.d("EmpathyApplication", "应用启动，检测到悬浮窗已启用，自动恢复服务")
                        FloatingWindowManager.startService(this@EmpathyApplication)
                    } else {
                        // 权限丢失，重置状态以保持一致性
                        android.util.Log.w("EmpathyApplication", "悬浮窗权限丢失，重置状态为关闭")
                        floatingWindowPreferences.saveEnabled(false)
                    }
                } else {
                    android.util.Log.d("EmpathyApplication", "悬浮窗未启用，跳过恢复")
                }
            } catch (e: Exception) {
                // 恢复失败不应该导致应用崩溃
                android.util.Log.e("EmpathyApplication", "恢复悬浮窗服务失败", e)
            }
        }
    }
}
