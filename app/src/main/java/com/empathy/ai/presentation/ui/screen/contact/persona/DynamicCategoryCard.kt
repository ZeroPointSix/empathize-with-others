package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.empathy.ai.R
import com.empathy.ai.domain.model.FactCategory
import com.empathy.ai.presentation.theme.toComposeColor

/**
 * 动态分类卡片
 *
 * 显示一个分类及其下的标签，支持折叠/展开动画
 *
 * @param category 分类数据
 * @param isEditMode 是否处于编辑模式
 * @param selectedFactIds 已选中的Fact ID集合
 * @param searchQuery 搜索关键词（用于高亮）
 * @param isDarkMode 是否为深色模式
 * @param onToggleExpand 切换展开/折叠回调
 * @param onFactClick 标签点击回调
 * @param onFactLongClick 标签长按回调
 * @param onToggleFactSelection 切换标签选中状态回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicCategoryCard(
    category: FactCategory,
    isEditMode: Boolean,
    selectedFactIds: Set<String>,
    searchQuery: String,
    isDarkMode: Boolean,
    onToggleExpand: () -> Unit,
    onFactClick: (String) -> Unit,
    onFactLongClick: (String) -> Unit,
    onToggleFactSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = category.color.toComposeColor()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // 分类标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 颜色指示条
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor.titleColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 分类名称
                Text(
                    text = category.key,
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor.titleColor,
                    modifier = Modifier.weight(1f)
                )

                // 标签数量
                Text(
                    text = stringResource(R.string.tag_count, category.factCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 展开/折叠图标
                Icon(
                    imageVector = if (category.isExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = if (category.isExpanded) {
                        stringResource(R.string.collapse)
                    } else {
                        stringResource(R.string.expand)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 标签列表（带动画）
            AnimatedVisibility(
                visible = category.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    category.facts.forEach { fact ->
                        SelectableTagChip(
                            fact = fact,
                            isEditMode = isEditMode,
                            isSelected = selectedFactIds.contains(fact.id),
                            searchQuery = searchQuery,
                            categoryColor = categoryColor,
                            onClick = { onFactClick(fact.id) },
                            onLongClick = { onFactLongClick(fact.id) },
                            onToggleSelection = { onToggleFactSelection(fact.id) }
                        )
                    }
                }
            }
        }
    }
}
