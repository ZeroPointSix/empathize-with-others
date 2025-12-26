package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.ui.component.state.EmptyView

/**
 * 现代化清单视图组件
 * 
 * 设计规范：
 * - 紧凑的列表项（类似文件管理）
 * - 左侧圆形勾选框/多选框（强调操作性）
 * - 可按类型、重要度排序
 * - 主要交互：批量选择、删除、导出
 * 
 * 与时光轴的区别：
 * - 时光轴：垂直连线+彩色节点（强调连续性），大圆角气泡，阅读/回顾
 * - 清单：圆形勾选框（强调操作性），紧凑列表项，批量操作
 * 
 * @param items 时间线项目列表
 * @param selectedItems 已选中的项目ID集合
 * @param onItemClick 项目点击回调
 * @param onItemSelect 项目选中/取消选中回调
 * @param isSelectionMode 是否处于选择模式
 * @param modifier 修饰符
 */
@Composable
fun ModernListView(
    items: List<TimelineItem>,
    modifier: Modifier = Modifier,
    selectedItems: Set<String> = emptySet(),
    onItemClick: ((TimelineItem) -> Unit)? = null,
    onItemSelect: ((String, Boolean) -> Unit)? = null,
    isSelectionMode: Boolean = false
) {
    if (items.isEmpty()) {
        EmptyListState(modifier = modifier)
        return
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            ModernListRow(
                item = item,
                isSelected = item.id in selectedItems,
                isSelectionMode = isSelectionMode,
                onClick = { onItemClick?.invoke(item) },
                onSelect = { selected -> onItemSelect?.invoke(item.id, selected) }
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(start = if (isSelectionMode) 56.dp else 16.dp),
                color = Color(0xFFE5E5EA),
                thickness = 0.5.dp
            )
        }
    }
}

/**
 * 现代化清单行组件
 */
@Composable
private fun ModernListRow(
    item: TimelineItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFF2F2F7) else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：勾选框（选择模式下显示）
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) iOSBlue else Color.Transparent)
                    .clickable { onSelect(!isSelected) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFE5E5EA))
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        // 情绪指示点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(getEmotionColor(item.emotionType))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 中间：内容
        Column(modifier = Modifier.weight(1f)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getListItemTitle(item),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 时间
                Text(
                    text = formatTimeOnly(item.timestamp),
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // 内容预览
            Text(
                text = getListItemContent(item),
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 右侧：类型标签
        getTypeTag(item)?.let { (text, color) ->
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 10.sp,
                    color = color
                )
            }
        }
    }
}

/**
 * 获取情绪颜色
 */
private fun getEmotionColor(emotionType: EmotionType): Color {
    return when (emotionType) {
        EmotionType.SWEET -> Color(0xFFFFB6C1)
        EmotionType.CONFLICT -> Color(0xFFFF6B6B)
        EmotionType.DATE -> Color(0xFFBA55D3)
        EmotionType.GIFT -> Color(0xFFFFD700)
        EmotionType.DEEP_TALK -> Color(0xFF20B2AA)
        EmotionType.NEUTRAL -> Color(0xFFB0C4DE)
    }
}

/**
 * 获取列表项标题
 */
private fun getListItemTitle(item: TimelineItem): String {
    return when (item) {
        is TimelineItem.Conversation -> item.log.userInput.take(30)
        is TimelineItem.AiSummary -> "AI 总结"
        is TimelineItem.Milestone -> item.title
        is TimelineItem.PhotoMoment -> "照片时刻"
        is TimelineItem.UserFact -> item.fact.key
    }
}

/**
 * 获取列表项内容
 */
private fun getListItemContent(item: TimelineItem): String {
    return when (item) {
        is TimelineItem.Conversation -> item.log.aiResponse?.take(50) ?: ""
        is TimelineItem.AiSummary -> item.summary.content.take(50)
        is TimelineItem.Milestone -> item.description
        is TimelineItem.PhotoMoment -> item.description
        is TimelineItem.UserFact -> item.fact.value
    }
}

/**
 * 获取类型标签
 */
private fun getTypeTag(item: TimelineItem): Pair<String, Color>? {
    return when (item) {
        is TimelineItem.AiSummary -> "AI" to Color(0xFF5856D6)
        is TimelineItem.Milestone -> "里程碑" to Color(0xFFFF9500)
        is TimelineItem.UserFact -> "手动" to Color(0xFF34C759)
        else -> null
    }
}

/**
 * 空清单状态
 */
@Composable
private fun EmptyListState(modifier: Modifier = Modifier) {
    EmptyView(
        message = "暂无记录\n开始聊天，记录会自动出现在这里",
        actionText = null,
        onAction = null,
        modifier = modifier
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "现代化清单视图", showBackground = true, heightDp = 400)
@Composable
private fun ModernListViewPreview() {
    EmpathyTheme {
        ModernListView(
            items = listOf(
                TimelineItem.Conversation(
                    id = "1",
                    timestamp = System.currentTimeMillis(),
                    emotionType = EmotionType.SWEET,
                    log = ConversationLog(
                        id = 1,
                        contactId = "contact_1",
                        userInput = "今天一起去看了电影，她很开心",
                        aiResponse = "建议用轻松的方式邀请",
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
                        content = "今天的互动整体氛围不错",
                        keyEvents = listOf(KeyEvent(event = "讨论周末计划", importance = 7)),
                        newFacts = emptyList(),
                        updatedTags = emptyList(),
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

@Preview(name = "选择模式", showBackground = true, heightDp = 400)
@Composable
private fun ModernListViewSelectionModePreview() {
    EmpathyTheme {
        ModernListView(
            items = listOf(
                TimelineItem.Conversation(
                    id = "1",
                    timestamp = System.currentTimeMillis(),
                    emotionType = EmotionType.SWEET,
                    log = ConversationLog(
                        id = 1,
                        contactId = "contact_1",
                        userInput = "今天一起去看了电影",
                        aiResponse = "建议用轻松的方式邀请",
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
                        content = "今天的互动整体氛围不错",
                        keyEvents = emptyList(),
                        newFacts = emptyList(),
                        updatedTags = emptyList(),
                        relationshipScoreChange = 5,
                        relationshipTrend = RelationshipTrend.IMPROVING
                    )
                )
            ),
            selectedItems = setOf("1"),
            isSelectionMode = true,
            onItemClick = {},
            onItemSelect = { _, _ -> }
        )
    }
}
