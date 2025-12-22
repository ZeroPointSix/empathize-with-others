package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.empathy.ai.R
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AiResult
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.FloatingWindowUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 悬浮窗视图V2 - 重构版本
 *
 * 支持三Tab切换（分析/润色/回复）、结果展示、微调功能
 *
 * 主要改进：
 * - 使用TabSwitcher组件实现Tab切换
 * - 使用ResultCard组件展示AI结果
 * - 使用RefinementOverlay实现微调对话框
 * - 支持状态保存和恢复
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
class FloatingViewV2(
    context: Context,
    private val windowManager: WindowManager
) : FrameLayout(context) {

    // Tab切换组件
    private var tabSwitcher: TabSwitcher? = null

    // Tab内容区域
    private var tabContentContainer: FrameLayout? = null
    private var contactSelectorLayout: TextInputLayout? = null
    private var contactSelector: AutoCompleteTextView? = null
    private var inputLayout: TextInputLayout? = null
    private var inputText: TextInputEditText? = null
    private var btnSubmit: MaterialButton? = null
    private var loadingContainer: LinearLayout? = null
    private var loadingIndicator: CircularProgressIndicator? = null
    private var loadingText: TextView? = null
    private var errorContainer: LinearLayout? = null
    private var errorText: TextView? = null
    private var btnRetry: MaterialButton? = null

    // 结果卡片组件
    private var resultCard: ResultCard? = null

    // 微调覆盖层
    private var refinementOverlay: RefinementOverlay? = null

    // 当前状态
    private var currentState = FloatingWindowUiState()
    private var contacts: List<ContactProfile> = emptyList()
    
    // 【TD-00016】当前主题内容（用于显示主题徽章）
    private var currentTopicContent: String? = null
    
    // 主题相关UI组件
    private var topicBadge: TextView? = null
    private var btnTopic: TextView? = null

    // 回调
    private var onTabChangedListener: ((ActionType) -> Unit)? = null
    private var onContactSelectedListener: ((String) -> Unit)? = null
    private var onSubmitListener: ((ActionType, String, String) -> Unit)? = null
    private var onCopyListener: ((String) -> Unit)? = null
    private var onRegenerateListener: ((ActionType, String?) -> Unit)? = null
    private var onMinimizeListener: (() -> Unit)? = null
    private var onTopicClickListener: (() -> Unit)? = null

    init {
        initViews()
        setupListeners()
    }

    private fun initViews() {
        // BUG-00020修复：移除外层ScrollView，改用ResultCard内部的MaxHeightScrollView实现滑框式设计
        // 这样可以避免嵌套滚动冲突，同时确保按钮固定在底部
        
        // 创建主布局
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.color.floating_background)
            elevation = 8f
        }

        // 添加顶部工具栏（只有最小化按钮）
        // BUG-00013: 移除关闭按钮，只保留最小化按钮
        // TD-00016: 添加主题设置按钮
        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(12, 8, 12, 0)
            gravity = android.view.Gravity.END
        }
        
        // 【TD-00016】主题徽章 - 显示当前主题预览
        val density = context.resources.displayMetrics.density
        topicBadge = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (32 * density).toInt()
            ).apply {
                marginEnd = (8 * density).toInt()
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            maxWidth = (150 * density).toInt()
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(android.graphics.Color.parseColor("#E8F5E9")) // 浅绿色背景
            }
            visibility = View.GONE // 默认隐藏，有主题时显示
        }
        toolbar.addView(topicBadge)
        
        // 【TD-00016】主题设置按钮
        val topicButtonSize = (40 * density).toInt()
        val topicBackgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#E3F2FD")) // 浅蓝色背景
        }
        
        btnTopic = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(topicButtonSize, topicButtonSize).apply {
                marginEnd = (8 * density).toInt()
            }
            text = "📝" // 主题图标
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            background = topicBackgroundDrawable
            isClickable = true
            isFocusable = true
            setOnClickListener {
                android.util.Log.d(TAG, "主题按钮被点击")
                onTopicClickListener?.invoke()
            }
        }
        toolbar.addView(btnTopic)

        // 最小化按钮 - 使用更醒目的样式
        // BUG-00013: 只保留最小化按钮，移除关闭按钮
        // BUG-00017: 修复按钮不可见问题 - 增大尺寸、添加背景色、使用更醒目的图标
        val buttonSize = (48 * density).toInt() // 48dp转换为px
        
        // 创建圆形背景
        val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#E0E0E0")) // 浅灰色背景
        }
        
        val btnMinimize = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
            text = "▼" // 使用向下箭头表示最小化，比减号更醒目
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#424242")) // 深灰色，对比度更高
            background = backgroundDrawable
            isClickable = true
            isFocusable = true
            setOnClickListener {
                android.util.Log.d(TAG, "最小化按钮被点击, listener=${onMinimizeListener != null}")
                onMinimizeListener?.invoke()
            }
        }
        toolbar.addView(btnMinimize)

        mainLayout.addView(toolbar)

        // 添加Tab切换器
        tabSwitcher = TabSwitcher(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainLayout.addView(tabSwitcher)

        // 添加Tab内容区域
        tabContentContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 加载Tab内容布局
        val tabContent = LayoutInflater.from(context)
            .inflate(R.layout.floating_tab_content, tabContentContainer, false)
        tabContentContainer?.addView(tabContent)

        // 初始化Tab内容组件
        contactSelectorLayout = tabContent.findViewById(R.id.contact_selector_layout)
        contactSelector = tabContent.findViewById(R.id.contact_selector)
        inputLayout = tabContent.findViewById(R.id.input_layout)
        inputText = tabContent.findViewById(R.id.input_text)
        btnSubmit = tabContent.findViewById(R.id.btn_submit)
        loadingContainer = tabContent.findViewById(R.id.loading_container)
        loadingIndicator = tabContent.findViewById(R.id.loading_indicator)
        loadingText = tabContent.findViewById(R.id.loading_text)
        errorContainer = tabContent.findViewById(R.id.error_container)
        errorText = tabContent.findViewById(R.id.error_text)
        btnRetry = tabContent.findViewById(R.id.btn_retry)

        mainLayout.addView(tabContentContainer)

        // 添加结果卡片
        resultCard = ResultCard(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }
        mainLayout.addView(resultCard)

        // BUG-00020修复：直接添加主布局，不使用外层ScrollView
        // 滚动功能由ResultCard内部的MaxHeightScrollView实现
        addView(mainLayout)

        // 创建微调覆盖层（延迟初始化）
        refinementOverlay = RefinementOverlay(context, windowManager)
    }

    private fun setupListeners() {
        // Tab切换监听
        tabSwitcher?.setOnTabSelectedListener { tab ->
            currentState = currentState.copy(selectedTab = tab)
            updateInputHint(tab)
            onTabChangedListener?.invoke(tab)
        }

        // 联系人选择监听
        contactSelector?.setOnItemClickListener { _, _, position, _ ->
            val contactId = contacts.getOrNull(position)?.id?.toString()
            if (contactId != null) {
                currentState = currentState.copy(selectedContactId = contactId)
                onContactSelectedListener?.invoke(contactId)
            }
        }

        // 发送按钮监听
        btnSubmit?.setOnClickListener {
            val text = inputText?.text?.toString()?.trim() ?: ""
            val contactId = currentState.selectedContactId
            if (text.isNotBlank() && contactId != null) {
                onSubmitListener?.invoke(currentState.selectedTab, contactId, text)
            }
        }

        // 重试按钮监听
        btnRetry?.setOnClickListener {
            hideError()
            val text = inputText?.text?.toString()?.trim() ?: ""
            val contactId = currentState.selectedContactId
            if (text.isNotBlank() && contactId != null) {
                onSubmitListener?.invoke(currentState.selectedTab, contactId, text)
            }
        }

        // 结果卡片监听
        resultCard?.setOnCopyClickListener { text ->
            onCopyListener?.invoke(text)
        }

        resultCard?.setOnRegenerateClickListener {
            showRefinementDialog()
        }

        // 微调覆盖层监听
        refinementOverlay?.setOnDirectRegenerateListener {
            onRegenerateListener?.invoke(currentState.selectedTab, null)
        }

        refinementOverlay?.setOnRegenerateWithInstructionListener { instruction ->
            onRegenerateListener?.invoke(currentState.selectedTab, instruction)
        }
    }

    // ==================== 公开方法 ====================

    /**
     * 设置联系人列表
     */
    fun setContacts(contactList: List<ContactProfile>) {
        contacts = contactList
        val names = contactList.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, names)
        contactSelector?.setAdapter(adapter)

        // 如果有保存的联系人ID，自动选中
        currentState.selectedContactId?.let { savedId ->
            val index = contactList.indexOfFirst { it.id.toString() == savedId }
            if (index >= 0) {
                contactSelector?.setText(contactList[index].name, false)
            }
        }
    }

    /**
     * 恢复状态
     * 
     * BUG-00020修复：恢复结果时也需要动态调整高度
     */
    fun restoreState(state: FloatingWindowUiState) {
        currentState = state

        // 恢复Tab
        tabSwitcher?.setSelectedTab(state.selectedTab)
        updateInputHint(state.selectedTab)

        // 恢复输入内容
        inputText?.setText(state.inputText)

        // 恢复联系人选择
        state.selectedContactId?.let { contactId ->
            val contact = contacts.find { it.id.toString() == contactId }
            contact?.let { contactSelector?.setText(it.name, false) }
        }

        // 恢复结果（showResult内部会调整高度）
        state.lastResult?.let { showResult(it) }

        // 恢复加载状态
        if (state.isLoading) {
            showLoading()
        }

        // 恢复错误状态
        state.errorMessage?.let { showError(it) }
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): FloatingWindowUiState {
        return currentState.copy(
            inputText = inputText?.text?.toString() ?: ""
        )
    }

    /**
     * 显示加载状态
     */
    fun showLoading(message: String = "AI正在思考...") {
        currentState = currentState.copy(isLoading = true)
        loadingContainer?.visibility = View.VISIBLE
        loadingText?.text = message
        btnSubmit?.isEnabled = false
        errorContainer?.visibility = View.GONE
    }

    /**
     * 隐藏加载状态
     */
    fun hideLoading() {
        currentState = currentState.copy(isLoading = false)
        loadingContainer?.visibility = View.GONE
        btnSubmit?.isEnabled = true
    }

    /**
     * 显示错误
     */
    fun showError(message: String) {
        currentState = currentState.copy(errorMessage = message)
        errorContainer?.visibility = View.VISIBLE
        errorText?.text = message
        hideLoading()
    }

    /**
     * 隐藏错误
     */
    fun hideError() {
        currentState = currentState.copy(errorMessage = null)
        errorContainer?.visibility = View.GONE
    }

    /**
     * 显示结果
     * 
     * BUG-00024最终修复：
     * - 内容区域固定最大高度120dp（在XML中设置）
     * - 保留输入框，让用户可以继续输入对话
     * - 内容超出120dp时通过滚动查看
     * - 按钮固定在底部，始终可见且有充足空间
     * 
     * 修复顺序：
     * 1. 先设置最大高度
     * 2. 再显示结果内容
     * 3. 最后设置可见性
     */
    fun showResult(result: AiResult) {
        currentState = currentState.copy(lastResult = result)

        // BUG-00024修复：动态计算并设置最大高度
        // 确保结果区域不会占满屏幕，给输入框和按钮留出充足空间
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // 使用固定的120dp作为最大高度，与XML中的设置保持一致
        // 这样可以确保在所有设备上按钮都能完整显示
        val maxHeightDp = 120
        val maxAllowedHeight = (maxHeightDp * density).toInt()

        android.util.Log.d(TAG, "showResult: 准备显示结果，设置最大高度为${maxAllowedHeight}px (${maxHeightDp}dp), 屏幕高度${screenHeight}px")
        
        // 1. 先设置最大高度
        resultCard?.setMaxHeight(maxAllowedHeight)
        
        // 2. 再显示结果内容
        resultCard?.showResult(result)
        
        // 3. 最后设置可见性
        resultCard?.visibility = View.VISIBLE
        
        hideLoading()
        hideError()

        android.util.Log.d(TAG, "showResult: 结果已显示完成")
    }

    /**
     * 清空结果
     */
    fun clearResult() {
        currentState = currentState.copy(lastResult = null)
        resultCard?.clearResult()
        resultCard?.visibility = View.GONE
    }

    /**
     * 显示微调对话框
     */
    fun showRefinementDialog() {
        refinementOverlay?.show()
    }

    /**
     * 隐藏微调对话框
     */
    fun hideRefinementDialog() {
        refinementOverlay?.dismiss()
    }

    /**
     * 重置视图（保留Tab和联系人记忆）
     */
    fun resetKeepingMemory() {
        currentState = currentState.resetKeepingMemory()
        inputText?.setText("")
        clearResult()
        hideLoading()
        hideError()
    }

    /**
     * 完全重置视图
     */
    fun resetAll() {
        currentState = currentState.resetAll()
        tabSwitcher?.setSelectedTab(ActionType.ANALYZE)
        inputText?.setText("")
        contactSelector?.setText("", false)
        clearResult()
        hideLoading()
        hideError()
    }

    // ==================== 回调设置 ====================

    fun setOnTabChangedListener(listener: (ActionType) -> Unit) {
        onTabChangedListener = listener
    }

    fun setOnContactSelectedListener(listener: (String) -> Unit) {
        onContactSelectedListener = listener
    }

    fun setOnSubmitListener(listener: (ActionType, String, String) -> Unit) {
        onSubmitListener = listener
    }

    fun setOnCopyListener(listener: (String) -> Unit) {
        onCopyListener = listener
    }

    fun setOnRegenerateListener(listener: (ActionType, String?) -> Unit) {
        onRegenerateListener = listener
    }

    fun setOnMinimizeListener(listener: () -> Unit) {
        onMinimizeListener = listener
    }
    
    /**
     * 【TD-00016】设置主题点击监听器
     */
    fun setOnTopicClickListener(listener: () -> Unit) {
        onTopicClickListener = listener
    }
    
    /**
     * 【TD-00016】更新主题显示
     * 
     * @param topicContent 主题内容，为null或空时隐藏主题徽章
     */
    fun updateTopicDisplay(topicContent: String?) {
        currentTopicContent = topicContent
        if (topicContent.isNullOrBlank()) {
            topicBadge?.visibility = View.GONE
            btnTopic?.text = "📝" // 无主题时显示普通图标
        } else {
            topicBadge?.visibility = View.VISIBLE
            topicBadge?.text = "🎯 ${topicContent.take(15)}${if (topicContent.length > 15) "..." else ""}"
            btnTopic?.text = "✏️" // 有主题时显示编辑图标
        }
    }
    
    /**
     * 【TD-00016】获取当前主题内容
     */
    fun getCurrentTopicContent(): String? = currentTopicContent

    // ==================== 私有方法 ====================

    private fun updateInputHint(tab: ActionType) {
        val hint = when (tab) {
            ActionType.ANALYZE -> "粘贴对方发来的消息..."
            ActionType.POLISH -> "输入你想说的话，AI帮你润色..."
            ActionType.REPLY -> "粘贴对方发来的消息，AI帮你回复..."
            else -> "输入内容..."
        }
        inputLayout?.hint = hint

        val buttonText = when (tab) {
            ActionType.ANALYZE -> "分析"
            ActionType.POLISH -> "润色"
            ActionType.REPLY -> "生成回复"
            else -> "发送"
        }
        btnSubmit?.text = buttonText
    }

    companion object {
        private const val TAG = "FloatingViewV2"
    }
}
