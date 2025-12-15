package com.empathy.ai.presentation.ui.screen.contact.factstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.control.QuickFilterChips

/**
 * 清单列表视图组件
 *
 * 事实流的列表模式，高信息密度，便于快速检索
 *
 * @param items 时间线项目列表
 * @param selectedFilters 选中的筛选条件
 * @param onFilterToggle 筛选条件切换回调
 * @param onItemClick 项目点击回调
 * @param modifier Modifier
 */
@Composable
fun ListView(
    items: List<TimelineItem>,
    selectedFilters: Set<FilterType>,
    onFilterToggle: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((TimelineItem) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 快速筛选
        QuickFilterChips(
            selectedFilters = selectedFilters,
            onFilterToggle = onFilterToggle,
            modifier = Modifier.padding(vertical = Dimensions.SpacingSmall)
        )
        
        // 列表
        if (items.isEmpty()) {
            // 空状态
            EmptyListView(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = Dimensions.SpacingSmall)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    ListViewRow(
                        item = item,
                        onClick = { onItemClick?.invoke(item) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimensions.SpacingMedium),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 空列表视图
 */
@Composable
private fun EmptyListView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📭",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "暂无记录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "开始聊天，记录会自动出现在这里",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========== 预览 ==========

@Preview(name = "有数据的列表", showBackground = true)
@Composable
private fun PreviewListViewWithData() {
    EmpathyTheme {
        ListView(
            items = listOf(
                TimelineItem.Conversation(
                    id = "1",
                    timestamp = System.currentTimeMillis(),
                    emotionType = EmotionType.NEUTRAL,
                    log = ConversationLog(
                        id = 1,
                        contactId = "contact_1",
                        userInput = "今天想约她出去吃饭",
                        aiResponse = "建议用轻松的方式邀请",
                        timestamp = System.currentTimeMillis(),
                        isSummarized = true
                    )
                ),
                TimelineItem.AiSummary(
                    id = "2",
                    timestamp = System.currentTimeMillis() - 86400000,
                    emotionType = EmotionType.NEUTRAL,
                    summary = DailySummary(
                        id = 1,
                        contactId = "contact_1",
                        summaryDate = "2025-12-13",
                        content = "今天的互动整体氛围不错",
                        keyEvents = listOf(KeyEvent(event = "讨论周末计划", importance = 7)),
                        newFacts = emptyList(),
                        updatedTags = emptyList(),
                        relationshipScoreChange = 2,
                        relationshipTrend = RelationshipTrend.IMPROVING
                    )
                ),
                TimelineItem.Milestone(
                    id = "3",
                    timestamp = System.currentTimeMillis() - 172800000,
                    emotionType = EmotionType.SWEET,
                    title = "相识100天",
                    description = "从陌生到熟悉",
                    icon = "🏆"
                )
            ),
            selectedFilters = emptySet(),
            onFilterToggle = {}
        )
    }
}

@Preview(name = "空列表", showBackground = true)
@Composable
private fun PreviewListViewEmpty() {
    EmpathyTheme {
        ListView(
            items = emptyList(),
            selectedFilters = emptySet(),
            onFilterToggle = {}
        )
    }
}
