package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 快速选择标签组
 *
 * 设计规格:
 * - 横向滚动
 * - 背景: #E5E5EA
 * - 圆角: 响应式
 * - 文字: 14sp, 灰色
 * - 点击态: iOS蓝色背景（可选）
 *
 * @param tags 预设标签列表
 * @param onTagClick 标签点击回调
 * @param modifier Modifier
 *
 * @see TDD-00021 3.9节 QuickSelectTags组件规格
 */
@Composable
fun QuickSelectTags(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
    ) {
        tags.forEach { tag ->
            QuickSelectTagItem(
                text = tag,
                onClick = { onTagClick(tag) }
            )
        }
    }
}

/**
 * 快速选择标签项
 */
@Composable
private fun QuickSelectTagItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Text(
        text = text,
        fontSize = dimensions.fontSizeBody,  // BUG-00055: 使用响应式字体
        color = iOSTextSecondary,
        modifier = modifier
            .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
            .background(iOSSeparator)
            .clickable(onClick = onClick)
            .padding(horizontal = dimensions.spacingMediumSmall, vertical = 6.dp)
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "快速选择标签 - 少量", showBackground = true)
@Composable
private fun QuickSelectTagsFewPreview() {
    EmpathyTheme {
        QuickSelectTags(
            tags = listOf("内向", "外向", "乐观", "谨慎"),
            onTagClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "快速选择标签 - 多量", showBackground = true)
@Composable
private fun QuickSelectTagsManyPreview() {
    EmpathyTheme {
        QuickSelectTags(
            tags = listOf("阅读", "运动", "音乐", "旅行", "美食", "电影", "游戏", "摄影"),
            onTagClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "快速选择标签 - 长文本", showBackground = true)
@Composable
private fun QuickSelectTagsLongTextPreview() {
    EmpathyTheme {
        QuickSelectTags(
            tags = listOf("喜欢户外运动", "热爱阅读", "音乐发烧友", "美食探索者"),
            onTagClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
