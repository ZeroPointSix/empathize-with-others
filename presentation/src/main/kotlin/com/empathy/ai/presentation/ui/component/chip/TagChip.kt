package com.empathy.ai.presentation.ui.component.chip

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.util.buildHighlightedText
import com.empathy.ai.presentation.util.createSearchHighlightStyle

/**
 * 标签芯片组件
 *
 * 用于显示大脑标签（雷区或策略）
 *
 * @param text 标签文本
 * @param tagType 标签类型
 * @param highlightQuery 搜索关键词（可选）
 * @param onDelete 删除回调，为null时不显示删除按钮
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun TagChip(
    text: String,
    tagType: TagType,
    highlightQuery: String = "",
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val colors = getTagColors(tagType)
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = colors.iconColor
    )
    
    AssistChip(
        onClick = { onClick?.invoke() },
        label = {
            Text(
                text = buildHighlightedText(
                    text = text,
                    query = highlightQuery,
                    highlightStyle = highlightStyle
                )
            )
        },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = when (tagType) {
                    TagType.RISK_RED -> Icons.Default.Warning
                    TagType.STRATEGY_GREEN -> Icons.Default.Lightbulb
                },
                contentDescription = when (tagType) {
                    TagType.RISK_RED -> "雷区"
                    TagType.STRATEGY_GREEN -> "策略"
                },
                modifier = Modifier.size(dimensions.iconSizeSmall),
                tint = colors.iconColor
            )
        },
        trailingIcon = if (onDelete != null) {
            {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(dimensions.iconSizeSmall + 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(dimensions.iconSizeSmall - 2.dp)
                    )
                }
            }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.backgroundColor,
            labelColor = colors.textColor,
            leadingIconContentColor = colors.iconColor
        )
    )
}

/**
 * 标签颜色配置
 */
private data class TagColors(
    val backgroundColor: Color,
    val textColor: Color,
    val iconColor: Color
)

/**
 * 获取标签类型对应的颜色
 * 使用MaterialTheme语义化颜色，自动适配深色模式
 */
@Composable
private fun getTagColors(tagType: TagType): TagColors {
    return when (tagType) {
        TagType.RISK_RED -> TagColors(
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer,
            iconColor = MaterialTheme.colorScheme.error
        )
        TagType.STRATEGY_GREEN -> TagColors(
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
            iconColor = MaterialTheme.colorScheme.tertiary
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "雷区标签", showBackground = true)
@Composable
private fun TagChipRiskPreview() {
    EmpathyTheme {
        TagChip(
            text = "不要提前妻",
            tagType = TagType.RISK_RED,
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(name = "策略标签", showBackground = true)
@Composable
private fun TagChipStrategyPreview() {
    EmpathyTheme {
        TagChip(
            text = "多夸他衣品好",
            tagType = TagType.STRATEGY_GREEN,
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(name = "无删除按钮", showBackground = true)
@Composable
private fun TagChipNoDeletePreview() {
    EmpathyTheme {
        TagChip(
            text = "忌讳迟到",
            tagType = TagType.RISK_RED,
            onDelete = null,
            onClick = {}
        )
    }
}

@Preview(name = "长文本", showBackground = true)
@Composable
private fun TagChipLongTextPreview() {
    EmpathyTheme {
        TagChip(
            text = "喜欢聊家庭和孩子的教育问题",
            tagType = TagType.STRATEGY_GREEN,
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagChipDarkPreview() {
    EmpathyTheme {
        TagChip(
            text = "不要提前妻",
            tagType = TagType.RISK_RED,
            onDelete = {},
            onClick = {}
        )
    }
}
