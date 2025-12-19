package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.empathy.ai.R
import com.empathy.ai.domain.model.AiResult
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.presentation.ui.component.MaxHeightScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * AI结果展示卡片组件
 *
 * 支持展示三种类型的AI结果：
 * - 分析结果（AnalysisResult）
 * - 润色结果（PolishResult）
 * - 回复结果（ReplyResult）
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
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
    private var btnCopy: MaterialButton? = null
    private var btnRegenerate: MaterialButton? = null

    private var currentResult: AiResult? = null
    private var onCopyClickListener: ((String) -> Unit)? = null
    private var onRegenerateClickListener: (() -> Unit)? = null

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
        }
    }

    /**
     * 动态设置内容区域最大高度
     *
     * @param height 最大高度（像素）
     */
    fun setMaxHeight(height: Int) {
        resultScroll?.setMaxHeight(height)
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
     * BUG-00017修复：确保复制和重新生成按钮始终可见
     * 
     * 问题：分析和润色模式下按钮不可见
     * 原因：按钮visibility可能在某些情况下被隐藏或未正确初始化
     * 解决：在每次显示结果时显式设置按钮为VISIBLE
     */
    private fun ensureButtonsVisible() {
        btnCopy?.visibility = View.VISIBLE
        btnRegenerate?.visibility = View.VISIBLE
        
        // 添加调试日志，帮助排查问题
        android.util.Log.d(TAG, "ensureButtonsVisible: btnCopy=${btnCopy != null}, btnRegenerate=${btnRegenerate != null}")
    }

    companion object {
        private const val TAG = "ResultCard"
    }
}
