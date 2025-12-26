package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 事实清单视图
 * 
 * 简化的清单视图（无时光轴样式）
 * 使用LazyColumn显示卡片列表
 * 
 * @param items 时光轴数据列表
 * @param modifier 修饰符
 * @param onItemClick 项目点击回调
 * 
 * @see TDD-00020 8.2 FactStreamTab清单视图
 */
@Composable
fun FactListView(
    items: List<TimelineItem>,
    modifier: Modifier = Modifier,
    onItemClick: (TimelineItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            TimelineCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "清单视图", showBackground = true)
@Composable
private fun FactListViewPreview() {
    EmpathyTheme {
        FactListView(
            items = TimelineItem.createSampleList(),
            onItemClick = {}
        )
    }
}
