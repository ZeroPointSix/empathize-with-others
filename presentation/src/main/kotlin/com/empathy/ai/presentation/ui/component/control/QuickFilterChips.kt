package com.empathy.ai.presentation.ui.component.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.util.icon

/**
 * 快速筛选组件
 *
 * 提供快速筛选按钮组，用于事实流的筛选功能
 *
 * @param selectedFilters 当前选中的筛选条件
 * @param onFilterToggle 筛选条件切换回调
 * @param modifier Modifier
 */
@Composable
fun QuickFilterChips(
    selectedFilters: Set<FilterType>,
    onFilterToggle: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
        contentPadding = PaddingValues(horizontal = Dimensions.SpacingMedium)
    ) {
        items(FilterType.entries) { filter ->
            val isSelected = filter in selectedFilters || 
                (selectedFilters.isEmpty() && filter == FilterType.ALL)
            
            FilterChip(
                selected = isSelected,
                onClick = { onFilterToggle(filter) },
                label = { 
                    Text(
                        text = filter.displayName,
                        style = MaterialTheme.typography.labelMedium
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

// ========== 预览 ==========

@Preview(name = "默认状态（全部选中）", showBackground = true)
@Composable
private fun PreviewQuickFilterChipsDefault() {
    EmpathyTheme {
        QuickFilterChips(
            selectedFilters = emptySet(),
            onFilterToggle = {}
        )
    }
}

@Preview(name = "选中AI总结", showBackground = true)
@Composable
private fun PreviewQuickFilterChipsAiSelected() {
    EmpathyTheme {
        var selectedFilters by remember { mutableStateOf(setOf(FilterType.AI_SUMMARY)) }
        QuickFilterChips(
            selectedFilters = selectedFilters,
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

@Preview(name = "多选状态", showBackground = true)
@Composable
private fun PreviewQuickFilterChipsMultiple() {
    EmpathyTheme {
        QuickFilterChips(
            selectedFilters = setOf(FilterType.CONFLICT, FilterType.SWEET),
            onFilterToggle = {}
        )
    }
}
