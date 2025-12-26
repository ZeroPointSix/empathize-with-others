package com.empathy.ai.presentation.ui.component.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.MacaronTagColors

/**
 * 马卡龙色标签胶囊
 * 
 * 使用MacaronTagColors.getColorPair获取颜色对
 * - 圆角20dp
 * - 内边距14dp×8dp
 * 
 * @param text 标签文字
 * @param onClick 点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 5.2 MacaronTagChip马卡龙标签
 */
@Composable
fun MacaronTagChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = MacaronTagColors.getColorPair(text)
    
    Text(
        text = text,
        fontSize = 14.sp,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/**
 * 马卡龙标签组（FlowRow布局）
 * 
 * @param tags 标签列表
 * @param onTagClick 标签点击回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacaronTagGroup(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            MacaronTagChip(
                text = tag,
                onClick = { onTagClick(tag) }
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "单个标签", showBackground = true)
@Composable
private fun MacaronTagChipPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MacaronTagChip(
                text = "喜欢旅行",
                onClick = {}
            )
        }
    }
}

@Preview(name = "多个标签", showBackground = true)
@Composable
private fun MacaronTagGroupPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MacaronTagGroup(
                tags = listOf(
                    "喜欢旅行",
                    "爱看电影",
                    "健身达人",
                    "美食爱好者",
                    "摄影",
                    "阅读"
                ),
                onTagClick = {}
            )
        }
    }
}
