package com.empathy.ai.domain.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

/**
 * 区域截图遮罩层（挖空矩形取景框）。
 *
 * 业务背景 (PRD-00036):
 * - 用户在任意 App 内通过悬浮窗快速截取“当前屏幕的一块区域”作为上下文补充。
 *
 * 交互约束 (PRD-00036/3.2, FD-00036/3.2):
 * - 只支持“单指拖拽创建矩形 → 松手确认”这一条主路径（不记忆上一次框选）。
 * - 背景为半透明黑色全屏覆盖；取景框区域挖空高亮。
 *
 * 实现说明 (TDD-00036):
 * - 使用 `PorterDuff.Mode.CLEAR` 在遮罩上挖空，需要启用 `LAYER_TYPE_SOFTWARE`。
 */
class ScreenshotOverlayView(context: Context) : View(context) {
    var onSelectionStart: (() -> Unit)? = null
    var onSelectionComplete: ((Rect) -> Unit)? = null
    var hintText: String? = null

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2
        isAntiAlias = true
    }
    private val hintPaint = Paint().apply {
        color = Color.WHITE
        textSize = resources.displayMetrics.density * 14
        isAntiAlias = true
    }
    private val hintPaddingPx = (12 * resources.displayMetrics.density).toInt()

    private val selectionRect = Rect()
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private val minSizePx = (24 * resources.displayMetrics.density).toInt()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        if (!selectionRect.isEmpty) {
            canvas.drawRect(selectionRect, clearPaint)
            canvas.drawRect(selectionRect, borderPaint)
        }
        hintText?.let { text ->
            val x = hintPaddingPx.toFloat()
            val y = hintPaddingPx + hintPaint.textSize
            canvas.drawText(text, x, y, hintPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onSelectionStart?.invoke()
                startX = event.x
                startY = event.y
                endX = event.x
                endY = event.y
                updateSelection()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                updateSelection()
                return true
            }
            MotionEvent.ACTION_UP -> {
                endX = event.x
                endY = event.y
                updateSelection()
                if (selectionRect.width() >= minSizePx && selectionRect.height() >= minSizePx) {
                    onSelectionComplete?.invoke(Rect(selectionRect))
                } else {
                    clearSelection()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                clearSelection()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearSelection() {
        selectionRect.setEmpty()
        invalidate()
    }

    private fun updateSelection() {
        val left = startX.coerceAtMost(endX).toInt()
        val right = startX.coerceAtLeast(endX).toInt()
        val top = startY.coerceAtMost(endY).toInt()
        val bottom = startY.coerceAtLeast(endY).toInt()
        selectionRect.set(left, top, right, bottom)
        invalidate()
    }
}
