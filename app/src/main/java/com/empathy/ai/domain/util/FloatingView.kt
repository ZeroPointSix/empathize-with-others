package com.empathy.ai.domain.util

import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import com.empathy.ai.R
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

/**
 * 统一悬浮视图
 * 
 * 职责：
 * - 管理悬浮窗的所有显示状态
 * - 处理触摸事件（拖动、点击）
 * - 实现边缘吸附
 * - 显示菜单和输入对话框
 * 
 * 三种模式：
 * 1. BUTTON: 悬浮按钮模式（默认）
 * 2. MENU: 菜单展开模式
 * 3. INPUT: 输入对话框模式
 */
class FloatingView(context: Context) : FrameLayout(context) {
    
    // 视图组件
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var menuLayout: LinearLayout
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var btnCheck: MaterialButton
    
    // 输入对话框组件
    private var inputDialogView: View? = null
    private var inputSectionContainer: LinearLayout? = null  // 输入区域容器（用于整体隐藏）
    private var contactSpinner: Spinner? = null
    private var inputText: EditText? = null
    private var charCount: TextView? = null
    private var loadingContainer: LinearLayout? = null
    private var loadingIndicator: ProgressBar? = null
    private var loadingText: TextView? = null
    
    // 结果展示组件
    private var resultContainer: android.widget.ScrollView? = null
    private var resultTitle: TextView? = null
    private var resultEmotion: TextView? = null
    private var resultInsights: TextView? = null
    private var resultSuggestions: TextView? = null
    private var btnCopyResult: MaterialButton? = null
    private var btnConfirm: MaterialButton? = null
    
    // 最小化指示器组件（新增）
    private var minimizedIndicator: View? = null
    private var indicatorProgress: ProgressBar? = null
    private var indicatorIcon: android.widget.ImageView? = null
    
    // 当前模式
    var currentMode = Mode.BUTTON
    
    // 当前请求信息（用于最小化时保存）
    private var currentRequestInfo: MinimizedRequestInfo? = null
    
    // 触摸事件相关
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging = false
    
    // 回调函数
    var onAnalyzeClick: (() -> Unit)? = null
    var onCheckClick: (() -> Unit)? = null
    var onPositionChanged: ((Int, Int) -> Unit)? = null
    var onMinimizeClicked: (() -> Unit)? = null  // 最小化按钮点击回调
    
    // 配置选项
    var enableEdgeSnap: Boolean = false  // 是否启用边缘吸附（默认关闭）
    
