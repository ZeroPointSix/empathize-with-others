package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 情绪时间节点组件（优化版）
 * 
 * 设计规范：
 * - 根据事件类型在线轴上放置小巧的彩色实心圆点
 * - 甜蜜：粉色小爱心节点 ❤️
 * - 冲突：红色闪电节点 ⚡
 * - 约会：紫色餐具节点 🍽️
 * - 礼物：金色礼物节点 🎁
 * - 深谈：青色对话节点 💬
 * - 中性：灰色思考节点 💭
 * - 节点大小：28dp（比原来的40dp更小巧）
 * - 带有轻微投影效果
 * 
 * @param emotionType 情绪类型
 * @param modifier 修饰符
 * @param size 节点大小
 */
@Composable
fun EmotionTimelineNodeV2(
    emotionType: EmotionType,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val nodeColor = getEmotionNodeColor(emotionType)
    val emoji = emotionType.emoji
    
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .background(nodeColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 12.sp
        )
    }
}

/**
 * 获取情绪节点颜色
 */
private fun getEmotionNodeColor(emotionType: EmotionType): Color {
    return when (emotionType) {
        EmotionType.SWEET -> Color(0xFFFFB6C1)      // 粉色
        EmotionType.CONFLICT -> Color(0xFFFF6B6B)   // 红色
        EmotionType.DATE -> Color(0xFFBA55D3)       // 紫色
        EmotionType.GIFT -> Color(0xFFFFD700)       // 金色
        EmotionType.DEEP_TALK -> Color(0xFF20B2AA)  // 青色
        EmotionType.NEUTRAL -> Color(0xFFB0C4DE)    // 灰蓝色
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "甜蜜节点", showBackground = true)
@Composable
private fun EmotionTimelineNodeSweetPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EmotionTimelineNodeV2(emotionType = EmotionType.SWEET)
        }
    }
}

@Preview(name = "冲突节点", showBackground = true)
@Composable
private fun EmotionTimelineNodeConflictPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EmotionTimelineNodeV2(emotionType = EmotionType.CONFLICT)
        }
    }
}

@Preview(name = "所有情绪节点", showBackground = true)
@Composable
private fun EmotionTimelineNodeAllPreview() {
    EmpathyTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmotionType.entries.forEach { type ->
                EmotionTimelineNodeV2(emotionType = type)
            }
        }
    }
}
