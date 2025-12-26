package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed

/**
 * 现代化时光轴卡片组件
 * 
 * 设计规范：
 * - 纯白卡片背景(#FFFFFF)
 * - 圆角16dp
 * - 极淡的弥散投影（Y轴偏移2dp，模糊10dp）
 * - 标题黑色加粗
 * - 正文深灰色，常规字重
 * - 日期/时间移到卡片内部右上角，字号最小，浅灰色
 * - 来源标签做成极其迷你的浅灰底深灰字胶囊
 * - AI总结卡片带有微光效果或淡紫色背景+✨图标
 * 
 * @param title 标题
 * @param content 内容
 * @param time 时间（仅显示时刻，如"13:38"）
 * @param sourceLabel 来源标签（如"手动添加"）
 * @param isAiSummary 是否为AI总结卡片
 * @param aiSuggestion AI建议（可选）
 * @param scoreChange 关系分数变化（可选）
 * @param tags 标签列表
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernTimelineCard(
    title: String,
    content: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sourceLabel: String? = null,
    isAiSummary: Boolean = false,
    aiSuggestion: String? = null,
    scoreChange: Int? = null,
    tags: List<String> = emptyList()
) {
    val cardBackground = if (isAiSummary) {
        // AI总结卡片使用淡紫色渐变背景
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFAF8FF),
                Color(0xFFF3EEFF)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color.White, Color.White)
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .padding(16.dp)
        ) {
            Column {
                // 顶部行：标题 + 来源标签 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧：AI图标（如果是AI总结）+ 标题 + 来源标签
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAiSummary) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI总结",
                                tint = iOSPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // 来源标签（迷你胶囊）
                        if (sourceLabel != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            MiniSourceTag(text = sourceLabel)
                        }
                    }
                    
                    // 右侧：时间
                    Text(
                        text = time,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 内容
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = Color(0xFF3C3C43),
                    lineHeight = 20.sp
                )
                
                // AI建议区域（如果有）
                if (aiSuggestion != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AiSuggestionBox(suggestion = aiSuggestion)
                }
                
                // 关系分数变化（如果有）
                if (scoreChange != null && scoreChange != 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScoreChangeIndicator(scoreChange = scoreChange)
                }
                
                // 标签（如果有）
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            MiniTag(text = tag)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 迷你来源标签
 */
@Composable
private fun MiniSourceTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color(0xFF8E8E93)
        )
    }
}

/**
 * AI建议区域
 */
@Composable
private fun AiSuggestionBox(suggestion: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(iOSPurple.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = iOSPurple,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI 建议",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSPurple
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suggestion,
                fontSize = 13.sp,
                color = Color(0xFF3C3C43),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 分数变化指示器
 */
@Composable
private fun ScoreChangeIndicator(scoreChange: Int) {
    val (text, color) = if (scoreChange > 0) {
        "关系分数 +$scoreChange" to iOSGreen
    } else {
        "关系分数 $scoreChange" to iOSRed
    }
    
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

/**
 * 迷你标签
 */
@Composable
private fun MiniTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFF636366)
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "普通卡片", showBackground = true)
@Composable
private fun ModernTimelineCardNormalPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ModernTimelineCard(
                title = "广东富豪风格的约会",
                content = "今天一起去看了电影，她很开心，说下次还想一起看。整体氛围很好，感觉关系在稳步升温。",
                time = "14:30",
                sourceLabel = "手动添加",
                scoreChange = 5,
                tags = listOf("约会", "电影"),
                onClick = {}
            )
        }
    }
}

@Preview(name = "AI总结卡片", showBackground = true)
@Composable
private fun ModernTimelineCardAiSummaryPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ModernTimelineCard(
                title = "本周关系总结",
                content = "本周你们的关系有明显改善，主要体现在沟通频率增加和情感表达更加直接。",
                time = "周日",
                isAiSummary = true,
                aiSuggestion = "建议继续保持这种良好的沟通习惯，定期进行深度交流。",
                scoreChange = 12,
                onClick = {}
            )
        }
    }
}

@Preview(name = "带AI建议的卡片", showBackground = true)
@Composable
private fun ModernTimelineCardWithSuggestionPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ModernTimelineCard(
                title = "工作上的小争执",
                content = "因为工作的事情有点小争执，但最后还是和好了。",
                time = "20:15",
                aiSuggestion = "建议下次遇到类似情况时，先倾听对方的想法，再表达自己的观点。",
                scoreChange = -3,
                tags = listOf("工作", "沟通"),
                onClick = {}
            )
        }
    }
}
