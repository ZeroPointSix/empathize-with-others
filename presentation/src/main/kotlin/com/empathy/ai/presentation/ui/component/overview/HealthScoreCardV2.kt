package com.empathy.ai.presentation.ui.component.overview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * Apple Fitness风格关系健康度卡片
 *
 * 设计原则：
 * 1. 双层圆环设计：极淡灰色轨道 + 渐变进度环
 * 2. 根据分数使用不同颜色：高分绿色、中分橙色、低分红色
 * 3. 圆头末端（Round Cap）
 * 4. 右侧显示描述文案和迷你折线图
 *
 * @param score 关系分数 (0-100)
 * @param trendData 趋势数据（最近7天）
 * @param modifier 修饰符
 */
@Composable
fun HealthScoreCardV2(
    score: Int,
    trendData: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：Apple Fitness风格圆环
            AppleFitnessRing(
                progress = score / 100f,
                score = score
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // 右侧：描述和趋势
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 主描述
                Text(
                    text = getScoreTitle(score),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 副描述
                Text(
                    text = getScoreAdvice(score),
                    fontSize = 14.sp,
                    color = iOSTextSecondary
                )
                
                // 迷你折线图
                if (trendData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniSparkline(
                            data = trendData,
                            color = getScoreColor(score),
                            modifier = Modifier.size(60.dp, 24.dp)
                        )
                        Text(
                            text = "近7天",
                            fontSize = 12.sp,
                            color = iOSTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple Fitness风格圆环
 */
@Composable
private fun AppleFitnessRing(
    progress: Float,
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 10.dp
) {
    val scoreColor = getScoreColor(score)
    
    // 渐变Brush - 根据分数选择颜色
    val gradientBrush = remember(score) {
        when {
            score >= 61 -> Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF34C759),  // iOS绿
                    Color(0xFF30D158),
                    Color(0xFF34C759)
                )
            )
            score >= 31 -> Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF9500),  // iOS橙
                    Color(0xFFFFCC00),
                    Color(0xFFFF9500)
                )
            )
            else -> Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF3B30),  // iOS红
                    Color(0xFFFF6B6B),
                    Color(0xFFFF3B30)
                )
            )
        }
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
            
            // 背景轨道 - 极淡灰色
            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // 进度圆弧 - 渐变色
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = "分",
                fontSize = 10.sp,
                color = iOSTextSecondary
            )
        }
    }
}

/**
 * 迷你折线图
 */
@Composable
private fun MiniSparkline(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = (maxValue - minValue).coerceAtLeast(1f)
        
        val path = Path()
        val stepX = width / (data.size - 1).coerceAtLeast(1)
        
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minValue) / range * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * 根据分数获取颜色
 */
private fun getScoreColor(score: Int): Color {
    return when {
        score >= 61 -> iOSGreen
        score >= 31 -> iOSOrange
        else -> iOSRed
    }
}

/**
 * 根据分数获取标题
 */
private fun getScoreTitle(score: Int): String {
    return when {
        score >= 81 -> "关系非常亲密"
        score >= 61 -> "关系良好"
        score >= 31 -> "关系一般"
        else -> "关系较冷淡"
    }
}

/**
 * 根据分数获取建议
 */
private fun getScoreAdvice(score: Int): String {
    return when {
        score >= 81 -> "继续保持，你们的关系很棒！"
        score >= 61 -> "有进一步发展的空间"
        score >= 31 -> "需要更多互动来增进感情"
        else -> "建议主动联系"
    }
}

// ==================== Previews ====================

@Preview(name = "高分-85分", showBackground = true)
@Composable
private fun HealthScoreCardV2HighPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthScoreCardV2(
                score = 85,
                trendData = listOf(80f, 82f, 78f, 85f, 83f, 86f, 85f)
            )
        }
    }
}

@Preview(name = "中分-50分", showBackground = true)
@Composable
private fun HealthScoreCardV2MediumPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthScoreCardV2(
                score = 50,
                trendData = listOf(45f, 48f, 52f, 50f, 47f, 51f, 50f)
            )
        }
    }
}

@Preview(name = "低分-25分", showBackground = true)
@Composable
private fun HealthScoreCardV2LowPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthScoreCardV2(
                score = 25,
                trendData = listOf(30f, 28f, 25f, 22f, 24f, 26f, 25f)
            )
        }
    }
}

@Preview(name = "无趋势数据", showBackground = true)
@Composable
private fun HealthScoreCardV2NoTrendPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthScoreCardV2(score = 60)
        }
    }
}
