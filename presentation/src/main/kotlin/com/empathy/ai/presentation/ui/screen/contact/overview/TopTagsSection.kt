package com.empathy.ai.presentation.ui.screen.contact.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.chip.SolidTagChip
import com.empathy.ai.presentation.ui.component.chip.SolidTagColors

/**
 * 核心标签速览组件
 *
 * 展示权重最高的3-5个标签，横向滚动
 *
 * 职责：
 * - 展示核心标签列表
 * - 按权重排序，最多显示5个
 * - 根据标签类别显示不同颜色
 *
 * @param tags 标签列表（已按权重排序）
 * @param onTagClick 标签点击回调
 * @param modifier Modifier
 */
@Composable
fun TopTagsSection(
    tags: List<Fact>,
    modifier: Modifier = Modifier,
    onTagClick: ((Fact) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.SpacingMedium)
    ) {
        // 标题
        Text(
            text = "核心标签",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = Dimensions.SpacingMedium,
                vertical = Dimensions.SpacingSmall
            )
        )
        
        // 标签列表
        if (tags.isEmpty()) {
            // 空状态
            Text(
                text = "暂无标签，AI正在学习中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimensions.SpacingMedium)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                contentPadding = PaddingValues(horizontal = Dimensions.SpacingMedium)
            ) {
                items(
                    items = tags.take(5),
                    key = { it.id }  // 使用唯一ID作为key，避免timestamp重复导致崩溃
                ) { tag ->
                    SolidTagChip(
                        text = tag.value,
                        backgroundColor = SolidTagColors.getColorByCategory(tag.key)
                    )
                }
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "有标签", showBackground = true)
@Composable
private fun PreviewTopTagsSectionWithTags() {
    EmpathyTheme {
        TopTagsSection(
            tags = listOf(
                Fact(
                    key = "兴趣爱好",
                    value = "喜欢吃辣",
                    source = FactSource.MANUAL,
                    timestamp = 1L
                ),
                Fact(
                    key = "兴趣爱好",
                    value = "猫奴",
                    source = FactSource.MANUAL,
                    timestamp = 2L
                ),
                Fact(
                    key = "性格特征",
                    value = "工作狂",
                    source = FactSource.AI_INFERRED,
                    timestamp = 3L
                ),
                Fact(
                    key = "禁忌话题",
                    value = "不要提前任",
                    source = FactSource.MANUAL,
                    timestamp = 4L
                )
            )
        )
    }
}

@Preview(name = "无标签", showBackground = true)
@Composable
private fun PreviewTopTagsSectionEmpty() {
    EmpathyTheme {
        TopTagsSection(tags = emptyList())
    }
}
