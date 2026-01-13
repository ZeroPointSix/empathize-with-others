package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.presentation.theme.ComposeCategoryColor
import com.empathy.ai.presentation.util.buildHighlightedText

/**
 * 可选标签芯片
 *
 * 支持点击、长按和选中状态的标签组件
 *
 * @param fact Fact数据
 * @param isEditMode 是否处于编辑模式
 * @param isSelected 是否被选中
 * @param searchQuery 搜索关键词（用于高亮）
 * @param categoryColor 分类颜色
 * @param highlightStyle 高亮样式
 * @param onClick 点击回调
 * @param onLongClick 长按回调
 * @param onToggleSelection 切换选中状态回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableTagChip(
    fact: Fact,
    isEditMode: Boolean,
    isSelected: Boolean,
    searchQuery: String,
    categoryColor: ComposeCategoryColor,
    highlightStyle: SpanStyle,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据选中状态决定颜色
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        categoryColor.tagBackgroundColor
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        categoryColor.tagTextColor
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = {
                    if (isEditMode) {
                        onToggleSelection()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中状态图标
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.selected),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // 标签文本（支持搜索高亮）
        Text(
            text = buildHighlightedText(
                text = fact.value,
                query = searchQuery,
                highlightStyle = highlightStyle
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
