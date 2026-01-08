package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.AiResult
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.KnowledgeQueryResponse
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.Recommendation
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.presentation.ui.component.MaxHeightScrollView
import com.google.android.material.card.MaterialCardView

/**
 * AI结果展示卡片组件
 *
 * 支持展示四种类型的AI结果：
 * - 分析结果（AnalysisResult）
 * - 润色结果（PolishResult）
 * - 回复结果（ReplyResult）
 * - 知识查询结果（KnowledgeQueryResponse）
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 */
class ResultCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var resultCard: MaterialCardView? = null
    private var resultScroll: MaxHeightScrollView? = null
    private var resultTitle: TextView? = null
    private var riskBadge: TextView? = null
    private var resultContent: TextView? = null
    private var riskWarningContainer: LinearLayout? = null
    private var riskWarningText: TextView? = null
    private var strategyNote: TextView? = null
    private var btnCopy: com.google.android.material.button.MaterialButton? = null
    private var btnRegenerate: com.google.android.material.button.MaterialButton? = null
    
    // TD-00031: 知识查询相关UI组件
    private var sourceLabel: TextView? = null
    private var recommendationsContainer: LinearLayout? = null

    private var currentResult: AiResult? = null
    private var onCopyClickListener: ((String) -> Unit)? = null
    private var onRegenerateClickListener: (() -> Unit)? = null
    
    // TD-00031: 推荐项点击回调
    private var onRecommendationClickListener: ((Recommendation) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.floating_result_card, this, true)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        resultCard = findViewById(R.id.result_card)
        resultScroll = findViewById(R.id.result_scroll)
        resultTitle = findViewById(R.id.result_title)
        riskBadge = findViewById(R.id.risk_badge)
        resultContent = findViewById(R.id.result_content)
        riskWarningContainer = findViewById(R.id.risk_warning_container)
        riskWarningText = findViewById(R.id.risk_warning_text)
        strategyNote = findViewById(R.id.strategy_note)
        btnCopy = findViewById(R.id.btn_copy)
        btnRegenerate = findViewById(R.id.btn_regenerate)
        
        // BUG-00021修复：添加初始化验证日志
        android.util.Log.d(TAG, "initViews完成: " +
            "btnCopy=${btnCopy != null}, " +
            "btnRegenerate=${btnRegenerate != null}, " +
            "resultCard=${resultCard != null}")
        
        // BUG-00021修复：如果按钮为null，尝试延迟查找
        if (btnCopy == null || btnRegenerate == null) {
            android.util.Log.w(TAG, "按钮初始化失败，尝试延迟查找")
            post {
                if (btnCopy == null) {
                    btnCopy = findViewById(R.id.btn_copy)
                }
                if (btnRegenerate == null) {
                    btnRegenerate = findViewById(R.id.btn_regenerate)
                }
                android.util.Log.d(TAG, "延迟查找结果: btnCopy=${btnCopy != null}, btnRegenerate=${btnRegenerate != null}")
                
                // 延迟查找后重新设置点击监听器
                if (btnCopy != null || btnRegenerate != null) {
                    setupClickListeners()
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnCopy?.setOnClickListener {
            currentResult?.let { result ->
                onCopyClickListener?.invoke(result.getCopyableText())
            }
        }

        btnRegenerate?.setOnClickListener {
            onRegenerateClickListener?.invoke()
        }
    }

    /**
     * 显示分析结果
     *
     * @param result 分析结果
     */
    fun showAnalysisResult(result: AnalysisResult) {
        currentResult = AiResult.Analysis(result)
        resultTitle?.text = "🔍 分析结果"
        resultContent?.text = buildString {
            appendLine("【军师分析】")
            appendLine(result.strategyAnalysis)
            appendLine()
            appendLine("【话术建议】")
            append(result.replySuggestion)
        }

        // 显示风险等级标签
        showRiskBadge(result.riskLevel)

        // 隐藏其他元素
        riskWarningContainer?.visibility = View.GONE
        strategyNote?.visibility = View.GONE

        // BUG-00017修复：显式设置按钮可见，确保复制和重新生成按钮始终显示
        ensureButtonsVisible()

        visibility = View.VISIBLE
    }

    /**
     * 显示润色结果
     *
     * @param result 润色结果
     */
    fun showPolishResult(result: PolishResult) {
        currentResult = AiResult.Polish(result)
        resultTitle?.text = "✍️ 润色结果"
        resultContent?.text = result.polishedText

        // 隐藏风险等级标签
        riskBadge?.visibility = View.GONE

        // 显示风险提示（如果有）
        if (result.hasRisk && !result.riskWarning.isNullOrBlank()) {
            riskWarningContainer?.visibility = View.VISIBLE
            riskWarningText?.text = result.riskWarning
        } else {
            riskWarningContainer?.visibility = View.GONE
        }

        // 隐藏策略说明
        strategyNote?.visibility = View.GONE

        // BUG-00017修复：显式设置按钮可见，确保复制和重新生成按钮始终显示
        ensureButtonsVisible()

        visibility = View.VISIBLE
    }

    /**
     * 显示回复结果
     *
     * @param result 回复结果
     */
    fun showReplyResult(result: ReplyResult) {
        currentResult = AiResult.Reply(result)
        resultTitle?.text = "💬 回复建议"
        resultContent?.text = result.suggestedReply

        // 隐藏风险等级标签
        riskBadge?.visibility = View.GONE

        // 隐藏风险提示
        riskWarningContainer?.visibility = View.GONE

        // 显示策略说明（如果有）
        if (!result.strategyNote.isNullOrBlank()) {
            strategyNote?.visibility = View.VISIBLE
            strategyNote?.text = "💡 ${result.strategyNote}"
        } else {
            strategyNote?.visibility = View.GONE
        }

        // BUG-00017修复：显式设置按钮可见，确保复制和重新生成按钮始终显示
        ensureButtonsVisible()

        visibility = View.VISIBLE
    }

    /**
     * 显示AI结果（自动判断类型）
     *
     * @param result AI结果
     */
    fun showResult(result: AiResult) {
        when (result) {
            is AiResult.Analysis -> showAnalysisResult(result.result)
            is AiResult.Polish -> showPolishResult(result.result)
            is AiResult.Reply -> showReplyResult(result.result)
            is AiResult.Knowledge -> showKnowledgeResult(result.result)
        }
    }
    
    /**
     * 显示知识查询结果
     *
     * TD-00031: 快速问答功能的结果展示
     *
     * @param result 知识查询结果
     * @see PRD-00031 悬浮窗快速知识回答功能需求
     */
    fun showKnowledgeResult(result: KnowledgeQueryResponse) {
        currentResult = AiResult.Knowledge(result)
        resultTitle?.text = "💡 知识解答"
        resultContent?.text = result.getDisplayContent()

        // 隐藏风险等级标签
        riskBadge?.visibility = View.GONE

        // 隐藏风险提示
        riskWarningContainer?.visibility = View.GONE

        // 显示来源标签
        if (sourceLabel != null) {
            sourceLabel?.visibility = View.VISIBLE
            sourceLabel?.text = "${result.getSourceIcon()} ${result.getSourceLabel()}"
        } else {
            // 如果没有专门的来源标签，使用策略说明区域显示
            strategyNote?.visibility = View.VISIBLE
            strategyNote?.text = "${result.getSourceIcon()} ${result.getSourceLabel()}"
        }

        // 显示推荐列表
        showRecommendations(result.recommendations)

        // BUG-00017修复：显式设置按钮可见
        ensureButtonsVisible()

        visibility = View.VISIBLE
    }
    
    /**
     * 显示推荐列表
     *
     * TD-00031: 展示相关推荐话题
     *
     * @param recommendations 推荐列表
     */
    private fun showRecommendations(recommendations: List<Recommendation>) {
        if (recommendations.isEmpty()) {
            recommendationsContainer?.visibility = View.GONE
            return
        }
        
        // 如果有专门的推荐容器，使用它
        if (recommendationsContainer != null) {
            recommendationsContainer?.visibility = View.VISIBLE
            recommendationsContainer?.removeAllViews()
            
            // 添加标题
            val titleView = TextView(context).apply {
                text = "📚 相关推荐"
                textSize = 14f
                setTextColor(context.getColor(android.R.color.darker_gray))
                setPadding(0, 16, 0, 8)
            }
            recommendationsContainer?.addView(titleView)
            
            // 添加推荐项
            recommendations.take(5).forEach { recommendation ->
                val itemView = TextView(context).apply {
                    text = "• ${recommendation.title}"
                    textSize = 13f
                    setTextColor(context.getColor(R.color.floating_primary))
                    setPadding(8, 4, 8, 4)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        onRecommendationClickListener?.invoke(recommendation)
                    }
                }
                recommendationsContainer?.addView(itemView)
            }
        } else {
            // 如果没有专门的推荐容器，在内容后追加推荐信息
            val currentText = resultContent?.text?.toString() ?: ""
            val recommendationText = buildString {
                append(currentText)
                appendLine()
                appendLine()
                appendLine("📚 相关推荐：")
                recommendations.take(5).forEach { rec ->
                    appendLine("• ${rec.title}")
                }
            }
            resultContent?.text = recommendationText
        }
    }
    
    /**
     * 设置推荐项点击监听器
     *
     * TD-00031: 点击推荐项时触发新的查询
     *
     * @param listener 点击回调，参数为推荐项
     */
    fun setOnRecommendationClickListener(listener: (Recommendation) -> Unit) {
        onRecommendationClickListener = listener
    }

    /**
     * 动态设置内容区域最大高度
     *
     * @param height 最大高度（像素）
     */
    fun setMaxHeight(height: Int) {
        android.util.Log.d(TAG, "setMaxHeight: height=$height, resultScroll=${resultScroll != null}")
        resultScroll?.setMaxHeight(height)
        // 强制重新布局
        resultScroll?.requestLayout()
    }

    /**
     * 清空结果
     */
    fun clearResult() {
        currentResult = null
        visibility = View.GONE
    }

    /**
     * 设置复制按钮点击监听器
     *
     * @param listener 点击回调，参数为可复制的文本
     */
    fun setOnCopyClickListener(listener: (String) -> Unit) {
        onCopyClickListener = listener
    }

    /**
     * 设置重新生成按钮点击监听器
     *
     * @param listener 点击回调
     */
    fun setOnRegenerateClickListener(listener: () -> Unit) {
        onRegenerateClickListener = listener
    }

    private fun showRiskBadge(riskLevel: RiskLevel) {
        riskBadge?.visibility = View.VISIBLE
        riskBadge?.text = when (riskLevel) {
            RiskLevel.SAFE -> "安全"
            RiskLevel.WARNING -> "注意"
            RiskLevel.DANGER -> "危险"
        }

        val backgroundColor = when (riskLevel) {
            RiskLevel.SAFE -> R.color.risk_safe
            RiskLevel.WARNING -> R.color.risk_warning
            RiskLevel.DANGER -> R.color.risk_danger
        }
        riskBadge?.background?.setTint(ContextCompat.getColor(context, backgroundColor))
    }

    /**
     * BUG-00017/BUG-00021修复：确保复制和重新生成按钮始终可见
     * 
     * 问题：分析和润色模式下按钮不可见或未渲染
     * 原因：
     * 1. 按钮visibility可能在某些情况下被隐藏或未正确初始化
     * 2. findViewById可能在布局未完全inflate时返回null
     * 解决：
     * 1. 在每次显示结果时显式设置按钮为VISIBLE
     * 2. 如果按钮引用为null，尝试重新查找
     * 3. 使用post{}确保在布局完成后执行
     */
    private fun ensureButtonsVisible() {
        // 首先尝试直接设置可见性
        btnCopy?.visibility = View.VISIBLE
        btnRegenerate?.visibility = View.VISIBLE
        
        android.util.Log.d(TAG, "ensureButtonsVisible: btnCopy=${btnCopy != null}, btnRegenerate=${btnRegenerate != null}")
        
        // BUG-00021修复：如果按钮引用为null，尝试重新查找并设置可见性
        if (btnCopy == null || btnRegenerate == null) {
            android.util.Log.w(TAG, "按钮引用为null，尝试重新查找")
            post {
                // 重新查找按钮
                if (btnCopy == null) {
                    btnCopy = findViewById(R.id.btn_copy)
                    btnCopy?.setOnClickListener {
                        currentResult?.let { result ->
                            onCopyClickListener?.invoke(result.getCopyableText())
                        }
                    }
                }
                if (btnRegenerate == null) {
                    btnRegenerate = findViewById(R.id.btn_regenerate)
                    btnRegenerate?.setOnClickListener {
                        onRegenerateClickListener?.invoke()
                    }
                }
                
                // 设置可见性
                btnCopy?.visibility = View.VISIBLE
                btnRegenerate?.visibility = View.VISIBLE
                
                android.util.Log.d(TAG, "延迟查找后: btnCopy=${btnCopy != null}, btnRegenerate=${btnRegenerate != null}")
            }
        }
        
        // BUG-00021修复：额外的保护措施 - 确保按钮在布局完成后可见
        post {
            btnCopy?.visibility = View.VISIBLE
            btnRegenerate?.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val TAG = "ResultCard"
    }
}
