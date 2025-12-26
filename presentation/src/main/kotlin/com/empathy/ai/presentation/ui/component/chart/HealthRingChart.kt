package com.empathy.ai.presentation.ui.component.chart

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * Apple Health风格圆环图
 * 
 * 技术要点:
 * - 使用Canvas绘制渐变圆弧
 * - 使用remember缓存Brush对象避免重复创建
 * - 使用animateFloatAsState实现进度动画（1000ms, FastOutSlowInEasing）
 * - 圆头末端使用StrokeCap.Round
 * - 背景轨道使用#E5E5EA
 * 
 * @param progress 进度值 (0f-1f)
 * @param score 显示的分数
 * @param modifier 修饰符
 * @param size 圆环大小
 * @param strokeWidth 圆环宽度
 * 
 * @see TDD-00020 3.2 HealthRingChart圆环图
 */
@Composable
fun HealthRingChart(
    progress: Float,
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 8.dp
) {
    // 缓存渐变Brush
    val gradientBrush = remember {
        Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF6B9D),  // 粉红
                Color(0xFFFF8A80),  // 珊瑚
                Color(0xFFFFC371),  // 金黄
                Color(0xFFFF6B9D)   // 回到粉红形成闭环
            )
        )
    }
    
    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokeWidthPx) / 2
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            val arcSize = Size(radius * 2, radius * 2)
            
            // 背景轨道
            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // 进度圆弧
            if (animatedProgress > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
        
        // 中心分数
        Text(
            text = score.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

/**
 * 简化版圆环图（无渐变、无动画）
 * 用于低端设备降级显示
 * 
 * @see TDD-00020 9.8 内存不足处理
 */
@Composable
fun SimpleRingChart(
    progress: Float,
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 8.dp,
    progressColor: Color = Color(0xFF007AFF)
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokeWidthPx) / 2
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            val arcSize = Size(radius * 2, radius * 2)
            
            // 背景轨道
            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // 进度圆弧（纯色，无动画）
            val clampedProgress = progress.coerceIn(0f, 1f)
            if (clampedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * clampedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
        
        // 中心分数
        Text(
            text = score.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "85分", showBackground = true)
@Composable
private fun HealthRingChart85Preview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthRingChart(
                progress = 0.85f,
                score = 85
            )
        }
    }
}

@Preview(name = "50分", showBackground = true)
@Composable
private fun HealthRingChart50Preview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthRingChart(
                progress = 0.5f,
                score = 50
            )
        }
    }
}

@Preview(name = "100分", showBackground = true)
@Composable
private fun HealthRingChart100Preview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthRingChart(
                progress = 1f,
                score = 100
            )
        }
    }
}

@Preview(name = "简化版", showBackground = true)
@Composable
private fun SimpleRingChartPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SimpleRingChart(
                progress = 0.75f,
                score = 75
            )
        }
    }
}