    init {
        // 确保使用Material Components主题
        val themedContext = ensureMaterialTheme(context)
        
        // 尝试加载主布局，如果失败则使用简化布局
        try {
            android.util.Log.d("FloatingView", "开始加载主布局: floating_view.xml")
            LayoutInflater.from(themedContext).inflate(R.layout.floating_view, this, true)
            android.util.Log.d("FloatingView", "主布局加载成功")
            
            // 初始化视图组件
            floatingButton = findViewById(R.id.floating_button)
            menuLayout = findViewById(R.id.menu_layout)
            btnAnalyze = findViewById(R.id.btn_analyze)
            btnCheck = findViewById(R.id.btn_check)
            
            // 验证关键组件是否成功初始化
            if (floatingButton == null || menuLayout == null || btnAnalyze == null || btnCheck == null) {
                throw RuntimeException("关键视图组件初始化失败: " +
                    "floatingButton=${floatingButton != null}, " +
                    "menuLayout=${menuLayout != null}, " +
                    "btnAnalyze=${btnAnalyze != null}, " +
                    "btnCheck=${btnCheck != null}")
            }
            
            android.util.Log.d("FloatingView", "所有视图组件初始化成功")
            
            // 设置按钮点击事件
            setupButtonMode()
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "加载主布局失败，尝试使用简化布局", e)
            
            // 清除可能部分加载的视图
            removeAllViews()
            
            // 尝试创建简化布局
            try {
                createSimplifiedLayout()
                android.util.Log.d("FloatingView", "简化布局创建成功")
            } catch (fallbackError: Exception) {
                android.util.Log.e("FloatingView", "简化布局创建也失败", fallbackError)
                // 最后的降级处理：创建最基本的按钮
                createMinimalLayout()
                android.util.Log.w("FloatingView", "使用最小化布局作为最后降级方案")
            }
        }
    }
    
    /**
     * 设置悬浮按钮模式
     * 
     * 需求 9.4：优化点击响应时间（< 300ms）
     */
    private fun setupButtonMode() {
        // 设置悬浮按钮的触摸监听器，拦截触摸事件以实现拖动
        floatingButton.setOnTouchListener { _, event ->
            // 将触摸事件传递给父视图（FloatingView）处理
            onTouchEvent(event)
        }
        
        // 保留点击监听器作为备用（如果触摸监听器返回false）
        floatingButton.setOnClickListener {
            if (!isDragging) {
                // 触觉反馈
                performHapticFeedback()
                performClick()
            }
        }
        
        btnAnalyze.setOnClickListener {
            // 触觉反馈
            performHapticFeedback()
            hideMenu()
            onAnalyzeClick?.invoke()
        }
        
        btnCheck.setOnClickListener {
            // 触觉反馈
            performHapticFeedback()
            hideMenu()
            onCheckClick?.invoke()
        }
    }
    
    /**
     * 执行触觉反馈
     * 
     * 需求 9.4：提供即时的触觉反馈，提升用户体验
     */
    private fun performHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } else {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * 处理触摸事件
     *
     * 实现拖动和点击功能
     * 
     * 性能优化：
     * - 监控拖动响应时间（需求 6.4）
     * - 目标：< 16ms (60 FPS)
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录初始位置
                    try {
                        val params = layoutParams as? WindowManager.LayoutParams
                            ?: throw RuntimeException("布局参数类型不正确")
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingView", "处理 ACTION_DOWN 事件失败", e)
                    }
                    return true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    try {
                        // 性能监控：记录拖动开始时间
                        val moveStartTime = System.nanoTime()
                        
                        // 计算移动距离
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        // 判断是否为拖动（移动距离超过阈值）
                        if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                            isDragging = true
                            
                            // 更新位置
                            val params = layoutParams as? WindowManager.LayoutParams
                                ?: throw RuntimeException("布局参数类型不正确")
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                            
                            // 安全更新视图布局
                            if (!updateViewLayoutSafely(params)) {
                                android.util.Log.w("FloatingView", "更新视图布局失败，拖动可能不流畅")
                            }
                            
                            // 性能监控：记录拖动响应时间
                            val moveEndTime = System.nanoTime()
                            val moveDuration = (moveEndTime - moveStartTime) / 1_000_000.0 // 转换为毫秒
                            
                            // 只在响应时间超过阈值时记录警告
                            if (moveDuration > 16) {
                                android.util.Log.w("FloatingView", "拖动响应时间超过 16ms: ${moveDuration}ms")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingView", "处理 ACTION_MOVE 事件失败", e)
                    }
                    return true
                }
                
                MotionEvent.ACTION_UP -> {
                    try {
                        if (isDragging) {
                            // 拖动结束，保存当前位置（不强制吸附到边缘）
                            val params = layoutParams as? WindowManager.LayoutParams
                            if (params != null) {
                                // 通知位置变化，保存位置
                                onPositionChanged?.invoke(params.x, params.y)
                                android.util.Log.d("FloatingView", "拖动结束，保存位置: (${params.x}, ${params.y})")
                            }
                        } else {
                            // 点击事件
                            performClick()
                        }
                        isDragging = false
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingView", "处理 ACTION_UP 事件失败", e)
                        isDragging = false
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "处理触摸事件失败", e)
            isDragging = false
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * 执行点击操作
     */
    override fun performClick(): Boolean {
        super.performClick()
        FloatingViewDebugLogger.logUserInteraction("performClick", currentMode.name)
        android.util.Log.d("FloatingView", "执行点击操作，当前模式: $currentMode")
        
        try {
            when (currentMode) {
                Mode.BUTTON -> {
                    FloatingViewDebugLogger.logStateTransition("BUTTON", "MENU", "showMenu")
                    showMenu()
                }
                Mode.MENU -> {
                    FloatingViewDebugLogger.logStateTransition("MENU", "BUTTON", "hideMenu")
                    hideMenu()
                }
                Mode.INPUT -> {
                    FloatingViewDebugLogger.logUserInteraction("INPUT模式点击被忽略", currentMode.name)
                    // 输入模式下不处理点击
                }
                Mode.MINIMIZED -> {
                    FloatingViewDebugLogger.logStateTransition("MINIMIZED", "INPUT", "restoreFromMinimized")
                    restoreFromMinimized() // 最小化模式下点击恢复对话框
                }
            }
        } catch (e: Exception) {
            FloatingViewDebugLogger.logException("performClick", e)
            android.util.Log.e("FloatingView", "执行点击操作失败", e)
        }
        
        return true
    }
    
    /**
     * 吸附到屏幕边缘（已废弃）
     *
     * 注意：此方法已不再使用。
     * 根据用户反馈，强制边缘吸附不够灵活，现已改为完全自由放置。
     * 用户可以将悬浮按钮放置在屏幕的任意位置。
     * 
     * @deprecated 已改为完全自由放置，不再强制吸附到边缘
     */
    @Deprecated("已改为完全自由放置，不再使用边缘吸附")
    private fun snapToEdge() {
        try {
            val params = layoutParams as? WindowManager.LayoutParams
                ?: throw RuntimeException("布局参数类型不正确")
            
            val windowManager = getWindowManagerSafely()
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            
            // 判断靠近哪一边
            val isLeftSide = params.x < screenWidth / 2
            
            // 吸附到边缘
            params.x = if (isLeftSide) 0 else screenWidth - width
            
            // 安全更新视图布局
            if (updateViewLayoutSafely(params)) {
                // 通知位置变化
                onPositionChanged?.invoke(params.x, params.y)
                android.util.Log.d("FloatingView", "吸附到边缘成功，新位置: (${params.x}, ${params.y})")
            } else {
                android.util.Log.w("FloatingView", "吸附到边缘失败，但通知位置变化")
                // 即使更新布局失败，也通知位置变化，以便下次尝试
                onPositionChanged?.invoke(params.x, params.y)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "吸附到边缘失败", e)
        }
    }
    
    /**
     * 显示菜单
     * 
     * 需求 9.4：优化点击响应时间，添加流畅的动画效果
     */
    private fun showMenu() {
        currentMode = Mode.MENU
        
        // 淡出悬浮按钮
        floatingButton.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction {
                floatingButton.visibility = View.GONE
                floatingButton.alpha = 1f
                floatingButton.scaleX = 1f
                floatingButton.scaleY = 1f
            }
            .start()
        
        // 淡入菜单
        menuLayout.visibility = View.VISIBLE
        menuLayout.alpha = 0f
        menuLayout.scaleX = 0.8f
        menuLayout.scaleY = 0.8f
        menuLayout.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }
    
    /**
     * 隐藏菜单
     * 
     * 需求 9.4：优化点击响应时间，添加流畅的动画效果
     */
    private fun hideMenu() {
        currentMode = Mode.BUTTON
        
        // 淡出菜单
        menuLayout.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction {
                menuLayout.visibility = View.GONE
                menuLayout.alpha = 1f
                menuLayout.scaleX = 1f
                menuLayout.scaleY = 1f
            }
            .start()
        
        // 淡入悬浮按钮
        floatingButton.visibility = View.VISIBLE
        floatingButton.alpha = 0f
        floatingButton.scaleX = 0.8f
        floatingButton.scaleY = 0.8f
        floatingButton.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }
    
    /**
     * 显示输入对话框
     * 
     * @param actionType 操作类型（分析或检查）
     * @param contacts 联系人列表
     * @param onConfirm 确认回调
     */
    fun showInputDialog(
        actionType: ActionType,
        contacts: List<ContactProfile>,
        onConfirm: (String, String) -> Unit
    ) {
        // 保存联系人列表和回调，用于重置时使用
        currentContacts = contacts
        currentOnConfirm = onConfirm
        
        android.util.Log.d("FloatingView", "显示输入对话框，联系人数量: ${contacts.size}")
        currentMode = Mode.INPUT
        
        // 隐藏悬浮按钮和菜单
        floatingButton.visibility = View.GONE
        menuLayout.visibility = View.GONE
        
        // 检查联系人列表是否为空
        if (contacts.isEmpty()) {
            showError("请先创建联系人画像")
            hideInputDialog()
            return
        }
        
        // 创建输入对话框
        if (inputDialogView == null) {
            // 确保使用Material Components主题
            android.util.Log.d("FloatingView", "开始创建输入对话框，准备主题包装")
            val themedContext = ensureMaterialTheme(context)
            android.util.Log.d("FloatingView", "主题包装完成，开始加载布局")
            
            try {
                inputDialogView = LayoutInflater.from(themedContext).inflate(
                    R.layout.floating_input_dialog,
                    this,
                    false
                )
                android.util.Log.d("FloatingView", "输入对话框布局加载成功")
                addView(inputDialogView)
                android.util.Log.d("FloatingView", "输入对话框视图已添加到父容器")
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "加载输入对话框布局失败", e)
                throw e
            }
            
            // 初始化对话框组件
            inputSectionContainer = inputDialogView?.findViewById(R.id.input_section_container)
            contactSpinner = inputDialogView?.findViewById(R.id.contact_spinner)
            inputText = inputDialogView?.findViewById(R.id.input_text)
            charCount = inputDialogView?.findViewById(R.id.char_count)
            loadingContainer = inputDialogView?.findViewById(R.id.loading_container)
            loadingIndicator = inputDialogView?.findViewById(R.id.loading_indicator)
            loadingText = inputDialogView?.findViewById(R.id.loading_text)
            
            // 初始化结果展示组件
            resultContainer = inputDialogView?.findViewById(R.id.result_container)
            resultTitle = inputDialogView?.findViewById(R.id.result_title)
            resultEmotion = inputDialogView?.findViewById(R.id.result_emotion)
            resultInsights = inputDialogView?.findViewById(R.id.result_insights)
            resultSuggestions = inputDialogView?.findViewById(R.id.result_suggestions)
            btnCopyResult = inputDialogView?.findViewById(R.id.btn_copy_result)
            
            android.util.Log.d("FloatingView", "开始初始化对话框组件")
            val dialogTitle = inputDialogView?.findViewById<TextView>(R.id.dialog_title)
            android.util.Log.d("FloatingView", "标题组件初始化: ${dialogTitle != null}")
            
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            android.util.Log.d("FloatingView", "取消按钮初始化: ${btnCancel != null}")
            
            val btnMinimize = inputDialogView?.findViewById<MaterialButton>(R.id.btn_minimize)
            android.util.Log.d("FloatingView", "最小化按钮初始化: ${btnMinimize != null}")
            
            btnConfirm = inputDialogView?.findViewById<MaterialButton>(R.id.btn_confirm)
            android.util.Log.d("FloatingView", "确认按钮初始化: ${btnConfirm != null}")
            
            // 设置标题
            dialogTitle?.text = when (actionType) {
                ActionType.ANALYZE -> "💡 帮我分析"
                ActionType.CHECK -> "🛡️ 帮我检查"
            }
            
            // 最小化按钮
            btnMinimize?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "最小化按钮被点击")
                    android.util.Log.d("FloatingView", "onMinimizeClicked 回调是否为空: ${onMinimizeClicked == null}")
                    
                    if (onMinimizeClicked != null) {
                        android.util.Log.d("FloatingView", "正在调用 onMinimizeClicked 回调...")
                        // 调用 Service 的最小化方法
                        onMinimizeClicked?.invoke()
                        android.util.Log.d("FloatingView", "onMinimizeClicked 回调调用完成")
                    } else {
                        // Bug修复：如果回调为空，应该调用 hideInputDialog() 返回按钮状态
                        // 而不是 minimizeDialog() 显示旋转指示器
                        android.util.Log.w("FloatingView", "⚠️ onMinimizeClicked 回调为空，调用 hideInputDialog() 返回按钮状态")
                        hideInputDialog()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理最小化按钮点击失败", e)
                    showError("最小化失败，请重试")
                }
            }
            
            // 设置联系人列表
            val contactNames = contacts.map { it.name }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, contactNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            contactSpinner?.adapter = adapter
            
            // 设置字符计数
            inputText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    charCount?.text = "$length/5000"
                    
                    // 字符数超过限制时显示警告颜色
                    if (length > 5000) {
                        charCount?.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    } else {
                        charCount?.setTextColor(context.getColor(android.R.color.darker_gray))
                    }
                }
            })
            
            // 取消按钮
            btnCancel?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "取消按钮被点击")
                    hideInputDialog()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理取消按钮点击失败", e)
                    // 即使出错也要尝试关闭对话框
                    try {
                        hideInputDialog()
                    } catch (hideException: Exception) {
                        android.util.Log.e("FloatingView", "强制关闭对话框也失败", hideException)
                    }
                }
            }
            
            // 确认按钮 - 保存回调引用以便重置时使用
            btnConfirm?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "确认按钮被点击（首次设置）")
                    android.util.Log.d("FloatingView", "联系人数量: ${contacts.size}")
                    validateAndConfirm(contacts, onConfirm)
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理确认按钮点击失败（首次设置）", e)
                    showError("操作失败，请重试")
                }
            }
        } else {
            // 更新现有对话框
            android.util.Log.d("FloatingView", "更新现有对话框，重新设置所有监听器")
            
            val dialogTitle = inputDialogView?.findViewById<TextView>(R.id.dialog_title)
            dialogTitle?.text = when (actionType) {
                ActionType.ANALYZE -> "💡 帮我分析"
                ActionType.CHECK -> "🛡️ 帮我检查"
            }
            
            // 更新联系人列表
            val contactNames = contacts.map { it.name }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, contactNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            contactSpinner?.adapter = adapter
            
            // Bug修复：重新设置取消按钮的点击监听器（之前缺失导致无法关闭对话框）
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "取消按钮被点击（更新对话框）")
                    hideInputDialog()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理取消按钮点击失败（更新对话框）", e)
                    // 即使出错也要尝试关闭对话框
                    try {
                        hideInputDialog()
                    } catch (hideException: Exception) {
                        android.util.Log.e("FloatingView", "强制关闭对话框也失败（更新对话框）", hideException)
                    }
                }
            }
            
            // 重新设置确认按钮的点击监听器
            val btnConfirm = inputDialogView?.findViewById<MaterialButton>(R.id.btn_confirm)
            btnConfirm?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "确认按钮被点击（更新对话框）")
                    android.util.Log.d("FloatingView", "联系人数量: ${contacts.size}")
                    validateAndConfirm(contacts, onConfirm)
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理确认按钮点击失败（更新对话框）", e)
                    showError("操作失败，请重试")
                }
            }
            
            // 重新设置最小化按钮的点击监听器
            val btnMinimize = inputDialogView?.findViewById<MaterialButton>(R.id.btn_minimize)
            btnMinimize?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "最小化按钮被点击（更新对话框）")
                    android.util.Log.d("FloatingView", "onMinimizeClicked 回调是否为空: ${onMinimizeClicked == null}")
                    
                    if (onMinimizeClicked != null) {
                        android.util.Log.d("FloatingView", "正在调用 onMinimizeClicked 回调...")
                        onMinimizeClicked?.invoke()
                        android.util.Log.d("FloatingView", "onMinimizeClicked 回调调用完成")
                    } else {
                        // Bug修复：如果回调为空，应该调用 hideInputDialog() 返回按钮状态
                        android.util.Log.w("FloatingView", "⚠️ onMinimizeClicked 回调为空，调用 hideInputDialog() 返回按钮状态")
                        hideInputDialog()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理最小化按钮点击失败（更新对话框）", e)
                    showError("最小化失败，请重试")
                }
            }
            
            // Bug修复：重新设置 TextWatcher（之前缺失导致字符计数不更新）
            // 先清除旧的 TextWatcher，避免重复添加
            try {
                clearTextWatchers()
            } catch (e: Exception) {
                android.util.Log.w("FloatingView", "清除旧 TextWatcher 失败（更新对话框）", e)
            }
            
            // 设置新的 TextWatcher
            inputText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    charCount?.text = "$length/5000"
                    
                    // 字符数超过限制时显示警告颜色
                    if (length > 5000) {
                        charCount?.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    } else {
                        charCount?.setTextColor(context.getColor(android.R.color.darker_gray))
                    }
                }
            })
            
            android.util.Log.d("FloatingView", "更新对话框完成，所有监听器已重新设置")
            inputDialogView?.visibility = View.VISIBLE
        }
        
        // 调整布局参数以显示对话框
        try {
            val params = layoutParams as? WindowManager.LayoutParams
                ?: throw RuntimeException("布局参数类型不正确")
            
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.CENTER
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            
            // 安全更新视图布局
            if (!updateViewLayoutSafely(params)) {
                android.util.Log.w("FloatingView", "更新输入对话框布局失败")
                showError("显示输入对话框失败")
                hideInputDialog()
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "显示输入对话框失败", e)
            showError("显示输入对话框失败")
            hideInputDialog()
            return
        }
        
        // 自动聚焦输入框并显示软键盘
        // 需求 9.5：实现输入框自动聚焦
        inputText?.postDelayed({
            inputText?.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(inputText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100) // 延迟 100ms 确保布局完成
    }
    
    /**
     * 验证输入并确认
     * 
     * @param contacts 联系人列表
     * @param onConfirm 确认回调
     */
    private fun validateAndConfirm(
        contacts: List<ContactProfile>,
        onConfirm: (String, String) -> Unit
    ) {
        try {
            android.util.Log.d("FloatingView", "开始验证输入，联系人数量: ${contacts.size}")
            
            // 验证联系人选择
            val selectedPosition = contactSpinner?.selectedItemPosition ?: -1
            android.util.Log.d("FloatingView", "选中的联系人位置: $selectedPosition")
            
            if (selectedPosition < 0 || selectedPosition >= contacts.size) {
                android.util.Log.w("FloatingView", "联系人选择无效: position=$selectedPosition, size=${contacts.size}")
                showError("请选择联系人")
                return
            }
            
            // 验证文本输入
            val text = inputText?.text?.toString() ?: ""
            android.util.Log.d("FloatingView", "输入文本长度: ${text.length}")
            
            when {
                text.isBlank() -> {
                    android.util.Log.w("FloatingView", "输入文本为空")
                    showError("请输入内容")
                    return
                }
                text.length > 5000 -> {
                    android.util.Log.w("FloatingView", "输入文本过长: ${text.length}")
                    showError("输入内容不能超过 5000 字符")
                    return
                }
            }
            
            // 验证通过，执行回调
            val contactId = contacts[selectedPosition].id
            android.util.Log.d("FloatingView", "验证通过，执行回调: contactId=$contactId, textLength=${text.length}")
            onConfirm(contactId, text)
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "验证输入时发生异常", e)
            showError("验证失败，请重试")
        }
    }
    
    /**
     * 显示错误提示
     * 
     * @param message 错误消息
     */
    private fun showError(message: String) {
        try {
            android.util.Log.w("FloatingView", "显示错误提示: $message")
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "显示错误提示失败", e)
        }
    }
    
    /**
     * 隐藏输入对话框
     *
     * 完全清理对话框状态并返回到按钮模式
     *
     * Bug修复：确保状态完全清理，防止再次打开时界面卡死
     * 修复：确保状态设置与视图清理的原子性
     */
    fun hideInputDialog() {
        // 使用同步块确保状态转换的原子性
        synchronized(this) {
            try {
                FloatingViewDebugLogger.logStateTransition(currentMode.name, "BUTTON", "hideInputDialog")
                android.util.Log.d("FloatingView", "========== hideInputDialog 开始 ==========")
                android.util.Log.d("FloatingView", "当前模式: $currentMode")
                android.util.Log.d("FloatingView", "floatingButton 可见性: ${floatingButton.visibility}")
                android.util.Log.d("FloatingView", "inputDialogView 可见性: ${inputDialogView?.visibility}")
                android.util.Log.d("FloatingView", "minimizedIndicator 可见性: ${minimizedIndicator?.visibility}")
                
                // 记录操作前的视图状态
                val beforeDialogVisible = inputDialogView?.visibility == View.VISIBLE
                val beforeButtonVisible = floatingButton.visibility == View.VISIBLE
                val beforeIndicatorVisible = minimizedIndicator?.visibility == View.VISIBLE
                FloatingViewDebugLogger.logViewState("隐藏前", beforeDialogVisible, beforeButtonVisible, beforeIndicatorVisible)
                
                // Bug修复：确保真正的原子性操作，所有状态变更在一个同步块中完成
                // 1. 原子性清理所有视图状态
                try {
                    // 隐藏所有对话框相关视图
                    inputDialogView?.visibility = View.GONE
                    minimizedIndicator?.visibility = View.GONE
                    loadingContainer?.visibility = View.GONE
                    resultContainer?.visibility = View.GONE
                    
                    // 显示悬浮按钮
                    floatingButton.visibility = View.VISIBLE
                    
                    android.util.Log.d("FloatingView", "所有视图可见性已原子性更新")
                    FloatingViewDebugLogger.logViewState("原子性视图更新", false, true, false)
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "原子性更新视图可见性失败", e)
                    FloatingViewDebugLogger.logException("原子性视图更新失败", e)
                    throw e
                }
                
                // 2. 完全清理对话框状态和资源
                try {
                    clearInputDialogState()
                    clearAllListeners() // 确保完全清理所有监听器
                    android.util.Log.d("FloatingView", "对话框状态和监听器已完全清理")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "清理对话框状态失败", e)
                    throw e
                }
                
                // 3. 恢复布局参数为按钮模式
                try {
                    restoreButtonLayoutParams()
                    android.util.Log.d("FloatingView", "布局参数已恢复")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "恢复布局参数失败", e)
                    throw e
                }
                
                // 4. 最后设置模式（确保所有操作完成后再设置状态）
                currentMode = Mode.BUTTON
                android.util.Log.d("FloatingView", "模式已设置为BUTTON，输入对话框已完全隐藏")
                android.util.Log.d("FloatingView", "========== hideInputDialog 完成 ==========")
                android.util.Log.d("FloatingView", "最终 floatingButton 可见性: ${floatingButton.visibility}")
                android.util.Log.d("FloatingView", "最终 inputDialogView 可见性: ${inputDialogView?.visibility}")
                android.util.Log.d("FloatingView", "最终 minimizedIndicator 可见性: ${minimizedIndicator?.visibility}")
                android.util.Log.d("FloatingView", "最终模式: $currentMode")
                
                // 5. 验证状态一致性
                validateStateConsistency()
                
                // 6. 记录操作后的最终状态
                val afterDialogVisible = inputDialogView?.visibility == View.VISIBLE
                val afterButtonVisible = floatingButton.visibility == View.VISIBLE
                val afterIndicatorVisible = minimizedIndicator?.visibility == View.VISIBLE
                FloatingViewDebugLogger.logViewState("隐藏后", afterDialogVisible, afterButtonVisible, afterIndicatorVisible)
                
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "隐藏输入对话框失败", e)
                FloatingViewDebugLogger.logException("hideInputDialog失败", e)
                
                // 尝试强制重置
                try {
                    atomicResetToButtonMode()
                    android.util.Log.d("FloatingView", "原子性重置完成")
                } catch (resetException: Exception) {
                    android.util.Log.e("FloatingView", "原子性重置也失败", resetException)
                    FloatingViewDebugLogger.logException("原子性重置失败", resetException)
                }
            }
        }
    }
    
    /**
     * 清理输入对话框的所有状态
     *
     * Bug修复：确保所有状态都被正确清理
     * 修复：确保完全清理所有监听器
     */
    private fun clearInputDialogState() {
        try {
            android.util.Log.d("FloatingView", "开始清理对话框状态")
            
            // 清空输入
            inputText?.text?.clear()
            
            // 重置结果区域
            resultContainer?.visibility = View.GONE
            resultInsights?.visibility = View.VISIBLE
            resultSuggestions?.visibility = View.VISIBLE
            btnCopyResult?.visibility = View.VISIBLE
            
            // 恢复输入区域容器可见性（整个输入区域）
            inputSectionContainer?.visibility = View.VISIBLE
            
            // 恢复取消按钮
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.visibility = View.VISIBLE
            
            // 重置按钮文本
            btnConfirm?.text = "确认"
            
            // ⚠️ 关键修复：完全清除所有旧的点击监听器
            clearAllListeners()
            
            // 清除加载状态
            try {
                hideLoading()
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "清除加载状态失败", e)
            }
            
            // 清除错误提示 - 注意：errorText 在布局中未定义，暂时注释掉
            // errorText?.visibility = View.GONE
            // errorText?.text = ""
            
            // 清除联系人选择器状态
            contactSpinner?.setSelection(0)
            
            android.util.Log.d("FloatingView", "对话框状态已清理")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "清理对话框状态失败", e)
            throw e
        }
    }
    
    /**
     * 完全清除所有监听器
     *
     * Bug修复：确保所有监听器都被正确清理
     */
    private fun clearAllListeners() {
        try {
            android.util.Log.d("FloatingView", "开始清除所有监听器")
            FloatingViewDebugLogger.logResourceCleanup("清除监听器开始", true, "开始清理所有监听器")
            
            var clearedCount = 0
            
            // Bug修复：确保完全清除所有监听器，防止残留
            
            // 1. 清除确认按钮监听器
            btnConfirm?.let {
                it.setOnClickListener(null)
                it.setOnLongClickListener(null)
                clearedCount++
                FloatingViewDebugLogger.logListenerCleanup("确认按钮", 2, true)
            }
            
            // 2. 清除取消按钮监听器
            try {
                val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
                btnCancel?.let {
                    it.setOnClickListener(null)
                    it.setOnLongClickListener(null)
                    clearedCount++
                    FloatingViewDebugLogger.logListenerCleanup("取消按钮", 2, true)
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "清除取消按钮监听器失败", e)
                FloatingViewDebugLogger.logException("清除取消按钮监听器", e)
            }
            
            // 3. 清除最小化按钮监听器
            try {
                val btnMinimize = inputDialogView?.findViewById<MaterialButton>(R.id.btn_minimize)
                btnMinimize?.let {
                    it.setOnClickListener(null)
                    it.setOnLongClickListener(null)
                    clearedCount++
                    FloatingViewDebugLogger.logListenerCleanup("最小化按钮", 2, true)
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "清除最小化按钮监听器失败", e)
                FloatingViewDebugLogger.logException("清除最小化按钮监听器", e)
            }
            
            // 4. 清除复制结果按钮监听器
            btnCopyResult?.let {
                it.setOnClickListener(null)
                it.setOnLongClickListener(null)
                clearedCount++
                FloatingViewDebugLogger.logListenerCleanup("复制按钮", 2, true)
            }
            
            // 5. 清除TextWatcher（使用增强的方法）
            val textWatcherCleared = clearTextWatchers()
            clearedCount += textWatcherCleared
            
            // 6. 清除Spinner监听器
            contactSpinner?.let {
                it.onItemSelectedListener = null
                it.setOnTouchListener(null)
                clearedCount++
                FloatingViewDebugLogger.logListenerCleanup("Spinner", 2, true)
            }
            
            // 7. 清除输入框的其他监听器
            inputText?.let {
                it.setOnFocusChangeListener(null)
                it.setOnEditorActionListener(null)
                it.setOnKeyListener(null)
                clearedCount++
                FloatingViewDebugLogger.logListenerCleanup("输入框其他监听器", 3, true)
            }
            
            // 8. 清除整个输入对话框的触摸监听器
            inputDialogView?.setOnTouchListener(null)
            clearedCount++
            FloatingViewDebugLogger.logListenerCleanup("输入对话框触摸监听器", 1, true)
            
            android.util.Log.d("FloatingView", "所有监听器已完全清除，总计: $clearedCount")
            FloatingViewDebugLogger.logResourceCleanup("清除监听器完成", true, "完全清除了${clearedCount}个监听器")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "清除监听器失败", e)
            FloatingViewDebugLogger.logException("清除监听器", e)
            throw e
        }
    }
    
    /**
     * 清除TextWatcher
     *
     * Bug修复：使用正确的方式清除TextWatcher
     */
    private fun clearTextWatchers(): Int {
        try {
            android.util.Log.d("FloatingView", "开始清除TextWatcher")
            var clearedCount = 0
            
            inputText?.let { editText ->
                // Bug修复：使用多种方法确保完全清除TextWatcher
                
                // 方法1：使用反射获取并清除所有TextWatcher
                try {
                    val watchersField = android.widget.TextView::class.java.getDeclaredField("mListeners")
                    watchersField.isAccessible = true
                    val watchers = watchersField.get(editText) as? java.util.ArrayList<*>
                    val watcherCount = watchers?.size ?: 0
                    watchers?.clear()
                    clearedCount += watcherCount
                    android.util.Log.d("FloatingView", "通过反射清除${watcherCount}个TextWatcher成功")
                } catch (e: Exception) {
                    android.util.Log.w("FloatingView", "通过反射清除TextWatcher失败，尝试其他方法", e)
                }
                
                // 方法2：尝试获取mSpannable字段并清除
                try {
                    val spannableField = android.widget.TextView::class.java.getDeclaredField("mText")
                    spannableField.isAccessible = true
                    val spannable = spannableField.get(editText) as? android.text.Spannable
                    if (spannable != null) {
                        val watchers = spannable.getSpans(0, spannable.length, android.text.TextWatcher::class.java)
                        watchers?.forEach { watcher ->
                            spannable.removeSpan(watcher)
                            clearedCount++
                        }
                        android.util.Log.d("FloatingView", "通过Spannable清除${watchers.size}个TextWatcher成功")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FloatingView", "通过Spannable清除TextWatcher失败", e)
                }
                
                // 方法3：降级方案 - 创建临时TextWatcher并移除（触发内部清理）
                try {
                    repeat(3) { // 尝试3次确保清理完成
                        val tempWatcher = object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {}
                        }
                        editText.addTextChangedListener(tempWatcher)
                        editText.removeTextChangedListener(tempWatcher)
                    }
                    clearedCount += 3
                    android.util.Log.d("FloatingView", "使用降级方案清除TextWatcher完成")
                } catch (e: Exception) {
                    android.util.Log.w("FloatingView", "降级方案清除TextWatcher失败", e)
                }
                
                // 方法4：最后尝试 - 直接设置新的Editable
                try {
                    val currentText = editText.text?.toString() ?: ""
                    editText.text = android.text.SpannableStringBuilder(currentText)
                    clearedCount++
                    android.util.Log.d("FloatingView", "通过重置Text清除TextWatcher成功")
                } catch (e: Exception) {
                    android.util.Log.w("FloatingView", "通过重置Text清除TextWatcher失败", e)
                }
            }
            
            android.util.Log.d("FloatingView", "TextWatcher完全清除完成，总计清除: $clearedCount")
            return clearedCount
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "清除TextWatcher失败", e)
            throw e
        }
    }
    
    /**
     * 恢复按钮模式的布局参数
     * 
     * Bug修复：确保布局参数正确恢复
     */
    private fun restoreButtonLayoutParams() {
        try {
            android.util.Log.d("FloatingView", "开始恢复按钮布局参数")
            
            val params = layoutParams as? WindowManager.LayoutParams
                ?: throw RuntimeException("布局参数类型不正确")
            
            // 恢复按钮大小
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.TOP or Gravity.START
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            
            // 安全更新视图布局
            if (!updateViewLayoutSafely(params)) {
                android.util.Log.w("FloatingView", "恢复按钮布局失败")
            } else {
                android.util.Log.d("FloatingView", "按钮布局参数已恢复")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "恢复按钮布局参数失败", e)
            throw e
        }
    }
    
    /**
     * 强制重置到按钮模式
     *
     * 用于异常情况下的状态恢复
     * Bug修复：提供应急恢复机制
     */
    fun forceResetToButtonMode() {
        try {
            android.util.Log.w("FloatingView", "强制重置到按钮模式")
            atomicResetToButtonMode()
            android.util.Log.d("FloatingView", "强制重置完成")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "强制重置失败", e)
        }
    }
    
    /**
     * 原子性重置到按钮模式
     *
     * 确保所有操作原子性执行，避免状态不一致
     * Bug修复：提供更可靠的恢复机制
     */
    private fun atomicResetToButtonMode() {
        synchronized(this) {
            try {
                android.util.Log.w("FloatingView", "开始原子性重置到按钮模式")
                
                // 定义所有需要执行的操作
                val operations = listOf<() -> Unit>(
                    { inputDialogView?.visibility = View.GONE },
                    { minimizedIndicator?.visibility = View.GONE },
                    { floatingButton.visibility = View.VISIBLE },
                    { clearInputDialogState() },
                    { restoreButtonLayoutParams() },
                    { currentMode = Mode.BUTTON }
                )
                
                // 原子性执行所有操作
                operations.forEach { operation ->
                    try {
                        operation()
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingView", "原子性操作失败", e)
                        throw e
                    }
                }
                
                // 验证状态一致性
                validateStateConsistency()
                
                android.util.Log.d("FloatingView", "原子性重置完成")
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "原子性重置失败", e)
                throw e
            }
        }
    }
    
    /**
     * 验证状态一致性
     *
     * 确保视图状态与内部状态一致
     * Bug修复：添加状态验证机制
     */
    private fun validateStateConsistency() {
        try {
            android.util.Log.d("FloatingView", "开始验证状态一致性")
            
            val issues = mutableListOf<String>()
            
            // 验证BUTTON模式
            if (currentMode == Mode.BUTTON) {
                if (floatingButton.visibility != View.VISIBLE) {
                    issues.add("BUTTON模式下悬浮按钮不可见")
                }
                if (inputDialogView?.visibility == View.VISIBLE) {
                    issues.add("BUTTON模式下输入对话框仍然可见")
                }
                if (minimizedIndicator?.visibility == View.VISIBLE) {
                    issues.add("BUTTON模式下最小化指示器仍然可见")
                }
            }
            
            // 验证INPUT模式
            if (currentMode == Mode.INPUT) {
                if (inputDialogView?.visibility != View.VISIBLE) {
                    issues.add("INPUT模式下输入对话框不可见")
                }
                if (floatingButton.visibility == View.VISIBLE) {
                    issues.add("INPUT模式下悬浮按钮仍然可见")
                }
                if (minimizedIndicator?.visibility == View.VISIBLE) {
                    issues.add("INPUT模式下最小化指示器仍然可见")
                }
            }
            
            // 验证MINIMIZED模式
            if (currentMode == Mode.MINIMIZED) {
                if (minimizedIndicator?.visibility != View.VISIBLE) {
                    issues.add("MINIMIZED模式下最小化指示器不可见")
                }
                if (floatingButton.visibility == View.VISIBLE) {
                    issues.add("MINIMIZED模式下悬浮按钮仍然可见")
                }
                if (inputDialogView?.visibility == View.VISIBLE) {
                    issues.add("MINIMIZED模式下输入对话框仍然可见")
                }
            }
            
            // 报告验证结果
            if (issues.isEmpty()) {
                android.util.Log.d("FloatingView", "状态一致性验证通过")
            } else {
                android.util.Log.e("FloatingView", "状态一致性验证失败: ${issues.joinToString(", ")}")
                // 尝试自动修复
                try {
                    when (currentMode) {
                        Mode.BUTTON -> {
                            floatingButton.visibility = View.VISIBLE
                            inputDialogView?.visibility = View.GONE
                            minimizedIndicator?.visibility = View.GONE
                        }
                        Mode.INPUT -> {
                            floatingButton.visibility = View.GONE
                            inputDialogView?.visibility = View.VISIBLE
                            minimizedIndicator?.visibility = View.GONE
                        }
                        Mode.MINIMIZED -> {
                            floatingButton.visibility = View.GONE
                            inputDialogView?.visibility = View.GONE
                            minimizedIndicator?.visibility = View.VISIBLE
                        }
                        Mode.MENU -> {
                            // MENU模式不在此处处理
                        }
                    }
                    android.util.Log.d("FloatingView", "状态一致性已自动修复")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "自动修复状态一致性失败", e)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "验证状态一致性失败", e)
        }
    }
    
    /**
     * 重置输入框状态
     *
     * 用于失败后重置状态，允许用户重新尝试
     */
    fun resetInputState() {
        try {
            android.util.Log.d("FloatingView", "开始重置输入框状态")
            
            // 确保输入对话框仍然可见
            if (currentMode != Mode.INPUT || inputDialogView?.visibility != View.VISIBLE) {
                android.util.Log.w("FloatingView", "输入对话框不可见，跳过重置")
                return
            }
            
            // 隐藏加载状态
            hideLoading()
            
            // 重置结果区域可见性
            resultContainer?.visibility = View.GONE
            
            // 恢复输入区域容器可见性（整个输入区域）
            inputSectionContainer?.visibility = View.VISIBLE
            
            // 恢复取消按钮
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.visibility = View.VISIBLE
            
            // 重置按钮文本和行为
            btnConfirm?.text = "确认"
            btnConfirm?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "重置后的确认按钮被点击（resetInputState中）")
                    android.util.Log.d("FloatingView", "当前联系人数量: ${currentContacts?.size}")
                    android.util.Log.d("FloatingView", "回调是否为空: ${currentOnConfirm == null}")
                    
                    // 获取当前的联系人列表和回调
                    val contacts = getCurrentContacts()
                    val onConfirm = getCurrentOnConfirmCallback()
                    if (contacts != null && onConfirm != null) {
                        validateAndConfirm(contacts, onConfirm)
                    } else {
                        android.util.Log.e("FloatingView", "无法获取联系人或回调，无法处理确认")
                        android.util.Log.e("FloatingView", "contacts为空: ${contacts == null}")
                        android.util.Log.e("FloatingView", "onConfirm为空: ${onConfirm == null}")
                        showError("状态异常，请重新打开对话框")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理重置后的确认按钮点击失败（resetInputState中）", e)
                    showError("操作失败，请重试")
                }
            }
            
            // 启用按钮
            btnConfirm?.isEnabled = true
            btnCopyResult?.isEnabled = true
            
            android.util.Log.d("FloatingView", "输入框状态已重置")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "重置输入框状态失败", e)
            // 尝试显示错误提示
            try {
                showError("重置失败，请重新打开对话框")
            } catch (toastException: Exception) {
                android.util.Log.e("FloatingView", "显示错误提示也失败", toastException)
            }
        }
    }
    
    // 保存当前联系人列表和回调的引用，用于重置时使用
    private var currentContacts: List<ContactProfile>? = null
    private var currentOnConfirm: ((String, String) -> Unit)? = null
    
    /**
     * 保存当前联系人列表
     */
    private fun getCurrentContacts(): List<ContactProfile>? = currentContacts
    
    /**
     * 保存当前回调
     */
    private fun getCurrentOnConfirmCallback(): ((String, String) -> Unit)? = currentOnConfirm
    
    /**
     * 显示加载状态
     * 
     * 需求 9.2：显示加载进度指示器
     * 
     * @param message 加载提示消息（可选）
     */
    fun showLoading(message: String = "正在处理...") {
        loadingContainer?.visibility = View.VISIBLE
        loadingText?.text = message
        inputText?.isEnabled = false
        contactSpinner?.isEnabled = false
        
        // 禁用按钮
        inputDialogView?.findViewById<MaterialButton>(R.id.btn_confirm)?.isEnabled = false
        inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)?.isEnabled = false
    }
    
    /**
     * 隐藏加载状态
     * 
     * 需求 9.2：隐藏加载进度指示器
     */
    fun hideLoading() {
        loadingContainer?.visibility = View.GONE
        inputText?.isEnabled = true
        contactSpinner?.isEnabled = true
        
        // 启用按钮
        inputDialogView?.findViewById<MaterialButton>(R.id.btn_confirm)?.isEnabled = true
        inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)?.isEnabled = true
    }
    
    /**
     * 显示成功提示
     * 
     * 需求 9.3：实现操作完成提示
     * 
     * @param message 成功消息
     */
    fun showSuccess(message: String) {
        android.widget.Toast.makeText(
            context,
            "✅ $message",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * 显示分析结果
     *
     * @param result 分析结果数据
     */
    fun showAnalysisResult(result: com.empathy.ai.domain.model.AnalysisResult) {
        try {
            android.util.Log.d("FloatingView", "开始显示分析结果")
            
            // 隐藏整个输入区域容器（包括标签、输入框等）
            inputSectionContainer?.visibility = View.GONE
            loadingContainer?.visibility = View.GONE
            
            // 显示结果区域
            resultContainer?.visibility = View.VISIBLE
            resultTitle?.text = "💭 AI 分析结果"
            resultEmotion?.text = "【风险等级】\n${result.riskLevel}"
            resultInsights?.text = "【军师分析】\n${result.strategyAnalysis}"
            resultInsights?.visibility = View.VISIBLE
            resultSuggestions?.text = "【建议回复】\n${result.replySuggestion}"
            resultSuggestions?.visibility = View.VISIBLE
            btnCopyResult?.visibility = View.VISIBLE
            
            // 复制按钮
            btnCopyResult?.setOnClickListener {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("AI建议", result.replySuggestion)
                    clipboard.setPrimaryClip(clip)
                    showSuccess("已复制到剪贴板")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "复制到剪贴板失败", e)
                    showError("复制失败")
                }
            }
            
            // 隐藏取消按钮，只显示关闭按钮
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.visibility = View.GONE
            
            // 修改按钮文本和行为
            btnConfirm?.text = "关闭"
            btnConfirm?.isEnabled = true
            btnConfirm?.setOnClickListener {
                android.util.Log.d("FloatingView", "分析结果页面的关闭按钮被点击")
                closeResultDialog()
            }
            
            android.util.Log.d("FloatingView", "分析结果显示完成")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "显示分析结果失败", e)
            showError("显示结果失败")
            hideInputDialog()
        }
    }
    
    /**
     * 关闭结果对话框
     * 
     * 专门用于关闭显示 AI 结果的对话框，确保正确清理状态
     */
    private fun closeResultDialog() {
        try {
            android.util.Log.d("FloatingView", "开始关闭结果对话框")
            
            // 重置结果区域
            resultContainer?.visibility = View.GONE
            resultInsights?.visibility = View.VISIBLE
            resultSuggestions?.visibility = View.VISIBLE
            btnCopyResult?.visibility = View.VISIBLE
            
            // 恢复输入区域容器可见性（为下次使用准备）
            inputSectionContainer?.visibility = View.VISIBLE
            
            // 恢复取消按钮
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.visibility = View.VISIBLE
            
            // 恢复确认按钮文本
            btnConfirm?.text = "确认"
            
            // 清除当前请求信息
            clearCurrentRequestInfo()
            
            // 关闭对话框
            hideInputDialog()
            
            android.util.Log.d("FloatingView", "结果对话框已关闭")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "关闭结果对话框失败", e)
            // 强制关闭
            try {
                hideInputDialog()
            } catch (hideException: Exception) {
                android.util.Log.e("FloatingView", "强制关闭对话框也失败", hideException)
            }
        }
    }
    
    /**
     * 显示安全检查结果
     *
     * @param result 安全检查结果数据
     */
    fun showSafetyResult(result: com.empathy.ai.domain.model.SafetyCheckResult) {
        try {
            android.util.Log.d("FloatingView", "开始显示安全检查结果")
            
            // 隐藏整个输入区域容器（包括标签、输入框等）
            inputSectionContainer?.visibility = View.GONE
            loadingContainer?.visibility = View.GONE
            
            // 显示结果区域
            resultContainer?.visibility = View.VISIBLE
            
            if (result.isSafe) {
                resultTitle?.text = "✅ 检查通过"
                resultEmotion?.text = "未发现风险内容"
                resultInsights?.visibility = View.GONE
                resultSuggestions?.visibility = View.GONE
                btnCopyResult?.visibility = View.GONE
            } else {
                resultTitle?.text = "⚠️ 检测到风险"
                resultEmotion?.text = "命中雷区: ${result.triggeredRisks.joinToString(", ")}"
                resultInsights?.text = "【建议】\n${result.suggestion ?: "无具体建议"}"
                resultInsights?.visibility = View.VISIBLE
                resultSuggestions?.visibility = View.GONE
                btnCopyResult?.visibility = View.GONE
            }
            
            // 隐藏取消按钮，只显示关闭按钮
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.visibility = View.GONE
            
            // 修改按钮文本和行为
            btnConfirm?.text = "关闭"
            btnConfirm?.isEnabled = true
            btnConfirm?.setOnClickListener {
                android.util.Log.d("FloatingView", "安全检查结果页面的关闭按钮被点击")
                closeResultDialog()
            }
            
            android.util.Log.d("FloatingView", "安全检查结果显示完成")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "显示安全检查结果失败", e)
            showError("显示结果失败")
            hideInputDialog()
        }
    }
    
    /**
     * 显示警告提示
     * 
     * 需求 9.3：实现操作完成提示
     * 
     * @param message 警告消息
     */
    fun showWarning(message: String) {
        android.widget.Toast.makeText(
            context,
            "⚠️ $message",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * 最小化对话框
     * 
     * 将输入对话框最小化为小型指示器，带有缩放动画
     * 
     * 需求 1.1, 1.4, 1.5, 6.1：支持最小化功能和流畅动画
     * 性能优化：
     * - 启用硬件加速提升动画性能（需求 6.3, 6.5）
     * - 动画时长 300ms，确保流畅（需求 6.1）
     * - 动画结束后释放输入对话框资源（需求 8.1, 8.2）
     * - 使用 Choreographer 监控帧率（需求 6.5）
     */
    fun minimizeDialog() {
        try {
            android.util.Log.d("FloatingView", "开始最小化对话框")
            
            // 性能监控：记录动画开始时间
            val startTime = System.currentTimeMillis()
            
            // 性能监控：记录初始内存使用
            val runtime = Runtime.getRuntime()
            val initialMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            android.util.Log.d("FloatingView", "最小化前内存使用: ${initialMemory}MB")
            
            // 检查当前是否处于输入模式
            if (currentMode != Mode.INPUT) {
                android.util.Log.w("FloatingView", "当前不在输入模式，无法最小化")
                return
            }
            
            // Bug修复：在最小化前保存用户输入状态
            saveCurrentInputState()
            android.util.Log.d("FloatingView", "已保存用户输入状态")
            
            // 启用硬件加速以提升动画性能（需求 6.3, 6.5）
            inputDialogView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 性能监控：使用 Choreographer 监控帧率
            var frameCount = 0
            val frameCallback = object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    frameCount++
                    if (frameCount * 16 < MINIMIZE_ANIMATION_DURATION_MS) {
                        android.view.Choreographer.getInstance().postFrameCallback(this)
                    } else {
                        val fps = (frameCount * 1000.0 / MINIMIZE_ANIMATION_DURATION_MS).toInt()
                        android.util.Log.d("FloatingView", "最小化动画帧率: ${fps} FPS")
                        if (fps < 55) {
                            android.util.Log.w("FloatingView", "最小化动画帧率低于 60 FPS: ${fps}")
                        }
                    }
                }
            }
            android.view.Choreographer.getInstance().postFrameCallback(frameCallback)
            
            // 对话框缩放动画（300ms）
            inputDialogView?.animate()
                ?.alpha(0f)
                ?.scaleX(0.3f)
                ?.scaleY(0.3f)
                ?.setDuration(MINIMIZE_ANIMATION_DURATION_MS)
                ?.withEndAction {
                    // 动画结束后隐藏输入对话框
                    inputDialogView?.visibility = View.GONE
                    
                    // 重置动画属性
                    inputDialogView?.alpha = 1f
                    inputDialogView?.scaleX = 1f
                    inputDialogView?.scaleY = 1f
                    
                    // 恢复正常渲染模式
                    inputDialogView?.setLayerType(View.LAYER_TYPE_NONE, null)
                    
                    // 释放输入对话框的 View 资源（需求 8.1, 8.2）
                    releaseInputDialogResources()
                    
                    // 显示最小化指示器（带淡入动画）
                    showMinimizedIndicator()
                    
                    // 性能监控：记录动画时长
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("FloatingView", "最小化动画完成，耗时: ${duration}ms")
                    if (duration > 300) {
                        android.util.Log.w("FloatingView", "最小化动画超时: ${duration}ms > 300ms")
                    }
                    
                    // 性能监控：记录最终内存使用
                    val finalMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                    val memoryReduced = initialMemory - finalMemory
                    android.util.Log.d("FloatingView", "最小化后内存使用: ${finalMemory}MB (释放: ${memoryReduced}MB)")
                    
                    // 验证内存占用是否符合要求（< 5MB）
                    if (finalMemory > 5) {
                        android.util.Log.w("FloatingView", "内存占用超过 5MB: ${finalMemory}MB")
                    }
                }
                ?.start()
            
            // 更新模式
            currentMode = Mode.MINIMIZED
            
            android.util.Log.d("FloatingView", "对话框已最小化")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "最小化对话框失败", e)
            showError("最小化失败: ${e.message}")
            
            // 保持对话框打开状态，用户可以继续操作
            try {
                inputDialogView?.visibility = View.VISIBLE
                inputDialogView?.alpha = 1f
                inputDialogView?.scaleX = 1f
                inputDialogView?.scaleY = 1f
                inputDialogView?.setLayerType(View.LAYER_TYPE_NONE, null)
            } catch (recoverException: Exception) {
                android.util.Log.e("FloatingView", "恢复对话框状态失败", recoverException)
            }
            
            // 记录错误但不抛出异常，避免中断服务
            android.util.Log.e("FloatingView", "最小化失败: ${e.message}", e)
        }
    }
    
    /**
     * 释放输入对话框的 View 资源
     *
     * 最小化时调用，释放不必要的 UI 资源以减少内存占用
     *
     * 需求 8.1, 8.2：优化内存使用
     * Bug修复：使用正确的方式清除TextWatcher
     */
    private fun releaseInputDialogResources() {
        try {
            android.util.Log.d("FloatingView", "开始释放输入对话框资源")
            
            // 记录释放前的内存使用
            val runtime = Runtime.getRuntime()
            val usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            android.util.Log.d("FloatingView", "释放前内存使用: ${usedMemoryBefore}MB")
            
            // 完全清除所有监听器
            clearAllListeners()
            
            // 清除 Spinner 的适配器
            contactSpinner?.adapter = null
            
            // 清除输入框的焦点
            inputText?.clearFocus()
            
            // 隐藏软键盘
            try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(inputText?.windowToken, 0)
            } catch (e: Exception) {
                android.util.Log.w("FloatingView", "隐藏软键盘失败", e)
            }
            
            // 建议垃圾回收（仅建议，不保证立即执行）
            System.gc()
            
            // 记录释放后的内存使用
            val usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            android.util.Log.d("FloatingView", "释放后内存使用: ${usedMemoryAfter}MB")
            android.util.Log.d("FloatingView", "释放内存: ${usedMemoryBefore - usedMemoryAfter}MB")
            
            android.util.Log.d("FloatingView", "输入对话框资源已释放")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "释放输入对话框资源失败", e)
        }
    }
    
    /**
     * 从最小化状态恢复对话框
     * 
     * 带有放大动画效果
     * 
     * 需求 1.4, 6.2：支持从最小化状态恢复和流畅动画
     * 性能优化：
     * - 启用硬件加速提升动画性能（需求 6.3, 6.5）
     * - 动画时长 300ms，确保流畅（需求 6.1）
     * - 恢复前重新创建输入对话框资源（需求 8.1, 8.2）
     * - 使用 Choreographer 监控帧率（需求 6.5）
     */
    fun restoreFromMinimized() {
        try {
            android.util.Log.d("FloatingView", "开始恢复对话框")
            FloatingViewDebugLogger.logStateTransition("MINIMIZED", "INPUT", "restoreFromMinimized")
            
            // 性能监控：记录动画开始时间
            val startTime = System.currentTimeMillis()
            
            // 检查当前是否处于最小化模式
            if (currentMode != Mode.MINIMIZED) {
                android.util.Log.w("FloatingView", "当前不在最小化模式，无法恢复")
                FloatingViewDebugLogger.logStateTransition("非MINIMIZED模式", "INPUT", "恢复失败")
                return
            }
            
            // Bug修复：确保输入对话框已正确初始化
            try {
                ensureInputDialogInitialized()
                android.util.Log.d("FloatingView", "输入对话框初始化检查完成")
            } catch (e: Exception) {
                android.util.Log.e("FloatingView", "输入对话框初始化失败", e)
                FloatingViewDebugLogger.logException("输入对话框初始化失败", e)
                showError("恢复失败: 对话框初始化异常")
                return
            }
            
            // 启用硬件加速以提升动画性能（需求 6.3, 6.5）
            minimizedIndicator?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 性能监控：使用 Choreographer 监控帧率
            var frameCount = 0
            val frameCallback = object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    frameCount++
                    if (frameCount * 16 < RESTORE_ANIMATION_DURATION_MS) {
                        android.view.Choreographer.getInstance().postFrameCallback(this)
                    } else {
                        val fps = (frameCount * 1000.0 / RESTORE_ANIMATION_DURATION_MS).toInt()
                        android.util.Log.d("FloatingView", "恢复动画帧率: ${fps} FPS")
                        if (fps < 55) {
                            android.util.Log.w("FloatingView", "恢复动画帧率低于 60 FPS: ${fps}")
                        }
                    }
                }
            }
            android.view.Choreographer.getInstance().postFrameCallback(frameCallback)
            
            // Bug修复：原子性状态转换，确保状态与视图同步
            synchronized(this) {
                // 指示器缩小动画（300ms）
                minimizedIndicator?.animate()
                    ?.alpha(0f)
                    ?.scaleX(0.3f)
                    ?.scaleY(0.3f)
                    ?.setDuration(RESTORE_ANIMATION_DURATION_MS)
                    ?.withEndAction {
                        // 动画结束后隐藏最小化指示器
                        hideMinimizedIndicator()
                        
                        // 调整布局参数以显示对话框
                        val params = layoutParams as? WindowManager.LayoutParams
                        if (params != null) {
                            params.width = WindowManager.LayoutParams.MATCH_PARENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            params.gravity = Gravity.CENTER
                            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            
                            updateViewLayoutSafely(params)
                        }
                        
                        // Bug修复：确保输入对话框完全重建状态
                        try {
                            rebuildInputDialogState()
                            android.util.Log.d("FloatingView", "输入对话框状态重建完成")
                        } catch (e: Exception) {
                            android.util.Log.e("FloatingView", "重建输入对话框状态失败", e)
                            FloatingViewDebugLogger.logException("重建输入对话框状态失败", e)
                        }
                        
                        // 显示输入对话框（带放大动画）
                        inputDialogView?.visibility = View.VISIBLE
                        inputDialogView?.alpha = 0f
                        inputDialogView?.scaleX = 0.3f
                        inputDialogView?.scaleY = 0.3f
                        inputDialogView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        
                        inputDialogView?.animate()
                            ?.alpha(1f)
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(RESTORE_ANIMATION_DURATION_MS)
                            ?.withEndAction {
                                // 恢复正常渲染模式
                                inputDialogView?.setLayerType(View.LAYER_TYPE_NONE, null)
                                
                                // 性能监控：记录动画时长
                                val duration = System.currentTimeMillis() - startTime
                                android.util.Log.d("FloatingView", "恢复动画完成，耗时: ${duration}ms")
                                if (duration > 300) {
                                    android.util.Log.w("FloatingView", "恢复动画超时: ${duration}ms > 300ms")
                                }
                                
                                // Bug修复：验证恢复后的状态一致性
                                validateRestoreState()
                            }
                            ?.start()
                    }
                    ?.start()
                
                // 更新模式（在动画开始时立即设置，确保状态同步）
                currentMode = Mode.INPUT
                android.util.Log.d("FloatingView", "模式已设置为INPUT，开始恢复动画")
            }
            
            android.util.Log.d("FloatingView", "对话框恢复流程已启动")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "恢复对话框失败", e)
            FloatingViewDebugLogger.logException("恢复对话框失败", e)
            showError("恢复失败: ${e.message}")
            
            // 保持指示器显示，用户可以重试
            try {
                // 确保指示器可见
                minimizedIndicator?.visibility = View.VISIBLE
                minimizedIndicator?.alpha = 1f
                minimizedIndicator?.scaleX = 1f
                minimizedIndicator?.scaleY = 1f
                minimizedIndicator?.setLayerType(View.LAYER_TYPE_NONE, null)
            } catch (recoverException: Exception) {
                android.util.Log.e("FloatingView", "恢复指示器状态失败", recoverException)
                FloatingViewDebugLogger.logException("恢复指示器状态失败", recoverException)
            }
            
            // 记录错误但不抛出异常，避免中断服务
            android.util.Log.e("FloatingView", "恢复失败: ${e.message}", e)
        }
    }
    
    /**
     * 确保输入对话框已正确初始化
     *
     * Bug修复：确保从最小化恢复时输入对话框完全可用
     */
    private fun ensureInputDialogInitialized() {
        try {
            android.util.Log.d("FloatingView", "检查输入对话框初始化状态")
            
            // 检查输入对话框是否存在
            if (inputDialogView == null) {
                android.util.Log.w("FloatingView", "输入对话框不存在，尝试重新创建")
                // 这里不能直接重新创建，因为需要联系人列表和回调
                throw RuntimeException("输入对话框未初始化，无法恢复")
            }
            
            // 检查关键组件是否存在
            val missingComponents = mutableListOf<String>()
            if (contactSpinner == null) missingComponents.add("contactSpinner")
            if (inputText == null) missingComponents.add("inputText")
            if (btnConfirm == null) missingComponents.add("btnConfirm")
            
            if (missingComponents.isNotEmpty()) {
                android.util.Log.e("FloatingView", "输入对话框缺少关键组件: ${missingComponents.joinToString(", ")}")
                throw RuntimeException("输入对话框组件缺失: ${missingComponents.joinToString(", ")}")
            }
            
            android.util.Log.d("FloatingView", "输入对话框初始化检查通过")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "输入对话框初始化检查失败", e)
            throw e
        }
    }
    
    /**
     * 重建输入对话框状态
     *
     * Bug修复：确保从最小化恢复时所有状态正确重建
     * Bug修复：恢复用户之前输入的文本内容和选中的联系人
     */
    private fun rebuildInputDialogState() {
        try {
            android.util.Log.d("FloatingView", "开始重建输入对话框状态")
            
            // 1. 确保视图可见性正确
            inputDialogView?.visibility = View.VISIBLE
            floatingButton.visibility = View.GONE
            minimizedIndicator?.visibility = View.GONE
            
            // 2. 恢复用户之前输入的文本内容和选中的联系人（关键修复）
            restoreUserInputState()
            
            // 3. 重新设置按钮监听器
            val btnCancel = inputDialogView?.findViewById<MaterialButton>(R.id.btn_cancel)
            btnCancel?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "恢复后的取消按钮被点击")
                    hideInputDialog()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理恢复后取消按钮点击失败", e)
                }
            }
            
            val btnMinimize = inputDialogView?.findViewById<MaterialButton>(R.id.btn_minimize)
            btnMinimize?.setOnClickListener {
                try {
                    android.util.Log.d("FloatingView", "恢复后的最小化按钮被点击")
                    android.util.Log.d("FloatingView", "onMinimizeClicked 回调是否为空: ${onMinimizeClicked == null}")
                    
                    if (onMinimizeClicked != null) {
                        android.util.Log.d("FloatingView", "正在调用 onMinimizeClicked 回调...")
                        onMinimizeClicked?.invoke()
                        android.util.Log.d("FloatingView", "onMinimizeClicked 回调调用完成")
                    } else {
                        // Bug修复：如果回调为空，应该调用 hideInputDialog() 返回按钮状态
                        android.util.Log.w("FloatingView", "⚠️ onMinimizeClicked 回调为空，调用 hideInputDialog() 返回按钮状态")
                        hideInputDialog()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "处理恢复后最小化按钮点击失败", e)
                }
            }
            
            // 4. 重新设置确认按钮监听器
            // Bug修复：检查是否正在显示结果，如果是，设置为关闭监听器
            val isShowingResult = resultContainer?.visibility == View.VISIBLE
            if (isShowingResult) {
                // 正在显示结果，设置为关闭监听器
                android.util.Log.d("FloatingView", "正在显示结果，设置关闭监听器")
                btnConfirm?.text = "关闭"
                btnConfirm?.setOnClickListener {
                    android.util.Log.d("FloatingView", "恢复后的关闭按钮被点击（结果页面）")
                    closeResultDialog()
                }
            } else {
                // 正常输入模式，设置为确认监听器
                btnConfirm?.text = "确认"
                btnConfirm?.setOnClickListener {
                    try {
                        android.util.Log.d("FloatingView", "恢复后的确认按钮被点击")
                        val contacts = getCurrentContacts()
                        val onConfirm = getCurrentOnConfirmCallback()
                        if (contacts != null && onConfirm != null) {
                            validateAndConfirm(contacts, onConfirm)
                        } else {
                            android.util.Log.e("FloatingView", "无法获取联系人或回调，无法处理确认")
                            showError("状态异常，请重新打开对话框")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingView", "处理恢复后确认按钮点击失败", e)
                        showError("操作失败，请重试")
                    }
                }
            }
            
            // 5. 重新设置TextWatcher
            inputText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    charCount?.text = "$length/5000"
                    
                    if (length > 5000) {
                        charCount?.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    } else {
                        charCount?.setTextColor(context.getColor(android.R.color.darker_gray))
                    }
                }
            })
            
            android.util.Log.d("FloatingView", "输入对话框状态重建完成")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "重建输入对话框状态失败", e)
            throw e
        }
    }
    
    /**
     * 恢复用户之前输入的文本内容和选中的联系人
     * 
     * Bug修复：最小化后恢复时，用户输入的内容丢失
     * 从 currentRequestInfo 中读取保存的输入内容并恢复到输入框
     */
    private fun restoreUserInputState() {
        try {
            val requestInfo = currentRequestInfo
            if (requestInfo == null) {
                android.util.Log.d("FloatingView", "无保存的请求信息，跳过恢复用户输入")
                return
            }
            
            android.util.Log.d("FloatingView", "开始恢复用户输入状态")
            android.util.Log.d("FloatingView", "保存的输入文本长度: ${requestInfo.inputText.length}")
            android.util.Log.d("FloatingView", "保存的联系人索引: ${requestInfo.selectedContactIndex}")
            
            // 恢复输入框文本
            if (requestInfo.inputText.isNotEmpty()) {
                inputText?.setText(requestInfo.inputText)
                // 将光标移动到文本末尾
                inputText?.setSelection(requestInfo.inputText.length)
                android.util.Log.d("FloatingView", "已恢复输入文本: ${requestInfo.inputText.take(50)}...")
                
                // 更新字符计数
                charCount?.text = "${requestInfo.inputText.length}/5000"
            }
            
            // 恢复选中的联系人
            if (requestInfo.selectedContactIndex >= 0) {
                val adapter = contactSpinner?.adapter
                if (adapter != null && requestInfo.selectedContactIndex < adapter.count) {
                    contactSpinner?.setSelection(requestInfo.selectedContactIndex)
                    android.util.Log.d("FloatingView", "已恢复联系人选择: 索引 ${requestInfo.selectedContactIndex}")
                } else {
                    android.util.Log.w("FloatingView", "联系人索引超出范围: ${requestInfo.selectedContactIndex}, 适配器数量: ${adapter?.count ?: 0}")
                }
            }
            
            android.util.Log.d("FloatingView", "用户输入状态恢复完成")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "恢复用户输入状态失败", e)
            // 不抛出异常，允许继续恢复其他状态
        }
    }
    
    /**
     * 验证恢复后的状态一致性
     *
     * Bug修复：确保从最小化恢复后状态完全正确
     */
    private fun validateRestoreState() {
        try {
            android.util.Log.d("FloatingView", "开始验证恢复后状态一致性")
            
            val issues = mutableListOf<String>()
            
            // 验证INPUT模式
            if (currentMode != Mode.INPUT) {
                issues.add("恢复后模式不是INPUT: $currentMode")
            }
            
            if (inputDialogView?.visibility != View.VISIBLE) {
                issues.add("恢复后输入对话框不可见")
            }
            
            if (floatingButton.visibility == View.VISIBLE) {
                issues.add("恢复后悬浮按钮仍然可见")
            }
            
            if (minimizedIndicator?.visibility == View.VISIBLE) {
                issues.add("恢复后最小化指示器仍然可见")
            }
            
            // 验证关键组件状态
            if (contactSpinner?.visibility != View.VISIBLE) {
                issues.add("恢复后联系人选择器不可见")
            }
            
            if (inputText?.visibility != View.VISIBLE) {
                issues.add("恢复后输入框不可见")
            }
            
            if (btnConfirm?.visibility != View.VISIBLE) {
                issues.add("恢复后确认按钮不可见")
            }
            
            // 报告验证结果
            if (issues.isEmpty()) {
                android.util.Log.d("FloatingView", "恢复后状态一致性验证通过")
                FloatingViewDebugLogger.logStateTransition("INPUT", "INPUT", "状态验证通过")
            } else {
                android.util.Log.e("FloatingView", "恢复后状态一致性验证失败: ${issues.joinToString(", ")}")
                FloatingViewDebugLogger.logException("恢复后状态验证失败", RuntimeException(issues.joinToString("; ")))
                
                // 尝试自动修复
                try {
                    inputDialogView?.visibility = View.VISIBLE
                    floatingButton.visibility = View.GONE
                    minimizedIndicator?.visibility = View.GONE
                    contactSpinner?.visibility = View.VISIBLE
                    inputText?.visibility = View.VISIBLE
                    btnConfirm?.visibility = View.VISIBLE
                    android.util.Log.d("FloatingView", "恢复后状态一致性已自动修复")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingView", "自动修复恢复后状态一致性失败", e)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "验证恢复后状态一致性失败", e)
        }
    }
    
    /**
     * 显示最小化指示器
     * 
     * 创建并显示一个小型圆形指示器，显示加载状态，带淡入动画
     * 
     * 需求 1.2, 1.3, 4.1, 6.1：显示指示器和流畅动画
     */
    private fun showMinimizedIndicator() {
        try {
            // 如果指示器已存在，先移除
            if (minimizedIndicator != null) {
                removeView(minimizedIndicator)
                minimizedIndicator = null
            }
            
            // 创建指示器容器
            val indicator = FrameLayout(context).apply {
                layoutParams = LayoutParams(
                    (56 * resources.displayMetrics.density).toInt(), // 56dp
                    (56 * resources.displayMetrics.density).toInt()
                )
                
                // 设置圆形背景
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(context.getColor(R.color.floating_primary))
                }
                
                elevation = 6f
                
                // 初始状态：不可见，缩小
                alpha = 0f
                scaleX = 0.3f
                scaleY = 0.3f
            }
            
            // 创建进度条
            val progress = ProgressBar(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(), // 40dp
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    gravity = Gravity.CENTER
                }
                indeterminateTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.WHITE
                )
            }
            
            // 创建图标（初始隐藏）
            val icon = android.widget.ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(), // 40dp
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    gravity = Gravity.CENTER
                }
                setColorFilter(android.graphics.Color.WHITE)
                visibility = View.GONE
            }
            
            // 添加子视图
            indicator.addView(progress)
            indicator.addView(icon)
            
            // 设置点击监听器
            indicator.setOnClickListener {
                performHapticFeedback()
                
                // 检查当前指示器状态
                val currentState = when {
                    indicatorProgress?.visibility == View.VISIBLE -> IndicatorState.LOADING
                    indicatorIcon?.drawable?.constantState == 
                        context.getDrawable(R.drawable.ic_check)?.constantState -> IndicatorState.SUCCESS
                    else -> IndicatorState.ERROR
                }
                
                // 如果是错误状态，显示详细错误信息
                if (currentState == IndicatorState.ERROR) {
                    android.util.Log.d("FloatingView", "点击错误指示器，显示详细错误信息")
                    showError("AI 请求失败，请检查网络连接或重试")
                }
                
                // 恢复对话框
                restoreFromMinimized()
            }
            
            // 设置触摸监听器以支持拖动
            indicator.setOnTouchListener { _, event ->
                onTouchEvent(event)
            }
            
            // 添加到主布局
            addView(indicator)
            
            // 保存引用
            minimizedIndicator = indicator
            indicatorProgress = progress
            indicatorIcon = icon
            
            // 调整布局参数以显示指示器
            val params = layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.TOP or Gravity.START
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                
                updateViewLayoutSafely(params)
            }
            
            // 启用硬件加速以提升动画性能
            indicator.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 淡入和放大动画（300ms）
            indicator.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction {
                    // 恢复正常渲染模式
                    indicator.setLayerType(View.LAYER_TYPE_NONE, null)
                }
                .start()
            
            android.util.Log.d("FloatingView", "最小化指示器已显示")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "显示最小化指示器失败", e)
        }
    }
    
    /**
     * 隐藏最小化指示器
     */
    private fun hideMinimizedIndicator() {
        try {
            minimizedIndicator?.let {
                removeView(it)
                minimizedIndicator = null
                indicatorProgress = null
                indicatorIcon = null
            }
            
            android.util.Log.d("FloatingView", "最小化指示器已隐藏")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "隐藏最小化指示器失败", e)
        }
    }
    
    /**
     * 更新指示器状态
     * 
     * 带有淡入淡出动画效果（200ms）
     * 
     * @param state 指示器状态（LOADING, SUCCESS, ERROR）
     * 
     * 需求 4.1, 4.2, 4.3, 6.3：状态指示和流畅动画
     * 性能优化：
     * - 使用属性动画（alpha）而非视图动画（需求 6.1）
     * - 动画时长 200ms，确保流畅（需求 6.1）
     */
    fun updateIndicatorState(state: IndicatorState) {
        try {
            when (state) {
                IndicatorState.LOADING -> {
                    // 淡出图标
                    indicatorIcon?.animate()
                        ?.alpha(0f)
                        ?.setDuration(STATE_CHANGE_ANIMATION_DURATION_MS)
                        ?.withEndAction {
                            indicatorIcon?.visibility = View.GONE
                            indicatorIcon?.alpha = 1f
                            
                            // 显示进度条（淡入）
                            indicatorProgress?.alpha = 0f
                            indicatorProgress?.visibility = View.VISIBLE
                            indicatorProgress?.animate()
                                ?.alpha(1f)
                                ?.setDuration(STATE_CHANGE_ANIMATION_DURATION_MS)
                                ?.start()
                        }
                        ?.start()
                    
                    // 更新背景颜色为蓝色（带动画）
                    animateBackgroundColor(context.getColor(R.color.floating_primary))
                }
                IndicatorState.SUCCESS -> {
                    // 淡出进度条
                    indicatorProgress?.animate()
                        ?.alpha(0f)
                        ?.setDuration(STATE_CHANGE_ANIMATION_DURATION_MS)
                        ?.withEndAction {
                            indicatorProgress?.visibility = View.GONE
                            indicatorProgress?.alpha = 1f
                            
                            // 显示成功图标（淡入）
                            indicatorIcon?.apply {
                                setImageResource(R.drawable.ic_check)
                                alpha = 0f
                                visibility = View.VISIBLE
                                animate()
                                    .alpha(1f)
                                    .setDuration(STATE_CHANGE_ANIMATION_DURATION_MS)
                                    .start()
                            }
                        }
                        ?.start()
                    
                    // 更新背景颜色为绿色（带动画）
                    animateBackgroundColor(context.getColor(android.R.color.holo_green_dark))
                }
                IndicatorState.ERROR -> {
                    // 淡出进度条
                    indicatorProgress?.animate()
                        ?.alpha(0f)
                        ?.setDuration(STATE_CHANGE_ANIMATION_DURATION_MS)
                        ?.withEndAction {
                            indicatorProgress?.visibility = View.GONE
                            indicatorProgress?.alpha = 1f
                            
                            // 显示错误图标（淡入）
                            indicatorIcon?.apply {
                                setImageResource(android.R.drawable.ic_dialog_alert)
                                alpha = 0f
                                visibility = View.VISIBLE
                                animate()
                                    .alpha(1f)
                                    .setDuration(STATE_CHANGE_ANIMATION_DURATION_MS)
                                    .start()
                            }
                        }
                        ?.start()
                    
                    // 更新背景颜色为红色（带动画）
                    animateBackgroundColor(context.getColor(android.R.color.holo_red_dark))
                }
            }
            
            android.util.Log.d("FloatingView", "指示器状态已更新: $state")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "更新指示器状态失败", e)
        }
    }
    
    /**
     * 动画化背景颜色变化
     * 
     * @param targetColor 目标颜色
     */
    private fun animateBackgroundColor(targetColor: Int) {
        try {
            val background = minimizedIndicator?.background as? android.graphics.drawable.GradientDrawable
            if (background != null) {
                // 使用 ValueAnimator 实现颜色渐变
                val currentColor = try {
                    // 尝试获取当前颜色（这是一个简化实现）
                    when {
                        targetColor == context.getColor(R.color.floating_primary) -> 
                            context.getColor(android.R.color.holo_green_dark)
                        targetColor == context.getColor(android.R.color.holo_green_dark) -> 
                            context.getColor(R.color.floating_primary)
                        else -> context.getColor(R.color.floating_primary)
                    }
                } catch (e: Exception) {
                    context.getColor(R.color.floating_primary)
                }
                
                val colorAnimator = android.animation.ValueAnimator.ofArgb(currentColor, targetColor)
                colorAnimator.duration = 200
                colorAnimator.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    background.setColor(color)
                }
                colorAnimator.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "动画化背景颜色失败，直接设置颜色", e)
            // 降级处理：直接设置颜色
            (minimizedIndicator?.background as? android.graphics.drawable.GradientDrawable)?.setColor(targetColor)
        }
    }
    
    /**
     * 获取当前请求信息
     * 
     * @return 当前请求信息，如果没有则返回 null
     */
    fun getCurrentRequestInfo(): MinimizedRequestInfo? {
        return currentRequestInfo
    }
    
    /**
     * 设置当前请求信息
     * 
     * 在发起 AI 请求前调用，保存请求信息以便最小化时使用
     * 
     * @param contactId 联系人ID
     * @param inputText 输入文本
     * @param actionType 操作类型
     */
    fun setCurrentRequestInfo(contactId: String, inputText: String, actionType: ActionType) {
        // 获取当前选中的联系人索引
        val selectedIndex = contactSpinner?.selectedItemPosition ?: 0
        
        currentRequestInfo = MinimizedRequestInfo(
            id = java.util.UUID.randomUUID().toString(),
            type = actionType,
            contactId = contactId,
            inputText = inputText,
            selectedContactIndex = selectedIndex,
            timestamp = System.currentTimeMillis()
        )
        
        android.util.Log.d("FloatingView", "已设置当前请求信息: ${currentRequestInfo?.id}, 联系人索引: $selectedIndex, 输入长度: ${inputText.length}")
    }
    
    /**
     * 保存当前输入状态到请求信息
     * 
     * 在最小化前调用，确保用户输入的内容被保存
     * Bug修复：最小化后恢复时输入内容丢失
     */
    fun saveCurrentInputState() {
        try {
            val currentText = inputText?.text?.toString() ?: ""
            val selectedIndex = contactSpinner?.selectedItemPosition ?: 0
            val contacts = getCurrentContacts()
            val contactId = if (contacts != null && selectedIndex >= 0 && selectedIndex < contacts.size) {
                contacts[selectedIndex].id
            } else {
                ""
            }
            
            // 获取当前操作类型（从标题推断）
            val dialogTitle = inputDialogView?.findViewById<TextView>(R.id.dialog_title)
            val actionType = if (dialogTitle?.text?.contains("分析") == true) {
                ActionType.ANALYZE
            } else {
                ActionType.CHECK
            }
            
            // 更新或创建请求信息
            if (currentRequestInfo != null) {
                // 更新现有请求信息的输入内容
                currentRequestInfo = currentRequestInfo?.copy(
                    inputText = currentText,
                    selectedContactIndex = selectedIndex,
                    contactId = contactId
                )
            } else {
                // 创建新的请求信息
                currentRequestInfo = MinimizedRequestInfo(
                    id = java.util.UUID.randomUUID().toString(),
                    type = actionType,
                    contactId = contactId,
                    inputText = currentText,
                    selectedContactIndex = selectedIndex,
                    timestamp = System.currentTimeMillis()
                )
            }
            
            android.util.Log.d("FloatingView", "已保存当前输入状态: 文本长度=${currentText.length}, 联系人索引=$selectedIndex")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "保存当前输入状态失败", e)
        }
    }
    
    /**
     * 清除当前请求信息
     */
    fun clearCurrentRequestInfo() {
        currentRequestInfo = null
        android.util.Log.d("FloatingView", "已清除当前请求信息")
    }
    
    /**
     * 获取当前模式
     *
     * @return 当前显示模式
     */
    fun getCurrentModeValue(): Mode {
        return currentMode
    }
    
    /**
     * 检查是否处于最小化状态
     * 
     * @return 如果处于最小化状态返回 true，否则返回 false
     */
    fun isMinimized(): Boolean {
        return currentMode == Mode.MINIMIZED
    }
    
    /**
     * 获取悬浮按钮位置
     * 
     * @return 位置坐标 (x, y)
     */
    fun getButtonPosition(): Pair<Int, Int> {
        val params = layoutParams as? WindowManager.LayoutParams
        return if (params != null) {
            Pair(params.x, params.y)
        } else {
            Pair(0, 100)
        }
    }
    
    /**
     * 设置悬浮按钮位置
     * 
     * @param x X坐标
     * @param y Y坐标
     */
    fun setButtonPosition(x: Int, y: Int) {
        try {
            val params = layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                params.x = x
                params.y = y
                updateViewLayoutSafely(params)
                
                android.util.Log.d("FloatingView", "悬浮按钮位置已设置: ($x, $y)")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "设置悬浮按钮位置失败", e)
        }
    }
    
    /**
     * 启用硬件加速
     * 
     * 提升渲染性能，确保拖动时 UI 流畅（60 FPS）
     * 
     * 需求 6.3：拖动时保持 UI 响应流畅
     */
    fun enableHardwareAcceleration() {
        // 启用硬件加速
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // 为悬浮按钮启用硬件加速
        floatingButton.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // 为菜单布局启用硬件加速
        menuLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * 创建 WindowManager.LayoutParams
     *
     * @return 布局参数
     */
    fun createLayoutParams(): WindowManager.LayoutParams {
        return try {
            createLayoutParamsSafely()
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "创建布局参数失败，使用默认参数", e)
            createDefaultLayoutParams()
        }
    }
    
    /**
     * 安全地创建 WindowManager.LayoutParams
     *
     * 包含版本兼容性检查
     *
     * @return 布局参数
     */
    private fun createLayoutParamsSafely(): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
    }
    
    /**
     * 创建默认的 WindowManager.LayoutParams
     *
     * 当主要创建方法失败时使用
     *
     * @return 默认布局参数
     */
    private fun createDefaultLayoutParams(): WindowManager.LayoutParams {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                @Suppress("DEPRECATION")
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
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "创建默认布局参数也失败，使用最基础参数", e)
            // 最基础的参数
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
        }
    }
    
    /**
     * 安全地更新视图布局
     *
     * @param params 新的布局参数
     * @return true 如果更新成功，false 如果更新失败
     */
    private fun updateViewLayoutSafely(params: WindowManager.LayoutParams): Boolean {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: throw RuntimeException("无法获取 WindowManager")
            
            windowManager.updateViewLayout(this, params)
            android.util.Log.d("FloatingView", "视图布局更新成功")
            true
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "更新视图布局失败", e)
            false
        }
    }
    
    /**
     * 安全地获取 WindowManager
     *
     * @return WindowManager 实例，如果获取失败则抛出异常
     */
    private fun getWindowManagerSafely(): WindowManager {
        return try {
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: throw RuntimeException("无法获取 WindowManager")
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "获取 WindowManager 失败", e)
            throw e
        }
    }
    
    /**
     * 创建简化布局
     *
     * 当主布局加载失败时使用
     */
    private fun createSimplifiedLayout() {
        android.util.Log.d("FloatingView", "开始创建简化布局")
        
        try {
            // 尝试加载简化版布局文件
            android.util.Log.d("FloatingView", "尝试加载简化版布局文件: simple_floating_view.xml")
            // 确保使用Material Components主题
            val themedContext = ensureMaterialTheme(context)
            LayoutInflater.from(themedContext).inflate(R.layout.simple_floating_view, this, true)
            android.util.Log.d("FloatingView", "简化版布局文件加载成功")
            
            // 初始化视图组件
            floatingButton = findViewById(R.id.floating_button)
            menuLayout = findViewById(R.id.menu_layout)
            btnAnalyze = findViewById(R.id.btn_analyze)
            btnCheck = findViewById(R.id.btn_check)
            
            // 验证关键组件是否成功初始化
            if (floatingButton == null || menuLayout == null || btnAnalyze == null || btnCheck == null) {
                throw RuntimeException("简化布局关键视图组件初始化失败: " +
                    "floatingButton=${floatingButton != null}, " +
                    "menuLayout=${menuLayout != null}, " +
                    "btnAnalyze=${btnAnalyze != null}, " +
                    "btnCheck=${btnCheck != null}")
            }
            
            // 设置按钮点击事件
            setupButtonMode()
            
            android.util.Log.d("FloatingView", "简化布局创建完成")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "加载简化版布局文件失败，通过代码创建布局", e)
            
            // 清除可能部分加载的视图
            removeAllViews()
            
            // 通过代码创建简化布局
            createSimplifiedLayoutByCode()
        }
    }
    
    /**
     * 通过代码创建简化布局
     *
     * 当简化版布局文件也失败时使用
     */
    private fun createSimplifiedLayoutByCode() {
        android.util.Log.d("FloatingView", "通过代码创建简化布局")
        
        // 确保使用Material Components主题
        val themedContext = ensureMaterialTheme(context)
        
        // 创建基本的悬浮按钮
        val tempFloatingButton = com.google.android.material.floatingactionbutton.FloatingActionButton(themedContext).apply {
            id = R.id.floating_button
            contentDescription = context.getString(R.string.floating_button_desc)
            setImageResource(R.drawable.ic_floating_button)
            setColorFilter(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.floating_primary)
            )
            size = com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL
            elevation = 6f
            // 不设置可能有问题的rippleColor属性
        }
        
        // 创建菜单布局
        val tempMenuLayout = LinearLayout(themedContext).apply {
            id = R.id.menu_layout
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48) // 12dp * 4 = 48px
            setBackgroundColor(context.getColor(R.color.floating_background))
            elevation = 8f
            visibility = View.GONE
        }
        
        // 创建分析按钮
        val tempBtnAnalyze = com.google.android.material.button.MaterialButton(themedContext).apply {
            id = R.id.btn_analyze
            text = "💡 帮我分析"
            textSize = 15f
            setTextColor(context.getColor(R.color.text_primary))
            setPadding(0, 48, 0, 48) // 12dp * 4 = 48px
            setIconResource(R.drawable.ic_analyze)
            iconTint = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.analyze_color)
            )
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
            cornerRadius = 32 // 8dp * 4 = 32px
            layoutParams = LinearLayout.LayoutParams(
                640, // 160dp * 4 = 640px
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32) // 8dp * 4 = 32px
            }
        }
        
        // 创建检查按钮
        val tempBtnCheck = com.google.android.material.button.MaterialButton(themedContext).apply {
            id = R.id.btn_check
            text = "🛡️ 帮我检查"
            textSize = 15f
            setTextColor(context.getColor(R.color.text_primary))
            setPadding(0, 48, 0, 48) // 12dp * 4 = 48px
            setIconResource(R.drawable.ic_check)
            iconTint = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.check_color)
            )
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
            cornerRadius = 32 // 8dp * 4 = 32px
            layoutParams = LinearLayout.LayoutParams(
                640, // 160dp * 4 = 640px
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 添加按钮到菜单布局
        tempMenuLayout.addView(tempBtnAnalyze)
        tempMenuLayout.addView(tempBtnCheck)
        
        // 添加视图到主布局
        addView(tempFloatingButton)
        addView(tempMenuLayout)
        
        // 赋值给类属性
        floatingButton = tempFloatingButton
        menuLayout = tempMenuLayout
        btnAnalyze = tempBtnAnalyze
        btnCheck = tempBtnCheck
        
        // 设置按钮点击事件
        setupButtonMode()
        
        android.util.Log.d("FloatingView", "通过代码创建简化布局完成")
    }
    
    /**
     * 创建最小化布局
     *
     * 当简化布局也失败时使用的最后降级方案
     */
    private fun createMinimalLayout() {
        android.util.Log.w("FloatingView", "创建最小化布局")
        
        // 确保使用Material Components主题
        val themedContext = ensureMaterialTheme(context)
        
        // 创建最基本的按钮
        val tempMinimalButton = android.widget.Button(themedContext).apply {
            id = R.id.floating_button
            text = "AI"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(context.getColor(R.color.floating_primary))
            elevation = 6f
            setPadding(32, 32, 32, 32) // 8dp * 4 = 32px
            
            // 设置点击监听器
            setOnClickListener {
                android.util.Log.d("FloatingView", "最小化布局按钮被点击")
                // 显示简单提示
                android.widget.Toast.makeText(context, "悬浮窗功能已启用", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 创建空的菜单布局（避免空指针异常）
        val tempMenuLayout = LinearLayout(themedContext).apply {
            id = R.id.menu_layout
            visibility = View.GONE
        }
        
        // 创建空的按钮（避免空指针异常）
        val tempBtnAnalyze = com.google.android.material.button.MaterialButton(themedContext).apply {
            id = R.id.btn_analyze
            visibility = View.GONE
        }
        
        val tempBtnCheck = com.google.android.material.button.MaterialButton(themedContext).apply {
            id = R.id.btn_check
            visibility = View.GONE
        }
        
        // 添加视图到主布局
        addView(tempMinimalButton)
        
        // 赋值给类属性
        // 注意：这里不能直接转换类型，需要修改属性类型或创建一个包装器
        // 为了保持类型安全，我们创建一个简单的FloatingActionButton包装器
        val wrapperButton = com.google.android.material.floatingactionbutton.FloatingActionButton(themedContext).apply {
            // 设置与Button相同的属性
            setImageResource(android.R.drawable.ic_menu_help) // 使用系统图标代替文本
            setColorFilter(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.floating_primary)
            )
            size = com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL
            elevation = 6f
            contentDescription = "AI助手"
            
            // 设置点击监听器
            setOnClickListener {
                android.util.Log.d("FloatingView", "最小化布局按钮被点击")
                // 显示简单提示
                android.widget.Toast.makeText(context, "悬浮窗功能已启用", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 替换Button为FloatingActionButton
        removeView(tempMinimalButton)
        addView(wrapperButton)
        
        floatingButton = wrapperButton
        menuLayout = tempMenuLayout
        btnAnalyze = tempBtnAnalyze
        btnCheck = tempBtnCheck
        
        android.util.Log.w("FloatingView", "最小化布局创建完成")
    }
    
    /**
     * 确保使用Material Components主题
     *
     * @param context 原始上下文
     * @return 包装了Material主题的上下文
     */
    private fun ensureMaterialTheme(context: Context): Context {
        return try {
            android.util.Log.d("FloatingView", "开始检查主题，上下文类型: ${context.javaClass.simpleName}")
            
            // 检查当前上下文是否已经是Material主题
            val currentTheme = try {
                val typedArray = context.theme.obtainStyledAttributes(
                    intArrayOf(android.R.attr.theme)
                )
                val themeId = typedArray.getResourceId(0, 0)
                typedArray.recycle()
                themeId
            } catch (e: Exception) {
                android.util.Log.w("FloatingView", "获取当前主题ID失败，使用默认值0", e)
                0
            }
            
            android.util.Log.d("FloatingView", "当前主题资源ID: 0x${Integer.toHexString(currentTheme)}")
            
            // 检查是否为ContextThemeWrapper
            if (context is ContextThemeWrapper) {
                android.util.Log.d("FloatingView", "当前上下文已经是ContextThemeWrapper，主题: ${context.theme}")
            }
            
            // 如果当前主题不是Material Components主题，则包装它
            if (!isMaterialComponentsTheme(currentTheme)) {
                android.util.Log.d("FloatingView", "当前主题不是Material Components主题，使用ContextThemeWrapper包装")
                android.util.Log.d("FloatingView", "包装主题: R.style.Theme_GiveLove")
                
                // 尝试使用自定义主题，如果失败则使用系统Material主题
                val wrappedContext = try {
                    ContextThemeWrapper(context, R.style.Theme_GiveLove)
                } catch (e: Exception) {
                    android.util.Log.w("FloatingView", "使用自定义主题失败，降级到系统主题", e)
                    // 降级方案：使用系统Material主题
                    ContextThemeWrapper(context, android.R.style.Theme_Material_Light_NoActionBar)
                }
                
                android.util.Log.d("FloatingView", "包装后主题: ${wrappedContext.theme}")
                wrappedContext
            } else {
                android.util.Log.d("FloatingView", "当前主题已经是Material Components主题")
                context
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingView", "检查主题失败，使用ContextThemeWrapper作为安全措施", e)
            android.util.Log.e("FloatingView", "错误详情: ${e.javaClass.simpleName}: ${e.message}")
            
            // 发生错误时，使用降级策略确保Material主题
            try {
                android.util.Log.d("FloatingView", "使用安全措施包装主题: R.style.Theme_GiveLove")
                ContextThemeWrapper(context, R.style.Theme_GiveLove)
            } catch (e1: Exception) {
                android.util.Log.w("FloatingView", "自定义主题失败，使用系统Material主题")
                ContextThemeWrapper(context, android.R.style.Theme_Material_Light_NoActionBar)
            }
        }
    }
    
    /**
     * 检查主题是否为Material Components主题
     *
     * @param themeResId 主题资源ID
     * @return true如果是Material Components主题
     */
    private fun isMaterialComponentsTheme(themeResId: Int): Boolean {
        return try {
            // 添加调试日志以验证资源ID
            android.util.Log.d("FloatingView", "检查主题资源ID: 0x${Integer.toHexString(themeResId)}")
            
            // 检查是否为无效资源ID
            if (themeResId == 0 || themeResId == 0x00000000) {
                android.util.Log.w("FloatingView", "检测到无效的主题资源ID: 0x${Integer.toHexString(themeResId)}")
                return false
            }
            
            // 尝试获取资源名称，如果失败则不是有效的Material Components主题
            val themeName = try {
                context.resources.getResourceEntryName(themeResId)
            } catch (e: android.content.res.Resources.NotFoundException) {
                android.util.Log.w("FloatingView", "无法找到资源ID对应的主题名称: 0x${Integer.toHexString(themeResId)}")
                return false
            } catch (e: Exception) {
                android.util.Log.w("FloatingView", "获取主题名称时发生异常: 0x${Integer.toHexString(themeResId)}", e)
                return false
            }
            
            android.util.Log.d("FloatingView", "主题名称: $themeName")
            
            val isMaterialTheme = themeName.contains("MaterialComponents", ignoreCase = true) ||
                    themeName.contains("Theme.MaterialComponents", ignoreCase = true) ||
                    themeName.contains("Theme.GiveLove", ignoreCase = true) // 添加对自定义主题的支持
            
            android.util.Log.d("FloatingView", "是否为Material Components主题: $isMaterialTheme")
            isMaterialTheme
        } catch (e: Exception) {
            android.util.Log.w("FloatingView", "无法确定主题类型，假设不是Material Components主题", e)
            android.util.Log.w("FloatingView", "错误详情: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }
    
    /**
     * 显示模式枚举
     */
    enum class Mode {
        BUTTON,     // 悬浮按钮模式
        MENU,       // 菜单展开模式
        INPUT,      // 输入对话框模式
        MINIMIZED   // 最小化指示器模式（新增）
    }
}

/**
 * 指示器状态枚举
 */
enum class IndicatorState {
    LOADING,   // 加载中（显示进度条）
    SUCCESS,   // 成功（显示绿色对勾）
    ERROR      // 错误（显示红色错误图标）
}

/**
 * 动画时长常量（毫秒）
 * 
 * 需求 6.1：确保动画时长合理
 * - 最小化/恢复动画：300ms
 * - 状态切换动画：200ms
 */
private const val MINIMIZE_ANIMATION_DURATION_MS = 300L
private const val RESTORE_ANIMATION_DURATION_MS = 300L
private const val STATE_CHANGE_ANIMATION_DURATION_MS = 200L
