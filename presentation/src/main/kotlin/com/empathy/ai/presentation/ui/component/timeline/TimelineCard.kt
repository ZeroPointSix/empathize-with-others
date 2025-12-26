package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.EmotionType
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 时光轴卡片组件
 * 
 * 技术要点:
 * - AI总结卡片添加紫色边框（iOSPurple.copy(alpha = 0.3f)）
 * - 显示时间戳（12sp, iOSTextSecondary）
 * - 显示内容（15sp, 22sp行高）
 * - AI建议区域使用紫色背景（iOSPurple.copy(alpha = 0.08f)）
 * - 关系分数变化显示（正数绿色，负数红色）
 * - 标签胶囊显示
 * 
 * @param item 时光轴数据项
 * @param onClick 点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 4.4 TimelineCard时光轴卡片
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineCard(
    item: TimelineItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (item.isAiSummary) {
        modifier.border(1.dp, iOSPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    } else {
        modifier
    }
    
    Card(
        modifier = cardModifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 时间戳
            Text(
                text = item.timestamp,
                fontSize = 12.sp,
                color = iOSTextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 内容
            Text(
                text = item.content,
                fontSize = 15.sp,
                color = iOSTextPrimary,
                lineHeight = 22.sp
            )
            
            // AI建议（如果有）
            if (item.aiSuggestion != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            iOSPurple.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "AI 建议",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSPurple
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.aiSuggestion,
                            fontSize = 14.sp,
                            color = iOSTextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            // 关系分数变化（如果有）
            if (item.scoreChange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "关系分数 ${if (item.scoreChange > 0) "+" else ""}${item.scoreChange}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.scoreChange > 0) iOSGreen else iOSRed
                )
            }
            
            // 标签（如果有）
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.tags.forEach { tag ->
                        TagChip(text = tag)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                iOSBlue.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = iOSBlue
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "普通卡片", showBackground = true)
@Composable
private fun TimelineCardNormalPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineCard(
                item = TimelineItem(
                    id = "1",
                    emotionType = EmotionType.SWEET,
                    timestamp = "今天 14:30",
                    content = "今天一起去看了电影，她很开心，说下次还想一起看。",
                    scoreChange = 5,
                    tags = listOf("约会", "电影")
                ),
                onClick = {}
            )
        }
    }
}

@Preview(name = "带AI建议", showBackground = true)
@Composable
private fun TimelineCardWithAiSuggestionPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineCard(
                item = TimelineItem(
                    id = "2",
                    emotionType = EmotionType.CONFLICT,
                    timestamp = "昨天 20:15",
                    content = "因为工作的事情有点小争执，但最后还是和好了。",
                    aiSuggestion = "建议下次遇到类似情况时，先倾听对方的想法，再表达自己的观点。",
                    scoreChange = -3,
                    tags = listOf("工作", "沟通")
                ),
                onClick = {}
            )
        }
    }
}

@Preview(name = "AI总结卡片", showBackground = true)
@Composable
private fun TimelineCardAiSummaryPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineCard(
                item = TimelineItem(
                    id = "3",
                    emotionType = EmotionType.DEEP_TALK,
                    timestamp = "本周总结",
                    content = "本周你们的关系有明显改善，主要体现在沟通频率增加和情感表达更加直接。",
                    isAiSummary = true,
                    aiSuggestion = "建议继续保持这种良好的沟通习惯，定期进行深度交流。",
                    scoreChange = 12
                ),
                onClick = {}
            )
        }
    }
}
