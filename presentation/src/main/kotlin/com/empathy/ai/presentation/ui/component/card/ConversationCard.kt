// Package card 实现了对话记录卡片组件
//
// 业务背景 (PRD-00008):
//   - 输入内容身份识别与双向对话历史
//   - 对方说的话和我的回复需要区分展示
//   - UI层隐藏身份前缀，以自然对话流形式展示
//
// 设计决策 (PRD-00008/6.2):
//   - 存储层：带身份前缀（【对方说】/【我正在回复】）
//   - 展示层：解析前缀，以气泡形式左右对齐
//
// 任务追踪:
//   - BUG-00071: 事实流对话/总结编辑点击无响应修复
//   - FEATURE-20260114: 身份前缀历史功能实现
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import com.empathy.ai.presentation.ui.component.message.ConversationBubble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话记录卡片组件
 *
 * 展示具体的对话内容，包括用户输入和AI响应
 * 支持长按编辑/删除
 *
 * @param item 对话记录数据
 * @param onClick 点击回调
 * @param onLongClick 长按回调（用于编辑/删除）
 * @param modifier Modifier
 */
@Composable
fun ConversationCard(
    item: TimelineItem.Conversation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val dimensions = AdaptiveDimensions.current

    // [Strategy] 身份前缀解析与缓存 (PRD-00008)
    // 使用 remember 避免重组时重复解析，提升性能
    // parseResult.role: CONTACT(对方) → 左对齐, USER(我) → 右对齐, LEGACY(旧数据) → 居中
    val parseResult = remember(item.log.userInput) {
        IdentityPrefixHelper.parse(item.log.userInput)
    }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: onLongClick // 点击时触发编辑
    ) {
        Column(modifier = Modifier.padding(dimensions.spacingMedium)) {
            // 时间和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${parseResult.role.displayName} · ${formatTime(item.log.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 总结状态
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.log.isSummarized) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Schedule
                        },
                        contentDescription = if (item.log.isSummarized) "已总结" else "待总结",
                        modifier = Modifier.size(dimensions.iconSizeSmall - 2.dp),
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
            
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            
            // 对话气泡（隐藏前缀，按身份左右对齐）
            // [Design Decision] PRD-00008/6.2: 复用 ConversationBubble 组件
            // - showHeader=false: 卡片头部已显示时间，避免重复
            // - maxLines=3: 限制行数，避免长文本把卡片撑高
            // - 气泡会根据 parseResult.role 自动左右对齐
            ConversationBubble(
                log = item.log,
                showHeader = false,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // AI响应已移除 - BUG-00001修复
            // 原因：AI回复显示为格式化摘要而非完整建议，影响用户体验
            // 对话记录只保留用户输入，AI建议在分析时实时生成
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
