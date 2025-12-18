package com.empathy.ai.presentation.ui.floating

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.empathy.ai.R
import com.empathy.ai.domain.model.FloatingBubbleState
import kotlin.math.sqrt

/**
 * 悬浮球视图
 *
 * 支持：
 * - 拖动到屏幕任意位置
 * - 四种状态显示（IDLE/LOADING/SUCCESS/ERROR）
 * - 点击展开悬浮窗
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 * @see PRD-00010 悬浮球状态指示与拖动功能需求
 */
class FloatingBubbleView(
    context: Context,
    private val windowManager: WindowManager
) : FrameLayout(context) {

    companion object {
        private const val TAG = "FloatingBubbleView"

        /** 悬浮球尺寸（dp） */
        const val BUBBLE_SIZE_DP = 56

        /** 图标尺寸（dp） */
        const val ICON_SIZE_DP = 32

        /** 点击判定阈值（dp） */
        private const val CLICK_THRESHOLD_DP = 10

        /** 点击时长阈值（ms） */
        private const val CLICK_TIME_THRESHOLD_MS = 200L

        /** 加载动画时长（ms） */
        private const val LOADING_ANIMATION_DURATION_MS = 1000L

        /** 成功弹跳动画时长（ms） */
        private const val SUCCESS_BOUNCE_DURATION_MS = 300L
    }

    // 当前状态
    private var currentState = FloatingBubbleState.IDLE

    // 拖动相关
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchStartTime = 0L
    private var isDragging = false

    // 阈值（像素）
    private val clickThresholdPx: Int
    private val density: Float

    // 动画
    private var rotationAnimator: ObjectAnimator? = null
    private var bounceAnimatorX: ObjectAnimator? = null
    private var bounceAnimatorY: ObjectAnimator? = null

    // 回调
    private var onBubbleClickListener: (() -> Unit)? = null
    private var onPositionChangedListener: ((Int, Int) -> Unit)? = null

    // 视图组件
    private val iconView: ImageView
    private val progressView: ProgressBar

    // 布局参数
    private var layoutParams: WindowManager.LayoutParams? = null

    init {
        density = context.resources.displayMetrics.density
        clickThresholdPx = (CLICK_THRESHOLD_DP * density).toInt()

        // 设置悬浮球大小
        val sizePx = (BUBBLE_SIZE_DP * density).toInt()
        val iconSizePx = (ICON_SIZE_DP * density).toInt()

        // 设置背景
        setBackgroundResource(R.drawable.bg_bubble_idle)
        elevation = 8 * density

        // 创建图标视图
        iconView = ImageView(context).apply {
            val iconParams = LayoutParams(iconSizePx, iconSizePx)
            iconParams.gravity = Gravity.CENTER
            layoutParams = iconParams
            setImageResource(R.drawable.ic_floating_button)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        addView(iconView)

        // 创建进度指示器（默认隐藏）
        progressView = ProgressBar(context, null, android.R.attr.progressBarStyle).apply {
            val progressParams = LayoutParams(iconSizePx, iconSizePx)
            progressParams.gravity = Gravity.CENTER
            layoutParams = progressParams
            visibility = View.GONE
            isIndeterminate = true
        }
        addView(progressView)

        // 设置触摸监听
        setupTouchListener()

        android.util.Log.d(TAG, "FloatingBubbleView 初始化完成，尺寸: ${sizePx}px")
    }

    /**
     * 设置触摸监听器
     *
     * 实现拖动和点击的区分
     */
    private fun setupTouchListener() {
        setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                    if (distance > clickThresholdPx) {
                        isDragging = true
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + deltaY.toInt()

                        // 边界保护：至少50%在屏幕内
                        applyBoundaryProtection(params)

                        try {
                            windowManager.updateViewLayout(this, params)
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "更新视图位置失败", e)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - touchStartTime

                    if (!isDragging && touchDuration < CLICK_TIME_THRESHOLD_MS) {
                        // 视为点击
                        android.util.Log.d(TAG, "检测到点击事件")
                        onBubbleClickListener?.invoke()
                    } else if (isDragging) {
                        // 拖动结束，保存位置
                        android.util.Log.d(TAG, "拖动结束，位置: (${params.x}, ${params.y})")
                        onPositionChangedListener?.invoke(params.x, params.y)
                    }
                    true
                }

                else -> false
            }
        }
    }

    /**
     * 应用边界保护
     *
     * 确保悬浮球至少50%在屏幕内
     */
    private fun applyBoundaryProtection(params: WindowManager.LayoutParams) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val halfSize = (BUBBLE_SIZE_DP * density / 2).toInt()

        params.x = params.x.coerceIn(-halfSize, screenWidth - halfSize)
        params.y = params.y.coerceIn(-halfSize, screenHeight - halfSize)
    }

    // ==================== 状态切换 ====================

    /**
     * 设置悬浮球状态
     *
     * @param state 目标状态
     */
    fun setState(state: FloatingBubbleState) {
        if (currentState == state) return

        android.util.Log.d(TAG, "状态切换: $currentState -> $state")
        currentState = state

        // 停止所有动画
        stopAllAnimations()

        when (state) {
            FloatingBubbleState.IDLE -> showIdleState()
            FloatingBubbleState.LOADING -> showLoadingState()
            FloatingBubbleState.SUCCESS -> showSuccessState()
            FloatingBubbleState.ERROR -> showErrorState()
        }
    }

    /**
     * 获取当前状态
     */
    fun getState(): FloatingBubbleState = currentState

    private fun showIdleState() {
        iconView.visibility = View.VISIBLE
        progressView.visibility = View.GONE
        iconView.setImageResource(R.drawable.ic_floating_button)
        iconView.clearColorFilter()
        setBackgroundResource(R.drawable.bg_bubble_idle)
    }

    private fun showLoadingState() {
        iconView.visibility = View.GONE
        progressView.visibility = View.VISIBLE
        setBackgroundResource(R.drawable.bg_bubble_idle)
    }

    private fun showSuccessState() {
        iconView.visibility = View.VISIBLE
        progressView.visibility = View.GONE
        iconView.setImageResource(R.drawable.ic_bubble_success)
        iconView.clearColorFilter()
        setBackgroundResource(R.drawable.bg_bubble_success)

        // 弹跳动画
        startBounceAnimation()
    }

    private fun showErrorState() {
        iconView.visibility = View.VISIBLE
        progressView.visibility = View.GONE
        iconView.setImageResource(R.drawable.ic_bubble_error)
        iconView.clearColorFilter()
        setBackgroundResource(R.drawable.bg_bubble_error)
    }

    /**
     * 启动弹跳动画
     */
    private fun startBounceAnimation() {
        bounceAnimatorX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.2f, 1f).apply {
            duration = SUCCESS_BOUNCE_DURATION_MS
            interpolator = OvershootInterpolator()
            start()
        }

        bounceAnimatorY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.2f, 1f).apply {
            duration = SUCCESS_BOUNCE_DURATION_MS
            interpolator = OvershootInterpolator()
            start()
        }
    }

    /**
     * 停止所有动画
     */
    private fun stopAllAnimations() {
        rotationAnimator?.cancel()
        rotationAnimator = null
        bounceAnimatorX?.cancel()
        bounceAnimatorX = null
        bounceAnimatorY?.cancel()
        bounceAnimatorY = null

        // 重置缩放
        scaleX = 1f
        scaleY = 1f
    }

    // ==================== 回调设置 ====================

    /**
     * 设置点击监听器
     */
    fun setOnBubbleClickListener(listener: () -> Unit) {
        onBubbleClickListener = listener
    }

    /**
     * 设置位置变化监听器
     */
    fun setOnPositionChangedListener(listener: (Int, Int) -> Unit) {
        onPositionChangedListener = listener
    }

    // ==================== 布局参数 ====================

    /**
     * 创建WindowManager布局参数
     *
     * @param x 初始X坐标
     * @param y 初始Y坐标
     * @return WindowManager.LayoutParams
     */
    fun createLayoutParams(x: Int, y: Int): WindowManager.LayoutParams {
        val sizePx = (BUBBLE_SIZE_DP * density).toInt()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            sizePx,
            sizePx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }.also {
            layoutParams = it
        }
    }

    /**
     * 更新布局参数引用
     */
    fun updateLayoutParams(params: WindowManager.LayoutParams) {
        layoutParams = params
    }

    // ==================== 清理 ====================

    /**
     * 清理资源
     */
    fun cleanup() {
        android.util.Log.d(TAG, "清理FloatingBubbleView资源")
        stopAllAnimations()
        onBubbleClickListener = null
        onPositionChangedListener = null
        layoutParams = null
    }
}
