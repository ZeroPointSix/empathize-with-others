package com.empathy.ai.presentation.ui.screen.contact.factstream

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.theme.AnimationSpec
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 事实流标签页组件
 *
 * 整合双视图（时光轴/清单列表）和顶部控件
 *
 * 职责：
 * - 管理视图模式切换
 * - 提供流体切换动画
 * - 整合筛选功能
 *
 * @param items 时间线项目列表
 * @param viewMode 当前视图模式
 * @param selectedFilters 选中的筛选条件
 * @param onViewModeChange 视图模式切换回调
 * @param onFilterToggle 筛选条件切换回调
 * @param onItemClick 项目点击回调
 * @param onFilterButtonClick 筛选按钮点击回调
 * @param modifier Modifier
 */
@Composable
fun FactStreamTab(
    items: List<TimelineItem>,
    viewMode: ViewMode,
    selectedFilters: Set<FilterType>,
    onViewModeChange: (ViewMode) -> Unit,
    onFilterToggle: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((TimelineItem) -> Unit)? = null,
    onFilterButtonClick: (() -> Unit)? = null
) {
    // 应用筛选
    val filteredItems = remember(items, selectedFilters) {
        if (selectedFilters.isEmpty() || selectedFilters.contains(FilterType.ALL)) {
            items
        } else {
            items.filter { item ->
                selectedFilters.any { filter -> filter.apply(listOf(item)).isNotEmpty() }
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 顶部控件
        FactStreamTopBar(
            viewMode = viewMode,
            onViewModeChange = onViewModeChange,
            onFilterClick = { onFilterButtonClick?.invoke() }
        )
        
        // 视图内容（带动画切换）
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(AnimationSpec.DurationNormal)) +
                    scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpec.DurationNormal)
                    )).togetherWith(
                    fadeOut(animationSpec = tween(AnimationSpec.DurationNormal)) +
                        scaleOut(
                            targetScale = 0.95f,
                            animationSpec = tween(AnimationSpec.DurationNormal)
                        )
                )
            },
            label = "ViewModeTransition",
            modifier = Modifier.weight(1f)
        ) { mode ->
            when (mode) {
                ViewMode.Timeline -> TimelineView(
                    items = filteredItems,
                    onItemClick = onItemClick
                )
                ViewMode.List -> ListView(
                    items = filteredItems,
                    selectedFilters = selectedFilters,
                    onFilterToggle = onFilterToggle,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "时光轴模式", showBackground = true)
@Composable
private fun PreviewFactStreamTabTimeline() {
    EmpathyTheme {
        var viewMode by remember { mutableStateOf(ViewMode.Timeline) }
        var selectedFilters by remember { mutableStateOf<Set<FilterType>>(emptySet()) }
        
        FactStreamTab(
            items = getSampleItems(),
            viewMode = viewMode,
            selectedFilters = selectedFilters,
            onViewModeChange = { viewMode = it },
            onFilterToggle = { filter ->
                selectedFilters = if (filter in selectedFilters) {
                    selectedFilters - filter
                } else {
                    selectedFilters + filter
                }
            }
        )
    }
}

@Preview(name = "列表模式", showBackground = true)
@Composable
private fun PreviewFactStreamTabList() {
    EmpathyTheme {
        var viewMode by remember { mutableStateOf(ViewMode.List) }
        var selectedFilters by remember { mutableStateOf<Set<FilterType>>(emptySet()) }
        
        FactStreamTab(
            items = getSampleItems(),
            viewMode = viewMode,
            selectedFilters = selectedFilters,
            onViewModeChange = { viewMode = it },
            onFilterToggle = { filter ->
                selectedFilters = if (filter in selectedFilters) {
                    selectedFilters - filter
                } else {
                    selectedFilters + filter
                }
            }
        )
    }
}

@Preview(name = "空数据", showBackground = true)
@Composable
private fun PreviewFactStreamTabEmpty() {
    EmpathyTheme {
        FactStreamTab(
            items = emptyList(),
            viewMode = ViewMode.Timeline,
            selectedFilters = emptySet(),
            onViewModeChange = {},
            onFilterToggle = {}
        )
    }
}

/**
 * 示例数据
 */
private fun getSampleItems(): List<TimelineItem> = listOf(
    TimelineItem.Conversation(
        id = "1",
        timestamp = System.currentTimeMillis(),
        emotionType = EmotionType.SWEET,
        log = ConversationLog(
            id = 1,
            contactId = "contact_1",
            userInput = "今天想约她出去吃饭，但不知道怎么开口比较好",
            aiResponse = "建议用轻松的方式邀请，比如说发现了一家不错的餐厅想一起去尝尝",
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
            content = "今天的互动整体氛围不错，你们讨论了周末的计划，对方表现出积极的态度。",
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
    TimelineItem.Milestone(
        id = "3",
        timestamp = System.currentTimeMillis() - 172800000,
        emotionType = EmotionType.GIFT,
        title = "相识100天",
        description = "从陌生到熟悉，感谢每一天的陪伴",
        icon = "🏆"
    )
)
