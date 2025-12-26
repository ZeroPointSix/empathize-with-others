package com.empathy.ai.presentation.ui.component.factstream

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 现代化筛选胶囊组件
 * 
 * 设计规范：
 * - 选中状态：深黑色背景(#000000) + 白色文字
 * - 未选中状态：极浅灰色背景(#E5E5EA) + 深灰文字
 * - 胶囊圆角：16dp
 * - 支持横向流畅滑动
 * 
 * @param filters 筛选项列表
 * @param selectedFilter 当前选中的筛选项
 * @param onFilterSelected 筛选项选中回调
 * @param modifier 修饰符
 */
@Composable
fun <T> ModernFilterChips(
    filters: List<T>,
    selectedFilter: T,
    onFilterSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    filterLabel: (T) -> String = { it.toString() }
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            ModernFilterChip(
                text = filterLabel(filter),
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

/**
 * 单个现代化筛选胶囊
 */
@Composable
private fun ModernFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color.Black else Color(0xFFE5E5EA)
    val textColor = if (isSelected) Color.White else Color(0xFF3C3C43)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "筛选胶囊-全部选中", showBackground = true)
@Composable
private fun ModernFilterChipsAllSelectedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ModernFilterChips(
                filters = listOf("全部", "甜蜜", "冲突", "约会", "AI总结"),
                selectedFilter = "全部",
                onFilterSelected = {}
            )
        }
    }
}

@Preview(name = "筛选胶囊-甜蜜选中", showBackground = true)
@Composable
private fun ModernFilterChipsSweetSelectedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ModernFilterChips(
                filters = listOf("全部", "甜蜜", "冲突", "约会", "AI总结"),
                selectedFilter = "甜蜜",
                onFilterSelected = {}
            )
        }
    }
}
