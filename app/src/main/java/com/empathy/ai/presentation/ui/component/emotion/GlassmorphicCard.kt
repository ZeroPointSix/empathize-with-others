package com.empathy.ai.presentation.ui.component.emotion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 磨砂玻璃卡片组件
 *
 * 提供半透明磨砂玻璃效果的卡片容器，营造现代、通透的视觉感受
 *
 * 职责：
 * - 提供半透明背景和模糊效果
 * - 支持白色边框光晕
 * - 支持可选的点击交互
 * - 提供细微阴影增强层次感
 *
 * 技术要点：
 * - 使用Surface实现圆角和阴影
 * - 使用Brush实现渐变光晕效果
 * - 支持可选的点击事件，带有Ripple效果
 *
 * @param modifier Modifier
 * @param backgroundColor 背景颜色（默认半透明）
 * @param borderColor 边框颜色（默认半透明白色）
 * @param onClick 可选的点击事件
 * @param content 卡片内容
 *
 * 使用示例：
 * ```kotlin
 * GlassmorphicCard(
 *     onClick = { /* 处理点击 */ }
 * ) {
 *     Text("卡片内容")
 * }
 * ```
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Surface(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
            } else Modifier
        ),
        shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
        color = backgroundColor,
        border = BorderStroke(width = 1.dp, color = borderColor),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
        ) {
            content()
        }
    }
}

// ========== 预览 ==========

@Preview(name = "基础卡片", showBackground = true)
@Composable
private fun PreviewGlassmorphicCard() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "磨砂玻璃卡片",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "这是一个带有半透明背景和白色边框光晕的卡片",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(name = "可点击卡片", showBackground = true)
@Composable
private fun PreviewGlassmorphicCardClickable() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* 点击事件 */ }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "可点击卡片",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "点击会有Ripple效果",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(name = "自定义颜色卡片", showBackground = true)
@Composable
private fun PreviewGlassmorphicCardCustomColor() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "自定义颜色",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "可以自定义背景色和边框色",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
