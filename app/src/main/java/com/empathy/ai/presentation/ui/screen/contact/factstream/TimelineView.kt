package com.empathy.ai.presentation.ui.screen.contact.factstream

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.util.PerformanceMetrics
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.card.AiSummaryCard
import com.empathy.ai.presentation.ui.component.card.ConversationCard
import com.empathy.ai.presentation.ui.component.card.MilestoneCard
import com.empathy.ai.presentation.ui.component.card.PhotoMomentCard
import com.empathy.ai.presentation.ui.component.emotion.EmotionalTimelineNode

/**
 * 时光轴视图组件
 *
 * 事实流的时光轴模式，沉浸式叙事体验
 *
 * 参考标准：
 * - [SD-00001] 代码规范和编码标准
 * - [AD-00001] 架构设计文档
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 *
 * 布局特点：
 * - 左侧：情绪节点和时间线
 * - 右侧：多样化卡片
 *
 * 性能优化（T066 + CR-00009改进）：
 * - 使用稳定的key参数（item.id）
 * - 使用contentType优化（区分不同卡片类型）
 * - 使用remember缓存计算结果
 * - 限制初始加载数量（首次加载≤50条）
 * - 实现分页加载（滚动到底部加载更多）
 * - 使用derivedStateOf优化滚动检测
 * - 自动降级机制：性能不达标时降级为简化视图
 *
 * 性能指标：
 * - 列表滚动帧率≥60fps
 * - 初始加载时间<1秒
 * - 滚动响应时间<16ms
 *
 * @param items 时间线项目列表
 * @param onItemClick 项目点击回调
 * @param onPerformanceDegraded 性能降级回调（可选）
 * @param modifier Modifier
 */
@Composable
fun TimelineView(
    items: List<TimelineItem>,
    modifier: Modifier = Modifier,
    onItemClick: ((TimelineItem) -> Unit)? = null,
    onConversationEdit: ((Long) -> Unit)? = null,
    onPerformanceDegraded: (() -> Unit)? = null
) {
    if (items.isEmpty()) {
        EmptyTimelineView(modifier = modifier)
        return
    }
    
    val listState = rememberLazyListState()
    
    // 分页加载状态
    var loadedCount by remember { mutableIntStateOf(PerformanceMetrics.INITIAL_LOAD_COUNT) }
    
    // 性能降级状态（CR-00009改进）
    var isDegraded by remember { mutableStateOf(false) }
    var frameDropCount by remember { mutableIntStateOf(0) }
    
    // 使用remember缓存显示项目，避免重复计算
    val displayItems = remember(items, loadedCount) {
        items.take(loadedCount)
    }
    
    // 检测是否滚动到底部，使用derivedStateOf优化
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && loadedCount < items.size
        }
    }
    
    // 性能监控和自动降级（CR-00009改进）
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            // 检测滚动性能
            val startTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(100)
            val elapsed = System.currentTimeMillis() - startTime
            
            // 如果响应时间超过阈值，增加掉帧计数
            if (elapsed > PerformanceMetrics.FRAME_TIME_WARNING_MS * 2) {
                frameDropCount++
                Log.w("TimelineView", "检测到性能问题: 响应时间=${elapsed}ms, 掉帧次数=$frameDropCount")
                
                // 连续3次掉帧则触发降级
                if (frameDropCount >= 3 && !isDegraded) {
                    isDegraded = true
                    Log.w("TimelineView", "TimelineView性能不达标，已降级为简化视图")
                    onPerformanceDegraded?.invoke()
                }
            } else {
                // 性能恢复，重置计数
                if (frameDropCount > 0) frameDropCount--
            }
        }
    }
    
    // 滚动到底部时加载更多
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadedCount = minOf(loadedCount + PerformanceMetrics.PAGE_SIZE, items.size)
        }
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = Dimensions.SpacingMedium,
            vertical = Dimensions.SpacingSmall
        ),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        items(
            items = displayItems,
            key = { it.id },
            // 使用contentType优化不同类型卡片的复用
            contentType = { item ->
                when (item) {
                    is TimelineItem.PhotoMoment -> "photo"
                    is TimelineItem.AiSummary -> "summary"
                    is TimelineItem.Milestone -> "milestone"
                    is TimelineItem.Conversation -> "conversation"
                    is TimelineItem.UserFact -> "user_fact"
                }
            }
        ) { item ->
            TimelineRow(
                item = item,
                onClick = { onItemClick?.invoke(item) },
                onConversationEdit = onConversationEdit,
                isSimplified = isDegraded // 降级时使用简化渲染
            )
        }
        
        // 加载更多提示
        if (loadedCount < items.size) {
            item(key = "load_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.SpacingMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载中... (${items.size - loadedCount} 条待加载)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 时间线行组件
 *
 * 包含情绪节点和卡片
 *
 * @param item 时间线项目
 * @param onClick 点击回调
 * @param onConversationEdit 对话编辑回调
 * @param isSimplified 是否使用简化渲染（性能降级模式）
 */
@Composable
private fun TimelineRow(
    item: TimelineItem,
    onClick: () -> Unit,
    onConversationEdit: ((Long) -> Unit)? = null,
    isSimplified: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        // 左侧：情绪节点和时间线（降级模式下简化显示）
        if (!isSimplified) {
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmotionalTimelineNode(emotionType = item.emotionType)
                VerticalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = Dimensions.TimelineLineWidth,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
        
        // 右侧：卡片
        Box(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineItem.PhotoMoment -> PhotoMomentCard(
                    item = item,
                    onClick = onClick
                )
                is TimelineItem.AiSummary -> AiSummaryCard(
                    item = item,
                    onClick = onClick
                )
                is TimelineItem.Milestone -> MilestoneCard(
                    item = item,
                    onClick = onClick
                )
                is TimelineItem.Conversation -> ConversationCard(
                    item = item,
                    onClick = onClick,
                    onLongClick = { onConversationEdit?.invoke(item.log.id) }
                )
                is TimelineItem.UserFact -> UserFactCard(
                    item = item,
                    onClick = onClick
                )
            }
        }
    }
}

/**
 * 用户事实卡片
 *
 * 显示用户手动添加的事实记录
 */
@Composable
private fun UserFactCard(
    item: TimelineItem.UserFact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 ${item.fact.key}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // 事实内容
            Text(
                text = item.fact.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            // 来源标签
            if (item.fact.source == com.empathy.ai.domain.model.FactSource.MANUAL) {
                Text(
                    text = "手动添加",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 空时间线视图
 */
@Composable
private fun EmptyTimelineView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📅",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "时光轴空空如也",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "开始记录你们的故事吧",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========== 预览 ==========

@Preview(name = "有数据的时光轴", showBackground = true)
@Composable
private fun PreviewTimelineViewWithData() {
    EmpathyTheme {
        TimelineView(
            items = listOf(
                TimelineItem.Conversation(
                    id = "1",
                    timestamp = System.currentTimeMillis(),
                    emotionType = EmotionType.SWEET,
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
                        content = "今天的互动整体氛围不错，你们讨论了周末的计划",
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
                    emotionType = EmotionType.GIFT,
                    title = "相识100天",
                    description = "从陌生到熟悉，感谢每一天的陪伴",
                    icon = "🏆"
                )
            )
        )
    }
}

@Preview(name = "空时光轴", showBackground = true)
@Composable
private fun PreviewTimelineViewEmpty() {
    EmpathyTheme {
        TimelineView(items = emptyList())
    }
}
