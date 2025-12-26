package com.empathy.ai.presentation.ui.screen.contact.factstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.chip.SolidTagChip
import com.empathy.ai.presentation.ui.component.chip.SolidTagColors
import com.empathy.ai.presentation.ui.component.state.EditedBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 列表视图行组件
 *
 * 清单列表模式下的单行展示，高信息密度
 *
 * 布局结构：
 * - 左侧：日期（MM-DD）
 * - 中间：[类别图标] + 标题文本 + 已编辑标识（TD-00012）
 * - 右侧：状态标签
 *
 * @param item 时间线项目
 * @param onClick 点击回调
 * @param onConversationEdit 对话编辑回调
 * @param onFactEdit 事实编辑回调（TD-00012）
 * @param onSummaryEdit 总结编辑回调（TD-00012）
 * @param modifier Modifier
 */
@Composable
fun ListViewRow(
    item: TimelineItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onConversationEdit: (() -> Unit)? = null,
    onFactEdit: (() -> Unit)? = null,
    onSummaryEdit: (() -> Unit)? = null
) {
    // 确定点击回调
    val clickHandler: (() -> Unit)? = when (item) {
        is TimelineItem.Conversation -> onConversationEdit
        is TimelineItem.UserFact -> onFactEdit
        is TimelineItem.AiSummary -> onSummaryEdit
        else -> onClick
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = clickHandler != null) { 
                clickHandler?.invoke()
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.md,
                    vertical = AppSpacing.sm
                ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：日期
            Text(
                text = formatDate(item.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp)
            )
            
            // 中间：图标 + 标题 + 已编辑标识
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getItemIcon(item),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getItemTitle(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // TD-00012: 显示已编辑标识
                getEditedAt(item)?.let { lastModifiedTime ->
                    EditedBadge(lastModifiedTime = lastModifiedTime)
                }
            }
            
            // 右侧：状态标签
            getItemTag(item)?.let { (text, color) ->
                SolidTagChip(
                    text = text,
                    backgroundColor = color,
                    textColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 获取项目图标
 */
private fun getItemIcon(item: TimelineItem): ImageVector {
    return when (item) {
        is TimelineItem.AiSummary -> Icons.Default.Psychology
        is TimelineItem.Milestone -> Icons.Default.Star
        is TimelineItem.Conversation -> Icons.Default.Chat
        is TimelineItem.PhotoMoment -> Icons.Default.Star
        is TimelineItem.UserFact -> Icons.Default.Edit
    }
}

/**
 * 获取项目标题
 */
private fun getItemTitle(item: TimelineItem): String {
    return when (item) {
        is TimelineItem.AiSummary -> item.summary.content.take(30) + "..."
        is TimelineItem.Milestone -> item.title
        is TimelineItem.Conversation -> item.log.userInput.take(30) + "..."
        is TimelineItem.PhotoMoment -> item.description.take(30) + "..."
        is TimelineItem.UserFact -> "${item.fact.key}: ${item.fact.value}".take(30) + "..."
    }
}

/**
 * 获取项目标签
 */
private fun getItemTag(item: TimelineItem): Pair<String, androidx.compose.ui.graphics.Color>? {
    return when (item) {
        is TimelineItem.AiSummary -> "AI总结" to SolidTagColors.Interest
        is TimelineItem.Milestone -> "里程碑" to SolidTagColors.Personality
        is TimelineItem.Conversation -> {
            if (item.log.isSummarized) {
                "已总结" to SolidTagColors.Interest
            } else {
                null
            }
        }
        is TimelineItem.PhotoMoment -> null
        is TimelineItem.UserFact -> "手动添加" to SolidTagColors.Personality
    }
}

/**
 * TD-00012: 获取项目的编辑时间
 */
private fun getEditedAt(item: TimelineItem): Long? {
    return when (item) {
        is TimelineItem.Conversation -> if (item.log.isUserModified) item.log.lastModifiedTime else null
        is TimelineItem.UserFact -> if (item.fact.isUserModified) item.fact.lastModifiedTime else null
        is TimelineItem.AiSummary -> if (item.summary.isUserModified) item.summary.lastModifiedTime else null
        else -> null
    }
}

// ========== 预览 ==========

@Preview(name = "对话记录行", showBackground = true)
@Composable
private fun PreviewListViewRowConversation() {
    EmpathyTheme {
        ListViewRow(
            item = TimelineItem.Conversation(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                log = ConversationLog(
                    id = 1,
                    contactId = "contact_1",
                    userInput = "今天想约她出去吃饭，但不知道怎么开口比较好",
                    aiResponse = "建议用轻松的方式邀请",
                    timestamp = System.currentTimeMillis(),
                    isSummarized = true
                )
            )
        )
    }
}

@Preview(name = "AI总结行", showBackground = true)
@Composable
private fun PreviewListViewRowAiSummary() {
    EmpathyTheme {
        ListViewRow(
            item = TimelineItem.AiSummary(
                id = "2",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                summary = DailySummary(
                    id = 1,
                    contactId = "contact_1",
                    summaryDate = "2025-12-14",
                    content = "今天的互动整体氛围不错，你们讨论了周末的计划",
                    keyEvents = listOf(KeyEvent(event = "讨论周末计划", importance = 7)),
                    newFacts = emptyList(),
                    updatedTags = emptyList(),
                    relationshipScoreChange = 2,
                    relationshipTrend = RelationshipTrend.IMPROVING
                )
            )
        )
    }
}

@Preview(name = "里程碑行", showBackground = true)
@Composable
private fun PreviewListViewRowMilestone() {
    EmpathyTheme {
        ListViewRow(
            item = TimelineItem.Milestone(
                id = "3",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.SWEET,
                title = "相识100天",
                description = "从陌生到熟悉",
                icon = "🏆"
            )
        )
    }
}
