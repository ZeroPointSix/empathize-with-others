package com.empathy.ai.presentation.ui.component.emotion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.LocalSemanticColors
import com.empathy.ai.presentation.theme.getEmotionColor

/**
 * 情绪时间线节点组件
 *
 * 在时间线上显示情绪Emoji，传递事件的情感色彩
 *
 * 职责：
 * - 根据情绪类型显示对应的Emoji
 * - 提供圆形背景和边框
 * - 支持不同尺寸
 *
 * @param emotionType 情绪类型
 * @param modifier Modifier
 */
@Composable
fun EmotionalTimelineNode(
    emotionType: EmotionType,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalSemanticColors.current
    val emotionColor = semanticColors.getEmotionColor(emotionType)
    
    Box(
        modifier = modifier
            .size(Dimensions.TimelineNodeSize)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = emotionColor.copy(alpha = 0.5f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emotionType.emoji,
            fontSize = 16.sp
        )
    }
}

// ========== 预览 ==========

@Preview(name = "所有情绪类型", showBackground = true)
@Composable
private fun PreviewEmotionalTimelineNodes() {
    EmpathyTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmotionType.entries.forEach { emotion ->
                EmotionalTimelineNode(emotionType = emotion)
            }
        }
    }
}

@Preview(name = "甜蜜节点", showBackground = true)
@Composable
private fun PreviewEmotionalTimelineNodeSweet() {
    EmpathyTheme {
        EmotionalTimelineNode(
            emotionType = EmotionType.SWEET,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "冲突节点", showBackground = true)
@Composable
private fun PreviewEmotionalTimelineNodeConflict() {
    EmpathyTheme {
        EmotionalTimelineNode(
            emotionType = EmotionType.CONFLICT,
            modifier = Modifier.padding(16.dp)
        )
    }
}
