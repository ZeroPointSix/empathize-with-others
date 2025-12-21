package com.empathy.ai.presentation.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import com.empathy.ai.presentation.ui.component.state.EditedBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI总结卡片组件
 *
 * 展示AI对一段时间的复盘和洞察，具有特殊的视觉风格
 * TD-00012: 支持点击编辑和显示已编辑标识
 *
 * @param item AI总结数据
 * @param onClick 点击回调（用于编辑）
 * @param modifier Modifier
 */
@Composable
fun AiSummaryCard(
    item: TimelineItem.AiSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
            // 标题行：图标 + 标题 + 已编辑标识
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI分析",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI 情感晴雨表",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                // TD-00012: 显示已编辑标识
                if (item.summary.isUserModified) {
                    EditedBadge(lastModifiedTime = item.summary.lastModifiedTime)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 日期
            Text(
                text = formatDate(item.summary.summaryDate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 总结内容
            Text(
                text = item.summary.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 关键事件
            if (item.summary.keyEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "关键事件：${item.summary.keyEvents.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 关系分数变化
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "关系变化：",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val scoreChange = item.summary.relationshipScoreChange
                val changeText = if (scoreChange >= 0) "+$scoreChange" else "$scoreChange"
                Text(
                    text = changeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (scoreChange >= 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM月dd日", Locale.CHINESE)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

// ========== 预览 ==========

@Preview(name = "AI总结卡片", showBackground = true)
@Composable
private fun PreviewAiSummaryCard() {
    EmpathyTheme {
        AiSummaryCard(
            item = TimelineItem.AiSummary(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                summary = DailySummary(
                    id = 1,
                    contactId = "contact_1",
                    summaryDate = "2025-12-14",
                    content = "今天的互动整体氛围不错，你们讨论了周末的计划，对方表现出积极的态度。建议继续保持这种轻松愉快的交流方式。",
                    keyEvents = listOf(
                        KeyEvent(event = "讨论周末计划", importance = 7),
                        KeyEvent(event = "分享美食照片", importance = 5)
                    ),
                    newFacts = emptyList(),
                    updatedTags = emptyList(),
                    relationshipScoreChange = 2,
                    relationshipTrend = RelationshipTrend.IMPROVING
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
