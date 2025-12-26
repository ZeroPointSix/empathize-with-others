package com.empathy.ai.presentation.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * 安全的Canvas绘制包装器
 * 
 * 添加try-catch错误处理，错误时显示降级占位符
 * 
 * @param modifier 修饰符
 * @param onDrawError 错误回调
 * @param onDraw 绘制逻辑
 * 
 * @see TDD-00020 9.5 Canvas绘制错误处理
 */
@Composable
fun SafeCanvas(
    modifier: Modifier = Modifier,
    onDrawError: (Throwable) -> Unit = {},
    onDraw: DrawScope.() -> Unit
) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    if (hasError) {
        // 降级显示：使用简单的占位符
        Box(
            modifier = modifier.background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "加载中...",
                color = iOSTextTertiary,
                fontSize = 12.sp
            )
        }
    } else {
        Canvas(modifier = modifier) {
            try {
                onDraw()
            } catch (e: Exception) {
                hasError = true
                errorMessage = e.message
                onDrawError(e)
            }
        }
    }
}

/**
 * 自适应圆环图
 * 根据内存状态选择渲染模式
 * 
 * @see TDD-00020 9.8 内存不足处理
 */
@Composable
fun AdaptiveHealthRingChart(
    progress: Float,
    score: Int,
    modifier: Modifier = Modifier
) {
    val memoryAdvice = remember { 
        com.empathy.ai.presentation.util.MemoryMonitor.getMemoryAdvice() 
    }
    
    when (memoryAdvice) {
        com.empathy.ai.presentation.util.MemoryAdvice.NORMAL -> {
            // 完整渲染：渐变 + 动画
            HealthRingChart(
                progress = progress,
                score = score,
                modifier = modifier
            )
        }
        com.empathy.ai.presentation.util.MemoryAdvice.REDUCE_QUALITY -> {
            // 简化渲染：纯色 + 无动画
            SimpleRingChart(
                progress = progress,
                score = score,
                modifier = modifier
            )
        }
    }
}
