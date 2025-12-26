package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 时光轴轴线组件
 * 
 * 设计规范：
 * - 在屏幕左侧（约20-30px处）绘制一条贯穿上下的极细浅灰实线
 * - 线宽1.5dp，颜色#E5E5EA
 * - 将所有零散的事件串联起来，形成连续的叙事
 * 
 * @param modifier 修饰符
 * @param lineColor 轴线颜色
 * @param lineWidth 轴线宽度
 */
@Composable
fun TimelineAxisLine(
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFE5E5EA),
    lineWidth: Dp = 1.5.dp
) {
    Canvas(
        modifier = modifier
            .width(lineWidth)
            .fillMaxHeight()
    ) {
        drawLine(
            color = lineColor,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = lineWidth.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "时光轴轴线", showBackground = true, heightDp = 200)
@Composable
private fun TimelineAxisLinePreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(start = 24.dp)) {
            TimelineAxisLine()
        }
    }
}
