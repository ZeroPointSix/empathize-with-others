package com.empathy.ai.presentation.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSGreen

/**
 * Sparkline趋势图
 * 
 * 技术要点:
 * - 使用Path绘制平滑曲线
 * - 使用quadraticBezierTo实现二次贝塞尔曲线
 * - 缓存Path对象避免重复创建
 * - 归一化数据处理
 * - 绘制终点圆点（3dp半径）
 * 
 * @param data 数据点列表
 * @param modifier 修饰符
 * @param lineColor 线条颜色
 * @param width 图表宽度
 * @param height 图表高度
 * 
 * @see TDD-00020 3.3 SparklineChart趋势图
 */
@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = iOSGreen,
    width: Dp = 60.dp,
    height: Dp = 28.dp
) {
    // 缓存Path对象
    val path = remember { Path() }
    
    // 归一化数据
    val normalizedData = remember(data) {
        if (data.isEmpty()) return@remember emptyList()
        val max = data.maxOrNull() ?: 1f
        val min = data.minOrNull() ?: 0f
        val range = (max - min).coerceAtLeast(0.001f)
        data.map { (it - min) / range }
    }
    
    Canvas(modifier = modifier.size(width, height)) {
        if (normalizedData.size < 2) return@Canvas
        
        path.reset()
        
        val stepX = size.width / (normalizedData.size - 1)
        val points = normalizedData.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = size.height * (1 - value)
            )
        }
        
        // 绘制平滑曲线
        path.moveTo(points.first().x, points.first().y)
        
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2
            
            path.quadraticBezierTo(
                prev.x + (midX - prev.x) * 0.5f, prev.y,
                midX, (prev.y + curr.y) / 2
            )
            path.quadraticBezierTo(
                midX + (curr.x - midX) * 0.5f, curr.y,
                curr.x, curr.y
            )
        }
        
        // 绘制曲线
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // 绘制终点圆点
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = points.last()
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "上升趋势", showBackground = true)
@Composable
private fun SparklineChartUpPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SparklineChart(
                data = listOf(60f, 65f, 70f, 68f, 75f, 80f, 85f)
            )
        }
    }
}

@Preview(name = "下降趋势", showBackground = true)
@Composable
private fun SparklineChartDownPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SparklineChart(
                data = listOf(85f, 80f, 75f, 70f, 65f, 60f, 55f),
                lineColor = Color(0xFFFF3B30)
            )
        }
    }
}

@Preview(name = "波动趋势", showBackground = true)
@Composable
private fun SparklineChartWavePreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SparklineChart(
                data = listOf(70f, 75f, 65f, 80f, 70f, 85f, 75f)
            )
        }
    }
}

@Preview(name = "平稳趋势", showBackground = true)
@Composable
private fun SparklineChartStablePreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SparklineChart(
                data = listOf(75f, 76f, 74f, 75f, 76f, 75f, 74f)
            )
        }
    }
}
