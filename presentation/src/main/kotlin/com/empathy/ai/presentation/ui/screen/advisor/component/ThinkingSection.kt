package com.empathy.ai.presentation.ui.screen.advisor.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.BlockMetadata
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 思考过程展示组件
 *
 * 参考Cherry Studio和iOS设计，实现可折叠的思考过程展示：
 * - 可折叠/展开
 * - 显示思考耗时
 * - 流式更新时显示动画
 * - iOS风格的卡片设计
 *
 * 业务背景 (FD-00028):
 * - DeepSeek R1等模型支持思考过程展示
 * - 用户可以查看AI的推理过程，增加透明度
 * - 默认折叠，避免占用过多屏幕空间
 *
 * @param content 思考内容
 * @param isStreaming 是否正在流式接收
 * @param metadata 块元数据（包含思考耗时）
 * @param modifier 修饰符
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
@Composable
fun ThinkingSection(
    content: String,
    isStreaming: Boolean,
    metadata: BlockMetadata?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = iOSPurple.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题栏（可点击折叠）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 思考图标（流式时旋转动画）
                    ThinkingIcon(isStreaming = isStreaming)

                    Spacer(modifier = Modifier.width(8.dp))

                    // 标题文本
                    Text(
                        text = if (isStreaming) "思考中..." else "思考过程",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = iOSPurple
                    )

                    // 思考耗时
                    metadata?.thinkingMs?.let { ms ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatThinkingTime(ms),
                            fontSize = 12.sp,
                            color = iOSTextSecondary
                        )
                    }
                }

                // 折叠/展开图标
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = iOSPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 思考内容（可折叠）
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text(
                            text = content,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = iOSTextSecondary
                        )
                        if (isStreaming) {
                            StreamingCursor()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 思考图标组件
 *
 * 流式接收时显示旋转动画，完成后静止。
 */
@Composable
private fun ThinkingIcon(isStreaming: Boolean) {
    if (isStreaming) {
        val infiniteTransition = rememberInfiniteTransition(label = "thinkingRotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Icon(
            imageVector = Icons.Outlined.Psychology,
            contentDescription = null,
            tint = iOSPurple,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Psychology,
            contentDescription = null,
            tint = iOSPurple,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 格式化思考时间
 *
 * @param ms 毫秒数
 * @return 格式化后的时间字符串
 */
private fun formatThinkingTime(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> String.format("%.1fs", ms / 1000.0)
        else -> String.format("%.1fmin", ms / 60000.0)
    }
}
