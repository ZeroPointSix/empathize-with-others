package com.empathy.ai.presentation.ui.component.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import coil.load

/**
 * 图片预览视图 (PRD-00036 截图预览功能)
 *
 * 业务背景:
 *   - PRD-00036: 悬浮窗截图缩略图太小，用户无法清楚看到截图内容
 *   - FEATURE-20260118: 补齐返回键处理、背景透明度、90%尺寸规则
 *
 * 设计决策 (FEATURE-20260118 决策 #4):
 *   - 使用 WindowManager 叠层而非 Dialog，兼容 Service context 显示
 *   - 全屏显示，黑色半透明背景 (alpha=0.9 即 #E6000000)
 *   - 图片居中显示，保持宽高比，最大尺寸为屏幕宽高的 90%
 *   - 点击外部或按返回键关闭预览
 *
 * 数据流:
 *   FloatingViewV2.onScreenshotPreviewListener → FloatingWindowService.showScreenshotPreview() → ImagePreviewView.show()
 *
 * @param context 上下文
 * @param windowManager WindowManager 实例
 * @param imagePath 图片本地路径
 *
 * @see com.empathy.ai.presentation.ui.floating.RefinementOverlay
 * @see PRD-00036 截图预览功能
 * @see FEATURE-20260118 截图预览功能完善
 * @see BUG-00074 截图预览功能无法显示问题分析
 */
class ImagePreviewView(
    private val context: Context,
    private val windowManager: WindowManager,
    private val imagePath: String
) : FrameLayout(context) {

    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var backgroundView: View
    private var isShowing = false

    init {
        setupView()
    }

    /**
     * 设置视图UI
     */
    private fun setupView() {
        // 设置容器属性
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        isFocusable = true
        isFocusableInTouchMode = true

        // 背景遮罩（点击外部关闭预览）
        // PRD-00036: 黑色半透明背景 alpha=0.9 (#E6000000)，点击外部关闭预览
        // BUG-00074 修复: 分离背景点击区域，避免与图片点击事件冲突
        backgroundView = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#E6000000"))
            isClickable = true
            setOnClickListener { dismiss() }
        }

        // PRD-00036: 图片最大显示尺寸为屏幕宽高的 90%
        // 权衡: 留出边距确保图片不被截断，同时最大化显示区域
        val maxWidthPx = (resources.displayMetrics.widthPixels * 0.9f).toInt().coerceAtLeast(1)
        val maxHeightPx = (resources.displayMetrics.heightPixels * 0.9f).toInt().coerceAtLeast(1)

        // 创建ImageView
        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            maxWidth = maxWidthPx
            maxHeight = maxHeightPx
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            // PRD-00036: 点击图片不关闭预览（只有关击外部背景才关闭）
            setOnClickListener { /* 阻止事件传递到容器 */ }
        }

        // 创建ProgressBar
        progressBar = ProgressBar(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            isIndeterminate = true
        }

        addView(backgroundView)
        addView(imageView)
        addView(progressBar)
    }

    /**
     * 显示预览视图
     */
    fun show() {
        if (isShowing) return

        // 配置WindowManager参数
        // BUG-00074 修复: 动态选择窗口类型，兼容 Android 8+ 和旧版本
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(this, params)
            isShowing = true
            requestFocus()
            loadPreviewImage()
            android.util.Log.d(TAG, "预览视图显示成功: $imagePath")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "显示预览视图失败", e)
            Toast.makeText(context, "预览失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 隐藏预览视图
     */
    fun dismiss() {
        if (!isShowing) return

        try {
            windowManager.removeView(this)
            android.util.Log.d(TAG, "预览视图已关闭")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "关闭预览视图失败", e)
        } finally {
            isShowing = false
        }
    }

    /**
     * 检查预览视图是否正在显示
     *
     * @return true 如果正在显示
     */
    fun isShowing(): Boolean = isShowing

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            dismiss()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * 加载预览图片
     */
    private fun loadPreviewImage() {
        // 显示加载进度
        progressBar.visibility = VISIBLE
        imageView.visibility = INVISIBLE

        // 使用Coil加载图片
        imageView.load(imagePath) {
            crossfade(true)
            placeholder(null)
            error(android.R.drawable.ic_dialog_alert)
            listener(
                onStart = {
                    android.util.Log.d(TAG, "开始加载图片: $imagePath")
                },
                onSuccess = { _, _ ->
                    // 图片加载成功
                    progressBar.visibility = GONE
                    imageView.visibility = VISIBLE
                    android.util.Log.d(TAG, "图片加载成功")
                },
                onError = { _, _ ->
                    // 图片加载失败
                    progressBar.visibility = GONE
                    android.util.Log.e(TAG, "图片加载失败")
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            )
            size(com.empathy.ai.domain.util.PerformanceMetrics.IMAGE_MAX_SIZE_PX)
        }
    }

    /**
     * 将dp转换为px
     */
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    companion object {
        private const val TAG = "ImagePreviewView"
    }
}
