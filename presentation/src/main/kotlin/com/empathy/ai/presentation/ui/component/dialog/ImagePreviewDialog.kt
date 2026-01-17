package com.empathy.ai.presentation.ui.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import coil.load

/**
 * 图片预览对话框
 *
 * 功能: 全屏显示截图预览
 *
 * ## 使用方式
 * ```kotlin
 * val dialog = ImagePreviewDialog(context, imagePath)
 * dialog.show()
 * ```
 *
 * ## 设计决策
 * - 全屏显示,黑色半透明背景
 * - 图片居中显示,保持宽高比
 * - 使用Coil加载图片
 * - 点击外部关闭对话框
 *
 * @param context 上下文
 * @param imagePath 图片本地路径
 */
class ImagePreviewDialog(
    private val context: Context,
    private val imagePath: String
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDialog()
        loadPreviewImage()
    }

    /**
     * 设置对话框UI
     */
    private fun setupDialog() {
        container = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC000000"))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                // 点击外部关闭对话框
                dismiss()
            }
        }

        imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(32), dpToPx(32), dpToPx(32), dpToPx(32))
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        progressBar = ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            isIndeterminate = true
        }

        container.addView(imageView)
        container.addView(progressBar)
        setContentView(container)

        // 设置对话框属性
        window?.apply {
            // PRD-00036: 设置window type为TYPE_APPLICATION_OVERLAY，使Dialog可以从Service显示
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    /**
     * 加载预览图片
     */
    private fun loadPreviewImage() {
        // 显示加载进度
        progressBar.visibility = android.view.View.VISIBLE
        imageView.visibility = android.view.View.INVISIBLE

        // 使用Coil加载图片
        imageView.load(imagePath) {
            crossfade(true)
            placeholder(null)
            error(android.R.drawable.ic_dialog_alert)
            listener(
                onStart = {
                    android.util.Log.d("ImagePreviewDialog", "开始加载图片: $imagePath")
                },
                onSuccess = { _, _ ->
                    // 图片加载成功
                    progressBar.visibility = android.view.View.GONE
                    imageView.visibility = android.view.View.VISIBLE
                    android.util.Log.d("ImagePreviewDialog", "图片加载成功")
                },
                onError = { _, _ ->
                    // 图片加载失败
                    progressBar.visibility = android.view.View.GONE
                    android.util.Log.e("ImagePreviewDialog", "图片加载失败")
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
}
