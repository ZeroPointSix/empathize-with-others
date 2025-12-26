package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagUpdate
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.animation.AnimatedViewSwitch
import com.empathy.ai.presentation.ui.component.ios.IOSSegmentedControl

/**
 * 现代化事实流标签页组件
 * 
 * 设计规范：
 * 1. 顶部导航：标题"事实流"居中 + 右上角"+"添加按钮
 * 2. iOS分段控制器：时光轴 | 清单（灰色背景+白色滑块）
 * 3. 现代化筛选胶囊：深黑选中/浅灰未选中
 * 4. 时光轴视图：左侧轴线+彩色节点+白色气泡卡片
 * 5. 清单视图：紧凑列表+勾选框+批量操作
 * 
 * 两种视图的明确分工：
 * - 时光轴：强调连续性，阅读/回顾/点赞
 * - 清单：强调操作性，批量选择/删除/导出
 * 
 * @param items 时间线项目列表
 * @param viewMode 当前视图模式
 * @param selectedFilter 选中的筛选条件
 * @param onViewModeChange 视图模式切换回调
 * @param onFilterChange 筛选条件切换回调
 * @param onItemClick 项目点击回调
 * @param onAddClick 添加按钮点击回调
 * @param modifier Modifier
 */
@Composable
fun ModernFactStreamTab(
    items: List<TimelineItem>,
    viewMode: ViewMode,
    selectedFilter: String,
    onViewModeChange: (ViewMode) -> Unit,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((TimelineItem) -> Unit)? = null,
    onAddClick: (() -> Unit)? = null
) {
    // 应用筛选
    val filteredItems = remember(items, selectedFilter) {
        if (selectedFilter == "全部") {
            items
        } else {
            items.filter { item ->
                when (selectedFilter) {
                    "甜蜜" -> item.emotionType == EmotionType.SWEET
                    "冲突" -> item.emotionType == EmotionType.CONFLICT
                    "约会" -> item.emotionType == EmotionType.DATE
                    "AI总结" -> item is TimelineItem.AiSummary
                    else -> true
                }
            }
        }
    }
    
    // 视图模式索引
    var selectedTabIndex by remember { 
        mutableIntStateOf(if (viewMode == ViewMode.Timeline) 0 else 1) 
    }
    
    // 筛选类型列表
    val filterTypes = listOf("全部", "甜蜜", "冲突", "约会", "AI总结")
    
    // 清单视图的选择状态
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // iOS系统分组背景色
    ) {
        // 顶部导航栏
        FactStreamHeader(
            onAddClick = { onAddClick?.invoke() }
        )
        
        // iOS分段控制器
        IOSSegmentedControl(
            tabs = listOf("时光轴", "清单"),
            selectedIndex = selectedTabIndex,
            onTabSelected = { index ->
                selectedTabIndex = index
                onViewModeChange(if (index == 0) ViewMode.Timeline else ViewMode.List)
                // 切换视图时退出选择模式
                if (index == 0) {
                    isSelectionMode = false
                    selectedItems = emptySet()
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 现代化筛选胶囊
        ModernFilterChips(
            filters = filterTypes,
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterChange
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 视图内容（带动画切换）
        AnimatedViewSwitch(
            targetState = viewMode,
            modifier = Modifier.weight(1f)
        ) { mode ->
            when (mode) {
                ViewMode.Timeline -> ModernTimelineView(
                    items = filteredItems,
                    onItemClick = onItemClick
                )
                ViewMode.List -> ModernListView(
                    items = filteredItems,
                    selectedItems = selectedItems,
                    isSelectionMode = isSelectionMode,
                    onItemClick = { item ->
                        if (isSelectionMode) {
                            selectedItems = if (item.id in selectedItems) {
                                selectedItems - item.id
                            } else {
                                selectedItems + item.id
                            }
                        } else {
                            onItemClick?.invoke(item)
                        }
                    },
                    onItemSelect = { id, selected ->
                        selectedItems = if (selected) {
                            selectedItems + id
                        } else {
                            selectedItems - id
                        }
                    }
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "时光轴模式", showBackground = true, heightDp = 700)
@Composable
private fun ModernFactStreamTabTimelinePreview() {
    EmpathyTheme {
        var viewMode by remember { mutableStateOf(ViewMode.Timeline) }
        var selectedFilter by remember { mutableStateOf("全部") }
        
        ModernFactStreamTab(
            items = getSampleItems(),
            viewMode = viewMode,
            selectedFilter = selectedFilter,
            onViewModeChange = { viewMode = it },
            onFilterChange = { selectedFilter = it },
            onItemClick = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "清单模式", showBackground = true, heightDp = 700)
@Composable
private fun ModernFactStreamTabListPreview() {
    EmpathyTheme {
        var viewMode by remember { mutableStateOf(ViewMode.List) }
        var selectedFilter by remember { mutableStateOf("全部") }
        
        ModernFactStreamTab(
            items = getSampleItems(),
            viewMode = viewMode,
            selectedFilter = selectedFilter,
            onViewModeChange = { viewMode = it },
            onFilterChange = { selectedFilter = it },
            onItemClick = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "空数据", showBackground = true, heightDp = 700)
@Composable
private fun ModernFactStreamTabEmptyPreview() {
    EmpathyTheme {
        ModernFactStreamTab(
            items = emptyList(),
            viewMode = ViewMode.Timeline,
            selectedFilter = "全部",
            onViewModeChange = {},
            onFilterChange = {},
            onItemClick = {},
            onAddClick = {}
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
            userInput = "今天一起去看了电影，她很开心，说下次还想一起看。整体氛围很好，感觉关系在稳步升温。",
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
            content = "今天的互动整体氛围不错，你们讨论了周末的计划，对方表现出积极的态度。建议继续保持这种良好的沟通习惯。",
            keyEvents = listOf(
                KeyEvent(event = "讨论周末计划", importance = 7),
                KeyEvent(event = "分享美食照片", importance = 5)
            ),
            newFacts = emptyList(),
            updatedTags = listOf(
                TagUpdate(action = "ADD", type = "STRATEGY_GREEN", content = "约会"),
                TagUpdate(action = "ADD", type = "STRATEGY_GREEN", content = "电影")
            ),
            relationshipScoreChange = 5,
            relationshipTrend = RelationshipTrend.IMPROVING
        )
    ),
    TimelineItem.Conversation(
        id = "3",
        timestamp = System.currentTimeMillis() - 7200000,
        emotionType = EmotionType.CONFLICT,
        log = ConversationLog(
            id = 2,
            contactId = "contact_1",
            userInput = "因为工作的事情有点小争执，但最后还是和好了。",
            aiResponse = "建议下次遇到类似情况时，先倾听对方的想法，再表达自己的观点。",
            timestamp = System.currentTimeMillis() - 7200000,
            isSummarized = false
        )
    ),
    TimelineItem.Milestone(
        id = "4",
        timestamp = System.currentTimeMillis() - 86400000,
        emotionType = EmotionType.GIFT,
        title = "相识100天",
        description = "从陌生到熟悉，感谢每一天的陪伴",
        icon = "🏆"
    )
)
