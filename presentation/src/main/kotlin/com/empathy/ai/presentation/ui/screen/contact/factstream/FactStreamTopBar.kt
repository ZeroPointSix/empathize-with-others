package com.empathy.ai.presentation.ui.screen.contact.factstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.control.SegmentedControl

/**
 * 事实流顶部控件组件
 *
 * 包含视图切换、添加事实和筛选按钮
 *
 * @param viewMode 当前视图模式
 * @param onViewModeChange 视图模式切换回调
 * @param onFilterClick 筛选按钮点击回调
 * @param onAddFactClick 添加事实按钮点击回调
 * @param modifier Modifier
 */
@Composable
fun FactStreamTopBar(
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddFactClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分段控制器
        SegmentedControl(
            items = listOf(ViewMode.Timeline.displayName, ViewMode.List.displayName),
            selectedIndex = if (viewMode == ViewMode.Timeline) 0 else 1,
            onItemSelected = { index ->
                onViewModeChange(if (index == 0) ViewMode.Timeline else ViewMode.List)
            },
            modifier = Modifier.weight(1f)
        )
        
        Row {
            // 添加事实按钮
            if (onAddFactClick != null) {
                IconButton(onClick = onAddFactClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加事实",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 筛选按钮
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "时光轴模式", showBackground = true)
@Composable
private fun PreviewFactStreamTopBarTimeline() {
    EmpathyTheme {
        FactStreamTopBar(
            viewMode = ViewMode.Timeline,
            onViewModeChange = {},
            onFilterClick = {}
        )
    }
}

@Preview(name = "列表模式", showBackground = true)
@Composable
private fun PreviewFactStreamTopBarList() {
    EmpathyTheme {
        FactStreamTopBar(
            viewMode = ViewMode.List,
            onViewModeChange = {},
            onFilterClick = {}
        )
    }
}
