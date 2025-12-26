package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.EmotionColors
import com.empathy.ai.presentation.theme.EmotionType

/**
 * 情绪节点组件
 * 
 * 技术要点:
 * - 使用Brush.linearGradient创建渐变背景
 * - 白色环形边框4dp
 * - 外层投影使用shadow修饰符（8dp）
 * - 节点大小40dp
 * - 中心显示情绪emoji（16sp）
 * 
 * @param emotionType 情绪类型
 * @param modifier 修饰符
 * @param size 节点大小
 * 
 * @see TDD-00020 4.1 EmotionNode情绪节点
 */
@Composable
fun EmotionNode(
    emotionType: EmotionType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val gradientColors = EmotionColors.getGradient(emotionType)
    
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = CircleShape
            )
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emotionType.emoji,
            fontSize = 16.sp
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "甜蜜", showBackground = true)
@Composable
private fun EmotionNodeSweetPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EmotionNode(emotionType = EmotionType.SWEET)
        }
    }
}

@Preview(name = "冲突", showBackground = true)
@Composable
private fun EmotionNodeConflictPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EmotionNode(emotionType = EmotionType.CONFLICT)
        }
    }
}

@Preview(name = "约会", showBackground = true)
@Composable
private fun EmotionNodeDatePreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EmotionNode(emotionType = EmotionType.DATE)
        }
    }
}

@Preview(name = "所有情绪类型", showBackground = true)
@Composable
private fun EmotionNodeAllTypesPreview() {
    EmpathyTheme {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            EmotionType.entries.forEach { type ->
                EmotionNode(emotionType = type)
            }
        }
    }
}
