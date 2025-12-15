package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.chip.ConfirmedTag
import com.empathy.ai.presentation.ui.component.chip.GuessedTag

/**
 * 标签分类区域组件
 *
 * 按类别展示标签，区分已确认和待确认标签
 *
 * 设计特点：
 * - 分类标题
 * - FlowRow自动换行布局
 * - 已确认标签使用ConfirmedTag
 * - 待确认标签使用GuessedTag（带呼吸动效）
 *
 * @param title 分类标题
 * @param tags 该分类下的所有标签
 * @param onTagClick 标签点击回调
 * @param enableAnimation 是否启用呼吸动效
 * @param modifier Modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySection(
    title: String,
    tags: List<BrainTag>,
    modifier: Modifier = Modifier,
    onTagClick: ((BrainTag) -> Unit)? = null,
    enableAnimation: Boolean = true
) {
    if (tags.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.SpacingSmall)
    ) {
        // 分类标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimensions.SpacingSmall)
        )
        
        // 标签流式布局
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                if (tag.isConfirmed) {
                    ConfirmedTag(
                        tag = tag,
                        onClick = { onTagClick?.invoke(tag) }
                    )
                } else {
                    GuessedTag(
                        tag = tag,
                        onClick = { onTagClick?.invoke(tag) },
                        enableAnimation = enableAnimation
                    )
                }
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "雷区标签分类", showBackground = true)
@Composable
private fun PreviewCategorySectionRisk() {
    EmpathyTheme {
        CategorySection(
            title = "🚫 雷区标签",
            tags = listOf(
                BrainTag(
                    id = 1,
                    contactId = "contact_1",
                    content = "不喜欢被催促",
                    type = TagType.RISK_RED,
                    isConfirmed = true,
                    source = "manual"
                ),
                BrainTag(
                    id = 2,
                    contactId = "contact_1",
                    content = "讨厌加班话题",
                    type = TagType.RISK_RED,
                    isConfirmed = true,
                    source = "manual"
                ),
                BrainTag(
                    id = 3,
                    contactId = "contact_1",
                    content = "可能不喜欢早起",
                    type = TagType.RISK_RED,
                    isConfirmed = false,
                    source = "ai"
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "策略标签分类", showBackground = true)
@Composable
private fun PreviewCategorySectionStrategy() {
    EmpathyTheme {
        CategorySection(
            title = "💡 策略标签",
            tags = listOf(
                BrainTag(
                    id = 4,
                    contactId = "contact_1",
                    content = "喜欢收到早安问候",
                    type = TagType.STRATEGY_GREEN,
                    isConfirmed = true,
                    source = "manual"
                ),
                BrainTag(
                    id = 5,
                    contactId = "contact_1",
                    content = "可能喜欢美食话题",
                    type = TagType.STRATEGY_GREEN,
                    isConfirmed = false,
                    source = "ai"
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "空分类", showBackground = true)
@Composable
private fun PreviewCategorySectionEmpty() {
    EmpathyTheme {
        CategorySection(
            title = "空分类",
            tags = emptyList(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
