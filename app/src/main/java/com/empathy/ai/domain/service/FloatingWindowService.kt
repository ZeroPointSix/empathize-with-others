package com.empathy.ai.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.empathy.ai.R
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AiResult
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.FloatingWindowUiState
import com.empathy.ai.domain.model.RefinementRequest
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.CheckDraftUseCase
import com.empathy.ai.domain.usecase.GenerateReplyUseCase
import com.empathy.ai.domain.usecase.PolishDraftUseCase
import com.empathy.ai.domain.usecase.RefinementUseCase
import com.empathy.ai.domain.util.ErrorHandler
import com.empathy.ai.domain.util.FloatingView
import com.empathy.ai.domain.util.FloatingViewDebugLogger
import com.empathy.ai.presentation.ui.floating.FloatingViewV2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * 悬浮窗服务
 * 
 * 职责：
 * - 管理悬浮窗的生命周期
 * - 显示和管理悬浮视图
 * - 处理用户交互事件
 * - 调用 UseCase 执行业务逻辑
 * 
 * 生命周期：
 * 1. onCreate: 初始化 WindowManager 和协程作用域
 * 2. onStartCommand: 启动前台服务并显示悬浮视图
 * 3. onDestroy: 移除悬浮视图并清理资源
 */
@AndroidEntryPoint
class FloatingWindowService : Service() {
    
    @Inject
    lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    
    @Inject
    lateinit var checkDraftUseCase: CheckDraftUseCase
    
    // TD-00009: 新增UseCase注入
    @Inject
    lateinit var polishDraftUseCase: PolishDraftUseCase
    
    @Inject
    lateinit var generateReplyUseCase: GenerateReplyUseCase
    
    @Inject
    lateinit var refinementUseCase: RefinementUseCase
    
    @Inject
    lateinit var contactRepository: ContactRepository
    
    @Inject
    lateinit var floatingWindowPreferences: com.empathy.ai.data.local.FloatingWindowPreferences
    
    @Inject
    lateinit var aiProviderRepository: com.empathy.ai.domain.repository.AiProviderRepository
    
    private lateinit var windowManager: WindowManager
    private var floatingView: FloatingView? = null  // 旧版View（保留兼容）
    private var floatingViewV2: FloatingViewV2? = null  // TD-00009: 新版View
    private var useNewUI: Boolean = true  // TD-00009: 是否使用新UI
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // TD-00009: UI状态管理
    private var currentUiState = FloatingWindowUiState()
    private var lastInputText: String = ""
    private var lastContactId: String = ""
    
    // 性能监控
    private var performanceMonitor: com.empathy.ai.domain.util.PerformanceMonitor? = null
    
    // 当前请求信息（用于最小化功能）
    private var currentRequestInfo: com.empathy.ai.domain.model.MinimizedRequestInfo? = null
    
    // 清理任务（用于取消定时清理）
    private var cleanupJob: kotlinx.coroutines.Job? = null
    
