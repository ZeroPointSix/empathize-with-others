package com.empathy.ai.presentation.ui.component

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import com.empathy.ai.R

/**
 * 支持maxHeight属性的自定义ScrollView
 *
 * Android标准ScrollView不支持maxHeight属性，当内容超长时会无限扩展。
 * 此组件通过重写onMeasure方法，实现maxHeight约束。
 *
 * 使用方法：
 * ```xml
 * <com.empathy.ai.presentation.ui.component.MaxHeightScrollView
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:maxHeight="220dp">
 *     <!-- 内容 -->
 * </com.empathy.ai.presentation.ui.component.MaxHeightScrollView>
 * ```
 *
 * @see BUG-00018 分析模式复制/重新生成按钮不可见问题
 */
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private var maxHeightPx: Int = Int.MAX_VALUE

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.MaxHeightScrollView)
            try {
                maxHeightPx = typedArray.getDimensionPixelSize(
                    R.styleable.MaxHeightScrollView_maxHeight,
                    Int.MAX_VALUE
                )
            } finally {
                typedArray.recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var newHeightMeasureSpec = heightMeasureSpec

        // 如果设置了maxHeight，则限制测量高度
        if (maxHeightPx != Int.MAX_VALUE) {
            val mode = MeasureSpec.getMode(heightMeasureSpec)
            val size = MeasureSpec.getSize(heightMeasureSpec)

            when (mode) {
                MeasureSpec.UNSPECIFIED -> {
                    // 无约束时，使用maxHeight作为AT_MOST约束
                    newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
                }
                MeasureSpec.AT_MOST -> {
                    // 已有AT_MOST约束时，取较小值
                    newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        minOf(size, maxHeightPx),
                        MeasureSpec.AT_MOST
                    )
                }
                MeasureSpec.EXACTLY -> {
                    // 精确约束时，取较小值
                    newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        minOf(size, maxHeightPx),
                        MeasureSpec.EXACTLY
                    )
                }
            }
        }

        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

    /**
     * 动态设置最大高度
     *
     * @param maxHeight 最大高度（像素）
     */
    fun setMaxHeight(maxHeight: Int) {
        if (maxHeightPx != maxHeight) {
            maxHeightPx = maxHeight
            requestLayout()
        }
    }

    /**
     * 动态设置最大高度（dp）
     *
     * @param maxHeightDp 最大高度（dp）
     */
    fun setMaxHeightDp(maxHeightDp: Float) {
        val px = (maxHeightDp * context.resources.displayMetrics.density).toInt()
        setMaxHeight(px)
    }

    /**
     * 获取当前最大高度设置
     *
     * @return 最大高度（像素），如果未设置则返回Int.MAX_VALUE
     */
    fun getMaxHeight(): Int = maxHeightPx

    companion object {
        private const val TAG = "MaxHeightScrollView"
    }
}
