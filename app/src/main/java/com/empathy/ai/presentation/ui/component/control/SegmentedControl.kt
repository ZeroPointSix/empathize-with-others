package com.empathy.ai.presentation.ui.component.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 分段控制器组件
 *
 * 提供多选项切换控件，用于在不同视图模式之间切换
 *
 * 职责：
 * - 提供多选项切换功能
 * - 支持选中状态的视觉反馈
 * - 使用Material Design 3风格
 *
 * 技术要点：
 * - 使用Surface实现圆角背景
 * - 选中项使用Primary颜色高亮
 * - 未选中项使用透明背景
 *
 * @param items 选项列表
 * @param selectedIndex 当前选中的索引
 * @param onItemSelected 选项选中回调
 * @param modifier Modifier
 *
 * 使用示例：
 * ```kotlin
 * SegmentedControl(
 *     items = listOf("时光轴", "清单"),
 *     selectedIndex = 0,
 *     onItemSelected = { index -> /* 处理选择 */ }
 * )
 * ```
 */
@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onItemSelected(index) },
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(
                            vertical = 8.dp,
                            horizontal = 16.dp
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "双选项", showBackground = true)
@Composable
private fun PreviewSegmentedControlTwoItems() {
    EmpathyTheme {
        var selectedIndex by remember { mutableIntStateOf(0) }
        SegmentedControl(
            items = listOf("时光轴", "清单"),
            selectedIndex = selectedIndex,
            onItemSelected = { selectedIndex = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "三选项", showBackground = true)
@Composable
private fun PreviewSegmentedControlThreeItems() {
    EmpathyTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }
        SegmentedControl(
            items = listOf("概览", "事实流", "标签"),
            selectedIndex = selectedIndex,
            onItemSelected = { selectedIndex = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "四选项", showBackground = true)
@Composable
private fun PreviewSegmentedControlFourItems() {
    EmpathyTheme {
        var selectedIndex by remember { mutableIntStateOf(2) }
        SegmentedControl(
            items = listOf("概览", "事实流", "标签", "资料库"),
            selectedIndex = selectedIndex,
            onItemSelected = { selectedIndex = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}
