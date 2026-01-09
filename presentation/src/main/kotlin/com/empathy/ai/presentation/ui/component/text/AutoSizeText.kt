package com.empathy.ai.presentation.ui.component.text

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 自动调整大小的文本组件
 *
 * BUG-00054: 解决模型列表长文本截断问题
 *
 * 功能：
 * - 自动缩小字体以适应可用空间
 * - 保持文本可读性
 * - 当字体缩小到最小值仍无法完全显示时，显示省略号
 *
 * 实现原理：
 * - 使用onTextLayout回调检测文本是否溢出
 * - 如果溢出，逐步缩小字体（每次缩小10%）
 * - 直到文本不溢出或达到最小字体
 * - 使用drawWithContent控制绘制时机，避免闪烁
 *
 * @param text 要显示的文本
 * @param modifier Modifier
 * @param maxFontSize 最大字体大小
 * @param minFontSize 最小字体大小
 * @param color 文本颜色
 * @param fontWeight 字体粗细
 * @param textAlign 文本对齐方式
 * @param maxLines 最大行数
 * @param style 文本样式
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 10.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    style: TextStyle = LocalTextStyle.current
) {
    // 使用text和maxFontSize作为key，当它们变化时重置状态
    var currentFontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text, maxFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        color = color,
        fontSize = currentFontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
        softWrap = false,  // 禁止软换行，确保单行显示
        onTextLayout = { textLayoutResult ->
            if (!readyToDraw) {
                if (textLayoutResult.didOverflowWidth) {
                    // 文本溢出，尝试缩小字体
                    val newFontSize = currentFontSize * 0.9f
                    if (newFontSize >= minFontSize) {
                        currentFontSize = newFontSize
                    } else {
                        // 已达到最小字体，停止缩小
                        currentFontSize = minFontSize
                        readyToDraw = true
                    }
                } else {
                    // 文本不溢出，可以绘制
                    readyToDraw = true
                }
            }
        }
    )
}

/**
 * 带有约束的自动调整大小文本组件
 * 
 * 使用BoxWithConstraints来获取可用宽度，更精确地计算字体大小
 */
@Composable
fun AutoSizeTextWithConstraints(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 10.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    style: TextStyle = LocalTextStyle.current
) {
    BoxWithConstraints(modifier = modifier) {
        AutoSizeText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            maxFontSize = maxFontSize,
            minFontSize = minFontSize,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style
        )
    }
}
