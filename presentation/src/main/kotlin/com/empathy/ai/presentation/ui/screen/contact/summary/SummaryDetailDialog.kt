package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagUpdate
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 总结详情对话框
 *
 * 展示AI总结的完整内容，包括：
 * - 总结正文
 * - 关键事件列表
 * - 新发现的事实
 * - 标签更新
 * - 关系评分变化
 *
 * @param summary 总结数据
 * @param onDismiss 关闭回调
 */
@Composable
fun SummaryDetailDialog(
    summary: DailySummary,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题栏
                SummaryDetailHeader(
                    dateRange = summary.getDisplayDateRange(),
                    onClose = onDismiss
                )

                HorizontalDivider()

                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 总结正文
                    SummaryContentSection(content = summary.content)

                    // 关键事件
                    if (summary.keyEvents.isNotEmpty()) {
                        KeyEventsSection(events = summary.keyEvents)
                    }

                    // 新发现的事实
                    if (summary.newFacts.isNotEmpty()) {
                        NewFactsSection(facts = summary.newFacts)
                    }

                    // 标签更新
                    if (summary.updatedTags.isNotEmpty()) {
                        TagUpdatesSection(tags = summary.updatedTags)
                    }

                    // 关系评分变化
                    RelationshipChangeSection(
                        scoreChange = summary.relationshipScoreChange,
                        trend = summary.relationshipTrend
                    )
                }
            }
        }
    }
}

/**
 * 标题栏
 */
@Composable
private fun SummaryDetailHeader(
    dateRange: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "AI 情感晴雨表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭"
            )
        }
    }
}

/**
 * 总结正文区域
 */
@Composable
private fun SummaryContentSection(content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📝 总结",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 关键事件区域
 */
@Composable
private fun KeyEventsSection(events: List<KeyEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🎯 关键事件",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            events.forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${event.event}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    ImportanceBadge(importance = event.importance)
                }
            }
        }
    }
}

/**
 * 重要性徽章
 */
@Composable
private fun ImportanceBadge(importance: Int) {
    val (text, color) = when {
        importance >= 4 -> "高" to MaterialTheme.colorScheme.error
        importance >= 2 -> "中" to MaterialTheme.colorScheme.tertiary
        else -> "低" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * 新发现事实区域
 */
@Composable
private fun NewFactsSection(facts: List<Fact>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💡 新发现",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            facts.forEach { fact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${fact.key}：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fact.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 标签更新区域
 */
@Composable
private fun TagUpdatesSection(tags: List<TagUpdate>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🏷️ 标签更新",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            tags.forEach { tag ->
                val icon = when (tag.type.uppercase()) {
                    "RISK_RED" -> "🔴"
                    "STRATEGY_GREEN" -> "🟢"
                    else -> "⚪"
                }
                val actionText = when (tag.action.uppercase()) {
                    "ADD" -> "新增"
                    "REMOVE" -> "移除"
                    else -> tag.action
                }
                Text(
                    text = "$icon [$actionText] ${tag.content}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 关系评分变化区域
 */
@Composable
private fun RelationshipChangeSection(
    scoreChange: Int,
    trend: RelationshipTrend
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                scoreChange > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                scoreChange < 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📈 关系变化")
                Icon(
                    imageVector = when (trend) {
                        RelationshipTrend.IMPROVING -> Icons.AutoMirrored.Filled.TrendingUp
                        RelationshipTrend.DECLINING -> Icons.AutoMirrored.Filled.TrendingDown
                        RelationshipTrend.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
                    },
                    contentDescription = null,
                    tint = when (trend) {
                        RelationshipTrend.IMPROVING -> MaterialTheme.colorScheme.primary
                        RelationshipTrend.DECLINING -> MaterialTheme.colorScheme.error
                        RelationshipTrend.STABLE -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = when {
                    scoreChange > 0 -> "+$scoreChange 分"
                    scoreChange < 0 -> "$scoreChange 分"
                    else -> "无变化"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    scoreChange > 0 -> MaterialTheme.colorScheme.primary
                    scoreChange < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

// ==================== Previews ====================

@Preview(name = "总结详情对话框", showBackground = true)
@Composable
private fun PreviewSummaryDetailDialog() {
    EmpathyTheme {
        SummaryDetailDialog(
            summary = DailySummary(
                id = 1,
                contactId = "contact_1",
                summaryDate = "2025-12-14",
                content = "今天的互动整体氛围不错，你们讨论了周末的计划，对方表现出积极的态度。建议继续保持这种轻松愉快的交流方式，适当增加一些深入的话题讨论。",
                keyEvents = listOf(
                    KeyEvent(event = "讨论周末计划", importance = 4),
                    KeyEvent(event = "分享美食照片", importance = 2),
                    KeyEvent(event = "约定下次见面", importance = 5)
                ),
                newFacts = listOf(
                    Fact(key = "兴趣爱好", value = "喜欢日料", timestamp = System.currentTimeMillis(), source = FactSource.AI_INFERRED),
                    Fact(key = "工作", value = "最近项目比较忙", timestamp = System.currentTimeMillis(), source = FactSource.AI_INFERRED)
                ),
                updatedTags = listOf(
                    TagUpdate("ADD", "STRATEGY_GREEN", "周末约会话题"),
                    TagUpdate("ADD", "RISK_RED", "避免提及加班")
                ),
                relationshipScoreChange = 3,
                relationshipTrend = RelationshipTrend.IMPROVING
            ),
            onDismiss = {}
        )
    }
}
