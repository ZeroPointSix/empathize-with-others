package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.empathy.ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 微调覆盖层组件
 *
 * 使用WindowManager覆盖层实现，避免在Service环境下使用AlertDialog导致的BadTokenException
 *
 * 功能：
 * - 直接重新生成（无微调指令）
 * - 按方向生成（有微调指令）
 * - 取消操作
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
class RefinementOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {

    private var overlayView: View? = null
    private var refinementCard: MaterialCardView? = null
    private var refinementInputLayout: TextInputLayout? = null
    private var refinementInput: TextInputEditText? = null
    private var btnCancel: MaterialButton? = null
    private var btnRegenerateDirect: MaterialButton? = null
    private var btnRegenerateWithInstruction: MaterialButton? = null

    private var isShowing = false

    private var onDirectRegenerateListener: (() -> Unit)? = null
    private var onRegenerateWithInstructionListener: ((String) -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    /**
     * 显示微调覆盖层
     */
    fun show() {
        if (isShowing) return

        // 创建覆盖层视图
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_refinement, null)
        initViews()
        setupClickListeners()
        setupTextWatcher()

        // 配置WindowManager参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(overlayView, params)
            isShowing = true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show refinement overlay", e)
        }
    }

    /**
     * 隐藏微调覆盖层
     */
    fun dismiss() {
        if (!isShowing) return

        try {
            overlayView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to dismiss refinement overlay", e)
        }

        overlayView = null
        isShowing = false
        onDismissListener?.invoke()
    }

    /**
     * 检查覆盖层是否正在显示
     *
     * @return true 如果正在显示
     */
    fun isShowing(): Boolean = isShowing

    /**
     * 设置直接重新生成监听器
     *
     * @param listener 回调
     */
    fun setOnDirectRegenerateListener(listener: () -> Unit) {
        onDirectRegenerateListener = listener
    }

    /**
     * 设置按方向生成监听器
     *
     * @param listener 回调，参数为微调指令
     */
    fun setOnRegenerateWithInstructionListener(listener: (String) -> Unit) {
        onRegenerateWithInstructionListener = listener
    }

    /**
     * 设置关闭监听器
     *
     * @param listener 回调
     */
    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    private fun initViews() {
        overlayView?.let { view ->
            refinementCard = view.findViewById(R.id.refinement_card)
            refinementInputLayout = view.findViewById(R.id.refinement_input_layout)
            refinementInput = view.findViewById(R.id.refinement_input)
            btnCancel = view.findViewById(R.id.btn_cancel)
            btnRegenerateDirect = view.findViewById(R.id.btn_regenerate_direct)
            btnRegenerateWithInstruction = view.findViewById(R.id.btn_regenerate_with_instruction)
        }
    }

    private fun setupClickListeners() {
        // 点击背景关闭
        overlayView?.setOnClickListener { dismiss() }

        // 点击卡片不关闭
        refinementCard?.setOnClickListener { /* 阻止事件传递 */ }

        // 取消按钮
        btnCancel?.setOnClickListener { dismiss() }

        // 直接重新生成按钮
        btnRegenerateDirect?.setOnClickListener {
            onDirectRegenerateListener?.invoke()
            dismiss()
        }

        // 按方向生成按钮
        btnRegenerateWithInstruction?.setOnClickListener {
            val instruction = refinementInput?.text?.toString()?.trim() ?: ""
            if (instruction.isNotBlank()) {
                onRegenerateWithInstructionListener?.invoke(instruction)
                dismiss()
            }
        }
    }

    private fun setupTextWatcher() {
        refinementInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 根据输入内容启用/禁用"按方向生成"按钮
                val hasInstruction = !s.isNullOrBlank()
                btnRegenerateWithInstruction?.isEnabled = hasInstruction
            }
        })
    }

    companion object {
        private const val TAG = "RefinementOverlay"
    }
}
