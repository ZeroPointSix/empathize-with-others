package com.empathy.ai.app

import android.app.Application
import android.util.Log
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.usecase.SummarizeDailyConversationsUseCase
import com.empathy.ai.domain.util.DataCleanupManager
import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.util.AndroidFloatingWindowManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * Application 入口
 *
 * 使用 @HiltAndroidApp 注解启用 Hilt 依赖注入
 * 
 * 职责：
 * 1. 初始化 Hilt 依赖注入
 * 2. 恢复悬浮窗服务（如果之前已启用）
 * 3. 触发每日自动总结（记忆系统）
 * 4. 执行数据清理
 * 
 * 容错机制 (BUG-00028 修复)：
 * - 使用 Provider<T> 延迟注入，避免在 Application 创建时触发 Keystore 访问
 * - 后台任务延迟执行（1秒），给系统服务（如 Keystore Daemon）更多启动时间
 * - 所有后台任务失败不影响应用正常启动
 * - 每个任务独立 try-catch，一个失败不影响其他任务
 */
@HiltAndroidApp
class EmpathyApplication : Application() {
    
    companion object {
        private const val TAG = "EmpathyApplication"
        
        /**
         * 启动延迟时间（毫秒）
         * 
         * 增加到 1 秒，给 Keystore 等系统服务更多启动时间。
         * 这是 BUG-00028 修复的一部分：避免在系统服务未就绪时访问 Keystore。
         */
        private const val STARTUP_DELAY_MS = 1000L
    }
    
    /**
     * 悬浮窗偏好设置（延迟注入）
     * 
     * 使用 Provider 确保只在实际需要时才创建实例，
     * 避免在 Application 创建时触发依赖链。
     */
    @Inject
    lateinit var floatingWindowPreferencesProvider: Provider<FloatingWindowPreferences>
    
    /**
     * 悬浮窗管理器（延迟注入）
     * 
     * 使用 Provider 确保只在实际需要时才创建实例。
     */
    @Inject
    lateinit var floatingWindowManagerProvider: Provider<AndroidFloatingWindowManager>
    
    /**
     * 每日总结用例（延迟注入）
     * 
     * 使用 Provider 确保只在实际需要时才创建实例。
     * 该用例依赖 AiRepository → AiProviderRepository → ApiKeyStorage，
     * 延迟注入可以避免过早触发 Keystore 访问。
     */
    @Inject
    lateinit var summarizeDailyConversationsUseCaseProvider: Provider<SummarizeDailyConversationsUseCase>
    
    /**
     * 数据清理管理器（延迟注入）
     */
    @Inject
    lateinit var dataCleanupManagerProvider: Provider<DataCleanupManager>
    
    /**
     * 应用级协程作用域
     * 
     * 使用 SupervisorJob 确保一个子任务失败不会影响其他任务。
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate 开始")
        
        // 延迟执行后台任务，给系统服务（如 Keystore）更多启动时间
        // 这是 BUG-00028 修复的关键：避免在系统服务未就绪时访问 Keystore
        applicationScope.launch {
            Log.d(TAG, "延迟 ${STARTUP_DELAY_MS}ms 后执行后台任务...")
            delay(STARTUP_DELAY_MS)
            initializeBackgroundTasks()
        }
        
        Log.d(TAG, "Application onCreate 完成（后台任务将延迟执行）")
    }
    
    /**
     * 初始化后台任务
     * 
     * 在延迟后执行，确保系统服务已就绪。
     * 每个任务独立 try-catch，一个失败不影响其他任务。
     */
    private suspend fun initializeBackgroundTasks() {
        Log.d(TAG, "开始执行后台任务...")
        
        // 任务1：恢复悬浮窗服务
        try {
            restoreFloatingWindowService()
        } catch (e: Exception) {
            Log.e(TAG, "恢复悬浮窗服务失败（不影响应用启动）", e)
        }
        
        // 任务2：触发每日自动总结
        try {
            triggerDailySummary()
        } catch (e: Exception) {
            Log.e(TAG, "触发每日总结失败（不影响应用启动）", e)
        }
        
        // 任务3：执行数据清理
        try {
            triggerDataCleanup()
        } catch (e: Exception) {
            Log.e(TAG, "触发数据清理失败（不影响应用启动）", e)
        }
        
        Log.d(TAG, "后台任务执行完成")
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
                
                // 通过 Provider.get() 延迟获取实例
                // 此时 Keystore 服务应该已经就绪
                val useCase = summarizeDailyConversationsUseCaseProvider.get()
                val result = useCase()
                
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
                val manager = dataCleanupManagerProvider.get()
                val result = manager.checkAndCleanup()
                
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
     * 在应用启动时检查悬浮窗状态，如果之前已启用且有权限，则自动启动服务。
     * 这确保了悬浮窗在应用重启后能够自动恢复，而不需要用户手动重新开启。
     */
    private suspend fun restoreFloatingWindowService() {
        try {
            Log.d(TAG, "检查悬浮窗服务状态...")
            
            // 通过 Provider.get() 延迟获取实例
            val prefs = floatingWindowPreferencesProvider.get()
            val state = prefs.loadState()
            
            if (state.isEnabled) {
                // 通过 Provider.get() 延迟获取 FloatingWindowManager 实例
                val floatingWindowManager = floatingWindowManagerProvider.get()
                
                // 检查悬浮窗权限
                val permissionResult = floatingWindowManager.hasPermission()
                
                if (permissionResult is FloatingWindowManager.PermissionResult.Granted) {
                    // 有权限，启动服务
                    Log.d(TAG, "应用启动，检测到悬浮窗已启用，自动恢复服务")
                    floatingWindowManager.startService()
                } else {
                    // 权限丢失，重置状态以保持一致性
                    Log.w(TAG, "悬浮窗权限丢失，重置状态为关闭")
                    prefs.saveEnabled(false)
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
