package com.empathy.ai.presentation.ui.component.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 筛选胶囊行
 * 
 * 水平滚动的筛选胶囊列表
 * - 选中状态使用iOSBlue背景+白色文字
 * - 未选中状态使用透明背景+iOSTextSecondary文字
 * 
 * @param filters 筛选项列表
 * @param selectedFilter 当前选中的筛选项
 * @param onFilterSelected 筛选项选中回调
 * @param modifier 修饰符
 * @param filterLabel 获取筛选项显示文字的函数
 * 
 * @see TDD-00020 8.2 FactStreamTab筛选胶囊
 */
@Composable
fun <T> FilterChipRow(
    filters: List<T>,
    selectedFilter: T,
    onFilterSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    filterLabel: (T) -> String = { it.toString() }
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                text = filterLabel(filter),
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

/**
 * 单个筛选胶囊
 */
@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) iOSBlue else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else iOSTextSecondary
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "情绪筛选", showBackground = true)
@Composable
private fun FilterChipRowEmotionPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FilterChipRow(
                filters = listOf("全部", "甜蜜", "冲突", "约会", "AI总结"),
                selectedFilter = "全部",
                onFilterSelected = {}
            )
        }
    }
}

@Preview(name = "选中甜蜜", showBackground = true)
@Composable
private fun FilterChipRowSelectedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FilterChipRow(
                filters = listOf("全部", "甜蜜", "冲突", "约会", "AI总结"),
                selectedFilter = "甜蜜",
                onFilterSelected = {}
            )
        }
    }
}
