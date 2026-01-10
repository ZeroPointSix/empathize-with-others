package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagUpdate
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.state.EmptyView
import kotlinx.coroutines.delay

/**
 * 现代化时光轴视图组件
 * 
 * 设计规范：
 * - 屏幕左侧有一条优雅的细线贯穿始终，像时间的刻度
 * - 线上点缀着不同颜色的小圆点，代表着悲欢离合
 * - 右侧是一排排洁白、圆润的卡片，清晰地记录着每一个"事实"
 * - 没有杂乱的按钮，没有浑浊的背景色
 * - 滑动屏幕时，就像在翻阅一本精心编排的电子手账
 * 
 * 技术要点：
 * - 左侧轴线位于距离屏幕左边缘约24dp处
 * - 情绪节点在轴线上，根据事件类型显示不同颜色
 * - 日期分组标题（今天、昨天、12月26日等）
 * - 卡片内仅保留时间（如13:38），不重复显示日期
 * - 淡入动画效果（400ms）+ 错落延迟（50ms间隔）
 * 
 * @param items 时间线项目列表
 * @param onItemClick 项目点击回调
 * @param onFactEdit 事实编辑回调（BUG-00065：点击UserFact类型时触发）
 * @param modifier 修饰符
 */
@Composable
fun ModernTimelineView(
    items: List<TimelineItem>,
    modifier: Modifier = Modifier,
    onItemClick: ((TimelineItem) -> Unit)? = null,
    onFactEdit: ((String) -> Unit)? = null
) {
    if (items.isEmpty()) {
        EmptyTimelineState(modifier = modifier)
        return
    }
    
    // 按日期分组
    val groupedItems = remember(items) {
        groupItemsByDate(items)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 背景轴线（贯穿整个列表）
        Box(
            modifier = Modifier
                .padding(start = 24.dp)
                .width(1.5.dp)
                .fillMaxHeight()
                .background(Color(0xFFE5E5EA))
        )
        
        // 内容列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            groupedItems.forEach { (date, dayItems) ->
                // 日期分组标题
                item(key = "header_$date") {
                    DateSectionHeader(
                        timestamp = dayItems.first().timestamp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                
                // 该日期下的所有项目
                itemsIndexed(
                    items = dayItems,
                    key = { _, item -> item.id }
                ) { index, item ->
                    ModernTimelineRow(
                        item = item,
                        index = index,
                        isLast = index == dayItems.lastIndex,
                        onClick = { onItemClick?.invoke(item) },
                        onFactEdit = onFactEdit
                    )
                }
            }
        }
    }
}

/**
 * 现代化时光轴行组件
 * 
 * @param item 时间线项目
 * @param index 项目索引（用于动画延迟）
 * @param isLast 是否是最后一项
 * @param onClick 通用点击回调
 * @param onFactEdit 事实编辑回调（BUG-00065）
 */
