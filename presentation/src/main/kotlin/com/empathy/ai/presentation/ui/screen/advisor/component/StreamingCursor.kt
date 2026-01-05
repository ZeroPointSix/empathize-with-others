package com.empathy.ai.presentation.ui.screen.advisor.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.empathy.ai.presentation.theme.iOSBlue

/**
 * 流式光标组件
 *
 * 在流式响应过程中显示闪烁的光标，提示用户AI正在生成内容。
 * 参考iOS风格设计，使用蓝色光标和平滑的闪烁动画。
 *
 * 业务背景 (FD-00028):
 * - 流式响应需要视觉反馈，让用户知道AI正在生成
 * - 光标闪烁是常见的打字机效果，用户熟悉
 *
 * @param modifier 修饰符
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
@Composable
fun StreamingCursor(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Text(
        text = "▌",
        modifier = modifier,
        color = iOSBlue.copy(alpha = alpha),
        style = MaterialTheme.typography.bodyLarge
    )
}
