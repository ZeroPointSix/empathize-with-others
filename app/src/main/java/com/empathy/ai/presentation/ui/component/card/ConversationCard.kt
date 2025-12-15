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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话记录卡片组件
 *
 * 展示具体的对话内容，包括用户输入和AI响应
 *
 * @param item 对话记录数据
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun ConversationCard(
    item: TimelineItem.Conversation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
            // 时间和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(item.log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 总结状态
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.log.isSummarized) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Schedule
                        },
                        contentDescription = if (item.log.isSummarized) "已总结" else "待总结",
                        modifier = Modifier.size(14.dp),
                        tint = if (item.log.isSummarized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (item.log.isSummarized) "已总结" else "待总结",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.log.isSummarized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 用户输入
            Text(
                text = "你：${item.log.userInput}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // AI响应（如果有）
            item.log.aiResponse?.let { response ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI：${response}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ========== 预览 ==========

@Preview(name = "对话卡片 - 已总结", showBackground = true)
@Composable
private fun PreviewConversationCardSummarized() {
    EmpathyTheme {
        ConversationCard(
            item = TimelineItem.Conversation(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                log = ConversationLog(
                    id = 1,
                    contactId = "contact_1",
                    userInput = "今天想约她出去吃饭，但不知道怎么开口比较好",
                    aiResponse = "建议用轻松的方式邀请，比如说发现了一家不错的餐厅想一起去尝尝",
                    timestamp = System.currentTimeMillis(),
                    isSummarized = true
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "对话卡片 - 待总结", showBackground = true)
@Composable
private fun PreviewConversationCardPending() {
    EmpathyTheme {
        ConversationCard(
            item = TimelineItem.Conversation(
                id = "2",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.SWEET,
                log = ConversationLog(
                    id = 2,
                    contactId = "contact_1",
                    userInput = "她说喜欢我送的礼物，感觉很开心",
                    aiResponse = null,
                    timestamp = System.currentTimeMillis(),
                    isSummarized = false
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
