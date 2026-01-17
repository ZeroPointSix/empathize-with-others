package com.empathy.ai.presentation.ui.component.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import coil.load

/**
 * 图片预览视图
 *
 * 功能: 全屏显示截图预览
 *
 * ## 使用方式
 * ```kotlin
 * val previewView = ImagePreviewView(context, windowManager, imagePath)
 * previewView.show()
 * ```
 *
 * ## 设计决策
 * - 使用WindowManager而不是Dialog，避免Service context的限制
 * - 全屏显示，黑色半透明背景
 * - 图片居中显示，保持宽高比
 * - 使用Coil加载图片
 * - 点击外部关闭预览
 *
 * ## 技术实现
 * - 参考RefinementOverlay的实现方式
 * - 使用TYPE_APPLICATION_OVERLAY允许从Service显示
 * - 使用FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH支持点击外部关闭
 *
 * @param context 上下文
 * @param windowManager WindowManager实例
 * @param imagePath 图片本地路径
 *
 * @see com.empathy.ai.presentation.ui.floating.RefinementOverlay
 * @see PRD-00036 截图预览功能
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

        // 背景遮罩（点击外部关闭预览）
        // BUG-00074 修复: 分离背景点击区域，避免与图片点击事件冲突
        backgroundView = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC000000"))
            isClickable = true
            setOnClickListener { dismiss() }
        }

        // 计算图片最大显示尺寸（留出 64dp 边距）
        val maxWidthPx = (resources.displayMetrics.widthPixels - dpToPx(64)).coerceAtLeast(1)
        val maxHeightPx = (resources.displayMetrics.heightPixels - dpToPx(64)).coerceAtLeast(1)

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
            // 点击图片不关闭预览（阻止事件传递）
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
            isShowing = false
            android.util.Log.d(TAG, "预览视图已关闭")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "关闭预览视图失败", e)
        }
    }

    /**
     * 检查预览视图是否正在显示
     *
     * @return true 如果正在显示
     */
    fun isShowing(): Boolean = isShowing

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