@Composable
private fun ModernTimelineRow(
    item: TimelineItem,
    index: Int,
    isLast: Boolean,
    onClick: () -> Unit,
    onFactEdit: ((String) -> Unit)? = null
) {
    // 淡入动画状态
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(400)
                )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
        ) {
            // 左侧：情绪节点（在轴线上）
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                EmotionTimelineNodeV2(emotionType = item.emotionType)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 右侧：内容卡片
            Column(modifier = Modifier.weight(1f)) {
                ModernTimelineCard(
                    title = getItemTitle(item),
                    content = getItemContent(item),
                    time = formatTimeOnly(item.timestamp),
                    sourceLabel = getSourceLabel(item),
                    isAiSummary = item is TimelineItem.AiSummary,
                    aiSuggestion = getAiSuggestion(item),
                    scoreChange = getScoreChange(item),
                    tags = getTags(item),
                    onClick = {
                        // BUG-00065: 区分事实类型的点击
                        if (item is TimelineItem.UserFact && onFactEdit != null) {
                            onFactEdit(item.fact.id)  // 事实类型：触发编辑
                        } else {
                            onClick()  // 其他类型：通用点击
                        }
                    }
                )
                
                if (!isLast) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 按日期分组项目
 */
private fun groupItemsByDate(items: List<TimelineItem>): List<Pair<String, List<TimelineItem>>> {
    val groups = mutableListOf<Pair<String, MutableList<TimelineItem>>>()
    var currentDate: String? = null
    var currentGroup: MutableList<TimelineItem>? = null
    
    items.sortedByDescending { it.timestamp }.forEach { item ->
        val dateKey = getDateKey(item.timestamp)
        
        if (dateKey != currentDate) {
            currentDate = dateKey
            currentGroup = mutableListOf()
            groups.add(dateKey to currentGroup!!)
        }
        
        currentGroup?.add(item)
    }
    
    return groups
}

/**
 * 获取日期键（用于分组）
 */
private fun getDateKey(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 获取项目标题
 */
private fun getItemTitle(item: TimelineItem): String {
    return when (item) {
        is TimelineItem.Conversation -> "对话记录"
        is TimelineItem.AiSummary -> "AI 智能总结"
        is TimelineItem.Milestone -> item.title
        is TimelineItem.PhotoMoment -> "照片时刻"
        is TimelineItem.UserFact -> item.fact.key
    }
}

/**
 * 获取项目内容
 */
private fun getItemContent(item: TimelineItem): String {
    return when (item) {
        is TimelineItem.Conversation -> item.log.userInput
        is TimelineItem.AiSummary -> item.summary.content
        is TimelineItem.Milestone -> item.description
        is TimelineItem.PhotoMoment -> item.description
        is TimelineItem.UserFact -> item.fact.value
    }
}

/**
 * 获取来源标签
 */
private fun getSourceLabel(item: TimelineItem): String? {
    return when (item) {
        is TimelineItem.UserFact -> "手动添加"
        is TimelineItem.AiSummary -> null // AI总结不需要来源标签
        else -> null
    }
}

/**
 * 获取AI建议
 */
private fun getAiSuggestion(item: TimelineItem): String? {
    return when (item) {
        is TimelineItem.Conversation -> item.log.aiResponse
        else -> null
    }
}

/**
 * 获取分数变化
 */
private fun getScoreChange(item: TimelineItem): Int? {
    return when (item) {
        is TimelineItem.AiSummary -> item.summary.relationshipScoreChange
        else -> null
    }
}

/**
 * 获取标签列表
 */
private fun getTags(item: TimelineItem): List<String> {
    return when (item) {
        is TimelineItem.AiSummary -> item.summary.updatedTags.map { it.content }
        else -> emptyList()
    }
}

/**
 * 空时光轴状态
 */
@Composable
private fun EmptyTimelineState(modifier: Modifier = Modifier) {
    EmptyView(
        message = "时光轴空空如也\n开始记录你们的故事吧",
        actionText = null,
        onAction = null,
        modifier = modifier
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "现代化时光轴", showBackground = true, heightDp = 600)
@Composable
private fun ModernTimelineViewPreview() {
    EmpathyTheme {
        ModernTimelineView(
            items = listOf(
                TimelineItem.Conversation(
                    id = "1",
                    timestamp = System.currentTimeMillis(),
                    emotionType = EmotionType.SWEET,
                    log = ConversationLog(
                        id = 1,
                        contactId = "contact_1",
                        userInput = "今天一起去看了电影，她很开心，说下次还想一起看。",
                        aiResponse = "建议用轻松的方式邀请，比如说发现了一家不错的餐厅想一起去尝尝",
                        timestamp = System.currentTimeMillis(),
                        isSummarized = true
                    )
                ),
                TimelineItem.AiSummary(
                    id = "2",
                    timestamp = System.currentTimeMillis() - 3600000,
                    emotionType = EmotionType.NEUTRAL,
                    summary = DailySummary(
                        id = 1,
                        contactId = "contact_1",
                        summaryDate = "2025-12-26",
                        content = "今天的互动整体氛围不错，你们讨论了周末的计划，对方表现出积极的态度。",
                        keyEvents = listOf(KeyEvent(event = "讨论周末计划", importance = 7)),
                        newFacts = emptyList(),
                        updatedTags = listOf(
                            TagUpdate(action = "ADD", type = "STRATEGY_GREEN", content = "约会"),
                            TagUpdate(action = "ADD", type = "STRATEGY_GREEN", content = "电影")
                        ),
                        relationshipScoreChange = 5,
                        relationshipTrend = RelationshipTrend.IMPROVING
                    )
                ),
                TimelineItem.Milestone(
                    id = "3",
                    timestamp = System.currentTimeMillis() - 86400000,
                    emotionType = EmotionType.GIFT,
                    title = "相识100天",
                    description = "从陌生到熟悉，感谢每一天的陪伴",
                    icon = "🏆"
                )
            ),
            onItemClick = {}
        )
    }
}

@Preview(name = "空时光轴", showBackground = true)
@Composable
private fun ModernTimelineViewEmptyPreview() {
    EmpathyTheme {
        ModernTimelineView(items = emptyList())
    }
}
