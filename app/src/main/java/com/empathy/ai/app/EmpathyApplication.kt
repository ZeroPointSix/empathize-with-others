package com.empathy.ai.app

import android.app.Application
import android.util.Log
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.usecase.SummarizeDailyConversationsUseCase
import com.empathy.ai.domain.util.DataCleanupManager
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
 * 3. 触发每日自动总结（记忆系统）
 */
@HiltAndroidApp
class EmpathyApplication : Application() {
    
    companion object {
        private const val TAG = "EmpathyApplication"
    }
    
    @Inject
    lateinit var floatingWindowPreferences: FloatingWindowPreferences
    
    @Inject
    lateinit var summarizeDailyConversationsUseCase: SummarizeDailyConversationsUseCase
    
    @Inject
    lateinit var dataCleanupManager: DataCleanupManager
    
    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // 恢复悬浮窗服务
        restoreFloatingWindowService()
        
        // 触发每日自动总结（在后台执行）
        triggerDailySummary()
        
        // 执行数据清理（在后台执行）
        triggerDataCleanup()
    }
    
    /**
     * 触发每日自动总结
     * 
     * 在应用启动时检查是否需要执行每日总结：
     * - 如果是新的一天，自动执行对话总结
     * - 在后台线程执行，不阻塞应用启动
     * - 失败不影响应用正常使用
     */
    private fun triggerDailySummary() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始检查每日总结...")
                val result = summarizeDailyConversationsUseCase()
                
                result.onSuccess { summaryResult ->
                    Log.d(TAG, "每日总结完成: 总计${summaryResult.totalContacts}个联系人, " +
                        "成功${summaryResult.successCount}, 失败${summaryResult.failedCount}, " +
                        "跳过${summaryResult.skippedCount}")
                }.onFailure { error ->
                    Log.e(TAG, "每日总结执行失败", error)
                }
            } catch (e: Exception) {
                // 总结失败不应该导致应用崩溃
                Log.e(TAG, "每日总结触发异常", e)
            }
        }
    }
    
    /**
     * 触发数据清理
     * 
     * 在应用启动时检查是否需要执行数据清理：
     * - 每7天执行一次清理
     * - 删除90天前的对话记录和总结
     * - 在后台线程执行，不阻塞应用启动
     */
    private fun triggerDataCleanup() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始检查数据清理...")
                val result = dataCleanupManager.checkAndCleanup()
                
                if (result != null) {
                    if (result.success) {
                        Log.d(TAG, "数据清理完成: 删除${result.totalDeleted}条记录 " +
                            "(对话${result.conversationsDeleted}, 总结${result.summariesDeleted}, " +
                            "失败任务${result.failedTasksDeleted})")
                    } else {
                        Log.w(TAG, "数据清理部分失败: ${result.errorMessage}")
                    }
                } else {
                    Log.d(TAG, "跳过数据清理：未到清理时间")
                }
            } catch (e: Exception) {
                // 清理失败不应该导致应用崩溃
                Log.e(TAG, "数据清理触发异常", e)
            }
        }
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
                        Log.d(TAG, "应用启动，检测到悬浮窗已启用，自动恢复服务")
                        FloatingWindowManager.startService(this@EmpathyApplication)
                    } else {
                        // 权限丢失，重置状态以保持一致性
                        Log.w(TAG, "悬浮窗权限丢失，重置状态为关闭")
                        floatingWindowPreferences.saveEnabled(false)
                    }
                } else {
                    Log.d(TAG, "悬浮窗未启用，跳过恢复")
                }
            } catch (e: Exception) {
                // 恢复失败不应该导致应用崩溃
                Log.e(TAG, "恢复悬浮窗服务失败", e)
            }
        }
    }
}
