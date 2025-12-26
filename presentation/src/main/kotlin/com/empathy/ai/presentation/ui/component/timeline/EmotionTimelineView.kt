package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import kotlinx.coroutines.delay

/**
 * 情绪时光轴视图
 * 
 * 技术要点:
 * - 使用LazyColumn实现虚拟化列表
 * - 提供稳定的key优化diff算法
 * - 使用AnimatedVisibility实现淡入动画（400ms）
 * - 错落延迟效果（50ms间隔）
 * - 左侧：情绪节点+连接线
 * - 右侧：内容卡片
 * 
 * @param items 时光轴数据列表
 * @param modifier 修饰符
 * @param onItemClick 项目点击回调
 * 
 * @see TDD-00020 4.3 EmotionTimelineView时光轴视图
 */
@Composable
fun EmotionTimelineView(
    items: List<TimelineItem>,
    modifier: Modifier = Modifier,
    onItemClick: (TimelineItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(
            items = items,
            // 使用复合key确保稳定性，避免数据更新时不必要的重组
            key = { _, item -> "${item.id}_${item.timestamp}" }
        ) { index, item ->
            TimelineRow(
                item = item,
                index = index,
                isLast = index == items.lastIndex,
                onItemClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    index: Int,
    isLast: Boolean,
    onItemClick: () -> Unit
) {
    // 淡入动画状态
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L) // 错落延迟
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
                .padding(horizontal = 16.dp)
        ) {
            // 左侧：情绪节点 + 连接线
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmotionNode(emotionType = item.emotionType)
                
                if (!isLast) {
                    TimelineLine(height = 80.dp)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 右侧：内容卡片
            Column(modifier = Modifier.weight(1f)) {
                TimelineCard(
                    item = item,
                    onClick = onItemClick
                )
                
                if (!isLast) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "时光轴视图", showBackground = true)
@Composable
private fun EmotionTimelineViewPreview() {
    EmpathyTheme {
        EmotionTimelineView(
            items = TimelineItem.createSampleList(),
            onItemClick = {}
        )
    }
}