    /**
     * 服务创建时调用
     * 
     * 初始化 WindowManager 和其他必要的资源
     */
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 启动性能监控
        performanceMonitor = com.empathy.ai.domain.util.PerformanceMonitor(this)
        performanceMonitor?.startMonitoring()
    }
    
    /**
     * 服务启动时调用
     * 
     * 启动前台服务并显示悬浮视图
     * 
     * @param intent 启动意图
     * @param flags 启动标志
     * @param startId 启动 ID
     * @return START_STICKY 确保服务被杀死后自动重启
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // 处理通知点击事件
            if (intent?.action == ACTION_RESTORE_DIALOG) {
                android.util.Log.d("FloatingWindowService", "收到恢复对话框请求")
                restoreFromMinimized()
                return START_STICKY
            }
            
            // 启动前台服务
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.d("FloatingWindowService", "前台服务启动成功")
            
            // 显示悬浮视图
            showFloatingView()
            
            // 尝试恢复请求状态（应用重启后）
            restoreRequestState()
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "启动前台服务失败", e)
            
            // 尝试使用最简单的通知
            try {
                val fallbackNotification = createFallbackNotification()
                startForeground(NOTIFICATION_ID, fallbackNotification)
                android.util.Log.w("FloatingWindowService", "使用降级通知启动前台服务成功")
                
                // 显示悬浮视图
                showFloatingView()
                
                // 尝试恢复请求状态（应用重启后）
                restoreRequestState()
            } catch (fallbackException: Exception) {
                android.util.Log.e("FloatingWindowService", "降级通知也失败，停止服务", fallbackException)
                
                // 如果连降级方案都失败，停止服务以避免崩溃
                stopSelf()
                return START_NOT_STICKY
            }
        }
        
        return START_STICKY
    }
    
    /**
     * 绑定服务时调用
     * 
     * 本服务不支持绑定，返回 null
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    /**
     * 服务销毁时调用
     * 
     * 移除悬浮视图并清理所有资源
     * 
     * 需求：8.1, 8.2, 8.3
     */
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("FloatingWindowService", "服务销毁开始")
        
        try {
            // 停止性能监控并记录报告
            performanceMonitor?.let {
                val report = it.stopMonitoring()
                android.util.Log.i("FloatingWindowService", report.toString())
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "停止性能监控失败", e)
        }
        performanceMonitor = null
        
        try {
            // 取消清理任务
            cleanupJob?.cancel()
            cleanupJob = null
            android.util.Log.d("FloatingWindowService", "清理任务已取消")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "取消清理任务失败", e)
        }
        
        try {
            // 移除最小化指示器（如果存在）
            floatingView?.let { view ->
                if (view.isMinimized()) {
                    // 调用 FloatingView 的内部方法移除指示器
                    // 注意：hideMinimizedIndicator 是 private 方法，我们通过恢复对话框来清理
                    try {
                        view.restoreFromMinimized()
                        android.util.Log.d("FloatingWindowService", "最小化指示器已移除")
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindowService", "移除最小化指示器失败", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "清理最小化指示器失败", e)
        }
        
        try {
            // TD-00009: 移除新版最小化指示器
            minimizedIndicatorV2?.let {
                if (it.parent != null) {
                    windowManager.removeView(it)
                    android.util.Log.d("FloatingWindowService", "minimizedIndicatorV2移除成功")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "移除minimizedIndicatorV2失败", e)
        }
        minimizedIndicatorV2 = null

        try {
            // TD-00009: 移除新版悬浮视图
            floatingViewV2?.let {
                if (it.parent != null) {
                    windowManager.removeView(it)
                    android.util.Log.d("FloatingWindowService", "FloatingViewV2移除成功")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "移除FloatingViewV2失败", e)
        }
        floatingViewV2 = null
        
        try {
            // 移除旧版悬浮视图
            floatingView?.let {
                if (it.parent != null) {
                    windowManager.removeView(it)
                    android.util.Log.d("FloatingWindowService", "悬浮视图移除成功")
                } else {
                    android.util.Log.w("FloatingWindowService", "悬浮视图已被移除，跳过")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "移除悬浮视图失败", e)
        }
        floatingView = null
        
        try {
            // 取消所有协程
            serviceScope.cancel()
            android.util.Log.d("FloatingWindowService", "协程作用域已取消")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "取消协程作用域失败", e)
        }
        
        android.util.Log.d("FloatingWindowService", "服务销毁完成")
    }
    
    /**
     * 显示悬浮视图
     *
     * 创建并添加悬浮视图到 WindowManager
     * 恢复上次保存的按钮位置
     *
     * 性能优化：
     * - 视图复用：只创建一次 FloatingView，避免重复创建
     * - 硬件加速：启用硬件加速提升渲染性能
     *
     * TD-00009: 支持新版三Tab UI（FloatingViewV2）
     */
    private fun showFloatingView() {
        try {
            if (useNewUI) {
                // TD-00009: 使用新版三Tab UI
                showFloatingViewV2()
                return
            }
            
            // 旧版UI（保留兼容）
            if (floatingView == null) {
                android.util.Log.d("FloatingWindowService", "创建悬浮视图")
                
                // 创建 FloatingView 实例，添加详细的错误日志
                try {
                    // Bug修复：Service的Context没有主题，需要使用ContextThemeWrapper包装
                    val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_GiveLove)
                    floatingView = FloatingView(themedContext)
                    android.util.Log.d("FloatingWindowService", "FloatingView 实例创建成功（使用主题包装）")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "创建 FloatingView 实例失败，可能是XML解析错误", e)
                    throw e
                }
                
                // 设置回调函数
                try {
                    floatingView?.apply {
                        onAnalyzeClick = { handleAnalyze() }
                        onCheckClick = { handleCheck() }
                        onPositionChanged = { x, y ->
                            // 保存位置变化
                            try {
                                floatingWindowPreferences.saveButtonPosition(x, y)
                            } catch (e: Exception) {
                                android.util.Log.e("FloatingWindowService", "保存按钮位置失败", e)
                            }
                        }
                        onMinimizeClicked = {
                            // 处理最小化按钮点击
                            // Bug修复：使用 this@FloatingWindowService 明确指定调用 Service 的 minimizeDialog()
                            // 而不是 FloatingView 的 minimizeDialog()（因为这里在 apply 块内部）
                            try {
                                android.util.Log.d("FloatingWindowService", "onMinimizeClicked 回调被触发，准备调用 Service.minimizeDialog()")
                                this@FloatingWindowService.minimizeDialog()
                            } catch (e: Exception) {
                                android.util.Log.e("FloatingWindowService", "最小化对话框失败", e)
                            }
                        }
                        
                        // 启用硬件加速（需求 6.3）
                        try {
                            enableHardwareAcceleration()
                        } catch (e: Exception) {
                            android.util.Log.e("FloatingWindowService", "启用硬件加速失败", e)
                        }
                    }
                    android.util.Log.d("FloatingWindowService", "FloatingView 回调函数设置成功")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "设置 FloatingView 回调函数失败", e)
                    throw e
                }
                
                // 配置 WindowManager.LayoutParams
                val params = try {
                    floatingView!!.createLayoutParams()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "创建布局参数失败，使用默认参数", e)
                    createDefaultLayoutParams()
                }
                
                // 恢复上次保存的位置
                try {
                    val savedX = floatingWindowPreferences.getButtonX()
                    val savedY = floatingWindowPreferences.getButtonY()
                    params.x = savedX
                    params.y = savedY
                    android.util.Log.d("FloatingWindowService", "恢复按钮位置: ($savedX, $savedY)")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "恢复按钮位置失败，使用默认位置", e)
                    params.x = 0
                    params.y = 100
                }
                
                // 添加视图到 WindowManager
                try {
                    windowManager.addView(floatingView, params)
                    android.util.Log.d("FloatingWindowService", "悬浮视图添加成功")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "添加悬浮视图失败", e)
                    // 清理资源
                    floatingView = null
                    throw e
                }
            } else {
                android.util.Log.d("FloatingWindowService", "悬浮视图已存在，跳过创建")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "显示悬浮视图失败", e)
            // 清理资源
            floatingView = null
            
            // 通知用户
            try {
                ErrorHandler.handleError(this,
                    com.empathy.ai.domain.model.FloatingWindowError.ServiceError("无法显示悬浮窗：${e.message}"))
            } catch (errorHandlerException: Exception) {
                android.util.Log.e("FloatingWindowService", "错误处理也失败", errorHandlerException)
            }
        }
    }
    
    /**
     * 创建默认的 WindowManager.LayoutParams
     *
     * 当 FloatingView.createLayoutParams() 失败时使用
     *
     * @return 默认布局参数
     */
    private fun createDefaultLayoutParams(): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
        }
    }
    
    /**
     * 处理分析请求
     * 
     * 加载联系人列表并显示输入对话框
     * 
     * 性能优化：
     * - 后台线程执行：使用 Dispatchers.IO 执行数据库查询（需求 6.4）
     * - 超时控制：设置 5 秒超时（需求 6.2）
     */
    private fun handleAnalyze() {
        serviceScope.launch {
            try {
                // 在后台线程执行数据库查询
                val contacts = withTimeout(OPERATION_TIMEOUT_MS) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        contactRepository.getAllProfiles().first()
                    }
                }
                
                if (contacts.isEmpty()) {
                    val error = FloatingWindowError.ValidationError("contact")
                    ErrorHandler.handleError(this@FloatingWindowService, error)
                    return@launch
                }
                
                floatingView?.showInputDialog(
                    actionType = ActionType.ANALYZE,
                    contacts = contacts,
                    onConfirm = { contactId, text ->
                        performAnalyze(contactId, text)
                    }
                )
            } catch (e: TimeoutCancellationException) {
                val error = FloatingWindowError.ServiceError("加载联系人超时")
                ErrorHandler.handleError(this@FloatingWindowService, error)
            } catch (e: Exception) {
                val error = FloatingWindowError.ServiceError("加载联系人失败：${e.message}")
                ErrorHandler.handleError(this@FloatingWindowService, error)
            }
        }
    }
    
    /**
     * 执行分析
     * 
     * 调用 AnalyzeChatUseCase 进行聊天分析
     * 
     * @param contactId 联系人 ID
     * @param text 聊天内容
     * 
     * 性能优化：
     * - 后台线程执行：UseCase 调用在 IO 线程执行（需求 6.4）
     * - 超时控制：设置 10 秒超时（需求 6.2）
     * - 内存检查：执行前检查内存使用情况（需求 6.1）
     */
    private fun performAnalyze(contactId: String, text: String) {
        serviceScope.launch {
            try {
                // 验证输入
                if (text.isBlank()) {
                    val error = FloatingWindowError.ValidationError("text")
                    ErrorHandler.handleError(this@FloatingWindowService, error)
                    return@launch
                }
                
                if (text.length > 5000) {
                    val error = FloatingWindowError.ValidationError("textLength")
                    ErrorHandler.handleError(this@FloatingWindowService, error)
                    return@launch
                }
                
                // 保存当前请求信息（用于最小化功能）
                currentRequestInfo = com.empathy.ai.domain.model.MinimizedRequestInfo(
                    id = java.util.UUID.randomUUID().toString(),
                    type = ActionType.ANALYZE,
                    timestamp = System.currentTimeMillis()
                )
                android.util.Log.d("FloatingWindowService", "已保存请求信息: ${currentRequestInfo?.id}")
                
                // 检查内存使用情况
                performanceMonitor?.let {
                    if (!it.isMemoryHealthy()) {
                        android.util.Log.w("FloatingWindowService", "内存使用较高，建议清理")
                        it.requestGarbageCollection()
                    }
                }
                
                floatingView?.showLoading("正在分析聊天内容...")
                
                // 获取动态超时时间
                val timeoutMs = getAiTimeout()
                
                // 在后台线程执行 AI 分析
                val result = withTimeout(timeoutMs) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        analyzeChatUseCase(contactId, listOf(text))
                    }
                }
                
                result.onSuccess { analysisResult ->
                    try {
                        android.util.Log.d("FloatingWindowService", "分析成功，显示结果")
                        
                        // 如果处于最小化状态，更新指示器状态为成功
                        if (floatingView?.isMinimized() == true) {
                            floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.SUCCESS)
                            android.util.Log.d("FloatingWindowService", "指示器状态已更新为成功")
                            
                            // 启动清理定时器（需求 8.4）
                            cleanupCompletedIndicator()
                        }
                        
                        // 发送完成通知（仅在最小化状态下）
                        sendCompletionNotification(analysisResult, true)
                        
                        // 显示分析结果
                        floatingView?.showAnalysisResult(analysisResult)
                        
                        // 清除当前请求信息（请求已完成）
                        currentRequestInfo = null
                        android.util.Log.d("FloatingWindowService", "分析成功，currentRequestInfo 已清除")
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindowService", "显示分析结果失败", e)
                        
                        // Bug修复：即使显示结果失败，也要清除请求信息，避免后续最小化时显示指示器
                        currentRequestInfo = null
                        android.util.Log.d("FloatingWindowService", "显示结果失败，currentRequestInfo 已清除")
                        
                        val useCaseError = FloatingWindowError.UseCaseError(e)
                        ErrorHandler.handleError(this@FloatingWindowService, useCaseError)
                        // 重置输入框状态，允许用户重新尝试
                        try {
                            floatingView?.resetInputState()
                        } catch (resetException: Exception) {
                            android.util.Log.e("FloatingWindowService", "重置输入框状态失败", resetException)
                            // 尝试隐藏输入对话框作为最后的降级方案
                            try {
                                floatingView?.hideInputDialog()
                            } catch (hideException: Exception) {
                                android.util.Log.e("FloatingWindowService", "隐藏输入对话框也失败", hideException)
                            }
                        }
                    }
                }.onFailure { error ->
                    android.util.Log.e("FloatingWindowService", "分析失败", error)
                    
                    // Bug修复：分析失败时也要清除请求信息，避免后续最小化时显示指示器
                    currentRequestInfo = null
                    android.util.Log.d("FloatingWindowService", "分析失败，currentRequestInfo 已清除")
                    
                    // 如果处于最小化状态，更新指示器状态为错误
                    if (floatingView?.isMinimized() == true) {
                        floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                        android.util.Log.d("FloatingWindowService", "指示器状态已更新为错误")
                        
                        // 启动清理定时器（需求 8.4）
                        cleanupCompletedIndicator()
                    }
                    
                    // 发送错误通知（仅在最小化状态下）
                    sendCompletionNotification(null, false)
                    
                    val useCaseError = FloatingWindowError.UseCaseError(error)
                    ErrorHandler.handleError(this@FloatingWindowService, useCaseError)
                    // 重置输入框状态，允许用户重新尝试
                    try {
                        floatingView?.resetInputState()
                    } catch (resetException: Exception) {
                        android.util.Log.e("FloatingWindowService", "重置输入框状态也失败", resetException)
                        // 尝试隐藏输入对话框作为最后的降级方案
                        try {
                            floatingView?.hideInputDialog()
                        } catch (hideException: Exception) {
                            android.util.Log.e("FloatingWindowService", "隐藏输入对话框也失败", hideException)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("FloatingWindowService", "分析操作超时", e)
                
                // Bug修复：超时时也要清除请求信息
                currentRequestInfo = null
                android.util.Log.d("FloatingWindowService", "分析超时，currentRequestInfo 已清除")
                
                // 如果处于最小化状态，更新指示器状态为错误
                if (floatingView?.isMinimized() == true) {
                    floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                    android.util.Log.d("FloatingWindowService", "指示器状态已更新为错误（超时）")
                    
                    // 启动清理定时器（需求 8.4）
                    cleanupCompletedIndicator()
                }
                
                // 发送错误通知（仅在最小化状态下）
                sendCompletionNotification(null, false)
                
                val error = FloatingWindowError.ServiceError("操作超时，请检查网络连接")
                ErrorHandler.handleError(this@FloatingWindowService, error)
                // 重置输入框状态，允许用户重新尝试
                try {
                    floatingView?.resetInputState()
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingWindowService", "超时后重置输入框状态失败", resetException)
                    // 尝试隐藏输入对话框作为最后的降级方案
                    try {
                        floatingView?.hideInputDialog()
                    } catch (hideException: Exception) {
                        android.util.Log.e("FloatingWindowService", "超时后隐藏输入对话框也失败", hideException)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "分析操作发生未预期异常", e)
                
                // Bug修复：异常时也要清除请求信息
                currentRequestInfo = null
                android.util.Log.d("FloatingWindowService", "分析异常，currentRequestInfo 已清除")
                
                // 如果处于最小化状态，更新指示器状态为错误
                if (floatingView?.isMinimized() == true) {
                    floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                    android.util.Log.d("FloatingWindowService", "指示器状态已更新为错误（异常）")
                    
                    // 启动清理定时器（需求 8.4）
                    cleanupCompletedIndicator()
                }
                
                // 发送错误通知（仅在最小化状态下）
                sendCompletionNotification(null, false)
                
                val error = FloatingWindowError.ServiceError("操作失败：${e.message}")
                ErrorHandler.handleError(this@FloatingWindowService, error)
                // 重置输入框状态，允许用户重新尝试
                try {
                    floatingView?.resetInputState()
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingWindowService", "异常后重置输入框状态失败", resetException)
                    // 尝试隐藏输入对话框作为最后的降级方案
                    try {
                        floatingView?.hideInputDialog()
                    } catch (hideException: Exception) {
                        android.util.Log.e("FloatingWindowService", "异常后隐藏输入对话框也失败", hideException)
                    }
                }
            } finally {
                try {
                    floatingView?.hideLoading()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "隐藏加载状态失败", e)
                }
            }
        }
    }
    
    /**
     * 处理检查请求
     * 
     * 显示输入对话框供用户输入草稿内容
     * 
     * 性能优化：
     * - 后台线程执行：使用 Dispatchers.IO 执行数据库查询（需求 6.4）
     * - 超时控制：设置 5 秒超时（需求 6.2）
     */
    private fun handleCheck() {
        serviceScope.launch {
            try {
                // 在后台线程执行数据库查询
                val contacts = withTimeout(OPERATION_TIMEOUT_MS) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        contactRepository.getAllProfiles().first()
                    }
                }
                
                if (contacts.isEmpty()) {
                    val error = FloatingWindowError.ValidationError("contact")
                    ErrorHandler.handleError(this@FloatingWindowService, error)
                    return@launch
                }
                
                floatingView?.showInputDialog(
                    actionType = ActionType.CHECK,
                    contacts = contacts,
                    onConfirm = { contactId, text ->
                        performCheck(contactId, text)
                    }
                )
            } catch (e: TimeoutCancellationException) {
                val error = FloatingWindowError.ServiceError("加载联系人超时")
                ErrorHandler.handleError(this@FloatingWindowService, error)
            } catch (e: Exception) {
                val error = FloatingWindowError.ServiceError("加载联系人失败：${e.message}")
                ErrorHandler.handleError(this@FloatingWindowService, error)
            }
        }
    }
    
    /**
     * 执行检查
     * 
     * 调用 CheckDraftUseCase 进行安全检查
     * 
     * @param contactId 联系人 ID
     * @param text 草稿内容
     * 
     * 性能优化：
     * - 后台线程执行：UseCase 调用在 IO 线程执行（需求 6.4）
     * - 超时控制：设置 10 秒超时（需求 6.2）
     * - 内存检查：执行前检查内存使用情况（需求 6.1）
     */
    private fun performCheck(contactId: String, text: String) {
        serviceScope.launch {
            try {
                // 验证输入
                if (text.isBlank()) {
                    val error = FloatingWindowError.ValidationError("text")
                    ErrorHandler.handleError(this@FloatingWindowService, error)
                    return@launch
                }
                
                if (text.length > 5000) {
                    val error = FloatingWindowError.ValidationError("textLength")
                    ErrorHandler.handleError(this@FloatingWindowService, error)
                    return@launch
                }
                
                // 保存当前请求信息（用于最小化功能）
                currentRequestInfo = com.empathy.ai.domain.model.MinimizedRequestInfo(
                    id = java.util.UUID.randomUUID().toString(),
                    type = ActionType.CHECK,
                    timestamp = System.currentTimeMillis()
                )
                android.util.Log.d("FloatingWindowService", "已保存请求信息: ${currentRequestInfo?.id}")
                
                // 检查内存使用情况
                performanceMonitor?.let {
                    if (!it.isMemoryHealthy()) {
                        android.util.Log.w("FloatingWindowService", "内存使用较高，建议清理")
                        it.requestGarbageCollection()
                    }
                }
                
                floatingView?.showLoading("正在检查内容安全...")
                
                // 获取动态超时时间
                val timeoutMs = getAiTimeout()
                
                // 在后台线程执行安全检查
                val result = withTimeout(timeoutMs) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        checkDraftUseCase(contactId, text)
                    }
                }
                
                result.onSuccess { safetyResult ->
                    try {
                        android.util.Log.d("FloatingWindowService", "安全检查成功，显示结果")
                        
                        // 如果处于最小化状态，更新指示器状态为成功
                        if (floatingView?.isMinimized() == true) {
                            floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.SUCCESS)
                            android.util.Log.d("FloatingWindowService", "指示器状态已更新为成功")
                        }
                        
                        // 发送完成通知（仅在最小化状态下）
                        sendCompletionNotification(safetyResult, true)
                        
                        // 显示检查结果
                        floatingView?.showSafetyResult(safetyResult)
                        
                        // 清除当前请求信息（请求已完成）
                        currentRequestInfo = null
                        android.util.Log.d("FloatingWindowService", "安全检查成功，currentRequestInfo 已清除")
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindowService", "显示安全检查结果失败", e)
                        
                        // Bug修复：即使显示结果失败，也要清除请求信息
                        currentRequestInfo = null
                        android.util.Log.d("FloatingWindowService", "显示检查结果失败，currentRequestInfo 已清除")
                        
                        val useCaseError = FloatingWindowError.UseCaseError(e)
                        ErrorHandler.handleError(this@FloatingWindowService, useCaseError)
                        // 重置输入框状态，允许用户重新尝试
                        try {
                            floatingView?.resetInputState()
                        } catch (resetException: Exception) {
                            android.util.Log.e("FloatingWindowService", "重置输入框状态失败", resetException)
                            // 尝试隐藏输入对话框作为最后的降级方案
                            try {
                                floatingView?.hideInputDialog()
                            } catch (hideException: Exception) {
                                android.util.Log.e("FloatingWindowService", "隐藏输入对话框也失败", hideException)
                            }
                        }
                    }
                }.onFailure { error ->
                    android.util.Log.e("FloatingWindowService", "安全检查失败", error)
                    
                    // Bug修复：检查失败时也要清除请求信息
                    currentRequestInfo = null
                    android.util.Log.d("FloatingWindowService", "安全检查失败，currentRequestInfo 已清除")
                    
                    // 如果处于最小化状态，更新指示器状态为错误
                    if (floatingView?.isMinimized() == true) {
                        floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                        android.util.Log.d("FloatingWindowService", "指示器状态已更新为错误")
                    }
                    
                    // 发送错误通知（仅在最小化状态下）
                    sendCompletionNotification(null, false)
                    
                    val useCaseError = FloatingWindowError.UseCaseError(error)
                    ErrorHandler.handleError(this@FloatingWindowService, useCaseError)
                    // 重置输入框状态，允许用户重新尝试
                    try {
                        floatingView?.resetInputState()
                    } catch (resetException: Exception) {
                        android.util.Log.e("FloatingWindowService", "重置输入框状态也失败", resetException)
                        // 尝试隐藏输入对话框作为最后的降级方案
                        try {
                            floatingView?.hideInputDialog()
                        } catch (hideException: Exception) {
                            android.util.Log.e("FloatingWindowService", "隐藏输入对话框也失败", hideException)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("FloatingWindowService", "安全检查操作超时", e)
                
                // Bug修复：超时时也要清除请求信息
                currentRequestInfo = null
                android.util.Log.d("FloatingWindowService", "安全检查超时，currentRequestInfo 已清除")
                
                // 如果处于最小化状态，更新指示器状态为错误
                if (floatingView?.isMinimized() == true) {
                    floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                    android.util.Log.d("FloatingWindowService", "指示器状态已更新为错误（超时）")
                }
                
                val error = FloatingWindowError.ServiceError("操作超时，请检查网络连接")
                ErrorHandler.handleError(this@FloatingWindowService, error)
                // 重置输入框状态，允许用户重新尝试
                try {
                    floatingView?.resetInputState()
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingWindowService", "超时后重置输入框状态失败", resetException)
                    // 尝试隐藏输入对话框作为最后的降级方案
                    try {
                        floatingView?.hideInputDialog()
                    } catch (hideException: Exception) {
                        android.util.Log.e("FloatingWindowService", "超时后隐藏输入对话框也失败", hideException)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "安全检查操作发生未预期异常", e)
                
                // Bug修复：异常时也要清除请求信息
                currentRequestInfo = null
                android.util.Log.d("FloatingWindowService", "安全检查异常，currentRequestInfo 已清除")
                
                // 如果处于最小化状态，更新指示器状态为错误
                if (floatingView?.isMinimized() == true) {
                    floatingView?.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                    android.util.Log.d("FloatingWindowService", "指示器状态已更新为错误（异常）")
                }
                
                val error = FloatingWindowError.ServiceError("操作失败：${e.message}")
                ErrorHandler.handleError(this@FloatingWindowService, error)
                // 重置输入框状态，允许用户重新尝试
                try {
                    floatingView?.resetInputState()
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingWindowService", "异常后重置输入框状态失败", resetException)
                    // 尝试隐藏输入对话框作为最后的降级方案
                    try {
                        floatingView?.hideInputDialog()
                    } catch (hideException: Exception) {
                        android.util.Log.e("FloatingWindowService", "异常后隐藏输入对话框也失败", hideException)
                    }
                }
            } finally {
                try {
                    floatingView?.hideLoading()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "隐藏加载状态失败", e)
                }
            }
        }
    }
    
    /**
     * 最小化对话框
     *
     * 将输入对话框最小化为小型指示器
     *
     * 职责：
     * - 获取当前请求信息并保存
     * - 调用 FloatingView 的最小化方法
     * - 记录日志
     *
     * 错误处理：
     * - 捕获所有异常并记录日志
     * - 保持对话框打开状态（不影响用户继续操作）
     * - 通过 ErrorHandler 显示用户友好的错误消息
     *
     * Bug修复：改进无请求情况下的处理逻辑，确保状态转换的一致性
     * 修复：移除不可靠的延迟验证机制，使用原子性操作
     *
     * 需求：1.1, 1.5, 2.1, 4.5, 5.1, 7.1
     */
    fun minimizeDialog() {
        try {
            android.util.Log.d("FloatingWindowService", "")
            android.util.Log.d("FloatingWindowService", "╔══════════════════════════════════════════════════════════════╗")
            android.util.Log.d("FloatingWindowService", "║           开始最小化对话框 - minimizeDialog()                ║")
            android.util.Log.d("FloatingWindowService", "╚══════════════════════════════════════════════════════════════╝")
            
            // 0. 检查 FloatingView 状态
            if (floatingView == null) {
                android.util.Log.e("FloatingWindowService", "❌ FloatingView 不可用")
                return
            }
            android.util.Log.d("FloatingWindowService", "✓ FloatingView 可用")
            
            // 1. 检查是否有正在处理的请求
            val requestInfo = currentRequestInfo
            val hasRequest = requestInfo != null
            
            android.util.Log.d("FloatingWindowService", "")
            android.util.Log.d("FloatingWindowService", "┌─────────────────────────────────────────────────────────────┐")
            android.util.Log.d("FloatingWindowService", "│ 检查 currentRequestInfo 状态                                │")
            android.util.Log.d("FloatingWindowService", "├─────────────────────────────────────────────────────────────┤")
            if (hasRequest) {
                android.util.Log.d("FloatingWindowService", "│ 状态: 有请求                                                │")
                android.util.Log.d("FloatingWindowService", "│ ID: ${requestInfo?.id}                                      │")
                android.util.Log.d("FloatingWindowService", "│ 类型: ${requestInfo?.type}                                  │")
            } else {
                android.util.Log.d("FloatingWindowService", "│ 状态: 无请求 (null)                                         │")
            }
            android.util.Log.d("FloatingWindowService", "└─────────────────────────────────────────────────────────────┘")
            android.util.Log.d("FloatingWindowService", "")
            
            FloatingViewDebugLogger.logMinimizeFlow("检查请求状态", hasRequest,
                if (hasRequest) "请求ID: ${requestInfo?.id}, 类型: ${requestInfo?.type}"
                else "无正在处理的请求")
            
            if (requestInfo == null) {
                // 没有正在处理的请求，安全地关闭对话框返回悬浮按钮
                // 这种情况下不应该显示旋转指示器，而是直接返回悬浮按钮状态
                android.util.Log.i("FloatingWindowService", "")
                android.util.Log.i("FloatingWindowService", "╔══════════════════════════════════════════════════════════════╗")
                android.util.Log.i("FloatingWindowService", "║  【无请求】将调用 hideInputDialog() 返回悬浮按钮状态         ║")
                android.util.Log.i("FloatingWindowService", "║  不会显示旋转指示器！                                        ║")
                android.util.Log.i("FloatingWindowService", "╚══════════════════════════════════════════════════════════════╝")
                FloatingViewDebugLogger.logMinimizeFlow("无请求处理", false, "调用hideInputDialog而非minimizeDialog")
                
                // Bug修复：确保无请求情况下正确处理，不显示最小化指示器
                // 直接调用hideInputDialog()返回按钮状态，确保原子性操作
                try {
                    // 记录操作前的状态
                    val beforeMode = floatingView?.getCurrentModeValue()
                    android.util.Log.d("FloatingWindowService", "【无请求】关闭前模式: $beforeMode")
                    
                    // 原子性操作：直接关闭对话框并返回按钮状态
                    // 注意：这里调用的是 hideInputDialog()，不是 minimizeDialog()
                    android.util.Log.d("FloatingWindowService", "【无请求】正在调用 floatingView.hideInputDialog()...")
                    floatingView?.hideInputDialog()
                    android.util.Log.d("FloatingWindowService", "【无请求】floatingView.hideInputDialog() 调用完成")
                    
                    // 验证状态转换
                    val afterMode = floatingView?.getCurrentModeValue()
                    android.util.Log.d("FloatingWindowService", "【无请求】关闭后模式: $afterMode")
                    
                    if (afterMode != com.empathy.ai.domain.util.FloatingView.Mode.BUTTON) {
                        android.util.Log.e("FloatingWindowService", "【无请求】关闭后状态异常: $afterMode，尝试强制重置")
                        floatingView?.forceResetToButtonMode()
                        
                        // 再次验证
                        val finalMode = floatingView?.getCurrentModeValue()
                        android.util.Log.d("FloatingWindowService", "【无请求】强制重置后模式: $finalMode")
                    }
                    
                    android.util.Log.i("FloatingWindowService", "【无请求】成功返回按钮模式")
                    FloatingViewDebugLogger.logMinimizeFlow("无请求处理完成", false, "成功返回按钮模式")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "【无请求】关闭对话框失败", e)
                    FloatingViewDebugLogger.logException("无请求关闭失败", e)
                    
                    // 降级处理：强制重置到按钮模式
                    try {
                        floatingView?.forceResetToButtonMode()
                        android.util.Log.w("FloatingWindowService", "【无请求】降级处理完成")
                    } catch (fallbackException: Exception) {
                        android.util.Log.e("FloatingWindowService", "【无请求】降级处理也失败", fallbackException)
                    }
                }
                
                android.util.Log.d("FloatingWindowService", "========== 无请求最小化完成 ==========")
                return
            }
            
            // 有请求，执行真正的最小化（显示旋转指示器）
            android.util.Log.i("FloatingWindowService", "")
            android.util.Log.i("FloatingWindowService", "╔══════════════════════════════════════════════════════════════╗")
            android.util.Log.i("FloatingWindowService", "║  【有请求】将调用 minimizeDialog() 显示旋转指示器             ║")
            android.util.Log.i("FloatingWindowService", "╚══════════════════════════════════════════════════════════════╝")
            
            // 2. 单一指示器约束：如果已有最小化指示器，先关闭旧的（需求 8.3）
            if (floatingView?.isMinimized() == true) {
                android.util.Log.i("FloatingWindowService", "检测到已存在的最小化指示器，先关闭旧指示器")
                try {
                    // 取消之前的清理任务
                    cleanupJob?.cancel()
                    cleanupJob = null
                    
                    // 恢复对话框（这会移除旧指示器）
                    floatingView?.restoreFromMinimized()
                    
                    // 清除旧的请求信息
                    floatingWindowPreferences.clearRequestInfo()
                    
                    android.util.Log.d("FloatingWindowService", "旧指示器已关闭")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "关闭旧指示器失败，继续创建新指示器", e)
                    // 即使关闭失败，也继续创建新指示器
                }
            }
            
            // 2.5 Bug修复：让 FloatingView 保存当前输入状态，然后获取更新后的请求信息
            try {
                floatingView?.saveCurrentInputState()
                val updatedRequestInfo = floatingView?.getCurrentRequestInfo()
                if (updatedRequestInfo != null) {
                    currentRequestInfo = updatedRequestInfo
                    android.util.Log.d("FloatingWindowService", "已更新请求信息，输入文本长度: ${updatedRequestInfo.inputText.length}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "保存当前输入状态失败", e)
                // 继续使用原有的请求信息
            }
            
            // 3. 保存请求信息到持久化存储（使用更新后的请求信息）
            val finalRequestInfo = currentRequestInfo ?: requestInfo
            try {
                floatingWindowPreferences.saveRequestInfo(finalRequestInfo)
                android.util.Log.d("FloatingWindowService", "请求信息已保存: ${finalRequestInfo.id}, 输入文本长度: ${finalRequestInfo.inputText.length}")
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "保存请求信息失败", e)
                val error = FloatingWindowError.ServiceError("无法保存请求状态: ${e.message}")
                ErrorHandler.handleError(this, error)
                return
            }
            
            // 4. 调用 FloatingView 的最小化方法
            try {
                if (floatingView == null) {
                    android.util.Log.e("FloatingWindowService", "悬浮视图不可用")
                    val error = FloatingWindowError.ServiceError("悬浮视图不可用")
                    ErrorHandler.handleError(this, error)
                    return
                }
                
                floatingView?.minimizeDialog()
                android.util.Log.d("FloatingWindowService", "对话框已最小化")
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "调用 FloatingView.minimizeDialog() 失败", e)
                val error = FloatingWindowError.ServiceError("UI 操作失败: ${e.message}")
                ErrorHandler.handleError(this, error)
                return
            }
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "最小化对话框发生未预期异常", e)
            val error = FloatingWindowError.ServiceError("未知错误: ${e.message}")
            ErrorHandler.handleError(this, error)
            // 保持对话框打开状态，用户可以继续操作
        }
    }
    
    /**
     * 安全关闭输入对话框
     *
     * Bug修复：确保无请求情况下关闭对话框的原子性和可靠性
     * 修复：移除延迟验证，使用立即验证和原子性操作
     */
    private fun safeCloseInputDialog() {
        try {
            android.util.Log.d("FloatingWindowService", "开始安全关闭输入对话框")
            FloatingViewDebugLogger.logMinimizeFlow("安全关闭开始", false, "无请求情况下安全关闭对话框")
            
            // 记录关闭前的状态
            val beforeMode = floatingView?.getCurrentModeValue()
            android.util.Log.d("FloatingWindowService", "关闭前模式: $beforeMode")
            FloatingViewDebugLogger.logMinimizeFlow("关闭前状态", false, "模式: $beforeMode")
            
            // 原子性关闭对话框
            try {
                floatingView?.hideInputDialog()
                android.util.Log.d("FloatingWindowService", "hideInputDialog() 调用成功")
                FloatingViewDebugLogger.logMinimizeFlow("hideInputDialog调用", false, "调用成功")
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "hideInputDialog() 调用失败", e)
                FloatingViewDebugLogger.logException("hideInputDialog调用失败", e)
                
                // 尝试强制重置
                try {
                    floatingView?.forceResetToButtonMode()
                    android.util.Log.d("FloatingWindowService", "强制重置成功")
                    FloatingViewDebugLogger.logMinimizeFlow("强制重置", false, "成功")
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingWindowService", "强制重置失败", resetException)
                    FloatingViewDebugLogger.logException("强制重置失败", resetException)
                    throw resetException
                }
            }
            
            // Bug修复：立即验证状态，不使用延迟验证
            val afterMode = floatingView?.getCurrentModeValue()
            android.util.Log.d("FloatingWindowService", "关闭后模式: $afterMode")
            FloatingViewDebugLogger.logMinimizeFlow("关闭后状态", false, "模式: $afterMode")
            
            if (afterMode != com.empathy.ai.domain.util.FloatingView.Mode.BUTTON) {
                android.util.Log.e("FloatingWindowService", "关闭对话框后状态异常: $afterMode")
                FloatingViewDebugLogger.logMinimizeFlow("状态异常", false, "期望BUTTON，实际$afterMode")
                
                // 立即强制重置，不使用延迟
                try {
                    floatingView?.forceResetToButtonMode()
                    android.util.Log.w("FloatingWindowService", "立即强制重置状态完成")
                    FloatingViewDebugLogger.logMinimizeFlow("立即强制重置", false, "完成")
                    
                    // 再次验证
                    val finalMode = floatingView?.getCurrentModeValue()
                    if (finalMode == com.empathy.ai.domain.util.FloatingView.Mode.BUTTON) {
                        android.util.Log.d("FloatingWindowService", "状态重置验证成功: $finalMode")
                        FloatingViewDebugLogger.logMinimizeFlow("状态验证", false, "成功: $finalMode")
                    } else {
                        android.util.Log.e("FloatingWindowService", "状态重置验证失败: $finalMode")
                        FloatingViewDebugLogger.logMinimizeFlow("状态验证失败", false, "失败: $finalMode")
                    }
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingWindowService", "立即强制重置失败", resetException)
                    FloatingViewDebugLogger.logException("立即强制重置失败", resetException)
                    throw resetException
                }
            } else {
                android.util.Log.d("FloatingWindowService", "对话框已正常关闭，状态正确: $afterMode")
                FloatingViewDebugLogger.logMinimizeFlow("正常关闭", false, "状态正确: $afterMode")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "安全关闭输入对话框失败", e)
            FloatingViewDebugLogger.logException("安全关闭输入对话框失败", e)
            
            // 最后的降级处理
            try {
                floatingView?.forceResetToButtonMode()
                android.util.Log.w("FloatingWindowService", "降级处理：强制重置到按钮模式")
                FloatingViewDebugLogger.logMinimizeFlow("降级处理", false, "强制重置到按钮模式")
            } catch (fallbackException: Exception) {
                android.util.Log.e("FloatingWindowService", "降级处理也失败", fallbackException)
                FloatingViewDebugLogger.logException("降级处理失败", fallbackException)
            }
        }
    }
    
    /**
     * 从最小化状态恢复对话框
     * 
     * 将最小化指示器恢复为完整的输入对话框
     * 
     * 职责：
     * - 获取保存的请求信息
     * - 调用 FloatingView 的恢复方法
     * - 清除保存的数据
     * 
     * 错误处理：
     * - 捕获所有异常并记录日志
     * - 保持指示器显示（不自动关闭）
     * - 通过 ErrorHandler 显示用户友好的错误消息
     * - 提供重试选项（用户可以再次点击指示器）
     * 
     * 需求：1.4, 3.3, 5.5, 7.2
     */
    fun restoreFromMinimized() {
        try {
            android.util.Log.d("FloatingWindowService", "开始恢复对话框")
            
            // 1. 获取保存的请求信息
            val requestInfo = try {
                floatingWindowPreferences.getRequestInfo()
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "读取请求信息失败", e)
                val error = FloatingWindowError.ServiceError("无法读取请求状态: ${e.message}")
                ErrorHandler.handleError(this, error)
                return
            }
            
            if (requestInfo == null) {
                android.util.Log.w("FloatingWindowService", "无保存的请求信息，尝试恢复默认状态")
                // 即使没有请求信息，也尝试恢复对话框
                try {
                    if (floatingView == null) {
                        android.util.Log.e("FloatingWindowService", "悬浮视图不可用")
                        val error = FloatingWindowError.ServiceError("悬浮视图不可用")
                        ErrorHandler.handleError(this, error)
                        return
                    }
                    
                    floatingView?.restoreFromMinimized()
                    android.util.Log.d("FloatingWindowService", "对话框已恢复（无请求信息）")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "恢复对话框失败", e)
                    val error = FloatingWindowError.ServiceError("UI 操作失败: ${e.message}")
                    ErrorHandler.handleError(this, error)
                }
                return
            }
            
            android.util.Log.d("FloatingWindowService", "找到保存的请求信息: ${requestInfo.id}, 输入文本长度: ${requestInfo.inputText.length}")
            
            // 2. 将保存的请求信息设置到 FloatingView 中（用于恢复输入内容）
            try {
                if (floatingView == null) {
                    android.util.Log.e("FloatingWindowService", "悬浮视图不可用")
                    val error = FloatingWindowError.ServiceError("悬浮视图不可用")
                    ErrorHandler.handleError(this, error)
                    return
                }
                
                // Bug修复：在恢复前设置请求信息，以便 FloatingView 可以恢复用户输入
                floatingView?.setCurrentRequestInfo(
                    contactId = requestInfo.contactId,
                    inputText = requestInfo.inputText,
                    actionType = requestInfo.type
                )
                android.util.Log.d("FloatingWindowService", "已将请求信息设置到 FloatingView")
                
                floatingView?.restoreFromMinimized()
                android.util.Log.d("FloatingWindowService", "对话框已恢复")
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "调用 FloatingView.restoreFromMinimized() 失败", e)
                val error = FloatingWindowError.ServiceError("UI 操作失败: ${e.message}")
                ErrorHandler.handleError(this, error)
                return
            }
            
            // 3. 清除保存的请求信息
            try {
                floatingWindowPreferences.clearRequestInfo()
                android.util.Log.d("FloatingWindowService", "请求信息已清除")
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "清除请求信息失败（非致命错误）", e)
                // 这不是致命错误，继续执行
            }
            
            // 4. 清除当前请求信息
            currentRequestInfo = null
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "恢复对话框发生未预期异常", e)
            val error = FloatingWindowError.ServiceError("未知错误: ${e.message}")
            ErrorHandler.handleError(this, error)
            // 保持指示器显示，用户可以重试
        }
    }
    
    /**
     * 恢复请求状态
     * 
     * 应用被杀死后重启时，尝试恢复未完成的请求状态
     * 
     * 职责：
     * - 获取保存的请求信息
     * - 检查请求是否过期（10 分钟）
     * - 如果未过期，恢复最小化指示器
     * - 如果过期，清除数据
     * 
     * 需求：9.1, 9.2, 9.3, 9.4
     */
    private fun restoreRequestState() {
        try {
            android.util.Log.d("FloatingWindowService", "开始恢复请求状态")
            
            // 1. 获取保存的请求信息
            val requestInfo = floatingWindowPreferences.getRequestInfo()
            if (requestInfo == null) {
                android.util.Log.d("FloatingWindowService", "没有未完成的请求，跳过恢复")
                return
            }
            
            android.util.Log.d("FloatingWindowService", "找到保存的请求信息: ${requestInfo.id}, 类型: ${requestInfo.type}")
            
            // 2. 检查请求是否已过期（超过 10 分钟）
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - requestInfo.timestamp
            val expirationTime = 10 * 60 * 1000L // 10 分钟
            
            if (elapsedTime > expirationTime) {
                android.util.Log.w("FloatingWindowService", "请求已过期（${elapsedTime / 1000}秒），清除状态")
                floatingWindowPreferences.clearRequestInfo()
                return
            }
            
            android.util.Log.d("FloatingWindowService", "请求未过期（${elapsedTime / 1000}秒），开始恢复")
            
            // 3. 恢复当前请求信息
            currentRequestInfo = requestInfo
            
            // 4. 判断请求状态
            // 如果请求时间超过 10 秒，视为失败
            val processingTimeout = 10 * 1000L // 10 秒
            val shouldMarkAsFailed = elapsedTime > processingTimeout
            
            if (shouldMarkAsFailed) {
                android.util.Log.w("FloatingWindowService", "请求处理超时（${elapsedTime / 1000}秒），标记为失败")
                
                // 恢复指示器为错误状态
                floatingView?.let { view ->
                    // 先最小化对话框（如果还没有最小化）
                    if (!view.isMinimized()) {
                        view.minimizeDialog()
                    }
                    // 更新指示器状态为错误
                    view.updateIndicatorState(com.empathy.ai.domain.util.IndicatorState.ERROR)
                }
                
                android.util.Log.d("FloatingWindowService", "指示器已恢复为错误状态")
            } else {
                android.util.Log.d("FloatingWindowService", "请求仍在处理中，恢复为加载状态")
                
                // 恢复指示器为加载状态
                floatingView?.let { view ->
                    // 先最小化对话框（如果还没有最小化）
                    if (!view.isMinimized()) {
                        view.minimizeDialog()
                    }
                    // 保持加载状态（默认状态）
                }
                
                android.util.Log.d("FloatingWindowService", "指示器已恢复为加载状态")
                android.util.Log.i("FloatingWindowService", "注意：实际的 AI 请求无法恢复，用户需要点击指示器查看详情并选择重试")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "恢复请求状态失败", e)
            
            // 清除损坏的状态数据
            try {
                floatingWindowPreferences.clearRequestInfo()
                android.util.Log.d("FloatingWindowService", "已清除损坏的状态数据")
            } catch (clearException: Exception) {
                android.util.Log.e("FloatingWindowService", "清除状态数据也失败", clearException)
            }
        }
    }
    
    /**
     * 发送完成通知
     * 
     * 当 AI 响应完成且对话框处于最小化状态时发送系统通知
     * 
     * @param result AI 响应结果（可为 null 表示失败）
     * @param isSuccess 是否成功
     * 
     * 需求：3.1, 3.2, 3.3, 3.4, 3.5
     */
    private fun sendCompletionNotification(result: Any?, isSuccess: Boolean) {
        try {
            // 1. 检查是否处于最小化状态
            if (floatingView?.isMinimized() != true) {
                android.util.Log.d("FloatingWindowService", "对话框未最小化，跳过发送通知")
                return
            }
            
            android.util.Log.d("FloatingWindowService", "开始发送完成通知，成功: $isSuccess")
            
            // 2. Android 13+ (API 33) 通知权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.w("FloatingWindowService", "缺少通知权限，无法发送通知")
                    // 降级处理：不发送通知，但不影响功能
                    // 用户仍可以通过点击指示器查看结果
                    return
                }
            }
            
            // 3. 创建完成通知渠道
            createCompletionNotificationChannel()
            
            // 4. 获取当前请求信息
            val requestInfo = currentRequestInfo
            if (requestInfo == null) {
                android.util.Log.w("FloatingWindowService", "无当前请求信息，使用默认通知内容")
            }
            
            // 5. 构建通知内容
            val title = if (isSuccess) {
                "AI 分析完成"
            } else {
                "AI 分析失败"
            }
            
            val content = if (isSuccess) {
                when (requestInfo?.type) {
                    ActionType.ANALYZE -> "聊天分析已完成，点击查看结果"
                    ActionType.CHECK -> "安全检查已完成，点击查看结果"
                    else -> "处理已完成，点击查看结果"
                }
            } else {
                "处理失败，点击查看详情"
            }
            
            // 6. 创建 PendingIntent 指向恢复操作
            val restoreIntent = Intent(this, FloatingWindowService::class.java).apply {
                action = ACTION_RESTORE_DIALOG
            }
            
            // Android 6.0+ (API 23) 需要使用 FLAG_IMMUTABLE
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = android.app.PendingIntent.getService(
                this,
                0,
                restoreIntent,
                pendingIntentFlags
            )
            
            // 7. 构建通知
            val notification = NotificationCompat.Builder(this, CHANNEL_ID_COMPLETION)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(resolveNotificationIcon())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            // 8. 发送通知
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID_COMPLETION, notification)
            
            android.util.Log.d("FloatingWindowService", "完成通知已发送")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "发送完成通知失败", e)
            val error = FloatingWindowError.ServiceError("通知失败: ${e.message}")
            ErrorHandler.handleError(this, error)
        }
    }
    
    /**
     * 创建完成通知渠道
     * 
     * 为 AI 响应完成通知创建专用渠道
     * 
     * Android 8.0+ (API 26) 需要创建通知渠道
     */
    private fun createCompletionNotificationChannel() {
        try {
            // Android 8.0+ (API 26) 通知渠道
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_COMPLETION,
                    "AI 完成通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "AI 分析或检查完成后的通知"
                    // 启用声音和震动
                    enableVibration(true)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
                    ?: throw RuntimeException("无法获取 NotificationManager")
                
                android.util.Log.d("FloatingWindowService", "完成通知渠道已创建")
            } else {
                // Android 8.0 以下不需要创建通知渠道
                android.util.Log.d("FloatingWindowService", "Android 8.0 以下，跳过创建通知渠道")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "创建完成通知渠道失败", e)
            throw e
        }
    }
    
    /**
     * 检查通知权限
     * 
     * Android 13+ (API 33) 需要运行时通知权限
     * 
     * @return 是否有通知权限
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 以下不需要运行时权限
            true
        }
    }
    
    /**
     * 清理已完成的指示器
     * 
     * 在 AI 响应完成后启动定时器，10 分钟后自动清理指示器
     * 
     * 职责：
     * - 延迟 10 分钟后检查指示器状态
     * - 如果指示器仍处于最小化状态且不是加载状态，则清理
     * - 移除指示器视图
     * - 清除持久化数据
     * 
     * 需求：8.1, 8.2, 8.3, 8.4, 8.5
     */
    private fun cleanupCompletedIndicator() {
        try {
            // 取消之前的清理任务（如果存在）
            cleanupJob?.cancel()
            
            // 启动新的清理任务
            cleanupJob = serviceScope.launch {
                try {
                    android.util.Log.d("FloatingWindowService", "清理定时器已启动，将在 10 分钟后执行")
                    
                    // 延迟 10 分钟
                    kotlinx.coroutines.delay(CLEANUP_DELAY_MS)
                    
                    android.util.Log.d("FloatingWindowService", "清理定时器触发，开始检查指示器状态")
                    
                    // 检查指示器是否仍处于最小化状态
                    val isMinimized = floatingView?.isMinimized() == true
                    
                    if (!isMinimized) {
                        android.util.Log.d("FloatingWindowService", "指示器已不在最小化状态，跳过清理")
                        return@launch
                    }
                    
                    // 检查指示器状态（不清理加载状态的指示器）
                    // 注意：由于 FloatingView 没有暴露获取指示器状态的方法，
                    // 我们假设如果仍处于最小化状态且已过 10 分钟，则应该清理
                    
                    android.util.Log.i("FloatingWindowService", "开始清理已完成的指示器")
                    
                    // 恢复对话框（这会移除指示器）
                    try {
                        floatingView?.restoreFromMinimized()
                        android.util.Log.d("FloatingWindowService", "指示器已通过恢复对话框移除")
                        
                        // 立即隐藏对话框，只保留悬浮按钮
                        floatingView?.hideInputDialog()
                        android.util.Log.d("FloatingWindowService", "对话框已隐藏，只保留悬浮按钮")
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindowService", "移除指示器失败", e)
                    }
                    
                    // 清除持久化数据
                    try {
                        floatingWindowPreferences.clearRequestInfo()
                        android.util.Log.d("FloatingWindowService", "持久化数据已清除")
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindowService", "清除持久化数据失败", e)
                    }
                    
                    // 清除当前请求信息
                    currentRequestInfo = null
                    
                    android.util.Log.i("FloatingWindowService", "指示器清理完成")
                    
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("FloatingWindowService", "清理任务被取消")
                    throw e // 重新抛出以正确处理协程取消
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindowService", "清理指示器失败", e)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "启动清理定时器失败", e)
        }
    }

    
    /**
     * 创建前台服务通知
     *
     * @return 通知对象
     */
    private fun createNotification(): Notification {
        val channelId = CHANNEL_ID
        
        try {
            // 创建通知渠道（Android 8.0+）
            createNotificationChannel(channelId)
            
            // 验证通知资源是否存在
            val smallIconResId = resolveNotificationIcon()
            
            // 构建通知
            return NotificationCompat.Builder(this, channelId)
                .setContentTitle(getNotificationTitle())
                .setContentText(getNotificationContent())
                .setSmallIcon(smallIconResId)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
                
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "创建通知失败，使用降级方案", e)
            
            // 降级处理：使用最简单的通知
            return createFallbackNotification()
        }
    }
    
    /**
     * 创建通知渠道
     *
     * @param channelId 通知渠道 ID
     */
    private fun createNotificationChannel(channelId: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "共情 AI 助手悬浮窗服务"
                    // 禁用声音和震动
                    setSound(null, null)
                    enableVibration(false)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
                    ?: throw RuntimeException("无法获取 NotificationManager")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "创建通知渠道失败", e)
            throw e // 重新抛出，让上层处理
        }
    }
    
    /**
     * 解析通知图标资源
     *
     * @return 图标资源 ID
     */
    private fun resolveNotificationIcon(): Int {
        return try {
            // 首选图标
            if (resources.getResourceEntryName(R.drawable.ic_launcher_foreground) != null) {
                R.drawable.ic_launcher_foreground
            } else {
                // 备选图标
                android.R.drawable.ic_dialog_info
            }
        } catch (e: Exception) {
            android.util.Log.w("FloatingWindowService", "无法解析通知图标，使用系统默认图标", e)
            // 降级到系统图标
            android.R.drawable.ic_dialog_info
        }
    }
    
    /**
     * 获取通知标题
     *
     * @return 通知标题
     */
    private fun getNotificationTitle(): String {
        return try {
            getString(R.string.app_name).ifEmpty { "共情 AI 助手" }
        } catch (e: Exception) {
            android.util.Log.w("FloatingWindowService", "无法获取应用名称", e)
            "共情 AI 助手"
        }
    }
    
    /**
     * 获取通知内容
     *
     * @return 通知内容
     */
    private fun getNotificationContent(): String {
        return try {
            "悬浮窗服务运行中"
        } catch (e: Exception) {
            android.util.Log.w("FloatingWindowService", "无法获取通知内容", e)
            "服务运行中"
        }
    }
    
    /**
     * 创建降级通知
     *
     * 当主通知创建失败时使用的最简通知
     *
     * @return 降级通知对象
     */
    private fun createFallbackNotification(): Notification {
        return try {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("共情 AI 助手")
                .setContentText("服务运行中")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "创建降级通知也失败，使用最基础通知", e)
            
            // 最后的降级方案：使用最基础的 Notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("AI 助手")
                    .setContentText("运行中")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build()
            } else {
                Notification.Builder(this)
                    .setContentTitle("AI 助手")
                    .setContentText("运行中")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build()
            }
        }
    }
    
    /**
     * 获取 AI 操作的动态超时时间
     * 
     * 根据当前默认服务商的配置动态计算超时时间
     * 协程超时 = Provider 超时 + 缓冲时间
     * 
     * @return 超时时间（毫秒）
     */
    private suspend fun getAiTimeout(): Long {
        return try {
            val provider = aiProviderRepository.getDefaultProvider().getOrNull()
            if (provider != null) {
                val timeout = provider.timeoutMs + AI_TIMEOUT_BUFFER_MS
                android.util.Log.d("FloatingWindowService", 
                    "使用 Provider 超时配置: ${provider.name} = ${provider.timeoutMs}ms + ${AI_TIMEOUT_BUFFER_MS}ms = ${timeout}ms")
                timeout
            } else {
                android.util.Log.w("FloatingWindowService", 
                    "未找到默认 Provider，使用默认超时: ${DEFAULT_AI_TIMEOUT_MS}ms")
                DEFAULT_AI_TIMEOUT_MS
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "获取 Provider 超时配置失败，使用默认值", e)
            DEFAULT_AI_TIMEOUT_MS
        }
    }
    
    // ==================== TD-00009: 新版UI方法 ====================
    
    /**
     * 显示新版悬浮视图（TD-00009）
     * 三Tab界面：分析/润色/回复
     */
    private fun showFloatingViewV2() {
        if (floatingViewV2 != null) {
            android.util.Log.d("FloatingWindowService", "FloatingViewV2已存在，跳过创建")
            return
        }

        android.util.Log.d("FloatingWindowService", "创建FloatingViewV2（新版三Tab UI）")

        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_GiveLove)
        floatingViewV2 = FloatingViewV2(themedContext, windowManager)

        // 设置回调
        floatingViewV2?.apply {
            setOnTabChangedListener { tab ->
                currentUiState = currentUiState.copy(selectedTab = tab)
                floatingWindowPreferences.saveSelectedTab(tab)
            }

            setOnContactSelectedListener { contactId ->
                currentUiState = currentUiState.copy(selectedContactId = contactId)
                floatingWindowPreferences.saveLastContactId(contactId)
            }

            setOnSubmitListener { tab, contactId, text ->
                lastInputText = text
                lastContactId = contactId
                when (tab) {
                    ActionType.ANALYZE -> handleAnalyzeV2(contactId, text)
                    ActionType.POLISH -> handlePolishV2(contactId, text)
                    ActionType.REPLY -> handleReplyV2(contactId, text)
                    else -> handleAnalyzeV2(contactId, text)
                }
            }

            setOnCopyListener { text ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("AI结果", text)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(this@FloatingWindowService, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
            }

            setOnRegenerateListener { tab, instruction ->
                handleRegenerateV2(tab, instruction)
            }

            setOnMinimizeListener {
                android.util.Log.d("FloatingWindowService", "收到最小化回调，准备调用minimizeFloatingViewV2()")
                minimizeFloatingViewV2()
            }

            setOnCloseListener {
                android.util.Log.d("FloatingWindowService", "收到关闭回调，准备调用hideFloatingViewV2()")
                hideFloatingViewV2()
            }
        }

        // 加载联系人列表
        serviceScope.launch {
            try {
                val contacts = contactRepository.getAllProfiles().first()
                floatingViewV2?.setContacts(contacts)
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "加载联系人失败", e)
            }
        }

        // 恢复状态
        val savedState = floatingWindowPreferences.restoreUiStateAsObject()
        if (savedState != null) {
            currentUiState = savedState
            floatingViewV2?.restoreState(savedState)
        }

        // 配置布局参数
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(floatingViewV2, params)
        android.util.Log.d("FloatingWindowService", "FloatingViewV2添加成功")
    }

    /**
     * 隐藏新版悬浮视图
     */
    private fun hideFloatingViewV2() {
        try {
            floatingViewV2?.let { view ->
                if (view.parent != null) {
                    windowManager.removeView(view)
                }
            }
            floatingViewV2 = null
            stopSelf()
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "隐藏FloatingViewV2失败", e)
        }
    }

    // TD-00009: 最小化悬浮指示器
    private var minimizedIndicatorV2: View? = null

    /**
     * 最小化新版悬浮视图
     * 
     * 将主界面隐藏，显示一个小的悬浮指示器，点击可恢复
     */
    private fun minimizeFloatingViewV2() {
        try {
            // 保存当前状态
            floatingViewV2?.let { view ->
                floatingWindowPreferences.saveUiState(view.getCurrentState())
            }
            
            // 隐藏主界面
            floatingViewV2?.visibility = View.GONE
            
            // 显示最小化指示器
            showMinimizedIndicatorV2()
            
            android.util.Log.d("FloatingWindowService", "FloatingViewV2已最小化")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "最小化FloatingViewV2失败", e)
        }
    }

    /**
     * 显示最小化指示器（小悬浮球）
     */
    private fun showMinimizedIndicatorV2() {
        if (minimizedIndicatorV2 != null) return

        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_GiveLove)
        
        // 创建一个简单的悬浮球
        minimizedIndicatorV2 = com.google.android.material.floatingactionbutton.FloatingActionButton(themedContext).apply {
            setImageResource(R.drawable.ic_floating_button)
            size = com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_MINI
            setOnClickListener {
                restoreFloatingViewV2()
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 16
        }

        windowManager.addView(minimizedIndicatorV2, params)
        android.util.Log.d("FloatingWindowService", "最小化指示器已显示")
    }

    /**
     * 恢复新版悬浮视图
     */
    private fun restoreFloatingViewV2() {
        try {
            // 移除最小化指示器
            minimizedIndicatorV2?.let { indicator ->
                if (indicator.parent != null) {
                    windowManager.removeView(indicator)
                }
            }
            minimizedIndicatorV2 = null

            // 显示主界面
            floatingViewV2?.visibility = View.VISIBLE
            
            android.util.Log.d("FloatingWindowService", "FloatingViewV2已恢复")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "恢复FloatingViewV2失败", e)
        }
    }

    /**
     * 处理新版分析请求
     */
    private fun handleAnalyzeV2(contactId: String, text: String) {
        floatingViewV2?.showLoading("AI正在分析...")
        
        serviceScope.launch {
            try {
                val timeoutMs = getAiTimeout()
                val result = withTimeout(timeoutMs) {
                    analyzeChatUseCase(contactId, listOf(text))
                }
                
                result.fold(
                    onSuccess = { analysisResult ->
                        val aiResult = AiResult.Analysis(analysisResult)
                        currentUiState = currentUiState.copy(lastResult = aiResult)
                        floatingViewV2?.showResult(aiResult)
                    },
                    onFailure = { error ->
                        floatingViewV2?.showError("分析失败：${error.message}")
                    }
                )
            } catch (e: TimeoutCancellationException) {
                floatingViewV2?.showError("请求超时，请重试")
            } catch (e: Exception) {
                floatingViewV2?.showError("分析失败：${e.message}")
            }
        }
    }

    /**
     * 处理润色请求（TD-00009）
     */
    private fun handlePolishV2(contactId: String, text: String) {
        floatingViewV2?.showLoading("AI正在润色...")
        
        serviceScope.launch {
            try {
                val timeoutMs = getAiTimeout()
                val result = withTimeout(timeoutMs) {
                    polishDraftUseCase(contactId, text)
                }
                
                result.fold(
                    onSuccess = { polishResult ->
                        val aiResult = AiResult.Polish(polishResult)
                        currentUiState = currentUiState.copy(lastResult = aiResult)
                        floatingViewV2?.showResult(aiResult)
                    },
                    onFailure = { error ->
                        floatingViewV2?.showError("润色失败：${error.message}")
                    }
                )
            } catch (e: TimeoutCancellationException) {
                floatingViewV2?.showError("请求超时，请重试")
            } catch (e: Exception) {
                floatingViewV2?.showError("润色失败：${e.message}")
            }
        }
    }

    /**
     * 处理回复生成请求（TD-00009）
     */
    private fun handleReplyV2(contactId: String, text: String) {
        floatingViewV2?.showLoading("AI正在生成回复...")
        
        serviceScope.launch {
            try {
                val timeoutMs = getAiTimeout()
                val result = withTimeout(timeoutMs) {
                    generateReplyUseCase(contactId, text)
                }
                
                result.fold(
                    onSuccess = { replyResult ->
                        val aiResult = AiResult.Reply(replyResult)
                        currentUiState = currentUiState.copy(lastResult = aiResult)
                        floatingViewV2?.showResult(aiResult)
                    },
                    onFailure = { error ->
                        floatingViewV2?.showError("生成回复失败：${error.message}")
                    }
                )
            } catch (e: TimeoutCancellationException) {
                floatingViewV2?.showError("请求超时，请重试")
            } catch (e: Exception) {
                floatingViewV2?.showError("生成回复失败：${e.message}")
            }
        }
    }

    /**
     * 处理重新生成请求（TD-00009）
     */
    private fun handleRegenerateV2(tab: ActionType, instruction: String?) {
        floatingViewV2?.showLoading("AI正在重新生成...")
        
        serviceScope.launch {
            try {
                val request = RefinementRequest(
                    originalInput = lastInputText,
                    originalTask = tab,
                    lastAiResponse = currentUiState.lastResult?.getDisplayContent() ?: "",
                    refinementInstruction = instruction,
                    contactId = lastContactId
                )
                
                val timeoutMs = getAiTimeout()
                val result = withTimeout(timeoutMs) {
                    refinementUseCase(request)
                }
                
                result.fold(
                    onSuccess = { aiResult ->
                        currentUiState = currentUiState.copy(lastResult = aiResult)
                        floatingViewV2?.showResult(aiResult)
                    },
                    onFailure = { error ->
                        floatingViewV2?.showError("重新生成失败：${error.message}")
                    }
                )
            } catch (e: TimeoutCancellationException) {
                floatingViewV2?.showError("请求超时，请重试")
            } catch (e: Exception) {
                floatingViewV2?.showError("重新生成失败：${e.message}")
            }
        }
    }
    
    companion object {
        /**
         * 通知 ID
         */
        private const val NOTIFICATION_ID = 1001
        
        /**
         * 通知渠道 ID
         */
        private const val CHANNEL_ID = "floating_window_service"
        
        /**
         * 完成通知渠道 ID
         * 
         * 用于 AI 响应完成后的通知
         */
        private const val CHANNEL_ID_COMPLETION = "floating_window_completion"
        
        /**
         * 完成通知 ID
         */
        private const val NOTIFICATION_ID_COMPLETION = 1002
        
        /**
         * 恢复对话框的 Action
         */
        const val ACTION_RESTORE_DIALOG = "com.empathy.ai.ACTION_RESTORE_DIALOG"
        
        /**
         * 操作超时时间（毫秒）
         * 
         * 用于数据库查询等快速操作
         */
        private const val OPERATION_TIMEOUT_MS = 5000L
        
        /**
         * AI 超时缓冲时间（毫秒）
         * 
         * 协程超时 = Provider 超时 + 缓冲时间
         * 这样可以让 HTTP 层先超时，协程超时作为兜底
         */
        private const val AI_TIMEOUT_BUFFER_MS = 5000L
        
        /**
         * 默认 AI 超时时间（毫秒）
         * 
         * 当无法获取 Provider 配置时使用
         */
        private const val DEFAULT_AI_TIMEOUT_MS = 50000L
        
        /**
         * 清理延迟时间（毫秒）
         * 
         * 根据需求 8.4，已完成的指示器在 10 分钟后自动清理
         */
        private const val CLEANUP_DELAY_MS = 10 * 60 * 1000L // 10 分钟
    }
}
  